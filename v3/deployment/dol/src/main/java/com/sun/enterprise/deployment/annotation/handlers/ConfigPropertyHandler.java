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
package com.sun.enterprise.deployment.annotation.handlers;

import com.sun.enterprise.deployment.annotation.context.RarBundleContext;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.util.LocalStringManagerImpl;

import javax.resource.spi.*;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Level;

import org.glassfish.apf.impl.HandlerProcessingResultImpl;
import org.glassfish.apf.*;
import org.jvnet.hk2.annotations.Service;

/**
 * @author Jagadish Ramu
 */
@Service
public class ConfigPropertyHandler extends AbstractHandler {

    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(AbstractHandler.class);

    public Class<? extends Annotation> getAnnotationType() {
        return ConfigProperty.class;
    }

    public HandlerProcessingResult processAnnotation(AnnotationInfo element) throws AnnotationProcessorException {
        AnnotatedElementHandler aeHandler = element.getProcessingContext().getHandler();
        ConfigProperty configProperty = (ConfigProperty) element.getAnnotation();

        if (aeHandler instanceof RarBundleContext) {

            RarBundleContext rbc = (RarBundleContext) aeHandler;
            ConnectorDescriptor desc = rbc.getDescriptor();

            String defaultValue = configProperty.defaultValue();
            String description = configProperty.description();
            boolean ignore = configProperty.ignore();
            boolean supportsDynamicUpdates = configProperty.supportsDynamicUpdates();
            boolean confidential = configProperty.confidential();
            
            Class type = configProperty.type();

            if (element.getElementType().equals(ElementType.METHOD)) {

                Method m = (Method) element.getAnnotatedElement();

                Class[] parameters = m.getParameterTypes();
                Class propertyType;
                if (parameters != null) {
                    if (parameters.length == 1) {
                        propertyType = parameters[0];
                    } else {
                        return getFailureResult(element, "more than one parameter for JavaBean setter method : " +
                                "[" + m.getName() + " ] ", true);
                    }
                } else {
                    return getFailureResult(element, "no parameters for JavaBean setter method : " +
                            "[" + m.getName() + " ] ", true);
                }

                if (type.equals(Object.class)) {
                    type = propertyType;
                } else {
                    //check compatibility between annotation type and property-type
                    if (!propertyType.isAssignableFrom(type)) {
                        return getFailureResult(element, "annotation type [" + type + "] and property-type" +
                                " [" + propertyType + "] " +
                                "are not assignment compatible", true);
                    }
                }

                ConnectorConfigProperty ep = getConfigProperty(defaultValue, description, ignore,
                        supportsDynamicUpdates, confidential, type, m.getName().substring(3));


                Class declaringClass = m.getDeclaringClass();
                handleConfigPropertyAnnotation(element, desc, ep, declaringClass);

            } else if (element.getElementType().equals(ElementType.FIELD)) {
                Field f = (Field) element.getAnnotatedElement();
                Class c = f.getDeclaringClass();
                Class returnType = f.getType();
                if (type.equals(Object.class)) {
                    type = returnType;
                } else {
                    //check compatibility between annotation type and return-type
                    if (!returnType.isAssignableFrom(type)) {
                        return getFailureResult(element, "annotation type [" + type + "] " +
                                "and return-type [" + returnType + "] " +
                                "are not assignment compatible", true);
                    }
                }

                if(defaultValue == null || defaultValue.equals("")){
                    defaultValue = deriveDefaultValueOfField(f);
                }
                ConnectorConfigProperty ep = getConfigProperty(defaultValue,description, ignore,
                        supportsDynamicUpdates, confidential,  type, f.getName());

                handleConfigPropertyAnnotation(element, desc, ep, c);

            }

        } else {
            return getFailureResult(element, "not a rar bundle context", true);
        }
        return getDefaultProcessedResult();
    }

    private String deriveDefaultValueOfField(Field f){
        Class declaringClass = f.getDeclaringClass();
        String fieldName = f.getName();
        String value = null;
        try {
            Object o = declaringClass.newInstance();
            String getterMethod = "get" + getCamelCasedPropertyName(fieldName);

            if(Boolean.class.isAssignableFrom(f.getType())){
                getterMethod = "is" + getCamelCasedPropertyName(fieldName);                
            }
            Method m = declaringClass.getDeclaredMethod(getterMethod);
            m.setAccessible(true);
            Object result = m.invoke(o);
            if(result != null) {
                value = result.toString();
            }
        } catch (Exception e) {
            Object[] args = {fieldName, declaringClass.getName(), e.getMessage()};
            String localString = localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.configpropertyfieldreadfailure",
                    "failed to read the value of field [{0}] on class [{1}], reason : {2}", args);
            logger.log(Level.WARNING, localString, e);
        }
        return value;
    }

    /**
     * Returns camel-cased version of a propertyName. Used to construct
     * correct accessor and mutator method names for a give property.
     */
    private String getCamelCasedPropertyName(String propertyName) {
        return propertyName.substring(0, 1).toUpperCase() +
                propertyName.substring(1);
    }


    private ConnectorConfigProperty getConfigProperty(String defaultValue, String description, boolean ignore,
                                                      boolean supportsDynamicUpdates, boolean confidential,
                                                      Class type, String propertyName) {
        ConnectorConfigProperty ep = new ConnectorConfigProperty();
        //use description if specified
        if (!description.equals("")) {
            ep.setDescription(description);
        }
        //use default value if specified
        if ( defaultValue!= null && !defaultValue.equals("")) {
            ep.setValue(defaultValue);
        }
        ep.setType(type.getName());

        ep.setName(propertyName);

        if (!ep.isSetIgnoreCalled()) {
            ep.setIgnore(ignore);
        }
        if (!ep.isSetConfidentialCalled()) {
            ep.setConfidential(confidential);
        }
        if (!ep.isSupportsDynamicUpdates()) {
            ep.setSupportsDynamicUpdates(supportsDynamicUpdates);
        }
        return ep;
    }

    private void handleConfigPropertyAnnotation(AnnotationInfo element, ConnectorDescriptor desc,
                                                ConnectorConfigProperty ep, Class declaringClass) {

        if (ResourceAdapter.class.isAssignableFrom(declaringClass)) {
            //@Connector must be of type ResourceAdapter and hence the above check is sufficient to
            //take care of JavaBean as well annotation
            if(!processConnector(desc, ep, declaringClass)){
                //need to book-keep the annotation for post-processing
                desc.addConfigPropertyAnnotation(declaringClass.getName(), element);
            }
        } else if (ManagedConnectionFactory.class.isAssignableFrom(declaringClass)) {
            //@ConnectionDefintion, @ConnectionDefinitions must be of type ManagedConnectionFactory and hence
            //the above check is sufficient to take care of JavaBean as well annotation.
            processConnectionDefinition(element, desc, ep, declaringClass);
        } else if (ActivationSpec.class.isAssignableFrom(declaringClass)
                || declaringClass.getAnnotation(Activation.class) != null) {
            processActivation(element, desc, ep, declaringClass);
        } else if (declaringClass.getAnnotation(AdministeredObject.class) != null
                || isAdminObjectJavaBean(declaringClass, desc) ) {
            proecessAdministeredObject(element, desc, ep, declaringClass);
        }
    }

    private boolean isAdminObjectJavaBean(Class adminObjectClass, ConnectorDescriptor desc) {
        boolean isAdminObject = false;
        Set adminObjects = desc.getAdminObjects();
        Iterator adminObjectsItr = adminObjects.iterator();
        while(adminObjectsItr.hasNext()){
            AdminObject adminObject = (AdminObject)adminObjectsItr.next();
            if(adminObject.getAdminObjectClass().equals(adminObjectClass.getName())){
                isAdminObject = true;
                break;
            }
        }
        return isAdminObject;
    }

    private void proecessAdministeredObject(AnnotationInfo element, ConnectorDescriptor desc,
                                                           ConnectorConfigProperty ep, Class declaringClass) {

        if (declaringClass.getAnnotation(AdministeredObject.class) != null) {
            AdministeredObject ao = (AdministeredObject) declaringClass.getAnnotation(AdministeredObject.class);
            Class[] adminObjectInterfaces = ao.adminObjectInterfaces();
            if(adminObjectInterfaces.length > 0){
                for (Class adminObjectInterface : adminObjectInterfaces) {
                    handleAdministeredObject(element, desc, ep, declaringClass, adminObjectInterface);
                }
            }else{
                //handle the case where admin object interfaces are not specified via annotaiton
                List<Class> interfacesList = AdministeredObjectHandler.
                        deriveAdminObjectInterfacesFromHierarchy(declaringClass);

                //We assume that there will be only one interface (if there had been many, admin-object annotation
                //handler would have rejected it.)
                if(interfacesList.size() == 1){
                    Class intf = interfacesList.get(0);
                    handleAdministeredObject(element, desc, ep, declaringClass, intf);
                }
            }
        } else {
            Set adminObjects = desc.getAdminObjects();
            Iterator adminObjectItr = adminObjects.iterator();
            while(adminObjectItr.hasNext()){
                AdminObject adminObject = (AdminObject)adminObjectItr.next();
                if(adminObject.getAdminObjectClass().equals(declaringClass.getName())){
                    if (!(isConfigDefined(adminObject.getConfigProperties(), ep))) {
                        adminObject.addConfigProperty(ep);
                    }
                }
            }
        }
    }

    private void handleAdministeredObject(AnnotationInfo element, ConnectorDescriptor desc,
                                          ConnectorConfigProperty ep, Class adminObjectClass, Class adminObjectIntf) {
        AdminObject adminObject = desc.getAdminObject(adminObjectIntf.getName(), adminObjectClass.getName());
        if (adminObject != null) {
            if (!(isConfigDefined(adminObject.getConfigProperties(), ep))) {
                adminObject.addConfigProperty(ep);
            }
        } else {
            // ideally adminObject should not be null as "@AdministeredObject" 
            // should have been handled before @ConfigProperty
            getFailureResult(element, "could not get adminobject of interface " +
                    "[ " + adminObjectIntf.getName() + " ]" +
                    " and class [ " + adminObjectClass.getName() + " ]", true);
        }
    }

    private void processActivation(AnnotationInfo element, ConnectorDescriptor desc,
                                   ConnectorConfigProperty ep, Class declaringClass) {

            // Inbound Resource Adapter should have been defined if @Activation annotation
            // was processed successfully, before.
            InboundResourceAdapter ira = desc.getInboundResourceAdapter();
            if (declaringClass.getAnnotation(Activation.class) != null) {
                Activation activation = (Activation) declaringClass.getAnnotation(Activation.class);
                Class[] messageListeners = activation.messageListeners();

                //messageListeners cant be 0 as we ask "@Activation" to be handled before "@ConfigProperty"
                for (Class clz : messageListeners) {
                    if (ira.hasMessageListenerType(clz.getName())) {
                        MessageListener ml = ira.getMessageListener(clz.getName());

                        //check whether the activation-spec class in the descriptor
                        //for a particular message-listener is the same as this class as it is possible
                        //that this activation-spec class may have been ignored if ra.xml is already defined with
                        //this particular message-listener-type. If so, we should not add config-property as they
                        // belong to a particular activation-spec class.
                        if (ml.getActivationSpecClass().equals(declaringClass.getName()) &&
                                !(isConfigDefined(ml.getConfigProperties(), ep))) {
                            ml.addConfigProperty(ep);
                        }
                    }
                }
            } else {
                if(desc.getInBoundDefined()){
                    Set messageListeners = desc.getInboundResourceAdapter().getMessageListeners();
                    Iterator mlItr = messageListeners.iterator();
                    while(mlItr.hasNext()){
                        MessageListener ml = (MessageListener)mlItr.next();
                        if(ml.getActivationSpecClass().equals(declaringClass.getName())){
                            if (!(isConfigDefined(ml.getConfigProperties(), ep))) {
                                ml.addConfigProperty(ep);
                            }
                        }
                    }
                }
            }
        }

    private void processConnectionDefinition(AnnotationInfo element, ConnectorDescriptor desc,
                                             ConnectorConfigProperty ep, Class declaringClass) {
        if (desc.getOutBoundDefined()) {

            OutboundResourceAdapter ora = desc.getOutboundResourceAdapter();
            Set connectionDefinitions = ora.getConnectionDefs();

            for (Object o : connectionDefinitions) {
                ConnectionDefDescriptor cd = (ConnectionDefDescriptor) o;

                if (cd.getManagedConnectionFactoryImpl().equals(declaringClass.getName())) {

                    if (!(isConfigDefined(cd.getConfigProperties(), ep))) {
                        cd.addConfigProperty(ep);
                    }
                }
                //ignore if connection-definition entry is not found as it is possible that
                //ra.xml has a connection-definition with the same connection-factory class
                //as this annotation.

                //it is possible that multiple ConnectionDefinitions with same MCF class, but different
                //connection-factory-interface can be defined.Hence process all connection definitions
            }

        } else {
            // if there is a @ConfigProperty annotation on any of the connection-definition (MCF), either it is
            // defined via ra.xml and hence actual @ConnectionDefinition(s) annotation is ignored
            // or
            // no clash between ra.xml and the annotation, actual annotation is considered
            // So, outbound-ra must have been defined either way.
            getFailureResult(element, "Outbound RA is not yet defined", true);
        }
    }

    public static boolean processConnector(ConnectorDescriptor desc, ConnectorConfigProperty ep, Class declaringClass) {
        // make sure that the RA Class considered here is the one specified in descriptor
        // If not, it will be processed once the @Connector is selected during post-processing 

        // handle the annotation specified on a ResourceAdapter JavaBean
        // make sure that the property is not already specified in DD
        if(desc.getResourceAdapterClass().equals(declaringClass.getName())){
            if (!(isConfigDefined(desc.getConfigProperties(), ep))) {
                desc.addConfigProperty(ep);
            }
            //indicate that the config-property is processed
            return true;
        }else{
            //indicate that the config-property is not processed and need to be processed during post-processing
            return false;
        }
    }

    private static boolean isConfigDefined(Set configProperties, ConnectorConfigProperty ep) {
        boolean result = false;
        for (Object o : configProperties) {
            ConnectorConfigProperty ddEnvProperty = (ConnectorConfigProperty) o;
            if (ddEnvProperty.getName().equals(ep.getName())) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * @return a default processed result
     */
    protected HandlerProcessingResult getDefaultProcessedResult() {
        return HandlerProcessingResultImpl.getDefaultResult(
                getAnnotationType(), ResultType.PROCESSED);
    }


    public Class<? extends Annotation>[] getTypeDependencies() {
        return new Class[]{Connector.class, ConnectionDefinition.class, ConnectionDefinitions.class,
                Activation.class, AdministeredObject.class};
    }

    private void debug(String s) {
        logger.log(Level.INFO, "[ConfigPropertyHandler] " + s);
    }

    private HandlerProcessingResultImpl getFailureResult(AnnotationInfo element, String message, boolean doLog) {
        HandlerProcessingResultImpl result = new HandlerProcessingResultImpl();
        result.addResult(getAnnotationType(), ResultType.FAILED);
        if (doLog) {
            Object o = element.getAnnotatedElement();
            String className = null;
            if(o instanceof Field){
                className = ((Field)o).getDeclaringClass().getName();
            }else { //else it can be only METHOD
                className = ((Method)o).getDeclaringClass().getName();
            }
            Object args[] = new Object[]{
                element.getAnnotation(),
                className,
                message,
            };
            String localString = localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.connectorannotationfailure",
                    "failed to handle annotation [ {0} ] on class [ {1} ], reason : {2}", args);
            logger.log(Level.WARNING, localString);
        }
        return result;
    }
}
