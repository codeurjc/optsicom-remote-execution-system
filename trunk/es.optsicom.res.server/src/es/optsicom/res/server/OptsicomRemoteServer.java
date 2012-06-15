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

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * 
 * @author Carlos
 *
 */
public interface OptsicomRemoteServer extends Remote {

	OptsicomRemoteExecutor getExecutor() throws RemoteException;
	
}
