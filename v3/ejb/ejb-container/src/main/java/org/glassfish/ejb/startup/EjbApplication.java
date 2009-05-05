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

package org.glassfish.ejb.startup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.ejb.Container;
import com.sun.ejb.ContainerFactory;
import com.sun.ejb.containers.AbstractSingletonContainer;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.security.SecurityUtil;
import org.glassfish.ejb.security.application.EJBSecurityManager;
import org.glassfish.ejb.security.factory.EJBSecurityManagerFactory;

import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.ApplicationContext;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.deployment.common.DeploymentException;

import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;

// For auto-deploying EJBTimerService
import com.sun.ejb.containers.EjbContainerUtil;
import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.ejb.containers.EJBTimerService;
import org.glassfish.api.ActionReport;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;

import java.io.File;
import java.io.IOException;
// For auto-deploying EJBTimerService

/**
 * This class represents a logical collection of EJB components contained in one ejb-jar
 * or one .war.
 *
 * @author Mahesh Kannan
 */
@Service(name = "ejb")
@Scoped(PerLookup.class)
public class EjbApplication
        implements ApplicationContainer<Collection<EjbDescriptor>> {

    private Collection<EjbDescriptor> ejbs;
    private Collection<Container> containers = new ArrayList();
    private ClassLoader ejbAppClassLoader;
    private DeploymentContext dc;
    
    private Habitat habitat;

    private EJBSecurityManagerFactory ejbSMF;
     
    private ContainerFactory ejbContainerFactory;

    private SingletonLifeCycleManager singletonLCM;

    boolean usesEJBTimerService = false;

    // TODO: move restoreEJBTimers to correct location
    private static boolean restored = false;
    private static Object lock = new Object();
    // TODO: move restoreEJBTimers to correct location

    public EjbApplication(
            Collection<EjbDescriptor> bundleDesc, DeploymentContext dc,
            ClassLoader cl, Habitat habitat, 
            EJBSecurityManagerFactory ejbSecMgrFactory) {
        this.ejbs = bundleDesc;
        this.ejbAppClassLoader = cl;
        this.dc = dc;
        this.habitat = habitat;
        this.ejbContainerFactory = habitat.getByContract(ContainerFactory.class);
        this.ejbSMF = ejbSecMgrFactory;
    }
    
    public Collection<EjbDescriptor> getDescriptor() {
        return ejbs;
    }

    public boolean start(ApplicationContext startupContext) throws Exception {

        try {
            DeployCommandParameters params = ((DeploymentContext)startupContext).
                    getCommandParameters(DeployCommandParameters.class);

            // TODO: move restoreEJBTimers to correct location           
            if (usesEJBTimerService) {
                initEJBTimerService(params);
            }
            // TODO: move restoreEJBTimers to correct location

            for (Container container : containers) {
                container.startApplication(params.origin == OpsParams.Origin.deploy);
            }

            // TODO handle singleton startup dependencies that refer to singletons in a different
            // module within the application
            singletonLCM.doStartup();

        } catch(Exception e) {
            abortInitializationAfterException();
            throw e;
        }

        return true;
    }

    /**
     * Initial phase of continer initialization.  This creates the concrete container
     * instance for each EJB component, registers JNDI entries, etc.  However, no
     * EJB bean instances or invocations occur during this phase.  Those must be
     * delayed until start() is called.  
     * @param startupContext
     * @return
     */
    boolean loadContainers(ApplicationContext startupContext) {
        /*
        Set<EjbDescriptor> descs = (Set<GEjbDescriptor>) bundleDesc.getEjbs();

        long appUniqueID = ejbs.getUniqueId();
        long appUniqueID = 0;
        if (appUniqueID == 0) {
            appUniqueID = (System.currentTimeMillis() & 0xFFFFFFFF) << 16;
        }
        */


        DeployCommandParameters params = ((DeploymentContext)startupContext).
            getCommandParameters(DeployCommandParameters.class);

        // If true the application is being deployed.  If false, it's
        // an initialization after the app was already deployed. 
        boolean deploy = (params.origin == OpsParams.Origin.deploy );

        int counter = 0;

        singletonLCM = new SingletonLifeCycleManager();

        try {
            
            String moduleName = null;
        
            for (EjbDescriptor desc : ejbs) {
                EJBSecurityManager ejbSM = null;

                // Initialize each ejb container (setup component environment, register JNDI objects, etc.)
                // Any instance instantiation , timer creation/restoration, message inflow is delayed until
                // start phase.
                // create and register the security manager with the factory
                ejbSM = ejbSMF.createManager(desc, true);
                Container container = ejbContainerFactory.createContainer(desc, ejbAppClassLoader,
                        ejbSM, dc);
                containers.add(container);

                // Used during later start phase to determine whether to start timer service
                usesEJBTimerService = (usesEJBTimerService || container.isTimedObject());

                if (container instanceof AbstractSingletonContainer) {
                    singletonLCM.addSingletonContainer((AbstractSingletonContainer) container);
                }

            }

        } catch(Throwable t) {
            abortInitializationAfterException();
            throw new RuntimeException("EJB Container initialization error", t);
        }
        
        return true;
    }

    public boolean stop(ApplicationContext stopContext) {

        OpsParams params = ((DeploymentContext)stopContext).
                getCommandParameters(OpsParams.class);

        // If true we're shutting down b/c of an undeploy or a fatal error during
        // deployment.  If false, it's a shutdown where the application will remain
        // deployed.
        boolean undeploy = (params.origin == OpsParams.Origin.undeploy ) ||
                (params.origin == OpsParams.Origin.deploy);

        // First, shutdown any singletons that were initialized based
        // on a particular ordering dependency.
        // TODO Make sure this covers both eagerly and lazily initialized
        // Singletons.
        singletonLCM.doShutdown();

        for (Container container : containers) {
            if( undeploy ) {
                container.undeploy();
                if(container.getSecurityManager() != null) {
                    container.getSecurityManager().destroy();
                }
            } else {
                container.onShutdown();
            }
        }
        
        containers.clear();
        
        return true;
    }

    /**
     * Suspends this application container.
     *
     * @return true if suspending was successful, false otherwise.
     */
    public boolean suspend() {
        // Not (yet) supported
        return false;
    }

    /**
     * Resumes this application container.
     *
     * @return true if resumption was successful, false otherwise.
     */
    public boolean resume() {
        // Not (yet) supported
        return false;
    }

    /**
     * Returns the class loader associated with this application
     *
     * @return ClassLoader for this app
     */
    public ClassLoader getClassLoader() {
        return ejbAppClassLoader;
    }


    /**
     * Called when an exception is thrown from either the load phase or the start phase.
     * In this case we can't guarantee that the deployment framework will give us an
     * opportunity to clean up, especially if the EjbApplication object itself is never
     * registered due to the exception.  The most important thing is to make sure
     * global resources like JNDI names are cleaned up. Otherwise, subsequent deployment
     * attempts might fail without a server restart.   The container instances
     * themselves must be prepared to gracefully handle any case where undeploy/shutdown
     * is called multiple times.
     */
    private void abortInitializationAfterException() {

        for (Container container : containers) {
            container.undeploy();
        }

    }

    private void initEJBTimerService(DeployCommandParameters callerParams) {

        synchronized (lock) {
            EjbContainerUtil ejbContainerUtil = EjbContainerUtilImpl.getInstance();

            EJBTimerService ejbTimerService = ejbContainerUtil.getEJBTimerService();

            if (ejbTimerService == null) {

                Logger logger = ejbContainerUtil.getLogger();

                Deployment deployment = habitat.getByContract(Deployment.class);
                boolean isRegistered = deployment.isRegistered("ejb-timer-service-app");

                if (isRegistered) {
                    logger.log (Level.WARNING, "EJBTimerService had been explicitly deployed.");
                    return;
                }

                logger.log (Level.INFO, "Loading EJBTimerService. Please wait.");

                ServerContext sc = habitat.getByContract(ServerContext.class);
                File root = sc.getInstallRoot();
                File app = new File(root,
                        "lib/install/applications/ejb-timer-service-app.war");

                ActionReport report = habitat.getComponent(ActionReport.class, "plain");
                DeployCommandParameters params = new DeployCommandParameters(app);
                params.name = "ejb-timer-service-app";

                params.origin = callerParams.origin;

                if (!app.exists()) {
                    throw new RuntimeException("Failed to deploy EJBTimerService: " + 
                            "required WAR file (ejb-timer-service-app.war) is not installed");
                }

                try {
                    ExtendedDeploymentContext dc = deployment.getContext(logger, app, params, report);
                    deployment.deploy(dc);

                    if (report.getActionExitCode() != ActionReport.ExitCode.SUCCESS) {
                        throw new RuntimeException("Failed to deploy EJBTimerService: " +
                                report.getFailureCause());
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException("Failed to deploy EJBTimerService: " + ioe);
                }
            }
        }
    }
}
