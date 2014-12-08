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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.Naming;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import es.optsicom.res.client.EvaluateContributionsHandler;
import es.optsicom.res.client.RESClientPlugin;
import es.optsicom.res.client.launcher.remote.IRemoteExecution;
import es.optsicom.res.server.OptsicomRemoteExecutor;
import es.optsicom.res.server.OptsicomRemoteServer;

public class ServerConfigurationPage extends WizardPage {

	private static final String SERVERS = "SERVERS";
	//mgarcia: Optiscom Res evolution 
	private static final String OPTSICOM = "/optsicom";
	private Text txtUser;
	private Text txtPass;
	private Text txtHost;
	private Text txtPort;
	private Text txtPortDebug;
	private Text txtVMarg;
	private Text txtPrgarg;
	private String mode;
	private Combo connectionType;
	private boolean connectionValid = false;
	//mgarcia: Optiscom Res evolution 
	private ISecurePreferences root;
	

	protected ServerConfigurationPage(String mode) {
		super("ServerConfiguration");
		setTitle("Server Configuration");
//		setDescription("Server Configuration");
		setMessage("Configure the connection in this page and press the Validate button.\nAdditional resources needed can be selected in next page");
		this.mode = mode;
	}

	@Override
	public void createControl(Composite parent) {
		setTitle("Server Connection Configuration");

		Composite contents = new Composite(parent, SWT.NONE | SWT.BORDER);
		GridLayout gly = new GridLayout(2, false);
		contents.setLayout(gly);
		contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label comboLabel = new Label(contents, SWT.LEFT);
		comboLabel.setText("Choose a previously saved configuration:");
		Combo savedConnections = new Combo(contents, SWT.BORDER | SWT.READ_ONLY);
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		savedConnections.setLayoutData(gd);
		
		Label connectionLabel = new Label(contents, SWT.LEFT);
		connectionLabel.setText("Choose a connection:");
		connectionType = new Combo(contents, SWT.BORDER | SWT.READ_ONLY);
		gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		connectionType.setLayoutData(gd);
		
		String [] connectionTypeList=null;
		EvaluateContributionsHandler pluginHandler= new EvaluateContributionsHandler();
		try {
			connectionTypeList=pluginHandler.getPluginNameList();
		} catch (InvalidSyntaxException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		
		
		connectionType.setItems(connectionTypeList);
		
		
		Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
		Preferences savedServers = prefs.node(SERVERS);
		String[] serverNames = new String[0];
		try {
			serverNames = savedServers.keys();
		} catch (BackingStoreException e) {
			RESClientPlugin.log(e);
		}
		
		//mgarcia: Optsicom res Evolution
		root = SecurePreferencesFactory.getDefault();
		
		savedConnections.setItems(serverNames);
		savedConnections.add("", 0);
		savedConnections.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo c = (Combo) e.widget;
				int itemSelected = c.getSelectionIndex();
				Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
				Preferences savedServers = prefs.node(SERVERS);
				if(c.getItem(itemSelected)!=""){
					connectionType.setEnabled(false);
				}
				else{
					connectionType.setEnabled(true);
				}
				if(savedServers.get(c.getItem(itemSelected), "")!=null){
					String parameters = new String(savedServers.get(c.getItem(itemSelected), ""));
					StringTokenizer st = new StringTokenizer(parameters, ":");
					txtHost.setText(st.nextToken());
					txtPort.setText(st.nextToken());
					String connectionTypeName= st.nextToken();			
					connectionType.select(connectionType.indexOf(connectionTypeName));
					txtUser.setText(st.nextToken());
					//mgarcia: Optsicom res Evolution
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
				
		Group datosConexion = new Group(contents, SWT.SHADOW_IN);
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
		
		Group argumentos = new Group(contents, SWT.SHADOW_IN);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 2;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;
		argumentos.setLayoutData(gridDataHV);
		argumentos.setText("Arguments");
		gly = new GridLayout(2, false);
		argumentos.setLayout(gly);
		argumentos.setSize(200, 150);
		
		Button validate = new Button(contents, SWT.PUSH);
		validate.setText("Validate connection");
		validate.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent event) {
				IRunnableWithProgress op = new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						doValidate(monitor);
					}
				};
				
				try {
					getWizard().getContainer().run(false, false, op);
				} catch (Exception e) {
					RESClientPlugin.log(e);
				}
				
				// We refresh the wizard buttons so that Finish can have a chance to be activated
				getWizard().getContainer().updateButtons();
			}
			
		});
		gd = new GridData();
		gd.horizontalAlignment = SWT.LEFT;
		gd.horizontalSpan = 1;
		validate.setLayoutData(gd);
		
		Button saveSettings = new Button(contents, SWT.PUSH);
		saveSettings.setText("Save settings");
		saveSettings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				InputDialog input = new InputDialog(getShell(), "Save settings", "Enter the connection name", "", null);
				int result = input.open();
				if(result == InputDialog.OK) {
					String configName = input.getValue();
					String parameters = txtHost.getText() + ":" + txtPort.getText() + ":" + connectionType.getItem(connectionType.getSelectionIndex())+ ":" + txtUser.getText();
					Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
					Preferences savedServers = prefs.node(SERVERS);
					savedServers.put(configName, parameters);
					try {
						savedServers.flush();
					} catch (BackingStoreException e1) {
						RESClientPlugin.log(e1);
						MessageDialog.openError(getShell(), "Saving settings", "Unable to store settings: " + e1.getMessage());
					}
					//mgarcia: Optsicom res Evolution
					if (root != null) {
						ISecurePreferences node = root.node(OPTSICOM);
						if (!txtPass.getText().equalsIgnoreCase("")) {
							try {
								node.put(configName, txtPass.getText(), false);
								node.flush();
							} catch (StorageException e1) {
								RESClientPlugin.log(e1);
							} catch (IOException e2) {
								RESClientPlugin.log(e2);
							} 
						}
					}
				}
			}
		});
		gd = new GridData();
		gd.horizontalAlignment = SWT.LEFT;
		gd.grabExcessHorizontalSpace = true;
		saveSettings.setLayoutData(gd);
		
		Label lblUser = new Label(datosPersonales, SWT.LEFT);
		lblUser.setText("User:");
		txtUser = new Text(datosPersonales, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;	 
		txtUser.setLayoutData(gridDataHV);
		
		
		Label lblPass = new Label(datosPersonales, SWT.CENTER);
		lblPass.setText("Password:");
		txtPass = new Text(datosPersonales, SWT.BORDER | SWT.PASSWORD);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;	 		
		txtPass.setLayoutData(gridDataHV);
		
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
		txtPort = new Text(datosConexion, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;
		txtPort.setLayoutData(gridDataHV);
		
		Label lblPortDebug = new Label(datosConexion, SWT.LEFT);
		lblPortDebug.setText("VM Debug port:");
		txtPortDebug = new Text(datosConexion, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;
		txtPortDebug.setLayoutData(gridDataHV);
		if (!mode.equals("debug")){
			txtPortDebug.setEditable(false);
			txtPortDebug.setEnabled(false);
		}
		
		Label lblVMarg = new Label(argumentos, SWT.LEFT);
		lblVMarg.setText("VM arguments:");
		txtVMarg = new Text(argumentos, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;	 
		txtVMarg.setLayoutData(gridDataHV);
		
		Label lblPrgarg = new Label(argumentos, SWT.LEFT);
		lblPrgarg.setText("Program arguments:");
		txtPrgarg = new Text(argumentos, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;	 
		txtPrgarg.setLayoutData(gridDataHV);

		setControl(contents);
	}

	public String getPasswd() {
		return txtPass.getText();
	}
	
	public String getUser() {
		return txtUser.getText();
	}
	
	public String getConnectionType() {
		return connectionType.getItem(connectionType.getSelectionIndex());
	}

	public String getHost() {
		return txtHost.getText();
	}

	public String getPort() {
		return txtPort.getText();
	}

	public String getVMArgs() {
		return txtVMarg.getText();
	}

	public String getProgramArgs() {
		return txtPrgarg.getText();
	}

	public String getVMDebugPort() {
		return txtPortDebug.getText();
	}

	public boolean isConnectionValid() {
		return connectionValid;
	}

	private void doValidate(IProgressMonitor monitor) {
		try {
			/*connectionValid = false;
			monitor.beginTask("Validating connection", 2);
			OptsicomRemoteServer veex = (OptsicomRemoteServer) Naming.lookup("//"+txtHost.getText()+":"+txtPort.getText()+"/optsicom");
			monitor.worked(1);
			if(veex != null) {
				OptsicomRemoteExecutor executor = veex.getExecutor();
				monitor.worked(1);
				if(executor != null) {
					connectionValid = true;
					setPageComplete(true);
					setErrorMessage(null);
					setMessage("Connection validated succesfully");
				} else {
					setErrorMessage("Optsicom server returned null");
					RESClientPlugin.log("OptsicomRemoteServer.getExecutor() returned null");
				}
			} else {
				setErrorMessage("Naming returned null");
				RESClientPlugin.log("Naming returned null");
			}*/
			EvaluateContributionsHandler pluginHandler= new EvaluateContributionsHandler();
			IRemoteExecution executor= pluginHandler.getPlugin(this.getConnectionType());
			executor.setHost(txtHost.getText());
			executor.setPort(txtPort.getText());
			executor.setUser(txtUser.getText());
			executor.setPassword(txtPass.getText());
			if(executor.validateExecution()) {
				connectionValid = true;
				setPageComplete(true);
				setErrorMessage(null);
				setMessage("Connection validated succesfully");
			} 
			else {
				setErrorMessage("Optsicom server returned null");
				RESClientPlugin.log("IRemoteExecution.validateExecution() returned false");
			}
			/*connectionValid = true;
			setPageComplete(true);
			setErrorMessage(null);
			setMessage("Connection validated succesfully");*/
			
		} catch (Exception e) {
			RESClientPlugin.log(e);
			setErrorMessage(e.getMessage());
		}
	}

}
