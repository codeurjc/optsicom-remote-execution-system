package es.optsicom.res.client.launcher.remote.delegate;

import org.eclipse.jdt.internal.launching.LaunchingPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

@SuppressWarnings("restriction")
public interface IJavaRemoteServerConfigurationConstants extends IJavaLaunchConfigurationConstants {
	
	
	public static final String ATTR_REMOTE_SERVER = LaunchingPlugin.getUniqueIdentifier() + ".remoteServer";
	
	public static final String ATTR_PORT_RMI = LaunchingPlugin.getUniqueIdentifier() + ".portRmi";
	
	public static final String ATTR_PASSWORD = LaunchingPlugin.getUniqueIdentifier() + ".password";
	
	public static final String ATTR_PORT_DEBUG = LaunchingPlugin.getUniqueIdentifier() + ".portDebug";
	
	public static final String ATTR_SELECTED_RESOURCES = LaunchingPlugin.getUniqueIdentifier() + ".selectedResources"; 
	
	public static final String ATTR_DEBUG_CONFIGURATION = LaunchingPlugin.getUniqueIdentifier() + ".debugConfiguration"; 

	public static final String ATTR_CONNECTION_TYPE = LaunchingPlugin.getUniqueIdentifier() + ".connectionType"; 


}
