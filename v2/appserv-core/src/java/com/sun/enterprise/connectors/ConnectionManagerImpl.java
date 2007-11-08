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

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Principal;
import java.util.Properties;
import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.resource.spi.IllegalStateException;
import javax.resource.NotSupportedException;
import com.sun.enterprise.*;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.connectors.ConnectorConnectionPool;
import com.sun.enterprise.connectors.ConnectorDescriptorInfo;
import com.sun.enterprise.connectors.authentication.AuthenticationService;
import com.sun.enterprise.deployment.ResourcePrincipal;
import com.sun.enterprise.distributedtx.J2EETransactionManagerOpt;
import com.sun.enterprise.naming.NamingManagerImpl;
import com.sun.enterprise.log.Log;
import com.sun.enterprise.iiop.PEORBConfigurator;
import com.sun.enterprise.resource.*;
import javax.security.auth.Subject;
import javax.naming.InitialContext;
import javax.resource.spi.security.PasswordCredential;
import java.util.logging.*;
import java.util.Set;
import com.sun.logging.*;
import com.sun.enterprise.connectors.util.ConnectionPoolObjectsUtils;

/**
 * @author Tony Ng
 */
public class ConnectionManagerImpl 
    implements ConnectionManager, Serializable {
    
    protected String jndiName;
    
    protected String poolName;

    // Create logger object per Java SDK 1.4 to log messages
    // introduced Santanu De, Sun Microsystems, March 2002

    static Logger _logger = LogDomains.getLogger(LogDomains.RSR_LOGGER);

    //The RAR name
    //This is pushed into the object in the connector runtime during
    //jndi publishing
    protected String rarName;

    protected ResourcePrincipal defaultPrin = null;

    public ConnectionManagerImpl(String poolName) {
        this.poolName = poolName;
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }
    
    public String getJndiName() {
        return jndiName;
    }
    
    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    /**
     * Allocate a non transactional connection. This connection, even if
     * acquired in the context of an existing transaction, will never
     * be associated with a transaction
     * The typical use case may be to check the original contents of an EIS
     * when a transacted connection is changing the contents, and the tx
     * is yet to be committed.
     *
     * We create a ResourceSpec for a non tx connection with a name ending
     * in __nontx. This is to maintain uniformity with the scheme of having
     * __pm connections. 
     * If one were to create a resource with a jndiName ending with __nontx
     * the same functionality might be achieved.
     */
    public Object allocateNonTxConnection( ManagedConnectionFactory mcf,
        ConnectionRequestInfo cxRequestInfo ) throws ResourceException 
    { 
        String localJndiName = jndiName;

	if (_logger.isLoggable(Level.FINE)) {
	    _logger.fine("Allocating NonTxConnection");
	}

	//If a resource has been created with __nontx, we don't want to
	//add it again.
	//Otherwise we need to add __nontx at the end to ensure that the
	//mechanism to check for the correct resource manager still works
	//We do the addition if and only if we are getting this call
	//from a normal datasource and not a __nontx datasource.
        if ( ! jndiName.endsWith( ConnectorConstants.NON_TX_JNDI_SUFFIX ) ) {
	    localJndiName = jndiName + ConnectorConstants.NON_TX_JNDI_SUFFIX ;
	    
	    if (_logger.isLoggable(Level.FINE)) {
	        _logger.fine("Adding __nontx to jndiname");
	    }
	} else {
	    if (_logger.isLoggable(Level.FINE)) {
	        _logger.fine("lookup happened from a __nontx datasource directly");
	    }
	}

        return allocateConnection( mcf, cxRequestInfo, localJndiName);


    }
    
    public Object
        allocateConnection(ManagedConnectionFactory mcf,
                           ConnectionRequestInfo cxRequestInfo)
        throws ResourceException {

        return this.allocateConnection( mcf, cxRequestInfo, jndiName); 
    }

    public Object allocateConnection(ManagedConnectionFactory mcf,
        ConnectionRequestInfo cxRequestInfo, String jndiNameToUse)
        throws ResourceException 
    {
        return this.allocateConnection( mcf, cxRequestInfo, jndiNameToUse, null );
    }

    public Object allocateConnection(ManagedConnectionFactory mcf,
        ConnectionRequestInfo cxRequestInfo, String jndiNameToUse, Object conn)
        throws ResourceException 
    {
        validatePool();
        PoolManager poolmgr = Switch.getSwitch().getPoolManager();
	boolean resourceShareable = true;

	ResourceReferenceDescriptor ref =  poolmgr.getResourceReference(jndiNameToUse);
	if (ref != null) {
	    String shareableStr = ref.getSharingScope();

	    // MQ resource adapter doesnt support connection sharing. Disabling it
	    // for the moment.
            if (shareableStr.equals(ref.RESOURCE_UNSHAREABLE)) {
                resourceShareable = false;
            }
        }

        if (ref == null) {
            _logger.log(Level.FINE,"poolmgr.no_resource_reference",jndiNameToUse);
            return internalGetConnection(mcf, defaultPrin, cxRequestInfo, 
                resourceShareable, jndiNameToUse, conn, true);
        }
        String auth = ref.getAuthorization();
	
        if (auth.equals(ref.APPLICATION_AUTHORIZATION) ) {
	    if (cxRequestInfo == null ) {
                StringManager localStrings =
                    StringManager.getManager(ConnectionManagerImpl.class);
	        String msg = localStrings.getString(
		    "con_mgr.null_userpass");
	        throw new ResourceException( msg );
	    }
            ConnectorRuntime.getRuntime().switchOnMatching(rarName, poolName);
            return internalGetConnection(mcf, null, cxRequestInfo,
                resourceShareable, jndiNameToUse, conn, false );
        } else {
            ResourcePrincipal prin =null;
            Set principalSet =null;
            Principal callerPrincipal = null;
            SecurityContext securityContext = null;
            ConnectorRuntime connectorRuntime = ConnectorRuntime.getRuntime();
            if(connectorRuntime.isServer() && 
             (securityContext = SecurityContext.getCurrent()) != null &&
	     (callerPrincipal = securityContext.getCallerPrincipal()) != null &&
	     (principalSet = securityContext.getPrincipalSet()) != null) {
                AuthenticationService authService = 
                    connectorRuntime.getAuthenticationService(rarName,poolName);
                if(authService != null) {
                    prin = (ResourcePrincipal)authService.mapPrincipal(
                            callerPrincipal, principalSet);
                }
            }
            
            if (prin == null) { 
                prin = ref.getResourcePrincipal();
                if (prin == null) {
                    _logger.log(Level.FINE,"default-resource-principal not" +
		                "specified for " + jndiNameToUse + ". Defaulting to" +
				" user/password specified in the pool");

                    prin = defaultPrin;
                } else if (!prin.equals(defaultPrin)){
                    ConnectorRuntime.getRuntime().switchOnMatching(rarName, poolName);
                }
            }
            return internalGetConnection(mcf, prin, cxRequestInfo,
                resourceShareable, jndiNameToUse, conn, false);
        }
   
    }


    protected Object internalGetConnection(ManagedConnectionFactory mcf,
        final ResourcePrincipal prin, ConnectionRequestInfo cxRequestInfo,
        boolean shareable, String jndiNameToUse, Object conn, boolean isUnknownAuth) 
        throws ResourceException 
    {
        try {
            PoolManager poolmgr = Switch.getSwitch().getPoolManager();
	    ConnectorRegistry registry = ConnectorRegistry.getInstance();
            PoolMetaData pmd = registry.getPoolMetaData( poolName );
            
            ResourceSpec spec = new ResourceSpec(jndiNameToUse, 
                    ResourceSpec.JNDI_NAME, pmd);
	    spec.setConnectionPoolName(this.poolName);
	    ManagedConnectionFactory freshMCF = pmd.getMCF();

        if(_logger.isLoggable(Level.INFO)){
            if (! freshMCF.equals(mcf)) {
                _logger.info("conmgr.mcf_not_equal");
            }
        }
            ConnectorDescriptor desc = registry.getDescriptor(rarName);

            Subject subject = null;
            ClientSecurityInfo info = null;
            boolean subjectDefined = false;
            if (isUnknownAuth && rarName.equals(ConnectorConstants.DEFAULT_JMS_ADAPTER)
                  && !(pmd.isAuthCredentialsDefinedInPool())) {
                 //System.out.println("Unkown Auth - pobably nonACC client");
                 //Unknown authorization. This is the case for standalone java clients,
                 //where the authorization is neither container nor component
                 //managed. In this case we associate an non-null Subject with no
                 //credentials, so that the RA can either use its own custom logic
                 //for figuring out the credentials. Relevant connector spec section
                 //is 9.1.8.2.
                 //create non-null Subject associated with no credentials
                 //System.out.println("RAR name "+ rarName);
                subject = ConnectionPoolObjectsUtils.createSubject(mcf, null);
            } else {
                if (prin == null) {
                    info = new ClientSecurityInfo(cxRequestInfo);
                } else {
                    info = new ClientSecurityInfo(prin);
                    //If subject is defined, don't attempt to redefine it
                    if (subject == null) {
                        if (prin.equals(defaultPrin)) {
                           subject = pmd.getSubject();
                        } else {
                           subject = ConnectionPoolObjectsUtils.createSubject(mcf, prin);
                        }
                    }
                }
            }

	    int txLevel = pmd.getTransactionSupport();
	    if (_logger.isLoggable(Level.FINE)) {
	        _logger.fine( "ConnectionMgr: poolName " + poolName +
		    "  txLevel : " + txLevel );
	    }
            
            ResourceAllocator alloc = null;
            if ( conn != null ) {
                spec.setConnectionToAssociate( conn );
            }
            
            switch (txLevel) {
            
            case ConnectorConstants.NO_TRANSACTION_INT:
                alloc =
                    new NoTxConnectorAllocator(poolmgr, mcf, spec,
                                               subject, cxRequestInfo, info,
                                               desc);
                return poolmgr.getResource(spec, alloc, info);
                
            case ConnectorConstants.LOCAL_TRANSACTION_INT:
                if (!shareable) {
                    StringManager localStrings =
                        StringManager.getManager(ConnectionManagerImpl.class);
		    String i18nMsg = localStrings.getString(
		        "con_mgr.resource_not_shareable");
		    throw new ResourceAllocationException( i18nMsg );	
                }
                alloc = 
                    new LocalTxConnectorAllocator(poolmgr, mcf, spec,
                                                  subject, cxRequestInfo,
                                                  info, desc);
                return poolmgr.getResource(spec, alloc, info);
            case ConnectorConstants.XA_TRANSACTION_INT:
                if (rarName.equals(ConnectorRuntime.DEFAULT_JMS_ADAPTER)) {
                    shareable = false;
                }
		spec.markAsXA();
                alloc = 
                    new ConnectorAllocator(poolmgr, mcf, spec, 
                                           subject, cxRequestInfo, info, desc,
                                           shareable);
                return poolmgr.getResource(spec, alloc, info);
            default:
                StringManager localStrings =
                    StringManager.getManager(ConnectionManagerImpl.class);
	        String i18nMsg = localStrings.getString(
		    "con_mgr.illegal_tx_level",  txLevel+ " ");
                throw new IllegalStateException(i18nMsg);
            }

        } catch (PoolingException ex) {
             Object[]  params = new Object[]{poolName, ex.getMessage()};
            _logger.log(Level.WARNING,"poolmgr.get_connection_failure",params);
            StringManager localStrings =
                StringManager.getManager(ConnectionManagerImpl.class);
            String i18nMsg = localStrings.getString( 
	        "con_mgr.error_creating_connection", ex.getMessage() );
            ResourceAllocationException rae = new ResourceAllocationException(
	        i18nMsg );
	    rae.initCause( ex );
            throw rae;
        }
    }

      
    public void setRarName( String _rarName ) {
        rarName = _rarName;
    }

    public String getRarName() {
        return rarName;
    }

    /*
    * This method is called from the ConnectorObjectFactory lookup
    * With this we move all the housekeeping work in allocateConnection
    * up-front 
    *
    */
    public void initialize() throws ConnectorRuntimeException {
    
        Switch sw = Switch.getSwitch();
	if (sw.getContainerType() == ConnectorConstants.NON_ACC_CLIENT) { 
            //Invocation from a non-ACC client
	    try {
	        // Initialize Transaction service. By now the ORB must have
		// been created during the new InitialContext() call using
		// the ORBManager
		PEORBConfigurator.initTransactionService(null, new Properties() );

	        if (sw.getTransactionManager() == null) { 
		    sw.setTransactionManager(new J2EETransactionManagerOpt());
		}
                // There wont be any invocation manager. So, treat this as a system 
	        // resource.
	        jndiName = ResourceInstaller.getPMJndiName(jndiName);
	    } catch(Exception e) {
	        throw (ConnectorRuntimeException) 
                    new ConnectorRuntimeException(e.getMessage()).initCause(e);
	    }
	}
        
        ConnectorRuntime runtime = ConnectorRuntime.getRuntime();
        ManagedConnectionFactory mcf = runtime.obtainManagedConnectionFactory(poolName);
	ConnectorRegistry registry = ConnectorRegistry.getInstance();
        PoolMetaData pmd = registry.getPoolMetaData( poolName );
        defaultPrin = pmd.getResourcePrincipal();

    }
    
    private void validatePool() throws ResourceException{
        ConnectorRegistry registry = ConnectorRegistry.getInstance();
        if (registry.getPoolMetaData(poolName) == null){
            StringManager localStrings = 
                StringManager.getManager(ConnectionManagerImpl.class);
            String msg = localStrings.getString("con_mgr.no_pool_meta_data");
            throw new ResourceException(poolName + ": " + msg);
            }
    }
}
