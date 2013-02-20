/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package org.glassfish.concurrent.runtime;

import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.security.integration.AppServSecurityContext;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.concurrent.runtime.deployer.ContextServiceConfig;
import org.glassfish.concurrent.runtime.deployer.ManagedExecutorServiceConfig;
import org.glassfish.concurrent.runtime.deployer.ManagedScheduledExecutorServiceConfig;
import org.glassfish.concurrent.runtime.deployer.ManagedThreadFactoryConfig;
import org.glassfish.enterprise.concurrent.*;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

/**
 * This class provides API to create various Concurrency Utilities objects
 */
@Service
@Singleton
public class ConcurrentRuntime implements PostConstruct, PreDestroy {

    private static ConcurrentRuntime _runtime;

    private Map<String, ManagedExecutorServiceImpl> managedExecutorServiceMap;
    private Map<String, ManagedScheduledExecutorServiceImpl> managedScheduledExecutorServiceMap;
    private Map<String, ContextServiceImpl> contextServiceMap;
    private Map<String, ManagedThreadFactoryImpl> managedThreadFactoryMap;

    public static final String CONTEXT_INFO_CLASSLOADER = "Classloader";
    public static final String CONTEXT_INFO_JNDI = "JNDI";
    public static final String CONTEXT_INFO_SECURITY = "Security";
    public static final String CONTEXT_INFO_WORKAREA = "WorkArea";

    @Inject
    AppServSecurityContext securityContext;

    @Inject
    InvocationManager invocationManager;

    @Inject
    Deployment deployment;

    @Inject
    Applications applications;

    /**
     * Returns the ConcurrentRuntime instance.
     * It follows singleton pattern and only one instance exists at any point
     * of time. External entities need to call this method to get
     * ConcurrentRuntime instance
     *
     * @return ConcurrentRuntime instance
     */
    public static ConcurrentRuntime getRuntime() {
        if (_runtime == null) {
            throw new RuntimeException("ConcurrentRuntime not initialized");
        }
        return _runtime;
    }

    private static void setRuntime(ConcurrentRuntime runtime) {
        _runtime = runtime;
    }

    /**
     * Constructor should be private to follow singleton pattern, but package access for unit testing.
     */
    ConcurrentRuntime() {
        setRuntime(this);
    }

    public synchronized ContextServiceImpl getContextService(ResourceInfo resource, ContextServiceConfig config) {
        String jndiName = config.getJndiName();
        if (contextServiceMap != null && contextServiceMap.containsKey(jndiName)) {
            return contextServiceMap.get(jndiName);
        }
        ContextServiceImpl contextService = createContextService(config.getJndiName(), config.getContextInfo());
        if (contextServiceMap == null) {
            contextServiceMap = new HashMap();
        }
        contextServiceMap.put(jndiName, contextService);
        return contextService;
    }

    public synchronized ManagedExecutorServiceImpl getManagedExecutorService(ResourceInfo resource, ManagedExecutorServiceConfig config) {
        String jndiName = config.getJndiName();
        if (managedExecutorServiceMap != null && managedExecutorServiceMap.containsKey(jndiName)) {
            return managedExecutorServiceMap.get(jndiName);
        }
        ManagedThreadFactoryImpl managedThreadFactory = new ManagedThreadFactoryImpl(
                config.getJndiName() + "-managedThreadFactory",
                null,
                config.getThreadPriority(),
                config.isLongRunningTasks());
        ManagedExecutorServiceImpl mes = new ManagedExecutorServiceImpl(config.getJndiName(),
                managedThreadFactory,
                config.getHungAfterSeconds() * 1000L, // in millseconds
                config.isLongRunningTasks(),
                config.getCorePoolSize(),
                config.getMaximumPoolSize(),
                config.getKeepAliveSeconds(), TimeUnit.SECONDS,
                0, //config.getTaskQueueCapacity(),
                createContextService(config.getJndiName() + "-contextservice", config.getContextInfo()),
                AbstractManagedExecutorService.RejectPolicy.ABORT,
                AbstractManagedExecutorService.RunLocation.LOCAL,
                false);
        if (managedExecutorServiceMap == null) {
            managedExecutorServiceMap = new HashMap();
        }
        managedExecutorServiceMap.put(jndiName, mes);
        return mes;
    }

    public synchronized ManagedScheduledExecutorServiceImpl getManagedScheduledExecutorService(ResourceInfo resource,
                                                                                               ManagedScheduledExecutorServiceConfig config) {
        String jndiName = config.getJndiName();
        if (managedScheduledExecutorServiceMap != null && managedScheduledExecutorServiceMap.containsKey(jndiName)) {
            return managedScheduledExecutorServiceMap.get(jndiName);
        }
        ManagedThreadFactoryImpl managedThreadFactory = new ManagedThreadFactoryImpl(
                config.getJndiName() + "-managedThreadFactory",
                null,
                config.getThreadPriority(),
                config.isLongRunningTasks());
        ManagedScheduledExecutorServiceImpl mes = new ManagedScheduledExecutorServiceImpl(config.getJndiName(),
                managedThreadFactory,
                config.getHungAfterSeconds() * 1000L, // in millseconds
                config.isLongRunningTasks(),
                config.getCorePoolSize(),
                config.getMaximumPoolSize(),
                config.getKeepAliveSeconds(), TimeUnit.SECONDS,
                config.getTaskQueueCapacity(),
                createContextService(config.getJndiName() + "-contextservice", config.getContextInfo()),
                AbstractManagedExecutorService.RejectPolicy.ABORT,
                AbstractManagedExecutorService.RunLocation.LOCAL,
                false);
        if (managedScheduledExecutorServiceMap == null) {
            managedScheduledExecutorServiceMap = new HashMap();
        }
        managedScheduledExecutorServiceMap.put(jndiName, mes);
        return mes;
    }

    public synchronized ManagedThreadFactoryImpl getManagedThreadFactory(ResourceInfo resource, ManagedThreadFactoryConfig config) {
        String jndiName = config.getJndiName();
        if (managedThreadFactoryMap != null && managedThreadFactoryMap.containsKey(jndiName)) {
            return managedThreadFactoryMap.get(jndiName);
        }
        ManagedThreadFactoryImpl managedThreadFactory = new ManagedThreadFactoryImpl(config.getJndiName(),
                createContextService(config.getJndiName() + "-contextservice", config.getContextInfo()),
                config.getThreadPriority(),
                false);
        if (managedThreadFactoryMap == null) {
            managedThreadFactoryMap = new HashMap();
        }
        managedThreadFactoryMap.put(jndiName, managedThreadFactory);
        return managedThreadFactory;
    }

    private ContextServiceImpl createContextService(String jndiName, String contextInfo) {
        ContextSetupProviderImpl.CONTEXT_TYPE[] contextTypes = parseContextInfo(contextInfo);
        ContextSetupProviderImpl contextSetupProvider =
                new ContextSetupProviderImpl(invocationManager, securityContext, deployment, applications,
                                             contextTypes);
        ContextServiceImpl obj = new ContextServiceImpl(jndiName, contextSetupProvider,
                new TransactionSetupProviderImpl());
        return obj;
    }

    private ContextSetupProviderImpl.CONTEXT_TYPE[] parseContextInfo(String contextInfo) {
        ArrayList<ContextSetupProviderImpl.CONTEXT_TYPE> contextTypeArray = new ArrayList<>();
        ContextSetupProviderImpl.CONTEXT_TYPE[] contextTypes = new ContextSetupProviderImpl.CONTEXT_TYPE[] {};
        if (contextInfo == null) {
            // by default, if no context info is passed, we propagate all context types
            contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE.CLASSLOADING);
            contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE.NAMING);
            contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE.SECURITY);
            contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE.WORKAREA);
            return contextTypeArray.toArray(contextTypes);
        }
        StringTokenizer st = new StringTokenizer(contextInfo, ",", false);
        while(st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            switch(token) {
                case CONTEXT_INFO_CLASSLOADER:
                    contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE.CLASSLOADING);
                    break;
                case CONTEXT_INFO_JNDI:
                    contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE.NAMING);
                    break;
                case CONTEXT_INFO_SECURITY:
                    contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE.SECURITY);
                    break;
                case CONTEXT_INFO_WORKAREA:
                    contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE.WORKAREA);
                    break;
                default:
                    break;
            }
        }
        return contextTypeArray.toArray(contextTypes);
    }

    @Override
    public void postConstruct() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void preDestroy() {
        // TODO shut down objects here
    }
}
