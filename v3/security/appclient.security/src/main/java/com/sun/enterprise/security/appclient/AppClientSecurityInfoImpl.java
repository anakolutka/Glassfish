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

package com.sun.enterprise.security.appclient;

import com.sun.enterprise.security.J2EESecurityManager;
import com.sun.enterprise.security.SecurityServicesUtil;
import com.sun.enterprise.security.UsernamePasswordStore;
import com.sun.enterprise.security.appclient.integration.AppClientSecurityInfo;
import com.sun.enterprise.security.auth.login.LoginContextDriver;
import com.sun.enterprise.security.common.SecurityConstants;
import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.security.jmac.config.GFAuthConfigFactory;
import com.sun.enterprise.security.ssl.AppClientSSL;
import com.sun.enterprise.security.ssl.SSLUtils;
import com.sun.logging.LogDomains;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.config.AuthConfigFactory;
import org.glassfish.appclient.client.acc.config.ClientContainer;
import org.glassfish.appclient.client.acc.config.Security;
import org.glassfish.appclient.client.acc.config.Ssl;
import org.glassfish.appclient.client.acc.config.TargetServer;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Kumar
 */

@Service
public class AppClientSecurityInfoImpl implements AppClientSecurityInfo {

    private static Logger _logger=null;
    static {
        _logger=LogDomains.getLogger(AppClientSecurityInfoImpl.class, LogDomains.SECURITY_LOGGER);
    }
    
     private static final String DEFAULT_PARSER_CLASS =
        "com.sun.enterprise.security.appclient.ConfigXMLParser";
     
    private ClientContainer clientContainer;
    private CallbackHandler callbackHandler;
    private CredentialType  appclientCredentialType;
    boolean isJWS;
    
    
    @Inject
    protected SSLUtils sslUtils;
    @Inject
    private SecurityServicesUtil secServUtil; 
    @Inject
    private Util util;
    
    
    public void initializeSecurity(
            ClientContainer container, CallbackHandler handler, 
            CredentialType credType, String username, 
            String password, boolean isJWS) {
       
           /* security init */
        this.isJWS = isJWS;
        this.appclientCredentialType = credType;
        this.callbackHandler = handler;
        this.clientContainer = container;
        
        SecurityManager secMgr = System.getSecurityManager();
        if (!isJWS && secMgr != null &&
                !(J2EESecurityManager.class.equals(secMgr.getClass()))) {
            J2EESecurityManager mgr = new J2EESecurityManager();
            System.setSecurityManager(mgr);
        }
        if (_logger.isLoggable(Level.INFO)) {
            if (secMgr != null) {
                _logger.info("acc.secmgron");
            } else {
                _logger.info("acc.secmgroff");
            }
        }

        //set the parser to ConfigXMLParser
        System.setProperty("config.parser", DEFAULT_PARSER_CLASS);
	try {
	    /* setup jsr 196 factory
	     * define default factory if it is not already defined
	     */
	    String defaultFactory = java.security.Security.getProperty
		(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY);
	    if (defaultFactory == null) {
		java.security.Security.setProperty
		    (AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY,
		     GFAuthConfigFactory.class.getName());
	    }

	} catch (Exception e) {
	    _logger.log(Level.WARNING, "main.jmac_default_factory");
	}

        //TODO:V3 LoginContextDriver has a static variable dependency on AuditManager
        //And since LoginContextDriver has too many static methods that use AuditManager
        //we have to make this workaround here.
        LoginContextDriver.AUDIT_MANAGER = secServUtil.getAuditManager();

        secServUtil.initSecureSeed();

        setSSLData(container);
        if (username != null || password != null) {
            UsernamePasswordStore.set(username, password);
        }

        secServUtil.setCallbackHandler(callbackHandler);
        util.setCallbackHandler(callbackHandler);
    }

    public int getCredentialEncoding(CredentialType type) {
        switch(type) {
            case USERNAME_PASSWORD :
                return SecurityConstants.USERNAME_PASSWORD;
            case CERTIFICATE :
                return SecurityConstants.CERTIFICATE;
            case ALL :
                return SecurityConstants.ALL;
            default :
                throw new RuntimeException("Unknown CredentialType");
        }
        
    }

    public Subject doClientLogin(CredentialType credType) {
        return LoginContextDriver.doClientLogin(this.getCredentialEncoding(credType), callbackHandler);
    }
    
    private AppClientSSL convert(Ssl ssl) {
        AppClientSSL appSSL = new AppClientSSL();
        appSSL.setCertNickname(ssl.getCertNickname());
        //appSSL.setClientAuthEnabled(ssl.isClientAuthEnabled());
        appSSL.setSsl2Ciphers(ssl.getSsl2Ciphers());
        appSSL.setSsl2Enabled(ssl.isSsl2Enabled());
        appSSL.setSsl3Enabled(ssl.isSsl3Enabled());
        appSSL.setSsl3TlsCiphers(ssl.getSsl3TlsCiphers());
        appSSL.setTlsEnabled(ssl.isTlsEnabled());
        appSSL.setTlsRollbackEnabled(ssl.isTlsRollbackEnabled());
        
        return appSSL;
    }
    
    private void setSSLData(ClientContainer cc) {
        try {
            List<TargetServer> tServers = cc.getTargetServer();
            // Set the SSL related properties for ORB
            TargetServer tServer = tServers.get(0);
            // TargetServer is required.
	    //temp solution to target-server+ change in DTD
            // assuming that multiple servers can be specified but only 1st
	    // first one will be used.
	    Security security = tServer.getSecurity();
	    if (security == null) {
		_logger.fine("No Security input set in ClientContainer.xml");
		// do nothing
		return;
	    }
	    Ssl ssl = security.getSsl();
	    if (ssl == null) {
		_logger.fine("No SSL input set in ClientContainer.xml");
		// do nothing
		return;
		
	    }
	    //XXX do not use NSS in this release
	    //CertDb   certDB  = security.getCertDb();
	    sslUtils.setAppclientSsl(convert(ssl));	
	} catch (Exception ex) {

        }
    }
}
