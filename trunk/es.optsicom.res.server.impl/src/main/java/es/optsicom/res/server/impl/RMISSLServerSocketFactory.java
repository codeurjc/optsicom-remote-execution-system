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


/*
 * @(#)RMISSLServerSocketFactory.java	1.5 01/05/10
 *
 * Copyright 1995-2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the following 
 * conditions are met:
 * 
 * -Redistributions of source code must retain the above copyright  
 * notice, this  list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduct the above copyright 
 * notice, this list of conditions and the following disclaimer in 
 * the documentation and/or other materials provided with the 
 * distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY 
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY 
 * DAMAGES OR LIABILITIES  SUFFERED BY LICENSEE AS A RESULT OF  OR 
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR 
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE 
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, 
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER 
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF 
 * THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that Software is not designed, licensed or 
 * intended for use in the design, construction, operation or 
 * maintenance of any nuclear facility. 
 */

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

public class RMISSLServerSocketFactory implements RMIServerSocketFactory, Serializable {

	private static final long serialVersionUID = -7960720636810393078L;

	public ServerSocket createServerSocket(int port) throws IOException { 
    	//Obtencion del directorio de trabajo
    	String ruta = System.getProperty("user.dir");
    	
		ruta = ruta.concat(File.separator+"serverKeystore");
		
    	System.setProperty("javax.net.ssl.keyStore",ruta);
		System.setProperty("javax.net.ssl.keyStorePassword","pepepe");
    	
    	ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
    	System.out.println("Puerto: " + port);
	    return ssf.createServerSocket(port);
	}
}
