package org.glassfish.webservices;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Singleton;
import com.sun.enterprise.container.common.spi.WebServiceReferenceManager;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.logging.LogDomains;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceFactory;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.PrivilegedActionException;
import java.net.URL;
import java.io.File;


/**
 * This class acts as a service to resolve the
 * </code>javax.xml.ws.WebServiceRef</code> references
 * and also <code>javax.xml.ws.WebServiceContext</code>
 * Whenever a lookup is done from GlassfishNamingManagerImpl
 * these methods are invoked to resolve the references
 *
 * @author Bhakti Mehta
 */

@Service
public class WebServiceReferenceManagerImpl implements WebServiceReferenceManager {

    protected Logger logger = LogDomains.getLogger(this.getClass(),LogDomains.WEBSERVICES_LOGGER);



    public Object getWSContextObject() {
        return new WebServiceContextImpl();
    }

    public Object resolveWSReference(ServiceReferenceDescriptor desc, Context context)
            throws NamingException {


        //Taken from NamingManagerImpl.getClientServiceObject
        Class serviceInterfaceClass = null;
        Object returnObj = null;
        WsUtil wsUtil = new WsUtil();

        try {

            WSContainerResolver.set(desc);

            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            serviceInterfaceClass = cl.loadClass(desc.getServiceInterface());

            resolvePortComponentLinks(desc);

            javax.xml.rpc.Service serviceDelegate = null;
            javax.xml.ws.Service jaxwsDelegate = null;
            Object injValue = null;

            if( desc.hasGeneratedServiceInterface() || desc.hasWsdlFile() ) {

                String serviceImplName  = desc.getServiceImplClassName();
                if(serviceImplName != null) {
                    Class serviceImplClass  = cl.loadClass(serviceImplName);
                    serviceDelegate = (javax.xml.rpc.Service) serviceImplClass.newInstance();
                } else {

                    // The target is probably a post JAXRPC-1.1- based service;
                    // If Service Interface class is set, check if it is indeed a subclass of Service
                    // initiateInstance should not be called if the user has given javax.xml.ws.Service itself
                    // as the interface through DD
                    if(javax.xml.ws.Service.class.isAssignableFrom(serviceInterfaceClass) &&
                            !javax.xml.ws.Service.class.equals(serviceInterfaceClass)) {
                        // OK - the interface class is indeed the generated service class; get an instance
                        injValue = initiateInstance(serviceInterfaceClass, desc);
                    } else {
                        // First try failed; Try to get the Service class type from injected field name
                        // and from there try to get an instance of the service class

                        // I assume the at all inejction target are expecting the SAME service
                        // interface, therefore I take the first one.
                        if (desc.isInjectable()) {

                            InjectionTarget target = desc.getInjectionTargets().iterator().next();
                            Class serviceType = null;
                            if (target.isFieldInjectable()) {
                                java.lang.reflect.Field f = target.getField();
                                if(f == null) {
                                    String fName = target.getFieldName();
                                    Class targetClass = cl.loadClass(target.getClassName());
                                    try {
                                        f = targetClass.getDeclaredField(target.getFieldName());
                                    } catch(java.lang.NoSuchFieldException nsfe) {}// ignoring exception
                                }
                                serviceType = f.getType();
                            }
                            if (target.isMethodInjectable()) {
                                Method m = target.getMethod();
                                if(m == null) {
                                    String mName = target.getMethodName();
                                    Class targetClass = cl.loadClass(target.getClassName());
                                    try {
                                        m = targetClass.getDeclaredMethod(target.getMethodName());
                                    } catch(java.lang.NoSuchMethodException nsfe) {}// ignoring exception
                                }
                                if (m.getParameterTypes().length==1) {
                                    serviceType = m.getParameterTypes()[0];
                                }
                            }
                            if (serviceType!=null){
                                Class loadedSvcClass = cl.loadClass(serviceType.getCanonicalName());
                                injValue = initiateInstance(loadedSvcClass, desc);
                            }
                        }
                    }
                    // Unable to get hold of generated service class -> try the Service.create avenue to get a Service
                    if(injValue == null) {
                        // Here create the service with WSDL (overridden wsdl if wsdl-override is present)
                        // so that JAXWS runtime uses this wsdl @ runtime
                        javax.xml.ws.Service svc =
                                javax.xml.ws.Service.create((new WsUtil()).privilegedGetServiceRefWsdl(desc),
                                        desc.getServiceName());
                        jaxwsDelegate = new JAXWSServiceDelegate(desc, svc, cl);
                    }
                }

                if( desc.hasHandlers() ) {
                    // We need the service's ports to configure the
                    // handler chain (since service-ref handler chain can
                    // optionally specify handler-port association)
                    // so create a configured service and call getPorts
                    //TODO BM fix for jaxrpc
                    /*javax.xml.rpc.Service configuredService =
                            wsUtil.createConfiguredService(desc);
                    Iterator ports = configuredService.getPorts();
                    wsUtil.configureHandlerChain
                            (desc, serviceDelegate, ports, cl);*/
                }

                // check if this is a post 1.1 web service
                if(javax.xml.ws.Service.class.isAssignableFrom(serviceInterfaceClass)) {
                    // This is a JAXWS based webservice client;
                    // process handlers and mtom setting
                    // moved test for handlers into wsUtil, in case
                    // we have to add system handler

                    javax.xml.ws.Service service =
                            (injValue != null ?
                                    (javax.xml.ws.Service) injValue : jaxwsDelegate);

                    if (service != null) {
                        // Now configure client side handlers
                        wsUtil.configureJAXWSClientHandlers(service, desc);
                    }
                    // the requested resource is not the service but one of its port.
                    if (injValue!=null && desc.getInjectionTargetType()!=null) {
                        Class requestedPortType = service.getClass().getClassLoader().loadClass(desc.getInjectionTargetType());
                        injValue = service.getPort(requestedPortType);
                    }

                }

            } else {
                // Generic service interface / no WSDL
                QName serviceName = desc.getServiceName();
                if( serviceName == null ) {
                    // ServiceFactory API requires a service-name.
                    // However, 109 does not allow getServiceName() to be
                    // called, so it's ok to use a dummy value.
                    serviceName = new QName("urn:noservice", "servicename");
                }
                ServiceFactory serviceFac = ServiceFactory.newInstance();
                serviceDelegate = serviceFac.createService(serviceName);
            }

            // Create a proxy for the service object.
            // Get a proxy only in jaxrpc case because in jaxws the service class is not
            // an interface any more
            InvocationHandler handler = null;
            if(serviceDelegate != null) {
                // TODO BM fix for jaxrpc
                /*handler = new ServiceInvocationHandler(desc, serviceDelegate, cl);
                returnObj = Proxy.newProxyInstance
                        (cl, new Class[] { serviceInterfaceClass }, handler);*/
            } else if(jaxwsDelegate != null) {
                returnObj = jaxwsDelegate;
            } else if(injValue != null) {
                returnObj = injValue;
            }
        } catch(PrivilegedActionException pae) {
            logger.log(Level.WARNING, "", pae);
            NamingException ne = new NamingException();
            ne.initCause(pae.getCause());
            throw ne;
        } catch(Exception e) {
            logger.log(Level.WARNING, "", e);
            NamingException ne = new NamingException();
            ne.initCause(e);
            throw ne;
        } finally {
            WSContainerResolver.unset();
        }

        return returnObj;
    }

    private Object initiateInstance(Class svcClass, ServiceReferenceDescriptor desc)
            throws Exception {

        java.lang.reflect.Constructor cons = svcClass.getConstructor
                (new Class[]{java.net.URL.class,
                        javax.xml.namespace.QName.class});

        //TODO BM if JBI needs this reenable it
        /*com.sun.enterprise.webservice.ServiceRefDescUtil descUtil =
           new com.sun.enterprise.webservice.ServiceRefDescUtil();
        descUtil.preServiceCreate(desc);*/
        WsUtil wsu = new WsUtil();
        URL wsdlFile = wsu.privilegedGetServiceRefWsdl(desc);
        // Check if there is a catalog for this web service client
        // If so resolve the catalog entry
        String genXmlDir;
        if(desc.getBundleDescriptor().getApplication() != null) {
            genXmlDir = desc.getBundleDescriptor().getApplication().getGeneratedXMLDirectory();
            if(!desc.getBundleDescriptor().getApplication().isVirtual()) {
                String subDirName = desc.getBundleDescriptor().getModuleDescriptor().getArchiveUri();
                genXmlDir += (File.separator+subDirName.replaceAll("\\.",  "_"));
            }
        } else {
            // this is the case of an appclient being run as class file from command line
            genXmlDir = desc.getBundleDescriptor().getModuleDescriptor().getArchiveUri();
        }
        /* TODO BM resolve catalog
        File catalogFile = new File(genXmlDir,
                desc.getBundleDescriptor().getDeploymentDescriptorDir() +
                    File.separator + "jax-ws-catalog.xml");

        if(catalogFile.exists()) {
            wsdlFile = wsu.resolveCatalog(catalogFile, desc.getWsdlFileUri(), null);
        }   */
        Object obj =
                cons.newInstance(wsdlFile, desc.getServiceName());


        /*TODO BM if jbi needs this reenable it
        descUtil.postServiceCreate();
        */
        return obj;

    }

    private void resolvePortComponentLinks(ServiceReferenceDescriptor desc)
            throws Exception {

        // Resolve port component links to target endpoint address.
        // We can't assume web service client is running in same VM
        // as endpoint in the intra-app case because of app clients.
        //
        // Also set port-qname based on linked port's qname if not
        // already set.
        for(Iterator iter = desc.getPortsInfo().iterator(); iter.hasNext();) {
            ServiceRefPortInfo portInfo = (ServiceRefPortInfo) iter.next();

            if( portInfo.isLinkedToPortComponent() ) {
                WebServiceEndpoint linkedPortComponent =
                        portInfo.getPortComponentLink();

                // XXX-JD we could at this point try to figure out the
                // endpoint-address from the ejb wsdl file but it is a
                // little complicated so I will leave it for post Beta2
                if( !(portInfo.hasWsdlPort()) ) {
                    portInfo.setWsdlPort(linkedPortComponent.getWsdlPort());
                }
            }
        }
    }


}

