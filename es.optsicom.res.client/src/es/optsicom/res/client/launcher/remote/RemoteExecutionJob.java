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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import es.optsicom.res.client.RESClientPlugin;
import es.optsicom.res.client.ZipFileCreation;
import es.optsicom.res.client.util.DependenciesResolverException;
import es.optsicom.res.client.util.ProjectDependenciesResolver;
import es.optsicom.res.server.OptsicomRemoteExecutor;
import es.optsicom.res.server.OptsicomRemoteServer;
import es.optsicom.res.server.PaqueteDatos;


public class RemoteExecutionJob extends Job {

	IRemoteExecution executor;
	
		
	public RemoteExecutionJob() {
		super("Remote Execution");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		return executor.run(monitor);
	}
	
	public void setRemoteExecution(IRemoteExecution executor){
		this.executor=executor;
	}
	
	public IRemoteExecution getRemoteExecution(){
		return this.executor;
	}
	
		
		
}