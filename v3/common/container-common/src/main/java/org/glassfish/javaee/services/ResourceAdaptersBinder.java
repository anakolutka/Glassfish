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
package org.glassfish.javaee.services;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Habitat;
import org.glassfish.api.naming.GlassfishNamingManager;
import com.sun.enterprise.config.serverbeans.JdbcResource;
import com.sun.enterprise.config.serverbeans.JdbcConnectionPool;
import com.sun.enterprise.config.serverbeans.ConnectorResource;
import com.sun.enterprise.config.serverbeans.ConnectorConnectionPool;
import com.sun.appserv.connectors.internal.api.ConnectorConstants;

import javax.naming.NamingException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;

/**
 * Binds proxy objects in the jndi namespace for all the resources and pools defined in the
 * configuration. Those objects will delay the instantiation of the actual resources and pools
 * until code looks them up in the naming manager.
 *
 * @author Jerome Dochez, Jagadish Ramu
 */
@Service
public class ResourceAdaptersBinder {

    @Inject
    private GlassfishNamingManager manager;

    @Inject
    private Logger logger;

    @Inject
    private Habitat raProxyHabitat;

    //TODO V3, if JdbcResource and ConnectorResource has a common super class, pool name can be got and mergeed.
    public void deployAllJdbcResourcesAndPools(JdbcResource[] jdbcResources, JdbcConnectionPool[] jdbcPools) {
        for (JdbcResource resource : jdbcResources) {
            try {
                JdbcConnectionPool pool = getAssociatedJdbcPool(resource.getPoolName(), jdbcPools);
                if (pool == null) {
                    logger.log(Level.SEVERE, "Could not get the pool [ " + resource.getPoolName() + " ] of resource [ " +
                            resource.getJndiName() + " ]");
                    continue;
                }
                bindResource(resource, pool, resource.getJndiName(), ConnectorConstants.RES_TYPE_JDBC);
            } catch (NamingException e) {
                logger.log(Level.SEVERE, "Cannot bind " + resource.getPoolName() + " to naming manager", e);
            }
        }
    }

    public void deployAllConnectorResourcesAndPools(ConnectorResource[] connectorResources,
                                                    ConnectorConnectionPool[] connectorPools) {
        for (ConnectorResource resource : connectorResources) {
            try {
                ConnectorConnectionPool pool = getAssociatedConnectorPool(resource.getPoolName(), connectorPools);
                if (pool == null) {
                    logger.log(Level.SEVERE, "Could not get the pool [ " + resource.getPoolName() + " ] of resource [ " +
                            resource.getJndiName() + " ]");
                    continue;
                }
                bindResource(resource, pool, resource.getJndiName(), ConnectorConstants.RES_TYPE_CR);
            } catch (NamingException e) {
                logger.log(Level.SEVERE, "Cannot bind " + resource.getPoolName() + " to naming manager", e);
            }
        }
    }


    public void bindResource(Object resource, Object pool, String resourceName, String resourceType)
            throws NamingException {
        ResourceAdapterProxy raProxy = constructResourceProxy(resource, pool, resourceType, resourceName);
        manager.publishObject(resourceName, raProxy, true);
    }

    private ResourceAdapterProxy constructResourceProxy(Object resource, Object pool, String resourceType,
                                                        String resourceName) {
        ResourceAdapterProxy raProxy = raProxyHabitat.getComponent(ResourceAdapterProxy.class);
        raProxy.setResource(resource);
        raProxy.setConnectionPool(pool);
        raProxy.setResourceType(resourceType);
        raProxy.setResourceName(resourceName);
        return raProxy;
    }

    /**
     * get the associated pool's name for the jdbc-resource
     * @param  poolName Pool Name
     * @return JdbcConnectionPool
     * @param jdbcPools JdbcConnectionPools
     */
    private JdbcConnectionPool getAssociatedJdbcPool(String poolName, JdbcConnectionPool[] jdbcPools) {
        JdbcConnectionPool result = null;
        for (JdbcConnectionPool pool : jdbcPools) {
            if (pool.getName().equalsIgnoreCase(poolName)) {
                result = pool;
                break;
            }
        }
        return result;
    }

    /**
     * get the associated pool's name for the jdbc-resource
     * @param  poolName Pool Name
     * @param connectorPools Connector Connection Pools
     * @return ConnectorConnectionPool
     */

    private ConnectorConnectionPool getAssociatedConnectorPool(String poolName, ConnectorConnectionPool[] connectorPools) {
        ConnectorConnectionPool result = null;
        for (ConnectorConnectionPool pool : connectorPools) {
            if (pool.getName().equalsIgnoreCase(poolName)) {
                result = pool;
                break;
            }
        }
        return result;
    }

    private List<String> getAllPoolNames(JdbcConnectionPool[] pools) {
        List<String> poolNames = new ArrayList<String>();
        for(JdbcConnectionPool pool : pools){
            poolNames.add(pool.getName());

        }
        return poolNames;
    }
}
