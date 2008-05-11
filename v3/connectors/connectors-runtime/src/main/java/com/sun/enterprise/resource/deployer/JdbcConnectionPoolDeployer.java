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
 * @(#) JdbcConnectionPoolDeployer.java
 *
 * Copyright 2000-2001 by iPlanet/Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of iPlanet/Sun Microsystems, Inc. ("Confidential Information").
 * You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license
 * agreement you entered into with iPlanet/Sun Microsystems.
 */
package com.sun.enterprise.resource.deployer;

import com.sun.appserv.connectors.spi.ConnectorConstants;
import com.sun.appserv.connectors.spi.ConnectorRuntimeException;
import com.sun.enterprise.config.serverbeans.JdbcConnectionPool;
import com.sun.enterprise.config.serverbeans.Property;
import com.sun.enterprise.connectors.ConnectorConnectionPool;
import com.sun.enterprise.connectors.ConnectorDescriptorInfo;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.util.ConnectionPoolObjectsUtils;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.deployment.ConnectionDefDescriptor;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.server.ResourceDeployer;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles Jdbc connection pool events in the server instance. When user adds a
 * jdbc connection pool , the admin instance emits resource event. The jdbc
 * connection pool events are propagated to this object.
 * <p/>
 * The methods can potentially be called concurrently, therefore implementation
 * need to be synchronized.
 *
 * @author Tamil Vengan
 */

// This class was created to fix the bug # 4650787

public class JdbcConnectionPoolDeployer implements ResourceDeployer {

    static private StringManager sm = StringManager.getManager(
            JdbcConnectionPoolDeployer.class);
    static private String msg = sm.getString("resource.restart_needed");

    static private Logger _logger = LogDomains.getLogger(LogDomains.RSR_LOGGER);

    public synchronized void deployResource(Object resource) throws Exception {
        //intentional no-op
        //From 8.1 PE/SE/EE, JDBC connection pools are no more resources and 
        //they would be available only to server instances that have a resoruce-ref
        //that maps to a pool. So deploy resource would not be called during 
        //JDBC connection pool creation. The actualDeployResource method 
        //below is invoked by JdbcResourceDeployer when a resource-ref for a 
        //resource that is pointed to this pool is added to a server instance

        _logger.fine(" JdbcConnectionPoolDeployer - deployResource : " + resource + " calling actualDeploy");
        actualDeployResource(resource);
    }

    /**
     * Deploy the resource into the server's runtime naming context
     *
     * @param resource a resource object
     * @throws Exception thrown if fail
     */
    public synchronized void actualDeployResource(Object resource) {
        _logger.fine(" JdbcConnectionPoolDeployer - actualDeployResource : " + resource);
        com.sun.enterprise.config.serverbeans.JdbcConnectionPool adminPool =
                (com.sun.enterprise.config.serverbeans.JdbcConnectionPool) resource;
        try {
            ConnectorConnectionPool connConnPool = createConnectorConnectionPool(adminPool);
            //now do internal book keeping
            ConnectorRuntime.getRuntime().createConnectorConnectionPool(connConnPool);
        } catch (ConnectorRuntimeException cre) {
            cre.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void undeployResource(Object resource) throws Exception {
        _logger.fine(" JdbcConnectionPoolDeployer - unDeployResource : " +
                "calling actualUndeploy of " + resource);
        actualUndeployResource(resource);
    }

    /**
     * Undeploy the resource from the server's runtime naming context
     *
     * @param resource a resource object
     * @throws UnsupportedOperationException Currently we are not supporting this method.
     */

    public synchronized void actualUndeployResource(Object resource) throws Exception {
        _logger.fine(" JdbcConnectionPoolDeployer - unDeployResource : " + resource);

        com.sun.enterprise.config.serverbeans.JdbcConnectionPool jdbcConnPool =
                (com.sun.enterprise.config.serverbeans.JdbcConnectionPool) resource;

        String poolName = jdbcConnPool.getName();
        ConnectorRuntime runtime = ConnectorRuntime.getRuntime();
        runtime.deleteConnectorConnectionPool(poolName);
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.finest("Pool Undeployed");
        }
    }

    /**
     * Pull out the MCF configuration properties and return them as an array
     * of EnvironmentProperty
     *
     * @param adminPool   - The JdbcConnectionPool to pull out properties from
     * @param conConnPool - ConnectorConnectionPool which will be used by Resource Pool
     * @param connDesc    - The ConnectorDescriptor for this JDBC RA
     * @return EnvironmentProperty[] array of MCF Config properties specified
     *         in this JDBC RA
     */
    private EnvironmentProperty[] getMCFConfigProperties(
            JdbcConnectionPool adminPool,
            ConnectorConnectionPool conConnPool, ConnectorDescriptor connDesc) {

        ArrayList propList = new ArrayList();

        propList.add(new EnvironmentProperty("ClassName",
                adminPool.getDatasourceClassname() == null ? "" :
                        adminPool.getDatasourceClassname(),
                "The datasource class name",
                "java.lang.String"));


        propList.add(new EnvironmentProperty("ConnectionValidationRequired",
                adminPool.getIsConnectionValidationRequired() + "",
                "Is connection validation required",
                "java.lang.String"));

        propList.add(new EnvironmentProperty("ValidationMethod",
                adminPool.getConnectionValidationMethod() == null ? "" :
                        adminPool.getConnectionValidationMethod(),
                "How the connection is validated",
                "java.lang.String"));

        propList.add(new EnvironmentProperty("ValidationTableName",
                adminPool.getValidationTableName() == null ?
                        "" : adminPool.getValidationTableName(),
                "Validation Table name",
                "java.lang.String"));

        propList.add(new EnvironmentProperty("TransactionIsolation",
                adminPool.getTransactionIsolationLevel() == null ? "" :
                        adminPool.getTransactionIsolationLevel(),
                "Transaction Isolatin Level",
                "java.lang.String"));

        propList.add(new EnvironmentProperty("GuaranteeIsolationLevel",
                adminPool.getIsIsolationLevelGuaranteed() + "",
                "Transaction Isolation Guarantee",
                "java.lang.String"));

        propList.add(new EnvironmentProperty("StatementWrapping",
                adminPool.getWrapJdbcObjects() + "",
                "Statement Wrapping",
                "java.lang.String"));


        propList.add(new EnvironmentProperty("StatementTimeout",
                adminPool.getStatementTimeoutInSeconds() + "",
                "Statement Timeout",
                "java.lang.String"));

        //dump user defined poperties into the list
        Set connDefDescSet = connDesc.getOutboundResourceAdapter().
                getConnectionDefs();
        //since this a 1.0 RAR, we will have only 1 connDefDesc
        if (connDefDescSet.size() != 1) {
            throw new MissingResourceException("Only one connDefDesc present",
                    null, null);
        }

        Iterator iter = connDefDescSet.iterator();

        //Now get the set of MCF config properties associated with each
        //connection-definition . Each element here is an EnviromnentProperty
        Set mcfConfigProps = null;
        while (iter.hasNext()) {
            mcfConfigProps = ((ConnectionDefDescriptor) iter.next()).
                    getConfigProperties();
        }
        if (mcfConfigProps != null) {

            Map mcfConPropKeys = new HashMap();
            Iterator mcfConfigPropsIter = mcfConfigProps.iterator();
            while (mcfConfigPropsIter.hasNext()) {
                String key = ((EnvironmentProperty) mcfConfigPropsIter.next()).
                        getName();
                mcfConPropKeys.put(key.toUpperCase(), key);
            }

            String driverProperties = "";
            for (Property rp : adminPool.getProperty()) {
                if (rp == null) {
                    continue;
                }
                String name = rp.getName();
                //TODO V3 can't these property be removed fromc configBean

                //The idea here is to convert the Environment Properties coming from
                //the admin connection pool to standard pool properties thereby
                //making it easy to compare in the event of a reconfig
                if ("MATCHCONNECTIONS".equals(name.toUpperCase())) {
                    //JDBC - matchConnections if not set is decided by the ConnectionManager
                    //so default is false
                    conConnPool.setMatchConnections(toBoolean(rp.getValue(), false));
                    logFine("MATCHCONNECTIONS");

                }else if ("ASSOCIATEWITHTHREAD".equals(name.toUpperCase())) {
                    conConnPool.setAssociateWithThread(toBoolean(rp.getValue(), false));
                    logFine("ASSOCIATEWITHTHREAD");

                } else if ("POOLDATASTRUCTURE".equals(name.toUpperCase())) {
                    conConnPool.setPoolDataStructureType(rp.getValue());
                    logFine("POOLDATASTRUCTURE");

                }else if ("POOLWAITQUEUE".equals(name.toUpperCase())) {
                    conConnPool.setPoolWaitQueue(rp.getValue());
                    logFine("POOLWAITQUEUE");

                } else if ("DATASTRUCTUREPARAMETERS".equals(name.toUpperCase())) {
                    conConnPool.setDataStructureParameters(rp.getValue());
                    logFine("DATASTRUCTUREPARAMETERS");

                } else if ("USERNAME".equals(name.toUpperCase()) ||
                        "USER".equals(name.toUpperCase())) {

                    propList.add(new EnvironmentProperty("User",
                            rp.getValue(), "user name", "java.lang.String"));

                } else if ("PASSWORD".equals(name.toUpperCase())) {

                    propList.add(new EnvironmentProperty("Password",
                            rp.getValue(), "Password", "java.lang.String"));

                } else if ("JDBC30DATASOURCE".equals(name.toUpperCase())) {

                    propList.add(new EnvironmentProperty("JDBC30DataSource",
                            rp.getValue(), "JDBC30DataSource", "java.lang.String"));

                } else if (mcfConPropKeys.containsKey(name.toUpperCase())) {

                    propList.add(new EnvironmentProperty(
                            (String) mcfConPropKeys.get(name.toUpperCase()),
                            rp.getValue() == null ? "" : rp.getValue(),
                            "Some property",
                            "java.lang.String"));
                } else {
                    driverProperties = driverProperties + "set" + escape(name)
                            + "#" + escape(rp.getValue()) + "##";
                }
            }

            if (!driverProperties.equals("")) {
                propList.add(new EnvironmentProperty("DriverProperties",
                        driverProperties,
                        "some proprietarty properties",
                        "java.lang.String"));
            }
        }


        propList.add(new EnvironmentProperty("Delimiter",
                "#", "delim", "java.lang.String"));

        propList.add(new EnvironmentProperty("EscapeCharacter",
                "\\", "escapeCharacter", "java.lang.String"));

        //create an array of EnvironmentProperties from above list
        EnvironmentProperty[] eProps = new EnvironmentProperty[propList.size()];
        ListIterator propListIter = propList.listIterator();

        for (int i = 0; propListIter.hasNext(); i++) {
            eProps[i] = (EnvironmentProperty) propListIter.next();
        }

        return eProps;

    }

    /**
     * To escape the "delimiter" characters that are internally used by Connector & JDBCRA.
     *
     * @param value String that need to be escaped
     * @return Escaped value
     */
    private String escape(String value) {
        CharSequence seq = "\\";
        CharSequence replacement = "\\\\";
        value = value.replace(seq, replacement);

        seq = "#";
        replacement = "\\#";
        value = value.replace(seq, replacement);
        return value;
    }


    private boolean toBoolean(Object prop, boolean defaultVal) {
        if (prop == null) {
            return defaultVal;
        }
        return Boolean.valueOf(((String) prop).toLowerCase());
    }

    /**
     * Use this method if the string being passed does not <br>
     * involve multiple concatenations<br>
     * Avoid using this method in exception-catch blocks as they
     * are not frequently executed <br>
     *
     * @param msg
     */
    private void logFine(String msg) {
        if (_logger.isLoggable(Level.FINE) && msg != null) {
            _logger.fine(msg);
        }
    }

    public ConnectorConnectionPool createConnectorConnectionPool(JdbcConnectionPool adminPool)
            throws ConnectorRuntimeException {

        String moduleName = ResourcesUtil.createInstance().getRANameofJdbcConnectionPool(adminPool);
        int txSupport = getTxSupport(moduleName);

        ConnectorDescriptor connDesc = ConnectorRuntime.getRuntime().getConnectorDescriptor(moduleName);

        //Create the connector Connection Pool object from the configbean object
        ConnectorConnectionPool conConnPool = new ConnectorConnectionPool(
                adminPool.getName());

        conConnPool.setTransactionSupport(txSupport);
        setConnectorConnectionPoolAttributes(conConnPool, adminPool);

        //Initially create the ConnectorDescriptor
        ConnectorDescriptorInfo connDescInfo =
                createConnectorDescriptorInfo(connDesc, moduleName);


        connDescInfo.setMCFConfigProperties(
                getMCFConfigProperties(adminPool, conConnPool, connDesc));

        //since we are deploying a 1.0 RAR, this is null
        connDescInfo.setResourceAdapterConfigProperties((Set) null);

        conConnPool.setConnectorDescriptorInfo(connDescInfo);

        return conConnPool;
    }


    private int getTxSupport(String moduleName) {
        if (ConnectorConstants.JDBCXA_RA_NAME.equals(moduleName)) {
            /*    TODO V3 handle XA later
           return ConnectionPoolObjectsUtils.parseTransactionSupportString(
               ConnectorConstants.XA_TRANSACTION_TX_SUPPORT_STRING );*/
            throw new UnsupportedOperationException("XA is not supported yet");
        }

        return ConnectionPoolObjectsUtils.parseTransactionSupportString(
                ConnectorConstants.LOCAL_TRANSACTION_TX_SUPPORT_STRING);
    }

    private ConnectorDescriptorInfo createConnectorDescriptorInfo(
            ConnectorDescriptor connDesc, String moduleName) {
        ConnectorDescriptorInfo connDescInfo = new ConnectorDescriptorInfo();

        connDescInfo.setManagedConnectionFactoryClass(
                connDesc.getOutboundResourceAdapter().
                        getManagedConnectionFactoryImpl());

        connDescInfo.setRarName(moduleName);

        connDescInfo.setResourceAdapterClassName(connDesc.
                getResourceAdapterClass());

        connDescInfo.setConnectionDefinitionName(
                connDesc.getOutboundResourceAdapter().
                        getConnectionFactoryIntf());

        connDescInfo.setConnectionFactoryClass(
                connDesc.getOutboundResourceAdapter().
                        getConnectionFactoryImpl());

        connDescInfo.setConnectionFactoryInterface(
                connDesc.getOutboundResourceAdapter().
                        getConnectionFactoryIntf());

        connDescInfo.setConnectionClass(
                connDesc.getOutboundResourceAdapter().
                        getConnectionImpl());

        connDescInfo.setConnectionInterface(
                connDesc.getOutboundResourceAdapter().
                        getConnectionIntf());

        return connDescInfo;
    }

    private void setConnectorConnectionPoolAttributes(
            ConnectorConnectionPool ccp, JdbcConnectionPool adminPool) {
        ccp.setMaxPoolSize(adminPool.getMaxPoolSize());
        ccp.setSteadyPoolSize(adminPool.getSteadyPoolSize());
        ccp.setMaxWaitTimeInMillis(adminPool.getMaxWaitTimeInMillis());

        ccp.setPoolResizeQuantity(adminPool.getPoolResizeQuantity());

        ccp.setIdleTimeoutInSeconds(adminPool.getIdleTimeoutInSeconds());

        ccp.setFailAllConnections(Boolean.valueOf(adminPool.getFailAllConnections()));

        ccp.setConnectionValidationRequired(Boolean.valueOf(adminPool.getIsConnectionValidationRequired()));

        ccp.setNonTransactional(Boolean.valueOf(adminPool.getNonTransactionalConnections()));

        //These are default properties of all Jdbc pools
        //So set them here first and then figure out from the parsing routine
        //if they need to be reset
        ccp.setMatchConnections(Boolean.valueOf(adminPool.getMatchConnections()));
        ccp.setAssociateWithThread(Boolean.valueOf(adminPool.getAssociateWithThread()));
        ccp.setConnectionLeakTracingTimeout(adminPool.getConnectionLeakTimeoutInSeconds());
        ccp.setConnectionReclaim(Boolean.valueOf(adminPool.getConnectionLeakReclaim()));

        ccp.setMaxConnectionUsage(adminPool.getMaxConnectionUsageCount());

        ccp.setConCreationRetryAttempts(adminPool.getConnectionCreationRetryAttempts());
        ccp.setConCreationRetryInterval(
                adminPool.getConnectionCreationRetryIntervalInSeconds());

        ccp.setValidateAtmostOncePeriod(adminPool.getValidateAtmostOncePeriodInSeconds());
    }
}
