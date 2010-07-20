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

package com.sun.enterprise.resource.recovery;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.deployment.ResourcePrincipal;
import com.sun.enterprise.transaction.api.XAResourceWrapper;
import com.sun.enterprise.transaction.spi.RecoveryResourceHandler;
import com.sun.logging.LogDomains;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import org.jvnet.hk2.config.types.Property;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.security.PasswordCredential;
import javax.resource.ResourceException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import java.security.Principal;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Habitat;

/**
 * Recovery Handler for Jdbc Resources
 *
 * @author Jagadish Ramu
 */
@Service
public class JdbcRecoveryResourceHandler implements RecoveryResourceHandler {

    @Inject
    private TransactionService txService;

    @Inject
    private Resources resources;

    @Inject
    private Habitat connectorRuntimeHabitat;

    private ResourcesUtil resourcesUtil = null;

    private static Logger _logger = LogDomains.getLogger(JdbcRecoveryResourceHandler.class, LogDomains.RSR_LOGGER);

    private void loadAllJdbcResources() {

        try {
            Collection<JdbcResource> jdbcResources = getJdbcResources();
            InitialContext ic = new InitialContext();
            for (Resource resource : jdbcResources) {
                JdbcResource jdbcResource = (JdbcResource) resource;
                if(getResourcesUtil().isEnabled(jdbcResource)) {
                    try {
                        ic.lookup(jdbcResource.getJndiName());
                    } catch (Exception ex) {
                        _logger.log(Level.SEVERE, "error.loading.jdbc.resources.during.recovery",
                                jdbcResource.getJndiName());
                        if (_logger.isLoggable(Level.FINE)) {
                            _logger.log(Level.FINE, ex.toString(), ex);
                        }
                    }
                }
            }
        } catch (NamingException ne) {
            _logger.log(Level.SEVERE, "error.loading.jdbc.resources.during.recovery", ne.getMessage());
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, ne.toString(), ne);
            }
        }
    }

    private Collection<JdbcResource> getJdbcResources() {
        return resources.getResources(JdbcResource.class);
    }

    private ResourcesUtil getResourcesUtil(){
        if(resourcesUtil == null){
            resourcesUtil = ResourcesUtil.createInstance();
        }
        return resourcesUtil;
    }

    /**
     * {@inheritDoc}
     */
    public void loadXAResourcesAndItsConnections(List xaresList, List connList) {

        Collection<JdbcResource> jdbcResources = getJdbcResources();

        if (jdbcResources == null || jdbcResources.size() == 0) {
            return;
        }

        List<JdbcConnectionPool> jdbcPools = new ArrayList<JdbcConnectionPool>();

        for (Resource resource : jdbcResources) {
            JdbcResource jdbcResource = (JdbcResource) resource;
            if(getResourcesUtil().isEnabled(jdbcResource)) {
                JdbcConnectionPool pool = getJdbcConnectionPoolByName(jdbcResource.getPoolName());
                if (pool != null &&
                        "javax.sql.XADataSource".equals(pool.getResType())) {
                    jdbcPools.add(pool);
                }
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("JdbcRecoveryResourceHandler:: loadXAResourcesAndItsConnections :: "
                            + "adding : " + (jdbcResource.getPoolName()));
                }
            }
        }

        //TODO V3 done so as to initialize connectors-runtime before loading jdbc-resources. need a better way ?
        ConnectorRuntime crt = connectorRuntimeHabitat.getComponent(ConnectorRuntime.class);

        loadAllJdbcResources();
        // Read from the transaction-service , if the replacement of
        // Vendor XAResource class with our version required.
        // If yes, put the mapping in the xaresourcewrappers properties.
        Properties XAResourceWrappers = new Properties();

        XAResourceWrappers.put(
                "oracle.jdbc.xa.client.OracleXADataSource",
                "com.sun.enterprise.transaction.jts.recovery.OracleXAResource");

        List<Property> properties = txService.getProperty();

        if (properties != null) {
            for (Property property : properties) {
                String name = property.getName();
                String value = property.getValue();
                if (name.equals("oracle-xa-recovery-workaround")) {
                    if ("false".equals(value)) {
                        XAResourceWrappers.remove(
                                "oracle.jdbc.xa.client.OracleXADataSource");
                    }
                } else if (name.equals("sybase-xa-recovery-workaround")) {
                    if (value.equals("true")) {
                        XAResourceWrappers.put(
                                "com.sybase.jdbc2.jdbc.SybXADataSource",
                                "com.sun.enterprise.transaction.jts.recovery.SybaseXAResource");
                    }
                }
            }
        }

        for(JdbcConnectionPool jdbcConnectionPool : jdbcPools){
            if (jdbcConnectionPool.getResType() == null
                    || jdbcConnectionPool.getName() == null
                    || !jdbcConnectionPool.getResType().equals(
                    "javax.sql.XADataSource")) {
                continue;
            }
            String poolName = jdbcConnectionPool.getName();
            try {

                String[] dbUserPassword = getdbUserPasswordOfJdbcConnectionPool(jdbcConnectionPool);
                String dbUser = dbUserPassword[0];
                String dbPassword = dbUserPassword[1];
                ManagedConnectionFactory fac =
                        crt.obtainManagedConnectionFactory(poolName);
                Subject subject = new Subject();
                PasswordCredential pc = new PasswordCredential(
                        dbUser, dbPassword.toCharArray());
                pc.setManagedConnectionFactory(fac);
                Principal prin = new ResourcePrincipal(dbUser, dbPassword);
                subject.getPrincipals().add(prin);
                subject.getPrivateCredentials().add(pc);
                ManagedConnection mc = fac.createManagedConnection(subject, null);
                connList.add(mc);
                try {
                    XAResource xares = mc.getXAResource();
                    if (xares != null) {

                        // See if a wrapper class for the vendor XADataSource is
                        // specified if yes, replace the XAResouce class of database
                        // vendor with our own version

                        String datasourceClassname =
                                jdbcConnectionPool.getDatasourceClassname();
                        String wrapperclass = (String) XAResourceWrappers.get(
                                datasourceClassname);
                        if (wrapperclass != null) {
                            //need to load wrapper class provided by "transactions" module.
                            //Using connector-class-loader so as to get access to "transaction" module.
                            XAResourceWrapper xaresWrapper = null;
                            xaresWrapper = (XAResourceWrapper) crt.getConnectorClassLoader().loadClass(wrapperclass).
                                    newInstance();
                            xaresWrapper.init(mc, subject);
                            xaresList.add(xaresWrapper);
                        } else {
                            xaresList.add(xares);
                        }
                    }
                } catch (ResourceException ex) {
                    // ignored. Not at XA_TRANSACTION level
                }
            } catch (Exception ex) {
                _logger.log(Level.WARNING, "datasource.xadatasource_error",
                        poolName);
                _logger.log(Level.FINE, "datasource.xadatasource_error_excp", ex);
            }
        }
    }


    private JdbcConnectionPool getJdbcConnectionPoolByName(String poolName) {
        return (JdbcConnectionPool)resources.getResourceByName(JdbcConnectionPool.class, poolName);
    }

    /**
     * {@inheritDoc}
     */
    public void closeConnections(List connList) {
        for (Object obj : connList) {
            try {
                ManagedConnection con = (ManagedConnection)obj;
                con.destroy();
            } catch (Exception ex) {
                _logger.log(Level.WARNING, "recovery.jdbc-resource.destroy-error", ex);
            }
        }
    }

    /**
     * gets the user-name & password for the jdbc-connection-pool
     * @param jdbcConnectionPool connection pool
     * @return user, password
     */
    public String[] getdbUserPasswordOfJdbcConnectionPool(
            JdbcConnectionPool jdbcConnectionPool) {

        String[] userPassword = new String[2];
        userPassword[0] = null;
        userPassword[1] = null;
        List<Property> properties = jdbcConnectionPool.getProperty();
        if (properties != null && properties.size() > 0) {
            for (Property property : properties) {
                String prop = property.getName().toUpperCase();
                if ("USERNAME".equals(prop) || "USER".equals(prop)) {
                    userPassword[0] = property.getValue();
                } else if ("PASSWORD".equals(prop)) {
                    userPassword[1] = property.getValue();
                }
            }
        } else {
            return userPassword;
        }
        return userPassword;
    }
}
