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
package es.optsicom.res.client.views;


import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.rmi.Naming;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.prefs.BackingStoreException;

import es.optsicom.res.client.EvaluateContributionsHandler;
import es.optsicom.res.client.RESClientPlugin;
import es.optsicom.res.client.launcher.remote.IRemoteExecution;
import es.optsicom.res.server.OptsicomRemoteExecutor;
import es.optsicom.res.server.OptsicomRemoteServer;


public class OptsicomView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "es.optsicom.res.client.views.OptsicomView";

	private TableViewer viewer;
	private Action refresh;
	private Action showOutputAction;
	private Action deleteAction;
	private Action doubleClickAction;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			RESClientPlugin a = RESClientPlugin.getDefault();
			ScopedPreferenceStore sps = (ScopedPreferenceStore)a.getPreferenceStore();
			IEclipsePreferences[] iep = sps.getPreferenceNodes(false);
			try {
				String[] keys = iep[0].keys();
				return keys;
			} catch (BackingStoreException e) {
				RESClientPlugin.log(e);
			}
			return null;
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}
		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().
					getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
		
		public String getColumnText(Object element, int columnIndex) {
			String idjob = element.toString();
			RESClientPlugin a = RESClientPlugin.getDefault();
			ScopedPreferenceStore sps = (ScopedPreferenceStore)a.getPreferenceStore();
			IEclipsePreferences[] iep = sps.getPreferenceNodes(false);
			
			
			
			String tokens[] = new String[5];
			

			tokens = iep[0].get(idjob,"").split(":");
			String connectionType = tokens[0];
			String host = tokens[1];
			String port = tokens[2];
			String user = tokens[3];
			String password = tokens[4];
			
			switch (columnIndex) {
				case 0:
					return connectionType;
				case 1:
					return idjob.toString();
				case 2: //Host
					return host;
				case 3:
					return port;
				case 4:
					IRemoteExecution executor= null;
					EvaluateContributionsHandler pluginHandler = new EvaluateContributionsHandler();
					try {
						executor=pluginHandler.getPlugin(connectionType);
						executor.setHost(host);
						executor.setPort(port);
						executor.setUser(user);
						executor.setPassword(password);
						return executor.getState(idjob);
					} catch (InvalidSyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return "---";
					}

			}
			return "undetermined";
		}	
	}
	
	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public OptsicomView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("Connection type");
		column.getColumn().setToolTipText("Connection type");
		column.getColumn().setWidth(150);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(true);
		
		
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("Process");
		column.getColumn().setToolTipText("Process");
		column.getColumn().setWidth(250);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(true);

				
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("Host");
		column.getColumn().setWidth(250);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(true);
		
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("Port");
		column.getColumn().setWidth(100);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(true);
		
		column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText("State");
		column.getColumn().setWidth(200);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(true);

		
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);


		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());
		//viewer.getTable().setLinesVisible(true);

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "es.optsicom.res.client.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				OptsicomView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(showOutputAction);
		manager.add(deleteAction);
		manager.add(refresh);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void makeActions() {
		
		refresh = new Action() {
			public void run() {
				viewer.refresh();
			}
		};
		refresh.setText("Refresh");
		refresh.setToolTipText("Refresh");
		
		showOutputAction = new Action() {
			public void run() {
				doubleClickAction.run();
				
			}
		};
		showOutputAction.setText("Show execution output");
		showOutputAction.setToolTipText("Show execution output");
		showOutputAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		deleteAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				RESClientPlugin a = RESClientPlugin.getDefault();
				ScopedPreferenceStore sps = (ScopedPreferenceStore)a.getPreferenceStore();
				IEclipsePreferences[] iep = sps.getPreferenceNodes(false);
				try{
					iep[0].remove(obj.toString());
					//showMessage("Tarea eliminada correctamente");
					viewer.refresh();
				}
				catch (Exception e){
					RESClientPlugin.log(e);
					showMessage("Couldn't delete the entry");
				}
			}
		};
		deleteAction.setText("Remove entry");
		deleteAction.setToolTipText("Removes the selected entry");
		deleteAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj == null){
					showMessage("Couldn't show output of program");
				}else{
					try {
						//Acceso a los datos de la ejecucion
						RESClientPlugin a = RESClientPlugin.getDefault();
						
						ScopedPreferenceStore sps = (ScopedPreferenceStore)a.getPreferenceStore();
						IEclipsePreferences[] iep = sps.getPreferenceNodes(false);
						String[] keys = iep[0].keys();
						
						for (int i=0;i<keys.length;i++){
							if (keys[i].equals(obj.toString())){
								//System.out.println("-->Crea el objeto remoto del server");
								String tokens[] = new String[5];
								String idjob = obj.toString();

								tokens = iep[0].get(idjob,"").split(":");
								String connectionType = tokens[0];
								String host = tokens[1];
								String port = tokens[2];
								String user = tokens[3];
								String password = tokens[4];
								
								IRemoteExecution executor= null;
								EvaluateContributionsHandler pluginHandler = new EvaluateContributionsHandler();
								try {
									IWorkspace ws = ResourcesPlugin.getWorkspace();
									String workSpaceRoot = ws.getRoot().getLocation().toOSString();
									executor=pluginHandler.getPlugin(connectionType);
									executor.setHost(host);
									executor.setPort(port);
									executor.setUser(user);
									executor.setPassword(password);
									executor.getResultFromView(workSpaceRoot, idjob);
								} catch (InvalidSyntaxException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								break;
							}
						}
						
						viewer.refresh();
					} catch (Exception e) {
						RESClientPlugin.log(e);
						MessageDialog.openError(JDIDebugUIPlugin.getActiveWorkbenchShell(), LauncherMessages.JavaLaunchShortcut_3, e.getMessage());
					}
					//showMessage("Double-click detected on "+obj.toString());
				}
				
			}
		};
		
		
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Veex View",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	
	
}