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

import java.io.*;
import java.net.*;
import java.rmi.server.*;
import javax.net.ssl.*;


public class RMISSLClientSocketFactory
	implements RMIClientSocketFactory, Serializable {

	private static final long serialVersionUID = 2006139466485209709L;

	public Socket createSocket(String host, int port) throws IOException {

		//Obtencion del directorio de trabajo
    	String ruta = System.getProperty("es.sidelab.optsicom.res.rutaTrabajo");

    	//String ruta = "/home/charly/workspace/es.sidelab.optsicom.res.client/clientTruststore";
    	System.setProperty("javax.net.ssl.trustStore",ruta);
	    SSLSocketFactory factory =
		(SSLSocketFactory)SSLSocketFactory.getDefault();
	    SSLSocket socket = (SSLSocket)factory.createSocket(host, port);
	    return socket;
	}
}
