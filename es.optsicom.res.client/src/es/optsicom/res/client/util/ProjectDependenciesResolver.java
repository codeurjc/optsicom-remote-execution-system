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
package es.optsicom.res.client.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import es.optsicom.res.client.RESClientPlugin;

/**
 * Class that resolves all the dependencies of a given java project.
 * 
 * @author Patxi Gortázar (patxi.gortazar@gmail.com)
 *
 */
public class ProjectDependenciesResolver {

	private List<String> dependencies;
	private List<String> classpath;
	private IJavaProject javaProject;
	private List<File> classpathFiles;

	/**
	 * Constructs a new instance to resolve dependencies of <code>jp</code> java project
	 * @param jp The java project for which this instance will resolve dependencies
	 */
	public ProjectDependenciesResolver(IJavaProject jp) {
		this.javaProject = jp;
	}
	
	/**
	 * Resolve dependencies for the project specified in constructor. All dependencies are resolved:
	 * 
	 * <ul>
	 * <li>Projects are traversed and their dependencies added in a recursive manner, 
	 * without duplicates</li>
	 * <li>Output folders</li>
	 * <li>Libraries</li>
	 * <li>Sources</li>
	 * <li>Variables are resolved and traversed</li>
	 * <li>Containers are added, except the JRE container</li>
	 * </ul>
	 * 
	 * As a result, two lists are created:
	 * 
	 * <ul>
	 * <li>{@link #getClasspath()} returns the list of resources (folders and jars) that are
	 * needed by the project. This is a list of strings.</li>
	 * <li>{@link #getClasspathFiles()} returns the list of {@link File}s that are needed
	 * by the project, including source containers.</li>
	 * </ul>
	 */
	public void resolveDependencies() {
		dependencies = new ArrayList<String>();
		classpath = new ArrayList<String>();
		classpathFiles = new ArrayList<File>();
		try {
			IClasspathEntry[] cpes = javaProject.getRawClasspath();
			calculateDependencies(cpes, javaProject.getProject());
		} catch (JavaModelException e) {
			RESClientPlugin.log(e);
		}
	}
	
	/*
	 * Files stored in lista are made absolute, so that they can be resolver to be included in the zip file.
	 * The zip file creator is responsible of making these files relative with respect to the workspace
	 * when adding the files to the zip file.
	 */
	private void calculateDependencies(IClasspathEntry[] cpe, IProject project){
		try {
			
			IWorkspace workspace = project.getWorkspace();
			IPath workspacePath = workspace.getRoot().getLocation();
			String nombreWorkspace = workspacePath.toString();
			IJavaProject jp = JavaCore.create(project);
			
			if(!dependencies.contains(project.getLocation().toString())) {
				// Añadimos la carpeta bin
				classpathFiles.add(workspacePath.append(jp.getOutputLocation()).toFile());
				classpath.add(jp.getOutputLocation().toString());
			}				

			for (IClasspathEntry cpEntry : cpe) {

				String path = cpEntry.getPath().toString();
				String dependency = nombreWorkspace.concat(path);
				
				if (!dependencies.contains(dependency)){
					RESClientPlugin.log("Adding dependency: " + dependency);
					dependencies.add(dependency);
					
					if (cpEntry.getOutputLocation() != null) {
						RESClientPlugin.log("Binarios: "+cpEntry.getOutputLocation().toString());
						classpath.add(cpEntry.getOutputLocation().makeRelativeTo(workspacePath).toString());
						classpathFiles.add(cpEntry.getOutputLocation().toFile());
					}
					
					int tipo = cpEntry.getEntryKind();
					
					//Si la dependencia es de una libreria(
					
					if(tipo == IClasspathEntry.CPE_LIBRARY){
						
						String dep = cpEntry.getPath().makeRelativeTo(workspacePath).toString();
						classpathFiles.add(new File(workspacePath.toFile(), cpEntry.getPath().toOSString()));
						
						//Añadimos las dependencias a las properties
						RESClientPlugin.log("Adding library: " + dep);
						classpath.add(cpEntry.getPath().toString());
						
					} else if (tipo == IClasspathEntry.CPE_PROJECT){
						
//						File[] files = new File(dependency).listFiles();
//						for (File f : files){
//							lista.add(f);
//							String dep = f.getPath();
//							RESClientPlugin.log("Adding dependency: " + dep);
//							dependencies.add(dep);
//						}
						
						IProject p = workspace.getRoot().getProject(cpEntry.getPath().lastSegment());
						IJavaProject projectDependency = JavaCore.create(p);
						IClasspathEntry[] cp = projectDependency.getRawClasspath();

						classpathFiles.add(workspacePath.append(projectDependency.getOutputLocation()).toFile());
						classpath.add(projectDependency.getOutputLocation().toString());

						RESClientPlugin.log("Populating files from: " + p.getName());
						calculateDependencies(cp,p);
						
					} else if (tipo == IClasspathEntry.CPE_SOURCE){

						File f = new File(dependency);
						classpathFiles.add(f);
						RESClientPlugin.log("Adding source: " + dependency);
						
					} else if (tipo == IClasspathEntry.CPE_VARIABLE){
						
						IClasspathEntry[] clpe = new IClasspathEntry[1];
						clpe[0] = JavaCore.getResolvedClasspathEntry(cpEntry);
						if (clpe[0] != null) {
							RESClientPlugin.log("Populating files from: " + clpe[0].getPath().toOSString());
							calculateDependencies(clpe, project);
						}
						
					} else if (tipo == IClasspathEntry.CPE_CONTAINER){
						
						if (cpEntry.getPath().toOSString().contains("JRE_CONTAINER") || cpEntry.getPath().toOSString().contains("requiredPlugins")){
							continue;
						}
						
						IClasspathContainer cc = JavaCore.getClasspathContainer(cpEntry.getPath(), jp);
						IClasspathEntry[] entradas = cc.getClasspathEntries();
						
						RESClientPlugin.log("Populating files from: " + cc.getPath().toOSString());
						calculateDependencies(entradas, project);
						
					}
				}
			}
		} catch (JavaModelException e) {
			RESClientPlugin.log(e);
		}
		
		for(String path : classpath) {
			RESClientPlugin.log("Classpath: " + path);
		}
		for(File file : classpathFiles) {
			RESClientPlugin.log("Classpath file: " + file.getAbsolutePath());
		}
	}

	public List<String> getClasspath() {
		return classpath;
	}

	public List<File> getClasspathFiles() {
		return classpathFiles;
	}

	public IJavaProject getJavaProject() {
		return javaProject;
	}
}
