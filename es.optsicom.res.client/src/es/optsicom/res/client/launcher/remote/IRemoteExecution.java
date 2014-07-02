package es.optsicom.res.client.launcher.remote;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;

import es.optsicom.res.server.OptsicomRemoteExecutor;

public interface IRemoteExecution {
	void send(String zipName, OptsicomRemoteExecutor executor, SubMonitor monitor) throws IOException;
	IStatus run(IProgressMonitor monitor);
	void openConsole(final OptsicomRemoteExecutor executor,final String idjob);
	void getResultingFile(final OptsicomRemoteExecutor executor,final String idjob);
	void getZipResultingFile(OptsicomRemoteExecutor executor,String idjob);
	void setPortRMI(String portRmi);
	void setHost(String host);
	void setMainClass(String mainClass);
	void setPassword(String password);
	void setProgramArgs(String[] programArgs);
	void setVmArgs(String[] vmArgs);
	void setMode(String mode);
	void setUserSelectedResources(List userSelectedResources);
	void setProject(IJavaProject project);
	void setPortDebug(String portDebug);
	String getZipName();
}
