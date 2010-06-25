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

package org.glassfish.deployment.admin;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.Param;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.Cluster;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.OpsParams.Origin;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.internal.data.ApplicationInfo;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.Transaction;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.io.File;
import java.net.URI;

/**
 * Delete application ref command
 */
@Service(name="delete-application-ref")
@I18n("delete.application.ref.command")
@Cluster(value={RuntimeType.DAS, RuntimeType.INSTANCE})
@Scoped(PerLookup.class)
@TargetType(value={CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
public class DeleteApplicationRefCommand implements AdminCommand {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteApplicationRefCommand.class);

    @Param(primary=true)
    public String name = null;

    @Param(optional=true)
    String target = "server";

    @Param(optional=true, defaultValue="false")
    public Boolean cascade;

    @Inject
    Deployment deployment;

    @Inject
    Domain domain;

    @Inject
    Applications applications;

    @Inject
    ArchiveFactory archiveFactory;

    @Inject(name= ServerEnvironment.DEFAULT_INSTANCE_NAME)
    protected Server server;

    /**
     * Entry point from the framework into the command execution
     * @param context context for the command.
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();

        Application application = applications.getApplication(name);
        if (application == null) {
            report.setMessage(localStrings.getLocalString("application.notreg","Application {0} not registered", name));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        ApplicationRef applicationRef = domain.getApplicationRefInTarget(name, target);
        if (applicationRef == null) {
            report.setMessage(localStrings.getLocalString("appref.not.exists","Target {1} does not have a reference to application {0}.", name, target));
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
            return;
        }

        try {
            ReadableArchive source = null;
            ApplicationInfo appInfo = deployment.get(name);
            if (appInfo != null) {
                source = appInfo.getSource();
            } else {
                File location = new File(new URI(application.getLocation()));
                source = archiveFactory.openArchive(location);
            }

            UndeployCommandParameters commandParams =
                new UndeployCommandParameters();

            if (server.isDas()) {
                commandParams.origin = Origin.unload;
            } else {
                // delete application ref on instance
                // is essentially an undeploy
                commandParams.origin = Origin.undeploy;
            }

            commandParams.name = name;
            commandParams.cascade = cascade;

            final ExtendedDeploymentContext deploymentContext =
                    deployment.getBuilder(logger, commandParams, report).source(source).build();
            deploymentContext.getAppProps().putAll(
                application.getDeployProperties());
            deploymentContext.setModulePropsMap(
                application.getModulePropertiesMap());

            if (domain.isCurrentInstanceMatchingTarget(target, server.getName())&& appInfo != null) {
                // stop and unload application if it's the target and the 
                // the application is in enabled state
                appInfo.stop(deploymentContext, deploymentContext.getLogger());
                appInfo.unload(deploymentContext);
            }

            if (report.getActionExitCode().equals(
                ActionReport.ExitCode.SUCCESS)) {
                try {
                    if (server.isInstance()) {
                        // if it's on instance, we should clean up
                        // the bits
                        deployment.undeploy(name, deploymentContext);
                        deploymentContext.clean();
                        if (!Boolean.valueOf(application.getDirectoryDeployed()) && source.exists()) {
                            FileUtils.whack(new File(source.getURI()));
                        }
                    }
                    deployment.unregisterAppFromDomainXML(name, target, true);
                } catch(TransactionFailure e) {
                    logger.warning("failed to delete application ref for " + name);
                }
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error during deleteing application ref ", e);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }
    }
}
