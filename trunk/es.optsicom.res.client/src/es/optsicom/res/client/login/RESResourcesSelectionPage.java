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

import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.dialogs.WizardExportResourcesPage;

public class RESResourcesSelectionPage extends WizardExportResourcesPage {

	protected RESResourcesSelectionPage(IStructuredSelection selection) {
		super("Resources to export", selection);
	}

	@Override
	protected void createDestinationGroup(Composite parent) {
		setTitle("Additional resources to export");
		setMessage("There is no need to select source and output folders,\nthese will be selected by default.");
	}

	@Override
	public void handleEvent(Event event) {
		if(event.type == SWT.OPEN) {
		}
	}

	public List getResources() {
		return this.getWhiteCheckedResources();
	}

}
