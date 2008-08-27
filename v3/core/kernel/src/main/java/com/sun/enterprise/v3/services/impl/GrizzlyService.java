/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */

package com.sun.enterprise.v3.services.impl;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Singleton;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.PreDestroy;
import org.jvnet.hk2.component.Habitat;
import org.glassfish.api.Startup;
import org.glassfish.api.FutureProvider;
import org.glassfish.api.container.RequestDispatcher;
import org.glassfish.api.container.EndpointRegistrationException;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.flashlight.provider.ProbeProviderFactory;
import org.glassfish.kernel.admin.monitor.ThreadPoolProbeProvider;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.HttpListener;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.Result;
import com.sun.enterprise.v3.admin.AdminAdapter;
import com.sun.enterprise.v3.admin.adapter.AdminConsoleAdapter;
import com.sun.grizzly.Controller;
import com.sun.grizzly.tcp.Adapter;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Network Service is responsible for starting grizzly and register the
 * top level proxy. It is also providing a runtime service where other
 * services (like admin for instance) can register endpoints proxy to
 * particular context root.
 *
 * @author Jerome Dochez
 */
@Service
@Scoped(Singleton.class)
public class GrizzlyService implements Startup, RequestDispatcher, PostConstruct, PreDestroy, FutureProvider<Result<Thread>> {

    public static final int ALL_PORTS = Integer.MAX_VALUE;

    static final ThreadPoolProbeProvider NO_OP_THREADPOOL_PROBE_PROVIDER =
        (ThreadPoolProbeProvider) Proxy.newProxyInstance(
            ThreadPoolProbeProvider.class.getClassLoader(),
            new Class[] { ThreadPoolProbeProvider.class },
            new NoopInvocationHandler());
    
    @Inject(name="server-config") // for now
    Config config;

    @Inject
    Logger logger;

    @Inject
    Habitat habitat;

    @Inject
    ProbeProviderFactory probeProviderFactory;

    List<NetworkProxy> proxies = new ArrayList<NetworkProxy>();

    List<Future<Result<Thread>>> futures;
    
    private final Controller controller  = new Controller();
    
    private Collection<String> hosts = new ArrayList<String>();

    private ThreadPoolProbeProvider threadPoolProbeProvider;


    /**
     * Add the new proxy to our list of proxies.
     * @param proxy new proxy to be added
     */
    public void addNetworkProxy(NetworkProxy proxy) {
        proxies.add(proxy);               
    }
    
    
    /**
     * Remove the new proxy from our list of proxies.
     * @param int port number to be removed
     */    
    public void removeNetworkProxy(int port) {
        NetworkProxy proxy = null;
        for (NetworkProxy p : proxies) {
            if (p.getPort() == port) {
                proxy = p;
            }
        }
        if (proxy != null) {
            proxy.stop();
            proxy.destroy();
            proxies.remove(proxy);
        }
    }
    
    
    /**
     * Returns the controller
     *
     * @return the controller.
     */   
    public Controller getController() {
        return controller;
    }
    
    
    /**
     * Gets the logger.
     *
     * @return the logger
     */   
    public Logger getLogger() {
        return logger;
    }


    /**
     * Gets the habitat.
     *
     * @return the habitat
     */   
    public Habitat getHabitat() {
        return habitat;
    }


    /**
     * Gets the ThreadPoolProbeProvider
     *
     * @return the ThreadPoolProbeProvider
     */   
    public ThreadPoolProbeProvider getThreadPoolProbeProvider() {
        return threadPoolProbeProvider;
    }


    /**
     * Returns the life expectency of the service
     *
     * @return the life expectency.
     */
    public Lifecycle getLifecycle() {
        return Lifecycle.SERVER;                
    }


    /**
     * The component has been injected with any dependency and
     * will be placed into commission by the subsystem.
     */
    public void postConstruct() {
        HttpService httpService = config.getHttpService();
        try {
            createProbeProviders();
            futures = new ArrayList<Future<Result<Thread>>>();
            for (HttpListener listener : httpService.getHttpListener()) {
                
               // Do not create listener when mod_ajp/jk is enabled. This 
               // should never happens one the grizzly-config configuration
               // will be used.
               if ("jk-connector".equals(listener.getId())){
                   continue;
               }

               futures.add(createNetworkProxy(listener, httpService));
            }
            registerNetworkProxy(); 
        } catch(RuntimeException e) { // So far postConstruct can not throw any other exception type
            logger.log(Level.WARNING, "Closing initialized network proxies");
            for(NetworkProxy proxy : proxies) {
                try {
                    proxy.stop();
                } catch(Exception proxyStopException) {
                    logger.log(Level.SEVERE, "Stop network proxy error ", proxyStopException);
                }
            }
            
            throw e;
        }
    }

    public List<Future<Result<Thread>>> getFutures() {
        return futures;
    }


    /**
     * Creates probe providers for monitoring purposes.
     */
    private void createProbeProviders() {

        try {
            threadPoolProbeProvider = probeProviderFactory.getProbeProvider(
                "core", "threadpool", null, ThreadPoolProbeProvider.class);
            if (threadPoolProbeProvider == null) {
                // Should never happen
                logger.log(Level.WARNING,
                    "Unable to create probe provider for interface " +
                    ThreadPoolProbeProvider.class.getName() +
                    ", using no-op provider");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                       "Unable to create probe provider for interface " +
                       ThreadPoolProbeProvider.class.getName(),
                       e);
        }
        if (threadPoolProbeProvider == null) {
            threadPoolProbeProvider = NO_OP_THREADPOOL_PROBE_PROVIDER;
        }
    }


    /*
     * Creates a new NetworkProxy for a particular HttpListner
     * @param listener HttpListener
     * @param httpService HttpService
     */
    public synchronized Future<Result<Thread>> createNetworkProxy(HttpListener listener, 
            HttpService httpService) {
        // create the proxy for the port.
        NetworkProxy proxy = new GrizzlyProxy(this, listener, httpService);
        proxy.setVsMapper(new VirtualHostMapper(logger, listener));
      
        // attach all virtual servers to this port
        for (VirtualServer vs : httpService.getVirtualServer()) {
            List<String> vsListeners = 
                    StringUtils.parseStringList(vs.getHttpListeners(), " ,");
            if (vsListeners == null || vsListeners.size() == 0 || 
                    vsListeners.contains(listener.getId())) {
                proxy.getVsMapper().addVirtualServer(vs);
                if (!hosts.contains(vs.getId())){
                    hosts.add(vs.getId());
                }
            }            
        }

        Future<Result<Thread>> future =  proxy.start();
        // add the new proxy to our list of proxies.
        proxies.add(proxy);

        return future;
    }
    
    
    /*
     * Registers all proxies
     */
    public void registerNetworkProxy(){
        // todo : this neeed some rework...
        // now register all proxies you can find out there !
        // TODO : so far these qets registered everywhere, maybe not the right thing ;-)
        for (org.glassfish.api.container.Adapter subAdapter : 
            habitat.getAllByContract(org.glassfish.api.container.Adapter.class)) {
            //@TODO change EndportRegistrationException processing if required
            try {
                if (subAdapter instanceof AdminAdapter) {
                    registerAdminAdapter((AdminAdapter)subAdapter);
                } else if (subAdapter instanceof AdminConsoleAdapter) {
                    registerAdminConsoleAdapter((AdminConsoleAdapter)subAdapter);
                } else {
                    registerEndpoint(subAdapter.getContextRoot(), hosts, 
                        subAdapter, null);
                }
            } catch(EndpointRegistrationException e) {
                logger.log(Level.WARNING, 
                        "GrizzlyService endpoint registration problem", e);
            }
        }
    }
    
    
    /**
     * The component is about to be removed from commission
     */
    public void preDestroy() {
        for (NetworkProxy proxy : proxies) {
            proxy.stop();
        }
    }

    /*
     * Registers a new endpoint (proxy implementation) for a particular
     * context-root. All request coming with the context root will be dispatched
     * to the proxy instance passed in.
     * @param contextRoot for the proxy
     * @param endpointAdapter servicing requests.
     */
    public void registerEndpoint(String contextRoot, Adapter endpointAdapter,
                                 ApplicationContainer container) throws EndpointRegistrationException {

        registerEndpoint(contextRoot, hosts, endpointAdapter, container);
    }

    /*
     * Registers a new endpoint (proxy implementation) for a particular
     * context-root. All request coming with the context root will be dispatched
     * to the proxy instance passed in.
     * @param contextRoot for the proxy
     * @param endpointAdapter servicing requests.
     */
    public void registerEndpoint(String contextRoot, Collection<String> vsServers,
            Adapter endpointAdapter,
            ApplicationContainer container) throws EndpointRegistrationException {
            
        registerEndpoint(contextRoot, ALL_PORTS, vsServers, endpointAdapter, container);
    }


    /**
     * Registers a new endpoint for the given context root at the given port
     * number.
     */
    public void registerEndpoint(String contextRoot,
                                 int port,
                                 Collection<String> vsServers,
                                 Adapter endpointAdapter,
                                 ApplicationContainer container) throws EndpointRegistrationException {
        for (NetworkProxy proxy : proxies) {
            if (port == ALL_PORTS || proxy.getPort() == port) {
                proxy.registerEndpoint(contextRoot, vsServers,
                                       endpointAdapter, container);
            }
        }
    }


    /**
     * Removes the contex-root from our list of endpoints.
     */
    public void unregisterEndpoint(String contextRoot) throws EndpointRegistrationException {
        unregisterEndpoint(contextRoot, null);
    }

    /**
     * Removes the contex-root from our list of endpoints.
     */
    public void unregisterEndpoint(String contextRoot, 
            ApplicationContainer app) throws EndpointRegistrationException {
        for (NetworkProxy proxy : proxies) {
            proxy.unregisterEndpoint(contextRoot, app);
        }
    }


    /**
     * Probe provider that implements each probe provider method as a 
     * no-op.
     */
    public static class NoopInvocationHandler implements InvocationHandler {

        public Object invoke(Object proxy, Method method, Object[] args) {
            // Deliberate no-op
            return null;
        };
    }

    private void registerAdminAdapter(AdminAdapter aa) throws EndpointRegistrationException {
        int port        = aa.getListenPort();
        List<String> vs = aa.getVirtualServers();
        String cr       = aa.getContextRoot();
        this.registerEndpoint(cr, port, vs, aa, null);
    }
    
    private void registerAdminConsoleAdapter(AdminConsoleAdapter aca) throws EndpointRegistrationException {
        int port        = aca.getListenPort();
        List<String> vs = aca.getVirtualServers();
        String cr       = aca.getContextRoot();
        this.registerEndpoint(cr, port, vs, aca, null);        
    }
}
