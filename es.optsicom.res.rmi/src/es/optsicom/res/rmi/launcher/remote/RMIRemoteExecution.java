package es.optsicom.res.rmi.launcher.remote;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import es.optsicom.res.client.RESClientPlugin;
import es.optsicom.res.client.ZipFileCreation;
import es.optsicom.res.client.launcher.remote.IRemoteExecution;
import es.optsicom.res.client.util.DependenciesResolverException;
import es.optsicom.res.client.util.ProjectDependenciesResolver;
import es.optsicom.res.server.OptsicomRemoteExecutor;
import es.optsicom.res.server.OptsicomRemoteServer;
import es.optsicom.res.server.PaqueteDatos;

public class RMIRemoteExecution implements IRemoteExecution {
	private String name="RMI";
	private OptsicomRemoteExecutor executor;

	private String user;
	private String host;
	private String portRMI;
	private String portDebug;
	private String password;
	private String mainClass;
	private String[] vmArgs;
	private String[] programArgs;
	private String zipName;
	private String mode;
	private ProjectDependenciesResolver resolver;
	private List userSelectedResources;
	private IJavaProject project;
	//mgarcia: Optsicom Res evolution
	private static final String OPTSICOMRESOUTPUT = "OptsicomRESOutput";
	private static final String RESULTINGFILES = "ResultingFiles";
	
	public RMIRemoteExecution(){
		
	}
	public RMIRemoteExecution(String host, String portRMI){
		this.host=host;
		this.portRMI=portRMI;
	}
	@Override
	public void send(SubMonitor monitor) throws IOException {
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
			this.executor.setZip(f,pd,true);
		}
		
		bytesRead += longitud;
		if(bytesRead > fileChunksToMonitor) {
			monitor.worked(1);
			bytesRead = 0;
		}
		
		////mgarcia: Optiscom Res evolution
		boolean isCanceled = false;
		while ( ((longitud=in.read(buf)) > 0) && !isCanceled) {
			PaqueteDatos pd = new PaqueteDatos(buf,longitud);
			this.executor.setZip(f,pd,false);
			bytesRead += longitud;
			if (monitor.isCanceled()) {
				isCanceled = true;
			}
			if(bytesRead > fileChunksToMonitor) {
				monitor.worked(1);
				bytesRead = 0;
			}
		}
		
		this.executor.setZip(f,new PaqueteDatos(buf,-1),false);
		in.close();
		
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor);
			subMonitor.beginTask("Launching", 5);
			
			//mgarcia: Optiscom Res evolution
			subMonitor.subTask("Creating zip file");
			ZipFileCreation zipjob = new ZipFileCreation(project);
			zipjob.setUserSelectedResources(userSelectedResources);
			try {
				IStatus status = zipjob.create(subMonitor.newChild(1));
							
				if (subMonitor.isCanceled()) {
					return new Status(IStatus.CANCEL, RESClientPlugin.PLUGIN_ID, "Operation has been cancelled");
				}
				
				if(!status.isOK()) {
					RESClientPlugin.log("Problems exporting zip file: " + status.getMessage());
					return new Status(IStatus.ERROR, RESClientPlugin.PLUGIN_ID, "Error creating zip file");
				}
			} catch (DependenciesResolverException e) {
				return new Status(IStatus.INFO, RESClientPlugin.PLUGIN_ID, "Remote execution doesn't support dependencies not included in the workspace");
			}
			
			this.setZipName(zipjob.getZipName());
			this.setResolved(zipjob.getResolver());
			
			subMonitor.subTask("Connecting to server");
			RESClientPlugin.log("Entra");
			OptsicomRemoteServer veex = (OptsicomRemoteServer) Naming.lookup("//"+host+":"+portRMI+"/optsicom");
			this.executor = veex.getExecutor();
			subMonitor.worked(1);
			//mgarcia: Optiscom Res evolution
			if (subMonitor.isCanceled()) {
				return new Status(IStatus.CANCEL, RESClientPlugin.PLUGIN_ID, "Operation has been cancelled");
			}
			
			while ( !this.executor.authenticate(password)) {
				RESClientPlugin.log("Authentication failed: wrong password");
				return new Status(IStatus.ERROR, RESClientPlugin.PLUGIN_ID, "Authentication failed: wrong password");
			}
			
			if(zipName == null) {
				return new Status(IStatus.ERROR, RESClientPlugin.PLUGIN_ID, "Couldn't create zip file");
			}
			
			//Enviamos el .zip
			subMonitor.subTask("Sending zip file");
			String projectName = resolver.getJavaProject().getElementName();
			send(subMonitor.newChild(1));
			subMonitor.worked(1);
			//mgarcia: Optiscom Res evolution
			if (subMonitor.isCanceled()) {
				return new Status(IStatus.CANCEL, RESClientPlugin.PLUGIN_ID, "Operation has been cancelled");
			}
			
			subMonitor.subTask("Setting classpath and working dir");
			this.executor.setJarDirs(resolver.getClasspath());
			this.executor.setWorkingDir(projectName);
			subMonitor.worked(1);
			//mgarcia: Optiscom Res evolution
			if (subMonitor.isCanceled()) {
				return new Status(IStatus.CANCEL, RESClientPlugin.PLUGIN_ID, "Operation has been cancelled");
			}
			
			subMonitor.subTask("Launching the program");
			String zipLastSegment = zipName.substring(zipName.lastIndexOf(File.separator) + 1);
			String idjob = this.executor.launch(mode,password,host,portRMI,/*portServer,*/portDebug,vmArgs,programArgs,mainClass,zipLastSegment);
			this.executor.setState(idjob, "Running");
			subMonitor.worked(1);
			//mgarcia: Optiscom Res evolution
			if (subMonitor.isCanceled()) {
				return new Status(IStatus.CANCEL, RESClientPlugin.PLUGIN_ID, "Operation has been cancelled");
			}
			
			ScopedPreferenceStore sps =  (ScopedPreferenceStore) RESClientPlugin.getDefault().getPreferenceStore();
			sps.putValue(idjob,name+":"+host+":"+portRMI+":"+user+":"+password);

			//mgarcia: Optiscom Res evolution
			subMonitor.subTask("Getting resulting files");
			getResultingFile(idjob);
			subMonitor.worked(1);
			if (subMonitor.isCanceled()) {
				return new Status(IStatus.CANCEL, RESClientPlugin.PLUGIN_ID, "Operation has been cancelled");
			}
									
			subMonitor.subTask("Opening remote console");
			openConsole(idjob);
			subMonitor.worked(1);
			//mgarcia: Optiscom Res evolution
			if (subMonitor.isCanceled()) {
				return new Status(IStatus.CANCEL, RESClientPlugin.PLUGIN_ID, "Operation has been cancelled");
			}
			
			subMonitor.done();

			return new Status(IStatus.OK, RESClientPlugin.PLUGIN_ID, "");
		} catch (Exception e) {
			RESClientPlugin.log(e);
			return new Status(IStatus.ERROR, RESClientPlugin.PLUGIN_ID, e.getMessage());
		}
	}

	@Override
	public void openConsole(final String idjob) {
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
					//mgarcia: Optiscom Res evolution
					IWorkspace ws = ResourcesPlugin.getWorkspace();
					String workSpaceRoot = ws.getRoot().getLocation().toOSString();
					String projectName = resolver.getJavaProject().getElementName();
					StringBuffer executionResultsPath = new StringBuffer();
					executionResultsPath.append(workSpaceRoot);
					executionResultsPath.append(File.separator);
					executionResultsPath.append(projectName);
					executionResultsPath.append(File.separator);
					executionResultsPath.append(OPTSICOMRESOUTPUT);
					
					File wFile = new File(executionResultsPath.toString()); 
					if(!wFile.exists()){
						wFile.mkdir();
					}
					wFile = new File(executionResultsPath.toString(), "Clientconsole_"+idjob+".txt");
					
					cos.write(cadena);
					cos.flush();

					for (;;){		
						FileWriter fw = new FileWriter(cFile,true);
						PrintWriter pw = new PrintWriter(fw);
						//mgarcia: Optiscom Res evolution
						FileWriter cfw = new FileWriter(wFile,true);
						PrintWriter cpw = new PrintWriter(cfw);

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
							//mgarcia: Optiscom Res evolution
							cpw.write(cadena+"\r\n");
							cos.write(cadena+"\n");
							cos.flush();
							pw.flush();
							pw.close();
							//mgarcia: Optiscom Res evolution
							cpw.flush();
							cpw.close();
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

	@Override
	public void getResultingFile(final String idjob) {
		new Thread(){
			public void run(){
				try {
					List<File> resultingFiles = executor.checkResultFiles(idjob);
					if(!resultingFiles.isEmpty()){
						executor.createZipResultingFiles(resultingFiles,idjob);
						getZipResultingFile(idjob);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();	
		
	}

	public void getZipResultingFile(String idjob) {
		try {
			long fileLenght = this.executor.getResultsFileLength(idjob);
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			String workSpaceRoot = ws.getRoot().getLocation().toOSString();
			String projectName = resolver.getJavaProject().getElementName();
			StringBuffer executionResultsPath = new StringBuffer();
			executionResultsPath.append(workSpaceRoot);
			executionResultsPath.append(File.separator);
			executionResultsPath.append(projectName);
			executionResultsPath.append(File.separator);
			executionResultsPath.append(OPTSICOMRESOUTPUT);
			
			File wFile = new File(executionResultsPath.toString()); 
			if(!wFile.exists()){
				wFile.mkdir();
			}
			wFile = new File(executionResultsPath.toString(), RESULTINGFILES + idjob +  ".zip");
			
			try {
				
				this.executor.createFileInputString(idjob);
				
				OutputStream os;
				os = new FileOutputStream(wFile,true);
				
				PaqueteDatos pd = null;
				int longitud = 0;
				while(fileLenght > longitud){
					pd = this.executor.readResultsFile(idjob,longitud);
					if(pd!=null){
						if( pd.getLongitud() > 0){
							os.write(pd.getBuf(),0,pd.getLongitud());
							longitud += pd.getLongitud();
							os.flush();
						}
					}
					
				}
				os.close();
				
				this.executor.closeFileInputString(idjob);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// TODO Auto-generated method stub
		
	}

	public void setHost(String host) {
		this.host = host;
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

	public void setVmArgs(String[] vmArgs) {
		this.vmArgs = vmArgs;
	}

	public void setProgramArgs(String[] programArgs) {
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

	//mgarcia: Optiscom Res evolution
	public void setUserSelectedResources(List userSelectedResources) {
		this.userSelectedResources = userSelectedResources;
	}
	
	public List getUserSelectedResources() {
		return userSelectedResources;
	}

	public void setProject(IJavaProject project) {
		this.project = project;
	}

	public IJavaProject getProject() {
		return project;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setPort(String port) {
		this.portRMI=port;
		
	}

	@Override
	public void setUser(String user) {
		this.user=user;
		
	}

	@Override
	public boolean validateExecution() {
		OptsicomRemoteServer veex;
		try {
			veex = (OptsicomRemoteServer) Naming.lookup("//"+this.host+":"+this.portRMI+"/optsicom");
			if(veex != null) {
				executor = veex.getExecutor();
				if(this.executor != null) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} 
		
	}
	@Override
	public String getState(String idjob) {
		OptsicomRemoteServer veex;
		OptsicomRemoteExecutor executor;
		if (!host.isEmpty()){
			try {
				veex = (OptsicomRemoteServer) Naming.lookup("//"+host+":"+portRMI +"/optsicom");
				executor = veex.getExecutor();
				return executor.getState(idjob);
			} catch (Exception e) {
				RESClientPlugin.log(e);
				return "undetermined";
			}
		}
		else{
			return "undetermined";
		}
	}
	@Override
	public void getResultFromView(String workspace, String idjob) {
		OptsicomRemoteServer veex=null;
		if (!host.isEmpty()){
			
			try {
				veex = (OptsicomRemoteServer) Naming.lookup("//"+host+":"+portRMI+"/optsicom");
				executor = veex.getExecutor();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			getConsole(executor, idjob);
		}
	}
private void getConsole(final OptsicomRemoteExecutor executor,final String idjob) {
		
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

					
					int contador = 0;
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
					RESClientPlugin.log(e);
				}
			}
		}.start();	
	}
}
