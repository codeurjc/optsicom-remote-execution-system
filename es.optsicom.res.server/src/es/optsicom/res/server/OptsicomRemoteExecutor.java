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
package es.optsicom.res.server;
import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


public interface OptsicomRemoteExecutor extends Remote {

	public boolean authenticate(String password) throws RemoteException;
	public void acquireLock(String idjob) throws RemoteException;
	public void releaseLock(String idjob) throws RemoteException;
	public void setZip(File zipFile,PaqueteDatos pd,boolean create) throws RemoteException;
	public void setJarDirs(List<String> librerias) throws RemoteException;
	public void setWorkingDir(String dir) throws RemoteException;
	public String launch(String mode,String password,String host,String portRmi,/*String portServer,*/String portDebug,
			String vmarg,String prgarg,String mainClass,String zipName) throws RemoteException;
	public String readConsole(String idjob) throws RemoteException;
	public String getState(String id) throws RemoteException;
	public void setState(String id,String state) throws RemoteException;
	public boolean hasProcessFinished(String id) throws RemoteException;
	
}
