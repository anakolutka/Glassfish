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
 * HttpServiceHandlers.java
 *
 * Created on August 12, 2006, 7:04 PM
 *
 */
package org.glassfish.admingui.handlers;


import com.sun.appserv.management.base.XTypes;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import com.sun.jsftemplating.annotation.Handler;  
import com.sun.jsftemplating.annotation.HandlerInput; 
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;  


import org.glassfish.admingui.util.AMXRoot; 
import org.glassfish.admingui.util.GuiUtil;

import com.sun.appserv.management.config.ConfigConfig; 
import com.sun.appserv.management.config.VirtualServerConfig;
import com.sun.appserv.management.config.AccessLogConfig;
import com.sun.appserv.management.config.ConfigElement;
import com.sun.appserv.management.config.RequestProcessingConfig;
import com.sun.appserv.management.config.RequestProcessingConfigKeys;
import com.sun.appserv.management.config.KeepAliveConfig;
import com.sun.appserv.management.config.KeepAliveConfigKeys;
import com.sun.appserv.management.config.HTTPProtocolConfig;
import com.sun.appserv.management.config.HTTPProtocolConfigKeys;
import com.sun.appserv.management.config.HTTPFileCacheConfig;
import com.sun.appserv.management.config.HTTPFileCacheConfigKeys;
import com.sun.appserv.management.config.HTTPServiceConfig;
import com.sun.appserv.management.config.HTTPListenerConfig;
import com.sun.appserv.management.config.ConnectionPoolConfig;
import com.sun.appserv.management.config.ConnectionPoolConfigKeys;
import java.util.Iterator;

/**
 *
 * @author Anissa Lam
 */
public class HttpServiceHandlers {


    /**
     *	<p> This handler returns the values for all the attributes in the
     *      Access Log Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "Rotation"       -- Type: <code>java.lang.Boolean</code></p>
     *	<p> Output value: "Policy"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Interval"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Suffix"     -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Format"        -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getAccessLogSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Rotation",      type=Boolean.class),
        @HandlerOutput(name="Policy",  type=String.class),
        @HandlerOutput(name="Interval",    type=String.class),
        @HandlerOutput(name="Suffix",       type=String.class),
        @HandlerOutput(name="Format", type=String.class)})
        
        public static void getAccessLogSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	AccessLogConfig al = config.getHTTPServiceConfig().getAccessLogConfig();
        
        String policy="";
        String interval="";
        String suffix="";
        String format="";
        boolean rotation = true;
        
        if (al != null){
             rotation = al.getRotationEnabled();
             policy = al.getRotationPolicy();
             interval = al.getRotationIntervalInMinutes();
             suffix = al.getRotationSuffix();
             format = al.getFormat();
        }else{
            Map defaultMap = config.getHTTPServiceConfig().getDefaultValues(XTypes.ACCESS_LOG_CONFIG);
            policy = (String) defaultMap.get("rotation-policy");
            interval = (String)defaultMap.get("rotation-interval-in-minutes");
            suffix = (String)defaultMap.get("rotation-suffix");
            format = (String)defaultMap.get("format");
            String rotationKey = (String) defaultMap.get("rotation-enabled");
            rotation = (rotationKey == null) ? false : Boolean.valueOf(rotationKey);
        }
        handlerCtx.setOutputValue("Rotation", rotation);
        handlerCtx.setOutputValue("Policy", policy);
        handlerCtx.setOutputValue("Interval", interval);
        handlerCtx.setOutputValue("Suffix", suffix);
        handlerCtx.setOutputValue("Format", format);        
        
    }   
    
/**
     *	<p> This handler returns the default values for all the attributes in the
     *      Access Log Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "Rotation"       -- Type: <code>java.lang.Boolean</code></p>
     *	<p> Output value: "Policy"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Interval"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Suffix"     -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Format"        -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getAccessLogDefaultSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Rotation",      type=Boolean.class),
        @HandlerOutput(name="Policy",  type=String.class),
        @HandlerOutput(name="Interval",    type=String.class),
        @HandlerOutput(name="Suffix",       type=String.class),
        @HandlerOutput(name="Format", type=String.class)})
        
        public static void getAccessLogDefaultSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
        Map defaultMap = config.getHTTPServiceConfig().getDefaultValues(XTypes.ACCESS_LOG_CONFIG);
        String policy = (String) defaultMap.get("rotation-policy");
        String interval = (String)defaultMap.get("rotation-interval-in-minutes");
        String suffix = (String)defaultMap.get("rotation-suffix");
        String format = (String)defaultMap.get("format");
        String rotationKey = (String) defaultMap.get("rotation-enabled");
        boolean rotation = (rotationKey == null) ? false : Boolean.valueOf(rotationKey);
        
        handlerCtx.setOutputValue("Rotation", rotation);
        handlerCtx.setOutputValue("Policy", policy);
        handlerCtx.setOutputValue("Interval", interval);
        handlerCtx.setOutputValue("Suffix", suffix);
        handlerCtx.setOutputValue("Format", format);        
        
    }   
    
/**
     *	<p> This handler returns the default values for all the attributes in the
     *      Access Log Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "Rotation"       -- Type: <code>java.lang.Boolean</code></p>
     *	<p> Input value: "Policy"       -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Interval"   -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Suffix"     -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Format"        -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="saveAccessLogSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true),
        @HandlerInput(name="Rotation",      type=Boolean.class),
        @HandlerInput(name="Policy",  type=String.class),
        @HandlerInput(name="Interval",    type=String.class),
        @HandlerInput(name="Suffix",       type=String.class),
        @HandlerInput(name="Format", type=String.class)})
        
        public static void saveAccessLogSettings(HandlerContext handlerCtx) {
        
        try{
            ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
            AccessLogConfig al = config.getHTTPServiceConfig().getAccessLogConfig();
            if (al == null){
                al = config.getHTTPServiceConfig().createAccessLogConfig(new HashMap());
            }
            Boolean temp = (Boolean) handlerCtx.getInputValue("Rotation");
            al.setRotationEnabled( (temp == null) ? false : temp);
            al.setRotationPolicy(((String)handlerCtx.getInputValue("Policy")));
            al.setRotationIntervalInMinutes(((String)handlerCtx.getInputValue("Interval")));
            al.setRotationSuffix(((String)handlerCtx.getInputValue("Suffix")));
            al.setFormat(((String)handlerCtx.getInputValue("Format")));
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
    }   
    
/**
     *	<p> This handler returns the values for all the attributes in the
     *      Request Processing Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "Count"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Initial"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Increment"     -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Timeout"        -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Buffer"        -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getRequestProcessingSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Count",  type=String.class),
        @HandlerOutput(name="Initial",    type=String.class),
        @HandlerOutput(name="Increment",       type=String.class),
        @HandlerOutput(name="Timeout", type=String.class),
        @HandlerOutput(name="Buffer", type=String.class)})
        
        public static void getRequestProcessingSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	RequestProcessingConfig rp = config.getHTTPServiceConfig().getRequestProcessingConfig();
        String count = rp.getThreadCount();
        String initial = rp.getInitialThreadCount();
        String increment = rp.getThreadIncrement();
        String timeout = rp.getRequestTimeoutInSeconds();
        String buffer = rp.getHeaderBufferLengthInBytes();
        handlerCtx.setOutputValue("Count", count);
        handlerCtx.setOutputValue("Initial", initial);
        handlerCtx.setOutputValue("Increment", increment);
        handlerCtx.setOutputValue("Timeout", timeout);
        handlerCtx.setOutputValue("Buffer", buffer);        
        
    }   
    

    /**
     *	<p> This handler returns the default values for all the attributes in the
     *      Request Processing Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "Count"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Initial"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Increment"     -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Timeout"        -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Buffer"        -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getRequestProcessingDefaultSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Count",  type=String.class),
        @HandlerOutput(name="Initial",    type=String.class),
        @HandlerOutput(name="Increment",       type=String.class),
        @HandlerOutput(name="Timeout", type=String.class),
        @HandlerOutput(name="Buffer", type=String.class)})
        
        public static void getRequestProcessingDefaultSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	RequestProcessingConfig rp = config.getHTTPServiceConfig().getRequestProcessingConfig();
        String count = rp.getDefaultValue(RequestProcessingConfigKeys.THREAD_COUNT_KEY);
        String initial = rp.getDefaultValue(RequestProcessingConfigKeys.INITIAL_THREAD_COUNT_KEY);
        String increment = rp.getDefaultValue(RequestProcessingConfigKeys.THREAD_INCREMENT_KEY);
        String timeout = rp.getDefaultValue(RequestProcessingConfigKeys.REQUEST_TIMEOUT_IN_SECONDS_KEY);
        String buffer = rp.getDefaultValue(RequestProcessingConfigKeys.HEADER_BUFFER_LENGTH_IN_BYTES_KEY);
        handlerCtx.setOutputValue("Count", count);
        handlerCtx.setOutputValue("Initial", initial);
        handlerCtx.setOutputValue("Increment", increment);
        handlerCtx.setOutputValue("Timeout", timeout);
        handlerCtx.setOutputValue("Buffer", buffer);        
        
    }   
    
/**
     *	<p> This handler saves the values for all the attributes in the
     *      Request Processing Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "Count"       -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Initial"   -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Increment"     -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Timeout"        -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Buffer"        -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="saveRequestProcessingSettings",
    input={
        @HandlerInput(name="ConfigName", type=String.class, required=true),
        @HandlerInput(name="Count",  type=String.class),
        @HandlerInput(name="Initial",    type=String.class),
        @HandlerInput(name="Increment",       type=String.class),
        @HandlerInput(name="Timeout", type=String.class),
        @HandlerInput(name="Buffer", type=String.class)})
        
        public static void saveRequestProcessingSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
        try{
            RequestProcessingConfig rp = config.getHTTPServiceConfig().getRequestProcessingConfig();
            rp.setThreadCount(((String)handlerCtx.getInputValue("Count")));
            rp.setInitialThreadCount(((String)handlerCtx.getInputValue("Initial")));
            rp.setThreadIncrement(((String)handlerCtx.getInputValue("Increment")));
            rp.setRequestTimeoutInSeconds(((String)handlerCtx.getInputValue("Timeout")));
            rp.setHeaderBufferLengthInBytes(((String)handlerCtx.getInputValue("Buffer"))); 
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
        
    }   
    
/**
     *	<p> This handler returns the values for all the attributes in the
     *      Keep Alive Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "Count"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Connections"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Timeout"     -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getKeepAliveSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Count",  type=String.class),
        @HandlerOutput(name="Connections",    type=String.class),
        @HandlerOutput(name="Timeout",       type=String.class)})
        
        public static void getKeepAliveSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	KeepAliveConfig rp = config.getHTTPServiceConfig().getKeepAliveConfig();
        String count = rp.getThreadCount();
        String connections = rp.getMaxConnections();
        String timeout = rp.getTimeoutInSeconds();
        handlerCtx.setOutputValue("Count", count);
        handlerCtx.setOutputValue("Connections", connections);
        handlerCtx.setOutputValue("Timeout", timeout);   
        
    } 
    
/**
     *	<p> This handler returns the default values for all the attributes in the
     *      Keep Alive Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "Count"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Connections"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Timeout"     -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getKeepAliveDefaultSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Count",  type=String.class),
        @HandlerOutput(name="Connections",    type=String.class),
        @HandlerOutput(name="Timeout",       type=String.class)})
        
        public static void getKeepAliveDefaultSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	KeepAliveConfig rp = config.getHTTPServiceConfig().getKeepAliveConfig();
        String count = rp.getDefaultValue(KeepAliveConfigKeys.THREAD_COUNT_KEY );
        String connections = rp.getDefaultValue(KeepAliveConfigKeys.MAX_CONNECTIONS_KEY );
        String timeout = rp.getDefaultValue(KeepAliveConfigKeys.TIMEOUT_IN_SECONDS_KEY );
        handlerCtx.setOutputValue("Count", count);
        handlerCtx.setOutputValue("Connections", connections);
        handlerCtx.setOutputValue("Timeout", timeout);   
        
    } 
    
/**
     *	<p> This handler saves the values for all the attributes in the
     *      Keep Alive Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "Count"       -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Connections"   -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Timeout"     -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="saveKeepAliveSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true),
        @HandlerInput(name="Count",  type=String.class),
        @HandlerInput(name="Connections",    type=String.class),
        @HandlerInput(name="Timeout",       type=String.class)})
        
        public static void saveKeepAliveSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	KeepAliveConfig rp = config.getHTTPServiceConfig().getKeepAliveConfig();
        rp.setThreadCount(((String)handlerCtx.getInputValue("Count")));
        rp.setMaxConnections(((String)handlerCtx.getInputValue("Connections")));
        rp.setTimeoutInSeconds(((String)handlerCtx.getInputValue("Timeout")));
        
    } 
    
    
    /**
     *	<p> This handler returns the values of properties in HttpService </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getHttpService",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},
    output={
        @HandlerOutput(name="accessLogBufferSize", type=String.class),
        @HandlerOutput(name="accessLogWriteInterval", type=String.class),
        @HandlerOutput(name="accessLoggingEnabled", type=Boolean.class),
        @HandlerOutput(name="Properties", type=Map.class)})
        
        public static void getHttpService(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
        HTTPServiceConfig hConfig = config.getHTTPServiceConfig();
        
        try{
            
            handlerCtx.setOutputValue("Properties", AMXRoot.getInstance().getNonSkipPropertiesMap(hConfig, httpServiceSkipPropsList));
            Map origProps = hConfig.getProperties();
            handlerCtx.setOutputValue("accessLogBufferSize", origProps.get("accessLogBufferSize"));
            handlerCtx.setOutputValue("accessLogWriteInterval", origProps.get("accessLogWriteInterval"));
            String alog = (String) origProps.get("accessLoggingEnabled");
            Boolean accessLoggingEnabled = true;
            if ( GuiUtil.isEmpty(alog))
                accessLoggingEnabled = true;
            else
            accessLoggingEnabled = (alog.equals("true")) ? true: false;
            
            handlerCtx.setOutputValue("accessLoggingEnabled", accessLoggingEnabled);
                    
        }catch (Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
        
    } 
    
    
    /**
     *	<p> This handler saves the Http Service properties 
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="saveHttpService",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true), 
        @HandlerInput(name="newProps", type=Map.class),  
        @HandlerInput(name="accessLogBufferSize", type=String.class),
        @HandlerInput(name="accessLogWriteInterval", type=String.class),
        @HandlerInput(name="accessLoggingEnabled",     type=Boolean.class)})
        
        public static void saveHttpService(HandlerContext handlerCtx) {
        AMXRoot amxRoot = AMXRoot.getInstance();
        
        try{
            ConfigConfig config = amxRoot.getConfig(((String)handlerCtx.getInputValue("ConfigName")));
            HTTPServiceConfig hConfig = config.getHTTPServiceConfig();
            Map newProps = (Map)handlerCtx.getInputValue("newProps");
        
            amxRoot.updateProperties(hConfig, newProps, httpServiceSkipPropsList);
            
            amxRoot.changeProperty(hConfig, "accessLogBufferSize", (String)handlerCtx.getInputValue("accessLogBufferSize"));
            amxRoot.changeProperty(hConfig, "accessLogWriteInterval", (String)handlerCtx.getInputValue("accessLogWriteInterval"));
            amxRoot.changeProperty(hConfig, "accessLoggingEnabled", ""+handlerCtx.getInputValue("accessLoggingEnabled"));
            
        }catch (Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
        
    }   
    
    /**
     *	<p> This handler returns the values for all the attributes in the
     *      Connection Pool Config Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "Count"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Queue"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Receive"     -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Send"     -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getConnectionPoolSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Count",  type=String.class),
        @HandlerOutput(name="Queue",    type=String.class),
        @HandlerOutput(name="Receive",       type=String.class),
        @HandlerOutput(name="Send",       type=String.class)})
        
        public static void getConnectionPoolSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	ConnectionPoolConfig cp = config.getHTTPServiceConfig().getConnectionPoolConfig();
        String count = cp.getMaxPendingCount();
        String queue = cp.getQueueSizeInBytes();
        String receive = cp.getReceiveBufferSizeInBytes();
        String send = cp.getSendBufferSizeInBytes();
        handlerCtx.setOutputValue("Count", count);
        handlerCtx.setOutputValue("Queue", queue);
        handlerCtx.setOutputValue("Receive", receive);   
        handlerCtx.setOutputValue("Send", send);   
        
    }     
    
    
/**
     *	<p> This handler returns the default values for all the attributes in the
     *      Connection Pool Config Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "Count"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Queue"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Receive"     -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Send"     -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getConnectionPoolDefaultSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Count",  type=String.class),
        @HandlerOutput(name="Queue",    type=String.class),
        @HandlerOutput(name="Receive",       type=String.class),
        @HandlerOutput(name="Send",       type=String.class)})
        
        public static void getConnectionPoolDefaultSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	ConnectionPoolConfig cp = config.getHTTPServiceConfig().getConnectionPoolConfig();
        String count = cp.getDefaultValue(ConnectionPoolConfigKeys.MAX_PENDING_COUNT_KEY);
        String queue = cp.getDefaultValue(ConnectionPoolConfigKeys.QUEUE_SIZE_IN_BYTES_KEY);
        String receive = cp.getDefaultValue(ConnectionPoolConfigKeys.RECEIVE_BUFFER_SIZE_IN_BYTES_KEY);
        String send = cp.getDefaultValue(ConnectionPoolConfigKeys.SEND_BUFFER_SIZE_IN_BYTES_KEY);
        handlerCtx.setOutputValue("Count", count);
        handlerCtx.setOutputValue("Queue", queue);
        handlerCtx.setOutputValue("Receive", receive);   
        handlerCtx.setOutputValue("Send", send);   
        
    }     
    
/**
     *	<p> This handler saves the values for all the attributes in the
     *      Connection Pool Config Page </p>
     *	<p> Input value: "ConfigName"       -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "Count"       -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Queue"   -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Receive"     -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Send"     -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="saveConnectionPoolSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true),
        @HandlerInput(name="Count",  type=String.class),
        @HandlerInput(name="Queue",    type=String.class),
        @HandlerInput(name="Receive",       type=String.class),
        @HandlerInput(name="Send",       type=String.class)})
        
        public static void saveConnectionPoolSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
        try{
            ConnectionPoolConfig cp = config.getHTTPServiceConfig().getConnectionPoolConfig();
            cp.setMaxPendingCount((String)handlerCtx.getInputValue("Count"));
            cp.setQueueSizeInBytes((String)handlerCtx.getInputValue("Queue"));
            cp.setReceiveBufferSizeInBytes((String)handlerCtx.getInputValue("Receive"));
            cp.setSendBufferSizeInBytes((String)handlerCtx.getInputValue("Send"));
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
        
    }     



    /**
     *	<p> This handler returns the values for all the attributes in the
     *      HTTP Protocol Config Page </p>
     *	<p> Input value: "ConfigName" -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "Version"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "DNS"       -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "SSL"       -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "Forced"    -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Default"   -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getHttpProtocolSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Version",  type=String.class),
        @HandlerOutput(name="DNS",      type=Boolean.class),
        @HandlerOutput(name="SSL",      type=Boolean.class),
        @HandlerOutput(name="Forced",   type=String.class),
        @HandlerOutput(name="Default",  type=String.class)})
        
        public static void getHttpProtocolSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	HTTPProtocolConfig hp = config.getHTTPServiceConfig().getHTTPProtocolConfig();
        String version = hp.getVersion();
        boolean dns = hp.getDNSLookupEnabled();
        boolean ssl = hp.getSSLEnabled();
        String forced = hp.getForcedType();
        String defaultResponse = hp.getDefaultType();
        handlerCtx.setOutputValue("Version", version);
        handlerCtx.setOutputValue("DNS", dns);
        handlerCtx.setOutputValue("SSL", ssl);   
        handlerCtx.setOutputValue("Forced", forced);    
        handlerCtx.setOutputValue("Default", defaultResponse);   
        
    }     
    
/**
     *	<p> This handler returns the default values for all the attributes in the
     *      HTTP Protocol Config Page </p>
     *	<p> Input value: "ConfigName" -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "Version"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "DNS"       -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "SSL"       -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "Forced"    -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Default"   -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getHttpProtocolDefaultSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Version",  type=String.class),
        @HandlerOutput(name="DNS",      type=Boolean.class),
        @HandlerOutput(name="SSL",      type=Boolean.class),
        @HandlerOutput(name="Forced",   type=String.class),
        @HandlerOutput(name="Default",  type=String.class)})
        
        public static void getHttpProtocolDefaultSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	HTTPProtocolConfig hp = config.getHTTPServiceConfig().getHTTPProtocolConfig();
        String version = hp.getDefaultValue(HTTPProtocolConfigKeys.VERSION_KEY);
        String dns = hp.getDefaultValue(HTTPProtocolConfigKeys.DNS_LOOKUP_ENABLED_KEY );
        String ssl = hp.getDefaultValue(HTTPProtocolConfigKeys.SSL_ENABLED_KEY);
        String forced = hp.getDefaultValue(HTTPProtocolConfigKeys.FORCED_TYPE_KEY);
        String defaultResponse = hp.getDefaultValue(HTTPProtocolConfigKeys.DEFAULT_TYPE_KEY);
        handlerCtx.setOutputValue("Version", version);
        if(dns.equals("true")) {
            handlerCtx.setOutputValue("DNS", true);    
        } else {
            handlerCtx.setOutputValue("DNS", false);
        }   
       if(ssl.equals("true")) {
            handlerCtx.setOutputValue("SSL", true);    
        } else {
            handlerCtx.setOutputValue("SSL", false);
        }           
        handlerCtx.setOutputValue("Forced", forced);    
        handlerCtx.setOutputValue("Default", defaultResponse);   
        
    }         
    
/**
     *	<p> This handler saves the values for all the attributes in the
     *      HTTP Protocol Config Page </p>
     *	<p> Input value: "ConfigName" -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "Version"   -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "DNS"       -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Input value: "SSL"       -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Input value: "Forced"    -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Default"   -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="saveHttpProtocolSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true),
        @HandlerInput(name="Version",  type=String.class),
        @HandlerInput(name="DNS",      type=Boolean.class),
        @HandlerInput(name="SSL",      type=Boolean.class),
        @HandlerInput(name="Forced",   type=String.class),
        @HandlerInput(name="Default",  type=String.class)})
        
        public static void saveHttpProtocolSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
        try{
            HTTPProtocolConfig hp = config.getHTTPServiceConfig().getHTTPProtocolConfig();
            hp.setVersion(((String)handlerCtx.getInputValue("Version")));
            hp.setDNSLookupEnabled(((Boolean)handlerCtx.getInputValue("DNS")).booleanValue());
            hp.setSSLEnabled(((Boolean)handlerCtx.getInputValue("SSL")).booleanValue());
            hp.setForcedType(((String)handlerCtx.getInputValue("Forced")));
            hp.setDefaultType(((String)handlerCtx.getInputValue("Default")));
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
    }    
    
/**
     *	<p> This handler returns the values for all the attributes in the
     *      HTTP File Caching Config Page </p>
     *	<p> Input value: "ConfigName" -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "Globally"   -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "FileTransmission"       -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "Age"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "FileCount"    -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "HashSize"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "MedLimit"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "MedSize"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "SmLimit"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "SmSize"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "FileCaching"   -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getHttpFileCachingSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Globally",  type=Boolean.class),
        @HandlerOutput(name="FileTransmission",      type=Boolean.class),
        @HandlerOutput(name="Age",          type=String.class),
        @HandlerOutput(name="FileCount",    type=String.class),        			
        @HandlerOutput(name="HashSize",     type=String.class),
        @HandlerOutput(name="MedLimit",     type=String.class),
        @HandlerOutput(name="MedSize",      type=String.class),
        @HandlerOutput(name="SmLimit",      type=String.class),
        @HandlerOutput(name="SmSize",  	    type=String.class),
        @HandlerOutput(name="FileCaching",  type=String.class)})
        
        public static void getHttpFileCachingSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	HTTPFileCacheConfig hp = config.getHTTPServiceConfig().getHTTPFileCacheConfig();
        boolean globally = hp.getGloballyEnabled();
        boolean fileTransmission = hp.getFileTransmissionEnabled();
        String age = hp.getMaxAgeInSeconds();
        String fileCount = hp.getMaxFilesCount();
        String hashSize = hp.getHashInitSize();
        String medLimit = hp.getMediumFileSizeLimitInBytes();
        String medSize = hp.getMediumFileSpaceInBytes();
        String smLimit = hp.getSmallFileSizeLimitInBytes();
        String smSize = hp.getSmallFileSpaceInBytes();
        boolean fileCaching = hp.getFileCachingEnabled();
       if(fileCaching == true) {
            handlerCtx.setOutputValue("FileCaching", "ON");    
        } else {
            handlerCtx.setOutputValue("FileCaching", "OFF");
        }              
        handlerCtx.setOutputValue("Globally", globally);
        handlerCtx.setOutputValue("FileTransmission", fileTransmission);
        handlerCtx.setOutputValue("Age", age);   
        handlerCtx.setOutputValue("FileCount", fileCount);    
        handlerCtx.setOutputValue("HashSize", hashSize);   
        handlerCtx.setOutputValue("MedLimit", medLimit);
        handlerCtx.setOutputValue("MedSize", medSize);
        handlerCtx.setOutputValue("SmLimit", smLimit);   
        handlerCtx.setOutputValue("SmSize", smSize);    
        
    }         
    
/**
     *	<p> This handler returns the default values for all the attributes in the
     *      HTTP File Caching Config Page </p>
     *	<p> Input value: "ConfigName" -- Type: <code>java.lang.String</code></p>
    *	<p> Output value: "Globally"   -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "FileTransmission"       -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "Age"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "FileCount"    -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "HashSize"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "MedLimit"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "MedSize"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "SmLimit"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "SmSize"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "FileCaching"   -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getHttpFileCachingDefaultSettings",
  input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},        
    output={
        @HandlerOutput(name="Globally",  type=Boolean.class),
        @HandlerOutput(name="FileTransmission",      type=Boolean.class),
        @HandlerOutput(name="Age",          type=String.class),
        @HandlerOutput(name="FileCount",    type=String.class),        			
        @HandlerOutput(name="HashSize",     type=String.class),
        @HandlerOutput(name="MedLimit",     type=String.class),
        @HandlerOutput(name="MedSize",      type=String.class),
        @HandlerOutput(name="SmLimit",      type=String.class),
        @HandlerOutput(name="SmSize",  	    type=String.class),
        @HandlerOutput(name="FileCaching",  type=String.class)})
        
        public static void getHttpFileCachingDefaultSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
	HTTPFileCacheConfig hp = config.getHTTPServiceConfig().getHTTPFileCacheConfig();
        String globally = hp.getDefaultValue(HTTPFileCacheConfigKeys.GLOBALLY_ENABLED_KEY);
        String fileTransmission = hp.getDefaultValue(HTTPFileCacheConfigKeys.FILE_TRANSMISSION_ENABLED_KEY);
        String age = hp.getDefaultValue(HTTPFileCacheConfigKeys.MAX_AGE_IN_SECONDS_KEY);
        String fileCount = hp.getDefaultValue(HTTPFileCacheConfigKeys.MAX_FILES_COUNT_KEY);
        String hashSize = hp.getDefaultValue(HTTPFileCacheConfigKeys.HASH_INIT_SIZE_KEY);
        String medLimit = hp.getDefaultValue(HTTPFileCacheConfigKeys.MEDIUM_FILE_SIZE_LIMIT_IN_BYTES_KEY);
        String medSize = hp.getDefaultValue(HTTPFileCacheConfigKeys.MEDIUM_FILE_SPACE_IN_BYTES_KEY);
        String smLimit = hp.getDefaultValue(HTTPFileCacheConfigKeys.SMALL_FILE_SIZE_LIMIT_IN_BYTES_KEY);
        String smSize = hp.getDefaultValue(HTTPFileCacheConfigKeys.SMALL_FILE_SPACE_IN_BYTES_KEY);
        String fileCaching = hp.getDefaultValue(HTTPFileCacheConfigKeys.FILE_CACHING_ENABLED_KEY);
        if(globally.equals("true")) {
            handlerCtx.setOutputValue("Globally", true);    
        } else {
            handlerCtx.setOutputValue("Globally", false);
        }   
        if(fileTransmission.equals("true")) {
            handlerCtx.setOutputValue("FileTransmission", true);    
        } else {
            handlerCtx.setOutputValue("FileTransmission", false);
        }        
        if(fileCaching.equals("true")) {
            handlerCtx.setOutputValue("FileCaching", "ON");    
        } else {
            handlerCtx.setOutputValue("FileCaching", "OFF");
        }                
        handlerCtx.setOutputValue("Age", age);   
        handlerCtx.setOutputValue("FileCount", fileCount);    
        handlerCtx.setOutputValue("HashSize", hashSize);   
        handlerCtx.setOutputValue("MedLimit", medLimit);
        handlerCtx.setOutputValue("MedSize", medSize);
        handlerCtx.setOutputValue("SmLimit", smLimit);   
        handlerCtx.setOutputValue("SmSize", smSize);      
        
    }        
    
/**
     *	<p> This handler saves the values for all the attributes in the
     *      HTTP File Caching Config Page </p>
     *	<p> Input value: "ConfigName" -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "Globally"   -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "FileTransmission"       -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "Age"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "FileCount"    -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "HashSize"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "MedLimit"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "MedSize"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "SmLimit"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "SmSize"   -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "FileCaching"   -- Type: <code>java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="saveHttpFileCachingSettings",
   input={
        @HandlerInput(name="ConfigName", type=String.class, required=true),        
        @HandlerInput(name="Globally",  type=Boolean.class),
        @HandlerInput(name="FileTransmission",      type=Boolean.class),
        @HandlerInput(name="Age",          type=String.class),
        @HandlerInput(name="FileCount",    type=String.class),        			
        @HandlerInput(name="HashSize",     type=String.class),
        @HandlerInput(name="MedLimit",     type=String.class),
        @HandlerInput(name="MedSize",      type=String.class),
        @HandlerInput(name="SmLimit",      type=String.class),
        @HandlerInput(name="SmSize",  	    type=String.class),
        @HandlerInput(name="FileCaching",  type=String.class)})
        
        public static void saveHttpFileCachingSettings(HandlerContext handlerCtx) {
        
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
        try{
            HTTPFileCacheConfig hp = config.getHTTPServiceConfig().getHTTPFileCacheConfig();
            hp.setGloballyEnabled(((Boolean)handlerCtx.getInputValue("Globally")).booleanValue());
            hp.setFileTransmissionEnabled(((Boolean)handlerCtx.getInputValue("FileTransmission")).booleanValue());
            hp.setMaxAgeInSeconds(((String)handlerCtx.getInputValue("Age")));
            hp.setMaxFilesCount(((String)handlerCtx.getInputValue("FileCount")));
            hp.setHashInitSize(((String)handlerCtx.getInputValue("HashSize")));
            hp.setMediumFileSizeLimitInBytes(((String)handlerCtx.getInputValue("MedLimit")));
            hp.setMediumFileSpaceInBytes(((String)handlerCtx.getInputValue("MedSize")));
            hp.setSmallFileSizeLimitInBytes(((String)handlerCtx.getInputValue("SmLimit")));
            hp.setSmallFileSpaceInBytes(((String)handlerCtx.getInputValue("SmSize")));
            String fileCaching = (String)handlerCtx.getInputValue("FileCaching");
            if(fileCaching.equals("ON")) {
                hp.setFileCachingEnabled(true);    
            } else {
                hp.setFileCachingEnabled(false);   
            }             
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
        
    }   
    
    
    /*
     *  HTTP Listener Handler
     * /
    
             
    /**
     *	<p> This handler returns the list of specified Listener elements for populating 
     *  <p> the table inHTTP Listeners page
     *  <p> Input  value: "ConfigName"   -- Type: <code> java.lang.String</code></p>
     *  <p> Input  value: "selectedRows" -- Type: <code> java.util.List</code></p>
     *  <p> Output  value: "Result"      -- Type: <code> java.util.List</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getHttpListenersList",
        input={
            @HandlerInput(name="ConfigName", type=String.class, required=true),
            @HandlerInput(name="selectedRows", type=List.class)},
        output={
            @HandlerOutput(name="Result", type=java.util.List.class)}
     )
    public static void getHttpListenersList(HandlerContext handlerCtx){
        ConfigConfig config = AMXRoot.getInstance().getConfig(((String)handlerCtx.getInputValue("ConfigName")));
        List result = new ArrayList();
        Iterator iter = null;
        try{
            iter = config.getHTTPServiceConfig().getHTTPListenerConfigMap().values().iterator();

            List<Map> selectedList = (List)handlerCtx.getInputValue("selectedRows");
            boolean hasOrig = (selectedList == null || selectedList.size()==0) ? false: true;
       
            if (iter != null){
                while(iter.hasNext()){
                    ConfigElement configE = (ConfigElement) iter.next();
                    HashMap oneRow = new HashMap();
                    String name=configE.getName();                
                    oneRow.put("name", name);
                    oneRow.put("selected", (hasOrig)? ConnectorsHandlers.isSelected(name, selectedList): false);
                    HTTPListenerConfig httpConfig = (HTTPListenerConfig)configE; 
                    boolean enabled = httpConfig.getEnabled();
                    String ntwkAddress = httpConfig.getAddress();
                    String listPort = httpConfig.getPort();
                    String virtualServer = httpConfig.getDefaultVirtualServer();
                    oneRow.put("enabled", enabled);
                    oneRow.put("ntwkAddress", (ntwkAddress == null) ? " ": ntwkAddress);
                    oneRow.put("listPort", (listPort == null) ? " ": listPort);
                    oneRow.put("defVirtualServer", (virtualServer == null) ? " ": virtualServer);
                    result.add(oneRow);
                }
            }
        } catch (Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
        handlerCtx.setOutputValue("Result", result);
    }
    
    /**
     *	<p> This handler takes in selected rows, and removes selected Listeners
     *  <p> Input  value: "selectedRows"  -- Type: <code> java.util.List</code></p>
     *  <p> Input  value: "ConfigName"    -- Type: <code> java.lang.String</code></p>
     *  <p> Input  value: "Type"          -- Type: <code> java.lang.String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="deleteHttpListeners",
    input={
        @HandlerInput(name="selectedRows", type=List.class, required=true),
        @HandlerInput(name="ConfigName",   type=String.class, required=true)
        }
    )
    public static void deleteHttpListeners(HandlerContext handlerCtx) {
        String configName = (String)handlerCtx.getInputValue("ConfigName");
        ConfigConfig config = AMXRoot.getInstance().getConfig(configName);
        List obj = (List) handlerCtx.getInputValue("selectedRows");
        List<Map> selectedRows = (List) obj;
        try{
            for(Map oneRow : selectedRows){
                String name = (String)oneRow.get("name");
                
                //Need to use JMX because we also need to remove the references in Virtual server.
                //This is specifed as the http-listeners attribute of the virtual server.
                
                //TODO-V3 TP2  change to use AMX
//                String[] types = new String[]{ "java.lang.String", "java.lang.String"};
//                Object[] params = new Object[]{name,configName};
//                JMXUtil.invoke( "com.sun.appserv:type=configs,category=config", 
//                    "deleteHttpListener", params, types);
            }
        }catch(Exception ex){
           GuiUtil.handleException(handlerCtx, ex);
        }
    }


    
    /**
     *	<p> This handler returns the values for all the attributes in 
     *      New/Edit HTTP Listener Page </p>
     *  <p> Input  value: "Edit"               -- Type: <code>java.lang.String</code></p>
     *  <p> Input  value: "FromStep2"          -- Type: <code>java.lang.String</code></p>
     *  <p> Input  value: "ConfigName"         -- Type: <code>java.lang.String</code></p>
     *  <p> Input  value: "HttpName"           -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Listener"           -- Type: <code>java.lang.Boolean</code></p>
     *	<p> Output value: "NetwkAddr"          -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "ListenerPort"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "DefaultVirtServer"  -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "ServerName"         -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "RedirectPort"       -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Acceptor"           -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "PoweredBy"          -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "Blocking"           -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Output value: "Properties"         -- Type: <code>java.util.Map</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getHttpListenerValues",
    input={
        @HandlerInput(name="Edit",       type=Boolean.class, required=true),
        @HandlerInput(name="FromStep2",  type=Boolean.class, required=true),
        @HandlerInput(name="ConfigName", type=String.class, required=true),
        @HandlerInput(name="HttpName",   type=String.class, required=true) },
    output={
        @HandlerOutput(name="Listener",          type=Boolean.class),
        @HandlerOutput(name="security",          type=Boolean.class),
        @HandlerOutput(name="NetwkAddr",         type=String.class), 
        @HandlerOutput(name="ListenerPort",      type=String.class),
        @HandlerOutput(name="DefaultVirtServer", type=String.class),
        @HandlerOutput(name="ServerName",        type=String.class),
        @HandlerOutput(name="RedirectPort",      type=String.class),
        @HandlerOutput(name="Acceptor",          type=String.class),
        @HandlerOutput(name="PoweredBy",         type=Boolean.class),
        @HandlerOutput(name="Blocking",          type=Boolean.class),
        @HandlerOutput(name="Properties",        type=Map.class)})
        
        public static void getHttpListenerValues(HandlerContext handlerCtx) {
        try{
            Boolean edit = (Boolean) handlerCtx.getInputValue("Edit");
            Boolean fromStep2 = (Boolean) handlerCtx.getInputValue("FromStep2");
            if(!edit){
                if((fromStep2 == null) || (! fromStep2)){
                    handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("httpProps", new HashMap());
                    handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("sslProps", null);
                    //TODO-V3 TP2 cannot get default value
                    /*
                    Map<String, String> httpAttrMap = AMXRoot.getInstance().getDomainConfig().getDefaultAttributeValues(HTTPListenerConfig.J2EE_TYPE);
                    handlerCtx.setOutputValue("Listener", httpAttrMap.get("enabled"));
                    handlerCtx.setOutputValue("security", httpAttrMap.get("security-enabled"));
                    handlerCtx.setOutputValue("Acceptor", httpAttrMap.get("acceptor-threads"));
                    handlerCtx.setOutputValue("PoweredBy", httpAttrMap.get("xpowered-by"));
                    handlerCtx.setOutputValue("Blocking", httpAttrMap.get("blocking-enabled"));
                    */
                }else{
                    Map props = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("httpProps");
                    handlerCtx.setOutputValue("Listener", props.get("enabled"));
                    handlerCtx.setOutputValue("security", props.get("securityEnabled"));
                    handlerCtx.setOutputValue("NetwkAddr", props.get("address"));
                    handlerCtx.setOutputValue("ListenerPort", props.get("port"));
                    handlerCtx.setOutputValue("DefaultVirtServer", props.get("virtualServer"));
                    handlerCtx.setOutputValue("ServerName", props.get("serverName"));
                    handlerCtx.setOutputValue("RedirectPort", props.get("redirectPort"));
                    handlerCtx.setOutputValue("Acceptor", props.get("acceptor-threads"));
                    handlerCtx.setOutputValue("PoweredBy", props.get("xpowered-by"));
                    handlerCtx.setOutputValue("Blocking", props.get("blocking-enabled"));
                    handlerCtx.setOutputValue("Properties", props.get("options"));
                }
                return;
            }
            String configName = (String) handlerCtx.getInputValue("ConfigName");
            String httpListenerName = (String) handlerCtx.getInputValue("HttpName");
            ConfigConfig config = AMXRoot.getInstance().getConfig(configName);
            HTTPListenerConfig httpListConfig = config.getHTTPServiceConfig().getHTTPListenerConfigMap().get(httpListenerName);
            handlerCtx.setOutputValue("Listener", httpListConfig.getEnabled());
            handlerCtx.setOutputValue("security", httpListConfig.getSecurityEnabled());
            handlerCtx.setOutputValue("NetwkAddr", httpListConfig.getAddress());
            handlerCtx.setOutputValue("ListenerPort", httpListConfig.getPort());
            handlerCtx.setOutputValue("DefaultVirtServer", httpListConfig.getDefaultVirtualServer());
            handlerCtx.setOutputValue("ServerName", httpListConfig.getServerName());
            handlerCtx.setOutputValue("RedirectPort", httpListConfig.getRedirectPort());
            handlerCtx.setOutputValue("Acceptor", httpListConfig.getAcceptorThreads());
            handlerCtx.setOutputValue("PoweredBy", httpListConfig.getXpoweredBy());
            handlerCtx.setOutputValue("Blocking", httpListConfig.getBlockingEnabled());
            Map<String, String> pMap = httpListConfig.getProperties();
            
            //refer to issue#2920; If we want to hide this property, just uncomment the following 2 lines.
            //if (httpListenerName.equals(ADMIN_LISTENER))
            //    pMap.remove(PROXIED_PROTOCOLS);
            
            handlerCtx.setOutputValue("Properties", pMap);
        }catch (Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
    }
    
    /**
     *	<p> This handler saves the values for all the attributes in 
     *      New/Edit HTTP Listener Page </p>
     *  <p> Input value: "ConfigName         -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "HttpName           -- Type: <code>java.lang.String</code></p>
     *	<p> Input value: "Edit"              -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Input value: "NetwkAddr"         -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "ListenerPort"      -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "DefaultVirtServer" -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "ServerName"        -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Listener"          -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Input value: "security"          -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Input value: "RedirectPort"      -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "Acceptor"          -- Type: <code>java.lang.String</code></p>
     *  <p> Input value: "PoweredBy"         -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Input value: "Blocking"          -- Type: <code>java.lang.Boolean</code></p>
     *  <p> Input value: "AddProps"          -- Type: <code>java.util.Map</code></p>
     *  <p> Input value: "RemoveProps"       -- Type: <code>java.util.ArrayList</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="saveHttpListenerValues",
    input={
        @HandlerInput(name="ConfigName",        type=String.class, required=true),
        @HandlerInput(name="HttpName",          type=String.class, required=true),
        @HandlerInput(name="Edit",              type=Boolean.class, required=true),
        @HandlerInput(name="NetwkAddr",         type=String.class, required=true),
        @HandlerInput(name="ListenerPort",      type=String.class, required=true),
        @HandlerInput(name="DefaultVirtServer", type=String.class, required=true),
        @HandlerInput(name="ServerName",        type=String.class, required=true),
        @HandlerInput(name="Listener",          type=Boolean.class),
        @HandlerInput(name="security",          type=Boolean.class),
        @HandlerInput(name="RedirectPort",      type=String.class),
        @HandlerInput(name="Acceptor",          type=String.class),
        @HandlerInput(name="PoweredBy",         type=Boolean.class),
        @HandlerInput(name="Blocking",          type=Boolean.class),
        @HandlerInput(name="AddProps",          type=Map.class),
        @HandlerInput(name="RemoveProps",       type=ArrayList.class) })
        
        public static void saveHttpListenerValues(HandlerContext handlerCtx) {
        String configName = (String) handlerCtx.getInputValue("ConfigName");
        String httpListenerName = (String) handlerCtx.getInputValue("HttpName");
        String listPort = (String)handlerCtx.getInputValue("ListenerPort");
        String address = (String)handlerCtx.getInputValue("NetwkAddr");
        String virtualServer = (String)handlerCtx.getInputValue("DefaultVirtServer");
        String serverName = (String)handlerCtx.getInputValue("ServerName");
        ConfigConfig config = AMXRoot.getInstance().getConfig(configName);       
        try{
            Boolean edit = (Boolean) handlerCtx.getInputValue("Edit");
            if(!edit){
                Map httpPropsMap = new HashMap();
                httpPropsMap.put("httpName", httpListenerName);
                httpPropsMap.put("address", address);
                httpPropsMap.put("port", listPort);
                httpPropsMap.put("virtualServer", virtualServer);
                httpPropsMap.put("serverName", serverName);
                httpPropsMap.put("options", (Map)handlerCtx.getInputValue("AddProps"));
                httpPropsMap.put("enabled", (Boolean)handlerCtx.getInputValue("Listener"));
                httpPropsMap.put("securityEnabled", (Boolean)handlerCtx.getInputValue("security"));
                httpPropsMap.put("redirectPort", (String)handlerCtx.getInputValue("RedirectPort"));
                httpPropsMap.put("acceptor-threads", (String)handlerCtx.getInputValue("Acceptor"));
                httpPropsMap.put("xpowered-by", (Boolean)handlerCtx.getInputValue("PoweredBy")); 
                httpPropsMap.put("blocking-enabled", (Boolean)handlerCtx.getInputValue("Blocking")); 
                handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("httpProps", httpPropsMap);
                //the actual creation is in step 2 of the wizard.
            } else {
                HTTPListenerConfig httpListConfig = config.getHTTPServiceConfig().getHTTPListenerConfigMap().get(httpListenerName);
                String previousVSName = httpListConfig.getDefaultVirtualServer();
                httpListConfig.setAddress(address);
                httpListConfig.setPort((String)handlerCtx.getInputValue("ListenerPort"));
                httpListConfig.setDefaultVirtualServer(virtualServer);
                httpListConfig.setServerName(serverName);
                httpListConfig.setEnabled((Boolean)handlerCtx.getInputValue("Listener"));
                httpListConfig.setSecurityEnabled((Boolean)handlerCtx.getInputValue("security"));
                httpListConfig.setRedirectPort((String)handlerCtx.getInputValue("RedirectPort"));
                httpListConfig.setAcceptorThreads((String)handlerCtx.getInputValue("Acceptor"));
                httpListConfig.setXpoweredBy((Boolean)handlerCtx.getInputValue("PoweredBy"));
                httpListConfig.setBlockingEnabled((Boolean)handlerCtx.getInputValue("Blocking"));
                AMXRoot.getInstance().editProperties(handlerCtx, httpListConfig);
                
                //refer to issue #2920
                if (httpListenerName.equals(ADMIN_LISTENER)){
                    if (httpListConfig.getSecurityEnabled()){
                        if (httpListConfig.existsProperty(PROXIED_PROTOCOLS))
                            httpListConfig.setPropertyValue(PROXIED_PROTOCOLS, PROXIED_PROTOCOLS_VALUE);
                         else
                             httpListConfig.createProperty(PROXIED_PROTOCOLS, PROXIED_PROTOCOLS_VALUE);
                    }else{
                        if (httpListConfig.existsProperty(PROXIED_PROTOCOLS))
                            httpListConfig.removeProperty(PROXIED_PROTOCOLS);
                    }
                }
                
                //Also need to change the http-listeners attributes of Virtual Server.
                Map<String,VirtualServerConfig>vservers = config.getHTTPServiceConfig().getVirtualServerConfigMap();
                VirtualServerConfig previousVS = vservers.get(previousVSName);
                VirtualServerConfig newVS = vservers.get(virtualServer);
                String hl = previousVS.getHTTPListeners();
                String[] hlArray = GuiUtil.stringToArray(hl, ",");
                
                //remove from previous VS.
                String tmp = "";
                for(int i=0; i<hlArray.length; i++){
                    if (! hlArray[i].equals(httpListenerName))
                        tmp= (tmp.equals("") )? hlArray[i] : tmp+","+hlArray[i];
                }
                previousVS.setHTTPListeners(tmp);
                
                //add to current VS.
                tmp = newVS.getHTTPListeners();
                if (GuiUtil.isEmpty(tmp))
                    newVS.setHTTPListeners(httpListenerName);
                else{
                    tmp = newVS.getHTTPListeners()+","+httpListenerName;
                    newVS.setHTTPListeners(tmp);
                }
                    
            }
            
        }catch (Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
    }
    
    /**
     *	<p> This handler returns the values for list of thread pools in 
     *      ORB Page </p>
     *  <p> Input  value: "ConfigName               -- Type: <code>java.lang.String</code></p>
     *	<p> Output value: "DefaultVirtualServers"   -- Type: <code>SelectItem[].class 
     *      SelectItem[] (castable to Option[])</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getDefaultVirtualServers",
    input={
        @HandlerInput(name="ConfigName", type=String.class, required=true)},
    output={
        @HandlerOutput(name="DefaultVirtualServers",  type=java.util.List.class)})
        
        public static void getDefaultVirtualServers(HandlerContext handlerCtx) {
            String configName = (String) handlerCtx.getInputValue("ConfigName");
            ConfigConfig config = AMXRoot.getInstance().getConfig(configName);
            Iterator<String> iter = config.getHTTPServiceConfig().getVirtualServerConfigMap().keySet().iterator();
            List options = new ArrayList();
            while(iter.hasNext()){
                    options.add( iter.next());
                }

            handlerCtx.setOutputValue("DefaultVirtualServers", options);
        }

    
    //mbean Attribute Name
    private static List httpServiceSkipPropsList = new ArrayList();
    
    static {
        httpServiceSkipPropsList.add("accessLogBufferSize");
        httpServiceSkipPropsList.add("accessLogWriteInterval");
        httpServiceSkipPropsList.add("accessLoggingEnabled");
    }
    
    
    private static final String ADMIN_LISTENER = "admin-listener";
    private static final String PROXIED_PROTOCOLS = "proxiedProtocols";
    private static final String PROXIED_PROTOCOLS_VALUE = "http";
    
}
