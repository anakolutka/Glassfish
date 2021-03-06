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


package com.sun.appserv.management.config;

import java.util.Map;
import java.util.List;

import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.base.Container;

/**
 * Config AMX MBean for Web Service Endpoint's config
 * 
 * @since AppServer 9.0
 */
public interface WebServiceEndpointConfig extends NamedConfigElement, Container
{
    public static final String	J2EE_TYPE	= XTypes.WEB_SERVICE_ENDPOINT_CONFIG;

    /**
    * Get the web service name.
    */
    public String getName();

    /**
    * visibility of this endoint as a service in JBI
    */
    public boolean getJBIEnabled();

    /**
    * visibility of this endoint as a service in JBI
    */
    public void setJBIEnabled( boolean enabled );

    /**
    * Get the web service' monitoring level can be OFF, LOW or HIGH.
    @see ModuleMonitoringLevelValues
    */
    public String getMonitoringLevel();

    /**
    * Sets the web service' monitoring level can be OFF, LOW or HIGH.
    @see ModuleMonitoringLevelValues
    */
    public void setMonitoringLevel( String level );

    /**
    * Gets the web service' max history size. Number of statistics stored in
    * memory.
    */
    public String getMaxHistorySize();

    /**
    * Sets the web service' max history size. Number of statistics stored in
    * memory.
    *
    * @param maxSize max size of stored statistics in memory
    */
    public void setMaxHistorySize(String maxSize);
    
    /**
        @since Appserver 9.0
     */
    public Map<String,RegistryLocationConfig> getRegistryLocationConfigMap();
    
    
    /**
        @since Appserver 9.0
     */
    public RegistryLocationConfig createRegistryLocationConfig( String jndiName );
    
    /**
        @since Appserver 9.0
     */
    public void removeRegistryLocationConfig( String jndiName );
    

    /**
    * Get the transformation rules (not in any specific order) defined for this
    * end point.
    *
    * @return Map of transformation rules added to web service.
    */
    public Map<String,TransformationRuleConfig> getTransformationRuleConfigMap();
    /**
    * Get the transformation rules (in the same order as in domain.xml) 
    * defined for this end point.
    *
    * @return Map of transformation rules added to web service.
    */
    public List<TransformationRuleConfig> getTransformationRuleConfigList();


    /**
    * Removes the specified transformation rule for this end point.
    *
    * @param name Name of the transformation rule
    */
    public void removeTransformationRuleConfig(String name);

    /**
    * Creates a transformation rule for this web service.
    *
    * @param name  this transformation rule's name
    * @param ruleFileLocation File path location of this transformation rule
    * @param applyTo Operation(s) for which this
    * transformation rule is applied. Possible values are "request", "response"
    * or "both"
    * @param enabled If true transformation rule is applied
    *
    * @return TransformationRule new transformation rule with the
    * specified parameters
    */
    public TransformationRuleConfig createTransformationRuleConfig(
         String name,  String ruleFileLocation,boolean enabled, String applyTo,
         Map<String,String> reserved);
}
