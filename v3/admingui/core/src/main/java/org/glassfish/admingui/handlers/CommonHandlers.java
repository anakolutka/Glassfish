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

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.List;
import org.glassfish.admingui.common.util.AMXRoot;
import org.glassfish.admingui.common.util.GuiUtil;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import com.sun.jsftemplating.util.Util;

import javax.faces.context.ExternalContext;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;

import com.sun.webui.jsf.component.Calendar;

import com.sun.appserv.management.ext.runtime.RuntimeMgr;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.glassfish.admingui.common.util.AMX;
import org.glassfish.admingui.common.util.MiscUtil;
import org.glassfish.admingui.common.util.V3AMX;
import org.glassfish.admingui.util.HtmlAdaptor;



public class CommonHandlers {
    
    /** Creates a new instance of CommonHandlers */
    public CommonHandlers() {
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
        
        //Ensure this method is called once per session
        Object initialized = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("_SESSION_INITIALIZED");
        if (initialized == null){
            GuiUtil.initSessionAttributes();
            HtmlAdaptor.registerHTMLAdaptor(AMXRoot.getInstance().getMBeanServerConnection());
        }
        return;
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
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getAppServerVersion",
        output={
        @HandlerOutput(name="version", type=String.class)})
    public static void getAppServerVersion(HandlerContext handlerCtx) {
        String version = (String) V3AMX.getAttrsMap(AMX.DOMAIN_ROOT).get("ApplicationServerFullVersion");
        handlerCtx.setOutputValue("version", version);
    }

    /**
     *	<p> This handler returns the full version of the app server, including the build number  </p>
     *
     *  <p> Output value: "fullVersion" -- Type: <code>String</code>/</p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getAppServerFullVersion",
        output={
        @HandlerOutput(name="fullVersion", type=String.class)})
    public static void getAppServerFullVersion(HandlerContext handlerCtx) {
        String version = (String) V3AMX.getAttrsMap(AMX.DOMAIN_ROOT).get("ApplicationServerFullVersion");
        handlerCtx.setOutputValue("fullVersion", version);
    }
    
     /**
     *	<p> This handler returns String[] of the given java.util.List </p>
     *
     *  <p> Output value: "selectedIndex" -- Type: <code>Object</code>/</p>
     *	@param	handlerCtx	The HandlerContext.
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
     * <p> This handler returns the encoded String using the type specified.
     * <p> If type is not specified, it defaults to UTF-8.
     * <p> Input value: "value" -- Type: <code>String</code> <p>
     * <p> Input value: "delim" -- Type: <code>String</code> <p>
     * <p> Input Value: "type" -- Type: <code>String</code> <p>
     * <p> Output Value: "value" -- Type: <code>String</code> <p>
     *@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="selectiveEncode",
    input={
        @HandlerInput(name="value", type=String.class, required=true ),
        @HandlerInput(name="delim", type=String.class),
        @HandlerInput(name="type", type=String.class)},
    output={
        @HandlerOutput(name="result", type=String.class)}
    )
    public static void selectiveEncode(HandlerContext handlerCtx) {
        
        String value = (String) handlerCtx.getInputValue("value");
        String delim = (String) handlerCtx.getInputValue("delim");
        String encType = (String) handlerCtx.getInputValue("type");
		String encodedString = GuiUtil.encode(value, delim, encType);
        handlerCtx.setOutputValue("result", encodedString);
   } 
    
    /**
     *	<p> This method kills the session, and logs out </p>
     *      Server Domain Attributes Page.</p>
     *	<p> Input value: "page" -- Type: <code>java.lang.String</code></p>
     *	@param	handlerCtx	The HandlerContext.
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
     *	@param	handlerCtx	The HandlerContext.
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
     *	<p> This handler returns the requestParameter value based on the key.
     *	    If it doesn't exists, then it will look at the request
     *	    attribute.  If there is no request attribute, it will return the
     *	    default, if specified.</p>
     *
     *	<p> This method will "html escape" any &lt;, &gt;, or &amp; characters
     *	    that appear in a String from the QUERY_STRING.  This is to help
     *	    prevent XSS vulnerabilities.</p>
     *
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
        if ((value == null) || "".equals(value)) {
            value = handlerCtx.getFacesContext().getExternalContext().getRequestMap().get(key);
            if ((value == null) && (defaultValue != null)){
                value = defaultValue;
            }
        } else {
	    // For URLs, the following could be used, but it URLEncodes  the
	    // values, which are not ideal for displaying in HTML... so I will
	    // instead call htmlEscape()
	    //value = GuiUtil.encode(value, "#=@%+;-&_.?:/()", "UTF-8");

	    // Only need to do this for QUERY_STRING values...
	    value = Util.htmlEscape((String) value);
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
        @HandlerOutput(name="RestartRequired", type=Boolean.class),
        @HandlerOutput(name="unprocessedChanges", type=List.class)}
    )
    public void checkRestart(HandlerContext handlerCtx) {
        List<Object[]> changes = AMXRoot.getInstance().getDomainRoot().getSystemStatus().getRestartRequiredChanges();
        handlerCtx.setOutputValue("RestartRequired", changes.size() > 0); 
        handlerCtx.setOutputValue("unprocessedChanges", changes); 
    }

 
    /**
     * <p> This handler sets a property on an object which is stored in an existing key
     * For example "advance.lazyConnectionEnlistment".  <strong>Note</strong>:  This does
     * <em>not</em> evaluate the EL expression.  Its value (e.g., "#{advance.lazyConnectionEnlistment}")
     * is passed as is to the EL API.
     */
    @Handler(id = "setValueExpression",
        input = {
            @HandlerInput(name = "expression", type = String.class, required = true),
            @HandlerInput(name = "value", type = Object.class, required = true)
    })
    public static void setValueExpression(HandlerContext handlerCtx) {
        MiscUtil.setValueExpression((String) handlerCtx.getHandler().getInputValue("expression"), 
                (Object) handlerCtx.getInputValue("value"));
    }
    
    /**
     *	<p> This handler checks if particular feature is supported  </p>
     *
     *  <p> Output value: "supportCluster" -- Type: <code>Boolean</code>/</p>
     *  <p> Output value: "supportHADB" -- Type: <code>Boolean</code>/</p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="checkSupport",
    output={
        @HandlerOutput(name="supportCluster", type=Boolean.class),
        @HandlerOutput(name="supportHADB", type=Boolean.class)})
        public static void checkSupport(HandlerContext handlerCtx) {
            handlerCtx.setOutputValue("supportCluster", AMXRoot.getInstance().supportCluster());  
            handlerCtx.setOutputValue("supportHADB", false);
    }
    
    
    /**
     * This handler will return the contents of the specified deployment 
     * descriptor as a String.
     * @param handlerCtx
     */
    @Handler(id="getDeploymentDescriptor",
        input={
            @HandlerInput(name="appName", type=String.class, required=true),
            @HandlerInput(name="descriptorName", type=String.class, required=true)
        },
        output={
            @HandlerOutput(name="descriptorText", type=String.class)
    })
    public static void getDeploymentDescriptor(HandlerContext handlerCtx) {
        String appName = (String) handlerCtx.getInputValue("appName");
        String descriptorName = (String) handlerCtx.getInputValue("descriptorName");
        RuntimeMgr runtimeMgr = AMXRoot.getInstance().getRuntimeMgr();
        String descriptorText = runtimeMgr.getDeploymentConfigurations(appName).get(descriptorName);  //get the content of the descriptor
        
        if (GuiUtil.isEmpty(descriptorText)){
            System.out.printf("Could not locate %s%n", descriptorName);
        }
        handlerCtx.setOutputValue("descriptorText", descriptorText);
        
    }
    
    private static final int INDEX=0;
    
}
