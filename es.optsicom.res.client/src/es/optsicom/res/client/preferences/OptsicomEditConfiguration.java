package es.optsicom.res.client.preferences;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import es.optsicom.res.client.EvaluateContributionsHandler;
import es.optsicom.res.client.RESClientPlugin;
import es.optsicom.res.client.launcher.remote.IRemoteExecution;

public class OptsicomEditConfiguration extends TitleAreaDialog {

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
	private Combo savedConnections;
	private Combo connectionType;
	private boolean connectionValid = false;
	private String user;
	private String pass;
	private String host;
	private String port;
	private String connectionName;
	private String connection;
	private ISecurePreferences root;
	
	
	

	
	public OptsicomEditConfiguration(Shell parentShell,String user, String pass, String host, String port, String connectionName, String connectionType){
		super(parentShell);
		this.user=user;
		this.pass=pass;
		this.host=host;
		this.port=port;
		this.connectionName=connectionName;
		this.connection=connectionType;
	}
	@Override
	 public void create() {
	    super.create();
	    setTitle("Edit saved configuration");
	    setMessage("Configure the connection in this page and press the Validate button");
		
	  }
	public Control createDialogArea(Composite parent) {
		Composite contentsAux = (Composite) super.createDialogArea(parent);
		Composite contents = new Composite(contentsAux, SWT.NONE | SWT.BORDER);
		contents.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, false));
		GridLayout gly = new GridLayout(2, false);
		contents.setLayout(gly);
		contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label comboLabel = new Label(contents, SWT.LEFT);
		comboLabel.setText("Choose a previously saved configuration:");
		savedConnections = new Combo(contents, SWT.BORDER | SWT.READ_ONLY);
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
		connectionType.select(connectionType.indexOf(connection));
		
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
		savedConnections.select(savedConnections.indexOf(connectionName));
		savedConnections.setEnabled(false);
			 
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
		
		Label lblUser = new Label(datosPersonales, SWT.LEFT);
		lblUser.setText("User:");
		txtUser = new Text(datosPersonales, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;	 
		txtUser.setLayoutData(gridDataHV);
		txtUser.setText(user);
		
		Label lblPass = new Label(datosPersonales, SWT.CENTER);
		lblPass.setText("Password:");
		txtPass = new Text(datosPersonales, SWT.BORDER | SWT.PASSWORD);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;	 		
		txtPass.setLayoutData(gridDataHV);
		txtPass.setText(pass);
		
		Label lblHost = new Label(datosConexion, SWT.LEFT);
		lblHost.setText("Host:");
		txtHost = new Text(datosConexion, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;	 
		txtHost.setLayoutData(gridDataHV);
		txtHost.setText(host);
		
		Label lblPortRmi = new Label(datosConexion, SWT.LEFT);
		lblPortRmi.setText("Host RMI port:");
		txtPort = new Text(datosConexion, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;
		txtPort.setLayoutData(gridDataHV);
		txtPort.setText(port);
		
		Label lblPortDebug = new Label(datosConexion, SWT.LEFT);
		lblPortDebug.setText("VM Debug port:");
		txtPortDebug = new Text(datosConexion, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;
		txtPortDebug.setLayoutData(gridDataHV);
		
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
		
		Button validate = new Button(contentsAux, SWT.PUSH);
		validate.setText("Validate connection");
		validate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				doValidate();
			}
		});
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gd.horizontalAlignment = SWT.LEFT;
		validate.setLayoutData(gridDataHV);
		
		return contentsAux;
	}
	public Text getTxtUser() {
		return txtUser;
	}

	public void setTxtUser(Text txtUser) {
		this.txtUser = txtUser;
	}

	public Text getTxtPass() {
		return txtPass;
	}

	public void setTxtPass(Text txtPass) {
		this.txtPass = txtPass;
	}

	public Text getTxtHost() {
		return txtHost;
	}

	public void setTxtHost(Text txtHost) {
		this.txtHost = txtHost;
	}

	public Text getTxtPort() {
		return txtPort;
	}

	public void setTxtPort(Text txtPort) {
		this.txtPort = txtPort;
	}

	public void setConnectionType(Combo connectionType) {
		this.connectionType = connectionType;
	}

	@Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Editing saved configuration");
  }

  @Override
  protected Point getInitialSize() {
    return new Point(600, 600);
  }
  
  @Override
  protected void okPressed(){
  	String parameters = txtHost.getText() + ":" + txtPort.getText() + ":" + connectionType.getItem(connectionType.getSelectionIndex())+ ":" + txtUser.getText();
	Preferences prefs = new InstanceScope().getNode(RESClientPlugin.PLUGIN_ID);
	Preferences savedServers = prefs.node(SERVERS);
	savedServers.put(connectionName, parameters);
	try {
		savedServers.flush();
	} catch (BackingStoreException e1) {
		RESClientPlugin.log(e1);
		MessageDialog.openError(getShell(), "Editing saved configuration", "Unable to store settings: " + e1.getMessage());
	}
	//mgarcia: Optsicom res Evolution
	if (root != null) {
		ISecurePreferences node = root.node(OPTSICOM);
		if (!txtPass.getText().equalsIgnoreCase("")) {
			try {
				node.put(connectionName, txtPass.getText(), false);
				node.flush();
			} catch (StorageException e1) {
				RESClientPlugin.log(e1);
			} catch (IOException e2) {
				RESClientPlugin.log(e2);
			} 
		}
	}
	MessageDialog.openInformation(getShell(),"Editing settings" , "Configuration has been modified successfully");
	close();
  }
	  
  private void doValidate() {
		try {
			EvaluateContributionsHandler pluginHandler= new EvaluateContributionsHandler();
			IRemoteExecution executor= pluginHandler.getPlugin(this.getConnectionType());
			executor.setHost(txtHost.getText());
			executor.setPort(txtPort.getText());
			executor.setUser(txtUser.getText());
			executor.setPassword(txtPass.getText());
			if(executor.validateExecution()) {
				connectionValid = true;
				setErrorMessage(null);
				setMessage("Connection validated succesfully");
			} 
			else {
				setErrorMessage("Optsicom server returned null");
				RESClientPlugin.log("IRemoteExecution.validateExecution() returned false");
			}
			
		} catch (Exception e) {
			RESClientPlugin.log(e);
			setErrorMessage(e.getMessage());
		}
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
}
