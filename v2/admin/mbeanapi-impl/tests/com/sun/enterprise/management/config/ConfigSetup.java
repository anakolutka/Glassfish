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
 
package com.sun.enterprise.management.config;

import java.util.Map;
import java.util.HashMap;

import com.sun.appserv.management.DomainRoot;

import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.StandaloneServerConfig;
import com.sun.appserv.management.config.DomainConfig;
import com.sun.appserv.management.config.ServerConfigKeys;


/**
 */
public final class ConfigSetup
{
    final DomainRoot    mDomainRoot;
    
    public static final String  TEST_SERVER_NAME    = "testServer";
    public static final String  TEST_CONFIG_NAME    = TEST_SERVER_NAME + "-config";
    
        public
    ConfigSetup( final DomainRoot domainRoot )
    {
        mDomainRoot = domainRoot;
    }
    
        public DomainConfig
    getDomainConfig()
    {
        return mDomainRoot.getDomainConfig();
    }
    
        public ConfigConfig
    createConfig( final String name)
    {
        final Map<String,String>    options = new HashMap<String,String>();
        
        final ConfigConfig  config =
            getDomainConfig().createConfigConfig( name, options );
            
        return config;
    }
    
        public boolean
    removeConfig( final String name)
    {
       boolean exists = getDomainConfig().getConfigConfigMap().get( name ) != null;
        
        if ( exists )
        {
            getDomainConfig().removeConfigConfig( name );
        }
        
       return exists;
    }
    
        public void
    setupServerPorts(
        final Map<String,String> options,
        final int   basePort )
    {
        if ( basePort > 0 )
        {
            options.put( ServerConfigKeys.HTTP_LISTENER_1_PORT_KEY, "" + (basePort + 0) );
            options.put( ServerConfigKeys.HTTP_LISTENER_2_PORT_KEY, "" + (basePort + 1) );
            options.put( ServerConfigKeys.ORB_LISTENER_1_PORT_KEY, "" + (basePort + 2) );
            options.put( ServerConfigKeys.SSL_PORT_KEY, "" + (basePort + 3) );
            options.put( ServerConfigKeys.SSL_MUTUALAUTH_PORT_KEY, "" + (basePort + 4) );
            options.put( ServerConfigKeys.JMX_SYSTEM_CONNECTOR_PORT_KEY, "" + (basePort + 5) );
            options.put( ServerConfigKeys.JMS_PROVIDER_PORT_KEY, "" + (basePort + 6) );
            options.put( ServerConfigKeys.ADMIN_LISTENER_PORT_KEY, "" + (basePort + 7) );
        }
    }
    
        public StandaloneServerConfig
    createServer(
        final String    name,
        int             basePort,
        final String    nodeAgentName,
        final String    configName )
    {
        final Map<String,String>    options = new HashMap<String,String>();
        
        setupServerPorts( options, basePort );
        
        final StandaloneServerConfig  server =
            getDomainConfig().createStandaloneServerConfig(
                name, nodeAgentName, configName, options );
            
        return server;
    }
    

        public boolean
    removeServer( final String name )
    {
        boolean exists = getDomainConfig().getStandaloneServerConfigMap().get( name ) != null;

        if ( exists )
        {
            getDomainConfig().removeStandaloneServerConfig( name );
        }

        return exists;
    }
}






