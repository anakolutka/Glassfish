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



package com.sun.enterprise.config.serverbeans;

import org.glassfish.api.amx.AMXCreatorInfo;
import org.glassfish.api.admin.config.Named;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "lifecycleModuleOrJ2EeApplicationOrEjbModuleOrWebModuleOrConnectorModuleOrAppclientModuleOrMbeanOrExtensionModule"
}) */
@org.glassfish.api.amx.AMXConfigInfo( amxInterfaceName="com.sun.appserv.management.config.ApplicationsConfig", omitAsAncestorInChildObjectName=true, singleton=true)

// general solution needed; this is intermediate solution
@AMXCreatorInfo( creatables={Application.class})
@Configured
public interface Applications extends ConfigBeanProxy, Injectable  {

    /**
     * Gets the value of the lifecycleModuleOrJ2EeApplicationOrEjbModuleOrWebModuleOrConnectorModuleOrAppclientModuleOrMbeanOrExtensionModuleorApplication property.
     * Objects of the following type(s) are allowed in the list
     * {@link LifecycleModule }
     * {@link J2eeApplication }
     * {@link EjbModule }
     * {@link WebModule }
     * {@link ConnectorModule }
     * {@link AppclientModule }
     * {@link Mbean }
     * {@link ExtensionModule }
     * {@link Application }
     */             
    @Element("*")
    public List<Named> getModules();     
            
    /**
     * Gets a subset of {@link #getModules()} that has the given type.
     */
    @DuckTyped
    <T> List<T> getModules(Class<T> type);
    
    @DuckTyped
    <T> T getModule(Class<T> type, String moduleID);

    @DuckTyped
    List<Application> getApplications();
    
    public class Duck {
        public static <T> List<T> getModules(Applications apps, Class<T> type) {
            List<T> modules = new ArrayList<T>();
            for (Object module : apps.getModules()) {
                if (type.isInstance(module)) {
                    modules.add(type.cast(module));
                }
            }
            return modules;
        }
                                                                                              
        public static <T> T getModule(Applications apps, Class<T> type, String moduleID) {
            if (moduleID == null) {
                return null;
            }

            for (Named module : apps.getModules())
                if (type.isInstance(module) && module.getName().equals(moduleID))
                    return type.cast(module);

            return null;

        }

        public static List<Application> getApplications(Applications apps) {
            return getModules(apps, Application.class);
        }
    }
}
