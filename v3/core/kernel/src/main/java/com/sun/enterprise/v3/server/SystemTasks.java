/*
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
package com.sun.enterprise.v3.server;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import java.util.Collections;
import java.util.List;
import org.glassfish.internal.api.Init;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.util.net.NetUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.config.support.TranslatedConfigView;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Init service to take care of vm related tasks.
 *
 * @author Jerome Dochez
 * @author Byron Nevins
 */
// TODO: eventually use CageBuilder so that this gets triggered when JavaConfig enters Habitat.
@Service
public class SystemTasks implements Init, PostConstruct {

    // in embedded environment, JavaConfig is pointless, so make this optional
    @Inject(optional = true)
    JavaConfig javaConfig;
   
    @Inject
    Server server;
    
    @Inject
    Domain domain;
    
    Logger _logger = Logger.getAnonymousLogger();

    public void postConstruct() {
        setSystemPropertiesFromEnv();
        setSystemPropertiesFromDomainXml();
        resolveJavaConfig();
    }

    /*
     * Here is where we make the change Post-TP2 to *not* use JVM System Properties
     */
    private void setSystemProperty(String name, String value) {
        System.setProperty(name, value);
    }

    private void setSystemPropertiesFromEnv() {
        // adding our version of some system properties.
        setSystemProperty(SystemPropertyConstants.JAVA_ROOT_PROPERTY, System.getProperty("java.home"));

        String hostname = "localhost";
        try {
            // canonical name checks to make sure host is proper
            hostname = NetUtils.getCanonicalHostName();
        }
        catch (Exception ex) {
            if (_logger != null)
                _logger.log(Level.SEVERE, "cannot determine host name, will use localhost exclusively", ex);
        }
        if (_logger != null)
            setSystemProperty(SystemPropertyConstants.HOST_NAME_PROPERTY, hostname);
    }

    private void setSystemPropertiesFromDomainXml() {
        // precedence order from high to low
        // 1. server
        // 2. server-config
        // so we need to add System Properties in *reverse order* to get the 
        // right precedence.

        List<SystemProperty> serverSPList = server.getSystemProperty();
        List<SystemProperty> configSPList = getConfigSystemProperties();

        setSystemProperties(configSPList);
        setSystemProperties(serverSPList);
    }

    private List<SystemProperty> getConfigSystemProperties() {
        try {
            String configName = server.getConfigRef();
            Configs configs = domain.getConfigs();
            List<Config> configsList = configs.getConfig();
            Config config = null;

            for (Config c : configsList) {
                if (c.getName().equals(configName)) {
                    config = c;
                    break;
                }
            }
            
            return config.getSystemProperty();
        }
        catch(Exception e) {  //possible NPE if domain.xml has issues!
            return Collections.emptyList();
        }
    }

    private void resolveJavaConfig() {
        if(javaConfig!=null) {
            Pattern p = Pattern.compile("-D([^=]*)=(.*)");
            for (String jvmOption : javaConfig.getJvmOptions()) {
                Matcher m = p.matcher(jvmOption);
                if (m.matches()) {
                    setSystemProperty(m.group(1), TranslatedConfigView.getTranslatedValue(m.group(2)).toString());
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("Setting " + m.group(1) + " = " + TranslatedConfigView.getTranslatedValue(m.group(2)));
                    }
                }
            }
        }
    }

    private void setSystemProperties(List<SystemProperty> spList) {
        for (SystemProperty sp : spList) {
            String name = sp.getName();
            String value = sp.getValue();
            if(ok(name)) {
                setSystemProperty(name,value);
            }
        }
    }
    
    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }
}

