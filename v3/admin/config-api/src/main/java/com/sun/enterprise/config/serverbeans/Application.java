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

import java.beans.PropertyVetoException;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.glassfish.api.admin.config.Property;
import org.glassfish.api.admin.config.PropertyBag;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.api.amx.AMXConfigInfo;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.quality.ToDo;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;

import javax.validation.constraints.NotNull;

/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "description",
    "engine",
    "property"
}) */
@AMXConfigInfo( amxInterfaceName="com.sun.appserv.management.config.ApplicationConfig")
@Configured
@RestRedirects(
        {
          @RestRedirect(opType= RestRedirect.OpType.DELETE, commandName="undeploy"),
          @RestRedirect(opType= RestRedirect.OpType.POST, commandName = "redeploy")
        }
)
public interface Application extends ConfigBeanProxy, Injectable, Named, PropertyBag {

    /**
     * Gets the value of the contextRoot property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    String getContextRoot();

    /**
     * Sets the value of the contextRoot property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setContextRoot(String value) throws PropertyVetoException;

    /**
     * Gets the value of the location property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    String getLocation();

    /**
     * Sets the value of the location property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setLocation(String value) throws PropertyVetoException;

    /**
     * Gets the value of the objectType property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getObjectType();

    /**
     * Sets the value of the objectType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setObjectType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the enabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(dataType=Boolean.class)
    String getEnabled();

    /**
     * Sets the value of the enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the libraries property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getLibraries();

    /**
     * Sets the value of the libraries property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setLibraries(String value) throws PropertyVetoException;

    /**
     * Gets the value of the availabilityEnabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(dataType=Boolean.class)
    String getAvailabilityEnabled();

    /**
     * Sets the value of the availabilityEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAvailabilityEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the directoryDeployed property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(dataType=Boolean.class)
    String getDirectoryDeployed();

    /**
     * Sets the value of the directoryDeployed property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDirectoryDeployed(String value) throws PropertyVetoException;

    /**
     * Gets the value of the description property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getDescription();

    /**
     * Sets the value of the description property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDescription(String value) throws PropertyVetoException;


    @Element("*")
    List<Module> getModule();
    
    /**
     * Gets the value of the engine property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the engine property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEngine().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Engine }
     */
    @Element(required = true)
    List<Engine> getEngine();

    /**
     * Gets the value of the webServiceEndpoint property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the webServiceEndpoint property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWebServiceEndpoint().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link WebServiceEndpoint }
     */
    @Element
    List<WebServiceEndpoint> getWebServiceEndpoint();

    /**
     * Gets the applicationConfig children.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the applicationConfig children.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getApplicationConfig().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link ApplicationConfig }
     */
    @Element("*")
    List<ApplicationConfig> getApplicationConfigs();

    /**
     * Retrieves the single ApplicationConfig object for the given type,
     * if any exists.
     * @param type the type of the application config of interest
     * @return ApplicationConfig for the specified type; null if none exists
     */
    @DuckTyped
    <T extends ApplicationConfig> T getApplicationConfig(Class<T> type);

    @DuckTyped
    Module getModule(String moduleName);

    @DuckTyped
    Properties getDeployProperties();

    @DuckTyped
    DeployCommandParameters getDeployParameters(ApplicationRef appRef);

    @DuckTyped
    Map<String, Properties> getModulePropertiesMap();

    class Duck {
        public static <T extends ApplicationConfig> T getApplicationConfig(Application me, Class<T> type) {
            return getApplicationConfig(me.getApplicationConfigs(), type);
        }

        public static <T extends ApplicationConfig> T getApplicationConfig(
                List<ApplicationConfig> candidates,
                Class<T> type) {
            for (ApplicationConfig ac : candidates) {
                if (type.isInstance(ac)) {
                    return (T) ac;
                }
            }
            return null;
        }

        public static Module getModule(Application instance, String name) {
            for (Module module : instance.getModule()) {
                if (module.getName().equals(name)) {
                    return module;
                }
            }
            return null;
        }

        public static Properties getDeployProperties(Application instance) {
            Properties deploymentProps = new Properties();
            for (Property prop : instance.getProperty()) {
                deploymentProps.put(prop.getName(), prop.getValue());
            }
            deploymentProps.setProperty(ServerTags.OBJECT_TYPE,
                instance.getObjectType());
            if (instance.getContextRoot() != null) {
                deploymentProps.setProperty(ServerTags.CONTEXT_ROOT,
                    instance.getContextRoot());
            }
            return deploymentProps;            
        }

        public static DeployCommandParameters getDeployParameters(Application app, ApplicationRef appRef) {

            if (appRef==null) {
                throw new IllegalArgumentException("Null appRef passed");
            }
            DeployCommandParameters deploymentParams = new DeployCommandParameters(new File(app.getLocation()));
            deploymentParams.name = app.getName();
            deploymentParams.enabled = Boolean.parseBoolean(app.getEnabled());
            deploymentParams.contextroot = app.getContextRoot();
            deploymentParams.libraries = app.getLibraries();
            deploymentParams.virtualservers = appRef.getVirtualServers();
            return deploymentParams;
        }

        public static Map<String, Properties> getModulePropertiesMap(
            Application me) {
            Map<String, Properties> modulePropertiesMap = 
                new HashMap<String, Properties>();
            for (Module module: me.getModule()) {
                if (module.getProperty() != null) {
                    Properties moduleProps = new Properties();
                    for (Property property : module.getProperty()) {
                        moduleProps.put(property.getName(), 
                            property.getValue());
                    }
                    modulePropertiesMap.put(module.getName(), moduleProps);
                }
            }
            return modulePropertiesMap;
        }
    }
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
