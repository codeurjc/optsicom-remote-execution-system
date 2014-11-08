package es.optsicom.res.ssh.launcher.remote;

import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;











import javax.swing.JOptionPane;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.jcraft.jsch.*;

import es.optsicom.res.client.RESClientPlugin;
import es.optsicom.res.client.launcher.remote.IRemoteExecution;
import es.optsicom.res.client.util.ProjectDependenciesResolver;
import es.optsicom.res.server.OptsicomRemoteExecutor;
import es.optsicom.res.ssh.session.UserSessionInfo;


public class SSHRemoteExecution implements IRemoteExecution {
	
	private static final String FOLDERNAME="optsicom-res";
	private Session session;
	String name="SSH";
	private boolean connected;
	private String host;
	private int port;
	private String portDebug;
	private String password;
	private String mainClass;
	private String[] vmArgs;
	private String[] programArgs;
	private String zipName;
	private String mode;
	private String user;
	private ProjectDependenciesResolver resolver;
	private List userSelectedResources;
	private IJavaProject project;
	private String serverProjectPath;
	private static final String resultFile="resultFile.txt";
	private boolean isOutput;
	public SSHRemoteExecution(){
		
	}
	
	@Override
	public IStatus run(IProgressMonitor monitor) {	
		
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask("Launching", 5);
		connect();
		subMonitor.subTask("Connecting to server");
		if (this.session!=null){
			subMonitor.subTask("Creating project folders");
			String opsticomFolder="/home/"+this.user+"/"+this.FOLDERNAME;
			this.executeCommand("mkdir "+opsticomFolder);
			
					
			DateFormat hourFormat = new SimpleDateFormat("HH-mm-ss");
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Date date = new Date();
			
			String serverProjectFolder= this.project.getElementName()+"_"+dateFormat.format(date)+
					"_"+hourFormat.format(date);
			
			this.serverProjectPath=opsticomFolder+"/"+serverProjectFolder;
			this.executeCommand("mkdir "+serverProjectPath);
			
			//Enviamos el proeycto	
			subMonitor.subTask("Sending project");
			send(subMonitor);
			
			this.executeCommand("find  -maxdepth 1 -type f  > "+this.serverProjectPath+"/initalDirectories.txt");
			
			subMonitor.subTask("Executing project");
			//Buscamos los archivos java los almacenamos y los compilamos guardando la dirección
			String expressionGetJavaSource="find "+serverProjectPath+" -name \"*.java\" > "+serverProjectPath+"/javafiles.txt";
			executeCommand(expressionGetJavaSource);
			
			
			String expressionCompileJava="javac @"+serverProjectPath+"/javafiles.txt";
			executeCommand(expressionCompileJava);
			
			
			
			String mainClassPath=getMainClassPath(serverProjectPath);
			
			mainClassPath=mainClassPath.replaceAll(mainClass+".java", "");
			
			String idjob=dateFormat.format(date)+
					"_"+hourFormat.format(date);
			
			ScopedPreferenceStore sps =  (ScopedPreferenceStore) RESClientPlugin.getDefault().getPreferenceStore();
			sps.putValue(idjob,host+":"+port);
			
			executeProject(mainClassPath);
			
			this.executeCommand("find  -maxdepth 1 -type f  > "+this.serverProjectPath+"/finalDirectories.txt");
			this.executeCommand("diff "+this.serverProjectPath+"/initalDirectories.txt  "+this.serverProjectPath+"/finalDirectories.txt | grep -v \"^---\" | grep -v \"^[0-9c0-9]\" > "+this.serverProjectPath+"/outputFiles.txt");
			
			subMonitor.subTask("Geetting output files");
			this.isOutput=this.sendOutputFiles();
			subMonitor.subTask("Opening console");
			this.openConsole(idjob);
		}
		else{
			RESClientPlugin.log("Authentication failed");
		}
		this.session.disconnect();
		return new Status(IStatus.OK, RESClientPlugin.PLUGIN_ID, "");
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

	

	public void send(SubMonitor monitor){
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		String workSpaceRoot = ws.getRoot().getLocation().toOSString();
		StringBuffer executionResultsPath = new StringBuffer();
		executionResultsPath.append(workSpaceRoot);
		executionResultsPath.append(File.separator);
		executionResultsPath.append(this.project.getElementName());
		
		String expressionSCP="scp -r "+executionResultsPath+" "+user+"@"+host+": "+serverProjectPath;
		executeCommand(expressionSCP);
		//Enviamos los archivos adicionales	(creando las carpetas necesarias)	
		if(userSelectedResources != null) { 
			for(Object o : userSelectedResources) {
				IResource resource = (IResource) o;
				String absoluteResourcePath= resource.getLocation().toOSString();
				String relativeResourcePath= absoluteResourcePath.replaceAll(workSpaceRoot, "");
				
				if(relativeResourcePath.lastIndexOf('/')!=-1){
					relativeResourcePath=relativeResourcePath.substring(0, relativeResourcePath.lastIndexOf('/'));
				}
				String mkdirRelativePath="mkdir -p "+serverProjectPath+relativeResourcePath;
				executeCommand(mkdirRelativePath);
				String expressionSCPresource="scp -r "+absoluteResourcePath+" "+user+"@"+host+": "+serverProjectPath+relativeResourcePath;
				executeCommand(expressionSCPresource);
			}
		}
	}
	
	public void openConsole(String idjob) {
		ChannelExec channelExec;
		try {
			MessageConsole miconsola;
			ConsolePlugin plugin;
			IConsoleManager cm;
			final IOConsoleOutputStream cos;
			miconsola = new MessageConsole("Consola Optsicom RES", null);
			miconsola.activate();
			plugin = ConsolePlugin.getDefault();
			cm = plugin.getConsoleManager();
			cm.addConsoles(new IConsole[]{miconsola});
			cos = miconsola.newOutputStream();
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			String workSpaceRoot = ws.getRoot().getLocation().toOSString();
			StringBuffer executionResultsPath = new StringBuffer();
			executionResultsPath.append(workSpaceRoot);
			executionResultsPath.append(File.separator);
			executionResultsPath.append(this.project.getElementName());
			File folder = new File(executionResultsPath+"/results");
			folder.mkdirs();
			this.executeCommand("scp -r "+user+"@"+host+": "+serverProjectPath+"/"+resultFile+" "+executionResultsPath+"/results");
			File localResultFile= new File (executionResultsPath+"/results/"+resultFile);
			FileReader fr= new FileReader(localResultFile);
			BufferedReader bf= new BufferedReader(fr);
			String line;
			while((line=bf.readLine())!=null){
				cos.write(line+"\n");
				cos.flush();
			}
			cos.write("Finished\n");
			cos.flush();
			if (this.isOutput){
				cos.write("There are some output files in : "+serverProjectPath+" \n");
					cos.flush();
			}
			cos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}     
		
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
		try { 
			this.port=Integer.parseInt(port);
	    } catch(NumberFormatException e) { 
	        this.port=-1;
	    }				
	}

	@Override
	public void setUser(String user) {
		this.user=user;
	}
	
	private void connect(){
		JSch jSSH = new JSch();
		try {
			this.session = jSSH.getSession(this.user, this.host, this.port);
			UserInfo userInfo = new UserSessionInfo(this.password, null);
	        this.session.setUserInfo(userInfo);        
	        this.session.setPassword(this.password);
	        this.session.connect();
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	}
	
	private String getMainClassPath(String serverProjectPath){
			
		String[] shellOutputs=this.executeCommand("grep "+this.mainClass+".java "+serverProjectPath+"/javafiles.txt");		
		if(shellOutputs!=null){
			return shellOutputs[0];
		}
		return null;
	
	}
	
	private void executeProject(String mainClassPath){
		//Pasamos los argumentos a string para ejecutarlos.
		String args="";
		for(String arg : this.programArgs) {
			args+=arg+" ";
		}
		this.executeCommand("touch "+serverProjectPath+"/"+resultFile);
		//Inicializamos el fichero por si contiene algo
		this.executeCommand("echo  > "+serverProjectPath+"/"+resultFile);
		String[] outputShell=this.executeCommand("java -cp "+mainClassPath+" "+mainClass+" "+args+ " >> "+serverProjectPath+"/"+resultFile);
	}

	private boolean sendOutputFiles(){
		boolean isOutput=false;
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		String workSpaceRoot = ws.getRoot().getLocation().toOSString();
		StringBuffer executionResultsPath = new StringBuffer();
		executionResultsPath.append(workSpaceRoot);
		executionResultsPath.append(File.separator);
		executionResultsPath.append(this.project.getElementName());

		String[] outputShell=this.executeCommand("grep \"\" "+this.serverProjectPath+"/outputFiles.txt");
		for(int i=0; i<outputShell.length;i++){
			isOutput=true;
			String path=outputShell[i].substring(2, outputShell[i].length());
			this.executeCommand("scp -r "+user+"@"+host+": "+path+" "+executionResultsPath);
			this.executeCommand("rm -rf "+path);
		}			
		return isOutput;
	}

	private String[] executeCommand(String command){
		ChannelExec channelExec;
		String  shellOutput=null;
		List<String> lines=null;
		try {
			channelExec = (ChannelExec)this.session.openChannel("exec");
			
			InputStream in = channelExec.getInputStream();
			channelExec.setCommand(command);
			channelExec.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			lines=new ArrayList<String>();
			shellOutput="";
			 while(true){
	               if(channelExec.isClosed()){ 
	            	   while((line=reader.readLine())!= null){
		   	        	   lines.add(line);
	            	   }
	            	   break;
	               }
	               try{Thread.sleep(50);}catch(Exception ee){}
	            }
			channelExec.disconnect();
	        
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		return (lines==null)?null:lines.toArray(new String[lines.size()]);
    }
	
	@Override
	public boolean validateExecution() {
		this.connect();
		return this.session!=null;
	}

	@Override
	public void getResultingFile(String idjob) {
		sendOutputFiles();
	}

	@Override
	public String getZipName() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
