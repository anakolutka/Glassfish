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

package com.sun.enterprise.connectors.util;

import com.sun.appserv.connectors.spi.ConnectorConstants;
import com.sun.appserv.connectors.spi.ConnectorRuntimeException;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.connectors.DeferredResourceConfig;
import org.glassfish.internal.api.ServerContext;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.util.logging.Logger;


public class ResourcesUtil {

    //The thread local ResourcesUtil is used in two cases
    //1. An event config context is to be used as in case of resource
    //   deploy/undeploy and enable/disable events.
    //2. An admin config context to be used for ConnectorRuntime.getConnection(...)
    //   request
    static ThreadLocal<ResourcesUtil> localResourcesUtil =
            new ThreadLocal<ResourcesUtil>();

    static Logger _logger = LogDomains.getLogger(LogDomains.RSR_LOGGER);

    static StringManager localStrings =
            StringManager.getManager(ResourcesUtil.class);

    static ServerContext sc_ = null;

    protected Domain dom = null;

    protected Resources res = null;


    public static void setServerContext(ServerContext sc) {
        sc_ = sc;
    }

    public boolean belongToStandAloneRar(String resourceAdapterName) {

        /* TODO V3
     Applications apps = dom.getApplications();
     ConnectorModule connectorModule = apps.getConnectorModuleByName(resourceAdapterName);
     return connectorModule != null;*/
        return false;
    }

    public static ResourcesUtil createInstance() {

        if (localResourcesUtil.get() != null)
            return localResourcesUtil.get();

        // TODO V3 temporarily return a ResourcesUtil
        localResourcesUtil.set(new ResourcesUtil());
        return localResourcesUtil.get();
    }


    /**
     * This method takes in an admin JdbcConnectionPool and returns the RA
     * that it belongs to.
     *
     * @param pool - The pool to check
     * @return The name of the JDBC RA that provides this pool's datasource
     */

    public String getRANameofJdbcConnectionPool(JdbcConnectionPool pool) {
        String dsRAName = ConnectorConstants.JDBCDATASOURCE_RA_NAME;

        if (pool.getResType() == null || pool.getDatasourceClassname() == null) {
            return dsRAName;
        }
        Class dsClass = null;

        try {
            dsClass = ClassLoadingUtility.loadClass(pool.getDatasourceClassname());
        } catch (ClassNotFoundException cnfe) {
            return dsRAName;
        }

        //check if its XA
        if ("javax.sql.XADataSource".equals(pool.getResType())) {
            if (javax.sql.XADataSource.class.isAssignableFrom(dsClass)) {
                /* TODO V3 handle XA Later
                return ConnectorConstants.JDBCXA_RA_NAME; */
                throw new UnsupportedOperationException("XA is not supported yet");
            }
        }

        //check if its CP
        if ("javax.sql.ConnectionPoolDataSource".equals(pool.getResType())) {
            if (javax.sql.ConnectionPoolDataSource.class.isAssignableFrom(
                    dsClass)) {
                return ConnectorConstants.JDBCCONNECTIONPOOLDATASOURCE_RA_NAME;
            }
        }
        //default to __ds
        return dsRAName;
    }

    public DeferredResourceConfig getDeferredResourceConfig(Object resource, Object pool, String resType, String raName)
            throws ConnectorRuntimeException {
        String resourceAdapterName = raName;
        DeferredResourceConfig resConfig = null;
        if (ConnectorConstants.RES_TYPE_JDBC.equalsIgnoreCase(resType)) {

            JdbcConnectionPool jdbcPool = (JdbcConnectionPool) pool;
            JdbcResource jdbcResource = (JdbcResource) resource;

            resourceAdapterName = getRANameofJdbcConnectionPool((JdbcConnectionPool) pool);

            resConfig = new DeferredResourceConfig(resourceAdapterName, null, null, null, jdbcPool, jdbcResource, null);

            ConfigBeanProxy[] resourcesToload = new ConfigBeanProxy[]{jdbcPool, jdbcResource};
            resConfig.setResourcesToLoad(resourcesToload);

        } else if (ConnectorConstants.RES_TYPE_CR.equalsIgnoreCase(resType)) {
            ConnectorConnectionPool connPool = (ConnectorConnectionPool) pool;
            ConnectorResource connResource = (ConnectorResource) resource;

            //TODO V3 need to get AOR & RA-Config later
            resConfig = new DeferredResourceConfig(resourceAdapterName, null, connPool, connResource, null, null, null);

            ConfigBeanProxy[] resourcesToload = new ConfigBeanProxy[]{connPool, connResource};
            resConfig.setResourcesToLoad(resourcesToload);

        } else {
            throw new ConnectorRuntimeException("unsupported resource type : " + resType);
        }
        return resConfig;
    }

/*
    public DeferredResourceConfig getDeferredJdbcResourceConfig(JdbcResource resource, JdbcConnectionPool pool) {
        DeferredResourceConfig resConfig = null;
        */
/*TODO V3 handle later
        if (resource.isEnabled())*/
/*
        {
            String rarName = getRANameofJdbcConnectionPool(pool);
            resConfig = new DeferredResourceConfig(rarName, null, null, null, pool, resource, null);
            ConfigBeanProxy[] resourcesToload = new ConfigBeanProxy[]{pool, resource};
            resConfig.setResourcesToLoad(resourcesToload);
        }
        return resConfig;
    }
*/

    /* TODO V3 handle later
    public DeferredResourceConfig getDeferredResourceConfig(String resourceName) {
        DeferredResourceConfig resConfig = getDeferredConnectorResourceConfigs(
                resourceName);
        if (resConfig != null) {
            return resConfig;
        }

        resConfig = getDeferredJdbcResourceConfigs(resourceName);

        if (resConfig != null) {
            return resConfig;
        }

//        TODO V3 handle admin-objects later
        resConfig = getDeferredAdminObjectConfigs(
                resourceName);


        return resConfig;
    } */

    /**
     * Returns the deffered connector resource config. This can be resource of JMS RA which is lazily
     * loaded. Or for other connector RA which is not loaded at startup. The connector RA which does
     * not have any resource or admin object associated with it are not loaded at startup. They are
     * all lazily loaded.
     */
    /*
    protected DeferredResourceConfig getDeferredConnectorResourceConfigs(
            String resourceName) {

        if (resourceName == null) {
            return null;
        }
        ConfigBeanProxy[] resourcesToload = new ConfigBeanProxy[2];

        try {
            if (!isReferenced(resourceName)) {
                return null;
            }
        } catch (ConfigException e) {
            String message = localStrings.getString(
                    "error.finding.resources.references",
                    resourceName);
            _logger.log(Level.WARNING, message + e.getMessage());
            _logger.log(Level.FINE, message + e.getMessage(), e);
        }


        ConnectorResource connectorResource =
                res.getConnectorResourceByJndiName(resourceName);
        if (connectorResource == null || !connectorResource.isEnabled()) {
            return null;
        }
        String poolName = connectorResource.getPoolName();
        ConnectorConnectionPool ccPool =
                res.getConnectorConnectionPoolByName(poolName);
        if (ccPool == null) {
            return null;
        }
        String rarName = ccPool.getResourceAdapterName();
        if (rarName != null) {
            resourcesToload[0] = ccPool;
            resourcesToload[1] = connectorResource;

            ResourceAdapterConfig[] resourceAdapterConfig =
                    new ResourceAdapterConfig[1];
            // TODO V3 handle resource adapter config later
            //resourceAdapterConfig[0] =
            //        res.getResourceAdapterConfigByResourceAdapterName(
            //                rarName);

            DeferredResourceConfig resourceConfig =
                    new DeferredResourceConfig(rarName, null, ccPool,
                            connectorResource, null, null,
                            resourceAdapterConfig);
            resourceConfig.setResourcesToLoad(resourcesToload);
            return resourceConfig;
        }
        return null;
    } */

    /**
     * Returns true if the given resource is referenced by this server.
     *
     * @param resourceName the name of the resource
     * @return true if the named resource is used/referred by this server
     * @throws ConfigException if an error while parsing domain.xml
     */
    protected boolean isReferenced(String resourceName) /*throws ConfigException */{
        throw new UnsupportedOperationException();
        /* TODO V3 handle once ServerHelper is available
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("isReferenced :: " + resourceName + " - "
                    + ServerHelper.serverReferencesResource(
                    configContext_, sc_.getInstanceName(),
                    resourceName));
        }

        return ServerHelper.serverReferencesResource(configContext_,
                sc_.getInstanceName(), resourceName);
        */
    }

    /**
     * Checks if a Resource is enabled.
     * <p/>
     * Since 8.1 PE/SE/EE, A resource [except resource adapter configs, connector and
     * JDBC connection pools which are global and hence enabled always] is enabled
     * only when the resource is enabled and there exists a resource ref to this
     * resource in this server instance and that resource ref is enabled.
     * <p/>
     * Before a resource is loaded or deployed, it is checked to see if it is
     * enabled.
     *
     * @since 8.1 PE/SE/EE
     */
/* TODO V3 enable once configBeans (resource.isEnabled()) is implemented
    public boolean isEnabled(ConfigBeanProxy res) throws ConfigException {
        _logger.fine("ResourcesUtil :: isEnabled");
        if (res == null)
            return false;
        if (res instanceof JdbcResource)
            return isEnabled((JdbcResource) res);
        else if (res instanceof ConnectorResource)
            return isEnabled((ConnectorResource) res);
        else if (res instanceof ConnectorConnectionPool)
            return isEnabled((ConnectorConnectionPool) res);
        else if (res instanceof JdbcConnectionPool)
            //JDBC RA is system RA and is always enabled
            return true;

        ResourceRef resRef = null;
*/
/*
        TODO V3 handle arbitrary resource type
        TODO V3 check whether the resource-ref is also enabled
        if(!res.isEnabled())
            return false;

        Server server = ServerBeansFactory.getServerBean(configContext_);

        //using ServerTags, otherwise have to resort to reflection or multiple instanceof/casts
        resRef =  server.getResourceRefByRef(res.getAttributeValue(ServerTags.JNDI_NAME));*/
/*

        if (resRef == null)
            return false;

        return resRef.isEnabled();
    }
*/

/* TODO V3 enable once configBeans (resource.isEnabled()) is implemented
    public boolean isEnabled(ConnectorConnectionPool ccp) throws ConfigException {
        if (ccp == null) {
            return false;
        }
        String raName = ccp.getResourceAdapterName();
        return isRarEnabled(raName);
    }
*/

/* TODO V3 enable once configBeans (resource.isEnabled()) is implemented
    public boolean isEnabled(JdbcResource jr) throws ConfigException {

        if (jr == null || !jr.isEnabled())
            return false;

        // TODO V3 handle resource-ref later
        //if(!isResourceReferenceEnabled(jr.getJndiName()))
        //   return false;

        return true;
    }
*/

/* TODO V3 enable once configBeans (resource.isEnabled()) is implemented
    public boolean isEnabled(ConnectorResource cr) throws ConfigException {

        if (cr == null || !cr.isEnabled())
            return false;

//          TODO V3 handle resource-ref , ccp later
//        if(!isResourceReferenceEnabled(cr.getJndiName()))
//            return false;
//
//        String poolName = cr.getPoolName();
//        ConnectorConnectionPool ccp = res.getConnectorConnectionPoolByName(poolName);
//        if (ccp == null) {
//            return false;
//        }
//
//        return isEnabled(ccp);

        return true;
    }
*/

/* TODO V3 enable once configBeans (dom.getApps().getConnectorModuleByName()) is implemented
    private boolean isRarEnabled(String raName) throws ConfigException {
        if (raName == null || raName.length() == 0)
            return false;
        ConnectorModule module = dom.getApplications().getConnectorModuleByName(raName);
        if (module != null) {
            if (!module.isEnabled())
                return false;
            return isApplicationReferenceEnabled(raName);
        } else if (ConnectorsUtil.belongsToSystemRA(raName)) {
            return true;
        } //  TODO V3 hande embeddedRar later
//        else {
//            return belongToEmbeddedRarAndEnabled(raName);
//        }
        return false;
    }
*/

    /**
     * Checks if a resource reference is enabled
     *
     * @since SJSAS 8.1 PE/SE/EE
     */

/* TODO V3 enable once configBeans (resourceRef.isEnabled()) is implemented
    private boolean isResourceReferenceEnabled(String resourceName)
            throws ConfigException {
        ResourceRef ref = null;
//         TODO V3 handle resource-ref later  ServerHelper
//            ref = ServerHelper.getServerByName( configContext_,
//                sc_.getInstanceName()).getResourceRefByRef(resourceName);

        if (ref == null) {
            _logger.fine("ResourcesUtil :: isResourceReferenceEnabled null ref");
            if (isADeployEvent())
                return true;
            else
                return false;
        }
        _logger.fine("ResourcesUtil :: isResourceReferenceEnabled ref enabled ?" + ref.isEnabled());
        return ref.isEnabled();
    }
*/

    /**
     * Checks if a resource reference is enabled
     *
     * @since SJSAS 9.1 PE/SE/EE
     */
/* TODO V3 handle once appRefEnabled is available
    private boolean isApplicationReferenceEnabled(String appName)
            throws ConfigException {
        ApplicationRef appRef = null;
        */
/* TODO V3 handle ServerHelper later
        appRef = ServerHelper.getServerByName( configContext_,
                sc_.getInstanceName()).getApplicationRefByRef(appName);
        */
/*
        if (appRef == null) {
            _logger.fine("ResourcesUtil :: isApplicationReferenceEnabled null ref");
            if (isADeployEvent())
                return true;
            else
                return false;
        }
        _logger.fine("ResourcesUtil :: isApplicationReferenceEnabled appRef enabled ?" + appRef.isEnabled());
        return appRef.isEnabled();
    }
*/

    /**
     * Checks whether call is from a deploy event.
     * Since in case of deploy event, the localResourceUtil will be set, so check is based on that.
     */
/*
    private boolean isADeployEvent() {
        if (localResourcesUtil.get() != null)
            return true;
        return false;
    }
*/
    public String getResourceType(ConfigBeanProxy cb) {
        //TODO V3 these constants need to be taken from ResourceDeployEvent

        if (cb instanceof ConnectorConnectionPool) {
            return ConnectorConstants.RES_TYPE_CCP;
        } else if (cb instanceof ConnectorResource) {
            return ConnectorConstants.RES_TYPE_CR;
        }
        if (cb instanceof JdbcConnectionPool) {
            return ConnectorConstants.RES_TYPE_JCP;
        } else if (cb instanceof JdbcResource) {
            return ConnectorConstants.RES_TYPE_JDBC;
        }
        return null;
    }

}
