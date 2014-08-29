package es.optsicom.res.client;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Execute;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import es.optsicom.res.client.launcher.remote.IRemoteExecution;

public class EvaluateContributionsHandler {
	
	@Execute
	  public void execute(IExtensionRegistry registry) {
	    evaluate(registry);
	  }
	  
	  private void evaluate(IExtensionRegistry registry) {
	    IConfigurationElement[] config =
	        registry.getConfigurationElementsFor("es.optsicom.res.client.extensionpoint.remoteExecutor");
	    try {
	      for (IConfigurationElement e : config) {
	        final Object o =
	            e.createExecutableExtension("class");
	        if (o instanceof IRemoteExecution) {
	          
	        }
	      }
	    } catch (CoreException ex) {
	      System.out.println(ex.getMessage());
	    }
	  }
	  
	  public String[] getPluginNameList() throws InvalidSyntaxException{
		  this.execute(Platform.getExtensionRegistry());
			BundleContext bundleContext = RESClientPlugin.getBundlecontext();
			ServiceReference[] serviceList=bundleContext.getServiceReferences(IRemoteExecution.class.getName(),null);
			String [] pluginNameList = new String[serviceList.length];
			for(int i=0; i<serviceList.length; i++){
				ServiceReference reference= serviceList[i];
				if(reference==null){
					
				}
				else{
					IRemoteExecution execution =(IRemoteExecution) bundleContext.getService(reference);
					pluginNameList[i]=execution.getName();
				}
			}	
			return pluginNameList;
		}
	  
	  public IRemoteExecution getPlugin(String name) throws InvalidSyntaxException{
		  IRemoteExecution executor=null;
		  this.execute(Platform.getExtensionRegistry());
			BundleContext bundleContext = RESClientPlugin.getBundlecontext();
			ServiceReference[] serviceList=bundleContext.getServiceReferences(IRemoteExecution.class.getName(),null);
			for(int i=0; i<serviceList.length; i++){
				ServiceReference reference= serviceList[i];
				if(reference==null){
					
				}
				else{
					IRemoteExecution execution =(IRemoteExecution) bundleContext.getService(reference);
					if (execution.getName().equalsIgnoreCase(name)){
						executor=execution;
					}
				}
			}	
			return executor;
	  }
	  
}
