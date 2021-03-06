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

/* CVS information
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/http/HttpUrlConnector.java,v 1.5 2007/02/24 00:41:35 ne110415 Exp $
 * $Revision: 1.5 $
 * $Date: 2007/02/24 00:41:35 $
 */

package com.sun.enterprise.admin.jmx.remote.http;

import com.sun.enterprise.admin.jmx.remote.https.SunOneBasicHostNameVerifier;
import com.sun.enterprise.admin.jmx.remote.https.SunOneBasicX509TrustManager;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.logging.Logger;
import java.util.Map;
import javax.management.remote.JMXServiceURL;

import com.sun.enterprise.admin.jmx.remote.DefaultConfiguration;
import com.sun.enterprise.admin.jmx.remote.UrlConnector;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

/** A Concrete implementation of UrlConnector that uses {@link java.net.URLConnection.openConnection} and
 * HttpUrlConnection to communicate with the server. 
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
 */

public class HttpUrlConnector extends UrlConnector {

    private HostnameVerifier hv = null;
    private X509TrustManager[] tms = null;
    private X509KeyManager[] kms = null;
    private SSLSocketFactory ssf = null;
    
    private static final Logger logger = Logger.getLogger(
        DefaultConfiguration.JMXCONNECTOR_LOGGER);/*, 
        DefaultConfiguration.LOGGER_RESOURCE_BUNDLE_NAME );*/

    /** Creates a new instance of HttpUrlConnector */
    
    public HttpUrlConnector(JMXServiceURL serviceUrl, Map environment) {
        super(serviceUrl, environment);
        hv = (HostnameVerifier)environment.get(
                DefaultConfiguration.HOSTNAME_VERIFIER_PROPERTY_NAME);
        if (hv == null) 
            hv = new SunOneBasicHostNameVerifier(serviceUrl.getHost());

         //fetching any custom SSLSocketFactory passed through environment
        ssf = (SSLSocketFactory)environment.get(
                DefaultConfiguration.SSL_SOCKET_FACTORY);
        
        //No custom SSLScoketFactory passed. So now fetch the X509 based managers
        //to get the SSLSocketFactory configured using SSLContext
        if (ssf == null) {
            //fetching any trustmanagers passed through environment - default is 
            //SunOneBasicX509TrustManager
            Object tmgr = environment.get(DefaultConfiguration.TRUST_MANAGER_PROPERTY_NAME);
            if (tmgr instanceof X509TrustManager[]) 
                tms = (X509TrustManager[])tmgr;
            else if (tmgr instanceof X509TrustManager)
                tms = new X509TrustManager[] { (X509TrustManager)tmgr };
            else if (tmgr == null)
                tms = new X509TrustManager[] { new SunOneBasicX509TrustManager(
                                                    serviceUrl, environment)};
            

            //fetching any keymanagers passed through environment - no defaults
            Object kmgr = environment.get(DefaultConfiguration.KEY_MANAGER_PROPERTY_NAME);
            if (kmgr instanceof X509KeyManager[]) 
                kms = (X509KeyManager[])kmgr;
            else if (kmgr instanceof X509KeyManager) 
                kms = new X509KeyManager[] { (X509KeyManager)kmgr };
        }

        initialize();
    }
    
    protected void validateJmxServiceUrl() throws RuntimeException {
        //additional validation
    }
    
    protected void validateEnvironment() throws RuntimeException {
        super.validateEnvironment();
    }

    private void initialize() {
        if (ssf == null) {
            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("SSLv3");
                sslContext.init(kms, tms, new SecureRandom());
            } catch(GeneralSecurityException e) {
                throw new RuntimeException(e);
            }

            if( sslContext != null ) 
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            
        } else HttpsURLConnection.setDefaultSSLSocketFactory(ssf);
        
        HttpsURLConnection.setDefaultHostnameVerifier( hv );
    }    
}