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
package es.optsicom.res.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.eclipse.e4.core.di.annotations.Execute;

import es.optsicom.res.client.launcher.remote.IRemoteExecution;

/**
 * The activator class controls the plug-in life cycle
 */
public class RESClientPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "es.optsicom.res.client";

	// The shared instance
	private static RESClientPlugin plugin;
	private static BundleContext bundleContext;
	/**
	 * The constructor
	 */
	public RESClientPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		this.bundleContext=context;
		//this.getStateLocation()
		//Ruta del fichero
		//this.execute(Platform.getExtensionRegistry());

		//Guardamos el fichero
		String ruta = this.getStateLocation().toOSString() + File.separator+ "clientTruststore";
		File file = new File(ruta);
		System.setProperty("es.sidelab.optsicom.res.rutaTrabajo", ruta);
		file.createNewFile();
		FileOutputStream fos = new FileOutputStream(file);
		
		//RUTA DEL FICHERO DENTRO DEL JAR
		InputStream is = this.getClass().getResourceAsStream("/clientTruststore");
		BufferedInputStream bis = new BufferedInputStream(is);
		int leido;
		
		while ( (leido=bis.read()) != -1){
			fos.write(leido);
			fos.flush();
		}
		fos.close();

	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static String getTimeStamp() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String cadenaFecha = formato.format(calendar.getTime());
		
		return cadenaFecha;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static RESClientPlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static void log(String message) {
		getDefault().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
	}
	
	public static void log(Exception e) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}
	
	public static BundleContext getBundlecontext(){
		return bundleContext;
	}
}
