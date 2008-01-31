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

import javax.xml.ws.WebServiceRef;

import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.Iterator;

import javax.xml.ws.*;

import org.glassfish.apf.AnnotationHandler;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.ProcessingContext;
import org.glassfish.apf.ResultType;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.impl.HandlerProcessingResultImpl;

import com.sun.enterprise.deployment.annotation.context.AppClientContext;
import com.sun.enterprise.deployment.annotation.context.WebBundleContext;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import com.sun.enterprise.deployment.annotation.context.EjbBundleContext;
import com.sun.enterprise.deployment.annotation.context.ServiceReferenceContainerContext;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.types.ServiceReferenceContainer;
import org.jvnet.hk2.annotations.Service;

/**
 * This annotation handler is responsible for processing the javax.jws.WebServiceRef annotation type.
 *
 * @author Jerome Dochez
 */
@Service
public class WebServiceRefHandler extends AbstractHandler  {
    
    /** Creates a new instance of WebServiceRefHandler */
    public WebServiceRefHandler() {
    }
    
    public Class<? extends Annotation> getAnnotationType() {
        return javax.xml.ws.WebServiceRef.class;
    }
    
    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    public Class<? extends Annotation>[] getTypeDependencies() {
        // it is easier if we return the array of component type. That
        // way, the @WebServiceRef is processed after the component
        // has been added to the DOL and the right EjbContext is
        // on the context stack. It won't hurt when @WebServiceRef
        // is used in appclients or web app since references are
        // declared at the bundle level.
        Class<? extends Annotation>[] dependencies = new Class[3];
        dependencies[0] = javax.ejb.Stateless.class;
        dependencies[1] = javax.ejb.Stateful.class;
        dependencies[2] = javax.persistence.Entity.class;
        return dependencies;
    }

    protected HandlerProcessingResult processAWsRef(AnnotationInfo annInfo, WebServiceRef annotation)
                throws AnnotationProcessorException {
        AnnotatedElementHandler annCtx = annInfo.getProcessingContext().getHandler();
        AnnotatedElement annElem = annInfo.getAnnotatedElement();
        
        Class annotatedType = null;
        Class declaringClass = null;
        String serviceRefName = annotation.name();
        if (annInfo.getElementType().equals(ElementType.FIELD)) {
            // this is a field injection
            Field annotatedField = (Field) annElem;
            
            // check this is a valid field
            if (annCtx instanceof AppClientContext){
                if (!Modifier.isStatic(annotatedField.getModifiers())){
                    throw new AnnotationProcessorException(
                            localStrings.getLocalString(
                            "enterprise.deployment.annotation.handlers.injectionfieldnotstatic",
                            "Injection fields for application clients must be declared STATIC"),
                            annInfo);
                }
            }
            
            annotatedType = annotatedField.getType();
            declaringClass = annotatedField.getDeclaringClass();
            // applying with default
            if (serviceRefName.equals("")) {
                serviceRefName = declaringClass.getName()
                + "/" + annotatedField.getName();
            }
        } else if (annInfo.getElementType().equals(ElementType.METHOD)) {
            
            // this is a method injection
            Method annotatedMethod = (Method) annElem;
            validateInjectionMethod(annotatedMethod, annInfo);
            
            if (annCtx instanceof AppClientContext){
                if (!Modifier.isStatic(annotatedMethod.getModifiers())){
                    throw new AnnotationProcessorException(
                            localStrings.getLocalString(
                            "enterprise.deployment.annotation.handlers.injectionmethodnotstatic",
                            "Injection methods for application clients must be declared STATIC"),
                            annInfo);
                }
            }
            
            annotatedType = annotatedMethod.getParameterTypes()[0];
            declaringClass = annotatedMethod.getDeclaringClass();
            if (serviceRefName == null || serviceRefName.equals("")) {
                // Derive javabean property name.
                String propertyName =
                    getInjectionMethodPropertyName(annotatedMethod, annInfo);
                // prefixing with fully qualified type name
                serviceRefName = declaringClass.getName()
                    + "/" + propertyName;
            }
        } else if (annInfo.getElementType().equals(ElementType.TYPE))
        {
            // name must be specified.
            if (serviceRefName==null || serviceRefName.length()==0) {
                throw new AnnotationProcessorException(
                        localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.nonametypelevel",
                        "TYPE-Level annotation  must specify name member."),  annInfo);                
            }
            // this is a dependency declaration, we need the service interface
            // to be specified
            annotatedType = annotation.type();
            if (annotatedType==null || annotatedType==Object.class  ) {
                throw new AnnotationProcessorException(
                        localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.typenotfound",
                        "TYPE-level annotation symbol must specify type member."),  
                         annInfo);
            }
            declaringClass = (Class) annElem;
        } else {    
                throw new AnnotationProcessorException(
                        localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.invalidtype",
                        "annotation not allowed on this element."),  annInfo);
            
        }
        
        ServiceReferenceContainer[] containers = null;
        ServiceReferenceDescriptor aRef =null;
        if (annCtx instanceof ServiceReferenceContainerContext) {
            containers = ((ServiceReferenceContainerContext) annCtx).getServiceRefContainers();
        }

        if (containers==null || containers.length==0) {
            annInfo.getProcessingContext().getErrorHandler().warning(
                    new AnnotationProcessorException(
                    localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.invalidannotationforthisclass",
                    "Illegal annotation symbol for this class will be ignored"),
                    annInfo));
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);
        }
        
        // now process the annotation for all the containers.
        for (ServiceReferenceContainer container : containers) {
            try {
                aRef =container.getServiceReferenceByName(serviceRefName);
            } catch(Throwable t) {}; // ignore 
               
            if (aRef==null) {
                // time to create it...
                aRef = new ServiceReferenceDescriptor();
                aRef.setName(serviceRefName);
                container.addServiceReferenceDescriptor(aRef);
            }

            // Store mapped name that is specified
            if(aRef.getMappedName() == null) {
                if(annotation.mappedName() != null && annotation.mappedName().length() != 0) {
                    aRef.setMappedName(annotation.mappedName());
                }
            }

            aRef.setInjectResourceType("javax.jws.WebServiceRef");
            
            if (!annInfo.getElementType().equals(ElementType.TYPE)) {
                InjectionTarget target = new InjectionTarget();
                if (annInfo.getElementType().equals(ElementType.FIELD)) {
                    // this is a field injection
                    Field annotatedField = (Field) annElem;
                    target.setFieldName(annotatedField.getName());
                    target.setClassName(annotatedField.getDeclaringClass().getName());
                } else {
                    if (annInfo.getElementType().equals(ElementType.METHOD)) {
                        // this is a method injection
                        Method annotatedMethod = (Method) annElem;
                        target.setMethodName(annotatedMethod.getName());
                        target.setClassName(annotatedMethod.getDeclaringClass().getName());
                    }
                }
                aRef.addInjectionTarget(target);
            }
            
            if (!Object.class.equals(annotation.value())) {
                // a value was provided, which should be the Service
                // interface, the requested injection is therefore on the
                // port.
                if (aRef.getServiceInterface()==null) {
                    aRef.setServiceInterface(annotation.value().getName());
                }
                
                if (aRef.getPortInfoBySEI(annotatedType.getName())==null) {
                    ServiceRefPortInfo portInfo = new ServiceRefPortInfo();
                    portInfo.setServiceEndpointInterface(annotatedType.getName());
                    aRef.addPortInfo(portInfo);
                }
                // set the port type requested for injection
                if (aRef.getInjectionTargetType()==null) {
                    aRef.setInjectionTargetType(annotatedType.getName());
                }
            }
            
            // watch the override order
            if(aRef.getName()==null || aRef.getName().length()==0) {
                aRef.setName(annotation.name());
            }
            if (aRef.getWsdlFileUri()==null) {
                if (annotation.wsdlLocation()==null || annotation.wsdlLocation().length()!=0) {
                    aRef.setWsdlFileUri(annotation.wsdlLocation());
                }
            }
            
            // Read the WebServiceClient annotation for the service name space uri and wsdl (if required)
            WebServiceClient wsclientAnn;
            if (Object.class.equals(annotation.value())) {
                wsclientAnn =  (WebServiceClient) annotatedType.getAnnotation(WebServiceClient.class);
            } else {
                wsclientAnn = (WebServiceClient) annotation.value().getAnnotation(WebServiceClient.class);
            }
            if (wsclientAnn==null) {
                throw new AnnotationProcessorException(
                        localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.classnotannotated",
                        "Class must be annotated with a {1} annotation\n symbol : {1}\n location: {0}",
                        new Object[] { annotatedType.toString(), WebServiceClient.class.toString() }));
            }
            
            // If wsdl file was not specified in a descriptor and not in the annotation, get it from WebServiceClient
            // annotation
            if (aRef.getWsdlFileUri()==null) {
                aRef.setWsdlFileUri(wsclientAnn.wsdlLocation());
            }
            
            // Set service name space URI and service local part
            if(aRef.getServiceName() == null) {
                aRef.setServiceNamespaceUri(wsclientAnn.targetNamespace());
                aRef.setServiceLocalPart(wsclientAnn.name());
            }
            
            if (aRef.getServiceInterface()==null) {
                aRef.setServiceInterface(annotatedType.getName());
            }
        }
        // Now force a HandlerChain annotation processing
        // This is to take care of the case where the client class does not
        // have @HandlerChain but the SEI has one specified through JAXWS customization
        if(annElem.getAnnotation(javax.jws.HandlerChain.class) == null) {
            return (new HandlerChainHandler()).processHandlerChainAnnotation(annInfo, annCtx, annotatedType, declaringClass, false);
        }
        return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);        
    }
    
    public HandlerProcessingResult processAnnotation(AnnotationInfo annInfo)
            throws AnnotationProcessorException {
        WebServiceRef annotation = (WebServiceRef) annInfo.getAnnotation();
        return(processAWsRef(annInfo, annotation));
    }
}
