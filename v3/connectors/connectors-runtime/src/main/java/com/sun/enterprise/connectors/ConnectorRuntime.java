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

package com.sun.enterprise.connectors;

import com.sun.appserv.connectors.spi.ConnectorConstants;
import com.sun.appserv.connectors.spi.ConnectorRuntimeException;
import com.sun.enterprise.config.serverbeans.Property;
import com.sun.enterprise.config.serverbeans.SecurityMap;
import com.sun.enterprise.connectors.service.*;
import com.sun.enterprise.connectors.util.RAWriterAdapter;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.resource.pool.PoolManager;
import com.sun.enterprise.util.ConnectorClassLoader;
import com.sun.logging.LogDomains;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.Singleton;

import javax.naming.ConfigurationException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.logging.Logger;


@Service
@Scoped(Singleton.class)
public class ConnectorRuntime implements ConnectorConstants, com.sun.appserv.connectors.spi.ConnectorRuntime, PostConstruct {

    /* TODO V3 environment set to server as of today
    private volatile int environment = CLIENT;*/
    private volatile int environment = SERVER;
    private static ConnectorRuntime _runtime;


    private ConnectorConnectionPoolAdminServiceImpl ccPoolAdmService;
    private ConnectorResourceAdminServiceImpl connectorResourceAdmService;
    private ConnectorService connectorService;
    private ResourceAdapterAdminServiceImpl resourceAdapterAdmService;

    private long startTime;

    @Inject
    private GlassfishNamingManager namingManager;

    @Inject
    private PoolManager poolManager;

    private final Object getTimerLock = new Object();
    private Timer timer;

    /**
     * Returns the ConnectorRuntime instance.
     * It follows singleton pattern and only one instance exists at any point
     * of time. External entities need to call this method to get
     * ConnectorRuntime instance
     *
     * @return ConnectorRuntime instance
     */
    public static ConnectorRuntime getRuntime() {
        if (_runtime == null) {
            throw new RuntimeException("Connector Runtime not initialized");
        }
        return _runtime;
    }

    /**
     * Private constructor. It is private as it follows singleton pattern.
     */
    //TODO V3 made public constructor as of now.
    public ConnectorRuntime() {
        startTime = System.currentTimeMillis();
        _runtime = this;
    }


    /**
     * Initializes the execution environment. If the execution environment
     * is appserv runtime it is set to ConnectorConstants.SERVER else
     * it is set ConnectorConstants.CLIENT
     *
     * @param environment set to ConnectorConstants.SERVER if execution
     *                    environment is appserv runtime else set to
     *                    ConnectorConstants.CLIENT
     */
    public void initialize(int environment) {
        this.environment = environment;
        //TODO V3
        connectorService.initialize(getEnviron());
    }

    /**
     * Returns the execution environment.
     *
     * @return ConnectorConstants.SERVER if execution environment is
     *         appserv runtime
     *         else it returns ConnectorConstants.CLIENT
     */
    public int getEnviron() {
        return environment;
    }

    /**
     * Returns the generated default connection poolName for a
     * connection definition.
     *
     * @return generated connection poolname
     * @moduleName rar module name
     * @connectionDefName connection definition name
     */

    public String getDefaultPoolName(String moduleName,
                                     String connectionDefName) {
        return connectorService.getDefaultPoolName(moduleName, connectionDefName);
    }

    /**
     * Deletes connector Connection pool
     *
     * @param poolName Name of the pool to delete
     * @throws ConnectorRuntimeException if pool deletion operation fails
     */

    public void deleteConnectorConnectionPool(String poolName)
            throws ConnectorRuntimeException {
        ccPoolAdmService.deleteConnectorConnectionPool(poolName);
    }

    /**
     * Creates connector connection pool in the connector container.
     *
     * @param connectorPoolObj ConnectorConnectionPool instance to be bound to JNDI. This
     *                         object contains the pool properties.
     * @throws ConnectorRuntimeException When creation of pool fails.
     */

    public void createConnectorConnectionPool(
            ConnectorConnectionPool connectorPoolObj)
            throws ConnectorRuntimeException {
        ccPoolAdmService.createConnectorConnectionPool(connectorPoolObj);
    }

    /**
     * Creates the connector resource on a given connection pool
     *
     * @param jndiName     JNDI name of the resource to be created
     * @param poolName     to which the connector resource belongs.
     * @param resourceType Unused.
     * @throws ConnectorRuntimeException If the resouce creation fails.
     */

    public void createConnectorResource(String jndiName, String poolName,
                                        String resourceType) throws ConnectorRuntimeException {

        connectorResourceAdmService.createConnectorResource(jndiName, poolName, resourceType);
    }

    /**
     * Returns the generated default connector resource for a
     * connection definition.
     *
     * @return generated default connector resource name
     * @moduleName rar module name
     * @connectionDefName connection definition name
     */

    public String getDefaultResourceName(String moduleName,
                                         String connectionDefName) {
        return connectorService.getDefaultResourceName(
                moduleName, connectionDefName);
    }


    public PrintWriter getResourceAdapterLogWriter() {
        Logger logger = LogDomains.getLogger(LogDomains.RSR_LOGGER);
        RAWriterAdapter writerAdapter = new RAWriterAdapter(logger);
        return new PrintWriter(writerAdapter);
    }

    /**
     * Deletes the connector resource.
     *
     * @param jndiName JNDI name of the resource to delete.
     * @throws ConnectorRuntimeException if connector resource deletion fails.
     */

    public void deleteConnectorResource(String jndiName)
            throws ConnectorRuntimeException {
        connectorResourceAdmService.deleteConnectorResource(jndiName);
    }

    /**
     * Obtains the connector Descriptor pertaining to rar.
     * If ConnectorDescriptor is present in registry, it is obtained from
     * registry and returned. Else it is explicitly read from directory
     * where rar is exploded.
     *
     * @param rarName Name of the rar
     * @return ConnectorDescriptor pertaining to rar.
     */
    public ConnectorDescriptor getConnectorDescriptor(String rarName)
            throws ConnectorRuntimeException {
        return connectorService.getConnectorDescriptor(rarName);

    }

    /**
     * Creates Active resource Adapter which abstracts the rar module.
     * During the creation of ActiveResourceAdapter, default pools and
     * resources also are created.
     *
     * @param moduleDir  Directory where rar module is exploded.
     * @param moduleName Name of the module
     * @throws ConnectorRuntimeException if creation fails.
     */

    public void createActiveResourceAdapter(String moduleDir,
                                            String moduleName) throws ConnectorRuntimeException {
        resourceAdapterAdmService.createActiveResourceAdapter(
                moduleDir, moduleName);
    }

    /**
     * Destroys/deletes the Active resource adapter object from the
     * connector container. Active resource adapter abstracts the rar
     * deployed. It checks whether any resources (pools and connector
     * resources) are still present. If they are present and cascade option
     * is false the deletion fails and all the objects and datastructures
     * pertaining to  the resource adapter are left untouched.
     * If cascade option is true, even if resources are still present, they are
     * also destroyed with the active resource adapter
     *
     * @param moduleName Name of the rarModule to destroy/delete
     * @param cascade    If true all the resources belonging to the rar are
     *                   destroyed recursively.
     *                   If false, and if resources pertaining to resource adapter
     *                   /rar are present deletetion is failed. Then cascade
     *                   should be set to true or all the resources have to
     *                   deleted explicitly before destroying the rar/Active
     *                   resource adapter.
     * @throws ConnectorRuntimeException if the deletion fails
     */
    public void destroyActiveResourceAdapter(String moduleName, boolean cascade)
            throws ConnectorRuntimeException {
        resourceAdapterAdmService.stopActiveResourceAdapter(moduleName, cascade);
    }

    /**
     * Returns the MCF instance. If the MCF is already created and
     * present in connectorRegistry that instance is returned. Otherwise it
     * is created explicitly and added to ConnectorRegistry.
     *
     * @param poolName Name of the pool.MCF pertaining to this pool is
     *                 created/returned.
     * @return created/already present MCF instance
     * @throws ConnectorRuntimeException if creation/retrieval of MCF fails
     */
    public ManagedConnectionFactory obtainManagedConnectionFactory(
            String poolName) throws ConnectorRuntimeException {
        return ccPoolAdmService.obtainManagedConnectionFactory(poolName);
    }

    public ConnectionManager obtainConnectionManager(String poolName,
                                                     boolean forceNoLazyAssoc)
            throws ConnectorRuntimeException {
        ConnectionManager mgr = ConnectionManagerFactory.
                getAvailableConnectionManager(poolName, forceNoLazyAssoc);
        return mgr;
    }


    /**
     * Code that checks whether a jndi suffix is valid or not.
     */
    public boolean isValidJndiSuffix(String name) {
        return connectorResourceAdmService.isValidJndiSuffix(name);
    }

    /**
     * Causes pool to switch on the matching of connections.
     * It can be either directly on the pool or on the ConnectorConnectionPool
     * object that is bound in JNDI.
     *
     * @param rarName  Name of Resource Adpater.
     * @param poolName Name of the pool.
     */
    public void switchOnMatching(String rarName, String poolName) {
        connectorService.switchOnMatching(rarName, poolName);
    }

    /**
     * Causes matching to be switched on the ConnectorConnectionPool
     * bound in JNDI
     *
     * @param poolName Name of the pool
     */
    public void switchOnMatchingInJndi(String poolName)
            throws ConnectorRuntimeException {
        ccPoolAdmService.switchOnMatching(poolName);
    }

    public Object createConnectionFactory(String jndiName, String moduleName, String poolName, Hashtable env)
            throws RuntimeException {
        Object cf = null;
        try {
            ManagedConnectionFactory mcf = getRuntime().obtainManagedConnectionFactory(poolName);
            if (mcf == null) {
                /* TODO V3 temp
            _logger.log(Level.FINE,"Failed to create MCF ",poolName);*/
                throw new ConnectorRuntimeException("Failed to create MCF");
            }


            boolean forceNoLazyAssoc = false;
            /* TODO V3 handle lazy-assoc later
            if ( jndiName.endsWith( com.sun.appserv.connectors.spi.ConnectorConstants.PM_JNDI_SUFFIX ) ) {
                forceNoLazyAssoc = true;
            }
            */

            String derivedJndiName = deriveJndiName(jndiName, env);
            ConnectionManagerImpl mgr = (ConnectionManagerImpl)
                    getRuntime().obtainConnectionManager(poolName, forceNoLazyAssoc);
            mgr.setJndiName(derivedJndiName);
            mgr.setRarName(moduleName);
            mgr.initialize();

            cf = mcf.createConnectionFactory(mgr);
            if (cf == null) {
                /* TODO V3 handle later
                    String msg = localStrings.getLocalString
                        ("no.resource.adapter", "");
                */
                String msg = "No resource adapter found";
                throw new RuntimeException(new ConfigurationException(msg));
            }
            /* TODO V3 handle later
                if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,"Connection Factory:" + cf);
            }
            */
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cf;
    }

    private String deriveJndiName(String name, Hashtable env) {
        String suffix = (String) env.get(ConnectorConstants.JNDI_SUFFIX_PROPERTY);
        if (getRuntime().isValidJndiSuffix(suffix)) {
            /* TODO V3 handle later
                _logger.log(Level.FINE, "JNDI name will be suffixed with :" + suffix);
            */
            return name + suffix;
        }
        return name;
    }

    public GlassfishNamingManager getNamingManager() {
        return namingManager;
    }

    /**
     * The component has been injected with any dependency and
     * will be placed into commission by the subsystem.
     */
    public void postConstruct() {


        ccPoolAdmService = (ConnectorConnectionPoolAdminServiceImpl)
                ConnectorAdminServicesFactory.getService(ConnectorConstants.CCP);
        connectorResourceAdmService = (ConnectorResourceAdminServiceImpl)
                ConnectorAdminServicesFactory.getService(ConnectorConstants.CR);
        connectorService = new ConnectorService();
        resourceAdapterAdmService = (ResourceAdapterAdminServiceImpl)
                ConnectorAdminServicesFactory.getService(ConnectorConstants.RA);

        //TODO V3 class-loader (temprorarily initializing with current thread's context cl)
        ConnectorClassLoader.getInstance(Thread.currentThread().getContextClassLoader());


        System.out.println("Time taken to initialize connector runtime : " + (System.currentTimeMillis() - startTime));
    }

    /**
     * Checks if a conncetor connection pool has been deployed to this server
     * instance
     *
     * @param poolName
     * @return
     */
    public boolean isConnectorConnectionPoolDeployed(String poolName) {
        return ccPoolAdmService.isConnectorConnectionPoolDeployed(poolName);
    }

    /**
     * Reconfigure a connection pool.
     * This method compares the passed connector connection pool with the one
     * in memory. If the pools are unequal and the MCF properties are changed
     * a pool recreate is required. However if the pools are unequal and the
     * MCF properties are not changed a recreate is not required
     *
     * @param ccp           - the Updated connector connection pool object that admin
     *                      hands over
     * @param excludedProps - A set of excluded property names that we want
     *                      to be excluded in the comparison check while
     *                      comparing MCF properties
     * @return true - if a pool restart is required, false otherwise
     * @throws ConnectorRuntimeException
     */
    public boolean reconfigureConnectorConnectionPool(ConnectorConnectionPool
            ccp, Set excludedProps) throws ConnectorRuntimeException {
        return ccPoolAdmService.reconfigureConnectorConnectionPool(
                ccp, excludedProps);
    }

    /**
     * Recreate a connector connection pool. This method essentially does
     * the following things:
     * 1. Delete the said connector connection pool<br>
     * 2. Bind the pool to JNDI<br>
     * 3. Create an MCF for this pool and register with the connector registry<br>
     *
     * @param ccp - the ConnectorConnectionPool to publish
     */

    public void recreateConnectorConnectionPool(ConnectorConnectionPool ccp)
            throws ConnectorRuntimeException {
        ccPoolAdmService.recreateConnectorConnectionPool(ccp);
    }

    /**
     * Creates connector connection pool in the connector container.
     *
     * @param ccp                      ConnectorConnectionPool instance to be bound to JNDI. This
     *                                 object contains the pool properties.
     * @param connectionDefinitionName Connection definition name against which
     *                                 connection pool is being created
     * @param rarName                  Name of the resource adapter
     * @param props                    Properties of MCF which are present in domain.xml
     *                                 These properties override the ones present in ra.xml
     * @param securityMaps             Array fo security maps.
     * @throws ConnectorRuntimeException When creation of pool fails.
     */

    public void createConnectorConnectionPool(ConnectorConnectionPool ccp,
                                              String connectionDefinitionName, String rarName,
                                              List<Property> props,
                                              List<SecurityMap> securityMaps)
            throws ConnectorRuntimeException {
        ccPoolAdmService.createConnectorConnectionPool(ccp, connectionDefinitionName, rarName, props, securityMaps);
    }


    public boolean checkAndLoadResource(Object resource, Object pool, String resourceType, String resourceName,
                                        String raName) throws ConnectorRuntimeException {
        return connectorService.checkAndLoadResource(resource, pool, resourceType, resourceName, raName);
    }

    /**
     * Calls the stop method for all J2EE Connector 1.5 spec compliant RARs
     */
    public void stopAllActiveResourceAdapters() {
        resourceAdapterAdmService.stopAllActiveResourceAdapters();
    }

    public void shutdownAllActiveResourceAdapters(List<String> poolNames, List<String> resourceNames) {
        destroyResourcesAndPools(resourceNames, poolNames);
        stopAllActiveResourceAdapters();
    }

    public void destroyResourcesAndPools(List<String> resourceNames, List<String> poolNames) {
        destroyConnectorResources(resourceNames);
        destroyConnectionPools(poolNames);
    }

    private void destroyConnectionPools(List<String> poolNames) {
        for (String poolName : poolNames) {
            try {
                ccPoolAdmService.deleteConnectorConnectionPool(poolName);
            } catch (ConnectorRuntimeException cre) {
                cre.printStackTrace();
                //TODO V3 handle / log exceptions
            }
        }
    }

    private void destroyConnectorResources(List<String> resourceNames) {
        for (String resourceName : resourceNames) {
            try {
                connectorResourceAdmService.deleteConnectorResource(resourceName);
            } catch (ConnectorRuntimeException cre) {
                cre.printStackTrace();
                //TODO V3 handle / log exceptions
            }
        }
    }

    public PoolManager getPoolManager() {
        return poolManager;
    }

    public Timer getTimer() {
        synchronized (getTimerLock) {
            if (timer == null) {
                // Create a scheduler as a daemon so it
                // won't prevent process from exiting.
                timer = new Timer(true);
            }
        }
        return timer;
    }

/* TODO V3 handle resourc-ref later
    public Set getResourceReferenceDescriptor(){
        return containerUtil.getComponentEnvManager().getCurrentJndiNameEnvironment().getResourceReferenceDescriptors();
    }
*/
}
