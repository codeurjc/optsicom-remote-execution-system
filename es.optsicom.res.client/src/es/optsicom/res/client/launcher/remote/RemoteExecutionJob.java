/* ******************************************************************************
 * This file is part of Optsicom Remote Experiment System client
 * 
 * License:
 *   EPL: http://www.eclipse.org/legal/epl-v10.html
 *   See the LICENSE file in the project's top-level directory for details.
 *   
 * Contributors:
 *   Optsicom(http://www.optsicom.es), Sidelab (http://www.sidelab.es) 
 *   and others
 * **************************************************************************** */
package es.optsicom.res.client.launcher.remote;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.rmi.Naming;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import es.optsicom.res.client.RESClientPlugin;
import es.optsicom.res.client.util.ProjectDependenciesResolver;
import es.optsicom.res.client.util.ZipCreator;
import es.optsicom.res.client.util.ZipCreatorException;
import es.optsicom.res.server.OptsicomRemoteExecutor;
import es.optsicom.res.server.OptsicomRemoteServer;
import es.optsicom.res.server.PaqueteDatos;

public class RemoteExecutionJob extends Job {

	private String host;
	private String portRMI;
	private String portDebug;
	private String password;
	private String mainClass;
	private String vmArgs;
	private String programArgs;
	private String zipName;
	private String mode;
	private ProjectDependenciesResolver resolver;
	
	public RemoteExecutionJob() {
		super("Remote Execution");
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//Accedemos al objeto remoto
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor);
			subMonitor.beginTask("Launching", 5);
			
			subMonitor.subTask("Connecting to server");
			RESClientPlugin.log("Entra");
			OptsicomRemoteServer veex = (OptsicomRemoteServer) Naming.lookup("//"+host+":"+portRMI+"/optsicom");
			OptsicomRemoteExecutor executor = veex.getExecutor();
			subMonitor.worked(1);
			
			while ( !executor.authenticate(password)) {
				RESClientPlugin.log("Authentication failed: wrong password");
				return new Status(IStatus.ERROR, RESClientPlugin.PLUGIN_ID, "Authentication failed: wrong password");
			}
			
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			
			if(zipName == null) {
				return new Status(IStatus.ERROR, RESClientPlugin.PLUGIN_ID, "Couldn't create zip file");
			}
			
			//Enviamos el .zip
			subMonitor.subTask("Sending zip file");
			String projectName = resolver.getJavaProject().getElementName();
			sendZip(zipName, executor, subMonitor.newChild(1));
			subMonitor.worked(1);
			
			subMonitor.subTask("Setting classpath and working dir");
			executor.setJarDirs(resolver.getClasspath());
			executor.setWorkingDir(projectName);
			subMonitor.worked(1);
			
			subMonitor.subTask("Launching the program");
			String zipLastSegment = zipName.substring(zipName.lastIndexOf(File.separator) + 1);
			String idjob = executor.launch(mode,password,host,portRMI,/*portServer,*/portDebug,vmArgs,programArgs,mainClass,zipLastSegment);
			executor.setState(idjob, "Running");
			subMonitor.worked(1);
			
			ScopedPreferenceStore sps =  (ScopedPreferenceStore) RESClientPlugin.getDefault().getPreferenceStore();
			sps.putValue(idjob,host+":"+portRMI);

			subMonitor.subTask("Opening remote console");
			openConsole(executor,idjob);
			subMonitor.worked(1);
			
			subMonitor.done();

			return new Status(IStatus.OK, RESClientPlugin.PLUGIN_ID, "");
		} catch (Exception e) {
			RESClientPlugin.log(e);
			return new Status(IStatus.ERROR, RESClientPlugin.PLUGIN_ID, e.getMessage());
		}
	}
	
	private void sendZip(String zipName, OptsicomRemoteExecutor executor, SubMonitor monitor) throws IOException {
		
		final File f = new File(zipName);
		
		InputStream in = new FileInputStream(f);
		byte[] buf = new byte[1024];
		int longitud;
    	
		monitor.beginTask("Sending zip file", 100);
		long fileLenght = f.length();
		long fileChunksToMonitor = fileLenght / 100;
		long bytesRead = 0;
		
		if ((longitud= in.read(buf)) > 0){
			PaqueteDatos pd = new PaqueteDatos(buf,longitud);
			executor.setZip(f,pd,true);
		}
		
		bytesRead += longitud;
		if(bytesRead > fileChunksToMonitor) {
			monitor.worked(1);
			bytesRead = 0;
		}
		
		while ( (longitud=in.read(buf)) > 0) {
			PaqueteDatos pd = new PaqueteDatos(buf,longitud);
			executor.setZip(f,pd,false);
			bytesRead += longitud;
			if(bytesRead > fileChunksToMonitor) {
				monitor.worked(1);
				bytesRead = 0;
			}
		}
		
		executor.setZip(f,new PaqueteDatos(buf,-1),false);
		in.close();
	}
	
	public void openConsole(final OptsicomRemoteExecutor executor,final String idjob) {
		
		MessageConsole miconsola;
		ConsolePlugin plugin;
		IConsoleManager cm;
		final IOConsoleOutputStream cos;
		miconsola = new MessageConsole("Consola"+idjob, null);
		miconsola.activate();

		plugin = ConsolePlugin.getDefault();
		cm = plugin.getConsoleManager();
		cm.addConsoles(new IConsole[]{miconsola});
		
		cos = miconsola.newOutputStream();
		
		new Thread(){
			public void run(){
				try {
					String cadena = "";
					IPath statePath = RESClientPlugin.getDefault().getStateLocation();
					File cFile = new File(statePath.toFile(), "Clientconsole_"+idjob+".txt");
					
					cos.write(cadena);
					cos.flush();

					for (;;){		
						FileWriter fw = new FileWriter(cFile,true);
						PrintWriter pw = new PrintWriter(fw);

						if (executor.hasProcessFinished(idjob)){
							cadena = executor.readConsole(idjob);
						}
						else{
							executor.acquireLock(idjob);
							cadena = executor.readConsole(idjob);
							executor.releaseLock(idjob);
						}
						
						
						if (cadena != null){
							pw.write(cadena+"\r\n");
							cos.write(cadena+"\n");
							cos.flush();
							pw.flush();
							pw.close();
						}
						else{
							if (executor.hasProcessFinished(idjob)){
								executor.setState(idjob, "Finished");
								break;
							}
							else{
								Thread.sleep(5000);
							}

						}
					}
					executor.setState(idjob, "Finished");
					cos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();	
	}



	public void setHost(String host) {
		this.host = host;
	}

	public void setPortRMI(String portRMI) {
		this.portRMI = portRMI;
	}

	public void setPortDebug(String portDebug) {
		this.portDebug = portDebug;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public void setVmArgs(String vmArgs) {
		this.vmArgs = vmArgs;
	}

	public void setProgramArgs(String programArgs) {
		this.programArgs = programArgs;
	}

	public void setDependenciesResolver(ProjectDependenciesResolver dependenciesResolver) {
		this.resolver = dependenciesResolver;
	}

	public String getZipName() {
		return zipName;
	}

	public void setMode(String mode) {
		this.mode = mode;		
	}

	public void setZipName(String zipName) {
		this.zipName = zipName;
	}

	public void setResolved(ProjectDependenciesResolver resolver) {
		this.resolver = resolver;		
	}

	
}
