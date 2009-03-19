/*
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.internal.data;

import org.glassfish.internal.data.EngineInfo;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.Deployer;
import org.glassfish.api.deployment.ApplicationContext;
import org.glassfish.api.ActionReport;
import org.glassfish.api.container.Adapter;
import org.glassfish.api.container.EndpointRegistrationException;
import org.glassfish.api.container.RequestDispatcher;
import org.jvnet.hk2.config.TransactionFailure;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.beans.PropertyVetoException;

import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.config.serverbeans.Engine;
import com.sun.hk2.component.Holder;

/**
 * When a module is attached to a LoadedEngine, it creates an Engine reference. Each module
 * of an application can be loaded in several engines, however, a particular module can
 * only be loaded once in a particular engine.
 *
 * @author Jerome Dochez
 */
public class EngineRef {

    final private EngineInfo ctrInfo;

    private ApplicationContainer appCtr;

    public EngineRef(EngineInfo container,  ApplicationContainer appCtr) {
        this.ctrInfo = container;
        this.appCtr = appCtr;
    }

    /**
     * Returns the container associated with this application
     *
     * @return the container for this application
     */
    public EngineInfo getContainerInfo() {
        return ctrInfo;
    }

    /**
     * Set the contaier associated with this application
     * @param appCtr the container for this application
     */
    public void setApplicationContainer(ApplicationContainer appCtr) {
        this.appCtr = appCtr;
    }

    /**
     * Returns the contaier associated with this application
     * @return the container for this application
     */
    public ApplicationContainer getApplicationContainer() {
        return appCtr;
    }

    public void load(ExtendedDeploymentContext context) {
        getContainerInfo().load(context);
    }

    public boolean start(ApplicationContext context, ProgressTracker tracker)
        throws Exception {

        if (appCtr==null) {
            // the container does not care to be started or stopped
            return true;
        }
        if (!appCtr.start(context)) {
            return false;
        }

        tracker.add("started", EngineRef.class, this);
        return true;
    }

    /**
     * unloads the module from its container.
     *
     * @param context
     * @param report
     * @return
     */
    public boolean unload(ExtendedDeploymentContext context) {

        ActionReport report = context.getActionReport();
        // then remove the application from the container
        Deployer deployer = ctrInfo.getDeployer();
        try {
            deployer.unload(appCtr, context);
            ctrInfo.unload(context);
        } catch(Exception e) {
            report.failure(context.getLogger(), "Exception while shutting down application container", e);
            return false;
        }
        appCtr=null;
        return true;
    }

    /**
     * Stops a module, meaning that components implemented by this module should not be accessed
     * by external modules
     *
     * @param context
     * @param logger
     * @return
     */
    public boolean stop(ApplicationContext context,  Logger logger) {

       return appCtr.stop(context);
    }

    public void clean(ExtendedDeploymentContext context, Logger logger) {

        try {
            getContainerInfo().clean(context);            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while cleaning module '" + this + "'" + e, e);
        }
    }

    /**
     * Saves its state to the configuration. this method must be called within a transaction
     * to the configured engine instance.
     *
     * @param engine the engine configuration being persisted
     */
    public void save(Engine engine) throws TransactionFailure, PropertyVetoException {
        engine.setSniffer(getContainerInfo().getSniffer().getModuleType());
    }

}
