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

package com.sun.enterprise.connectors.system;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.server.ServerContext;
import com.sun.enterprise.server.ApplicationServer;
import com.sun.logging.LogDomains;
import com.sun.enterprise.config.ConfigContext;
import com.sun.enterprise.config.ConfigException;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.connectors.ConnectorRuntimeException;
import com.sun.enterprise.connectors.util.JmsRaUtil;

/** 
 * Defines an MQ addressList. 
 *
 * @author Binod P.G
 */
public class MQAddressList {

    static Logger logger = LogDomains.getLogger(LogDomains.RSR_LOGGER);
    private static String myName = 
               System.getProperty(SystemPropertyConstants.SERVER_NAME);

    private List<MQUrl> urlList = new ArrayList<MQUrl>();

    private JmsService jmsService = null;
    private AppserverClusterViewFromCacheRepository rep = null;
    private static String nodeAgentHost = null;
    private String targetName = null;

    /**
     * Create an empty address list
     */
    public MQAddressList() throws ConfigException {
        this(null);
    }

    /**
     * Use the provided <code>JmsService</code> to create an addresslist
     */
    public MQAddressList(JmsService service) throws ConfigException {
        //use the server instance this is being run in as the target
        this(service, getServerName());
    }
    
    /**
     * Creates an instance from jmsService and resolves 
     * values using the provided target name
     * @param targetName Represents the target for which the addresslist 
     * needs to be created
     * @param service <code>JmsService</code> instance.
     */
    public MQAddressList(JmsService service, String targetName) throws ConfigException {
        logFine(" init" + service + "target " + targetName);
        this.jmsService = service;
        this.targetName = targetName;
    }

    /**
     * Sets up the addresslist.
     */
    public void setup() throws ConfigException {
        try {
            if (isClustered() && (!this.jmsService.getType().equals(ActiveJmsResourceAdapter.REMOTE)) ) {
                //setup for LOCAL/EMBEDDED clusters.
                logFine("setting up for cluster " +  this.targetName);
                setupClusterViewFromRepository();
                setupForCluster();
            } else {
                logFine("setting up for SI/DAS " + this.targetName);
                if (isAConfig(targetName) || isDAS(targetName)) {
                    logFine("performing default setup for DAS/remote clusters/PE instance" + targetName);
                    defaultSetup();
                } else {
                    logFine("configuring for Standalone EE server instance");
                    //resolve and add.
                    setupClusterViewFromRepository();
                    setupForStandaloneServerInstance();
                }
            }
        } catch (ConnectorRuntimeException ce) {
            throw new ConfigException(ce);
        }
    }

    private void setupClusterViewFromRepository() throws ConfigException {
        ServerContext context = ApplicationServer.getServerContext();
        Server server = context.getConfigBean();
        String domainurl = context.getServerConfigURL();
        rep = new AppserverClusterViewFromCacheRepository(domainurl);
        try {
            nodeAgentHost = rep.getNodeAgentHostName(server);
            logFine("na host" + nodeAgentHost);
        } catch (Exception e) {
            logger.log(Level.FINE,"Exception while attempting to get nodeagentHost", e.getMessage());
            logger.log(Level.FINER, e.getMessage(), e);
        }
    }

    public String getMasterBroker(String clustername) {
	String masterbrk = null;
	if (rep != null) {
	    try {
		JmsHost mb = rep.getMasterJmsHostInCluster(clustername);
		JmsService js = rep.getJmsServiceForMasterBroker
                                 (clustername);
		MQUrl url = createUrl(mb, js); 
		masterbrk = url.toString();
		logger.log(Level.FINE, "Master broker obtained is " 
	           + masterbrk);
	    }
	    catch (Exception e) {
		logger.log(Level.SEVERE, "Cannot obtain master broker");
		logger.log(Level.SEVERE, e.getMessage(), e);
	    }
	}
	return masterbrk;
    }
	
    private boolean isDAS(String targetName) throws ConfigException {
        if (isAConfig(targetName)) {
            return false;   
        }
        return ServerHelper.isDAS(getAdminConfigContext(), targetName);
    }
    
    private boolean isAConfig(String targetName) throws ConfigException {
        return ServerHelper.isAConfig(getAdminConfigContext(), targetName);
    }
    

    /**
     * Gets the admin config context associated with this server instance
     * Usage Notice: Use this only for operations that are performed in DAS
     * and requires the admin config context
     */
    private ConfigContext getAdminConfigContext() {
        return com.sun.enterprise.admin.server.core.AdminService.
                   getAdminService().getAdminContext().getAdminConfigContext();
    }

    /**
     * Setup addresslist for Standalone server instance in EE
     */
    private void setupForStandaloneServerInstance() throws ConfigException {
        if (jmsService.getType().equals(ActiveJmsResourceAdapter.REMOTE)) {
            logFine("REMOTE Standalone server instance and hence use default setup");
            defaultSetup();
        } else {
            //For LOCAL or EMBEDDED standalone server instances, we need to resolve
            //the JMSHost
            logFine("LOCAL/EMBEDDED Standalone server instance");
            JmsHost host = getResolvedJmsHostForStandaloneServerInstance(this.targetName);
            MQUrl url = createUrl(host);
            urlList.add(url);
        }            
    }

    /**
     * Default setup concatanates all JMSHosts in a JMSService to create the address list
     */
    private void defaultSetup() throws ConfigException {
        logFine("performing defaultsetup");
        JmsHost[] hosts = jmsService.getJmsHost();
        for (int i=0; i < hosts.length; i++) {
            MQUrl url = createUrl(hosts[i]);
            urlList.add(url);
        }
    }

    /**
     * Setup the address list after calculating the JMS hosts
     * belonging to the local appserver cluster members.
     * For LOCAL/EMBEDDED clusters the MQ broker corresponding
     * to "this" server instance needs to be placed ahead
     * of the other brokers of the other siblings in the AS
     * cluter to enable sticky connection balancing by MQ.
     */
    private void setupForCluster() throws ConfigException {
        java.util.Map<String,JmsHost> hostMap = 
            rep.getResolvedLocalJmsHostsInMyCluster(true);
        //First add my jms host.
        JmsHost jmsHost = hostMap.get(myName); 
        MQUrl myUrl = createUrl(jmsHost, nodeAgentHost);
        urlList.add(myUrl);
        hostMap.remove(myName);
        
        // Add all buddies to URL.
        for (JmsHost host : hostMap.values() ) {
            MQUrl url = createUrl(host);
            urlList.add(url);
        }
    }
    

    /**
     * Creates a String representation of address list from
     * array list. In short, it is a comma separated list.
     * Actual syntax of an MQ url is inside MQUrl class.
     * 
     * @returns AddressList String
     * @see MQUrl
     */
    public String toString() {
        String s = "";
	
        Iterator it = urlList.iterator();
	if (it.hasNext()) {
            s = it.next().toString();
	}
	
        while (it.hasNext()) {
            s = s + "," +  it.next().toString();
        }

        logFine("toString returns :: " + s);
        return s;
    }    

    /**
     * Creates an instance of MQUrl from JmsHost element in
     * the dtd and add it to the addresslist.
     *
     * @param host An instance of <code>JmsHost</code> object.
     */
    public void addMQUrl(JmsHost host) {
        MQUrl url = createUrl(host);
        urlList.add(url);
    }

    /**
     * Deletes the url represented by the JmsHost from the AddressList.
     *
     * @param host An instance of <code>JmsHost</code> object.
     */
    public void removeMQUrl(JmsHost host) {
        MQUrl url = createUrl(host);
        urlList.remove(url);
    }

    /**
     * Updates the information about the <code>JmsHost</code>
     * in the address list.
     *
     * @param host An instance of <code>JmsHost</code> object.
     */
    public void updateMQUrl(JmsHost host) {
        MQUrl url = createUrl(host);
        urlList.remove(url);
        urlList.add(url);
    }

    private MQUrl createUrl(JmsHost host) {
        return createUrl(host, this.jmsService);
    }
    
    private MQUrl createUrl(JmsHost host, String overridedHostName) {
        return createUrl(host, this.jmsService, overridedHostName);
    }
    
    public static MQUrl createUrl(JmsHost host, JmsService js) {
        return createUrl(host, js, null);
    }
    
    public static MQUrl createUrl(JmsHost host, JmsService js, String overridedHostName) {
        try {
        String name = host.getName();
        String hostName = host.getHost();
        // For LOCAL/EMBEDDED Clustered instances and 
        // standalone server instances, use
        // their nodeagent's hostname as the jms host name.
        ServerContext serverContext = ApplicationServer.getServerContext();
        Server server = serverContext.getConfigBean();
        if (overridedHostName != null && !overridedHostName.trim().equals("")) {
           hostName = overridedHostName;
        }

        String port = host.getPort();
        MQUrl url = new MQUrl(name);
        url.setHost(hostName);
        url.setPort(port);
        if (js != null) {
            String scheme = js.getMqScheme();
            if (scheme != null && !scheme.trim().equals("")) {
                url.setScheme(scheme);
            }

            String service = js.getMqService();
            if (service != null && !service.trim().equals("")) {
                url.setService(service);
            }
        }
        return url;
        } catch (ConfigException ce) {
            ce.printStackTrace();
        }
        return null;
    }
    
     //Used to get resolved local JmsHost for a standalone server instance
    private JmsHost getResolvedJmsHostForStandaloneServerInstance(
                                         String serverName) throws ConfigException {
        logFine(" getresolved " + serverName);
       ConfigContext con =  getAdminConfigContext();
       Server serverInstance = ServerHelper.getServerByName(con, serverName);
       logFine("serverinstace " + serverInstance);
       JmsHost jmsHost = getResolvedJmsHost(serverInstance);
       return jmsHost;
    }

    private JmsHost getResolvedJmsHost(Server as) throws ConfigException{
        logFine("getResolvedJmsHost " + as);
        return rep.getResolvedJmsHost(as);
    }

    private boolean isClustered() throws ConnectorRuntimeException {
        return JmsRaUtil.isClustered();
    }
    
    private static String getServerName() {
        String serverName=System.getProperty(SystemPropertyConstants.SERVER_NAME);
        return serverName;
    }
    
    private void logFine(String s) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "MQAddressList :: " + s);
        }
    }

    public int getSize() {
        if (this.urlList != null) {
            return this.urlList.size();
        } else {
            return 0;
        }
    }
}
