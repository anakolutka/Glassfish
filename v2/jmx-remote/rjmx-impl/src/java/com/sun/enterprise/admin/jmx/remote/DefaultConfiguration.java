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
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/DefaultConfiguration.java,v 1.6 2007/03/28 02:37:31 ne110415 Exp $
 * $Revision: 1.6 $
 * $Date: 2007/03/28 02:37:31 $
*/

package com.sun.enterprise.admin.jmx.remote;
/** 
 * A class that holds the default information for this implementation. Place holder for defaults.
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
*/

public final class DefaultConfiguration {
    public static final String ADMIN_USER_ENV_PROPERTY_NAME		= "USER";
    public static final String ADMIN_PASSWORD_ENV_PROPERTY_NAME		= "PASSWORD";
            
    //Class instance of an instance of X509TrustManager (defaults to DEFAULT_TRUST_MANAGER)
    public static final String TRUST_MANAGER_PROPERTY_NAME              = "TRUST_MANAGER_KEY";
    
    //Class instance of an instance of X509KeyManager (no defaults)
    public static final String KEY_MANAGER_PROPERTY_NAME                = "KEYMANAGER_KEY";
    
    //Class instance of a SSLSocketFactory(defaults to the configured ssl socket 
    //factory obtained through SSLContext)SunOneB
    public static final String SSL_SOCKET_FACTORY                       = "SSL_SOCKET_FACTORY";

    //Class instance of an instance of HostnameVerifier (defaults to DEFAULT_HOST_NAME_VERIFIER)
    public static final String HOSTNAME_VERIFIER_PROPERTY_NAME          = "HOSTNAME_VERIFIER_KEY";

    //Class instance of an instance of IStringManager (defaults to com.sun.enterprise.admin.jmx.remote.StringManager)
    public static final String STRING_MANAGER_CLASS_NAME                = "STRING_MANAGER_CLASS_KEY";
    
    //Default JES Trust Manager class name
    public static final String DEFAULT_TRUST_MANAGER                    = "com.sun.enterprise.security.trustmanager.SunOneBasicX509TrustManager";
    
    public static final String SERVLET_CONTEXT_PROPERTY_NAME		= "com.sun.enterprise.as.context.root";
    public static final String HTTP_AUTH_PROPERTY_NAME			= "com.sun.enterprise.as.http.auth";
    
    public static final String DEFAULT_SERVLET_CONTEXT_ROOT		= "/web1/remotejmx"; /* This is to be in sync with the web.xml */
    public static final String DEFAULT_HTTP_AUTH_SCHEME			= "BASIC";
    public static final String DIGEST_HTTP_AUTH_SCHEME			= "Digest";
	
    public static final String S1_HTTP_PROTOCOL				= "s1ashttp";
    public static final String S1_HTTPS_PROTOCOL			= "s1ashttps";
/* BEGIN -- S1WS_MOD */
    public static final String NOTIF_ENABLED_PROPERTY_NAME      = "com.sun.jmx.remote.http.notification.enabled";
    public static final String NOTIF_BUFSIZ_PROPERTY_NAME      = "com.sun.jmx.remote.http.notification.bufsize";
    public static final String MBEANSERVER_FACTORY_PROPERTY_NAME      = "com.sun.jmx.remote.http.MBeanServerFactory.class";
    public static final String REDIRECTION           = "com.sun.jmx.remote.http.redirect.enable";
    public static final String REDIRECTION_DETECTION = "com.sun.jmx.remote.http.redirect.detect";
    
    public static final int NOTIF_WAIT_INTERVAL = 10000; // in millis
    public static final int NOTIF_MIN_BUFSIZ = 10;
    public static final int NOTIF_MAX_BUFSIZ = 50;
    public static final int NOTIF_CONNECT_TIMEOUT = 2000; // in millis
    public static final int NOTIF_TOTAL_RECONNECTS_BEFORE_TIMEOUT = 3;

    public static final String NOTIF_MGR_PATHINFO = "/NotificationManager";
    public static final String NOTIF_ID_PARAM = "id";
    public static final String NOTIF_CMD_PARAM = "cmd";
    public static final String NOTIF_CMD_CLOSE = "close";
/* END -- S1WS_MOD */
    
    public static final String JMXCONNECTOR_LOGGER =  "com.sun.logging.enterprise.system.admin.jmx.connector";
    public static final String LOGGER_RESOURCE_BUNDLE_NAME = ".LogStrings";
    
    private DefaultConfiguration() {
    }
}
