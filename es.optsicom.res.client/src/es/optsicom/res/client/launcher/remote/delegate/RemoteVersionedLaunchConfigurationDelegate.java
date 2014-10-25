package es.optsicom.res.client.launcher.remote.delegate;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;

import es.optsicom.res.client.RESClientPlugin;

@SuppressWarnings("restriction")
public class RemoteVersionedLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	protected static final String EMPTY_STRING = "";

	private String user;
	private String host;
	private String port;
	private String portDebug;
	private String password;
	private String mainClass;
	private String[] vmArgs;
	private String[] programArgs;
	private String connectionType;
	@SuppressWarnings("rawtypes")
	private List userSelectedResources;
	private List selectedResourcesString;
	private IJavaProject project;

	

	
	protected static final Map<String,String> EMPTY_MAP = new HashMap<String,String>();
	
	
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {

		if (mode.equals("debug")){
			
			if (configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_DEBUG_CONFIGURATION, false)) {
	
				if (monitor == null) {
					monitor = new NullProgressMonitor();
				}
				
				ILaunchConfigurationWorkingCopy config = configuration.getWorkingCopy();
				monitor.beginTask("Launch debug configuration",3);
				config.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_DEBUG_CONFIGURATION, false);
				config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, JavaRuntime.getDefaultVMConnector().getIdentifier());
			    configuration = config.doSave();
			    
			    user = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_USER,EMPTY_STRING);			
				host = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_REMOTE_SERVER,EMPTY_STRING);
				port = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PORT,EMPTY_STRING);
				connectionType = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_CONNECTION_TYPE,EMPTY_STRING);
				password = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PASSWORD,EMPTY_STRING);
				portDebug = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PORT_DEBUG,EMPTY_STRING);
								
				mainClass = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,EMPTY_STRING);
				project = getJavaProject(configuration);
	
				if ((getVMArguments(configuration) != null)	&& (!getVMArguments(configuration).equalsIgnoreCase(""))) {
					vmArgs = getArguments(getVMArguments(configuration));
				}
				if ((getProgramArguments(configuration) != null) && (!getProgramArguments(configuration).equalsIgnoreCase(""))) {
					programArgs = getArguments(getProgramArguments(configuration));
				}
	
				selectedResourcesString = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_SELECTED_RESOURCES,	selectedResourcesString);
				
				if(selectedResourcesString.size()>0){
					int index = 0;
					userSelectedResources = new ArrayList();
					while(index<selectedResourcesString.size()){
						IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(selectedResourcesString.get(index).toString());
						userSelectedResources.add(index, resource);
						index++;
					}
				}
				
				monitor.subTask("Launch remote versioned configuration");
				
				RemoteVersionedLaunchConfiguration remoteLaunch = new RemoteVersionedLaunchConfiguration();
				remoteLaunch.launch(configuration, mode, launch, monitor, host, port, portDebug, connectionType,user,password, mainClass, vmArgs, programArgs, userSelectedResources, project);
				
				if (monitor.isCanceled()) {
					RESClientPlugin.log("Remote versioned launch configuration canceled");
					return;
				}
				
				monitor.done();
				
			} else {
				
				if (monitor == null) {
					monitor = new NullProgressMonitor();
				}
				
				monitor.beginTask("Debug configuration",3);
				
				ILaunchConfigurationWorkingCopy config = configuration.getWorkingCopy();
				config.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_DEBUG_CONFIGURATION, true);
				config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, JavaRuntime.getDefaultVMConnector().getIdentifier());
			    configuration = config.doSave();
			    
			    monitor.subTask("Debug remote versioned configuration");
			    
			    JavaRemoteApplicationLaunchConfigurationDelegate applicactionDelegate = new JavaRemoteApplicationLaunchConfigurationDelegate();
			    applicactionDelegate.launch(configuration, mode, launch, monitor);
	
			    if (monitor.isCanceled()) {
					RESClientPlugin.log("Remote versioned debug configuration canceled");
					return;
				}
			    
			    monitor.done();
			}
		
		} else {
			

			if (monitor == null) {
				monitor = new NullProgressMonitor();
			}
			
			ILaunchConfigurationWorkingCopy config = configuration.getWorkingCopy();
			monitor.beginTask("Launch run configuration",3);
			config.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_DEBUG_CONFIGURATION, true);
			
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, JavaRuntime.getDefaultVMConnector().getIdentifier());
		    configuration = config.doSave();
		    
		    
		    user = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_USER,EMPTY_STRING);
			host = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_REMOTE_SERVER,EMPTY_STRING);
			port = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PORT,EMPTY_STRING);
			password = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PASSWORD,EMPTY_STRING);
			connectionType = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_CONNECTION_TYPE,EMPTY_STRING);
						
			mainClass = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,EMPTY_STRING);
			project = getJavaProject(configuration);

			if ((getVMArguments(configuration) != null)	&& (!getVMArguments(configuration).equalsIgnoreCase(""))) {
				vmArgs = getArguments(getVMArguments(configuration));
			}
			if ((getProgramArguments(configuration) != null) && (!getProgramArguments(configuration).equalsIgnoreCase(""))) {
				programArgs = getArguments(getProgramArguments(configuration));
			}

			selectedResourcesString = configuration.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_SELECTED_RESOURCES, selectedResourcesString);
			
			if(selectedResourcesString.size()>0){
				int index = 0;
				userSelectedResources = new ArrayList();
				while(index<selectedResourcesString.size()){
					IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(selectedResourcesString.get(index).toString());
					userSelectedResources.add(index, resource);
					index++;
				}
			}
			
			monitor.subTask("Launch remote versioned configuration");
			
			RemoteVersionedLaunchConfiguration remoteLaunch = new RemoteVersionedLaunchConfiguration();
			remoteLaunch.launch(configuration, mode, launch, monitor, host, port, portDebug,connectionType,user, password, mainClass, vmArgs, programArgs, userSelectedResources, project);
			
			if (monitor.isCanceled()) {
				RESClientPlugin.log("Remote versioned launch configuration canceled");
				return;
			}
			
			monitor.done();
			
		}

	}

	private String[] getArguments(String args) {

		StringTokenizer arguments = new StringTokenizer(args);
		String[] arrayArguments = new String[arguments.countTokens()];
		int indexArgument = 0;
		while (arguments.hasMoreTokens()) {
			arrayArguments[indexArgument] = arguments.nextToken();
		}

		return arrayArguments;

	}

}
