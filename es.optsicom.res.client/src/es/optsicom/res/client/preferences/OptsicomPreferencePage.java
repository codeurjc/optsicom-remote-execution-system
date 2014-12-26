package es.optsicom.res.client.preferences;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import es.optsicom.res.client.EvaluateContributionsHandler;
import es.optsicom.res.client.RESClientPlugin;


public class OptsicomPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {

	private static final String SERVERS = "SERVERS";
	private static final String OPTSICOM = "/optsicom";
	
	private HashMap<String,String> connections;
	private ISecurePreferences root;
	private Table tableSavedConfifurations;
	
	private Button bEdit;
	private Button bDelete;
	
	public void init(IWorkbench workbench) {
		noDefaultAndApplyButton();
	}

	protected Control createContents(final Composite parent) {
	
		final Composite contents =new Composite (parent,SWT.NONE| SWT.NORMAL);
		
		GridLayout gly = new GridLayout (2,false);
		contents.setLayout(gly);
		contents.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
		
		tableSavedConfifurations= new Table(contents, SWT.BORDER | SWT.SINGLE);
		tableSavedConfifurations.setLinesVisible(true);
		tableSavedConfifurations.setHeaderVisible(true);
		TableColumn c1= new TableColumn(tableSavedConfifurations, SWT.NONE);
		c1.setText("Name");
		c1.setWidth(150);
		
		TableColumn c2= new TableColumn(tableSavedConfifurations, SWT.NONE);
		c2.setText("Host");
		c2.setWidth(100);

		TableColumn c3= new TableColumn(tableSavedConfifurations, SWT.NONE);
		c3.setText("ConnectionType");
		c3.setWidth(100);
		
		tableSavedConfifurations.setVisible(true);
		GridData gridTable = new GridData(SWT.FILL,SWT.FILL,true,true);
		gridTable.heightHint= 200;
		tableSavedConfifurations.setLayoutData(gridTable);
		
		tableSavedConfifurations.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				if (tableSavedConfifurations.getSelection().length>0){
					bEdit.setEnabled(true);
					bDelete.setEnabled(true);
				}
				else{
					bEdit.setEnabled(false);
					bDelete.setEnabled(false);
				}
			}
			
		});
		
		Composite contentsEdit =new Composite (contents,SWT.RIGHT| SWT.NORMAL);
		contentsEdit.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
		gly = new GridLayout (1,false);
		contentsEdit.setLayout(gly);
		
		bEdit = new Button (contentsEdit, SWT.CENTER);
		bEdit.setText("Edit..");
		bEdit.setEnabled(false);
		
		root = SecurePreferencesFactory.getDefault();
		
		bEdit.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				//JOptionPane.showMessageDialog(null, "Not developed");
				TableItem [] row=tableSavedConfifurations.getSelection();
				if(row!=null){
					String connectionName=row[0].getText();
					Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
					Preferences savedServers = prefs.node(SERVERS);
					
					if(savedServers.get(connectionName, "")!=null){
						String parameters = new String(savedServers.get(connectionName, ""));
						StringTokenizer st = new StringTokenizer(parameters, ":");
						String host= st.nextToken();
						String port= st.nextToken();
						String connectionType= st.nextToken();		
						String user= st.nextToken();	
						String pass="";
						if (root != null) {
							if (root.nodeExists(OPTSICOM)) {
								ISecurePreferences node = root.node(OPTSICOM);
								try {
									if(node.get(connectionName, null) != null){
										pass= node.get(connectionName, null);
										
									}
								} catch (StorageException e1) {
									RESClientPlugin.log(e1);
								}
							} 
						}
						
						OptsicomEditConfiguration editConfigurationView = new OptsicomEditConfiguration(contents.getShell(),user, pass, host, port, connectionName, connectionType);
						editConfigurationView.open();
						loadTable();
					}
				}
			}
		});
		
		bDelete = new Button (contentsEdit, SWT.CENTER);
		bDelete.setText("Delete");
		bDelete.setEnabled(false);
		bDelete.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				TableItem [] row=tableSavedConfifurations.getSelection();
				if(row!=null){
					String connectionName=row[0].getText();
					Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
					boolean resultConfirm=MessageDialog.openConfirm(contents.getShell(), "Confirm", "Are you sure to delelte the saved configuration selected?");
					if(prefs!=null && resultConfirm){
						Preferences savedServers = prefs.node(SERVERS);
						savedServers.remove(connectionName);
						if (root != null) {
							ISecurePreferences node = root.node(OPTSICOM);
							try {
								if(node.get(connectionName, null)!=null){
									node.remove(connectionName);
								}
							} catch (StorageException e) {
								MessageDialog.openError(contents.getShell(), "Error", "It was impossible to delete the saved configuration.");
							}
						}
					}
					else{
						if(prefs==null){
							MessageDialog.openError(contents.getShell(), "Error", "It was impossible to delete the saved configuration.");
						}
					}
					
					loadTable();
				}
			}
		});
		
		Button deleteAll = new Button(contents, SWT.PUSH);
		deleteAll.setText("Delete all");
		deleteAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TableItem [] rows=tableSavedConfifurations.getItems();
				if(rows!=null){
					Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
					boolean resultConfirm=MessageDialog.openConfirm(contents.getShell(), "Confirm", "Are you sure to delelte all of the saved configurations?");
					if(prefs!=null && resultConfirm){
						for(TableItem tableItem : rows){
							Preferences savedServers = prefs.node(SERVERS);
							savedServers.remove(tableItem.getText(0));
							if (root != null) {
								ISecurePreferences node = root.node(OPTSICOM);
								try {
									if(node.get(tableItem.getText(0), null)!=null){
										node.remove(tableItem.getText(0));
									}
								} catch (StorageException e) {
									MessageDialog.openError(contents.getShell(), "Error", "It was impossible to delete all of the saved configurations.");
								}
							}
						}
						
					}
					else{
						if(prefs==null){
							MessageDialog.openError(contents.getShell(), "Error", "It was impossible to delete all of the saved configurations.");
						}
					}
					
					loadTable();
				}
			}
		});
		GridData gd = new GridData();
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = SWT.LEFT;
		deleteAll.setLayoutData(gd);
		
		
		loadTable();
		return new Composite(parent, SWT.NULL);
	}
	
	protected  void	performApply() {
		
		
	}
	
	
	protected void performDefaults() {
		
		
	}
	
	private void loadTable(){
		tableSavedConfifurations.removeAll();
		Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
		Preferences savedServers = prefs.node(SERVERS);
		String[] serverNames = new String[0];
		try {
			serverNames = savedServers.keys();
		} catch (BackingStoreException e) {
			RESClientPlugin.log(e);
		}
		for(int i=0; i<serverNames.length; i++){
			String parameters = new String(savedServers.get(serverNames[i], ""));
			StringTokenizer st = new StringTokenizer(parameters, ":");
			String host="", port="", connectionTypeName="", user="";
			try{
				host=st.nextToken();
				port=st.nextToken();
				connectionTypeName=st.nextToken();					
				user =st.nextToken();
			}
			catch(NoSuchElementException e){
				e.printStackTrace();
			}
			TableItem item = new TableItem(tableSavedConfifurations, SWT.NONE);
			item.setText(0,serverNames[i]);
			item.setText(1,host);
			item.setText(2,connectionTypeName);
		}
		bDelete.setEnabled(false);
		bEdit.setEnabled(false);
	}
	

	
}