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
 * CommonHandlers.java
 *
 * Created on August 30, 2006, 4:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.glassfish.admingui.handlers;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.glassfish.admingui.common.util.AMXRoot;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.util.AMXUtil;
import com.sun.enterprise.util.SystemPropertyConstants;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.config.PropertiesAccess;
import javax.faces.context.ExternalContext;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

import com.sun.webui.jsf.component.Calendar;

import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.DASConfig;

import com.sun.appserv.management.config.PropertyConfig;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;


public class CommonHandlers {
    
    /** Creates a new instance of CommonHandlers */
    public CommonHandlers() {
    }
    
    /**
     *	<p> This handler returns true if clusters are supported  </p>
     *
     *  <p> Output value: "isEE" -- Type: <code>Boolean</code>/</p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="isEE",
    output={
        @HandlerOutput(name="isEE", type=Boolean.class)})
        public static void isEE(HandlerContext handlerCtx) {
            handlerCtx.setOutputValue("isEE", AMXRoot.getInstance().isEE());    
    }
    
    /**
     * <p> This handler will be called during initialization when Cluster Support is detected.
     */
    @Handler(id="initClusterSessionAttribute")
    public static void initClusterSessionAttribute(HandlerContext handlerCtx){
        Map sessionMap = handlerCtx.getFacesContext().getExternalContext().getSessionMap();
        //The summary or detail view of deploy tables is stored in session to remember user's previous
        //preference.
        sessionMap.put("appSummaryView", true);
        sessionMap.put("webSummaryView", true);
        sessionMap.put("ejbSummaryView", true);
        sessionMap.put("appclientSummaryView", true);
        sessionMap.put("rarSummaryView", true);
        sessionMap.put("lifecycleSummaryView", true);
        
        sessionMap.put("adminObjectSummaryView", true);
        sessionMap.put("connectorResSummaryView", true);
        sessionMap.put("customResSummaryView", true);
        sessionMap.put("externalResSummaryView", true);
        sessionMap.put("javaMailSessionSummaryView", true);
        sessionMap.put("jdbcResSummaryView", true);
        sessionMap.put("jmsConnectionSummaryView", true);
        sessionMap.put("jmsDestinationSummaryView", true);
    }
    
    /**
     * <p> This handler will be called during initialization for doing any initialization.
     */
    @Handler(id="initSessionAttributes")
    public static void initSessionAttributes(HandlerContext handlerCtx){
        Map sessionMap = handlerCtx.getFacesContext().getExternalContext().getSessionMap();
	DomainRoot domainRoot = AMXRoot.getInstance().getDomainRoot();
         
        //Ensure this method is called once per session
        Object initialized = sessionMap.get("_SESSION_INITIALIZED");
        if (initialized != null) 
            return;
        
        /* TODO-V3  
        try{
            
            //Get number of new updates available
            int numUpdates = domainRoot.getUpdateStatus().getNumModules();
            GuiUtil.setSessionValue(UPDATE_CENTER_NUM_UPDATES, ""+numUpdates);
        
            //Get number of new software available
            //int numNewModules = amxRoot.getUpdateStatus().getNumNewSoftware();
            int numNewModules = numUpdates;   //TODO
            GuiUtil.setSessionValue(UPDATE_CENTER_NUM_SOFTWARES, ""+numNewModules);

            String msg = GuiUtil.getMessage("updateCenter.commonTask.noNewUpdates");
            if (numNewModules != 0){
                msg = GuiUtil.getMessage("updateCenter.commonTask.hasNewModules",  new Object[]{numNewModules});
            }else
            if (numUpdates != 0){
                msg = GuiUtil.getMessage("updateCenter.commonTask.hasNewUpdates",  new Object[]{numUpdates});
            
            }
            GuiUtil.setSessionValue("updateCenterTaskName", msg);
        }catch(Exception ex){
            //ex.printStackTrace();    was told that we shouldn't log update center exception
            sessionMap.put(UPDATE_CENTER_NUM_UPDATES, "0");
            sessionMap.put(UPDATE_CENTER_NUM_SOFTWARES, "0");
        }
        */
        
        try{
            sessionMap.put("domainName", domainRoot.getAppserverDomainName());
        }catch(Exception ex){
            ex.printStackTrace();
            sessionMap.put("domainName", "");
        }
        
        String user = handlerCtx.getFacesContext().getExternalContext().getRemoteUser();
        sessionMap.put("userName", (user == null) ? "" : user);
        
        Object request = handlerCtx.getFacesContext().getExternalContext().getRequest();
        if (request instanceof javax.servlet.ServletRequest){
            String serverName = ((javax.servlet.ServletRequest)request).getServerName();
            int serverPort = ((javax.servlet.ServletRequest)request).getServerPort();
            sessionMap.put("serverName", serverName);
            sessionMap.put("severPort", serverPort);
        }else{
            //should never get here.
            sessionMap.put("serverName", "");
        }
        
        sessionMap.put("reqMsg", GuiUtil.getMessage("msg.JS.enterValue"));
        sessionMap.put("reqMsgSelect", GuiUtil.getMessage("msg.JS.selectValue"));
        sessionMap.put("reqInt", GuiUtil.getMessage("msg.JS.enterIntegerValue"));
        sessionMap.put("reqNum", GuiUtil.getMessage("msg.JS.enterNumericValue"));
        sessionMap.put("reqPort", GuiUtil.getMessage("msg.JS.enterPortValue"));
        sessionMap.put("_SESSION_INITIALIZED","TRUE");
        
        ConfigConfig config = AMXRoot.getInstance().getConfig("server-config");
        DASConfig dConfig = config.getAdminServiceConfig().getDASConfig();
        String timeOut = dConfig.getAdminSessionTimeoutInMinutes();

		if((timeOut != null) && (!timeOut.equals(""))) {
			try {
				int time = new Integer(timeOut).intValue();
				if (time == 0) {
					((HttpServletRequest)request).getSession().setMaxInactiveInterval(-1);
				} else {
					((HttpServletRequest)request).getSession().setMaxInactiveInterval(time*60);
				}
			} catch (NumberFormatException nfe) {
				//We may never get here, in case...
					((HttpServletRequest)request).getSession().setMaxInactiveInterval(-1);
			}
		} 
    }
    
    /** This function is called in login.jsf to set the various product specific attributes such as the 
     *  product GIFs and product names. A similar function is called for Sailfin to set Sailfin specific
     *  product GIFs and name.
     *  The function is defined in com.sun.extensions.comms.SipUtilities
     */
    @Handler(id="initProductInfoAttributes")
    public static void initProductInfoAttributes(HandlerContext handlerCtx) {
        Map sessionMap = handlerCtx.getFacesContext().getExternalContext().getSessionMap();
        
        //Ensure this method is called once per session
        Object initialized = sessionMap.get("_INFO_SESSION_INITIALIZED");
        if (initialized != null) 
            return;
        // Initialize Product Specific Attributes
        sessionMap.put("productImageURL", GuiUtil.getMessage("productImage.URL"));
        sessionMap.put("productImageWidth", Integer.parseInt(GuiUtil.getMessage("productImage.width")));
        sessionMap.put("productImageHeight", Integer.parseInt(GuiUtil.getMessage("productImage.height")));
        sessionMap.put("loginProductImageURL", GuiUtil.getMessage("login.productImage.URL"));
        sessionMap.put("loginProductImageWidth", Integer.parseInt(GuiUtil.getMessage("login.productImage.width")));
        sessionMap.put("loginProductImageHeight", Integer.parseInt(GuiUtil.getMessage("login.productImage.height")));        
        sessionMap.put("fullProductName", GuiUtil.getMessage("versionImage.description"));
        sessionMap.put("loginButtonTooltip", GuiUtil.getMessage("loginButtonTooltip"));
        sessionMap.put("mastHeadDescription", GuiUtil.getMessage("mastHeadDescription"));
        
        // showLoadBalancer is a Sailfin specific attribute. Sailfin uses Converged LB instead
        // of HTTP LB. It is true for GF and false for Sailfin. In sailfin this is set in
        // com.sun.extensions.comms.SipUtilities.initProductInfoAttributes() called for Sailfin in login.jsf
        
        //TODO-V3 may need to set this back to true
        //sessionMap.put("showLoadBalancer", true); 
        
        sessionMap.put("_INFO_SESSION_INITIALIZED","TRUE");
    }
    
    
     /**
     *	<p> This handler returns the version of the app server  </p>
     *
     *  <p> Output value: "version" -- Type: <code>String</code>/</p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getAppServerVersion",
        output={
        @HandlerOutput(name="version", type=String.class)})
    public static void getAppServerVersion(HandlerContext handlerCtx) {
        
        String version = AMXRoot.getInstance().getDomainRoot().getApplicationServerFullVersion();
        handlerCtx.setOutputValue("version", version);
    }

    /**
     *	<p> This handler returns the full version of the app server, including the build number  </p>
     *
     *  <p> Output value: "fullVersion" -- Type: <code>String</code>/</p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getAppServerFullVersion",
        output={
        @HandlerOutput(name="fullVersion", type=String.class)})
    public static void getAppServerFullVersion(HandlerContext handlerCtx) {
        String version = AMXRoot.getInstance().getDomainRoot().getApplicationServerFullVersion();
        handlerCtx.setOutputValue("fullVersion", version); 
    }
    
     /**
     *	<p> This handler returns String[] of the given java.util.List </p>
     *
     *  <p> Output value: "selectedIndex" -- Type: <code>Object</code>/</p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getListElement",
    	input={
        @HandlerInput(name="list", type=java.util.List.class, required=true ),
        @HandlerInput(name="index", type=Integer.class)},
        output={
        @HandlerOutput(name="selectedIndex", type=Object.class)})
    public static void getListElement(HandlerContext handlerCtx) {
		List<String> list = (List)handlerCtx.getInputValue("list");	
		Integer selectedIndex = (Integer)handlerCtx.getInputValue("index");	
		String[] listItem = null;
		if(list != null) {
			if(selectedIndex == null) {
				//default to 0
				selectedIndex = new Integer(INDEX);
			}
			listItem = new String[]{list.get(selectedIndex)};
		}
        handlerCtx.setOutputValue("selectedIndex", listItem);
    }
    
    
    
    /**
     * <p> This handler returns the config name of the specified instance or cluster.
     *
     * <p> Input value: "target" -- Type: <code>String</code> <p>
     * <p> Output Value: "configName" -- Type: <code>String</code> <p>
     *@param	context	The HandlerContext.
     */
    @Handler(id="getConfigName",
    input={
        @HandlerInput(name="target", type=String.class, required=true )},
    output={
        @HandlerOutput(name="configName", type=String.class)}
    )
    public static void getConfigName(HandlerContext handlerCtx) {
        
        String target = (String) handlerCtx.getInputValue("target");
        String configName = AMXRoot.getInstance().getConfigName(target);
        handlerCtx.setOutputValue("configName", configName);
   } 
    /**
     * <p> This handler returns the encoded String using the type specified.
     * <p> If type is not specified, it defaults to UTF-8.
     * <p> Input value: "value" -- Type: <code>String</code> <p>
     * <p> Input value: "delim" -- Type: <code>String</code> <p>
     * <p> Input Value: "type" -- Type: <code>String</code> <p>
     * <p> Output Value: "value" -- Type: <code>String</code> <p>
     *@param	context	The HandlerContext.
     */
    @Handler(id="selectiveEncode",
    input={
        @HandlerInput(name="value", type=String.class, required=true ),
        @HandlerInput(name="delim", type=String.class),
        @HandlerInput(name="type", type=String.class)},
    output={
        @HandlerOutput(name="value", type=String.class)}
    )
    public static void selectiveEncode(HandlerContext handlerCtx) {
        
        String value = (String) handlerCtx.getInputValue("value");
        String delim = (String) handlerCtx.getInputValue("delim");
        String encType = (String) handlerCtx.getInputValue("type");
		String encodedString = GuiUtil.encode(value, delim, encType);
        handlerCtx.setOutputValue("value", encodedString);
   } 
    
    /**
     *	<p> This method kills the session, and logs out </p>
     *      Server Domain Attributes Page.</p>
     *	<p> Input value: "page" -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="logout")
    public static void logout(HandlerContext handlerCtx) {
	ExternalContext extContext = handlerCtx.getFacesContext().getExternalContext();
	HttpServletRequest request = (HttpServletRequest) extContext.getRequest();
	request.getSession().invalidate();
    } 
    
    /**
     *	<p> This method sets the required attribute of a UI component .
     *	<p> Input value: "id" -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "required" -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="setComponentRequired",
    input={
        @HandlerInput(name="id",     type=String.class, required=true),
        @HandlerInput(name="required",     type=String.class, required=true)
    })
    public static void setComponentRequired(HandlerContext handlerCtx) {
        String id = (String) handlerCtx.getInputValue("id");
        String required = (String) handlerCtx.getInputValue("required");
        UIComponent viewRoot = handlerCtx.getFacesContext().getViewRoot();
        if (viewRoot == null) return;
        try {
            UIInput targetComponent = (UIInput) viewRoot.findComponent(id);
            if (targetComponent != null ){
                targetComponent.setRequired(Boolean.valueOf(required));
            }
            
        }catch(Exception ex){
            //Cannot find component, do nothing.
        }
    }
    
    
    /**
     *  <p> Test if a particular attribute exists.
     *      It will look at request scope, then page, then session.
     */
    @Handler(id="testExists",
    input={
        @HandlerInput(name="attr", type=String.class, required=true )},
    output={
        @HandlerOutput(name="defined", type=Boolean.class)}
    )
    public static void testExists(HandlerContext handlerCtx) {
        String attr = (String) handlerCtx.getInputValue("attr");
        if(GuiUtil.isEmpty(attr)){
            handlerCtx.setOutputValue("defined", false);
        }else{
            handlerCtx.setOutputValue("defined", true);
        }
    }

    /**
     *  <p> Returns the date pattern for this calendar component.
     *      
     */
    @Handler(id="getDatePattern",
    input={
           @HandlerInput(name="calendarComponent", type=com.sun.webui.jsf.component.Calendar.class, required=true)},
    output={
        @HandlerOutput(name="pattern", type=String.class)}
    )
    public static void getDatePattern(HandlerContext handlerCtx) {
        Calendar calendar = (Calendar) handlerCtx.getInputValue("calendarComponent");
		String pattern = calendar.getDateFormatPattern();

		if(pattern == null || pattern.length() == 0) {
			pattern = calendar.getDatePicker().getDateFormatPattern();

			if(pattern == null || pattern.length() == 0) {
				pattern="MM/dd/yyyy"; //default pattern
			}
		}
        handlerCtx.setOutputValue("pattern", pattern);
    }
    
    /**
     * <p> This handler returns the requestParameter value based on the key.  If it doesn't
     *  exists, then it will look at the request Attribute.<p>
     * If not request Attribute is specified, it will return the default if default is specified.
     * 	<p> Input value: "key" -- Type: <code>String</code></p>
     *
     *	<p> Output value: "value" -- Type: <code>String</code></p>
     *
     */
    @Handler(id="getRequestValue",
    input={
        @HandlerInput(name="key", type=String.class, required=true),
        @HandlerInput(name="default", type=String.class)},
    output={
        @HandlerOutput(name="value", type=Object.class)}
    )
    public static void getRequestValue(HandlerContext handlerCtx) {
        
        String key = (String) handlerCtx.getInputValue("key");
        Object defaultValue = handlerCtx.getInputValue("default");
        Object value = handlerCtx.getFacesContext().getExternalContext().getRequestParameterMap().get(key);
        if (value == null){
            value = handlerCtx.getFacesContext().getExternalContext().getRequestMap().get(key);
            if ((value == null) && (defaultValue != null)){
                value = defaultValue;
            }
        }else{
            if ( (value instanceof String) && "".equals(value))
                value = handlerCtx.getFacesContext().getExternalContext().getRequestMap().get(key);
        }
        handlerCtx.setOutputValue("value", value);
   } 
   
    /**
     *	This method adds two long integers together.  The 2 longs should be
     *	stored in "long1" and "long2".  The result will be stored as "result".
     */
    @Handler(id="longAdd",
    input={
        @HandlerInput(name="Long1", type=Long.class, required=true ),
        @HandlerInput(name="Long2", type=Long.class, required=true )},
    output={
        @HandlerOutput(name="LongResult", type=Long.class)}
    )    
    public void longAdd(HandlerContext handlerCtx) {
    	// Get the inputs
	Long long1 = (Long)handlerCtx.getInputValue("Long1");
	Long long2 = (Long)handlerCtx.getInputValue("Long2");

	// Add the 2 numbers together
	Long result = new Long(long1.longValue()+long2.longValue());

	// Set the result
	handlerCtx.setOutputValue("LongResult", result);
    }
    
    /**
     * <p> Returns the current system time formatted<p>
     * <p> Output value: "Time" -- Type: <code>String</code></p>
     *
     */
    @Handler(id="getCurrentTime",
    output={
        @HandlerOutput(name="CurrentTime", type=String.class)}
    )
    public void getCurrentTime(HandlerContext handlerCtx) {
        Date d = new Date(System.currentTimeMillis());
        DateFormat dateFormat = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.MEDIUM, handlerCtx.getFacesContext().getViewRoot().getLocale());
        String currentTime = dateFormat.format(d);
        handlerCtx.setOutputValue("CurrentTime", currentTime);
    }
    
    /**
     * <p> Returns the restart required status<p>
     * <p> Output value: "RestartRequired" -- Type: <code>java.lang.Boolean</code></p>
     *
     */
    @Handler(id="checkRestart",
    output={
        @HandlerOutput(name="RestartRequired", type=Boolean.class)}
    )
    public void checkRestart(HandlerContext handlerCtx) {
        
        //TODO-V3
        //Boolean restartRequired = (Boolean)JMXUtil.getAttribute("com.sun.appserv:j2eeType=J2EEServer,name=server,category=runtime", "restartRequired");
        handlerCtx.setOutputValue("RestartRequired", false); 
    }
    
    /**
     * <p> Get the Property Value of the AMX mbean.<p>
     *
     */
    @Handler(id="getPropsValue",
    input={
        @HandlerInput(name="mbean", type=PropertiesAccess.class, required=true),
        @HandlerInput(name="propsName", type=java.util.List.class, required=true)},
    output={
        @HandlerOutput(name="propsValue", type=java.util.List.class)}
    )
    public void getPropsValue(HandlerContext handlerCtx) {
        
        PropertiesAccess mbean = (PropertiesAccess)handlerCtx.getInputValue("mbean");
        List<String> propsName = (List)handlerCtx.getInputValue("propsName");
        if (mbean==null){
            if (propsName != null){
                handlerCtx.setOutputValue("propsValue", new ArrayList(propsName.size()));
            }
            return;
        }
        Map<String, PropertyConfig>  mbeanProps = mbean.getPropertyConfigMap();
        List propsValue = new ArrayList(propsName.size());
        for(String nm : propsName){
           String value = mbeanProps.get(nm).getValue();
           propsValue.add( (value==null) ? "" : value);
        }
        handlerCtx.setOutputValue("propsValue", propsValue);
    }
    
    /**
     * <p> Save the Property Value of the AMX mbean.<p>
     *
     */
    @Handler(id="savePropsValue",
    input={
        @HandlerInput(name="mbean", type=PropertiesAccess.class, required=true),
        @HandlerInput(name="propsName", type=java.util.List.class, required=true),
        @HandlerInput(name="propsValue", type=java.util.List.class, required=true)}
    )
    public void savePropsValue(HandlerContext handlerCtx) {
        
        PropertiesAccess mbean = (PropertiesAccess)handlerCtx.getInputValue("mbean");
        List<String> propsName = (List)handlerCtx.getInputValue("propsName");
        List<String> propsValue = (List)handlerCtx.getInputValue("propsValue");
        if (mbean==null){
            //TODO: log error
            return;
        }
        for(int i=0; i<propsName.size(); i++){
           AMXUtil.setPropertyValue(mbean, propsValue.get(i), propsName.get(i));
        }
    }
    
    
    /**
     * <P> returns the list of Properties of the specified mbean.
     * The list returned will not contain the specified "ignoreProps"
     */
     @Handler(id="getMbeanProperties",
    input={
        @HandlerInput(name="mbean", type=PropertiesAccess.class, required=true),
        @HandlerInput(name="ignoreProps", type=java.util.List.class)},
    output={
        @HandlerOutput(name="result", type=Map.class)}
    )
    public void getMbeanProperties(HandlerContext handlerCtx) {
        
        PropertiesAccess mbean = (PropertiesAccess)handlerCtx.getInputValue("mbean");
        List<String> ignoreProps = (List)handlerCtx.getInputValue("ignoreProps");
        Map<String,PropertyConfig> allProps = mbean.getPropertyConfigMap();
        
        if (ignoreProps != null ){
            for(String nm : ignoreProps){
                if (allProps.get(nm) != null){
                    allProps.remove(nm);
                }
            }
        }
        handlerCtx.setOutputValue("result", allProps);
     }
     
    /**
     * <p> Get the Property Value of the AMX mbean.<p>
     *
     */
    @Handler(id="getChartingCookieName",
    output={
        @HandlerOutput(name="cookieName", type=String.class)}
    )
    public void getChartingCookieName(HandlerContext handlerCtx) {
        String userName = (String) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("userName");
        handlerCtx.setOutputValue("cookieName", userName + "." + CHARTING_COOKIE_NAME);
    }
    
    
    /**
     * <p> returns the charting cookie value. 
     *
     */
    @Handler(id="getChartingCookieInfo",
    output={
        @HandlerOutput(name="name", type=String.class),
        @HandlerOutput(name="doCharting", type=Boolean.class),
        @HandlerOutput(name="setCookieTo", type=String.class)}
    )
    public void getChartingCookieInfo(HandlerContext handlerCtx) {
        String userName = (String) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("userName");
        String cookieName =  userName + "." + CHARTING_COOKIE_NAME;
        Map<String, Object> cookies = handlerCtx.getFacesContext().getExternalContext().getRequestCookieMap();
        Cookie cookie = (Cookie) cookies.get(cookieName);
        String value = (cookie == null) ? "" : cookie.getValue();
        handlerCtx.setOutputValue("name", cookieName);
        handlerCtx.setOutputValue("doCharting", value.equals("true"));
        handlerCtx.setOutputValue("setCookieTo", (value.equals("true"))?  "false" : "true");
    }

    /**
     * <p> returns update center info. 
     *
     */
    @Handler(id="getUpdateCenterInfo",
    output={
        @HandlerOutput(name="updates", type=String.class),
        @HandlerOutput(name="modules", type=String.class),
        @HandlerOutput(name="tool", type=String.class),
        @HandlerOutput(name="wiki", type=String.class)}
    )
    public void getUpdateCenterInfo(HandlerContext handlerCtx) {
        //Get number of new updates available
        String numUpdates = (String) GuiUtil.getSessionValue(UPDATE_CENTER_NUM_UPDATES);
        //Get number of new software available
        String numNewModules = (String) GuiUtil.getSessionValue(UPDATE_CENTER_NUM_SOFTWARES);
        
        //Get Update Center tools location
        String installRoot = System.getProperty(
                SystemPropertyConstants.INSTALL_ROOT_PROPERTY);
        if (installRoot == null) {
            installRoot="install-root";
        }
        String updateTool = installRoot + File.separator + "updatecenter" +
                File.separator + "bin" + File.separator + "updatetool";
        updateTool = updateTool.replace('\\', '/');
        
        handlerCtx.setOutputValue("updates", numUpdates);
        handlerCtx.setOutputValue("modules", numNewModules);
        handlerCtx.setOutputValue("tool", updateTool);
        handlerCtx.setOutputValue("wiki", "http://wiki.updatecenter.java.net/Wiki.jsp?page=GettingStarted");        
    }
    /**
     *	<p> This handler sets a property on an object which is stored in an existing key
     *  For example "advance.lazyConnectionEnlistment"
     */     
    
@Handler(id="setValueExpression",
        input={
		@HandlerInput(name="keyObjectName", type=String.class, required=true),
                @HandlerInput(name="hasBoolean", type=Boolean.class),
                @HandlerInput(name="objectValue", type=String.class, required=true)}
      )
      public static void setValueExpression(HandlerContext handlerCtx) {
        String name = (String) handlerCtx.getInputValue("keyObjectName");
        String value = (String) handlerCtx.getInputValue("objectValue");
        Boolean hasBoolean = (Boolean)handlerCtx.getInputValue("hasBoolean");
        FacesContext facesContext = FacesContext
                .getCurrentInstance();
        ELContext elcontext = facesContext.getELContext();
        ValueExpression ve =
                facesContext.getApplication().getExpressionFactory().
                createValueExpression(
                facesContext.getELContext(), "#{"+name+"}", Object.class);
        if(hasBoolean == null) {
            ve.setValue(facesContext.getELContext(), value);
        } else {
            if(hasBoolean.booleanValue()) {
                if(value.equals("true")) {
                    ve.setValue(facesContext.getELContext(), Boolean.TRUE);
                } else {
                    ve.setValue(facesContext.getELContext(), Boolean.FALSE);
                }
            } else {
                ve.setValue(facesContext.getELContext(), value);
            }
        }
    }    
    
/**
     *	<p> This handler checks if particular feature is supported  </p>
     *
     *  <p> Output value: "supportCluster" -- Type: <code>Boolean</code>/</p>
     *  <p> Output value: "supportHADB" -- Type: <code>Boolean</code>/</p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="checkSupport",
    output={
        @HandlerOutput(name="supportCluster", type=Boolean.class),
        @HandlerOutput(name="supportHADB", type=Boolean.class)})
        public static void checkSupport(HandlerContext handlerCtx) {
            handlerCtx.setOutputValue("supportCluster", AMXRoot.getInstance().supportCluster());  
            handlerCtx.setOutputValue("supportHADB", false);
    }
    
        
    
    
    private static final String CHARTING_COOKIE_NAME = "as91-doCharting";
    private static final int INDEX=0;
    private static final String UPDATE_CENTER_NUM_UPDATES="updateCenterNumUpdates";
    private static final String UPDATE_CENTER_NUM_SOFTWARES="updateCenterNumSoftwares";
    
}
