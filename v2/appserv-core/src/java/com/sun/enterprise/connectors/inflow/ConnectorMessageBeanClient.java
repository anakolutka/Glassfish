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
package com.sun.enterprise.connectors.inflow;

import com.sun.ejb.MessageBeanClient;
import com.sun.ejb.MessageBeanListener;
import com.sun.ejb.MessageBeanProtocolManager;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.MessageListener;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.connectors.util.*;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.ConnectorRuntimeException;
import com.sun.enterprise.connectors.ActiveResourceAdapter;
import com.sun.enterprise.connectors.ActiveInboundResourceAdapter;
import com.sun.enterprise.connectors.ActiveOutboundResourceAdapter;
import com.sun.enterprise.connectors.system.ActiveJmsResourceAdapter;
import com.sun.logging.LogDomains;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.UnavailableException;
import javax.transaction.xa.XAResource;

/**
 * Main helper implementation for message-beans associated with 
 * a queue. Uses connection consumer for concurrent message
 * delivery.  
 * 
 * @author Qingqing Ouyang
 */
public final class ConnectorMessageBeanClient 
    implements MessageBeanClient, MessageEndpointFactory {

    private static final String MESSAGE_ENDPOINT = 
    "javax.resource.spi.endpoint.MessageEndpoint";
    
    private ConnectorRegistry registry_; 
   
    private MessageBeanProtocolManager messageBeanPM_;
    private final EjbMessageBeanDescriptor descriptor_;
    private final BasicResourceAllocator allocator_;
    private boolean started_ = false;

    private final int CREATED = 0;
    private final int BLOCKED = 1;
    private final int UNBLOCKED = 2;
    private int myState=CREATED;

    private final long WAIT_TIME = 60000;

    //unique identify a message-driven bean
    private String beanID_; //appName:modlueID:beanName
    
    private static final Logger logger = 
    LogDomains.getLogger(LogDomains.RSR_LOGGER);

    /**
     * Creates an instance of <code>ConnectorMessageBeanClient</code>
     * 
     * @param descriptor <code>EjbMessageBeanDescriptor</code> object.
     */
    public ConnectorMessageBeanClient(EjbMessageBeanDescriptor descriptor) {
        descriptor_ = descriptor;
        allocator_  = new BasicResourceAllocator(); 
        
        String appName = descriptor.getApplication().getName();
        
        String moduleID = 
            descriptor.getEjbBundleDescriptor().getModuleID();
        
        String beanName = descriptor.getName();
        
        beanID_ = appName + ":" + moduleID + ":" + beanName;

	try {
            registry_ = ConnectorRegistry.getInstance();
        } catch (Exception e) {
        }

    }
    
    /**
     * Gets executed as part of message bean deployment. Creates the
     * <code>ActivationSpec</code> javabean and does endpointfactory
     * activation with the resource adapter. This code also converts
     * all J2EE 1.3 MDB properties to MQ resource adapter activation
     * spec properties, if user doesnt specifies resource adapter 
     * module name in sun-ejb-jar.xml of the MDB. This is done using
     * <code>com.sun.enterprise.connector.system.ActiveJmsResourceAdapter
     * </code>
     *
     * @param pm <code>MessageBeanProtocolManager</code> object.
     */
    public void setup(MessageBeanProtocolManager messageBeanPM) 
        throws Exception {
        boolean d = true;

        messageBeanPM_ = messageBeanPM;
        
        String resourceAdapterMid = descriptor_.getResourceAdapterMid();
        ActiveResourceAdapter activeRar = null;
        if (resourceAdapterMid == null) {
            resourceAdapterMid = ConnectorRuntime.DEFAULT_JMS_ADAPTER;
        }
        activeRar = registry_.getActiveResourceAdapter(resourceAdapterMid);
        
        if(activeRar == null && 
              resourceAdapterMid.equals(ConnectorRuntime.DEFAULT_JMS_ADAPTER)) {
            ConnectorRuntime crt = ConnectorRuntime.getRuntime();
            crt.loadDeferredResourceAdapter(resourceAdapterMid);
            activeRar = registry_.getActiveResourceAdapter(resourceAdapterMid);
        }

        if (activeRar == null) {
	    String msg = "Resource adapter "+resourceAdapterMid+ " is not deployed";
	    throw new ConnectorRuntimeException(msg);
        }

        if (activeRar instanceof ActiveJmsResourceAdapter) {
            ActiveJmsResourceAdapter jmsRa = (ActiveJmsResourceAdapter) activeRar;
            jmsRa.updateMDBRuntimeInfo(descriptor_, 
                                       messageBeanPM_.getPoolDescriptor());
        }

        if (!(activeRar instanceof ActiveInboundResourceAdapter)) {
            throw new Exception("Resource Adapter selected doesn't support Inflow");
        } 
        ActiveInboundResourceAdapter rar = (ActiveInboundResourceAdapter) activeRar;

        //the resource adapter this MDB client is deployed to
        ResourceAdapter ra =  rar.getResourceAdapter();
        
        ConnectorDescriptor desc = rar.getDescriptor();
        
        String msgListenerType = getDescriptor().getMessageListenerType();
        if (msgListenerType == null || "".equals(msgListenerType))
          msgListenerType = "javax.jms.MessageListener";

        Iterator i = 
            desc.getInboundResourceAdapter().getMessageListeners().iterator();

        MessageListener msgListener = null;
        while (i.hasNext()) {
            msgListener = (MessageListener) i.next();
            if (msgListenerType.equals(msgListener.getMessageListenerType()))
                break;
        }

        String activationSpecClassName = null;
        if (msgListener != null) {
            activationSpecClassName = msgListener.getActivationSpecClass();
        }

    
        if (activationSpecClassName != null) {
            if (logger.isLoggable(Level.FINEST)) {
                String msg = 
                    "ActivationSpecClassName = " + activationSpecClassName;
                logger.log(Level.FINEST, msg);
            }
            
            try {
                ClassLoader cl = rar.getClassLoader();
                Class aClass = cl.loadClass(activationSpecClassName);
                
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "classloader = " 
                            + aClass.getClassLoader());
                    logger.log(Level.FINEST, "classloader parent = " 
                            + aClass.getClassLoader().getParent());
                }

                ActivationSpec activationSpec = 
                    (ActivationSpec) aClass.newInstance();
                Set props = RARUtils.getMergedActivationConfigProperties(getDescriptor());

                AccessController.doPrivileged 
                    (new SetMethodAction(activationSpec, props));
                    
                activationSpec.setResourceAdapter(ra);
                
                /*
                  AccessController.doPrivileged(new PrivilegedAction() {
                  public java.lang.Object run() {
                  activationSpec.setResourceAdapter(ra);
                  return null;
                  }
                  });
                */
                
                boolean validate = 
                    "true".equals(System.getProperty("validate.jms.ra"));
                if (validate) {
                    try {
                        activationSpec.validate();
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, 
                                "endpointfactory.as_validate_Failed", ex);
                    }
                }
                
                myState=BLOCKED;
                ra.endpointActivation(this, activationSpec);
            
                rar.addEndpointFactoryInfo(beanID_, 
                        new MessageEndpointFactoryInfo(this, activationSpec));
	    
                
            } catch (Exception ex) {
		
                ex.printStackTrace();
                throw (Exception) (new Exception()).initCause(ex);
            }
        } else {
            //FIXME  throw some exception here.
            throw new Exception("Unsupported message listener type");
        }
    }

    /**
     * Marks the completion of MDB deployment. Unblocks the createEndPoint
     * method call.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        logger.logp(Level.FINEST, 
                "ConnectorMessageBeanClient", "start", "called...");
        started_ = true;
	myState=UNBLOCKED;
        synchronized (this) {
	    notifyAll();
        }
    }

    /**
     * Does endpoint deactivation with the resource adapter.
     * Also remove sthe <code>MessageEndpointFactoryInfo</code>
     * from house keeping.
     */
    public void close() {
        logger.logp(Level.FINEST, 
                "ConnectorMessageBeanClient", "close", "called...");
        
        started_ = false; //no longer available 
        
        
        String resourceAdapterMid = getDescriptor().getResourceAdapterMid();
        
        ActiveResourceAdapter activeRar = 
            registry_.getActiveResourceAdapter(resourceAdapterMid);

        if (activeRar instanceof ActiveInboundResourceAdapter) { //in case the RA is already undeployed
            ActiveInboundResourceAdapter rar = 
	       (ActiveInboundResourceAdapter) activeRar;
            MessageEndpointFactoryInfo info = 
                rar.getEndpointFactoryInfo(beanID_);
            
	    if (info != null) {
                rar.getResourceAdapter().endpointDeactivation(
                    info.getEndpointFactory(), info.getActivationSpec());
            
                rar.removeEndpointFactoryInfo(beanID_);
	    } else {
	        logger.log(Level.FINE,"Not de-activating the end point, since it is not activated");
	    }
        }
    }

    private EjbMessageBeanDescriptor getDescriptor() {
        return descriptor_;
    }

    /**
     * Creates a MessageEndpoint. This method gets blocked either until start()
     * is called or until one minute. This is the time for completion
     * of MDB deployment.
     *
     * Internally this method creates a message bean listener from the MDB
     * container and a proxy object fo delivering messages.
     *
     * @return <code>MessageEndpoint</code> object.
     * @throws <code>UnavailableException</code> In case of any failure. This
     *         should change.
     */
    public MessageEndpoint
    createEndpoint (XAResource xa) throws UnavailableException {
        
	// This is a temperory workaround for blocking the the create enpoint
        // until the deployment completes.  One thread would wait for maximum a
        // a minute. 
        synchronized (this) {
	    if (myState == BLOCKED) {
               try {
	           wait(WAIT_TIME); 
	       }catch (Exception e) {
	           // This exception should not affect the functionality.
	       }finally {

		   // Once the first thread comes out of wait, block is 
                   // is removed. This makes sure that the time for which the
	           // the block remains is limited. Max 2x6000.
	           myState = UNBLOCKED;
	       }
            }
        }
        
        if (!started_) {
            logger.log(Level.WARNING, "endpointfactory.unavailable");
            throw new UnavailableException(
                    "EndpointFactory is currently not available");
        }
        
        MessageEndpoint endpoint = null;
        try {
            ResourceHandle resourceHandle = allocator_.createResource(xa);
            
            MessageBeanListener listener = 
                messageBeanPM_.createMessageBeanListener(resourceHandle);

            //Use the MDB's application classloader to load the
            //message listener class.  If it is generic listener
            //class, it is expected to be packaged with the MDB application
            //or in the system classpath.
            String moduleID = getDescriptor().getApplication().getModuleID();
            Class endpointClass = null;
            ClassLoader loader = null;
            try {
		BundleDescriptor moduleDesc = 
			getDescriptor().getEjbBundleDescriptor();
		loader = moduleDesc.getClassLoader();
            }catch(Exception e){
            	logger.log(Level.WARNING, "endpointfactory.loader_not_found",e);
            }

            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }

            endpointClass = loader.loadClass(MESSAGE_ENDPOINT);
            

            String msgListenerType = getDescriptor().getMessageListenerType();
            if (msgListenerType == null || "".equals(msgListenerType))
                msgListenerType = "javax.jms.MessageListener";

            Class listenerClass = loader.loadClass(msgListenerType);

            MessageEndpointInvocationHandler handler = 
                new MessageEndpointInvocationHandler(listener, messageBeanPM_);
            endpoint = (MessageEndpoint) Proxy.newProxyInstance
                (loader, new Class[] {endpointClass, listenerClass}, handler);
            
        } catch (Exception ex) {
            throw (UnavailableException) 
                (new UnavailableException()).initCause(ex);
        }
            
        return endpoint;
    }

    /**
     * Checks whether the message delivery is transacted for the method.
     * 
     * @return true or false.
     */
    public boolean isDeliveryTransacted(Method method) {
        return messageBeanPM_.isDeliveryTransacted(method);
    }
    
    /**
     * @return beanID of the message bean client
     */
    public String toString() {
        return beanID_;
    }   

}
