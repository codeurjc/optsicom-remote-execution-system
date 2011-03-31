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
package es.optsicom.res.client.login;

import org.eclipse.jface.wizard.Wizard;

import es.optsicom.res.client.launcher.remote.RemoteVersionedJavaShortcut;

public class ServerConfigurationWizard extends Wizard {

	private RemoteVersionedJavaShortcut rvjs;
	private RESResourcesSelectionPage selectionPage;
	private ServerConfigurationPage configurationPage;
	private String mode;

	public ServerConfigurationWizard(String mode, RemoteVersionedJavaShortcut rvjs) {
		super();
		this.rvjs = rvjs;
		this.mode = mode;
		this.setNeedsProgressMonitor(true);
	}
	
	@Override
	public void addPages() {
		super.addPages();
		configurationPage = new ServerConfigurationPage(mode);
		this.addPage(configurationPage);
		selectionPage = new RESResourcesSelectionPage(null);
		this.addPage(selectionPage);
	}
	
	@Override
	public boolean performFinish() {
		// Poner los valores en rvjs
		rvjs.setPassword(configurationPage.getPasswd());
		rvjs.setHost(configurationPage.getHost());
		rvjs.setPortRmi(configurationPage.getPortRMI());
		rvjs.setVMarg(configurationPage.getVMArgs());
		rvjs.setPrgarg(configurationPage.getProgramArgs());
		rvjs.setPortDebug(configurationPage.getVMDebugPort());
		rvjs.setSalir(false);
		
		rvjs.setSelectedResources(selectionPage.getResources());

		return true;
	}
	
	@Override
	public boolean performCancel() {
		rvjs.setSalir(true);
		return true;
	}

	@Override
	public boolean canFinish() {
		return configurationPage.isConnectionValid();
	}
	
}
