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
	private static final String LOGFILE="/log.txt";
	private Session session;
	private String name="SSH";
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
	private StringBuffer executionResultsPath;
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
			
			executeCommand("echo name:"+this.project.getElementName()+">>"+serverProjectPath+LOGFILE);
			executeCommand("echo date:"+dateFormat.format(date)+
					"/"+hourFormat.format(date)+">>"+serverProjectPath+LOGFILE);
			executeCommand("echo status:sent "+">>"+serverProjectPath+LOGFILE);
			
			
			
			subMonitor.subTask("Executing project");
			//Buscamos los archivos java los almacenamos y los compilamos guardando la dirección
			String expressionGetJavaSource="find "+serverProjectPath+" -name \"*.java\" > "+serverProjectPath+"/javafiles.txt";
			executeCommand(expressionGetJavaSource);
			
			
			String expressionCompileJava="javac @"+serverProjectPath+"/javafiles.txt";
			executeCommand(expressionCompileJava);
			executeCommand("sed -i 's/status:[a-z]*/status:compiled/' "+serverProjectPath+LOGFILE);
			

			
			String mainClassPath=getMainClassPath(serverProjectPath);
			
			mainClassPath=mainClassPath.replaceAll(mainClass+".java", "");
			executeCommand("echo mainclass:"+mainClassPath+mainClass+"  "+">>"+serverProjectPath+LOGFILE);
			String idjob=serverProjectFolder;
			
			ScopedPreferenceStore sps =  (ScopedPreferenceStore) RESClientPlugin.getDefault().getPreferenceStore();
			sps.putValue(idjob,name+":"+host+":"+port+":"+user+":"+password);
			executeCommand("sed -i 's/status:[a-z]*/status:executing/' "+serverProjectPath+LOGFILE);
			
			
			//Creamos los ficheros necesarios antes de realizar el control de los ficheros de salida.
			executeCommand("touch "+serverProjectPath+LOGFILE);
			executeCommand("touch "+serverProjectPath+"/"+resultFile);
			executeCommand("touch "+serverProjectPath+"/finalDirectories.txt");
			
			executeCommand("find "+this.serverProjectPath+"  -maxdepth 1 -type f  > "+this.serverProjectPath+"/initialDirectories.txt");
			executeProject(mainClassPath);
			executeCommand("find "+this.serverProjectPath+"  -maxdepth 1 -type f  > "+this.serverProjectPath+"/finalDirectories.txt");
			
			executeCommand("diff "+this.serverProjectPath+"/initialDirectories.txt  "+this.serverProjectPath+"/finalDirectories.txt"
					+ " | grep -v \"^---\" | grep -v \"^[0-9c0-9]\" "
					+ "> "+this.serverProjectPath+"/outputFiles.txt");
			executeCommand("echo output files: >> "+serverProjectPath+LOGFILE);
			executeCommand("cat "+this.serverProjectPath+"/outputFiles.txt"+" >> "+serverProjectPath+LOGFILE);
			
			
			//Obtenemos los resultados y los enviamos a la carpeta results.
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			String workSpaceRoot = ws.getRoot().getLocation().toOSString();
			executionResultsPath = new StringBuffer();
			executionResultsPath.append(workSpaceRoot);
			executionResultsPath.append(File.separator);
			executionResultsPath.append(this.project.getElementName());
			File folder = new File(executionResultsPath+"/results");
			folder.mkdirs();
			
			subMonitor.subTask("Getting output files");
			this.isOutput=this.sendOutputFiles();
			executeCommand("sed -i 's/status:[a-z]*/status:finished/' "+serverProjectPath+LOGFILE);
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
		StringBuffer projectPath = new StringBuffer();
		projectPath.append(workSpaceRoot);
		projectPath.append(File.separator);
		projectPath.append(this.project.getElementName());
		
		String expressionSCP="scp -r "+projectPath+" "+user+"@"+host+": "+serverProjectPath;
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
				cos.write("There are some output files in : "+executionResultsPath+"/results \n");
					cos.flush();
			}
			cos.close();
			//Eliminamos los archivos del servidor.
			executeCommand("rm -rf "+this.serverProjectPath+"/initialDirectories.txt "+
					serverProjectPath+"/finalDirectories.txt "+
					serverProjectPath+"/javafiles.txt "+
					serverProjectPath+"/"+this.project.getElementName());
			
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
		
		//Inicializamos el fichero por si contiene algo
		executeCommand("echo  > "+serverProjectPath+"/"+resultFile);
		String[] outputShell=this.executeCommand("cd "+serverProjectPath+" && java -cp "+mainClassPath+" "+mainClass+" "+args+ " >> "+serverProjectPath+"/"+resultFile);
	}

	private boolean sendOutputFiles(){
		boolean isOutput=false;
		
		String[] outputShell=this.executeCommand("grep \"\" "+this.serverProjectPath+"/outputFiles.txt");
		for(int i=0; i<outputShell.length;i++){
			isOutput=true;
			String path=outputShell[i].substring(2, outputShell[i].length());
			this.executeCommand("scp -r "+user+"@"+host+": "+path+" "+executionResultsPath+"/results");
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

	@Override
	public String getState(String idjob){
		connect();
		String opsticomFolder="/home/"+this.user+"/"+this.FOLDERNAME;
		String logPath=opsticomFolder+"/"+idjob;
		String [] output=executeCommand("grep 'status:[a-z]*' "+logPath+LOGFILE);
		String status="undetermined";
		if (output.length>0){
			status=output[0];
			String[] tokens= status.split(":");
			if (tokens.length>1){
				status=tokens[1];
			}
			else{
				
			}
		}
		else{
			
		}
		return status;
	}

	@Override
	public void getResultFromView(String workspace, String idjob) {
		connect();
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
			
			//Obtenemos el nombre del proyecto
			String opsticomFolder="/home/"+this.user+"/"+this.FOLDERNAME;
			String logPath=opsticomFolder+"/"+idjob;
			String [] output=executeCommand("grep 'name:[a-z]*' "+logPath+LOGFILE);
			String name="undetermined";
			if (output.length>0){
				name=output[0];
				String[] tokens= name.split(":");
				if (tokens.length>1){
					name=tokens[1];
				}
				else{
					
				}
			}
			else{
				
			}
			
			File folder = new File(workspace+"/"+name+"/results");
			folder.mkdirs();
			
			serverProjectPath="/home/"+this.user+"/"+this.FOLDERNAME+"/"+idjob;
			//Enviamos los archivos de salida
			String[] outputShell=this.executeCommand("grep \"\" "+serverProjectPath+"/outputFiles.txt");
			for(int i=0; i<outputShell.length;i++){
				isOutput=true;
				String path=outputShell[i].substring(2, outputShell[i].length());
				this.executeCommand("scp -r "+user+"@"+host+": "+path+" "+workspace+"/"+name+"/results");
			}
			
			this.executeCommand("scp -r "+user+"@"+host+": "+serverProjectPath+"/"+resultFile+" "+workspace+"/"+name+"/results");
			File localResultFile= new File (workspace+"/"+name+"/results/"+resultFile);
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
				cos.write("There are some output files in : "+workspace+"/"+name+"/results \n");
					cos.flush();
			}
			cos.close();
			
			//Eliminamos los archivos del servidor.
			executeCommand("rm -rf "+this.serverProjectPath+"/initialDirectories.txt "+
					serverProjectPath+"/finalDirectories.txt "+
					serverProjectPath+"/javafiles.txt "+
					serverProjectPath+"/"+this.project.getElementName());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}     
	}

	
}
