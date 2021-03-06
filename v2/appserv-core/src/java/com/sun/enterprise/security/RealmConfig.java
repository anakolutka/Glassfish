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

package com.sun.enterprise.security;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import javax.security.auth.login.*;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.config.*;
import com.sun.enterprise.server.*;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.util.LocalStringManagerImpl;


/**
 * Process realm initial configuration.
 *
 * <P>This class contains S1AS-specific initialization code for Realms, used
 * by RealmManager.
 *
 *
 */

public class RealmConfig
{
    private static Logger logger =
        LogDomains.getLogger(LogDomains.SECURITY_LOGGER);

    
    public static void createRealms(String defaultRealm, AuthRealm[] realms) 
    {
        assert(realms != null);

        String goodRealm = null; // need at least one good realm

        for (int i=0; i < realms.length; i++) {

            AuthRealm aRealm = realms[i];
            String realmName = aRealm.getName();
            String realmClass = aRealm.getClassname();
            assert (realmName != null);
            assert (realmClass != null);

            try {
                ElementProperty[] realmProps =
                    aRealm.getElementProperty();

                Properties props = new Properties();

                for (int j=0; j < realmProps.length; j++) {
                    ElementProperty p = realmProps[j];
                    String name = p.getName();
                    String value = p.getValue();
                    props.setProperty(name, value);
                }
                Realm.instantiate(realmName, realmClass, props);

                logger.fine("Configured realm: " + realmName);

                if (goodRealm == null) {
                    goodRealm = realmName;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING,
                           "realmconfig.disable", realmName);
                logger.log(Level.WARNING, "security.exception", e);
            }
        }

        // done loading all realms, check that there is at least one
        // in place and that default is installed, or change default
        // to the first one loaded (arbitrarily).

        if (goodRealm == null) {
            logger.severe("realmconfig.nogood");

        } else {
            try {
                Realm def = Realm.getInstance(defaultRealm);
                if (def == null) {
                    defaultRealm = goodRealm;
                }
            } catch (Exception e) {
                defaultRealm = goodRealm;
            }
            Realm.setDefaultRealm(defaultRealm);
            logger.fine("Default realm is set to: "+ defaultRealm);
        }
    }
    
    /**
     * Load all configured realms from server.xml and initialize each
     * one.  Initialization is done by calling Realm.initialize() with
     * its name, class and properties.  The name of the default realm
     * is also saved in the Realm class for reference during server
     * operation.
     *
     * <P>This method superceeds the RI RealmManager.createRealms() method.
     *
     * */
    public static void createRealms()
    {
        
        try {
            logger.fine("Initializing configured realms.");
            
            ConfigContext configContext =
                ApplicationServer.getServerContext().getConfigContext();
            assert(configContext != null);

            Server configBean =
                ServerBeansFactory.getServerBean(configContext);
            assert(configBean != null);

            SecurityService securityBean =
                ServerBeansFactory.getSecurityServiceBean(configContext);
            assert(securityBean != null);

                                // grab default realm name
            String defaultRealm = securityBean.getDefaultRealm();

                                // get set of auth-realms and process each
            AuthRealm[] realms = securityBean.getAuthRealm();
            assert(realms != null);

            createRealms(defaultRealm, realms);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "realmconfig.nogood", e);

        }
    }



    
}
