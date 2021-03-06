/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * DeploymentHandler.java
 *
 */

package com.sun.enterprise.tools.admingui.handlers;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Properties;

import com.sun.enterprise.connectors.ConnectorRuntime;

import com.sun.enterprise.deployment.backend.DeploymentStatus;
import com.sun.enterprise.deployment.client.DeploymentFacility;
import com.sun.enterprise.deployment.client.DeploymentFacilityFactory;
import com.sun.enterprise.deployment.client.JESProgressObject;
import com.sun.enterprise.deployment.deploy.shared.AbstractArchive;
import com.sun.enterprise.deployment.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.util.DeploymentProperties;

import com.sun.appserv.management.config.J2EEApplicationConfig;
import com.sun.appserv.management.config.EJBModuleConfig;
import com.sun.appserv.management.config.WebModuleConfig;
import com.sun.appserv.management.config.RARModuleConfig;
import com.sun.appserv.management.config.AppClientModuleConfig;
import com.sun.appserv.management.config.ResourceAdapterConfig;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectName;


import com.sun.enterprise.tools.admingui.util.GuiUtil;
import com.sun.enterprise.tools.admingui.util.JMXUtil;
import com.sun.enterprise.tools.admingui.util.AMXUtil;
import com.sun.enterprise.tools.admingui.util.TargetUtil;

/**
 *
 */
public class DeploymentHandler {

	private static final HashMap<String, String> nextPageMap=new HashMap();
	static {
            nextPageMap.put(WebModuleConfig.J2EE_TYPE, "applications/webApplications.jsf");
            nextPageMap.put(J2EEApplicationConfig.J2EE_TYPE, "applications/enterpriseApplications.jsf");
            nextPageMap.put(EJBModuleConfig.J2EE_TYPE, "applications/ejbModules.jsf");
            nextPageMap.put(AppClientModuleConfig.J2EE_TYPE, "applications/appclientModules.jsf");
            nextPageMap.put(RARModuleConfig.J2EE_TYPE, "applications/connectorModulePropsTable.jsf");
	}

    // using DeploymentFacility API
     protected static void deploy(String[] targets, Properties deploymentProps, String location,  HandlerContext handlerCtx) throws Exception {
            
     	deploymentProps.setProperty(DeploymentProperties.FORCE, "false");
        String appType = deploymentProps.getProperty("appType");
        
        deploymentProps.remove("appType");
        boolean status = invokeDeploymentFacility(targets, deploymentProps, location, handlerCtx);
        if(status){
            //String mesg = GuiUtil.getMessage("msg.deploySuccess", new Object[] {"", "deployed"});
            //GuiUtil.prepareAlert(handlerCtx, "success", mesg, null);
        }
        String type = AMXUtil.getAppType(deploymentProps.getProperty(DeploymentProperties.NAME));
        
        String nextPage=(String) handlerCtx.getInputValue("listPageLink");
        if ( RARModuleConfig.J2EE_TYPE.equals(type)){
                nextPage = nextPageMap.get(type);
                Properties rarProps = new Properties();
                setProperty(rarProps, "threadPool", (String)handlerCtx.getInputValue("threadpool"), "");
                setProperty(rarProps, "registry", (String)handlerCtx.getInputValue("registryType"), "");
                setProperty(rarProps, "target", (String)handlerCtx.getInputValue("target"), "");
                setProperty(rarProps, "listPageLink", (String)handlerCtx.getInputValue("listPageLink"), "");
                setProperty(rarProps, "cancelPage", (String)handlerCtx.getInputValue("cancelPage"), "applications/connectorModules.jsf");
                setProperty(rarProps, "filePath", location, "");
                setProperty(rarProps, "name", deploymentProps.getProperty(DeploymentProperties.NAME), "");
                if (status)
                    GuiUtil.prepareAlert(handlerCtx, "success", GuiUtil.getMessage("msg.deploy.connectorModule"), null);
                handlerCtx.setOutputValue("rarProps", rarProps);
        }else{
            if (GuiUtil.isEmpty(nextPage)){
                nextPage = nextPageMap.get(type);
            }
        }
        handlerCtx.setOutputValue("nextPage", nextPage);
    }
     
     static private void setProperty(Properties rarProps, String key, String value, String defValue)
     {
         rarProps.setProperty(key, (value==null) ? defValue : value);
     }
     
     protected static boolean invokeDeploymentFacility(String[] targets, Properties props, String archivePath, HandlerContext handlerCtx) 
     	throws Exception {
     	if(archivePath == null) {
            //Localize this message.
            GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("msg.deploy.nullArchiveError"));
     	}
        
        if (targets == null){
            String defaultTarget =  (AMXUtil.isEE()) ? "domain" : "server";
            targets = new String[] {defaultTarget};
        }
        
     	archivePath = archivePath.replace('\\', '/' );
        AbstractArchive archive = (new ArchiveFactory()).openArchive(archivePath); 
        DeploymentFacility df= DeploymentFacilityFactory.getLocalDeploymentFacility();
        JESProgressObject progressObject = null;
        
        progressObject = df.deploy(df.createTargets(targets), archive, null , props);  //null for deployment plan
        DeploymentStatus status = null;
        do {
            status = progressObject.getCompletedStatus();
            if(status == null) {
                try {
                    Thread.currentThread().sleep(1000);
                } catch(InterruptedException ie) {
                }
            }
        } while (status == null);
        boolean ret = checkDeployStatus(status, handlerCtx, true);
     	return ret;
     }

     private static boolean checkDeployStatus(DeploymentStatus status, HandlerContext handlerCtx, boolean stopProcessing) 
     {
        // parse the deployment status and retrieve failure/warning msg
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bos);
        DeploymentStatus.parseDeploymentStatus(status, pw);
        byte[] statusBytes = bos.toByteArray();
        String statusString = new String(statusBytes);
        
         if (status!=null && status.getStatus() == DeploymentStatus.FAILURE){ 
            if (stopProcessing) 
                GuiUtil.handleError(handlerCtx, statusString);
            else
                GuiUtil.prepareAlert(handlerCtx,"error", GuiUtil.getMessage("msg.Error"), statusString);
                
            return false;
         }
         if (status!=null && status.getStatus() == DeploymentStatus.WARNING){
            //We may need to log this mesg.
            GuiUtil.prepareAlert(handlerCtx, "warning", GuiUtil.getMessage("deploy.warning"), statusString);
            return false;
         }
         return true;
     }
     
    /**
     *  <p> This handler redeploy any application
     */
     @Handler(id="redeploy",
        input={
        @HandlerInput(name="filePath", type=String.class, required=true),
        @HandlerInput(name="origPath", type=String.class, required=true),
        @HandlerInput(name="appName", type=String.class, required=true)})
        
    public static void redeploy(HandlerContext handlerCtx) {
         try{
             String filePath = (String) handlerCtx.getInputValue("filePath");
             String origPath = (String) handlerCtx.getInputValue("origPath");
             String appName = (String) handlerCtx.getInputValue("appName");
             Properties deploymentProps = new Properties();
             //If we are redeploying a web app, we want to preserve context root. 
             WebModuleConfig module = AMXUtil.getDomainConfig().getWebModuleConfigMap().get(appName);
	     if (module != null){
                deploymentProps.setProperty(DeploymentProperties.CONTEXT_ROOT, ((WebModuleConfig) module).getContextRoot());
             }
             deploymentProps.setProperty(DeploymentProperties.ARCHIVE_NAME, origPath);
             deploymentProps.setProperty(DeploymentProperties.FORCE, "true");
             deploymentProps.setProperty(DeploymentProperties.NAME, appName);
             invokeDeploymentFacility(null, deploymentProps, filePath, handlerCtx);
        } catch (Exception ex) {
                GuiUtil.handleException(handlerCtx, ex);
        }
     }
        
     
     /**
     *	<p> This handler takes in selected rows, and do the undeployment
     *  <p> Input  value: "selectedRows" -- Type: <code>java.util.List</code></p>
     *  <p> Input  value: "appType" -- Type: <code>String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="undeploy",
    input={
        @HandlerInput(name="selectedRows", type=List.class, required=true),
        @HandlerInput(name="appType", type=String.class, required=true)})
        
    public static void undeploy(HandlerContext handlerCtx) {
        
        Object obj = handlerCtx.getInputValue("selectedRows");

        //appType can be one of the following: application,webApp,ejbModule,connector,appClient
        String appType = (String)handlerCtx.getInputValue("appType");
		Properties dProps = null;

		if(appType.equals("connector")) {
			dProps = new Properties();
			//Default cascade is true. May be we can issue a warning,
			//bcz undeploy will fail anyway if cascade is false.
			dProps.put(DeploymentProperties.CASCADE, "true");
		}
        
        List selectedRows = (List) obj;
        JESProgressObject progressObject = null;
         DeploymentFacility df= DeploymentFacilityFactory.getLocalDeploymentFacility();
        //Hard coding to server, fix me for actual targets in EE.
        String[] targetNames = new String[] {"server"};
        
        for(int i=0; i< selectedRows.size(); i++){
            Map oneRow = (Map) selectedRows.get(i);
            String appName = (String) oneRow.get("name");
            //Undeploy the app here.
            if(AMXUtil.isEE()){
                List<String> refList = TargetUtil.getDeployedTargets(appName, true);
                if(refList.size() > 0)
                    targetNames = refList.toArray(new String[refList.size()]);
                else
                    targetNames=new String[]{"domain"};
            }
            progressObject = df.undeploy(df.createTargets(targetNames), appName, dProps);
            DeploymentStatus status = df.waitFor(progressObject);
            //we DO want it to continue and call the rest handlers, ie navigate(). This will
            //re-generate the table data because there may be some apps thats been undeployed 
            //successfully.  If we stopProcessing, the table data is stale and still shows the
            //app that has been gone.
            if( checkDeployStatus(status, handlerCtx, false)){
                String mesg = GuiUtil.getMessage("msg.deploySuccess", new Object[]{appName, "undeployed"});
                //we need to fix cases where more than 1 app is undeployed before putting this msg.                
                //GuiUtil.prepareAlert(handlerCtx, "success", mesg, null); 
            }
        }
    }

    /**
     *	<p> This method returns the resource-adapter properties </p>
     *
     *  <p> input value: "adapterProperties" -- Type: <code>java.util.Map</code>/</p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="createResourceAdapterConfig",
    input={
        @HandlerInput(name="dProps", type=Properties.class),
        @HandlerInput(name="AddProps",    type=Map.class)},
    output={
        @HandlerOutput(name="nextPage", type=String.class)}
    )
    public static void createResourceAdapterConfig(HandlerContext handlerCtx) {
        Properties dProps = (Properties)handlerCtx.getInputValue("dProps");

        String name = dProps.getProperty("name");
        ResourceAdapterConfig ra = AMXUtil.getDomainConfig().createResourceAdapterConfig(name, null);

        String threadPool = dProps.getProperty("threadPool");
        if(!GuiUtil.isEmpty(threadPool))
            ra.setThreadPoolIDs(threadPool);

        String registry = dProps.getProperty("registry");
        if (!GuiUtil.isEmpty(registry)){
            ra.createProperty(registry, "true");
        }
        
        Map<String,String> addProps = (Map)handlerCtx.getInputValue("AddProps");
        if(addProps != null ){
             for(String key: addProps.keySet()){
                 String value = addProps.get(key);
                 if (!GuiUtil.isEmpty(value))
                    ra.createProperty(key,value);
             }  
         }
        handlerCtx.setOutputValue("nextPage", "applications/connectorModules.jsf");
    }
    
    

    /**
     *	<p> This method returns the resource-adapter properties </p>
     *
     *  <p> Output value: "adapterProperties" -- Type: <code>java.util.List</code>/</p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getAdapterProperties",
    input={
        @HandlerInput(name="dProps", type=Properties.class)},
    output={
        @HandlerOutput(name="properties", type=java.util.Map.class),
        @HandlerOutput(name="dProps", type=Properties.class)})

        public static void getAdapterProperties(HandlerContext handlerCtx) {

            Properties dProps = (Properties)handlerCtx.getInputValue("dProps");
            String filePath = dProps.getProperty("filePath");
            Map props = new HashMap();
            try{
                props = ConnectorRuntime.getRuntime().getResourceAdapterBeanProperties(filePath);
                clearValues(props);
            }catch(Exception ex){
                //TODO: Log exception,  for now just ignore and return empty properties list
            }
            handlerCtx.setOutputValue("properties", props);
            handlerCtx.setOutputValue("dProps", dProps);
    }    

    /**
     *	<p> This method returns the deployment descriptors for a given app. </p>
     *
     *  <p> Output value: "descriptors" -- Type: <code>java.util.List</code>/</p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getDescriptors",
    input={
        @HandlerInput(name="appName", type=String.class, required=true),
        @HandlerInput(name="includeSubComponent", type=Boolean.class)},
    output={
        @HandlerOutput(name="descriptors", type=List.class)})
        public static void getDescriptors(HandlerContext handlerCtx) {
            String appName = (String)handlerCtx.getInputValue("appName");
            Boolean includeSubComponent = (Boolean)handlerCtx.getInputValue("includeSubComponent");
            if (includeSubComponent == null){
                includeSubComponent = false;
            }
            List list = new ArrayList();
            String[] descriptors = null;
            try{
                descriptors = getDescriptors(appName, null);
                for(int i = 0;descriptors != null && i < descriptors.length;i++){ 
                    HashMap map = new HashMap();
                    map.put("name", appName);
                    map.put("moduleName", "");
                    int index = descriptors[i].lastIndexOf(File.separator)+1;
                    map.put("descriptor", descriptors[i].substring(index));
                    map.put("descriptorPath", descriptors[i]);
                    list.add(map);
                }
                if (includeSubComponent){
                    String[] modules = (String[])JMXUtil.invoke("com.sun.appserv:type=applications,category=config", "getModuleComponents",
			new Object[]{appName}, new String[]{"java.lang.String"});
                    if(modules != null) {
                        for (int i=0; i < modules.length; i++) {
                            String subComponentName = new ObjectName(modules[i]).getKeyProperty("name");
                            String[] subDesc = getDescriptors(appName, subComponentName);
                            for(int j=0; subDesc != null && j < subDesc.length; j++) {
                                HashMap map = new HashMap();
                                map.put("name", appName);
                                map.put("moduleName", subComponentName);
                                int index = subDesc[j].lastIndexOf(File.separator)+1;
                                map.put("descriptor", subDesc[j].substring(index));
                                map.put("descriptorPath", subDesc[j]);
                                list.add(map);
                            }
                        }
                    }
              }
            }catch(Exception ex){
                GuiUtil.handleException(handlerCtx, ex);
            }
            handlerCtx.setOutputValue("descriptors", list);
}   
    
    
    /**
     *	<p> This handler creates references for the given application/module name 
     *
     *  <p> Input value: "name" -- Type: <code>String</code>/</p>
     *  <p> Input value: "targets" -- Type: <code>String[]</code>/</p>
     *  <p> Output value: "name" -- Type: <code>String</code>/</p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="createApplicationReferences",
        input={
        @HandlerInput(name="name", type=String.class, required=true),
        @HandlerInput(name="targets", type=String[].class, required=true )})
    public static void createApplicationReferences(HandlerContext handlerCtx) {
        String name = (String)handlerCtx.getInputValue("name");
        String[] selTargets = (String[])handlerCtx.getInputValue("targets");
        List<String> targets = Arrays.asList(selTargets);
        List<String> associatedTargets = TargetUtil.getDeployedTargets(name, true);
        try{
            List addTargets = new ArrayList();
            for(String targetName:targets) {
                if(!(associatedTargets.contains(targetName))) {
                       addTargets.add(targetName);
                }
            }
            handleAppRefs(name, (String[])addTargets.toArray(new String[addTargets.size()]), handlerCtx, true, null);
            
            //removes the old application references
            List removeTargets = new ArrayList();
            for(String targetName:associatedTargets) {
                if(!(targets.contains(targetName))) {
                    removeTargets.add(targetName);
                }
            }
            handleAppRefs(name, (String[])removeTargets.toArray(new String[removeTargets.size()]), handlerCtx, false, null);
            
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    
    //Status of app-ref created will be the same as the app itself.
    static public void handleAppRefs(String appName, String[] targetNames, HandlerContext handlerCtx, boolean addFlag, Boolean enableFlag) {
        if (targetNames != null && targetNames.length > 0){
            DeploymentFacility df= DeploymentFacilityFactory.getLocalDeploymentFacility();
            JESProgressObject progressObject = null;
            Properties dProps = new Properties();

            if (enableFlag != null)
                dProps.setProperty(DeploymentProperties.ENABLE, enableFlag.toString());
            
            if (addFlag)
                progressObject = df.createAppRef(df.createTargets(targetNames), appName, dProps);
            else
                progressObject = df.deleteAppRef(df.createTargets(targetNames), appName, dProps);
            DeploymentStatus status = df.waitFor(progressObject);
            checkDeployStatus(status, handlerCtx, true);
        }
    }
    

	private static String[] getDescriptors(String appName, String subComponent) {
		String methodName = "getDeploymentDescriptorLocations";
		String objectName = "com.sun.appserv:type=applications,category=config";
		Object[] params = {appName, subComponent};
		String[] types = {"java.lang.String", "java.lang.String"};
		String[] descriptors = (String[])JMXUtil.invoke(objectName, methodName,
								params, types);

		return descriptors;
	}

    /**
     *	<p> This method displays the deployment descriptors for a given app. </p>
     *
     *  <p> Output value: "descriptors" -- Type: <code>String.class</code>/</p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="descriptorDisplay",
    input={
        @HandlerInput(name="filePath", type=String.class),
        @HandlerInput(name="appName", type=String.class),
        @HandlerInput(name="pageName", type=String.class)},
    output={
        @HandlerOutput(name="descriptor", type=String.class),
        @HandlerOutput(name="appName", type=String.class),
        @HandlerOutput(name="pageName", type=String.class)})

        public static void descriptorDisplay(HandlerContext handlerCtx) {
			String filePath = (String)handlerCtx.getInputValue("filePath");
			String appName = (String)handlerCtx.getInputValue("appName");
			String pageName = (String)handlerCtx.getInputValue("pageName");
			String objectName = "com.sun.appserv:type=applications,category=config";
			String methodName = "getDeploymentDescriptor";
			Object[] params = {filePath};
			String[] types = {"java.lang.String"};
	
			String descriptor=(String)JMXUtil.invoke(objectName, methodName,
											 params, types);
			handlerCtx.setOutputValue("descriptor", descriptor);
			handlerCtx.setOutputValue("appName", appName);
			handlerCtx.setOutputValue("pageName", pageName);
		}


	private static void clearValues(Map map) {
            //refer to bugid 6212118
            //The value returned by connector runtime is the type for that property, we want to 
            //empty it out so that user can fill in the value 

            Set<String> keySet = map.keySet();
            for(String key: keySet) {
                    map.put(key, "");
            }
	}
}
