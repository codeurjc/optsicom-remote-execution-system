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
package es.optsicom.res.client.launcher.local;

import java.io.File;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.DirectorySourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.ui.JavaUISourceLocator;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaApplicationLaunchShortcut;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import es.optsicom.res.client.RESClientPlugin;
import es.optsicom.res.client.ZipFileCreation;
import es.optsicom.res.client.util.ProjectDependenciesResolver;
import es.optsicom.res.client.util.ZipCreator;
import es.optsicom.res.client.util.ZipCreatorException;
import es.optsicom.res.server.OptsicomRemoteServer;

public class LocalVersionedJavaShortcut extends JavaApplicationLaunchShortcut {

	
	private final static String JAVA_VERSIONED_ID_APPLICATION= "es.optsicom.res.client.launcher.local.launchConfigurationType";
	
	private String nombreWorkspace = null;
	private String nombreProyecto = null;
	private List<String> dependencias = new ArrayList<String>();
	private List<String> librerias = new ArrayList<String>();
	
	
	protected ILaunchConfigurationType getConfigurationType() {
		return getLaunchManager().getLaunchConfigurationType(JAVA_VERSIONED_ID_APPLICATION);		
	}
	
	private ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
	
	public void vaciarDependencias(){
		dependencias.clear();
	}


	private List<File> getFicheros(IClasspathEntry[] cpe,IType type,List<File> lista,String nombreProyecto){
		try {
			
			for (int i=0;i<cpe.length;i++){
				
				IWorkspace ws = type.getResource().getWorkspace();
				
				String localizacionProyecto = nombreWorkspace;
				String path = cpe[i].getPath().makeAbsolute().toOSString();
				String dependencia = localizacionProyecto.concat(path);
				
				if (!dependencias.contains(dependencia)){
					dependencias.add(dependencia);
					if (cpe[i].getOutputLocation() != null)
						RESClientPlugin.log("Binarios: "+cpe[i].getOutputLocation().toOSString());
					
					int tipo = cpe[i].getEntryKind();
					
					//Si la dependencia es de una libreria(
					if(tipo == 1){
						String ruta = localizacionProyecto+cpe[i].getPath().toOSString();
						File f = new File(ruta);
						String dep = f.getAbsolutePath();
						
						//Añadimos las dependencias a las properties
						librerias.add(dep);
					}
					
					//Si la dependencia es un proyecto
					if (tipo == 2){
						File[] f = new File(dependencia).listFiles();
						for (int j=0;j<f.length;j++){
							lista.add(f[j]);
							String dep = f[j].getAbsolutePath();
							dependencias.add(dep);
						}
						
						IProject p = ws.getRoot().getProject(nombreProyecto);
						IJavaProject jp = JavaCore.create(p);
						IClasspathEntry[] cp = jp.getRawClasspath();
						String nombre = cpe[i].getPath().lastSegment();
						
						lista.addAll(getFicheros(cp,type,lista,nombre));
					}
					if (tipo == 3){
						File f = new File(dependencia);
						lista.add(f);
						String dep = f.getAbsolutePath();
						dependencias.add(dep);
					}
					
					//Si la dependencia es de una variable
					if (tipo == 4){
						IClasspathEntry[] clpe = new IClasspathEntry[1];
						clpe[0] = JavaCore.getResolvedClasspathEntry(cpe[i]);
						if (clpe[0] != null)
							getFicheros(clpe,type,lista,nombreProyecto);
					}
					
					//Si la dependencia es de un contenedor
					if (tipo == 5){
						IProject project = type.getResource().getProject();
						IJavaProject jp = JavaCore.create(project);
						if (cpe[i].getPath().toOSString().contains("JRE_CONTAINER") || cpe[i].getPath().toOSString().contains("requiredPlugins")){
							continue;
						}
						IClasspathContainer cc = JavaCore.getClasspathContainer(cpe[i].getPath(), jp);
						IClasspathEntry[] entradas = cc.getClasspathEntries();
						
						getFicheros(entradas,type,lista,nombreProyecto);
					}
				}
			}
			return lista;
		} catch (JavaModelException e) {
			RESClientPlugin.log(e);
			return null;
		}
	}




	private String creacionFicheros(IType type){
		
		List<File> listaZip = new ArrayList<File>();
		
		//Creacion del fichero comprimido
		IProject project = type.getResource().getProject();
		IJavaProject jp = JavaCore.create(project);
		nombreProyecto = jp.getElementName();
		
		//Obtencion del directorio de trabajo
		IWorkspace ws = type.getResource().getWorkspace();
		nombreWorkspace = ws.getRoot().getLocation().toOSString();
			
		String nombreZip = "";
		try {
			String dirBinarios = nombreWorkspace + jp.getOutputLocation().toOSString();
			File binarios = new File(dirBinarios);
			IClasspathEntry[] cpe = jp.getRawClasspath();
			listaZip = getFicheros(cpe, type, listaZip,nombreProyecto);
			vaciarDependencias();
			
			listaZip.add(binarios);
			File[] filesZip = listaZip.toArray(new File[listaZip.size()]);
			ZipCreator zc = new ZipCreator();
			File zipFileFolder = new File(nombreWorkspace, nombreProyecto);
			File zipFile = new File(zipFileFolder, nombreProyecto + RESClientPlugin.getTimeStamp() + ".zip");
			nombreZip = zipFile.getAbsolutePath();
			zc.zip(filesZip, nombreZip);
		} catch (JavaModelException e) {
			RESClientPlugin.log(e);
			MessageDialog.openError(getShell(), "Error while retrieving project classpath files", e.getMessage());
		} catch (ZipCreatorException e) {
			RESClientPlugin.log(e);
			MessageDialog.openError(getShell(), "Error while creating zip file", e.getMessage());
		}
		
		return nombreZip;
	}
	
	protected ILaunchConfiguration createConfiguration(IType type) {
		ILaunchConfiguration config = null;
		ILaunchConfigurationWorkingCopy wc = null;
		try {
			//Configuracion del ShortCut
			ILaunchConfigurationType configType = getConfigurationType();
			wc = configType.newInstance(null, getLaunchManager().generateUniqueLaunchConfigurationNameFrom(type.getTypeQualifiedName('.')));
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
					type.getFullyQualifiedName());
			
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
					type.getJavaProject().getElementName());
			
			//Configuracion de los ficheros fuentes que va a admitir
			ZipFileCreation zipjob = new ZipFileCreation(type.getJavaProject());
			zipjob.setUserSelectedResources(Collections.emptyList());
			zipjob.create(new NullProgressMonitor());

			String nombre = zipjob.getZipName();
			addSourceLocations(wc, new Path(nombre));
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_SOURCE_PATH, false);			
			
			wc.setMappedResources(new IResource[] {type.getUnderlyingResource()});
			config = wc.doSave();
		} catch (CoreException exception) {
			RESClientPlugin.log(exception);
			MessageDialog.openError(JDIDebugUIPlugin.getActiveWorkbenchShell(), LauncherMessages.JavaLaunchShortcut_3, exception.getStatus().getMessage());	
		} catch (Exception exception) {
			RESClientPlugin.log(exception);
			MessageDialog.openError(JDIDebugUIPlugin.getActiveWorkbenchShell(), LauncherMessages.JavaLaunchShortcut_3, exception.getMessage());
		} 
		return config;
	}
	
	
	private void addSourceLocation(ISourceLocator locator, AbstractSourceLookupDirector director, Path unitLocation){
		
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
			return;
		}

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
			MessageDialog.openError(getShell(), LauncherMessages.JavaLaunchShortcut_0, e.getMessage()); 
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
	
	
	private void launch(IType type, String mode) {
		ILaunchConfiguration config;
		config = createConfiguration(type);
		DebugUITools.launch(config, mode);
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

}