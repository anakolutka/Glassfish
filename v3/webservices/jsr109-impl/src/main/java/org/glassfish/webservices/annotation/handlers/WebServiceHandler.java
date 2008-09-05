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

package org.glassfish.webservices.annotation.handlers;

import java.util.Set;
import java.util.StringTokenizer;

import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;

import javax.enterprise.deploy.shared.ModuleType;

import org.glassfish.apf.AnnotationHandler;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.ProcessingContext;
import org.glassfish.apf.ResultType;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.apf.AnnotationProcessorException;

import org.glassfish.apf.impl.AnnotationUtils;
import org.glassfish.apf.impl.HandlerProcessingResultImpl;

import org.glassfish.apf.context.AnnotationContext;
import com.sun.enterprise.deployment.annotation.context.WebBundleContext;
import com.sun.enterprise.deployment.annotation.context.WebComponentContext;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import com.sun.enterprise.deployment.annotation.context.EjbBundleContext;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.annotation.handlers.AbstractHandler;

import javax.xml.namespace.QName;

import org.jvnet.hk2.annotations.Service;

/**
 * This annotation handler is responsible for processing the javax.jws.WebService 
 * annotation type.
 *
 * @author Jerome Dochez
 */
@Service
public class WebServiceHandler extends AbstractHandler {
    
    /** Creates a new instance of WebServiceHandler */
    public WebServiceHandler() {
    }
        
    public Class<? extends Annotation> getAnnotationType() {
        return javax.jws.WebService.class;
    }    

    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    public Class<? extends Annotation>[] getTypeDependencies() {

        return null;
    }
    
    public HandlerProcessingResult processAnnotation(AnnotationInfo annInfo) 
        throws AnnotationProcessorException     
    {
        AnnotatedElementHandler annCtx = annInfo.getProcessingContext().getHandler();
        AnnotatedElement annElem = annInfo.getAnnotatedElement();
        AnnotatedElement origAnnElem = annElem;
        
        // sanity check
        if (!(annElem instanceof Class)) {
            AnnotationProcessorException ape = new AnnotationProcessorException(
                    localStrings.getLocalString("enterprise.deployment.annotation.handlers.wrongannotationlocation",
                        "symbol annotation can only be specified on TYPE"),annInfo);
            annInfo.getProcessingContext().getErrorHandler().error(ape);
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.FAILED);                        
        }
        
        
        // Ignore @WebService annotation on an interface; process only those in an actual service impl class
        if (((Class)annElem).isInterface()) {         
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);            
        }        

        // let's get the main annotation of interest. 
        javax.jws.WebService ann = (javax.jws.WebService) annInfo.getAnnotation();
        
        BundleDescriptor bundleDesc;
        
        // Ensure that an EJB endpoint is packaged in EJBJAR and a servlet endpoint is packaged in a WAR
        if(annCtx instanceof EjbContext && 
                (annElem.getAnnotation(javax.ejb.Stateless.class) == null)) {
            AnnotationProcessorException ape = new AnnotationProcessorException(
                    localStrings.getLocalString("enterprise.deployment.annotation.handlers.webeppkgwrong",
                        "Class {0} is annotated with @WebService and without @Stateless but is packaged in a JAR." +
                        " If it is supposed to be a servlet endpoint, it should be packaged in a WAR; Deployment will continue assuming  this " +
                        "class to be just a POJO used by other classes in the JAR being deployed", 
                        new Object[] {((Class)annElem).getName()}),annInfo);
            ape.setFatal(false);
            throw ape;
        }
        if(annCtx instanceof EjbBundleContext && 
                (annElem.getAnnotation(javax.ejb.Stateless.class) == null)) {
            AnnotationProcessorException ape = new AnnotationProcessorException(
                    localStrings.getLocalString("enterprise.deployment.annotation.handlers.webeppkgwrong",
                        "Class {0} is annotated with @WebService and without @Stateless but is packaged in a JAR." +
                        " If it is supposed to be a servlet endpoint, it should be packaged in a WAR; Deployment will continue assuming this " +
                        "class to be just a POJO used by other classes in the JAR being deployed", 
                        new Object[] {((Class)annElem).getName()}),annInfo);
            ape.setFatal(false);
            throw ape;
        }
        if(annCtx instanceof WebBundleContext && 
                (annElem.getAnnotation(javax.ejb.Stateless.class) != null)) {
            AnnotationProcessorException ape = new AnnotationProcessorException(
                    localStrings.getLocalString("enterprise.deployment.annotation.handlers.ejbeppkgwrong",
                        "Class {0} is annotated with @WebService and @Stateless but is packaged in a WAR." +
                        " If it is supposed to be an EJB endpoint, it should be packaged in a JAR; Deployment will continue assuming this " +
                        " class to be just a POJO used by other classes in the WAR being deployed",
                        new Object[] {((Class)annElem).getName()}),annInfo);
            ape.setFatal(false);
            throw ape;
        }
        
        // let's see the type of web service we are dealing with...
        if (annElem.getAnnotation(javax.ejb.Stateless.class)!=null) {
            // this is an ejb !
            EjbContext ctx = (EjbContext) annCtx;
            bundleDesc = ctx.getDescriptor().getEjbBundleDescriptor();
            bundleDesc.setSpecVersion("3.0");
        } else {
            // this has to be a servlet since there is no @Servlet annotation yet
            if(annCtx instanceof WebComponentContext) {
                bundleDesc = ((WebComponentContext)annCtx).getDescriptor().getWebBundleDescriptor();
            } else {
                bundleDesc = ((WebBundleContext)annCtx).getDescriptor();
            }
            bundleDesc.setSpecVersion("2.5");            
        }        
        
        //WebService.name in the impl class identifies port-component-name
        // If this is specified in impl class, then that takes precedence
        String portComponentName = ann.name();

        // As per JSR181, the serviceName is either specified in the deployment descriptor
        // or in @WebSErvice annotation in impl class; if neither service name implclass+Service
        String svcNameFromImplClass = ann.serviceName();
        String implClassName = ((Class) annElem).getSimpleName();
        String implClassFullName = ((Class)annElem).getName();

        // In case user gives targetNameSpace in the Impl class, that has to be used as
        // the namespace for service, port; typically user will do this in cases where
        // port_types reside in a different namespace than that of server/port.
        // Store the targetNameSpace, if any, in the impl class for later use
        String targetNameSpace = ann.targetNamespace();
        
        // As per JSR181, the portName is either specified in deployment desc or in @WebService
        // in impl class; if neither, it will @WebService.name+Port; if @WebService.name is not there,
        // then port name is implClass+Port
        String portNameFromImplClass = ann.portName();
        if( (portNameFromImplClass == null) ||
            (portNameFromImplClass.length() == 0) ) {
            if( (portComponentName != null) && (portComponentName.length() != 0) ) {
                portNameFromImplClass = portComponentName + "Port";
            } else {
                portNameFromImplClass = implClassName+"Port";
            }
        }

        // Store binding type specified in Impl class
        String userSpecifiedBinding = null;
        javax.xml.ws.BindingType bindingAnn = (javax.xml.ws.BindingType)
                ((Class)annElem).getAnnotation(javax.xml.ws.BindingType.class);
        if(bindingAnn != null) {
            userSpecifiedBinding = bindingAnn.value();
        }

        // Store wsdlLocation in the impl class (if any) 
        String wsdlLocation = null;
        if (ann.wsdlLocation()!=null && ann.wsdlLocation().length()!=0) {
            wsdlLocation = ann.wsdlLocation();
        }

        // At this point, we need to check if the @WebService points to an SEI
        // with the endpointInterface attribute, if that is the case, the 
        // remaining attributes should be extracted from the SEI instead of SIB.
        boolean sibAnnotationOverriden=false;
        if (ann.endpointInterface()!=null && ann.endpointInterface().length()>0) {            
            Class endpointIntf;
            try {
                endpointIntf = ((Class) annElem).getClassLoader().loadClass(ann.endpointInterface());
            } catch(java.lang.ClassNotFoundException cfne) {
                throw new AnnotationProcessorException(
                        localStrings.getLocalString("enterprise.deployment.annotation.handlers.classnotfound",
                            "class {0} referenced from annotation symbol cannot be loaded"), annInfo);
            } 
            annElem = endpointIntf;
  
            ann = annElem.getAnnotation(javax.jws.WebService.class);
            if (ann==null) {
                throw new AnnotationProcessorException("SEI " + ((javax.jws.WebService) annInfo.getAnnotation()).endpointInterface()
                    + " referenced from the @WebService annotation on " + ((Class) annElem).getName() 
                    + " does not contain a @WebService annotation");                               
            }
            sibAnnotationOverriden = true;
            
            // SEI cannot have @BindingType
            if(annElem.getAnnotation(javax.xml.ws.BindingType.class) != null) {
                throw new AnnotationProcessorException("SEI " + ((javax.jws.WebService) annInfo.getAnnotation()).endpointInterface()
                    + " cannot have @BindingType");                                               
            }
        }

        WebServicesDescriptor wsDesc = bundleDesc.getWebServices();
        //WebService.name not found; as per 109, default port-component-name
        //is the simple class name as long as the simple class name will be a 
        // unique port-component-name for this module
        if(portComponentName == null || portComponentName.length() == 0) {
            portComponentName = implClassName;
        }
        // Check if this port-component-name is unique for this module
        WebServiceEndpoint wep = wsDesc.getEndpointByName(portComponentName);
        if(wep!=null) {
            //there is another port-component by this name in this module;
            //now we have to look at the SEI/impl of that port-component; if that SEI/impl
            //is the same as the current SEI/impl then it means we have to override values;
            //If the SEI/impl classes do not match, then no overriding should happen; we should
            //use fully qualified class name as port-component-name for the current endpoint
            if((wep.getServiceEndpointInterface() != null) &&
               (wep.getServiceEndpointInterface().length() != 0) &&
               (!((Class)annElem).getName().equals(wep.getServiceEndpointInterface()))) {
                portComponentName = implClassFullName;
            }
        }
        
        // Check if the same endpoint is already defined in webservices.xml
        // This has to be done again after applying the 109 rules as above
        // for port-component-name
        WebServiceEndpoint endpoint = wsDesc.getEndpointByName(portComponentName);
        WebService newWS;
        if(endpoint == null) {
            // Check if a service with the same name is already present
            // If so, add this endpoint to the existing service
            if (svcNameFromImplClass!=null && svcNameFromImplClass.length()!=0) {
                newWS = wsDesc.getWebServiceByName(svcNameFromImplClass);
            } else {
                newWS = wsDesc.getWebServiceByName(implClassName+"Service");
            }
            if(newWS==null) {
                newWS = new WebService();
                // service name from annotation
                if (svcNameFromImplClass!=null && svcNameFromImplClass.length()!=0) {
                    newWS.setName(svcNameFromImplClass);
                } else {
                    newWS.setName(implClassName+"Service");            
                }
                wsDesc.addWebService(newWS);
            }
            endpoint = new WebServiceEndpoint();
            if (portComponentName!=null && portComponentName.length()!=0) {
                endpoint.setEndpointName(portComponentName);
            } else {
                endpoint.setEndpointName(((Class) annElem).getName());
            }
            newWS.addEndpoint(endpoint);
            wsDesc.setSpecVersion(com.sun.enterprise.deployment.node.WebServicesDescriptorNode.SPEC_VERSION);            
        } else {
            newWS = endpoint.getWebService();
        }

        // If wsdl-service is specified in the descriptor, then the targetnamespace
        // in wsdl-service should match the @WebService.targetNameSpace, if any.
        // make that assertion here - and the targetnamespace in wsdl-service, if
        // present overrides everything else
        if(endpoint.getWsdlService() != null) {
            if( (targetNameSpace != null) && (targetNameSpace.length() != 0 ) &&
                (!endpoint.getWsdlService().getNamespaceURI().equals(targetNameSpace)) ) {
                AnnotationProcessorException ape = new AnnotationProcessorException(
                        "Target Namespace in wsdl-service element does not match @WebService.targetNamespace", 
                        annInfo);
                annInfo.getProcessingContext().getErrorHandler().error(ape);
                return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.FAILED);                        
            }
            targetNameSpace = endpoint.getWsdlService().getNamespaceURI();
        }

        // Service and port should reside in the same namespace - assert that
        if( (endpoint.getWsdlService() != null) &&
            (endpoint.getWsdlPort() != null) ) {
            if(!endpoint.getWsdlService().getNamespaceURI().equals(
                                    endpoint.getWsdlPort().getNamespaceURI())) {
                AnnotationProcessorException ape = new AnnotationProcessorException(
                        "Target Namespace for wsdl-service and wsdl-port should be the same", 
                        annInfo);
                annInfo.getProcessingContext().getErrorHandler().error(ape);
                return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.FAILED);                        
            }
        }
        
        //Use annotated values only if the deployment descriptor equivalen has not been specified

        // If wsdlLocation was not given in Impl class, see if it is present in SEI
        // Set this in DOL if there is no Depl Desc entry
        // Precedence given for wsdlLocation in impl class
        if(newWS.getWsdlFileUri() == null) {
            if(wsdlLocation != null) {
                newWS.setWsdlFileUri(wsdlLocation);
            } else {
                if (ann.wsdlLocation()!=null && ann.wsdlLocation().length()!=0) {
                    newWS.setWsdlFileUri(ann.wsdlLocation());
                }
            }
        }
        
        // Set binding id id @BindingType is specified by the user in the impl class
        if((!endpoint.hasUserSpecifiedProtocolBinding()) &&
                    (userSpecifiedBinding != null) &&
                        (userSpecifiedBinding.length() != 0)){
            endpoint.setProtocolBinding(userSpecifiedBinding);
        }        

        if(endpoint.getServiceEndpointInterface() == null) {
            // take SEI from annotation
            if (ann.endpointInterface()!=null && ann.endpointInterface().length()!=0) {
                endpoint.setServiceEndpointInterface(ann.endpointInterface());
            } else {
                endpoint.setServiceEndpointInterface(((Class)annElem).getName());
            }
        }

        // at this point the SIB has to be used no matter what @WebService was used.
        annElem = annInfo.getAnnotatedElement();

        if (ModuleType.WAR.equals(bundleDesc.getModuleType())) {
            if(endpoint.getServletImplClass() == null) {
                // Set servlet impl class here
                endpoint.setServletImplClass(((Class)annElem).getName());
            }

            // Servlet link name
            WebBundleDescriptor webBundle = (WebBundleDescriptor) bundleDesc;
            if(endpoint.getWebComponentLink() == null) {
                //<servlet-link> = <port-component-name>
                endpoint.setWebComponentLink(endpoint.getEndpointName());
            }
            if(endpoint.getWebComponentImpl() == null) {
                WebComponentDescriptor webComponent = (WebComponentDescriptor) webBundle.
                    getWebComponentByCanonicalName(endpoint.getWebComponentLink());

                // if servlet is not known, we should add it now
                if (webComponent == null) {
                    webComponent = new WebComponentDescriptor();
                    webComponent.setServlet(true);                
                    webComponent.setWebComponentImplementation(((Class) annElem).getCanonicalName());
                    webComponent.setName(endpoint.getEndpointName());
                    webComponent.addUrlPattern("/"+newWS.getName());
                    webBundle.addWebComponentDescriptor(webComponent);
                }
                endpoint.setWebComponentImpl(webComponent);
            }
        } else {
            if(endpoint.getEjbLink() == null) {
                javax.ejb.Stateless stateless = annElem.getAnnotation(javax.ejb.Stateless.class);
                String name;
                if (stateless.name()==null || stateless.name().length()>0) {
                    name = stateless.name();
                } else {
                    name = ((Class) annElem).getSimpleName();
                }
                EjbDescriptor ejb = ((EjbBundleDescriptor) bundleDesc).getEjbByName(name);
                endpoint.setEjbComponentImpl(ejb);
                ejb.setWebServiceEndpointInterfaceName(endpoint.getServiceEndpointInterface());
                endpoint.setEjbLink(ejb.getName());
            }
        }

        if(endpoint.getWsdlPort() == null) {
            // Use targetNameSpace given in wsdl-service/Impl class for port and service
            // If none, derive the namespace from package name and this will be used for
            // service and port - targetNamespace, if any, in SEI will be used for pprtType 
            // during wsgen phase
            if(targetNameSpace == null || targetNameSpace.length()==0) {
                // No targerNameSpace anywhere; calculate targetNameSpace and set wsdl port
                // per jax-ws 2.0 spec, the target name is the package name in 
                // the reverse order prepended with http:// 
                if (((Class) annElem).getPackage()!=null) {

                    StringTokenizer tokens = new StringTokenizer(
                            ((Class) annElem).getPackage().getName(), ".", false);

                    if (tokens.hasMoreElements()) {
                        while (tokens.hasMoreElements()) {
                            if(targetNameSpace == null || targetNameSpace.length()==0) {
                                targetNameSpace=tokens.nextElement().toString();
                            } else {
                                targetNameSpace=tokens.nextElement().toString()+"."+targetNameSpace;
                            }
                        }
                    } else {
                        targetNameSpace = ((Class) annElem).getPackage().getName();
                    }
                } else {
                    throw new AnnotationProcessorException("JAX-WS 2.0 paragraph 3.2. " +
                            "The javax.jws.WebService annotation " 
                            + "targetNamespace MUST be used for classes or interfaces in no package");
                }
                targetNameSpace = "http://" + (targetNameSpace==null?"":targetNameSpace+"/");
            }
            // WebService.portName = wsdl-port
            endpoint.setWsdlPort(new QName(targetNameSpace, portNameFromImplClass, "ns1"));
        }

        if(endpoint.getWsdlService() == null) {
            // Set wsdl-service properly; namespace is the same as that of wsdl port;
            // service name derived from deployment desc / annotation / default
            String serviceNameSpace = endpoint.getWsdlPort().getNamespaceURI();
            String serviceName = null;
            if ( (svcNameFromImplClass != null) && 
                  (svcNameFromImplClass.length()!= 0)) {
                // Use the serviceName annotation if available
                serviceName= svcNameFromImplClass;
            } else {
              serviceName = newWS.getName();
            }
            endpoint.setWsdlService(new QName(serviceNameSpace, serviceName, "ns1"));
        }

        // Now force a HandlerChain annotation processing
        // This is to take care of the case where the endpoint Impl class does not
        // have @HandlerChain but the SEI has one specified through JAXWS customization
        if((((Class)origAnnElem).getAnnotation(javax.jws.HandlerChain.class)) == null) {
            return (new HandlerChainHandler()).processHandlerChainAnnotation(annInfo, annCtx, origAnnElem, (Class)origAnnElem, true);
        }
        return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);
    }
}
