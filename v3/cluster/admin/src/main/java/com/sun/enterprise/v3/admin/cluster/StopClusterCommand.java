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

package com.sun.enterprise.v3.admin.cluster;

import java.util.logging.Logger;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;

@I18n("stop.cluster.command")
@Service(name="stop-cluster")
@Scoped(PerLookup.class)
public class StopClusterCommand implements AdminCommand, PostConstruct {

    @Param(optional=false, primary=true)
    private String clusterName;

    @Inject
    private ServerEnvironment env;

    @Inject
    private Servers servers;

    @Inject
    private Domain domain;

    @Inject
    private Configs configs;

    @Inject
    private CommandRunner runner;

    @Inject(name=ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Server das_server;

    @Param(optional = true, defaultValue = "false")
    private boolean verbose;

    private RemoteInstanceCommandHelper helper;

    @Override
    public void postConstruct() {
        helper = new RemoteInstanceCommandHelper(env, servers, configs, domain);
    }

    @Override
    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();
        Logger logger = context.getLogger();

        logger.info(Strings.get("stop.cluster", clusterName));

        // Require that we be a DAS
        if(!helper.isDas()) {
            String msg = Strings.get("cluster.command.notDas");
            logger.warning(msg);
            report.setActionExitCode(ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        ClusterCommandHelper clusterHelper = new ClusterCommandHelper(domain,
                runner);

        try {
            // Run start-instance against each instance in the cluster
            String commandName = "stop-instance";
            clusterHelper.runCommand(commandName, null, clusterName, context,
                    verbose);
        } catch (CommandException e) {
            String msg = e.getLocalizedMessage();
            logger.warning(msg);
            report.setActionExitCode(ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
    }
}
