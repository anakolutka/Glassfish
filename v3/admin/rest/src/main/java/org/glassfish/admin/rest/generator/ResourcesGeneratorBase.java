/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.admin.rest.generator;

import org.glassfish.admin.rest.Constants;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.api.admin.RestRedirects;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.DomDocument;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mitesh Meswani
 */
public abstract class ResourcesGeneratorBase implements ResourcesGenerator {

    private Set<String> alreadyGenerated = new HashSet<String>();

    @Override
    /**
     * Generate REST resource for a single config model.
     */
    public void generateSingle(ConfigModel model, DomDocument domDocument) {
        //processRedirectsAnnotation(model); // TODO need to extract info from RestRedirect Annotations

        String serverConfigName = getUnqualifiedTypeName(model.targetTypeName);
        String beanName = getBeanName(serverConfigName);
        String className = getClassName(beanName);

        if (alreadyGenerated(className)) {
            return;
        }

        String baseClassName = "TemplateResource";
        String resourcePath = null;

        if (beanName.equals("Domain")) {
            baseClassName = "org.glassfish.admin.rest.resources.GlassFishDomainResource";
            resourcePath  = "domain";
        } 

        ClassWriter classWriter = getClassWriter(className, baseClassName, resourcePath);

        generateCommandResources(beanName, classWriter);

        generateGetDeleteCommandMethod(beanName, classWriter);

        generateCustomResourceMapping(beanName, classWriter);

        for (String elementName : model.getElementNames()) {
            ConfigModel.Property childElement = model.getElement(elementName);
            if (childElement.isLeaf()) {
                if (childElement.isCollection()) {
                    //handle the CollectionLeaf config objects.
                    //JVM Options is an example of CollectionLeaf object.
                    String childResourceBeanName = getBeanName(elementName); 
                    String childResourceClassName = getClassName(childResourceBeanName);
                    classWriter.createGetChildResource(elementName, childResourceClassName);

                    //create resource class
                    generateCollectionLeafResource(childResourceBeanName);
                }
            } else {  // => !childElement.isLeaf()
                processNonLeafChildElement(elementName, childElement, domDocument, classWriter);
            }
        }

        classWriter.done();
    }

    public void generateList(ConfigModel model, DomDocument domDocument)  {

        String serverConfigName = getUnqualifiedTypeName(model.targetTypeName);
        String beanName = getBeanName(serverConfigName);
        String className = "List" + getClassName(beanName);

        if (alreadyGenerated(className)) return;

        ClassWriter classWriter = getClassWriter(className, "TemplateListOfResource", null);

        String keyAttributeName = getKeyAttributeName(model);
        String childResourceClassName = getClassName(beanName);
        classWriter.createGetChildResourceForListResources(keyAttributeName, childResourceClassName);
        generateCommandResources("List" + beanName, classWriter);

        generateGetPostCommandMethod("List" + beanName, classWriter);

        classWriter.done();

        generateSingle(model, domDocument);

    }

    private void generateCollectionLeafResource(String beanName) {
        String className = getClassName(beanName);

        if (alreadyGenerated(className)) return;

        ClassWriter classWriter = getClassWriter(className, "CollectionLeafResource", null);

        CollectionLeafMetaData metaData = configBeanToCollectionLeafMetaData.get(beanName);

        if (metaData != null) {
            if (metaData.postCommandName != null) {
                classWriter.createGetPostCommandForCollectionLeafResource(metaData.postCommandName);
            }

            if (metaData.deleteCommandName != null ) {
                classWriter.createGetDeleteCommandForCollectionLeafResource(metaData.deleteCommandName);
            }

            //display name method
            classWriter.createGetDisplayNameForCollectionLeafResource(metaData.displayName);
        }

        classWriter.done();

    }

    private void processNonLeafChildElement(String elementName, ConfigModel.Property element, DomDocument domDocument, ClassWriter classWriter) {
        ConfigModel.Node node = (ConfigModel.Node) element;
        ConfigModel model = node.getModel();
        String beanName = getUnqualifiedTypeName(model.targetTypeName);

        if (elementName.equals("*")) { //e.g. element Resource of Resources
            List<ConfigModel> childConfigModels = null;
            try {
                Class<?> subType = model.classLoaderHolder.get().loadClass(model.targetTypeName);
                childConfigModels = domDocument.getAllModelsImplementing(subType);
            } catch (ClassNotFoundException e) {
                throw new GeneratorException(e);
            }
            if (childConfigModels != null) {
                for (ConfigModel childConfigModel : childConfigModels) {
                    processNonLeafChildConfigModel(childConfigModel, element, domDocument, classWriter);
                }
            } else {
                //e.g. model.targetTypeName == "engine" (Application/Module/Engine)
                processNonLeafChildConfigModel(model, element, domDocument, classWriter);
            }
        } else { // => !childElement.isLeaf() && !elementName.equals("*")
            if (beanName.equals("Property")) {
                classWriter.createGetChildResource("property", "PropertiesBagResource");
            } else {
                String childResourceClassName = getClassName(beanName);
                if(element.isCollection()) {
                    childResourceClassName = "List" + childResourceClassName;
                }
                classWriter.createGetChildResource(model.getTagName(), childResourceClassName);
            }

            if (element.isCollection()) {
                generateList(model, domDocument);
            } else {
                generateSingle(model, domDocument);
            }
        }
    }

    /**
     * process given childConfigModel.
     * @param childConfigModel
     * @param childElement
     * @param domDocument
     * @param classWriter
     */
    private void processNonLeafChildConfigModel(ConfigModel childConfigModel, ConfigModel.Property childElement, DomDocument domDocument, ClassWriter classWriter) {
        String childResourceClassName = getClassName("List" + getUnqualifiedTypeName(childConfigModel.targetTypeName));
        String childPath = childConfigModel.getTagName();
        classWriter.createGetChildResource(childPath, childResourceClassName);
        if (childElement.isCollection()) {
            generateList(childConfigModel, domDocument);
        } else {
            //The code flow should never reach here. NonLeaf ChildElements are assumed to be collection typed that is why we generate childResource as
            //generateSingle(childConfigModel, domDocument);
        }
    }

    private void generateGetDeleteCommandMethod(String beanName, ClassWriter classWriter) {
        String commandName = configBeanToDELETECommand.get(beanName);
        if (commandName != null) {
            classWriter.createGetDeleteCommand(commandName);
        }
    }

    private void generateCustomResourceMapping(String beanName, ClassWriter classWriter) {
        for (int i = 0; i < configBeanCustomResources.length; i++) {
            String row[] = configBeanCustomResources[i];
            if (row[0].equals(beanName)) {
                classWriter.createCustomResourceMapping(row[1], row[2]);
            }
        }
    }

    void generateGetPostCommandMethod(String resourceName, ClassWriter classWriter) {
        String commandName = configBeanToPOSTCommand.get(resourceName);
        if(commandName != null) {
            classWriter.createGetPostCommand(commandName);

        }
    }

    /**
     * Generate resources for commands mapped under given parentBeanName
     * @param parentBeanName
     * @param parentWriter
     */
    private void generateCommandResources(String parentBeanName, ClassWriter parentWriter)  {

        List<CommandResourceMetaData> commandMetaData = CommandResourceMetaData.getMetaData(parentBeanName);
        if(commandMetaData.size() > 0) {
            for (CommandResourceMetaData metaData : commandMetaData) {
                String commandResourceName = parentBeanName + getBeanName(metaData.resourcePath);
                String commandResourceClassName = getClassName(commandResourceName);

                //Generate command resource class
                generateCommandResourceClass(parentBeanName, metaData);

                //Generate getCommandResource() method in parent
                parentWriter.createGetCommandResource(commandResourceClassName, metaData.resourcePath);

            }
            //Generate GetCommandResourcePaths() method in parent 
            parentWriter.createGetCommandResourcePaths(commandMetaData);
        }
    }

    /**
     * Generate code for Resource class corresponding to given parentBeanName and command
     * @param parentBeanName
     * @param metaData
     */
    private void generateCommandResourceClass(String parentBeanName, CommandResourceMetaData metaData) {

        String commandResourceClassName = getClassName(parentBeanName + getBeanName(metaData.resourcePath));
        String commandName = metaData.command;
        String commandDisplayName = metaData.resourcePath;
        String httpMethod = metaData.httpMethod;
        String commandAction = metaData.displayName;
        String baseClassName;

        if (httpMethod.equals("GET")) {
            baseClassName = "org.glassfish.admin.rest.resources.TemplateCommandGetResource";
        } else if (httpMethod.equals("DELETE")) {
            baseClassName = "org.glassfish.admin.rest.resources.TemplateCommandDeleteResource";
        } else if (httpMethod.equals("POST")) {
            baseClassName = "org.glassfish.admin.rest.resources.TemplateCommandPostResource";
        } else {
            throw new GeneratorException("Invalid httpMethod specified: " + httpMethod);
        }

        ClassWriter writer = getClassWriter(commandResourceClassName, baseClassName, null);

        boolean isLinkedToParent = false;
        if(metaData.commandParams != null) {
            for(CommandResourceMetaData.ParameterMetaData parameterMeraData : metaData.commandParams) {
                if(Constants.PARENT_NAME_VARIABLE.equals(parameterMeraData.value) ) {
                    isLinkedToParent = true;
                }
            }
        }

        writer.createCommandResourceConstructor(commandResourceClassName, commandName, httpMethod, isLinkedToParent, metaData.commandParams, commandDisplayName, commandAction);

        writer.done();
    }

    /**
     * @param className
     * @return true if the given className is already generated. false otherwise.
     */
    private boolean alreadyGenerated(String className) {
        boolean retVal = true;
        if (!alreadyGenerated.contains(className)) {
            alreadyGenerated.add(className);
            retVal = false;
        }
        return retVal;
    }

    /**
     * @param beanName
     * @return generated class name for given beanName
     */
    private String getClassName(String beanName) {
        return beanName + "Resource";
    }

    /**
     * @param elementName
     * @return bean name for the given element name. The name is derived by uppercasing first letter of elementName,
     *         eliminating hyphens from elementName and ppercasing letter followed by hyphen
     */
    private String getBeanName(String elementName) {
        String ret = "";
        boolean nextisUpper = true;
        for (int i = 0; i < elementName.length(); i++) {
            if (nextisUpper == true) {
                ret = ret + elementName.substring(i, i + 1).toUpperCase();
                nextisUpper = false;
            } else {
                if (elementName.charAt(i) == '-') {
                    nextisUpper = true;
                } else {
                    nextisUpper = false;
                    ret = ret + elementName.substring(i, i + 1);
                }
            }
        }
        return ret;
    }

    /**
     * @param model
     * @return name of the key attribute for the given model.
     */
    private String getKeyAttributeName(ConfigModel model) {
        String keyAttributeName = null;
        if (model.key == null) {
            for (String s : model.getAttributeNames()) {//no key, by default use the name attr
                if (s.equals("name")) {
                    keyAttributeName = getBeanName(s);
                }
            }
            if (keyAttributeName == null)//nothing, so pick the first one
            {
                Set<String> attributeNames =  model.getAttributeNames();
                if(!attributeNames.isEmpty()) {
                    keyAttributeName = getBeanName(attributeNames.iterator().next());
                } else {
                    //TODO carried forward from old generator. Should never reach here. But we do for ConfigExtension and WebModuleConfig
                    keyAttributeName = "ThisIsAModelBug:NoKeyAttr"; //no attr choice fo a key!!! Error!!!
                }

            }
        } else {
            keyAttributeName = getBeanName(model.key.substring(1, model.key.length()));
        }
        return keyAttributeName;
    }

    /**
     * @param qualifiedTypeName
     * @return unqualified type name for given qualified type name. This is a substring of qualifiedTypeName after last "."
     */
    private String getUnqualifiedTypeName(String qualifiedTypeName) {
        return qualifiedTypeName.substring(qualifiedTypeName.lastIndexOf(".") + 1, qualifiedTypeName.length());
    }

    private void processRedirectsAnnotation(ConfigModel model) {
        Class<? extends ConfigBeanProxy> cbp = null;
        try {
            cbp = (Class<? extends ConfigBeanProxy>) model.classLoaderHolder.get().loadClass(model.targetTypeName);
            // cbp = (Class<? extends ConfigBeanProxy>)this.getClass().getClassLoader().loadClass(model.targetTypeName) ;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        RestRedirects restRedirects = cbp.getAnnotation(RestRedirects.class);
        if (restRedirects != null) {

            RestRedirect[] values = restRedirects.value();
            for (RestRedirect r : values) {
                System.out.println(r.commandName());
                System.out.println(r.opType());
            }
        }
    }

    //TODO - fetch command name from config bean(RestRedirect annotation).
    private static final Map<String, String> configBeanToDELETECommand = new HashMap<String, String>() {{
        put("ApplicationRef", "delete-application-ref");
        put("ExternalJndiResource", "delete-jndi-resource");
        put("JaccProvider", "delete-jacc-provider");
        put("NetworkListener", "delete-network-listener");
        put("Profiler", "delete-profiler");
        put("Property", "GENERIC-DELETE");
        put("Protocol", "delete-protocol");
        put("ProtocolFilter", "delete-protocol-filter");
        put("ProtocolFinder", "delete-protocol-finder");
        put("Transport", "delete-transport");
        put("ThreadPool", "delete-threadpool");
    }};

    //TODO - fetch command name from config bean(RestRedirect annotation).
    private static final Map<String, String> configBeanToPOSTCommand = new HashMap<String, String>()
    {{
        put("Application", "redeploy"); //TODO check : This row is not used
        put("JavaConfig", "create-profiler"); // TODO check: This row is not used
        put("ListAdminObjectResource", "create-admin-object");
        put("ListApplication", "deploy");
        put("ListAuditModule", "create-audit-module");
        put("ListAuthRealm", "create-auth-realm");
        put("ListCluster", "create-cluster");
        put("ListConnectorConnectionPool", "create-connector-connection-pool");
        put("ListConnectorResource", "create-connector-resource");
        put("ListCustomResource", "create-custom-resource");
        put("ListExternalJndiResource", "create-jndi-resource");
        put("ListHttpListener", "create-http-listener");
        put("ListIiopListener", "create-iiop-listener");
        put("ListJdbcConnectionPool", "create-jdbc-connection-pool");
        put("ListJdbcResource", "create-jdbc-resource");
        put("ListJmsHost", "create-jms-host");
        put("ListMailResource", "create-javamail-resource");
        put("ListMessageSecurityConfig", "create-message-security-provider");
        put("ListNetworkListener", "create-network-listener");
        put("ListProtocol", "create-protocol");
        put("ListResourceAdapterConfig", "create-resource-adapter-config");
        put("ListResourceRef", "create-resource-ref");
        put("ListSystemProperty", "create-system-properties");
        put("ListThreadPool", "create-threadpool");
        put("ListTransport", "create-transport");
        put("ListVirtualServer", "create-virtual-server");
        put("ListWorkSecurityMap", "create-connector-work-security-map");
        put("ProtocolFilter", "create-protocol-filter");
        put("ProtocolFinder", "create-protocol-finder");
    }};

    private static final String[][] configBeanCustomResources = {
        // ConfigBean, Custom Resource Class, path
        {"NetworkListener", "FindHttpProtocolResource", "find-http-protocol"}
    };

    private static class CollectionLeafMetaData {
        String postCommandName;
        String deleteCommandName;
        String displayName;

        CollectionLeafMetaData(String postCommandName, String deleteCommandName, String displayName) {
            this.postCommandName = postCommandName;
            this.deleteCommandName = deleteCommandName;
            this.displayName = displayName;
        }
    }

    //This map is used to generate CollectionLeaf resources.
    //Example: JVM Options. This information will eventually move to config bean-
    //JavaConfig or JvmOptionBag
    private static final Map<String, CollectionLeafMetaData> configBeanToCollectionLeafMetaData =
            new HashMap<String, CollectionLeafMetaData>() {{
        put("JvmOptions",new CollectionLeafMetaData("create-jvm-options", "delete-jvm-options", "JvmOption"));
    }};

}
