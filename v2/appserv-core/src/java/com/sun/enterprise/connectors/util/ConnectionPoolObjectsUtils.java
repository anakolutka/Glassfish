

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

import com.sun.enterprise.connectors.*;
import com.sun.enterprise.deployment.runtime.connector.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.config.serverbeans.ElementProperty;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.config.ConfigBean;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.resource.spi.security.PasswordCredential;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.resource.spi.ManagedConnectionFactory;
import java.lang.reflect.Method; 

import org.netbeans.modules.schema2beans.Common;

import com.sun.appserv.connectors.spi.TransactionSupport;


/**
 * This is an util class for creating poolObjects of the type 
 * ConnectorConnectionPool from ConnectorDescriptor and also using the
 * default values.
 * @author Srikanth P
 */ 

public  final class ConnectionPoolObjectsUtils {
    private ConnectionPoolObjectsUtils() { /* disallow instantiation */ }
    
    public static final String ELEMENT_PROPERTY = "ElementProperty";
    private static final Logger _logger = LogDomains.getLogger( LogDomains.RSR_LOGGER );

    private static final String VALIDATE_ATMOST_EVERY_IDLE_SECS =
 	"com.sun.enterprise.connectors.ValidateAtmostEveryIdleSecs";

    private static final String validateAtmostEveryIdleSecsProperty = System.getProperty(VALIDATE_ATMOST_EVERY_IDLE_SECS);

    private static final StringManager localStrings = 
        StringManager.getManager( ConnectionPoolObjectsUtils.class);
    /**
     * Creates default ConnectorConnectionPool consisting of default
     * pool values. 
     * @param poolName Name of the pool
     * @return ConnectorConnectionPool created ConnectorConnectionPool instance
     */

    public static ConnectorConnectionPool createDefaultConnectorPoolObject(
            String poolName, String rarName) {

        ConnectorConnectionPool connectorPoolObj =
                new ConnectorConnectionPool(poolName);
        connectorPoolObj.setMaxPoolSize("20");
        connectorPoolObj.setSteadyPoolSize("10");
        connectorPoolObj.setMaxWaitTimeInMillis("7889");
        connectorPoolObj.setIdleTimeoutInSeconds("789");
        connectorPoolObj.setPoolResizeQuantity("2");
        connectorPoolObj.setFailAllConnections(false);
        connectorPoolObj.setMatchConnections(true); //always


        setDefaultAdvancedPoolAttributes(connectorPoolObj);

        try {
            connectorPoolObj.setTransactionSupport(getTransactionSupportFromRaXml(rarName));
        } catch (Exception ex) {
            _logger.fine("error in setting txSupport");
        }
        return connectorPoolObj;
    }

    /**
     * Sets default values for advanced pool properties<br>
     * @param connectorPoolObj Connector Connection Pool
     */
    private static void setDefaultAdvancedPoolAttributes(ConnectorConnectionPool connectorPoolObj) {
        //Other advanced attributes like connection-leak-reclaim, lazy-connection-enlistment,
        //lazy-connection-association, associate-with-thread are boolean values which are not required
        //to be explicitly initialized to default values.
        connectorPoolObj.setMaxConnectionUsage(ConnectorConnectionPool.DEFAULT_MAX_CONNECTION_USAGE);
        connectorPoolObj.setConnectionLeakTracingTimeout(ConnectorConnectionPool.DEFAULT_LEAK_TIMEOUT);
        connectorPoolObj.setConCreationRetryAttempts(ConnectorConnectionPool.DEFAULT_CON_CREATION_RETRY_ATTEMPTS);
        connectorPoolObj.setConCreationRetryInterval(ConnectorConnectionPool.DEFAULT_CON_CREATION_RETRY_INTERVAL);
        connectorPoolObj.setValidateAtmostOncePeriod(ConnectorConnectionPool.DEFAULT_VALIDATE_ATMOST_ONCE_PERIOD);
    }

    /**
     * Creates ConnectorConnectionPool object pertaining to the pool props
     * mentioned in the sun-ra/xml i.e it represents the pool mentioned in the
     * sun-ra.xm.
     *
     * @param poolName Name of the pool
     * @param desc     ConnectorDescriptor which represent ra.xml and sun-ra.xml.
     * @return ConnectorConnectionPool created ConnectorConnectionPool instance
     */
    public static ConnectorConnectionPool createSunRaConnectorPoolObject(
            String poolName, ConnectorDescriptor desc, String rarName) {

        ConnectorConnectionPool connectorPoolObj =
                new ConnectorConnectionPool(poolName);
        SunConnector sundesc = desc.getSunDescriptor();
        ResourceAdapter sunRAXML = sundesc.getResourceAdapter();

        connectorPoolObj.setMaxPoolSize(
                (String) sunRAXML.getValue(ResourceAdapter.MAX_POOL_SIZE));
        connectorPoolObj.setSteadyPoolSize(
                (String) sunRAXML.getValue(ResourceAdapter.STEADY_POOL_SIZE));
        connectorPoolObj.setMaxWaitTimeInMillis((String) sunRAXML.getValue(
                ResourceAdapter.MAX_WAIT_TIME_IN_MILLIS));
        connectorPoolObj.setIdleTimeoutInSeconds((String) sunRAXML.getValue(
                ResourceAdapter.IDLE_TIMEOUT_IN_SECONDS));
        connectorPoolObj.setPoolResizeQuantity((String) "2");
        connectorPoolObj.setFailAllConnections(false);
        connectorPoolObj.setMatchConnections(true); //always


        setDefaultAdvancedPoolAttributes(connectorPoolObj);


        try {
            connectorPoolObj.setTransactionSupport(getTransactionSupportFromRaXml(rarName));
        } catch (Exception ex) {
            _logger.fine("error in setting txSupport");
        }

        boolean validateAtmostEveryIdleSecs = false;

        //For SunRAPool, get the value of system property VALIDATE_ATMOST_EVERY_IDLE_SECS.
        if (validateAtmostEveryIdleSecsProperty != null && validateAtmostEveryIdleSecsProperty.equalsIgnoreCase("TRUE")) {
            validateAtmostEveryIdleSecs = true;
            _logger.log(Level.FINE, "CCP.ValidateAtmostEveryIdleSecs.Set", poolName);
        }
        connectorPoolObj.setValidateAtmostEveryIdleSecs(validateAtmostEveryIdleSecs);

        return connectorPoolObj;
    }

    /**
     * Return the integer representation of the transaction-support attribure
     *
     * @param txSupport one of <br>
     * <ul>
     *  <li>NoTransaction</li>
     *  <li>LocalTransaction</li>
     *  <li>XATransaction</li>
     * </ul>
     * @return one of
     * <ul>
     *  <li>ConnectorConstants.UNDEFINED_TRANSACTION_INT</li>
     *  <li>ConnectorConstants.NO_TRANSACTION_INT</li>
     *  <li>ConnectorConstants.LOCAL_TRANSACTION_INT</li>
     *  <li>ConnectorConstants.XA_TRANSACTION_INT</li>
     * </ul>
     *
     */
    public static int parseTransactionSupportString( String txSupport ) {
        int txSupportIntVal = ConnectorConstants.UNDEFINED_TRANSACTION_INT;

	
	if ( txSupport == null ) {
	    if (_logger.isLoggable(Level.FINE)) {
	        _logger.fine("txSupport is null");
	    }
	    return txSupportIntVal;
	}
	
        if (_logger.isLoggable(Level.FINE) ) {
	    _logger.fine("parseTransactionSupportString: passed in " + 
	        "txSupport =>" + txSupport);
	}
	
        if (ConnectorConstants.NO_TRANSACTION_TX_SUPPORT_STRING.equals(txSupport) ) {
	    txSupportIntVal = ConnectorConstants.NO_TRANSACTION_INT;
	} else if (ConnectorConstants.LOCAL_TRANSACTION_TX_SUPPORT_STRING.equals(txSupport)) {
	    txSupportIntVal = ConnectorConstants.LOCAL_TRANSACTION_INT;
	} else if (ConnectorConstants.XA_TRANSACTION_TX_SUPPORT_STRING.equals(txSupport)) {
	    txSupportIntVal = ConnectorConstants.XA_TRANSACTION_INT;
	}

	return txSupportIntVal;
    }
    
    /**
     * A utility method to map TransactionSupport ints as represented
     * in ConnectorConstants to the new TransactionSupport enum
     */
    public static TransactionSupport getTransactionSupport(int ts) {
    	switch (ts) {
			case ConnectorConstants.NO_TRANSACTION_INT:
				return TransactionSupport.NO_TRANSACTION;
			case ConnectorConstants.LOCAL_TRANSACTION_INT:
				return TransactionSupport.LOCAL_TRANSACTION;
			case ConnectorConstants.XA_TRANSACTION_INT:
				return TransactionSupport.XA_TRANSACTION;
		}
    	return null;
    }

    public static String getValueFromMCF(String prop, String poolName,
	        ManagedConnectionFactory mcf) {
        String result = null;
        try {
            Method m = mcf.getClass().getMethod("get"+prop, (java.lang.Class[])null);
            result = (String) m.invoke(mcf,(java.lang.Object[])null);
        } catch (Throwable t) {
            _logger.log(Level.FINE, t.getMessage(), t);
        }
        
	return result == null ? "" : result;
    }
    
    public static Subject createSubject(ManagedConnectionFactory mcf, 
            final ResourcePrincipal prin) { 
                                                                     
       final Subject tempSubject = new Subject();
        if( prin != null) {
            String password = prin.getPassword();
            if (password != null) {
                final PasswordCredential pc =
                  new PasswordCredential(prin.getName(),
                  password.toCharArray());
                pc.setManagedConnectionFactory(mcf);
                AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    tempSubject.getPrincipals().add(prin);
                    tempSubject.getPrivateCredentials().add(pc);
                    return null;
                }
                });
           }
        }
        return tempSubject;
    }


 
      /**
      * Validates and sets the values for LazyConnectionEnlistment and LazyConnectionAssociation.
      * @param lazyAssocString Property value
      * @param adminPool  Config Bean
      * @param conConnPool Connector Connection Pool
      */
     public static void setLazyEnlistAndLazyAssocProperties(String lazyAssocString, ConfigBean adminPool, ConnectorConnectionPool conConnPool){
 
         //Get LazyEnlistment value.
         //To set LazyAssoc to true, LazyEnlist also need to be true.
         //If LazyAssoc is true and LazyEnlist is not set, set it to true.
         //If LazyAssoc is true and LazyEnlist is false, throw exception.
 
         ElementProperty[] o = (ElementProperty[])adminPool.getValues(ELEMENT_PROPERTY);
         if (o == null) return ;
 
         ElementProperty lazyEnlistElement = null;
 
          for (int i=0; i < o.length; i++) {
              if(o[i].getAttributeValue(Common.convertName(ServerTags.NAME)).equalsIgnoreCase("LAZYCONNECTIONENLISTMENT")) {
                  lazyEnlistElement = o[i];
                  break;
              }
          }
 
             boolean lazyAssoc = toBoolean( lazyAssocString, false );
 
             if(lazyEnlistElement != null){
 
                 boolean lazyEnlist = toBoolean(lazyEnlistElement.getValue(),false);
                 if(lazyAssoc){
                     if(lazyEnlist){
                         conConnPool.setLazyConnectionAssoc( true) ;
                         conConnPool.setLazyConnectionEnlist( true);
                     }else{
                          _logger.log(Level.SEVERE,"conn_pool_obj_utils.lazy_enlist-lazy_assoc-invalid-combination",conConnPool.getName());
                         String i18nMsg = localStrings.getString(
	        "cpou.lazy_enlist-lazy_assoc-invalid-combination");
                         throw new RuntimeException(i18nMsg + conConnPool.getName());
                     }
                 }else{
                     conConnPool.setLazyConnectionAssoc(false);
                 }
             }else{
                 if(lazyAssoc){
                     conConnPool.setLazyConnectionAssoc( true) ;
                     conConnPool.setLazyConnectionEnlist( true);
                 }else{
                     conConnPool.setLazyConnectionAssoc( false) ;
                 }
             }
 
 }
 
     private static boolean toBoolean( Object prop, boolean defaultVal ) {
         if ( prop == null ) {
             return defaultVal;
         }
         return Boolean.valueOf(((String) prop).toLowerCase());
      }
    
    public static boolean isPoolSystemPool(com.sun.enterprise.config.serverbeans.ConnectorConnectionPool 
            domainCcp){
    	String poolName = domainCcp.getName();
    	return isPoolSystemPool(poolName);
    }
        
    public static boolean isPoolSystemPool(String poolName){
        Pattern pattern = Pattern.compile("#");
        Matcher matcher = pattern.matcher(poolName);
        
        // If the pool name does not contain #, return false
        if (!matcher.find()){
        	return false;
        }
        
        matcher.reset();
        
        String moduleNameFromPoolName = null;
        int matchCount=0;
        
        while (matcher.find()){
        	matchCount++;
        	int patternStart = matcher.start();
        	moduleNameFromPoolName = poolName.substring(0,patternStart);
        }
        
        // If pool name contains more than 2 #, return false as the 
        // default system pool will have exacly one # for a standalone rar
        // and exactly two #s for an embedded rar
        ResourcesUtil resUtil = ResourcesUtil.createInstance();
        switch (matchCount){
        
        case 1: 
        	if (resUtil.belongToStandAloneRar(moduleNameFromPoolName))
        		return true;
        case 2: 
        	if (resUtil.belongToEmbeddedRar(moduleNameFromPoolName))
        		return true;
        default: 
        	return false;
        }
    }

    public static int getTransactionSupportFromRaXml( String rarName ) throws
            ConnectorRuntimeException
    {
        String txSupport =
            ConnectorRuntime.getRuntime().getConnectorDescriptor( rarName ).
            getOutboundResourceAdapter().getTransSupport() ;

        return parseTransactionSupportString( txSupport );
    }

}
