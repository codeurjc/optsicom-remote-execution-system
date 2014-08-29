package es.optsicom.res.client.launcher.remote.tabs;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.InvalidSyntaxException;

import es.optsicom.res.client.EvaluateContributionsHandler;
import es.optsicom.res.client.RESClientPlugin;
import es.optsicom.res.client.launcher.remote.delegate.IJavaRemoteServerConfigurationConstants;



public class RemoteServiceTab extends AbstractLaunchConfigurationTab {

	private Text txtPassword;
	private Text txtHost;
	private Text txtPortRmi;
	private Text txtPortDebug;
	private String mode;
	private Combo connectionType;
	
	protected static final String EMPTY_STRING = "";
	protected static final Map<String,String> EMPTY_MAP = new HashMap<String,String>();
    
	protected RemoteServiceTab(String mode) {
		super();
		this.mode = mode;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		
		comp.setLayout(new GridLayout(2, true));
		comp.setFont(parent.getFont());
		
		createRemoteServiceComponent(comp);
		
	}
	
	private void createRemoteServiceComponent(Composite contents) {
		
		GridLayout gly = new GridLayout(2, false);
		contents.setLayout(gly);
		contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
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
		
	
		
		Label lblPassword = new Label(datosPersonales, SWT.LEFT);
		lblPassword.setText("Password:");
		txtPassword = new Text(datosPersonales, SWT.BORDER | SWT.PASSWORD);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;	 
		txtPassword.setLayoutData(gridDataHV);
		txtPassword.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		
		Group datosConexion = new Group(contents, SWT.SHADOW_IN);
		gridDataHV = new GridData();
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
		
		Label connectionLabel = new Label(datosConexion, SWT.LEFT);
		connectionLabel.setText("Choose a connection:");
		connectionType = new Combo(datosConexion, SWT.BORDER | SWT.READ_ONLY);
		gridDataHV = new GridData();
		gridDataHV.horizontalAlignment = SWT.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;
		connectionType.setLayoutData(gridDataHV);
		
		String [] connectionTypeList=null;
		EvaluateContributionsHandler pluginHandler= new EvaluateContributionsHandler();
		try {
			connectionTypeList=pluginHandler.getPluginNameList();
		} catch (InvalidSyntaxException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}	
		connectionType.setItems(connectionTypeList);
		
		Label lblHost = new Label(datosConexion, SWT.LEFT);
		lblHost.setText("Host:");
		txtHost = new Text(datosConexion, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;	 
		txtHost.setLayoutData(gridDataHV);
		txtHost.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		
		Label lblPortRmi = new Label(datosConexion, SWT.LEFT);
		lblPortRmi.setText("Host RMI port:");
		txtPortRmi = new Text(datosConexion, SWT.BORDER | SWT.SINGLE);
		gridDataHV = new GridData();
		gridDataHV.horizontalSpan = 1;
		gridDataHV.horizontalAlignment = GridData.FILL;
		gridDataHV.grabExcessHorizontalSpace = true;
		txtPortRmi.setLayoutData(gridDataHV);
		txtPortRmi.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});
		
		if("debug".equals(mode)){
			Label lblPortDebug = new Label(datosConexion, SWT.LEFT);
			lblPortDebug.setText("VM Debug port:");
			txtPortDebug = new Text(datosConexion, SWT.BORDER | SWT.SINGLE);
			gridDataHV = new GridData();
			gridDataHV.horizontalSpan = 1;
			gridDataHV.horizontalAlignment = GridData.FILL;
			gridDataHV.grabExcessHorizontalSpace = true;
			txtPortDebug.setLayoutData(gridDataHV);
			txtPortDebug.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					setDirty(true);
					updateLaunchConfigurationDialog();
				}
			});
		}
		
	}
	
	
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		Map<String,String> attrMap = new HashMap<String,String>();
        configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_REMOTE_SERVER, EMPTY_STRING);
        configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PORT_RMI, EMPTY_STRING);
        configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PASSWORD, EMPTY_STRING);
        configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_CONNECTION_TYPE, EMPTY_STRING);
        if (mode.equals("debug")){
        	configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PORT_DEBUG, EMPTY_STRING);
        	configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, attrMap);
		}
       
    }

	public void initializeFrom(ILaunchConfiguration config) {
		updateRemoteServiceConfig(config);
    }

    protected void updateRemoteServiceConfig(ILaunchConfiguration config) {
    	try {
    		txtHost.setText(config.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_REMOTE_SERVER, EMPTY_STRING));
    		txtPortRmi.setText(config.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PORT_RMI, EMPTY_STRING));
    		txtPassword.setText(config.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PASSWORD, EMPTY_STRING));
    		String connectionTypeName= config.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_CONNECTION_TYPE, EMPTY_STRING);			
			connectionType.select(connectionType.indexOf(connectionTypeName));
    		if (mode.equals("debug")){
    			txtPortDebug.setText(config.getAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PORT_DEBUG, EMPTY_STRING));
    			ILaunchConfigurationWorkingCopy configuration = config.getWorkingCopy();
    			configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_DEBUG_CONFIGURATION, true);
				configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, JavaRuntime.getDefaultVMConnector().getIdentifier());
    		    config = configuration.doSave();
    		}    
		} catch (CoreException ce) {
    		RESClientPlugin.log(ce);
        }
    }

   @SuppressWarnings("unchecked")
   public void performApply(ILaunchConfigurationWorkingCopy configuration) {
	  	configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_REMOTE_SERVER, txtHost.getText().trim());
		configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PORT_RMI, txtPortRmi.getText().trim());
		configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PASSWORD, txtPassword.getText().trim());
		configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_CONNECTION_TYPE, connectionType.getItem(connectionType.getSelectionIndex()).trim());
		if (mode.equals("debug")){
			try {
				configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_PORT_DEBUG, txtPortDebug.getText().trim());
				Map<String,String> attrMap = new HashMap<String,String>();
				attrMap = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, EMPTY_MAP);
				attrMap.put("port", txtPortDebug.getText().trim());
				attrMap.put("hostname", txtHost.getText().trim());
				configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, attrMap);
				configuration.setAttribute(IJavaRemoteServerConfigurationConstants.ATTR_DEBUG_CONFIGURATION, true);
				configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, JavaRuntime.getDefaultVMConnector().getIdentifier());
			} catch (CoreException ce) {
				RESClientPlugin.log(ce);
	        }
		}
	}

	public String getName() {
		return "Remote service";
	}
	
}