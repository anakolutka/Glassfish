/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.admingui.handlers;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.glassfish.admin.amx.base.Runtime;
import org.glassfish.deployment.client.DFDeploymentStatus;
import org.glassfish.deployment.client.DeploymentFacility;
import org.glassfish.deployment.client.DFProgressObject;
import org.glassfish.deployment.client.DFDeploymentProperties;


import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admingui.common.handlers.ProxyHandlers;
import org.glassfish.admingui.common.util.DeployUtil;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.V3AMX;

/**
 *
 */
public class DeploymentHandler {

    //should be the same as in DeploymentProperties in deployment/common
    public static final String KEEP_SESSIONS = "keepSessions";

        /**
     *	<p> This method deploys the uploaded file </p>
     *      to a give directory</p>
     *	<p> Input value: "file" -- Type: <code>String</code></p>
     *	<p> Input value: "appName" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "appType" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "ctxtRoot" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "VS" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "enabled" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "verifier" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "jws" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "precompileJSP" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "libraries" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "description" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "rmistubs" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "threadpool" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "registryType" -- Type: <code>java.lang.String</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id = "deploy", input = {
        @HandlerInput(name = "filePath", type = String.class),
        @HandlerInput(name = "origPath", type = String.class),
        @HandlerInput(name = "appName", type = String.class),
        @HandlerInput(name = "ctxtRoot", type = String.class),
        @HandlerInput(name = "VS", type = String.class),
        @HandlerInput(name = "enabled", type = String.class),
        @HandlerInput(name = "precompileJSP", type = String.class),
        @HandlerInput(name = "libraries", type = String.class),
        @HandlerInput(name = "description", type = String.class),
        @HandlerInput(name="targets", type=String[].class )
        })
    public static void deploy(HandlerContext handlerCtx) {

        Properties deploymentProps = new Properties();
        String appName = (String) handlerCtx.getInputValue("appName");
        String origPath = (String) handlerCtx.getInputValue("origPath");
        String filePath = (String) handlerCtx.getInputValue("filePath");
        String ctxtRoot = (String) handlerCtx.getInputValue("ctxtRoot");
        String[] vs = (String[]) handlerCtx.getInputValue("VS");
        String enabled = (String) handlerCtx.getInputValue("enabled");
        
        String libraries = (String) handlerCtx.getInputValue("libraries");
        String precompile = (String) handlerCtx.getInputValue("precompileJSP");
        String desc = (String) handlerCtx.getInputValue("description");
        String[] targets = (String[]) handlerCtx.getInputValue("targets");
        if (targets == null || targets.length == 0 || !V3AMX.getInstance().isEE()) {
            targets = null;
        }
        if (GuiUtil.isEmpty(origPath)) {
            String mesg = GuiUtil.getMessage("msg.deploy.nullArchiveError");
            GuiUtil.handleError(handlerCtx, mesg);
            return;
        }

        deploymentProps.setProperty(DFDeploymentProperties.NAME, appName != null ? appName : "");
        //If user doesn't set the context root, do not pass anything to DF in the deploymentProperties.  
        //Otherwise, a "/" will be set as the context root instead of looking into sun-web.xml or using the filename.
        if (!GuiUtil.isEmpty(ctxtRoot))
            deploymentProps.setProperty(DFDeploymentProperties.CONTEXT_ROOT, ctxtRoot);
        deploymentProps.setProperty(DFDeploymentProperties.ENABLED, enabled != null ? enabled : "false");
        deploymentProps.setProperty(DFDeploymentProperties.DEPLOY_OPTION_LIBRARIES, libraries != null ? libraries : "");
        deploymentProps.setProperty(DFDeploymentProperties.DESCRIPTION, desc != null ? desc : "");
        deploymentProps.setProperty(DFDeploymentProperties.PRECOMPILE_JSP, precompile != null ? precompile : "false");
        //do not send VS if user didn't specify, refer to bug#6542276
        if (vs != null && vs.length > 0) {
            if (!GuiUtil.isEmpty(vs[0])) {
                String vsTargets = GuiUtil.arrayToString(vs, ",");
                deploymentProps.setProperty(DFDeploymentProperties.VIRTUAL_SERVERS, vsTargets);
            }
        }
        try {
            DeployUtil.deploy(targets, deploymentProps, filePath, handlerCtx);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    @Handler(id = "deploy2",
    input = {
        @HandlerInput(name = "filePath", type = String.class),
        @HandlerInput(name = "origPath", type = String.class),
        @HandlerInput(name = "allMaps", type = Map.class),
        @HandlerInput(name = "appType", type = String.class),
        @HandlerInput(name="propertyList", type=List.class) })
    public static void deploy2(HandlerContext handlerCtx) {

        String appType = (String) handlerCtx.getInputValue("appType");
        String origPath = (String) handlerCtx.getInputValue("origPath");
        String filePath = (String) handlerCtx.getInputValue("filePath");
        Map allMaps = (Map) handlerCtx.getInputValue("allMaps");
        Map attrMap = new HashMap((Map) allMaps.get(appType));

        if (GuiUtil.isEmpty(origPath)) {
            String mesg = GuiUtil.getMessage("msg.deploy.nullArchiveError");
            GuiUtil.handleError(handlerCtx, mesg);
            return;
        }

        DFDeploymentProperties deploymentProps = new DFDeploymentProperties();

        /* Take care some special properties, such as VS  */

        //do not send VS if user didn't specify, refer to bug#6542276
        String[] vs = (String[])attrMap.get(DFDeploymentProperties.VIRTUAL_SERVERS);
        if (vs != null && vs.length > 0) {
            if (!GuiUtil.isEmpty(vs[0])) {
                String vsTargets = GuiUtil.arrayToString(vs, ",");
                deploymentProps.setProperty(DFDeploymentProperties.VIRTUAL_SERVERS, vsTargets);
            }
        }
        attrMap.remove(DFDeploymentProperties.VIRTUAL_SERVERS);

        //Take care of checkBox
        List<String> convertToFalseList = (List) attrMap.get("convertToFalseList");
        if (convertToFalseList != null){
            for(String one : convertToFalseList ){
                if (attrMap.get(one) == null){
                    attrMap.put(one, "false");
                }
            }
            attrMap.remove("convertToFalseList");
        }

        Properties props = new Properties();
        for(Object attr : attrMap.keySet()){
            String key = (String)attr;
            String prefix = "PROPERTY-";
            String value = (String) attrMap.get(key);
            if (value == null)
                continue;
            if (key.startsWith(prefix) ){
                props.setProperty( key.substring( prefix.length()), value);
                
            }else{
                deploymentProps.setProperty(key, value);
            }
        }
        


        // include any  additional property that user enters
        List<Map<String,String>> propertyList = (List)handlerCtx.getInputValue("propertyList");
        if (propertyList != null){
            Set propertyNames = new HashSet();
            for(Map<String, String> oneRow : propertyList){
                final String  name = oneRow.get(ProxyHandlers.PROPERTY_NAME);
                if (GuiUtil.isEmpty(name)){
                    continue;
                }
                if (propertyNames.contains(name)){
                    GuiUtil.getLogger().warning("Ignored Duplicate Property Name : " + name);
                    continue;
                }else{
                    propertyNames.add(name);
                }
                String value = oneRow.get(ProxyHandlers.PROPERTY_VALUE);
                if (GuiUtil.isEmpty(value)){
                    continue;
                }
                props.setProperty( name, value);
                
            }
        }
        
        if (props.size() > 0){
            deploymentProps.setProperties(props);
        }

        try {
            DeployUtil.deploy(null, deploymentProps, filePath, handlerCtx);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
        
    }



    /**
     *  <p> This handler redeploy any application
     */
     @Handler(id="redeploy",
        input={
        @HandlerInput(name="filePath", type=String.class, required=true),
        @HandlerInput(name="origPath", type=String.class, required=true),
        @HandlerInput(name="appName", type=String.class, required=true),
        @HandlerInput(name="keepSessions", type=Boolean.class)})
        
    public static void redeploy(HandlerContext handlerCtx) {
         try{
             String filePath = (String) handlerCtx.getInputValue("filePath");
             String origPath = (String) handlerCtx.getInputValue("origPath");
             String appName = (String) handlerCtx.getInputValue("appName");
             Boolean keepSessions = (Boolean) handlerCtx.getInputValue("keepSessions");
             DFDeploymentProperties deploymentProps = new DFDeploymentProperties();

             //If we are redeploying a web app, we want to preserve context root.
             //can use Query instead of hard code object name
             //AMXProxy app = V3AMX.getInstance().getApplication(appName);
	     AMXProxy app = V3AMX.objectNameToProxy("amx:pp=/domain/applications,type=application,name="+appName);
             String ctxRoot = (String) app.attributesMap().get("ContextRoot");
             if (ctxRoot != null){
                 deploymentProps.setContextRoot(ctxRoot);
             }
             deploymentProps.setForce(true);
             deploymentProps.setUpload(false);
             deploymentProps.setName(appName);
             
             Properties prop = new Properties();
             String ks = (keepSessions==null) ? "false" : keepSessions.toString();
             prop.setProperty(KEEP_SESSIONS, ks);
             deploymentProps.setProperties(prop);
             
             DeployUtil.invokeDeploymentFacility(null, deploymentProps, filePath, handlerCtx);
        } catch (Exception ex) {
                GuiUtil.handleException(handlerCtx, ex);
        }
     }
        
     
     /**
     *	<p> This handler takes in selected rows, and do the undeployment
     *  <p> Input  value: "selectedRows" -- Type: <code>java.util.List</code></p>
     *  <p> Input  value: "appType" -- Type: <code>String</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="undeploy",
    input={
        @HandlerInput(name="selectedRows", type=List.class, required=true)})
        
    public static void undeploy(HandlerContext handlerCtx) {
        
        Object obj = handlerCtx.getInputValue("selectedRows");

        Properties dProps = new Properties();
//        if(appType.equals("connector")) {
//                //Default cascade is true. May be we can issue a warning,
//                //bcz undeploy will fail anyway if cascade is false.
//                dProps.put(DFDeploymentProperties.CASCADE, "true");
//        }
        
        List selectedRows = (List) obj;
        DFProgressObject progressObject = null;
        DeploymentFacility df= GuiUtil.getDeploymentFacility();
        //Hard coding to server, fix me for actual targets in EE.
        String[] targetNames = new String[] {"server"};
        
        for(int i=0; i< selectedRows.size(); i++){
            Map oneRow = (Map) selectedRows.get(i);
            String appName = (String) oneRow.get("name");
            //Undeploy the app here.
//            if(V3AMX.getInstance().isEE()){
//                List<String> refList = TargetUtil.getDeployedTargets(appName, true);
//                if(refList.size() > 0)
//                    targetNames = refList.toArray(new String[refList.size()]);
//                else
//                    targetNames=new String[]{"domain"};
//            }

            progressObject = df.undeploy(df.createTargets(targetNames), appName, dProps);
            
            progressObject.waitFor();
            DFDeploymentStatus status = progressObject.getCompletedStatus();
            //we DO want it to continue and call the rest handlers, ie navigate(). This will
            //re-generate the table data because there may be some apps thats been undeployed 
            //successfully.  If we stopProcessing, the table data is stale and still shows the
            //app that has been gone.
            if( DeployUtil.checkDeployStatus(status, handlerCtx, false)){
                String mesg = GuiUtil.getMessage("msg.deploySuccess", new Object[]{appName, "undeployed"});
                //we need to fix cases where more than 1 app is undeployed before putting this msg.                
                //GuiUtil.prepareAlert(handlerCtx, "success", mesg, null); 
            }
        }
    }
    
    
        
    /**
     *	<p> This handler takes in selected rows, and change the status of the app
     *  <p> Input  value: "selectedRows" -- Type: <code>java.util.List</code></p>
     *  <p> Input  value: "appType" -- Type: <code>String</code></p>
     *  <p> Input  value: "enabled" -- Type: <code>Boolean</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="changeAppStatus",
    input={
        @HandlerInput(name="selectedRows", type=List.class, required=true),
        @HandlerInput(name="enabled", type=Boolean.class, required=true)})
        
    public static void changeAppStatus(HandlerContext handlerCtx) {
        
        List obj = (List) handlerCtx.getInputValue("selectedRows");
        boolean enabled = ((Boolean)handlerCtx.getInputValue("enabled")).booleanValue();
       
        DeploymentFacility df= GuiUtil.getDeploymentFacility();
        //Hard coding to server, fix me for actual targets in EE.
        String[] targetNames = new String[] {"server"};
        List selectedRows = (List) obj;
        try{
            for(int i=0; i< selectedRows.size(); i++){
                Map oneRow = (Map) selectedRows.get(i);
                String appName = (String) oneRow.get("name");
                
                // In V3, use DF to do disable or enable
                if (enabled)
                    df.enable(df.createTargets(targetNames), appName);
                else
                    df.disable(df.createTargets(targetNames), appName); 
                
                if (V3AMX.getInstance().isEE()){
                    String msg = GuiUtil.getMessage((enabled)? "msg.enableSuccessful" : "msg.disableSuccessful");
                    GuiUtil.prepareAlert(handlerCtx, "success", msg, null);
                }else{
                    String msg = GuiUtil.getMessage((enabled)? "msg.enableSuccessfulPE" : "msg.disableSuccessfulPE");
                    GuiUtil.prepareAlert(handlerCtx, "success", msg, null);
                }
            }
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    /**
     *	<p> This method returns the deployment descriptors for a given app. </p>
     *
     *  <p> Output value: "descriptors" -- Type: <code>java.util.List</code>/</p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getDeploymentDescriptors",
    input={
        @HandlerInput(name="appName", type=String.class, required=true)},
    output={
        @HandlerOutput(name="descriptors", type=List.class)})
        public static void getDeploymentDescriptors(HandlerContext handlerCtx) {
            String appName = (String)handlerCtx.getInputValue("appName");
            List result = new ArrayList();
            Runtime runtimeMgr = V3AMX.getInstance().getRuntime();
            List<Map<String,String>> ddList = runtimeMgr.getDeploymentConfigurations(appName);
            if (ddList.size() >0 ){
                for(Map<String, String> oneDD : ddList){
                    HashMap oneRow = new HashMap();
                    oneRow.put("moduleName", oneDD.get(Runtime.MODULE_NAME_KEY) );
                    oneRow.put("ddPath", oneDD.get(Runtime.DD_PATH_KEY) );
                    oneRow.put("ddContent", oneDD.get(Runtime.DD_CONTENT_KEY) );
                    result.add(oneRow);
                }
            }
            handlerCtx.setOutputValue("descriptors", result);  
    }   


}
