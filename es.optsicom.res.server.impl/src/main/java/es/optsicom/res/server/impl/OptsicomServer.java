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
package es.optsicom.res.server.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import es.optsicom.res.server.OptsicomRemoteExecutor;
import es.optsicom.res.server.OptsicomRemoteServer;
import es.optsicom.res.server.RMISSLClientSocketFactory;


public class OptsicomServer extends UnicastRemoteObject implements OptsicomRemoteServer {

	private static final long serialVersionUID = -8613203442881884342L;


	//Clase que guardara la informacion del proceso y un cerrojo para su fichero
	static class ProcessInfo implements Serializable{

		private static final long serialVersionUID = -1112154971418290234L;
		transient Process process;
		transient Lock lock;
		String jFile;
		String zFile;
		String command;
		String VMoptions;
		String programArgs;
		String host;
		String portRmi;
		//String portServer;
		String portDebug;
		String state;
		
		public ProcessInfo(Process process, Lock fileOutputLock,String password,String command,String host,
							String portRmi,/*String portServer,*/String portDebug,String vmarg,String prgarg,String jarName,String zipName) {
			this.process =  process;
			this.lock = fileOutputLock;
			this.jFile = jarName;
			this.command = command;
			this.VMoptions = vmarg;
			this.programArgs = prgarg;
			this.host = host;
			this.portRmi = portRmi;
			//this.portServer = portServer;
			this.portDebug = portDebug;
			this.state = "Running";
		}
	}
	
	private Map<String, ProcessInfo> register = new HashMap<String,ProcessInfo>();
	private String password = "9dabcd5f5f253d4721690820cf3b7777";
	private int objectPort; // The port used for objects
	private int executorPort;
	private File jvm;
	
	
	//Constructor de la clase
	public OptsicomServer(int port, int portExecutor, String jreBinFolder) throws RemoteException {
		
		super(port, new RMISSLClientSocketFactory(),
				 new RMISSLServerSocketFactory());

		this.objectPort = port;
		this.executorPort = portExecutor;
		this.jvm = new File(jreBinFolder, "java");
		if(!this.jvm.exists()) {
			throw new RemoteException("Couldn't find java: " + this.jvm.getAbsolutePath());
		}
		
		Executors.newFixedThreadPool(5);
		loadConfigurationFile();
	}
	
	/*
	 * Metodos privados del servidor
	 */
	
	private void loadConfigurationFile(){
		try {
			File f = new File("config.fobj");
			FileInputStream fis = new FileInputStream(f);
			ObjectInputStream ois = new ObjectInputStream(fis);
			this.register =(Map<String, ProcessInfo>)ois.readObject();
			ois.close();
		} catch (FileNotFoundException fnfe){
			setConfigurationFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setConfigurationFile(){		
		try {
			File f = new File("config.fobj");
			FileOutputStream fos = new FileOutputStream(f);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(register);
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/*
	 * Unico metodo remoto al que podra acceder el cliente
	 */
	@Override
	public OptsicomRemoteExecutor getExecutor() throws RemoteException {
		System.out.println("Serving executor...");
		return new OptsicomExecutor(this);
	}
	

	
	/*
	 * Metodos a los que podra llamar el ejecutor
	 */
	public boolean correctPassword(String pass) {
		String password = cypher(pass);
		
		return password.equals(this.password);

	}

	public void acquireLock(String idjob) {
		ProcessInfo p = getProcessInfo(idjob);
		p.lock.lock();
	}
	public void releaseLock(String idjob) {
		ProcessInfo p = getProcessInfo(idjob);
		p.lock.unlock();
	}
	
	public String getState(String id){
		return register.get(id).state;
	}
	
	public void setState(String id,String state){
		ProcessInfo p = getProcessInfo(id);
		p.state = state;
		setProcessInfo(p, id);
		setConfigurationFile();
	}
	
	public ProcessInfo getProcessInfo(String id) {
		ProcessInfo p = register.get(id);
		return p;
	}
	
	public void setProcessInfo(ProcessInfo pi,String id){
		register.put(id, pi);
	}
	
	
	public String setidJob() {
		//Asignacion del id de trabajo cuando comienza la ejecucion
		long time=System.currentTimeMillis();
		Date fecha=new Date(time);
		//mgarcia: Optiscom Res evolution 
		SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String cadenaFecha = formato.format(fecha);
		
		return cadenaFecha;
	}
	
	
	public String saveConsolefile(final Process p,final String password,final String command,final String host,
		final String portRmi,/*final String portServer,*/final String portDebug,final String vmargs,final String prgarg,
		final String jarName,final String zipName) {
		
		final String id = setidJob();
		
		final Lock cerrojo = new ReentrantLock();
		register.put(id, new ProcessInfo(p,cerrojo,password,command,host,portRmi,/*portServer,*/portDebug,vmargs,prgarg,jarName,zipName));
		setConfigurationFile();
		
		new Thread(){
			public void run(){
				
				try {
					//saveFile(p,password,host,port,vmargs,prgarg,jarName,zipName);
					File cFile = new File("console_"+id+".txt");
					String cadena= "";
					
					
					FileWriter fw = new FileWriter(cFile);
					PrintWriter pw = new PrintWriter(fw);
					pw.write(id+":\n");
					pw.flush();
					pw.close();
					Scanner in = new Scanner(p.getInputStream());
					while (in.hasNext()){
						fw = new FileWriter(cFile,true);
						pw = new PrintWriter(fw);
						cerrojo.lock();
						cadena = in.nextLine();
						pw.write(cadena+"\n");
						pw.flush();
						pw.close();
						
						cerrojo.unlock();
					}
					fw = new FileWriter(cFile,true);
					pw = new PrintWriter(fw);
					cerrojo.lock();
					pw.write("\nEjecucion finalizada correctamente\n");
					pw.flush();
					pw.close();
					cerrojo.unlock();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
		
		new Thread(){
			public void run(){
				
				try {
					//saveFile(p,password,host,port,vmargs,prgarg,jarName,zipName);
					File cFile = new File("console_"+id+".txt");
					String cadena= "";
					
					
					FileWriter fw = new FileWriter(cFile);
					PrintWriter pw = new PrintWriter(fw);
					pw.write(id+":\n");
					pw.flush();
					pw.close();
					Scanner in = new Scanner(p.getErrorStream());
					while (in.hasNext()){
						fw = new FileWriter(cFile,true);
						pw = new PrintWriter(fw);
						cerrojo.lock();
						cadena = in.nextLine();
						pw.write(cadena+"\n");
						pw.flush();
						pw.close();
						
						cerrojo.unlock();
					}
					fw = new FileWriter(cFile,true);
					pw = new PrintWriter(fw);
					cerrojo.lock();
					pw.write("\nEjecucion finalizada correctamente\n");
					pw.flush();
					pw.close();
					cerrojo.unlock();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

		return id;
	}
	
	public int getObjectPort() {
		return this.objectPort;
	}

	public int getExecutorPort() {
		return executorPort;
	}
	
	/*
	 * Metodo main del servidor
	 */
	public static void main(String[] args) {
		try {
			//mgarcia: Optiscom Res evolution
			String pass="adminserver";
			int portServer=0;
			int portRmi=2002;
			int portExecutor = 0;
			String host="127.0.0.1";
			String jreBinFolder = "";
			
			//mgarcia: Optiscom Res evolution
			Properties configFile = new Properties();
			
			String configServer = System.getProperty("user.dir");
	    	configServer = configServer.concat(File.separator+"configserver.properties");
			
			InputStream is = new FileInputStream(configServer);

			configFile.load(is);

			if(!configFile.getProperty("password").isEmpty()){
				pass = configFile.getProperty("password");
			} else {
				System.out.println("Check configserver.properties");
				System.out.println("password value is empty");
				System.exit(-1);
			}
			if(!configFile.getProperty("host").isEmpty()){
				host = configFile.getProperty("host");
			} else {
				System.out.println("Check configserver.properties");
				System.out.println("host value is empty");
				System.exit(-1);
			}
			if(!configFile.getProperty("portRmi").isEmpty()){
				portRmi = Integer.parseInt(configFile.getProperty("portRmi"));
			} else {
				System.out.println("Check configserver.properties");
				System.out.println("portRmi value is empty");
				System.exit(-1);
			}
			if(!configFile.getProperty("portServer").isEmpty()){
				portServer = Integer.parseInt(configFile.getProperty("portServer"));
			} else {
				System.out.println("Check configserver.properties");
				System.out.println("portServer value is empty");
				System.exit(-1);
			}
			if(!configFile.getProperty("portExecutor").isEmpty()){
				portExecutor = Integer.parseInt(configFile.getProperty("portExecutor"));
			} else {
				System.out.println("Check configserver.properties");
				System.out.println("portExecutor value is empty");
				System.exit(-1);
			}
			if(!configFile.getProperty("jreBinFolder").isEmpty()){
				jreBinFolder = configFile.getProperty("jreBinFolder");
			} else {
				System.out.println("Check configserver.properties");
				System.out.println("jreBinFolder value is empty");
				System.exit(-1);
			}
						
			// We change the password
			String cypheredPass = cypher(pass);
			File passwdFile = new File(".auth");
			if(passwdFile.exists()) {
				// Make a backup
				passwdFile.renameTo(new File(".auth_bak"));
				passwdFile.delete();
			}
			if(passwdFile.createNewFile()) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(passwdFile));
				bw.append(cypheredPass);
				bw.close();
				passwdFile.setReadable(false, false);
				passwdFile.setReadable(true, true);
				passwdFile.setWritable(false, false);
			} else {
				System.out.println("Couldn't create file: check permissions");
				System.exit(-1);
			}
			
			System.getProperties().put("java.rmi.server.hostname", host);
			
			// Read password if there is a file
			pass = null;
			if(passwdFile.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(passwdFile));
				pass = br.readLine();
			}
			
			OptsicomServer server = new OptsicomServer(portServer, portExecutor, jreBinFolder);
			if(pass != null) {
				server.setPasswd(pass);
			}
				
			LocateRegistry.createRegistry(portRmi);
			Registry registry = LocateRegistry.getRegistry("127.0.0.1",portRmi);	
			registry.rebind("optsicom",server);
			System.out.println("Server bound in registry");
		//mgarcia: Optiscom Res evolution
		} catch (FileNotFoundException e) {
			System.out.println("Server err: configserver.properties doesn't exist");
		    e.printStackTrace();
		} catch (NullPointerException e) {
			System.out.println("Server err: Check configserver.properties");
			System.out.println("Server err: Wrong number of properties");
			System.out.println("Server err: <password> <host> <portRmi> <portServer> <portExecutor> <jreBinFolder>");
			e.printStackTrace();
		} catch (Exception e) {
		    System.out.println("Server err: " + e.getMessage());
		    e.printStackTrace();
		}
	}

	private void setPasswd(String pass) {
		this.password = pass;
	}

	private static String cypher(String pass) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			
			byte[] digest = md.digest(pass.getBytes());
            
			StringBuffer buf = new StringBuffer();
			for (byte b: digest){
				buf.append(String.format("%02x",b));
			}

			return buf.toString();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getJavaProgram() {
		return jvm.getAbsolutePath();
	}
	

}
