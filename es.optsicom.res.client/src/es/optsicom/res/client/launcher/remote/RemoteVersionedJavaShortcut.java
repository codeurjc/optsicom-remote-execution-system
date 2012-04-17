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
package es.optsicom.res.client.launcher.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaApplicationLaunchShortcut;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.dnd.SwtUtil;

import es.optsicom.res.client.RESClientPlugin;
import es.optsicom.res.client.ZipFileCreation;
import es.optsicom.res.client.login.ServerConfigurationWizard;
import es.optsicom.res.client.util.ProjectDependenciesResolver;



public class RemoteVersionedJavaShortcut extends JavaApplicationLaunchShortcut {

	private final static String REMOTE_JAVA_VERSIONED_ID_APPLICATION= "es.optsicom.res.client.launcher.remote.launchConfigurationType";
	public static final String RMI_OBJ_REGISTRY_NAME = "RemoteManagerUniqueName";
	
	private String password = null;
	private String host = null;
	private String portRmi = null;
	private String portDebug = null;
	private String vmargs = null;
	private String prgargs = null;
	
	private boolean salir = false;
	
	private List selectedResources;
	
	// Metodos para la configuracion del Shortcut
	protected ILaunchConfigurationType getConfigurationType() {
		return getLaunchManager().getLaunchConfigurationType(REMOTE_JAVA_VERSIONED_ID_APPLICATION);		
	}
	
	private ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
	
	protected void addSourceLocations(ILaunchConfigurationWorkingCopy configuration,Path path) {
		String type = null;
		try {
			type = configuration.getType().getSourceLocatorId();

			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			ISourceLocator locator = launchManager.newSourceLocator(type);
			
			if (locator instanceof AbstractSourceLookupDirector) {
				AbstractSourceLookupDirector director = (AbstractSourceLookupDirector) locator;
				director.initializeDefaults(configuration);
				//addSourceLocation(locator, director, path);
				if (path.toFile().exists()) {
					String unitLocationPathString = path.toOSString();
					ExternalArchiveSourceContainer easc = new ExternalArchiveSourceContainer(unitLocationPathString,false);
					ArrayList containerList = new ArrayList(Arrays.asList(director.getSourceContainers()));
					containerList.add(easc);
					director.setSourceContainers((ISourceContainer[]) containerList.toArray(new ISourceContainer[containerList.size()]));
				}
				configuration.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, director.getMemento());
				configuration.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, director.getId());
			}
		} catch (CoreException e) {
			RESClientPlugin.log(e);
		}

	}
	
	//Metodo que crea la configuracion del Shortcut
	protected ILaunchConfiguration createConfiguration(IType type, String mode, String zipFile) {
		ILaunchConfiguration config = null;
		ILaunchConfigurationWorkingCopy wc = null;
		Map<String,String> attrMap = new HashMap<String,String>();
		
		try {		
			
			//Configuracion del ShortCut
			ILaunchConfigurationType configType = getConfigurationType();
			wc = configType.newInstance(null, getLaunchManager().generateUniqueLaunchConfigurationNameFrom(type.getTypeQualifiedName('.')));
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					type.getJavaProject().getElementName());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR,
					JavaRuntime.getDefaultVMConnector().getIdentifier());
			
			attrMap.put("hostname",host);
			
			if (mode.equals("debug")){
				attrMap.put("port",portDebug);
			}
			else{
				attrMap.put("port","0");
			}
			
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, attrMap);
			
			//Configuracion de los ficheros fuentes que va a admitir
			addSourceLocations(wc, new Path(zipFile));
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_SOURCE_PATH, false);

			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE,true);

			wc.setMappedResources(new IResource[] {type.getUnderlyingResource()});
			config = wc.doSave();
		} catch (Exception e) {
			RESClientPlugin.log(e);
			MessageDialog.openError(getShell(), "Launch Configuration Error", e.getMessage());
		}
		return config;
	}
	
	
	private void searchAndLaunch(Object[] scope, String mode, String selectTitle, String emptyMessage) {
		IType[] types = null;
		try {
			types = findTypes(scope, PlatformUI.getWorkbench().getProgressService());
		} catch (InterruptedException e) {
			RESClientPlugin.log(e);
			return;
		} catch (CoreException e) {
			RESClientPlugin.log(e);
			MessageDialog.openError(getShell(), "Launch Configuration Error", e.getMessage()); 
			return;
		}
		
		IType type = null;
		if (types.length == 0) {
			MessageDialog.openError(getShell(), LauncherMessages.JavaLaunchShortcut_1, emptyMessage); 
		} else if (types.length > 1) {
			type = chooseType(types, selectTitle);
		} else {
			type = types[0];
		}
		
		if (type != null) {
			launch(type, mode);
		}
	}
	
	protected void launch(final IType type, final String mode) {

		ProjectDependenciesResolver dependenciesResolver = new ProjectDependenciesResolver(type.getJavaProject());
		dependenciesResolver.resolveDependencies();
		
		WizardDialog wd = new WizardDialog(getShell(), new ServerConfigurationWizard(mode, this));
		wd.create();
//		wd.setTitle("Server connection configuration");
//		wd.setMessage("Configure the connection in this page and press the Validate button.\nAdditional resources needed can be selected in next page");
		wd.open();
		
		RESClientPlugin.log("Salir: "+salir);

		if (salir) {
			return;
		}
		
		//mgarcia: Optiscom Res evolution
		final RemoteExecutionJob job = new RemoteExecutionJob();
		job.setHost(host);
		job.setPortRMI(portRmi);
		job.setPortDebug(portDebug);
		job.setPassword(password);
		job.setVmArgs(vmargs);
		job.setProgramArgs(prgargs);
		job.setMainClass(type.getFullyQualifiedName());
		job.setMode(mode);
		job.setUserSelectedResources(selectedResources);
		job.setProject(type.getJavaProject());
		
		job.addJobChangeListener(new JobChangeAdapter(){
			@Override
			public void done(IJobChangeEvent event) {
				final IStatus status = event.getResult();
				if(status.isOK()) {
					ILaunchConfiguration config = createConfiguration(type, mode, job.getZipName());
					if (config != null && "debug".equals(mode)){
						DebugUITools.launch(config, mode);
					}
				} else {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							MessageDialog.openError(getShell(), "Error", status.getMessage());
						}
					});
				}
			}
		});
		job.schedule();
						
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
	 */
	public void launch(IEditorPart editor, String mode) {
		IEditorInput input = editor.getEditorInput();
		IJavaElement je = (IJavaElement) input.getAdapter(IJavaElement.class);
		if (je != null) {
			searchAndLaunch(new Object[] {je}, mode, getTypeSelectionTitle(), getEditorEmptyMessage());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection, java.lang.String)
	 */
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			searchAndLaunch(((IStructuredSelection)selection).toArray(), mode, getTypeSelectionTitle(), getSelectionEmptyMessage());
		}
	}

	public void setSelectedResources(List selectedResources) {
		this.selectedResources = selectedResources;
	}
	
	//Metodos setters utilizados por la interfaz grafica
	public void setPassword(String password) {
		this.password = password;
	}

	public void setSalir(boolean salir) {
		this.salir = salir;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPortRmi(String port) {
		this.portRmi = port;
	}
	/*public void setPortServer(String port) {
		this.portServer = port;
	}*/
	public void setPortDebug(String port) {
		this.portDebug = port;
	}

	public void setVMarg(String vMarg) {
		this.vmargs = vMarg;
	}
	
	public void setPrgarg(String prgarg) {
		this.prgargs = prgarg;
	}

}
