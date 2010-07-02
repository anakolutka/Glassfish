/*
 *
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

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.Cluster;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import java.util.logging.Logger;
import org.glassfish.cluster.ssh.connect.RemoteConnectHelper;
import org.glassfish.cluster.ssh.connect.RemoteConnectHelper;


/**
 * Remote AdminCommand to create a config node.  This command is run only on DAS.
 *  Register the config node on DAS
 *
 * @author Carla Mott
 */
@Service(name = "delete-node-config")
@I18n("delete.node.config")
@Scoped(PerLookup.class)
@Cluster({RuntimeType.DAS})
public class DeleteNodeConfigCommand implements AdminCommand, PostConstruct {
    @Inject
    Habitat habitat;

    @Inject
    Node[] nodeList;

    @Inject
    Nodes nodes;

    @Inject
    private CommandRunner cr;

    @Param(name="name", primary = true)
    String name;
    private RemoteInstanceCommandHelper helper;

    @Override
    public void postConstruct() {
        helper = new RemoteInstanceCommandHelper(habitat);
    }        

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        RemoteConnectHelper rch;
        Logger logger = context.logger;

        if (nodes.getNode(name) == null) {
            //no node to delete  nothing to do here
            String msg = Strings.get("noSuchNode", name);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        int dasPort = helper.getAdminPort(SystemPropertyConstants.DAS_SERVER_NAME);
        String dasHost = System.getProperty(SystemPropertyConstants.HOST_NAME_PROPERTY);
        rch = new RemoteConnectHelper(habitat, nodeList, logger, dasHost, dasPort);
        
        if (rch.isRemoteConnectRequired(name))
            return;

        // for now delete-node-ssh deletes all types of nodes so can call it.  that needs to be fixed.
        CommandInvocation ci = cr.getCommandInvocation("delete-node-ssh", report);
        ParameterMap map = new ParameterMap();
        map.add("DEFAULT", name);
        ci.parameters(map);
        ci.execute();

        
    }

}
