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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.sound.midi.Patch;

import es.optsicom.res.server.OptsicomRemoteExecutor;
import es.optsicom.res.server.PaqueteDatos;

public class OptsicomExecutor extends UnicastRemoteObject implements OptsicomRemoteExecutor, Serializable {

	private transient OptsicomServer server;
	private long offset;
	private List<String> librerias = new ArrayList<String>();
	private String workingDir;
	

	//Constructor de la clase
	public OptsicomExecutor(OptsicomServer veexServer) throws RemoteException {
		super(veexServer.getExecutorPort());
		this.server = veexServer;
		this.offset = 0;
	}
	
	/*
	 * Metodos privados del executor
	 */
	public int descomprimirZip(String zipName,String zipDir){
		File directorio = new File(zipDir);
		try {
			directorio.mkdir();
			ZipFile zipFile = new ZipFile(zipName);
			Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();
			ZipEntry entry;
			
			while(entries.hasMoreElements()){
				entry = entries.nextElement();
				File fichero = new File(directorio,entry.getName());
				System.out.println("La ruta es: "+fichero.getAbsolutePath());
				
				File padre = fichero.getParentFile();
				if (!padre.exists()) {
					padre.mkdirs();
				}
				
				fichero.createNewFile();
				
				FileOutputStream fos = new FileOutputStream(fichero);
				InputStream is = zipFile.getInputStream(entry);
				copyInputStream(is, fos);
				fos.close();
				is.close();
			}
			return 0;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}	
	}
	
	private final void copyInputStream(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int len;

	    while((len = in.read(buffer)) >= 0) {
	      out.write(buffer, 0, len);
	    }

	    in.close();
	    out.close();
	}

	/*
	 * Metodos remotos
	 */
	
	//Metodo que autentica el acceso
	public boolean authenticate(String pass) throws RemoteException {
		return server.correctPassword(pass);
	}
	
	//Metodo que coje el cerrojo
	public void acquireLock(String idjob) throws RemoteException{
		this.server.acquireLock(idjob);
	}
	//Metodo que libera el cerrojo
	public void releaseLock(String idjob) throws RemoteException{
		this.server.releaseLock(idjob);
	}

	//Metodo que almacena el fichero .zip en el servidor
	public void setZip(File zipFile,PaqueteDatos pd,boolean create) throws RemoteException {
		try {
			OutputStream os;
			if (create){
				os = new FileOutputStream(zipFile.getName());
			}
			else{
				os = new FileOutputStream(zipFile.getName(),true);
			}
			
			if( pd.getLongitud() > 0){
				os.write(pd.getBuf(),0,pd.getLongitud());
				os.flush();
			}
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Metodo para obtener las dependencias de los .jar
	public void setJarDirs(List<String> lib){
		this.librerias = lib;
	}
	
	@Override
	public void setWorkingDir(String dir) throws RemoteException {
		this.workingDir = dir;
	}
	
	private String getJarDirs(String zipdir){
		StringBuilder cadena=new StringBuilder();
		String sistemaOperativo = System.getProperty("os.name");
		String separator = null;
		if (sistemaOperativo.contains("Windows")){
			separator = ";";
		}
		else{
			separator = ":";
		}
		
		for (String lib: librerias){
			// We remove the initial separator if exists
			String cleanLib = lib.startsWith(File.separator) ? lib.substring(1) : lib;
			// zipdir already finishes with a File.separator
			cadena.append(separator + zipdir + cleanLib);
		}
		return cadena.toString();
	}
	
	
	//Metodo para lanzar el jar
	public String launch(String mode,String password,String host,String portRmi,/*String portServer,*/String portDebug,
			String vmargs,String prgarg,String mainClass,String zipName) throws RemoteException {
		try {
			/*
			 * DESCOMPRIMIMOS EL ZIP Y EJECUTAMOS
			 */
			
			//Obtencion del directorio de trabajo
	    	String ruta = System.getProperty("user.dir");
			zipName = ruta.concat(File.separator+zipName);
			int indice = zipName.lastIndexOf(".");
			String zipdir = zipName.substring(0,indice)+File.separator;
			
			descomprimirZip(zipName,zipdir);
			String cp = getJarDirs(zipdir);
			String command=null;
			
			Process p = null;
			String javaProgram = server.getJavaProgram();
			String[] argumentos = null;
			if (vmargs == null || "".equals(vmargs)){
				if ("debug".equals(mode)) {
					argumentos=new String[] {javaProgram,"-Xdebug","-Xrunjdwp:transport=dt_socket,address="+portDebug+",server=y",
						"-cp",cp,mainClass};
				} else {
					argumentos=new String[] {javaProgram,"-cp",cp,mainClass};
				}
				System.out.println("No hay vmargs:"+vmargs);
			} else {
				if ("debug".equals(mode)) {
					argumentos=new String[] {javaProgram,"-Xdebug","-Xrunjdwp:transport=dt_socket,address="+portDebug+",server=y",
						vmargs,"-cp",cp,mainClass};
				} else {
					argumentos=new String[] {javaProgram,vmargs,"-cp",cp,mainClass};
				}
				System.out.println("Si hay: "+vmargs);
				
			}
			System.out.println("args: "+Arrays.toString(argumentos));
			p = Runtime.getRuntime().exec(argumentos, null, new File(zipdir, workingDir));
			
			return server.saveConsolefile(p,password,command,host,portRmi,/*portServer,*/portDebug,vmargs,prgarg,mainClass,zipName);			
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	//Metodo que lee el fichero de consola del proceso
	public String readConsole(String idjob) throws RemoteException {
		String cadena="";
		File f = new File("console_"+idjob+".txt");
		
		try {
			RandomAccessFile raf = new RandomAccessFile(f,"r");
			raf.seek(offset);
			cadena = raf.readLine();
			long fp = raf.getFilePointer();
			offset = fp;
			raf.close();
		} catch (Exception e) {

			return null;
		}
		return cadena;
	}
	
	//Metodo para obtener el estado de una tarea
	public String getState(String id){
		return server.getState(id);
	}
	//Metodo para actualizar el estado de una tarea
	public void setState(String id,String state){
		server.setState(id, state);
	}
	
	//Metodo para comprobar si una tarea ha terminado
	public boolean hasProcessFinished(String id) {
		Process p = server.getProcessInfo(id).process;
		if (p != null){
			try{
				//Si devuelve un valor de salida es que ha terminado
				p.exitValue();
				return true;
			}
			//Si se produce una excepcion es que no ha terminado
			catch(Exception e){
				return false;
			}
		}
		else{
			try {
				Process p1 = Runtime.getRuntime().exec("ps -ef | grep "+server.getProcessInfo(id).command);
				InputStreamReader isr = new InputStreamReader(p1.getInputStream());
				BufferedReader br = new BufferedReader(isr);
				int contador = 0;
				
				while ( (br.readLine())!= null ){
					contador++;
				}
				return contador != 2;
				
			} catch (Exception e) {
				return true;
			}	
		}
	}	
}
