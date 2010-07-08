/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.enterprise.config.serverbeans;

import com.sun.enterprise.config.util.PortManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.logging.LogDomains;
import java.io.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.config.support.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.glassfish.quality.ToDo;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.constraints.Min;
import org.glassfish.api.admin.ServerEnvironment;

/**
 *
 * Java EE Application Server Configuration
 *
 * Each Application Server instance is a Java EE compliant container. One
 * server instance is specially designated as the Administration Server in SE/EE
 *
 * User applications cannot be deployed to an Administration Server instance
 */
@Configured
@SuppressWarnings("unused")
public interface Server extends ConfigBeanProxy, Injectable, PropertyBag, Named, SystemPropertyBag, ReferenceContainer, RefContainer {

    @Param(name = "name", primary = true)
    public void setName(String value) throws PropertyVetoException;


    /**
     * Gets the value of the configRef property.
     *
     * Points to a named config. Needed for stand-alone servers. If server
     * instance is part of a cluster, then it points to the cluster config
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getConfigRef();

    /**
     * Sets the value of the configRef property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name = "config", optional = true)
    void setConfigRef(String value) throws PropertyVetoException;

    /**
     * Gets the value of the nodeAgentRef property.
     *
     * SE/EE only. Specifies name of node agent where server instance is hosted
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getNodeAgentRef();

    /**
     * Sets the value of the nodeAgentRef property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name = "nodeagent", optional = true)
    void setNodeAgentRef(String value) throws PropertyVetoException;

     /**
     * Sets the value of the node property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name = "node", optional = true)
    void setNode(String value) throws PropertyVetoException;
    /**
     * Gets the value of the node property.
     *
     * SE/EE only. Specifies name of node agent where server instance is hosted
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getNode();
    /**
     * Gets the value of the lbWeight property.
     *
     * Each server instance in a cluster has a weight, which may be used to
     * represent the relative processing capacity of that instance. Default
     * weight is 100 for every instance. Weighted load balancing policies will
     * use this weight while load balancing requests within the cluster.
     * It is the responsibility of the administrator to set the relative weights
     * correctly, keeping in mind deployed hardware capacity
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue = "100")
    @Min(value = 1)
    String getLbWeight();

    /**
     * Sets the value of the lbWeight property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    void setLbWeight(String value) throws PropertyVetoException;

    /**
     * Gets the value of the systemProperty property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the systemProperty property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSystemProperty().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link SystemProperty }
     */
    @ToDo(priority = ToDo.Priority.IMPORTANT, details = "Provide PropertyDesc for legal system properties")
    @Element
    @Param(name = "systemproperties", optional = true)
    List<SystemProperty> getSystemProperty();

    /**
    Properties as per {@link PropertyBag}
     */
    @ToDo(priority = ToDo.Priority.IMPORTANT, details = "Provide PropertyDesc for legal props")
    @PropertiesDesc(props = {})
    @Element
    @Param(name = "properties", optional = true)
    List<Property> getProperty();

    @DuckTyped
    String getReference();

    @DuckTyped
    ResourceRef getResourceRef(String name);

    @DuckTyped
    boolean isResourceRefExists(String refName);

    @DuckTyped
    void deleteResourceRef(String name) throws TransactionFailure;

    @DuckTyped
    void createResourceRef(String enabled, String refName) throws TransactionFailure;

    @DuckTyped
    ApplicationRef getApplicationRef(String appName);

    /**
     * Returns the cluster instance this instance is referenced in or null
     * if there is no cluster referencing this server instance.
     *
     * @return the cluster owning this instance or null if this is a standalone
     * instance
     */
    @DuckTyped
    Cluster getCluster();

    // four trivial methods that ReferenceContainer's need to implement
    @DuckTyped
    @Override
    boolean isCluster();

    @DuckTyped
    @Override
    boolean isServer();

    @DuckTyped
    @Override
    boolean isDas();

    @DuckTyped
    @Override
    boolean isInstance();

    class Duck {
        public static boolean isCluster(Server server) { return false; }
        public static boolean isServer(Server server)  { return true; }
        public static boolean isInstance(Server server) {
            String name = (server == null) ? null : server.getName();
            return name != null && !name.equals("server");
        }
        public static boolean isDas(Server server) {
            String name = (server == null) ? null : server.getName();
            return "server".equals(name);
        }

        public static Cluster getCluster(Server server) {
            Dom serverDom = Dom.unwrap(server);
            Clusters clusters = serverDom.getHabitat().getComponent(Clusters.class);
            if (clusters != null) {
                for (Cluster cluster : clusters.getCluster()) {
                    for (ServerRef serverRef : cluster.getServerRef()) {
                        if (serverRef.getRef().equals(server.getName())) {
                            return cluster;
                        }
                    }
                }
            }
            return null;
        }

        public static String getReference(Server server) {
            return server.getConfigRef();
        }

        public static ApplicationRef getApplicationRef(Server server,
                String appName) {
            for (ApplicationRef appRef : server.getApplicationRef()) {
                if (appRef.getRef().equals(appName)) {
                    return appRef;
                }
            }
            return null;
        }

        public static ResourceRef getResourceRef(Server server, String refName) {
            for (ResourceRef ref : server.getResourceRef()) {
                if (ref.getRef().equals(refName)) {
                    return ref;
                }
            }
            return null;
        }

        public static boolean isResourceRefExists(Server server, String refName) {
            return getResourceRef(server, refName) != null;
        }

        public static void deleteResourceRef(Server server, String refName) throws TransactionFailure {
            final ResourceRef ref = getResourceRef(server, refName);
            if (ref != null) {
                ConfigSupport.apply(new SingleConfigCode<Server>() {

                    public Object run(Server param) {
                        return param.getResourceRef().remove(ref);
                    }
                }, server);
            }
        }

        public static void createResourceRef(Server server, final String enabled, final String refName) throws TransactionFailure {

            ConfigSupport.apply(new SingleConfigCode<Server>() {

                public Object run(Server param) throws PropertyVetoException, TransactionFailure {

                    ResourceRef newResourceRef = param.createChild(ResourceRef.class);
                    newResourceRef.setEnabled(enabled);
                    newResourceRef.setRef(refName);
                    param.getResourceRef().add(newResourceRef);
                    return newResourceRef;
                }
            }, server);
        }
    }

    @Service
    @Scoped(PerLookup.class)
    class CreateDecorator implements CreationDecorator<Server> {

        @Param(name = "cluster", optional = true)
        String clusterName;

        @Param(name="node",optional=true)
        String node=null;

        @Inject
        Domain domain;

        @Inject
        private ServerEnvironment env;

        @Override
        public void decorate(AdminCommandContext context, Server instance) throws TransactionFailure, PropertyVetoException {
            Config ourConfig = null;
            Cluster ourCluster = null;
            Logger logger = LogDomains.getLogger(Cluster.class, LogDomains.ADMIN_LOGGER);
            LocalStringManagerImpl localStrings = new LocalStringManagerImpl(Server.class);
            Transaction tx = Transaction.getTransaction(instance);
            String configRef = instance.getConfigRef();
            Clusters clusters = domain.getClusters();

            if (tx==null) {
                throw new TransactionFailure(localStrings.getLocalString(
                        "noTransaction", "Internal Error - Cannot obtain transaction object"));
            }


            if(node==null)
                instance.setNode("localhost");
            //There should be no cluster/config with the same name as the server
            if (((clusters != null && domain.getClusterNamed(instance.getName()) != null))
                    || (domain.getConfigNamed(instance.getName()) != null)) {
                throw new TransactionFailure(localStrings.getLocalString(
                        "cannotAddDuplicate", "There is an instance {0} already present.", instance.getName()));
            }
            // cluster instance using cluster config
            if (clusterName != null) {
                if (configRef != null) {
                    throw new TransactionFailure(localStrings.getLocalString(
                            "Server.cannotSpecifyBothConfigAndCluster",
                            "A configuration name and cluster name cannot both be specified."));
                }
                boolean clusterExists = false;

                if (clusters != null) {
                    for (Cluster cluster : clusters.getCluster()) {
                        if (cluster != null && clusterName.equals(cluster.getName())) {
                            ourCluster = cluster;
                            String configName = cluster.getConfigRef();
                            instance.setConfigRef(configName);
                            clusterExists = true;
                            ourConfig = domain.getConfigNamed(configName);
                            break;
                        }
                    }
                }

                if (!clusterExists) {
                    throw new TransactionFailure(localStrings.getLocalString(
                            "noSuchCluster", "Cluster {0} does not exist.", clusterName));
                }

                Cluster cluster = domain.getClusterNamed(clusterName);

                final String instanceName = instance.getName();
                try {
                    File configConfigDir = new File(env.getConfigDirPath(), cluster.getConfigRef());
                    new File(configConfigDir, "docroot").mkdirs();
                    new File(configConfigDir, "lib/ext").mkdirs();
                } catch (Exception e) {
                    // no big deal - just ignore
                }

                if (cluster != null) {
                    if (tx != null) {
                        Cluster c = tx.enroll(cluster);
                        ServerRef newServerRef = c.createChild(ServerRef.class);
                        newServerRef.setRef(instanceName);
                        c.getServerRef().add(newServerRef);
                    }
                }
            }

            // instance using specified config
            if (configRef != null) {
                Config specifiedConfig = domain.getConfigs().getConfigByName(configRef);
                if (specifiedConfig == null) {
                    throw new TransactionFailure(localStrings.getLocalString(
                            "noSuchConfig", "Configuration {0} does not exist.", configRef));
                }
                ourConfig = specifiedConfig;
                try {
                    File configConfigDir = new File(env.getConfigDirPath(), specifiedConfig.getName());
                    new File(configConfigDir, "docroot").mkdirs();
                    new File(configConfigDir, "lib/ext").mkdirs();
                } catch (Exception e) {
                    // no big deal - just ignore
                }
            }

            //stand-alone instance using default-config if config not specified
            if (configRef == null && clusterName == null) {
                Config defaultConfig = domain.getConfigs().getConfigByName("default-config");

                if (defaultConfig == null) {
                    final String msg = localStrings.getLocalString(Server.class,
                            "Cluster.noDefaultConfig",
                            "Can''t find the default config (an element named \"default-config\") "
                            + "in domain.xml.  You may specify the name of an existing config element next time.");

                    logger.log(Level.SEVERE, msg);
                    throw new TransactionFailure(msg);
                }

                Configs configs = domain.getConfigs();
                Configs writableConfigs = tx.enroll(configs);

                final Config configCopy;
                try {
                    configCopy = (Config) defaultConfig.deepCopy(writableConfigs);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, localStrings.getLocalString(Server.class,
                            "Cluster.error_while_copying",
                            "Error while copying the default configuration {0}",
                            e.toString(), e));
                    throw new TransactionFailure(e.toString(), e);
                }
                ourConfig = configCopy;

                final String configName = instance.getName() + "-config";
                instance.setConfigRef(configName);
                try {
                    File configConfigDir = new File(env.getConfigDirPath(), configName);
                    new File(configConfigDir, "docroot").mkdirs();
                    new File(configConfigDir, "lib/ext").mkdirs();
                } catch (Exception e) {
                    // no big deal - just ignore
                }

                configCopy.setName(configName);
                writableConfigs.getConfig().add(configCopy);
            }

            for (Resource resource : domain.getResources().getResources()) {
                if (resource.getObjectType().equals("system-all") || resource.getObjectType().equals("system-instance")) {
                    String name = null;
                    if (resource instanceof BindableResource) {
                        name = ((BindableResource) resource).getJndiName();
                    }
                    if (resource instanceof Named) {
                        name = ((Named) resource).getName();
                    }
                    if (name == null) {
                        throw new TransactionFailure("Cannot add un-named resources to the new server instance");
                    }
                    ResourceRef newResourceRef = instance.createChild(ResourceRef.class);
                    newResourceRef.setRef(name);
                    instance.getResourceRef().add(newResourceRef);
                }
            }
            for (Application application : domain.getApplications().getApplications()) {
                if (application.getObjectType().equals("system-all") || application.getObjectType().equals("system-instance")) {
                    ApplicationRef newAppRef = instance.createChild(ApplicationRef.class);
                    newAppRef.setRef(application.getName());
                    // todo : what about virtual-servers ?
                    instance.getApplicationRef().add(newAppRef);
                }
            }
            
            this.addClusterRefs(ourCluster, instance);

            PortManager pm = new PortManager(ourCluster, ourConfig, domain, instance);
            pm.process(); // might throw
        }

        private void addClusterRefs(Cluster cluster, Server instance) throws TransactionFailure, PropertyVetoException {
            if (cluster != null) {
                for (ApplicationRef appRef : cluster.getApplicationRef()) {
                    ApplicationRef newAppRef = instance.createChild(ApplicationRef.class);
                    newAppRef.setRef(appRef.getRef());
                    newAppRef.setDisableTimeoutInMinutes(appRef.getDisableTimeoutInMinutes());
                    newAppRef.setEnabled(appRef.getEnabled());
                    newAppRef.setLbEnabled(appRef.getLbEnabled());
                    newAppRef.setVirtualServers(appRef.getVirtualServers());
                    instance.getApplicationRef().add(newAppRef);
                }
                for (ResourceRef rr : cluster.getResourceRef()) {
                    ResourceRef newRR = instance.createChild(ResourceRef.class);
                    newRR.setRef(rr.getRef());
                    newRR.setEnabled(rr.getEnabled());
                    instance.getResourceRef().add(newRR);
                }
            }
        }
    }

    @Service
    @Scoped(PerLookup.class)
    class DeleteDecorator implements DeletionDecorator<Servers, Server> {

        // for backward compatibility, ignored.
        @Param(name="nodeagent", optional=true)
        String nodeagent;
        
        @Inject
        Configs configs;
        @Inject
        private Domain domain;
        @Inject
        private ServerEnvironment env;

        @Override
        public void decorate(AdminCommandContext context, Servers parent, final Server child) throws PropertyVetoException, TransactionFailure {
            final Logger logger = LogDomains.getLogger(Server.class, LogDomains.ADMIN_LOGGER);
            LocalStringManagerImpl localStrings = new LocalStringManagerImpl(Server.class);
            final ActionReport report = context.getActionReport();
            Transaction t = Transaction.getTransaction(parent);
            Cluster cluster = domain.getClusterForInstance(child.getName());
            boolean isStandAlone = cluster == null ? true : false;

            if (isStandAlone) { // remove config <instance>-config
                String instanceConfig = child.getConfigRef();
                final Config config = configs.getConfigByName(instanceConfig);

                // bnevins June 2010
                // don't delete the config is someone else holds a reference to it!
                if (config != null && domain.getReferenceContainersOf(config).size() > 1) {
                    return;
                }
                try {
                    if (config != null) {
                        File configConfigDir = new File(env.getConfigDirPath(), config.getName());
                        FileUtils.whack(configConfigDir);
                    }
                } catch (Exception e) {
                    // no big deal - just ignore
                }
                try {
                    if (t != null) {
                        Configs c = t.enroll(configs);
                        List<Config> configList = c.getConfig();
                        configList.remove(config);
                    }
                } catch (TransactionFailure ex) {
                    logger.log(Level.SEVERE,
                            localStrings.getLocalString("deleteConfigFailed",
                            "Unable to remove config {0}", instanceConfig), ex);
                    String msg = ex.getMessage() != null ? ex.getMessage()
                            : localStrings.getLocalString("deleteConfigFailed",
                            "Unable to remove config {0}", instanceConfig);
                    report.setMessage(msg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setFailureCause(ex);
                    throw ex;
                }
            } else { // remove server-ref from cluster
                final String instanceName = child.getName();
                if (t != null) {
                    try {
                        Cluster c = t.enroll(cluster);

                        List<ServerRef> serverRefList = c.getServerRef();
                        ServerRef serverRef = null;

                        for (ServerRef sr : serverRefList) {
                            if (sr.getRef().equals(instanceName)) {
                                serverRef = sr;
                                break;
                            }
                        }
                        if (serverRef != null) {
                            serverRefList.remove(serverRef);
                        }
                    } catch (TransactionFailure ex) {
                        logger.log(Level.SEVERE,
                                localStrings.getLocalString("deleteServerRefFailed",
                                "Unable to remove server-ref {0} from cluster {1}", instanceName, cluster.getName()), ex);
                        String msg = ex.getMessage() != null ? ex.getMessage()
                                : localStrings.getLocalString("deleteServerRefFailed",
                                "Unable to remove server-ref {0} from cluster {1}", instanceName, cluster.getName());
                        report.setMessage(msg);
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setFailureCause(ex);
                        throw ex;
                    }
                }
            }
        }
    }
}
