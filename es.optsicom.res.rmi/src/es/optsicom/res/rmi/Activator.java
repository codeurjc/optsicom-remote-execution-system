package es.optsicom.res.rmi;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import es.optsicom.res.client.launcher.remote.IRemoteExecution;
import es.optsicom.res.rmi.launcher.remote.RMIRemoteExecution;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "es.optsicom.res.rmi"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	private IRemoteExecution execution;
	private ServiceRegistration<?> registration;

	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		registration = context.registerService(
				IRemoteExecution.class.getName(), 
				new RMIRemoteExecution(), 
				null);
		
		/*ServiceReference reference = context
			        .getServiceReference(IRemoteExecution.class.getName());
		  execution = (IRemoteExecution) context.getService(reference);	   */
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		registration.unregister();
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
}
