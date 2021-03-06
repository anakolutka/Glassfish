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

import com.sun.enterprise.*;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.naming.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.runtime.connector.ResourceAdapter;
import com.sun.enterprise.deployment.runtime.connector.*;
import com.sun.enterprise.util.*;
import com.sun.enterprise.connectors.util.*;
import com.sun.enterprise.resource.*;
import com.sun.enterprise.server.*;
import com.sun.enterprise.config.*;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import javax.resource.spi.*;
import javax.naming.*;
import javax.sql.*;
import java.security.*;
import java.sql.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.logging.*;
import com.sun.appserv.connectors.spi.*;


/**
 * This class represents the abstraction of a 1.0 complient rar.
 * It holds the ra.xml (connector decriptor) values, class loader used to
 * to load the Resource adapter class and managed connection factory and
 * module name (rar) to which it belongs.
 * It is also the base class for ActiveInboundResourceAdapter(a 1.5 complient
 * rar).
 * @author :    Srikanth P and Binod PG
 */

public class ActiveOutboundResourceAdapter implements ActiveResourceAdapter {

    protected ConnectorDescriptor desc_;
    protected String moduleName_;
    protected ClassLoader jcl_;
    protected ConnectionDefDescriptor[] connectionDefs_;
    protected ConnectorRuntime connectorRuntime_ = null;

    static Logger _logger = LogDomains.getLogger(LogDomains.RSR_LOGGER);
    private StringManager localStrings =
        StringManager.getManager( ActiveOutboundResourceAdapter.class);

    /**
     * Constructor.
     * @param desc Connector Descriptor. Holds the all ra.xml values
     * @param moduleName  Name of the module i.e rar Name. Incase of
     *        embedded resource adapters its name will be appName#rarName
     * @param jcl Classloader used to load the ResourceAdapter and managed
     *        connection factory class.
     * @param writeToDomainXML Flag indicating whether to write sun-ra.xml
              values to domain.xml.
     */

    public ActiveOutboundResourceAdapter( ConnectorDescriptor desc,
                 String moduleName, ClassLoader jcl)
    {

        this.desc_ = desc;
        moduleName_ = moduleName;
        jcl_ = jcl;
        connectorRuntime_ = ConnectorRuntime.getRuntime();
        connectionDefs_ = ConnectorDDTransformUtils. getConnectionDefs(desc_);
    }


    public String getModuleName() {
        return moduleName_;
    }

    /**
     * It initializes the resource adapter. It also creates the default pools
     * and resources of all the connection definitions.
     * @throws ConnectorRuntimeException. This exception is thrown if the
     *         ra.xml is invalid or the default pools and resources couldn't
     *         be created
     */

    public void setup() throws ConnectorRuntimeException {

        if(connectionDefs_ == null ||
                    connectionDefs_.length != 1) {
            _logger.log(Level.SEVERE,"rardeployment.invalid_connector_desc",
                    moduleName_);
	    String i18nMsg = localStrings.getString(
	        "ccp_adm.invalid_connector_desc");
            throw new ConnectorRuntimeException( i18nMsg );
        }
        ResourcesUtil resUtil = ResourcesUtil.createInstance();

        if(isServer() && !resUtil.belongToSystemRar(moduleName_)) {
            createAllConnectorResources();
        }
        _logger.log(Level.FINE,
		"Completed Active Resource adapter setup", moduleName_);
    }

    /** Check if the execution environment is appserver runtime or application
     *  client container.
     */

    protected boolean isServer() {
        if(connectorRuntime_.getEnviron() == ConnectorConstants.SERVER) {
            return true;
        } else {
            return false;
        }
    }

    /** Creates both the default connector connection pools and resources
     */

    protected void createAllConnectorResources()
                         throws ConnectorRuntimeException
    {
        try {

            if (desc_.getSunDescriptor() != null &&
                desc_.getSunDescriptor().getResourceAdapter() != null) {
        
                // sun-ra.xml exists
                String jndiName = (String)desc_.getSunDescriptor().
                    getResourceAdapter().getValue(ResourceAdapter.JNDI_NAME);
            
                if (jndiName == null || jndiName.equals("")) {
                    // jndiName is empty, do not create duplicate pools, use setting in sun-ra.xml
                    createDefaultConnectorConnectionPools(true);
                } else {
                    // jndiName is not empty, so create duplicate pools, both default and sun-ra.xml
                    createSunRAConnectionPool();
                    createDefaultConnectorConnectionPools(false);
                 }
        
             } else {
            
                 // sun-ra.xml doesn't exist, so create default pools
                 createDefaultConnectorConnectionPools(false);
        
             }
        
             // always create default connector resources
             createDefaultConnectorResources();
        } catch (ConnectorRuntimeException cre) {
             //Connector deployment should _not_ fail if default connector
             //connector pool and resource creation fails.
            _logger.log(Level.SEVERE,"rardeployment.defaultpoolresourcecreation.failed",
                    cre);
            _logger.log(Level.FINE, "Error while trying to create the default connector" +
                    "connection pool and resource", cre);
          }

    }

    /** Deletes both the default connector connection pools and resources
     */

    protected void destroyAllConnectorResources() {
       if(!(ResourcesUtil.createInstance().belongToSystemRar(moduleName_))){
           deleteDefaultConnectorResources();
           deleteDefaultConnectorConnectionPools();
        
           // Added to ensure clean-up of the Sun RA connection pool
           if (desc_.getSunDescriptor() != null &&
               desc_.getSunDescriptor().getResourceAdapter() != null) {
                    
               // sun-ra.xml exists
               String jndiName = (String)desc_.getSunDescriptor().
                   getResourceAdapter().getValue(ResourceAdapter.JNDI_NAME);
            
               if (jndiName == null || jndiName.equals("")) {
                   // jndiName is empty, sunRA pool not created, so don't need to delete
                
                } else {
                    // jndiName is not empty, need to delete pool
                   deleteSunRAConnectionPool();    
                }
           }
       }
    }

    /** Deletes the default connector connection pools.
     */

    protected void deleteDefaultConnectorConnectionPools() {
        for(int i=0;i<connectionDefs_.length;++i) {
            String connectionDefName =
                    connectionDefs_[i].getConnectionFactoryIntf();
            String resourceJndiName = connectorRuntime_.getDefaultPoolName(
                    moduleName_,
                    connectionDefName);
            try {
                connectorRuntime_.deleteConnectorConnectionPool(
                               resourceJndiName);
            } catch(ConnectorRuntimeException cre) {
                _logger.log(Level.WARNING,
                    "rar.undeployment.default_resource_delete_fail",
                    resourceJndiName);
            }
        }
    }

    /** Deletes the default connector resources.
     */

    protected void deleteDefaultConnectorResources() {
        for(int i=0;i<connectionDefs_.length;++i) {
            String connectionDefName =
                    connectionDefs_[i].getConnectionFactoryIntf();
            String resourceJndiName = connectorRuntime_.getDefaultResourceName(
                    moduleName_,
                    connectionDefName);
            try {
                connectorRuntime_.deleteConnectorResource(resourceJndiName);
            } catch(ConnectorRuntimeException cre) {
                _logger.log(Level.WARNING,
                    "rar.undeployment.default_resource_delete_fail",
                    resourceJndiName);
                _logger.log(Level.FINE, "Error while trying to delete the default " +
                        "connector resource", cre);
            }
        }
    }

    /**
     * uninitializes the resource adapter. It also destroys the default pools
     * and resources
     */

    public void destroy() {
        if(isServer()) {
            destroyAllConnectorResources();
        }
    }

    /** Returns the Connector descriptor which represents/holds ra.xml
     *  @return ConnectorDescriptor Representation of ra.xml.
     */

    public ConnectorDescriptor getDescriptor() {
        return desc_;
    }

    /** Creates managed Connection factories corresponding to one pool.
     *  This should be implemented in the ActiveJmsResourceAdapter, for
     * jms resources, has been implemented to perform xa resource recovery
     * in mq clusters, not supported for any other code path.
     *  @param ccp Connector connection pool which contains the pool properties
     *             and ra.xml values pertaining to managed connection factory
     *             class. These values are used in MCF creation.
     *  @param jcl Classloader used to managed connection factory class.
     *  @return ManagedConnectionFactory created managed connection factories
     */
    public ManagedConnectionFactory [] createManagedConnectionFactories (
                ConnectorConnectionPool ccp, ClassLoader jcl) {
	throw new UnsupportedOperationException("This operation is not supported");
    }

	
    /** Creates managed Connection factory instance.
     *  @param ccp Connector connection pool which contains the pool properties
     *             and ra.xml values pertaining to managed connection factory
     *             class. These values are used in MCF creation.
     *  @param jcl Classloader used to managed connection factory class.
     *  @return ManagedConnectionFactory created managed connection factory
     *          instance
     */
    public ManagedConnectionFactory createManagedConnectionFactory (
                ConnectorConnectionPool ccp, ClassLoader jcl)
    {
            final String mcfClass = ccp.getConnectorDescriptorInfo().
                          getManagedConnectionFactoryClass();
        try { 

             ManagedConnectionFactory mcf = null;
             if (moduleName_.equals(ConnectorRuntime.DEFAULT_JMS_ADAPTER)) {
                  Object tmp = AccessController.doPrivileged(
                     new PrivilegedExceptionAction() {
                  public Object run() throws Exception{
                     return instantiateMCF(mcfClass);
                    }
                  });
                  mcf = (ManagedConnectionFactory) tmp;
	     } else {
                    mcf = instantiateMCF(mcfClass);
             }
             
             if (mcf instanceof ConfigurableTransactionSupport) {
            	 TransactionSupport ts = 
            		 ConnectionPoolObjectsUtils.getTransactionSupport(
            				 ccp.getTransactionSupport()); 
            	 ((ConfigurableTransactionSupport)mcf).setTransactionSupport(ts);
             }

            SetMethodAction setMethodAction = new SetMethodAction(mcf,
                ccp.getConnectorDescriptorInfo().getMCFConfigProperties());
            setMethodAction.run();
            _logger.log(Level.FINE,"Created MCF object : ",mcfClass);
            return mcf;
        } catch (PrivilegedActionException ex) {
            _logger.log(Level.SEVERE,
                      "rardeployment.privilegedaction_error",
                       new Object[]{mcfClass, ex.getException().getMessage()});
            _logger.log(Level.FINE,
                    "rardeployment.privilegedaction_error",
                       ex.getException());
            return null;
        } catch (ClassNotFoundException Ex) {
            _logger.log(Level.SEVERE,"rardeployment.class_not_found",
                        new Object[]{mcfClass, Ex.getMessage()});
            _logger.log(Level.FINE,"rardeployment.class_not_found",
                        Ex);
            return null;
        } catch (InstantiationException Ex) {
            _logger.log(Level.SEVERE,
                      "rardeployment.class_instantiation_error",
                                new Object[]{mcfClass, Ex.getMessage()});
            _logger.log(Level.FINE,
                    "rardeployment.class_instantiation_error",
                                Ex);
            return null;
        } catch (IllegalAccessException Ex) {
            _logger.log(Level.SEVERE,
                      "rardeployment.illegalaccess_error",
                                new Object[]{mcfClass, Ex.getMessage()});
            _logger.log(Level.FINE,
                    "rardeployment.illegalaccess_error",
                                Ex);

            return null;
        } catch (Exception Ex) {
            _logger.log(Level.SEVERE,"rardeployment.mcfcreation_error",
                        new Object[]{mcfClass, Ex.getMessage()});
            _logger.log(Level.FINE,"rardeployment.mcfcreation_error",
                        Ex);
            return null;
        }

    }

    private ManagedConnectionFactory instantiateMCF(String mcfClass)
                                                   throws Exception {
        if(jcl_ != null) {
            return (ManagedConnectionFactory)jcl_.loadClass(
                     mcfClass).newInstance();
        } else {
            return (ManagedConnectionFactory) Class.forName(
                     mcfClass).newInstance();
        }
    }


    /** Creates default connector resource
     */

    protected void createDefaultConnectorResources()
                    throws ConnectorRuntimeException
    {
        for(int i=0; i<connectionDefs_.length;++i) {
            String connectionDefName =
                    connectionDefs_[i].getConnectionFactoryIntf();
            String resourceName = connectorRuntime_.getDefaultResourceName(
                    moduleName_,
                    connectionDefName);
            String poolName = connectorRuntime_.getDefaultPoolName(
                    moduleName_,
                    connectionDefName);
            connectorRuntime_.createConnectorResource(
                              resourceName,poolName,null);
            _logger.log(Level.FINE,"Created default connector resource : ",
                 resourceName);

        }
    }

    /** Creates default connector connection pool
     *  @param useSunRA whether to use default pool settings or settings in sun-ra.xml
     */

    protected void createDefaultConnectorConnectionPools(boolean useSunRA)
                    throws ConnectorRuntimeException
    {

        for(int i = 0;i < connectionDefs_.length;++i) {
            String poolName = connectorRuntime_.getDefaultPoolName(
                    moduleName_,
                    connectionDefs_[i].getConnectionFactoryIntf());

            ConnectorDescriptorInfo connectorDescriptorInfo =
                       ConnectorDDTransformUtils.getConnectorDescriptorInfo(
                       connectionDefs_[i]);
            connectorDescriptorInfo.setRarName(moduleName_);
            connectorDescriptorInfo.setResourceAdapterClassName(
                       desc_.getResourceAdapterClass());
            ConnectorConnectionPool connectorPoolObj;
            
            // if useSunRA is true, then create connectorPoolObject using settings
            // from sunRAXML
            if (useSunRA) {
                connectorPoolObj =
                    ConnectionPoolObjectsUtils.createSunRaConnectorPoolObject(poolName, desc_, moduleName_);
            } else {
                connectorPoolObj =
                    ConnectionPoolObjectsUtils.createDefaultConnectorPoolObject(poolName, moduleName_);
            }

            connectorPoolObj.setConnectorDescriptorInfo(
                connectorDescriptorInfo );
            connectorRuntime_.createConnectorConnectionPool(connectorPoolObj);
            _logger.log(Level.FINE,"Created default connection pool : ",
                 poolName);
        }
    }

    /**
     * Creates connector connection pool pertaining to sun-ra.xml. This is
     * only for 1.0 complient rars.
     * @throws ConnectorRuntimeException Thrown when pool creation fails.
     */

    public void createSunRAConnectionPool()
                throws ConnectorRuntimeException
    {

        String defaultPoolName = connectorRuntime_.getDefaultPoolName(
                 moduleName_,connectionDefs_[0].getConnectionFactoryIntf());

        String sunRAPoolName = defaultPoolName+ConnectorConstants.SUN_RA_POOL;

        ConnectorDescriptorInfo connectorDescriptorInfo =
            ConnectorDDTransformUtils.getConnectorDescriptorInfo(
                 connectionDefs_[0]);
        connectorDescriptorInfo.setRarName(moduleName_);
        connectorDescriptorInfo.setResourceAdapterClassName(
                 desc_.getResourceAdapterClass());
        ConnectorConnectionPool connectorPoolObj =
                 ConnectionPoolObjectsUtils.createSunRaConnectorPoolObject(
                 sunRAPoolName,desc_,moduleName_);

        connectorPoolObj.setConnectorDescriptorInfo(
            connectorDescriptorInfo );
        connectorRuntime_.createConnectorConnectionPool(connectorPoolObj);
        _logger.log(Level.FINE,"Created SUNRA connection pool:",sunRAPoolName);
        String jndiName = (String)desc_.getSunDescriptor().
                 getResourceAdapter().getValue(ResourceAdapter.JNDI_NAME);
        connectorRuntime_.createConnectorResource(jndiName,sunRAPoolName,null);
        _logger.log(Level.FINE,"Created SUNRA connector resource : ",
                     jndiName);

    }

    /**
     * Added to clean up the connector connection pool pertaining to sun-ra.xml. This is
     * only for 1.0 complient rars.
     */

    public void deleteSunRAConnectionPool()
    {

        String defaultPoolName = connectorRuntime_.getDefaultPoolName(
                 moduleName_,connectionDefs_[0].getConnectionFactoryIntf());

        String sunRAPoolName = defaultPoolName+ConnectorConstants.SUN_RA_POOL;
        try {
            connectorRuntime_.deleteConnectorConnectionPool(
                           sunRAPoolName);
        } catch(ConnectorRuntimeException cre) {
            _logger.log(Level.WARNING,
                "rar.undeployment.default_resource_delete_fail",
                sunRAPoolName);
        }
    }

    /**
     * Returns the class loader that is used to load the RAR.
     * @return <code>ClassLoader</code> object.
     */
    public ClassLoader getClassLoader(){
        return jcl_;
    }

}
