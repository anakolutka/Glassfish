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
package com.sun.enterprise.webservice;

import com.sun.logging.LogDomains;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.config.ConfigContext;
import com.sun.enterprise.config.ConfigException;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.ServerHelper;
import com.sun.enterprise.config.serverbeans.HttpListener;
import com.sun.enterprise.server.ApplicationServer;
import java.util.logging.Level;

import java.util.logging.Logger;

/**
 * This class contains utility code used by the WS-TX system apps (such as
 * calculating the addresses of their ws endpoints).
 *
 * @author Ryan.Shoemaker@Sun.COM
 * @version $Revision: 1.2 $
 * @since 1.0
 */
public class WsTxUtils {

    private static Logger logger = 
        LogDomains.getLogger(LogDomains.EJB_LOGGER);
    
    /**
     * Get the hostname and port of the secure or non-secure http listener for the default
     * virtual server in this server instance.  The string representation will be of the
     * form 'hostname:port'.
     *
     * @param secure true if you want the secure port, false if you want the non-secure port
     * @return the 'hostname:port' combination or null if there were any errors calculating the address
     */
    public static String getDefaultVirtualServerHostAndPort(boolean secure) {
        final String host = getHostName();
        final String port = getPort(secure);
        if ((host == null) || (port == null)) {
            return null;
        }
        return host + ":" + port;
    }

    /**
     * Lookup the canonical host name of the system this server instance is running on.
     *
     * @return the canonical host name or null if there was an error retrieving it
     */
    private static String getHostName() {
        // this value is calculated from InetAddress.getCanonicalHostName when the AS is
        // installed.  asadmin then passes this value as a system property when the server
        // is started.
        return System.getProperty(SystemPropertyConstants.HOST_NAME_PROPERTY);
    }

    /**
     * Get the http/https port number for the default virtual server of this server instance.
     * <p/>
     * If the 'secure' parameter is true, then return the secure http listener port, otherwise
     * return the non-secure http listener port.
     *
     * @param secure true if you want the secure port, false if you want the non-secure port
     * @return the port or null if there was an error retrieving it.
     */
    private static String getPort(boolean secure) {
        try {
            ConfigContext configContext = ApplicationServer.getServerContext().getConfigContext();
            final String serverName = System.getProperty(SystemPropertyConstants.SERVER_NAME);
            Config config = ServerHelper.getConfigForServer(configContext, serverName);
            final HttpListener[] listeners = config.getHttpService().getHttpListener();
            for (HttpListener listener : listeners) {
                if ((secure) && (listener.isSecurityEnabled())) {
                    return listener.getPort();
                } else if ((!secure) && (!listener.isSecurityEnabled())) {
                    return listener.getPort();
                }
            }
        } catch (Throwable t) {
            // error condition handled in wsit code
            logger.log(Level.FINEST, "Exception occurred retrieving port " +
                "configuration for WSTX service", t);
        }
        return null;
    }
}

