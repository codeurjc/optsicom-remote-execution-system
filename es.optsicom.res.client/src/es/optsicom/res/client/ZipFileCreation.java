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

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;

import es.optsicom.res.client.util.ProjectDependenciesResolver;
import es.optsicom.res.client.util.ZipCreator;
import es.optsicom.res.client.util.ZipCreatorException;

/**
 * <p>This class exports a given {@link IJavaProject} to a zip file, including all
 * its dependencies, except those belonging to the JRE</p>
 * 
 * <p>Users of this class can specify additional files and folders to be included
 * in the zip, like images or files that are needed by the project, by calling the
 * {@link ZipFileCreation#setUserSelectedResources(List)} method and passing in
 * a list of files to be included.</p> 
 * 
 * @author Patxi Gortázar (patxi.gortazar@gmail.com)
 *
 */
public class ZipFileCreation {

	private ProjectDependenciesResolver resolver;
	private List userSelectedResources;
	private String zipName;
	
	/**
	 * Configures this instance to export the specified java project to a zip file.
	 *  
	 * @param project The {@link IJavaProject} that is to be exportad as a zip file
	 */
	public ZipFileCreation(IJavaProject project) {
		resolver = new ProjectDependenciesResolver(project);
	}

	/**
	 * This method exports the project specified in the constructor, along with all its
	 * dependencies, and used supplied files, to a zip file. The name of the file
	 * is: java project name + timestamp + ".zip". The file is saved in the project root folder.
	 *  	
	 * @param monitor A monitor that is used to report progress
	 * @return The resulting status of the operation
	 */
	public IStatus create(SubMonitor monitor) {
		monitor.beginTask("Creating zip file", 2);

		//Variables para el envio de los ficheros .jar y .zip
		resolver.resolveDependencies();
		List<File> dependencies = resolver.getClasspathFiles();
		
		monitor.worked(1);
		
		// Añadimos los ficheros que seleccionó el usuario en el wizard
		if(userSelectedResources != null) { 
			for(Object o : userSelectedResources) {
				IResource resource = (IResource) o;
				dependencies.add(resource.getRawLocation().toFile());
			}
		}

		IWorkspace ws = ResourcesPlugin.getWorkspace();
		String projectName = resolver.getJavaProject().getElementName();
		try {
			zipName = createZIPFile(ws.getRoot().getLocation().toOSString(), projectName, dependencies, monitor.newChild(1));
		} catch (ZipCreatorException e) {
			RESClientPlugin.log(e);
			return new Status(IStatus.ERROR, RESClientPlugin.PLUGIN_ID, "Couldn't create zip file");
		}
		
		//mgarcia: Optiscom Res evolution
		if (monitor.isCanceled()) {
			return new Status(IStatus.CANCEL, RESClientPlugin.PLUGIN_ID, "Operation has been cancelled");
		}
		
		monitor.worked(1);
		
		if(zipName == null) {
			return new Status(IStatus.ERROR, RESClientPlugin.PLUGIN_ID, "Couldn't create zip file");
		}

		return new Status(IStatus.OK, RESClientPlugin.PLUGIN_ID, "File succesfully created");
	}
	
	private String createZIPFile(String workspaceLocation, String projectName, List<File> filesToAdd, IProgressMonitor monitor) throws ZipCreatorException {
		
		final ZipCreator zc = new ZipCreator();

		//Paths para los ficheros jar y zip
		File zipFileFolder = new File(workspaceLocation, projectName);
		File zipFile = new File(zipFileFolder, projectName + RESClientPlugin.getTimeStamp() + ".zip");
		String nombreZip = zipFile.getAbsolutePath();
		
		final File[] filesZip = filesToAdd.toArray(new File[filesToAdd.size()]);
		
		monitor.beginTask("Creating zip file", filesZip.length + 1);
		
		zc.setBaseDir(workspaceLocation);
		zc.zip(filesZip, nombreZip);
		
		//mgarcia: Optiscom Res evolution
		if (monitor.isCanceled()) {
			RESClientPlugin.log("Canceled: delete zip file");
			zipFile.delete();
		}
		
		monitor.worked(1);

		return nombreZip;
	}

	/**
	 * Returns the name of the zip file created. This method should only be called
	 * after calling {@link #create(IProgressMonitor)}.
	 * 
	 * @return the name of the file created in the last call to {@link #create(IProgressMonitor)}
	 */
	public String getZipName() {
		return zipName;
	}

	/**
	 * The resolver to be used to resolve project dependencies. If not specified, the default
	 * {@link ProjectDependenciesResolver} is used.
	 * 
	 * @param resolver The resolver to be used to resolve project dependencies
	 */
	public void setResolver(ProjectDependenciesResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * Specifies additional resources to be added to the zip file.
	 * 
	 * @param userSelectedResources The resources to be added to the zip file
	 */
	public void setUserSelectedResources(List userSelectedResources) {
		this.userSelectedResources = userSelectedResources;
	}

	/** 
	 * Returns the resolver configured for this instance.
	 * 
	 * @return The resolver configured for this instance
	 */
	public ProjectDependenciesResolver getResolver() {
		return resolver;
	}
}
