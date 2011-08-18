/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.javadbplugin.cli;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.paas.orchestrator.provisioning.DatabaseProvisioner;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.provisioning.iaas.CloudProvisioner;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryEntry;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryService;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 * @author Jagadish Ramu
 */
@Service(name = "create-database-service")
@Scoped(PerLookup.class)
public class CreateDatabaseService implements AdminCommand {

    @Param(name = "servicename", primary = true, optional = false)
    private String serviceName;

    @Inject
    private CloudRegistryService cloudRegistryService;

    @Inject
    private DatabaseServiceUtil dbServiceUtil;

    @Param(name="appname", optional=true)
    private String appName;

    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        // Check if the service is already configured.
        if (dbServiceUtil.isServiceAlreadyConfigured(serviceName, appName, ServiceType.DATABASE)) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Service with name [" +
                    serviceName + "] is already configured.");
            return;
        }

        //hack : We are using the default AMI which will work fine, but not for other DBs.
        CloudProvisioner cloudProvisioner = cloudRegistryService.getCloudProvisioner();
        String instanceID = cloudProvisioner.createMasterInstance();
        String ipAddress = cloudProvisioner.getIPAddress(instanceID);

        DatabaseProvisioner dbProvisioner = cloudRegistryService.getDatabaseProvisioner();
        dbProvisioner.startDatabase(ipAddress);

        CloudRegistryEntry entry = new CloudRegistryEntry();
        entry.setInstanceId(instanceID);
        entry.setIpAddress(ipAddress);
        entry.setState(CloudRegistryEntry.State.Running.toString());
        entry.setCloudName(serviceName);
        entry.setAppName(appName);
        entry.setServerType("database");

        dbServiceUtil.registerDBInfo(entry);

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        report.setMessage("Service with name [" +
                serviceName + "] is configured successfully.");
    }

}
