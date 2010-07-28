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

package com.sun.enterprise.connectors.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.TransactionSupport;
import javax.security.auth.Subject;

import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.enterprise.config.serverbeans.JdbcResource;
import com.sun.enterprise.config.serverbeans.ResourcePool;
import com.sun.enterprise.config.serverbeans.SecurityMap;
import com.sun.enterprise.connectors.ActiveResourceAdapter;
import com.sun.enterprise.connectors.*;
import com.sun.enterprise.connectors.authentication.ConnectorSecurityMap;
import com.sun.enterprise.connectors.authentication.RuntimeSecurityMap;
import com.sun.enterprise.connectors.util.*;
import com.sun.enterprise.deployment.ConnectionDefDescriptor;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.ConnectorConfigProperty ;
import com.sun.enterprise.deployment.ResourcePrincipal;
import com.sun.enterprise.resource.listener.UnpooledConnectionEventListener;
import com.sun.enterprise.resource.pool.PoolManager;
import org.glassfish.internal.api.RelativePathResolver;
import com.sun.enterprise.util.i18n.StringManager;
import javax.resource.spi.ConnectionRequestInfo;
import org.jvnet.hk2.config.types.Property;


/**
 * Connector connection pool admin service performs the
 * functionality of creation, deletion, recreation, testing of the pools.
 *
 * @author Srikanth P and Aditya Gore
 */

public class ConnectorConnectionPoolAdminServiceImpl extends ConnectorService {

    private static StringManager localStrings =
            StringManager.getManager(ConnectorConnectionPoolAdminServiceImpl.class);

    /**
     * Default constructor
     */
    public ConnectorConnectionPoolAdminServiceImpl() {
        super();

    }

    /**
     * Creates connector connection pool in the connector container.
     *
     * @param ccp     ConnectorConnectionPool instance to be bound to JNDI. This
     *                object contains the pool properties.
     * @param cdd     ConnectorDescriptor obejct which abstracts the ra.xml
     * @param rarName Name of the resource adapter
     * @throws ConnectorRuntimeException When creation of pool fails.
     */
    public void createConnectorConnectionPool(ConnectorConnectionPool ccp,
                                              ConnectionDefDescriptor cdd, String rarName)
            throws ConnectorRuntimeException {

        if ((ccp == null) || (cdd == null) || (rarName == null)) {

            _logger.log(Level.FINE, "Wrong parameters for pool creation ");
            String i18nMsg = localStrings.getString("ccp_adm.wrong_params_for_create");
            throw new ConnectorRuntimeException(i18nMsg);
        }
        ConnectorDescriptorInfo cdi = new ConnectorDescriptorInfo();

        ConnectorDescriptor connectorDescriptor = _registry.getDescriptor(rarName);

        if (connectorDescriptor == null) {
            String i18nMsg = localStrings.getString("ccp_adm.no_conn_pool_obj", rarName);
            ConnectorRuntimeException cre = new ConnectorRuntimeException(i18nMsg);
            _logger.log(Level.SEVERE, "rardeployment.connector_descriptor_notfound_registry", rarName);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        }
        cdi.setRarName(rarName);
        cdi.setResourceAdapterClassName(
                connectorDescriptor.getResourceAdapterClass());
        cdi.setConnectionDefinitionName(cdd.getConnectionFactoryIntf());
        cdi.setManagedConnectionFactoryClass(
                cdd.getManagedConnectionFactoryImpl());
        cdi.setConnectionFactoryClass(cdd.getConnectionFactoryImpl());
        cdi.setConnectionFactoryInterface(cdd.getConnectionFactoryIntf());
        cdi.setConnectionClass(cdd.getConnectionImpl());
        cdi.setConnectionInterface(cdd.getConnectionIntf());
        cdi.setMCFConfigProperties(cdd.getConfigProperties());
        cdi.setResourceAdapterConfigProperties(
                connectorDescriptor.getConfigProperties());
        createConnectorConnectionPool(ccp, cdi);
    }

    /**
     * Creates connector connection pool in the connector container.
     *
     * @param connectorPoolObj  ConnectorConnectionPool instance to be bound to JNDI. This
     *                          object contains the pool properties.
     * @param connectorDescInfo ConnectorDescriptorInfo object which
     *                          abstracts the connection definition values
     *                          present in ra.xml
     * @throws ConnectorRuntimeException When creation of pool fails.
     */

    private void createConnectorConnectionPool(
            ConnectorConnectionPool connectorPoolObj,
            ConnectorDescriptorInfo connectorDescInfo)
            throws ConnectorRuntimeException {

        connectorPoolObj.setConnectorDescriptorInfo(connectorDescInfo);
        createConnectorConnectionPool(connectorPoolObj);
    }

    /**
     * Creates connector connection pool in the connector container.
     *
     * @param connectorPoolObj ConnectorConnectionPool instance to be bound to JNDI. This
     *                         object contains the pool properties.
     * @throws ConnectorRuntimeException When creation of pool fails.
     */
    public void createConnectorConnectionPool(ConnectorConnectionPool connectorPoolObj)
            throws ConnectorRuntimeException {

        String poolName = connectorPoolObj.getName();
        if (connectorPoolObj == null || poolName == null) {
            _logger.log(Level.FINE, "Wrong parameters for pool creation ");
            String i18nMsg = localStrings.getString("ccp_adm.wrong_params_for_create");
            throw new ConnectorRuntimeException(i18nMsg);
        }
        String jndiNameForPool = ConnectorAdminServiceUtils.getReservePrefixedJNDINameForPool(poolName);
        try {
            // TODO why rebind is 'true' as pool should have been undeployed for reconfiguration ?
            _runtime.getNamingManager().publishObject(jndiNameForPool, connectorPoolObj, true);
            ManagedConnectionFactory mcf = obtainManagedConnectionFactory(poolName);
            if (mcf == null) {
                Context ic = _runtime.getNamingManager().getInitialContext();
                ic.unbind(jndiNameForPool);
                String i18nMsg = localStrings.getString("ccp_adm.failed_to_create_mcf", poolName);
                ConnectorRuntimeException cre = new ConnectorRuntimeException(i18nMsg);
                _logger.log(Level.SEVERE, "rardeployment.mcf_creation_failure", poolName);
                _logger.log(Level.SEVERE, "", cre);
                throw cre;
            }

        } catch (NamingException ex) {

            String i18nMsg = localStrings.getString("ccp_adm.failed_to_publish_in_jndi", poolName);
            ConnectorRuntimeException cre = new ConnectorRuntimeException(i18nMsg);
            cre.initCause(ex);
            _logger.log(Level.SEVERE, "rardeployment.pool_jndi_bind_failure", poolName);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        } catch (NullPointerException ex) {
            try {
                Context ic = _runtime.getNamingManager().getInitialContext();
                ic.unbind(jndiNameForPool);
            } catch (NamingException ne) {
                _logger.log(Level.FINE, "Failed to unbind connection pool object  ", poolName);
            }

            String i18nMsg = localStrings.getString("ccp_adm.failed_to_register_mcf", poolName);
            ConnectorRuntimeException cre = new ConnectorRuntimeException(i18nMsg);
            cre.initCause(ex);
            _logger.log(Level.SEVERE, "rardeployment.mcf_registration_failure", poolName);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        }
    }

    /**
     * Creates connector connection pool in the connector container.
     * cannot be used for 1.5 rar cases
     *
     * @param connectorPoolObj ConnectorConnectionPool instance to be bound to JNDI. This
     *                         object contains the pool properties.
     * @param security         unused
     * @param configProperties MCF config properties
     * @throws ConnectorRuntimeException When creation of pool fails.
     */
    public void createConnectorConnectionPool(
            ConnectorConnectionPool connectorPoolObj, String security,
            Set configProperties) throws ConnectorRuntimeException {
        if (connectorPoolObj == null || configProperties == null) {
            _logger.log(Level.FINE, "Wrong parameters for pool creation ");
            String i18nMsg = localStrings.getString("ccp_adm.wrong_params_for_create");
            throw new ConnectorRuntimeException(i18nMsg);
        }
        String poolName = connectorPoolObj.getName();
        String moduleName =
                connectorPoolObj.getConnectorDescriptorInfo().getRarName();
        String connectionDefinitionName =
                connectorPoolObj.getConnectorDescriptorInfo().
                        getConnectionDefinitionName();

        ConnectorDescriptor connectorDescriptor =
                _registry.getDescriptor(moduleName);

        if (connectorDescriptor == null) {
            String i18nMsg = localStrings.getString("ccp_adm.null_connector_desc", moduleName);
            ConnectorRuntimeException cre = new
                    ConnectorRuntimeException(i18nMsg);
            _logger.log(Level.SEVERE, "rardeployment.null_mcf_in_registry", moduleName);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        }

        Set connectionDefs =
                connectorDescriptor.getOutboundResourceAdapter().getConnectionDefs();

        Iterator iterator = connectionDefs.iterator();

        ConnectionDefDescriptor connectionDefDescriptor = null;

        while (iterator.hasNext()) {
            connectionDefDescriptor =
                    (ConnectionDefDescriptor) iterator.next();
            if (connectionDefinitionName.equals(
                    connectionDefDescriptor.getConnectionFactoryIntf()))
                break;
        }

        ConnectorDescriptorInfo connectorDescInfo =
                ConnectorDDTransformUtils.getConnectorDescriptorInfo(
                        connectionDefDescriptor);
        connectorDescInfo.setMCFConfigProperties(configProperties);
        connectorDescInfo.setRarName(moduleName);
        connectorDescInfo.setResourceAdapterClassName(
                connectorDescriptor.getResourceAdapterClass());

        createConnectorConnectionPool(connectorPoolObj, connectorDescInfo);
    }


    /**
     * Deletes connector Connection pool
     *
     * @param poolName Name of the pool to delete
     * @throws ConnectorRuntimeException if pool deletion operation fails
     */

    public void deleteConnectorConnectionPool(String poolName)
            throws ConnectorRuntimeException {
        deleteConnectorConnectionPool(poolName, false);
    }

    /**
     * Deletes connector Connection pool. Here check in made whether resources
     * pertaining to pool are present in domain.xml.
     *
     * @param poolName Name of the pool to delete
     * @param cascade  If true all the resources associed with that are also
     *                 deleted from connector container
     *                 If false and if some resources pertaining to pool
     *                 are present deletion operation fails . If no resources
     *                 are present pool is deleted.
     * @throws ConnectorRuntimeException if pool deletion operation fails
     */

    public void deleteConnectorConnectionPool(String poolName, boolean cascade)
            throws ConnectorRuntimeException {

        if (poolName == null) {
            _logger.log(Level.WARNING, "ccp_adm.null_pool_name");
            String i18nMsg = localStrings.getString("ccp_adm.null_pool_name");
            throw new ConnectorRuntimeException(i18nMsg);
        }

        boolean errorOccured = false;

        /*  Not needed as --cascade is handled via v3's application life cycle contracts

                ResourcesUtil resUtil = ResourcesUtil.createInstance();
                Object[] connectorResourcesJndiNames =
                                resUtil.getConnectorResourcesJndiNames(poolName);
                if(cascade==true && connectorResourcesJndiNames != null) {
                   for(int i=0;i<connectorResourcesJndiNames.length;++i) {
                       try {
                           getRuntime().deleteConnectorResource(
                                      (String)connectorResourcesJndiNames[i]);
                       } catch(ConnectorRuntimeException cre) {
                         errorOccured=true;
                       }
                   }

                } else if(connectorResourcesJndiNames != null &&
                              connectorResourcesJndiNames.length != 0) {

                }
        */
        killPool(poolName);
        boolean result = _registry.removeManagedConnectionFactory(poolName);

        if (!result) {
            _logger.log(Level.FINE, "rardeployment.mcf_removal_failure", poolName);
            return;
        }

        try {
            //String originalPoolName = ConnectorAdminServiceUtils.getOriginalResourceName(poolName);
            String jndiNameForPool = ConnectorAdminServiceUtils.getReservePrefixedJNDINameForPool(poolName);
            Context ic = _runtime.getNamingManager().getInitialContext();
            ic.unbind(jndiNameForPool);
        } catch (NamingException ne) {
            _logger.log(Level.SEVERE, "rardeployment.connectionpool_removal_from_jndi_error", poolName);
            String i18nMsg = localStrings.getString("ccp_adm.failed_to_remove_from_jndi", poolName);
            ConnectorRuntimeException cre = new ConnectorRuntimeException(i18nMsg);
            cre.initCause(ne);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        }
        if (errorOccured){
            String i18nMsg = localStrings.getString("ccp_adm.failed_to_delete_conn_res", poolName);
            ConnectorRuntimeException cre = new
                    ConnectorRuntimeException(i18nMsg);
            _logger.log(Level.SEVERE, "rardeployment.all_resources_removal_error", poolName);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        }
    }

    /**
     * Kills a specific pool
     *
     * @param poolName poolName to kill
     */
    public void killPool(String poolName) {
        _runtime.getPoolManager().killPool(poolName);
    }

    /**
     * Gets the properties of the Java bean connection definition class that
     * have setter methods defined and the default values as provided by the
     * Connection Definition java bean developer.
     *
     * @param connectionDefinitionClassName The Connection Definition Java bean class for which
     *                                      overrideable properties are required.
     * @return Map<String, Object> String represents property name
     *         and Object is the defaultValue that is a primitive type or String
     */
    public static Map<String, Object> getConnectionDefinitionPropertiesAndDefaults(String connectionDefinitionClassName, String resType) {
        return ConnectionDefinitionUtils
                .getConnectionDefinitionPropertiesAndDefaults(
                        connectionDefinitionClassName, resType);
    }

    /**
     * asadmin test-connection-pool
     * This method is used to provide backend functionality for the
     * test-connection-pool asadmin command. Briefly the design is as
     * follows:<br>
     * 1. obtainManagedConnection for the poolname<br>
     * 2. lookup ConnectorDescriptorInfo from InitialContext using poolname<br>
     * 3. from cdi get username and password<br>
     * 4. create ResourcePrincipal using default username and password<br>
     * 5. create a Subject from this (doPriveleged)<br>
     * 6. createManagedConnection using above subject<br>
     * 7. getConnection from the ManagedConnection with above subject<br>
     *
     * @return true if the connection pool is healthy. false otherwise
     * @throws ResourceException
     *          if pool is not usable
     */
    public boolean testConnectionPool(String poolName)
            throws ResourceException {
        //dump(poolName); //TODO V3 no way to call dump ?
        Object con = null;
        try {

            //Create the ManagedConnection
            con = getUnpooledConnection(poolName, null, false);

        } catch (Exception re) {
            //Since we have an exception, the pool is not good
            Object params[] = new Object[]{poolName, re.getMessage()};
            _logger.log(Level.WARNING, "test.connection.pool.failed", params);
            ResourceException e = new ResourceException("Ping failed Exception - " +
                    re.getMessage() +
                    " Please check the server.log for more details.");
            e.initCause(re);
            throw e;
        } finally {
            try {
                //destroy the MC
                ((ManagedConnection) con).destroy();

            } catch (Throwable e) {
                //ignore
            }
        }

        //We did not get a ResourceException, so pool must be OK
        return true;
    }

    /**
     * Utility method that is used to get the default subject for the 
     * specified mcf and resource principal.
     * @param poolName
     * @param mcf
     * @param prin
     * @return
     * @throws javax.resource.ResourceException
     */
    protected Subject getDefaultSubject(String poolName, ManagedConnectionFactory mcf, 
            ResourcePrincipal prin) throws ResourceException {
        ResourcePrincipal resourcePrincipal = null;
        if (prin == null) {
            try {
                resourcePrincipal = getDefaultResourcePrincipal(poolName, mcf);
            } catch (NamingException ne) {
                _logger.log(Level.WARNING, "jdbc.pool_not_reachable",
                        ne.getMessage());
                String l10nMsg = localStrings.getString(
                        "pingpool.name_not_bound", poolName);
                ResourceException e = new ResourceException(l10nMsg + poolName);
                e.initCause(ne);
                throw e;
            }
        } else {
            resourcePrincipal = prin;
        }

        final Subject defaultSubject =
                ConnectionPoolObjectsUtils.createSubject(mcf, resourcePrincipal);


        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("using subject: " + defaultSubject);

        }        
        return defaultSubject;
    }

    /**
     * Utility method to get Managed connection from the supplied mcf and 
     * default subject.
     * @param mcf
     * @param defaultSubject
     * @return
     * @throws javax.resource.ResourceException
     */
    protected ManagedConnection getManagedConnection(ManagedConnectionFactory mcf, 
            Subject defaultSubject, ConnectionRequestInfo cReqInfo) throws ResourceException {
        
        ManagedConnection mc = null;
        
        //Create the ManagedConnection
        mc = mcf.createManagedConnection(defaultSubject, cReqInfo);
        return mc;

    }    

/*
    */
/**
     * Utility method to get a managed connection factory for the jdbc connection
     * pool name.
     * @param poolName
     * @return
     * @throws javax.resource.ResourceException
     *//*

    protected ManagedConnectionFactory getManagedConnectionFactory(String poolName) 
            throws ResourceException {
        ManagedConnectionFactory mcf = null;
        try {
            mcf = obtainManagedConnectionFactory(poolName);

        } catch (ConnectorRuntimeException cre) {
            logFine("getUnpooledConnection :: obtainManagedConnectionFactory " +
                    "threw exception. Doing checkAndLoadPoolResource");
            if (checkAndLoadPool(poolName)) {
                logFine("getUnpooledConnection:: checkAndLoadPoolResource is true");
                try {
                    */
/* TODO not needed for non-cluster
                 //deploy the pool resource if not already done
                 //The pool resource would get loaded in case we are in DAS
                 //due to the checkAndLoadPoolResource call
                 //but in EE, if the pool we are trying to access is in a
                 //remote instance, the pool will not have been created
                 if ( ! isConnectorConnectionPoolDeployed( poolName ) ) {
                             logFine("getUnpooledConnection :: " +
                     "isConnectorConnectionPoolDeployed is false");
                     try {
                         jdbcPoolToDeploy = getJdbcConnectionPoolServerBean( poolName );
                     if ( jdbcPoolToDeploy != null ) {
                             (new JdbcConnectionPoolDeployer()).deployResource(
                             jdbcPoolToDeploy );
                                     logFine("getUnpooledConnection :: force deployed the " +
                             "JdbcConnectionPool : " + poolName);
                     } else {
                         ccPoolToDeploy = getConnectorConnectionPoolServerBean(
                         poolName );
                         (new ConnectorConnectionPoolDeployer()).deployResource(
                         ccPoolToDeploy);
                                     logFine("getUnpooledConnection :: force deployed the " +
                             "ConnectorConnectionPool :" + poolName);
                     }
                     needToUndeployPool = true;
                     } catch(Exception e ) {
                         _logger.log( Level.SEVERE,
                             "jdbc.could_not_do_actual_deploy for : ", poolName );
                         throw new ResourceException( e );
                     }
                 }*//*

                    logFine("getUnpooledConnection :: " +
                            "Now calling obtainManagedConnectionFactory again");
                    mcf = obtainManagedConnectionFactory(poolName);
                    logFine("getUnpooledConnection:: " +
                            "done obtainManagedConnectionFactory again");
                } catch (ConnectorRuntimeException creAgain) {
                    String l10nMsg = localStrings.getString(
                            "pingpool.cannot_obtain_mcf", poolName);
                    _logger.log(Level.WARNING, "jdbc.pool_not_reachable",
                            l10nMsg);
                    ResourceException e = new ResourceException(l10nMsg);
                    e.initCause(creAgain);
                    throw e;
                }
            } else {
                _logger.log(Level.WARNING, "jdbc.pool_not_reachable",
                        cre.getMessage());
                String l10nMsg = localStrings.getString(
                        "pingpool.cannot_obtain_mcf", poolName);
                ResourceException e = new ResourceException(l10nMsg);
                e.initCause(cre);
                throw e;

            }
        }
        return mcf;
    }
*/

    /**
     * This method is used to provide backend functionality for the
     * ping-connection-pool asadmin command. Briefly the design is as
     * follows:<br>
     * 1. obtainManagedConnectionFactory for the poolname<br>
     * 2. lookup ConnectorDescriptorInfo from InitialContext using poolname<br>
     * 3. from cdi get username and password<br>
     * 4. create ResourcePrincipal using default username and password<br>
     * 5. create a Subject from this (doPriveleged)<br>
     * 6. createManagedConnection using above subject<br>
     * 7. add a dummy ConnectionEventListener to the mc that simply handles connectionClosed
     * 8. getConnection from the ManagedConnection with above subject<br>
     *
     * @param poolName               The poolname from whose MCF to obtain the unpooled mc
     * @param principal                   The ResourcePrincipal to use for authenticating the request if not null.
     *                               If null, the pool's default authentication mechanism is used
     * @param returnConnectionHandle If true will return the logical connection handle
     *                               derived from the Managed Connection, else will only return mc
     * @return an unPooled connection
     * @throws ResourceException for various error conditions
     */
    public Object getUnpooledConnection(String poolName, ResourcePrincipal principal, boolean returnConnectionHandle)
            throws ResourceException {
        ManagedConnectionFactory mcf = null;
        ResourcePool poolToDeploy = null;
        boolean needToUndeployPool = false;
        ConnectorRuntime runtime = ConnectorRuntime.getRuntime();

        try {
            //START CR 6597868
            if (!isPoolReferredByDatabaseResource(poolName)) {
                if (_registry.isMCFCreated(poolName)){
                    unloadAndKillPool(poolName);
                }
            }
            //END CR 6597868
            mcf = obtainManagedConnectionFactory(poolName, new Hashtable());
        } catch (ConnectorRuntimeException re) {
            logFine("getUnpooledConnection :: obtainManagedConnectionFactory " +
                    "threw exception. So doing checkAndLoadPoolResource");
            if (checkAndLoadPool(poolName)) {
                logFine("getUnpooledConnection:: checkAndLoadPoolResource is true");
                try {

                    //deploy the pool resource if not already done
                    //The pool resource would get loaded in case we are in DAS
                    //due to the checkAndLoadPoolResource call
                    //but in EE, if the pool we are trying to access is in a
                    //remote instance, the pool will not have been created
                    if (!isConnectorConnectionPoolDeployed(poolName)) {
                        logFine("getUnpooledConnection :: isConnectorConnectionPoolDeployed is false");
                        try {
                            poolToDeploy = (ResourcePool)
                                    runtime.getResources().getResourceByName(ResourcePool.class, poolName);
                            runtime.getResourceDeployer(poolToDeploy).deployResource(poolToDeploy);
                            logFine("getUnpooledConnection :: force deployed the ConnectionPool : " + poolName);
                            needToUndeployPool = true;
                        } catch (Exception e) {
                            _logger.log(Level.SEVERE, "jdbc.could_not_do_actual_deploy for : ", poolName);
                            throw new ResourceException(e);
                        }
                    }
                    logFine("getUnpooledConnection :: Now calling obtainManagedConnectionFactory again");
                    mcf = obtainManagedConnectionFactory(poolName);
                    logFine("getUnpooledConnection:: done obtainManagedConnectionFactory again");

                } catch (ConnectorRuntimeException creAgain) {
                    String l10nMsg = localStrings.getString(
                            "pingpool.cannot_obtain_mcf", poolName);
                    _logger.log(Level.WARNING, "jdbc.pool_not_reachable", l10nMsg);
                    ResourceException e = new ResourceException(l10nMsg);
                    e.initCause(creAgain);
                    throw e;
                }
            } else {
                _logger.log(Level.WARNING, "jdbc.pool_not_reachable", re.getMessage());
                String l10nMsg = localStrings.getString("pingpool.cannot_obtain_mcf", poolName);
                ResourceException e = new ResourceException(l10nMsg);
                e.initCause(re);
                throw e;
            }
        }

        ResourcePrincipal resourcePrincipal = null;
        if (principal == null) {
            try {
                resourcePrincipal = getDefaultResourcePrincipal(poolName, mcf);
            } catch (NamingException ne) {
                _logger.log(Level.WARNING, "jdbc.pool_not_reachable", ne.getMessage());
                String l10nMsg = localStrings.getString("pingpool.name_not_bound", poolName);
                ResourceException e = new ResourceException(l10nMsg + poolName);
                e.initCause(ne);
                throw e;
            }
        } else {
            resourcePrincipal = principal;
        }

        final Subject defaultSubject = ConnectionPoolObjectsUtils.createSubject(mcf, resourcePrincipal);

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("using subject: " + defaultSubject);
        }

        //Create the ManagedConnection
        ManagedConnection mc = mcf.createManagedConnection(defaultSubject, null);
        //We are done with the pool for now, so undeploy if we deployed
        //it here
        if (needToUndeployPool) {
            if (poolToDeploy != null) {
                logFine("getUnpooledConnection :: need to force undeploy pool");
                try {
                    runtime.getResourceDeployer(poolToDeploy).undeployResource(poolToDeploy);
                } catch (Exception e) {
                    _logger.fine("getUnpooledConnection: error undeploying pool");
                }
                logFine("getUnpooledConnection :: done.. force undeploy of pool");
            }
        }
        //Add our dummy ConnectionEventListener impl.
        //This impl only knows how to handle connectionClosed events
        mc.addConnectionEventListener(new UnpooledConnectionEventListener());
        return returnConnectionHandle ? mc.getConnection(defaultSubject, null) : mc;
    }

    //START CR 6597868
    /** This method is used to find out if pool is linked to data-source or not
      * if pool is not linked then ping will not take changes to pool
      * dynamically.
      * @param poolName pool-name
      * @return boolean indicating whether the pool is referred or not
      */
    private boolean isPoolReferredByDatabaseResource(String poolName){
        ResourcesUtil resUtil = ResourcesUtil.createInstance();
        return resUtil.isJdbcPoolReferredInServerInstance(poolName);
    }
    //END CR 6597868


    /**
     * Utility method to get property value from ConnectorDescriptorInfo.
     * @param prop
     * @return
     */
    protected String getPropertyValue(String prop, 
            ConnectorConnectionPool connectorConnectionPool) {
        String result = null;
        ConnectorDescriptorInfo cdi = connectorConnectionPool.getConnectorDescriptorInfo();

        Set mcfConfigProperties = cdi.getMCFConfigProperties();
        Iterator mcfConfPropsIter = mcfConfigProperties.iterator();
        while (mcfConfPropsIter.hasNext()) {
            ConnectorConfigProperty  envProp = (ConnectorConfigProperty ) mcfConfPropsIter.next();

            if (envProp.getName().toUpperCase().equals(prop)) {
                result = envProp.getValue();
            } 
        }
        return result;
    }

    private ResourcePrincipal getDefaultResourcePrincipal( String poolName,
        ManagedConnectionFactory mcf ) throws NamingException {
        return getDefaultResourcePrincipal(poolName, mcf, null);
    }
    
    /*
    * Returns a ResourcePrincipal object populated with a pool's
    * default USERNAME and PASSWORD
    *
    * @throws NamingException if poolname lookup fails
    */
    private ResourcePrincipal getDefaultResourcePrincipal(String poolName,
                                                          ManagedConnectionFactory mcf, Hashtable env)
            throws NamingException {
        String userName = null;
        String password = null;
        // All this to get the default user name and principal
        ConnectorConnectionPool connectorConnectionPool = null;
        try {
            String jndiNameForPool = ConnectorAdminServiceUtils.getReservePrefixedJNDINameForPool(poolName);
            //Context ic = _runtime.getNamingManager().getInitialContext();
            Context ic ;
            if(env != null){
                ic = new InitialContext(env);
            }else{
                ic = new InitialContext();
            }
            connectorConnectionPool = (ConnectorConnectionPool) ic.lookup(jndiNameForPool);
        } catch (NamingException ne) {
            throw ne;
        }

        userName = getPropertyValue("USERNAME", connectorConnectionPool);
        if(userName == null) {
            userName = getPropertyValue("USER", connectorConnectionPool);
        }
        password = getPropertyValue("PASSWORD", connectorConnectionPool);
        try {
            password = RelativePathResolver.getRealPasswordFromAlias(password);
        } catch(Exception e) {
            _logger.log(Level.WARNING, "unable_to_get_password_from_alias", e);
        }

        // To avoid using "", "" as the default username password, try to get
        // the username and password from MCF, to use in subject. MQ adapter
        // cannot use "","" as the username/password.

        if (userName == null || userName.trim().equals("")) {
            userName = ConnectionPoolObjectsUtils.getValueFromMCF("UserName", poolName, mcf);
            //It is possible that ResourceAdapter may have getUser() instead of
            //getUserName() property getter
            if (userName.trim().equals("")) {
                userName = ConnectionPoolObjectsUtils.getValueFromMCF("User", poolName, mcf);
            }
            password = ConnectionPoolObjectsUtils.getValueFromMCF("Password", poolName, mcf);
        }
        //Now return the ResourcePrincipal
        return new ResourcePrincipal(userName, password);
    }

    /**
     * Rebinds the connection pool with matchning flag set.
     *
     * @param poolName pool for which matching need to be switched on
     * @throws ConnectorRuntimeException , if a Naming error occurs.
     */
    public void switchOnMatching(String poolName) throws ConnectorRuntimeException {
        try {
            ConnectorConnectionPool origCcp =
                    getOriginalConnectorConnectionPool(poolName);
            origCcp.setMatchConnections(true);

            //now rebind the object in jndi
            String jndiNameForPool = ConnectorAdminServiceUtils.getReservePrefixedJNDINameForPool(poolName);


            _runtime.getNamingManager().getInitialContext().unbind(jndiNameForPool);
            _runtime.getNamingManager().publishObject(jndiNameForPool, (Object) origCcp, true);
        } catch (NamingException e) {
            ConnectorRuntimeException ex =
                    new ConnectorRuntimeException(e.getMessage());
            throw(ConnectorRuntimeException) ex.initCause(e);
        }
    }

   /**
    * Create a ConnectorConnectionPool from information in memory
    */
    private ConnectorConnectionPool getOriginalConnectorConnectionPool(
            String poolName) throws NamingException {

        ConnectorConnectionPool ccpOrig = null;

        String jndiNameForPool = ConnectorAdminServiceUtils.getReservePrefixedJNDINameForPool(poolName);
        try {
            ccpOrig = (ConnectorConnectionPool)
                    _runtime.getNamingManager().getInitialContext().lookup(jndiNameForPool);
        } catch (NamingException ne) {
                throw ne;
        }
        return ccpOrig;
    }

    private ConnectorConnectionPool getConnectorConnectionPool(String poolName)
        throws ConnectorRuntimeException, NamingException  {
        return getConnectorConnectionPool(poolName, null);
    }
    
    /**
     * Returns the connector connection pool object corresponding
     * to the pool name
     *
     * @param poolName Name of the pool.MCF pertaining to this pool is
     *                 created/returned.
     * @return Connector connection pool corresponding to this instance
     * @throws ConnectorRuntimeException if creation/retrieval
     *                                   of MCF fails
     */
    private ConnectorConnectionPool getConnectorConnectionPool(String poolName, Hashtable env)
            throws ConnectorRuntimeException, NamingException {
        String jndiNameForPool = ConnectorAdminServiceUtils.getReservePrefixedJNDINameForPool(poolName);

        //Context ic = _runtime.getNamingManager().getInitialContext();
        Context ic ;
        if(env != null){
            ic = new InitialContext(env);
        }else{
            ic = new InitialContext();
        }

        ConnectorConnectionPool connectorConnectionPool = (ConnectorConnectionPool)ic.lookup(jndiNameForPool);
        if (connectorConnectionPool == null) {
            String i18nMsg = localStrings.getString("ccp_adm.null_pool", poolName);
            ConnectorRuntimeException cre = new ConnectorRuntimeException(i18nMsg);
            _logger.log(Level.SEVERE, "rardeployment.connectionpool_object_null", poolName);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "", cre);
            }
            throw cre;
        }
        return connectorConnectionPool;
    }

    /**
     * Returns the resource adapter object corresponding
     * to the pool
     *
     * @param connectorConnectionPool Name of the pool.
     *                                MCF pertaining to this pool is
     *                                created/returned.
     * @return Resource adapter instance corresponding to this pool.
     * @throws ConnectorRuntimeException if creation/retrieval
     *                                   of RA fails
     */
    private ActiveResourceAdapter getResourceAdapter(ConnectorConnectionPool connectorConnectionPool)
            throws ConnectorRuntimeException {

        String rarName = connectorConnectionPool.getConnectorDescriptorInfo().getRarName();
        ActiveResourceAdapter activeResourceAdapter = getActiveResourceAdapter(rarName);
        if (activeResourceAdapter == null) {
            String i18nMsg = localStrings.getString("ccp_adm.active_ra_not_init", rarName);

            ConnectorRuntimeException cre = new ConnectorRuntimeException(i18nMsg);
            _logger.log(Level.SEVERE, "rardeployment.resourceadapter_not_initialized", rarName);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "", cre);
            }
            throw cre;
        }
        return activeResourceAdapter;
    }

    private ActiveResourceAdapter getActiveResourceAdapter(String rarName)
            throws ConnectorRuntimeException {
        ActiveResourceAdapter activeResourceAdapter = _registry.getActiveResourceAdapter(rarName);
        //we dont need this check for normal resources of domain.xml config, but datasource-definition needs it.
        if (activeResourceAdapter == null) {
            ifSystemRarLoad(rarName);
            activeResourceAdapter = _registry.getActiveResourceAdapter(rarName);
        }
        return activeResourceAdapter;
    }

    /** Returns the MCF instances.
     *  @param poolName Name of the pool.MCF pertaining to this pool is
     *         created/returned.
     *  @return created/already present MCF instance
     *  @throws ConnectorRuntimeException if creation/retrieval of MCF fails
     */
    public ManagedConnectionFactory[] obtainManagedConnectionFactories(
           String poolName) throws ConnectorRuntimeException {
	ManagedConnectionFactory[] mcfs = null;
        String raName = null;
        try {
		ConnectorConnectionPool conPool =
				getConnectorConnectionPool(poolName);
		ActiveResourceAdapter activeResourceAdapter =
					getResourceAdapter(conPool);
            raName = activeResourceAdapter.getModuleName();
                mcfs =
                     activeResourceAdapter.
                        createManagedConnectionFactories
                                (conPool, null);
        } catch(NamingException ne) {
            String i18nMsg = localStrings.getString(
                "pingpool.name_not_bound", poolName);
            ConnectorRuntimeException cre = new
                ConnectorRuntimeException( i18nMsg);
            cre.initCause(ne);
            _logger.log(Level.FINE,"rardeployment.jndi_lookup_failed",
                               poolName);
            if (_logger.isLoggable( Level.FINE ) ) {
                _logger.log(Level.FINE,"",cre);
            }
            //_logger.log(Level.SEVERE,"",cre);
            throw cre;
        }
        catch(NullPointerException ne) {
            String i18nMsg = localStrings.getString(
                "ccp_adm.failed_to_register_mcf", poolName);
            ConnectorRuntimeException cre = new
                ConnectorRuntimeException( i18nMsg );
            cre.initCause(ne);
            _logger.log(Level.SEVERE,"mcf_add_toregistry_failed",poolName);
            if (_logger.isLoggable( Level.FINE ) ) {
                _logger.log(Level.FINE,"",cre);
            }
            //_logger.log(Level.SEVERE,"",cre);
            throw cre;
        }
        for(ManagedConnectionFactory mcf : mcfs){
            validateMCF(mcf, raName);
        }
	return mcfs;
    }

    private void validateMCF(ManagedConnectionFactory mcf, String raName) {
        _runtime.getConnectorBeanValidator().validateJavaBean(mcf, raName);
    }


    public ManagedConnectionFactory obtainManagedConnectionFactory(String poolName) throws ConnectorRuntimeException{
        return obtainManagedConnectionFactory(poolName, null);
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
    public ManagedConnectionFactory obtainManagedConnectionFactory(String poolName, Hashtable env)
            throws ConnectorRuntimeException {
        try {
            if (_registry.isMCFCreated(poolName)) {
                return _registry.getManagedConnectionFactory(poolName);
            } else {

                ConnectorConnectionPool connectorConnectionPool = getConnectorConnectionPool(poolName, env);
                ActiveResourceAdapter activeResourceAdapter = getResourceAdapter(connectorConnectionPool);
                ClassLoader loader = activeResourceAdapter.getClassLoader();
                ManagedConnectionFactory mcf = activeResourceAdapter.
                        createManagedConnectionFactory(connectorConnectionPool, loader);
                if (mcf != null) {
                    //validate MCF before it is used or related pooling infrastructure is created.
                    validateMCF(mcf, activeResourceAdapter.getModuleName());

                    ResourcePrincipal prin =
                            getDefaultResourcePrincipal(poolName, mcf, env);
                    Subject s = ConnectionPoolObjectsUtils.createSubject(mcf, prin);
                    int txSupport = connectorConnectionPool.getTransactionSupport();

                    //JSR-322 : check the runtime transaction level support of MCF and use appropriately.
                    if (mcf instanceof javax.resource.spi.TransactionSupport) {
                        TransactionSupport.TransactionSupportLevel mcfTS =
                                ((javax.resource.spi.TransactionSupport) mcf).getTransactionSupport();

                        int containerTxSupport = ConnectionPoolObjectsUtils.convertSpecTxSupportToContainerTxSupport(mcfTS);
                        boolean isValidTxSupportLevel = ConnectionPoolObjectsUtils.isTxSupportConfigurationSane(
                                containerTxSupport, activeResourceAdapter.getModuleName());

                        if (isValidTxSupportLevel) {
                            txSupport = containerTxSupport;
                        } else {

                            Object params[] = { mcfTS, activeResourceAdapter.getModuleName() };
                            String i18nMsg = localStrings.getString("ccp_adm_service.incorrect_tx_support", params);
                            ConnectorRuntimeException cre = new
                                    ConnectorRuntimeException(i18nMsg);
                            _logger.log(Level.SEVERE, "rardeployment.incorrect_tx_support",
                                    connectorConnectionPool.getName());
                            throw cre;
                        }
                    }

                    boolean isPM = false;
                    boolean isNonTx = connectorConnectionPool.isNonTransactional();
                    ConnectorSecurityMap[] securityMaps =
                            connectorConnectionPool.getSecurityMaps();
                    RuntimeSecurityMap runtimeSecurityMap =
                            SecurityMapUtils.processSecurityMaps(securityMaps);
                    boolean lazyEnlistable = connectorConnectionPool.isLazyConnectionEnlist();
                    boolean lazyAssoc = connectorConnectionPool.isLazyConnectionAssoc();

                    if (isPM || isNonTx) {
                        /*
                        We should not do lazyEnlistment if we are an __pm
                        resource since we won't have an InvocationContext and
                        the lazy enlistment depends upon an InvocationContext
                        For a nonTx resource enlistment (lazy or otherwise)
                        doesn't come into the picture at all
                        */
                        lazyEnlistable = false;
                    }

                    if (isPM) {
                        //We need to switch off lazy association here because of
                        //the way our Persistence layer behaves. Adding a system
                        //property here to allow other persistence layers to use
                        //lazy association with PM resources
                        if (lazyAssoc) {
                            String str = System.getProperty(
                                    "com.sun.enterprise.resource.AllowLazyAssociationWithPM", "FALSE");
                            if (str.toUpperCase().trim().equals("FALSE")) {
                                lazyAssoc = false;
                            }
                        }
                    }

                    PoolMetaData pmd = new PoolMetaData(poolName, mcf, s, txSupport, prin,
                            isPM, isNonTx, lazyEnlistable, runtimeSecurityMap, lazyAssoc);
                    logFine(pmd.toString());
                    _registry.addManagedConnectionFactory(poolName, pmd);
                }

                PoolType pt = getPoolType(connectorConnectionPool);

                createAndAddPool(poolName, pt, env);
                return mcf;
            }
        } catch (NamingException ne) {
            String i18nMsg = localStrings.getString("pingpool.name_not_bound", poolName);
            ConnectorRuntimeException cre = new
                    ConnectorRuntimeException(i18nMsg);
            cre.initCause(ne);
            _logger.log(Level.FINE, "rardeployment.jndi_lookup_failed", poolName);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "", cre);
            }
            throw cre;
        }
        catch (NullPointerException ne) {
            String i18nMsg = localStrings.getString("ccp_adm.failed_to_register_mcf", poolName);
            ConnectorRuntimeException cre = new ConnectorRuntimeException(i18nMsg);
            cre.initCause(ne);
            _logger.log(Level.SEVERE, "mcf_add_toregistry_failed", poolName);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "", cre);
            }
            throw cre;
        }
    }


    private PoolType getPoolType(ConnectorConnectionPool connectorConnectionPool) {
        PoolType pt = PoolType.STANDARD_POOL;
        if (!connectorConnectionPool.isPoolingOn()) {
            pt = PoolType.POOLING_DISABLED;
        } else if (connectorConnectionPool.isAssociateWithThread()) {
            pt = PoolType.ASSOCIATE_WITH_THREAD_POOL;
        } else if (connectorConnectionPool.isPartitionedPool()) {
            pt = PoolType.PARTITIONED_POOL;
        }
        return pt;
    }

    public PoolType getPoolType(String poolName) throws ConnectorRuntimeException {
        ConnectorConnectionPool ccp;
        try {
            ccp = getConnectorConnectionPool(poolName);
        } catch (NamingException e) {
            ConnectorRuntimeException cre = new ConnectorRuntimeException(e.getMessage());
            cre.initCause(e);
            throw cre;
        }
        return getPoolType(ccp);
    }


    private void logFine(String msg) {
        if (msg != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine(msg);
            }
        }
    }

    /**
     * create an empty connection pool
     *
     * @param poolName pool to be created
     * @param pt       type of pool
     * @throws ConnectorRuntimeException
     */
    private void createAndAddPool(String poolName, PoolType pt, Hashtable env) throws ConnectorRuntimeException {
        PoolManager poolMgr = _runtime.getPoolManager();
        try {
            poolMgr.createEmptyConnectionPool(poolName, pt, env);
        } catch (PoolingException pe) {
            String i18nMsg = localStrings.getString("ccp_adm.failed_to_create_pool_object");
            ConnectorRuntimeException cre = new ConnectorRuntimeException(i18nMsg);
            cre.initCause(pe);
            throw cre;
        }
    }

    /**
     * Checks if a conncetor connection pool has been deployed to this server
     * instance
     *
     * @param poolName
     * @return
     */
    public boolean isConnectorConnectionPoolDeployed(String poolName) {
        try {
            Context ic = ConnectorRuntime.getRuntime().getNamingManager().getInitialContext();
            String jndiName = ConnectorAdminServiceUtils.
                    getReservePrefixedJNDINameForPool(poolName);
            ic.lookup(jndiName);
            return true;
        } catch (NamingException e) {
            return false;
        }
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
        if (ccp == null) {
            throw new ConnectorRuntimeException("No pool to reconfigure, new pool object is null");
        }
        logFine("new ccp :\n" + ccp.toString());

        //see if the new ConnectorConnectionPool is different from
        //the original one and update relevant properties
        String poolName = ccp.getName();
        ConnectorConnectionPool origCcp = null;
        try {
            origCcp = getOriginalConnectorConnectionPool(poolName);
        } catch (NamingException ne) {
            throw new ConnectorRuntimeException(ne.getMessage());
        }

        if (origCcp == null) {
            throw new ConnectorRuntimeException("No pool to reconfigure, original pool object is null");
        }
        logFine("original ccp :\n" + origCcp.toString());

        ConnectionPoolReconfigHelper.ReconfigAction action = ConnectionPoolReconfigHelper.compare(origCcp, ccp,
                excludedProps);
        logFine("pool reconfig action == " + action);
        if (action == ConnectionPoolReconfigHelper.ReconfigAction.UPDATE_MCF_AND_ATTRIBUTES) {
            updateMCFAndPoolAttributes(ccp);
        } else if (action == ConnectionPoolReconfigHelper.ReconfigAction.RECREATE_POOL) {
            return true;
        }

        return false;
    }

    private void updateMCFAndPoolAttributes(ConnectorConnectionPool
            ccp) throws ConnectorRuntimeException {
        String poolName = ccp.getName();
        try {
            ConnectorConnectionPool origCcp =
                    getOriginalConnectorConnectionPool(poolName);

            //update properties
            origCcp.setSteadyPoolSize(ccp.getSteadyPoolSize());
            origCcp.setMaxPoolSize(ccp.getMaxPoolSize());
            origCcp.setMaxWaitTimeInMillis(ccp.getMaxWaitTimeInMillis());
            origCcp.setPoolResizeQuantity(ccp.getPoolResizeQuantity());
            origCcp.setIdleTimeoutInSeconds(ccp.getIdleTimeoutInSeconds());
            origCcp.setFailAllConnections(ccp.isFailAllConnections());

            //lazyEnlist, lazyAssoc and assocWithThread not required since they result
            //in a pool restart anyways, so they wouldn't have changed if we
            //came here
            origCcp.setMatchConnections(ccp.matchConnections());
            origCcp.setMaxConnectionUsage(ccp.getMaxConnectionUsage());
            origCcp.setNonComponent(ccp.isNonComponent());
            origCcp.setNonTransactional(ccp.isNonTransactional());
            origCcp.setConCreationRetryAttempts(ccp.getConCreationRetryAttempts());
            origCcp.setConCreationRetryInterval
                    (ccp.getConCreationRetryInterval());
            origCcp.setValidateAtmostOncePeriod(ccp.getValidateAtmostOncePeriod());
            origCcp.setConnectionLeakTracingTimeout(ccp.getConnectionLeakTracingTimeout());
            origCcp.setConnectionReclaim(ccp.isConnectionReclaim());

            //now rebind the object in jndi
            String jndiNameForPool = ConnectorAdminServiceUtils.getReservePrefixedJNDINameForPool(poolName);
            InitialContext ic = new InitialContext();
            ic.unbind(jndiNameForPool);
            _runtime.getNamingManager().publishObject(jndiNameForPool, (Object) origCcp, true);


        } catch (NamingException ne) {
            throw new ConnectorRuntimeException(ne.getMessage());
        }

        //Check if this pool has been brought into memory
        //If its already in memory, just call reconfig on it

        PoolManager poolMgr = _runtime.getPoolManager();
        try {
            poolMgr.reconfigPoolProperties(ccp);
        } catch (PoolingException pe) {
            throw new ConnectorRuntimeException(pe.getMessage());
        }
        //Run setXXX methods on the copy of the MCF that we have
        //this is done to update the MCF to reflect changes in the
        //MCF properties for which we don't really need to recreate
        //the pool
        ConnectorRegistry registry = ConnectorRegistry.getInstance();
        ManagedConnectionFactory mcf = registry.getManagedConnectionFactory(
                poolName);
        SetMethodAction sma = new SetMethodAction(mcf,
                ccp.getConnectorDescriptorInfo().getMCFConfigProperties());
        try {
            sma.run();
        } catch (Exception e) {
            _logger.log(Level.WARNING, e.getMessage());
            ConnectorRuntimeException cre = new ConnectorRuntimeException(e.getMessage());
            cre.initCause(e);
            throw cre;
        }

        //update the properties "allow-non-component-callers" and
        //"non-transactional-connections" in the PoolMetaData
        PoolMetaData pmd = registry.getPoolMetaData(poolName);
        pmd.setIsPM(ccp.isNonComponent());
        pmd.setIsNonTx(ccp.isNonTransactional());
        pmd.setAuthCredentialsDefinedInPool(ccp.getAuthCredentialsDefinedInPool());

        logFine("Pool properties reconfiguration done");
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
        ConnectorRegistry registry = ConnectorRegistry.getInstance();
        if (registry == null) {
            throw new ConnectorRuntimeException(
                    "Cannot get ConnectorRegistry");
        }
        String poolName = ccp.getName();
        //First remove this pool from memory
        try {
            unloadAndKillPool(poolName);
        } catch (ConnectorRuntimeException cre) {
            throw cre;
        }
        //kill the pool
        //FIXME: deleteConnectorConnectionPool should do this
        //PoolManager poolManager = Switch.getSwitch().getPoolManager();
        //poolManager.killPool( poolName );

        //Now bind the updated pool and
        //obtain a new managed connection factory for this pool

        String jndiNameForPool = ConnectorAdminServiceUtils.
                getReservePrefixedJNDINameForPool(poolName);
        ManagedConnectionFactory mcf = null;
        try {
            _runtime.getNamingManager().publishObject(jndiNameForPool, (Object) ccp, true);
            mcf = obtainManagedConnectionFactory(poolName);

        } catch (NamingException ne) {
            _logger.log(Level.SEVERE,
                    "rardeployment.pool_jndi_bind_failure", poolName);
            String i18nMsg = localStrings.getString(
                    "ccp_adm.could_not_recreate_pool", poolName);
            ConnectorRuntimeException crex = new ConnectorRuntimeException(i18nMsg);
            crex.initCause(ne);
            throw crex;
        } finally{
            if (mcf == null) {
                try{
                    Context ic = _runtime.getNamingManager().getInitialContext();
                    ic.unbind(jndiNameForPool);
                }catch(NamingException e){
                    _logger.log(Level.WARNING,"Unable to unbind the pool configuration object " +
                            "of pool [ "+ poolName+" ] during MCF creation failure");
                }
                _logger.log(Level.WARNING, "rardeployment.mcf_creation_failure", poolName);

                String i18nMsg = localStrings.getString(
                        "ccp_adm.failed_to_create_mcf", poolName);
                throw new ConnectorRuntimeException(i18nMsg);
            }
        }

    }

    /**
     * unloads and kills the connector Connection pool without checking for
     * resources in domain.xml.
     *
     * @param poolName Name of the pool to delete
     * @throws ConnectorRuntimeException if pool unload or kill operation fails
     */

    private void unloadAndKillPool(String poolName)
            throws ConnectorRuntimeException {

        killPool(poolName);
        boolean result = _registry.removeManagedConnectionFactory(poolName);
        if (result == false) {
            _logger.log(Level.SEVERE,
                    "rardeployment.mcf_removal_failure", poolName);
            String i18nMsg = localStrings.getString(
                    "ccp_adm.wrong_params_for_create", poolName);
            ConnectorRuntimeException cre = new
                    ConnectorRuntimeException(i18nMsg);
            _logger.log(Level.FINE, "", cre);
            throw cre;
        }
        try {
            String jndiNameForPool = ConnectorAdminServiceUtils.getReservePrefixedJNDINameForPool(poolName);
            Context ic = ConnectorRuntime.getRuntime().getNamingManager().getInitialContext();
            ic.unbind(jndiNameForPool);
        } catch (NamingException ne) {
            String i18nMsg = localStrings.getString(
                    "ccp_adm.failed_to_remove_from_jndi", poolName);
            ConnectorRuntimeException cre = new
                    ConnectorRuntimeException(i18nMsg);
            cre.initCause(ne);
            _logger.log(Level.SEVERE,
                    "rardeployment.connectionpool_removal_from_jndi_error",
                    poolName);
            _logger.log(Level.FINE, "", cre);
            throw cre;
        }

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
                                              List<Property> props, List<SecurityMap> securityMaps)
            throws ConnectorRuntimeException {

        if ((ccp == null) || (connectionDefinitionName == null)
                || (rarName == null)) {
            _logger.log(Level.FINE, "Wrong parameters for pool creation ");
            String i18nMsg = localStrings.getString(
                    "ccp_adm.wrong_params_for_create");
            throw new ConnectorRuntimeException(i18nMsg);
        }
        ConnectorDescriptor connectorDescriptor =
                _registry.getDescriptor(rarName);
        if (connectorDescriptor == null) {
            String i18nMsg = localStrings.getString(
                    "ccp_adm.no_conn_pool_obj", rarName);
            ConnectorRuntimeException cre = new ConnectorRuntimeException(
                    i18nMsg);
            _logger.log(Level.SEVERE,
                    "rardeployment.connector_descriptor_notfound_registry", rarName);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        }
        Set connectionDefs =
                connectorDescriptor.getOutboundResourceAdapter().getConnectionDefs();
        ConnectionDefDescriptor cdd = null;
        Iterator it = connectionDefs.iterator();
        while (it.hasNext()) {
            cdd = (ConnectionDefDescriptor) it.next();
            if (connectionDefinitionName.equals(cdd.getConnectionFactoryIntf()))
                break;

        }
        ConnectorDescriptorInfo cdi = new ConnectorDescriptorInfo();

        cdi.setRarName(rarName);
        cdi.setResourceAdapterClassName(
                connectorDescriptor.getResourceAdapterClass());
        cdi.setConnectionDefinitionName(cdd.getConnectionFactoryIntf());
        cdi.setManagedConnectionFactoryClass(
                cdd.getManagedConnectionFactoryImpl());
        cdi.setConnectionFactoryClass(cdd.getConnectionFactoryImpl());
        cdi.setConnectionFactoryInterface(cdd.getConnectionFactoryIntf());
        cdi.setConnectionClass(cdd.getConnectionImpl());
        cdi.setConnectionInterface(cdd.getConnectionIntf());
        Set mergedProps = ConnectorDDTransformUtils.mergeProps(props, cdd.getConfigProperties());
        cdi.setMCFConfigProperties(mergedProps);
        cdi.setResourceAdapterConfigProperties(
                connectorDescriptor.getConfigProperties());
        ccp.setSecurityMaps(SecurityMapUtils.getConnectorSecurityMaps(securityMaps));
        createConnectorConnectionPool(ccp, cdi);
    }
    
    /**
     * Flush Connection pool by reinitializing the connections 
     * established in the pool.
     * @param poolName
     * @throws com.sun.appserv.connectors.internal.api.ConnectorRuntimeException
     */
    public boolean flushConnectionPool(String poolName) throws ConnectorRuntimeException {
        if (PoolType.POOLING_DISABLED.equals(getPoolType(poolName))) {
            //throw RuntimeException
            String i18nMsg = localStrings.getString("flush_when_pooling_disabled", poolName);
            _logger.log(Level.SEVERE, "flush_when_pooling_disabled", poolName);
            throw new ConnectorRuntimeException(i18nMsg);
        }
        PoolManager poolMgr = _runtime.getPoolManager();
        try {
            return poolMgr.flushConnectionPool( poolName );
        } catch (PoolingException ex) {
            throw new ConnectorRuntimeException(ex.getMessage());
        }
    }

    /**
     * Get a sql connection from the DataSource specified by the jdbcJndiName.
     * This API is intended to be used in the DAS. The motivation for having this
     * API is to provide the CMP backend a means of acquiring a connection during
     * the codegen phase. If a user is trying to deploy an app on a remote server,
     * without this API, a resource reference has to be present both in the DAS
     * and the server instance. This makes the deployment more complex for the
     * user since a resource needs to be forcibly created in the DAS Too.
     * This API will mitigate this need.
     *
     * @param jndiName the jndi name of the resource being used to get Connection from
     *                 This resource can either be a pmf resource or a jdbc resource
     * @param user     the user used to authenticate this request
     * @param password the password used to authenticate this request
     * @return a java.sql.Connection
     * @throws java.sql.SQLException in case of errors
     */
    public Connection getConnection(String jndiName, String user, String password)
            throws SQLException {
        java.sql.Connection con = null;
        try {
            //DASResourcesUtil.setAdminConfigContext();
            String poolName = getPoolNameFromResourceJndiName(jndiName);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("ConnectorRuntime.getConnection :: poolName : " + poolName);
            }
            //Maintain consitency with the ConnectionManagerImpl change to be checked in later
            String passwd = (password == null) ? "" : password;

            //From what we have seen so far, the user cannot be null
            //but password can be
            //if user is null we will use default authentication
            //TODO: Discuss if this is the right thing to do
            ResourcePrincipal prin = (user == null) ?
                    null : new ResourcePrincipal(user, password);
            con = (java.sql.Connection) getUnpooledConnection(poolName, prin, true);
            if (con == null) {
                String i18nMsg = localStrings.getString(
                        "ccp_adm.null_unpooled_connection");
                throw new SQLException(i18nMsg);
            }
        } catch (ResourceException re) {
            SQLException sqle = new SQLException(re.getMessage());
            sqle.initCause(re);
            _logger.log(Level.WARNING, "jdbc.exc_get_conn", re.getMessage());
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine(" getConnection in ConnectorRuntime failed : " + re);
            }
            throw sqle;
        } catch (Exception ex) {
            SQLException sqle = new SQLException(ex.getMessage());
            sqle.initCause(ex);
            _logger.log(Level.WARNING, "jdbc.exc_get_conn", ex.getMessage());
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine(" getConnection in ConnectorRuntime failed : " + ex);
            }
            throw sqle;
        }/* finally {
            try {
                DASResourcesUtil.resetAdminConfigContext();
            } catch (Exception e) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("caught exception while setting " +
                            "getConnectionFromConnectorRuntime to false");
                }
            }

        }*/

        return con;
    }

    /**
     * Get a sql connection from the DataSource specified by the jdbcJndiName.
     * This API is intended to be used in the DAS. The motivation for having this
     * API is to provide the CMP backend a means of acquiring a connection during
     * the codegen phase. If a user is trying to deploy an app on a remote server,
     * without this API, a resource reference has to be present both in the DAS
     * and the server instance. This makes the deployment more complex for the
     * user since a resource needs to be forcibly created in the DAS Too.
     * This API will mitigate this need.
     *
     * @param jndiName the jndi name of the resource being used to get Connection from
     *                 This resource can either be a pmf resource or a jdbc resource
     * @return a java.sql.Connection
     * @throws java.sql.SQLException in case of errors
     */
    public Connection getConnection(String jndiName)
            throws SQLException {
        java.sql.Connection con = null;
        try {
            //DASResourcesUtil.setAdminConfigContext();
            String poolName = getPoolNameFromResourceJndiName(jndiName);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("ConnectorRuntime.getConnection :: poolName : "
                        + poolName);
            }
            con = (java.sql.Connection) getUnpooledConnection(poolName, null,
                    true);
            if (con == null) {
                String i18nMsg = localStrings.getString(
                        "ccp_adm.null_unpooled_connection");
                throw new SQLException(i18nMsg);
            }
        } catch (ResourceException re) {
            SQLException sqle = new SQLException(re.getMessage());
            sqle.initCause(re);
            _logger.log(Level.WARNING, "jdbc.exc_get_conn", re.getMessage());
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Exception : " + re);
            }
            throw sqle;
        } catch (Exception ex) {
            SQLException sqle = new SQLException(ex.getMessage());
            sqle.initCause(ex);
            _logger.log(Level.WARNING, "jdbc.exc_get_conn", ex.getMessage());
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine(" getConnection in ConnectorRuntime failed : " + ex);
            }
            throw sqle;
        } /*finally {
            try {
              //  DASResourcesUtil.resetAdminConfigContext();
            } catch (Exception e) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("caught exception while setting " +
                            "getConnectionFromConnectorRuntime to false");
                }
            }
        }*/

        return con;
    }

    /**
     * Gets the Pool name that this JDBC resource points to. In case of a PMF resource
     * gets the pool name of the pool pointed to by jdbc resource being pointed to by
     * the PMF resource
     *
     * @param jndiName the jndi name of the resource being used to get Connection from
     *                 This resource can either be a pmf resource or a jdbc resource
     * @return poolName of the pool that this resource directly/indirectly points to
     */
    private String getPoolNameFromResourceJndiName(String jndiName) {
        //String poolName = null;
        JdbcResource jdbcResource = null;
/*     
        DASResourcesUtil resourcesUtil = (DASResourcesUtil) ResourcesUtil.createInstance();

        //check if the jndi name is that of a pmf resource or a jdbc resource
        PersistenceManagerFactoryResource pmfRes =
                resourcesUtil.getPMFResourceByJndiName(jndiName );
	if (pmfRes != null) {
	    jdbcRes = resourcesUtil.getJdbcResourceByJndiName(
                    pmfRes.getJdbcResourceJndiName());
	} else {

	    jdbcRes = resourcesUtil.getJdbcResourceByJndiName(jndiName);
/*	}*/
        ConnectorRuntime runtime = ConnectorRuntime.getRuntime();
        jdbcResource = (JdbcResource) runtime.getResources().getResourceByName(JdbcResource.class, jndiName);

        if (jdbcResource != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("jdbcRes is ---: " + jdbcResource.getJndiName());
                _logger.fine("poolName is ---: " + jdbcResource.getPoolName());
            }
        }
        return jdbcResource == null ? null : jdbcResource.getPoolName();
    }
}
