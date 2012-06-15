package es.optsicom.res.client.preferences;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import es.optsicom.res.client.RESClientPlugin;


public class OptsicomPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {

	private static final String SERVERS = "SERVERS";
	private static final String OPTSICOM = "/optsicom";
	private Text txtPass;
	private Text txtHost;
	private Text txtPortRmi;
	private Combo savedConnections;
	private HashMap<String,String> connections;
	private ISecurePreferences root;
	
	public void init(IWorkbench workbench) {
		setDescription("Configuration server");
	}

	protected Control createContents(Composite parent) {
		
		Composite contents = new Composite(parent, SWT.NONE | SWT.BORDER);
		GridLayout gly = new GridLayout(2, false);
		contents.setLayout(gly);
		contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label comboLabel = new Label(contents, SWT.LEFT);
		comboLabel.setText("Saved configuration:");
		savedConnections = new Combo(contents, SWT.BORDER | SWT.READ_ONLY);
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		savedConnections.setLayoutData(gd);
		
		Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
		Preferences savedServers = prefs.node(SERVERS);
		
		String[] serverNames = new String[0];
		try {
			serverNames = savedServers.keys();
		} catch (BackingStoreException e) {
			RESClientPlugin.log(e);
		}
		
		connections = new HashMap<String, String>();
		root = SecurePreferencesFactory.getDefault();
				
		savedConnections.setItems(serverNames);
		savedConnections.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo c = (Combo) e.widget;
				int itemSelected = c.getSelectionIndex();
				if(!connections.containsKey(c.getItem(itemSelected))){
					Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
					Preferences savedServers = prefs.node(SERVERS);
					String parameters = new String(savedServers.get(c.getItem(itemSelected), ""));
					StringTokenizer st = new StringTokenizer(parameters, ":");
					txtHost.setText(st.nextToken());
					txtPortRmi.setText(st.nextToken());
					if (root != null) {
						if (root.nodeExists(OPTSICOM)) {
							ISecurePreferences node = root.node(OPTSICOM);
							try {
								if(node.get(c.getItem(itemSelected), null) != null){
									txtPass.setText(node.get(c.getItem(itemSelected), null));
								}
							} catch (StorageException e1) {
								RESClientPlugin.log(e1);
							}
						} 
					}
				} else {
					String parameters = connections.get(c.getItem(itemSelected));
					StringTokenizer st = new StringTokenizer(parameters, ":");
					txtPass.setText(st.nextToken());
					txtHost.setText(st.nextToken());
					txtPortRmi.setText(st.nextToken());
				}
			}
		});
		
		Group datosPersonales = new Group(contents, SWT.SHADOW_IN);
		GridData gridDataHV = new GridData();
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.horizontalSpan = 2;
		gridDataHV.grabExcessHorizontalSpace = true;
		datosPersonales.setLayoutData(gridDataHV);
		datosPersonales.setText("Authentication");
		gly = new GridLayout();
		gly.numColumns = 2;
		datosPersonales.setLayout(gly);
		datosPersonales.setSize(200, 200);
		
		Label lblPass = new Label(datosPersonales, SWT.CENTER);
		lblPass.setText("Password:");
		txtPass = new Text(datosPersonales, SWT.BORDER | SWT.PASSWORD);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;	 
		txtPass.setLayoutData(gridDataHV);
		
		Group datosConexion = new Group(contents, SWT.SHADOW_IN);
		gridDataHV = new GridData();
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 2;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;
		datosConexion.setLayoutData(gridDataHV);
		datosConexion.setText("Connection data");
		gly = new GridLayout();
		gly.numColumns = 2;
		datosConexion.setLayout(gly);
		datosConexion.setSize(200, 200);
		
		Label lblHost = new Label(datosConexion, SWT.LEFT);
		lblHost.setText("Host:");
		txtHost = new Text(datosConexion, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;	 
		txtHost.setLayoutData(gridDataHV);
		
		Label lblPortRmi = new Label(datosConexion, SWT.LEFT);
		lblPortRmi.setText("Host RMI port:");
		txtPortRmi = new Text(datosConexion, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;
		txtPortRmi.setLayoutData(gridDataHV);
		
		if(serverNames.length == 0){
			txtPass.setEnabled(false);
			txtHost.setEnabled(false);
			txtPortRmi.setEnabled(false);
		}
		
		return new Composite(parent, SWT.NULL);
	}
	
	protected  void	performApply() {
		int itemSelected = savedConnections.getSelectionIndex();
		if(itemSelected >= 0){
			String configName = savedConnections.getItem(itemSelected);
			String parameters = txtPass.getText() + ":" + txtHost.getText() + ":" + txtPortRmi.getText();
			connections.put(configName, parameters);
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	public boolean performOk() {
		int itemSelected = savedConnections.getSelectionIndex();
		if(itemSelected >= 0){
			String configName = savedConnections.getItem(itemSelected);
			String parameters = txtPass.getText() + ":" + txtHost.getText() + ":" + txtPortRmi.getText();
			connections.put(configName, parameters);
		}
		
		Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
		Preferences savedServers = prefs.node(SERVERS);
		
		if(!connections.isEmpty()) {
			Iterator iter = connections.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry mEntry = (Map.Entry) iter.next();
				System.out.println(mEntry.getKey() + " : " + mEntry.getValue());
				StringTokenizer st = new StringTokenizer(mEntry.getValue().toString(), ":");
				String password = st.nextToken();
				String host = st.nextToken();
				String portRmi = st.nextToken();
				if (root != null) {
					if (root.nodeExists(OPTSICOM)) {
						ISecurePreferences node = root.node(OPTSICOM);
						try {
							if(mEntry.getKey().toString() != null){
								node.put(mEntry.getKey().toString(),password, false);
								node.flush();
							}
						} catch (StorageException e1) {
							RESClientPlugin.log(e1);
						} catch (IOException e) {
							RESClientPlugin.log(e);
						} 
					} 
				}
				savedServers.put(mEntry.getKey().toString(), host + ":" + portRmi);
			}
			
			try {
				savedServers.flush();
			} catch (BackingStoreException e1) {
				RESClientPlugin.log(e1);
				MessageDialog.openError(getShell(), "Saving settings", "Unable to store settings: " + e1.getMessage());
			}
		}
		return true;
		
	}
	
	protected void performDefaults() {
		
		connections = new HashMap<String, String>();
		
		Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
		Preferences savedServers = prefs.node(SERVERS);
				
		int itemSelected = savedConnections.getSelectionIndex();
		if(itemSelected >= 0){
			String configName = savedConnections.getItem(itemSelected);
			
			String parameters = new String(savedServers.get(configName, ""));
			StringTokenizer st = new StringTokenizer(parameters, ":");
			txtHost.setText(st.nextToken());
			txtPortRmi.setText(st.nextToken());
			if (root != null) {
				if (root.nodeExists(OPTSICOM)) {
					ISecurePreferences node = root.node(OPTSICOM);
					try {
						txtPass.setText(node.get(configName, null));
					} catch (StorageException e1) {
						RESClientPlugin.log(e1);
					}
				} 
			}
		}
		
	}
	
}