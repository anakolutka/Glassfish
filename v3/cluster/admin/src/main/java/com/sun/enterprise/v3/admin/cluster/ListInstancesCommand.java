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
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.cluster.InstanceInfo;
import java.util.*;
import java.util.logging.*;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.RuntimeType;
import org.jvnet.hk2.annotations.*;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.jvnet.hk2.component.*;
import static com.sun.enterprise.v3.admin.cluster.Constants.*;
import com.sun.enterprise.admin.util.RemoteInstanceCommandHelper;

/**
 * AdminCommand to list all instances and their states
 *
 * This is so clumsy & hard to remember I leave it here as a comment:
 * @Inject(name = ServerEnvironment.DEFAULT_INSTANCE_NAME)
 * @author Byron Nevins
 */
@org.glassfish.api.admin.Cluster(RuntimeType.DAS)
@Service(name = "list-instances")
@I18n("list.instances.command")
@Scoped(PerLookup.class)
public class ListInstancesCommand implements AdminCommand {

    @Inject
    private Habitat habitat;
    @Inject
    private Domain domain;
    @Inject
    private ServerEnvironment env;
    @Inject
    private Servers allServers;
    @Param(optional = true, defaultValue = "false")
    private boolean verbose;
    @Param(optional = true, defaultValue = "2000")
    private String timeoutmsec;
    @Param(optional = true, defaultValue = "false")
    private boolean standaloneonly;
    @Param(optional = true, defaultValue = "false")
    private boolean nostatus;
    @Param(optional = true, primary = true, defaultValue = "domain")
    String whichTarget;
    private List<InstanceInfo> infos = new LinkedList<InstanceInfo>();
    private List<Server> serverList;
    private ActionReport report;
    private ActionReport.MessagePart top;
    private static final String EOL = "\n";
    @Override
    public void execute(AdminCommandContext context) {
        // setup
        int timeoutInMsec;
        try {
            timeoutInMsec = Integer.parseInt(timeoutmsec);
        }
        catch (Exception e) {
            timeoutInMsec = 2000;
        }

        report = context.getActionReport();
        top = report.getTopMessagePart();

        Logger logger = context.getLogger();

        if (!validateParams())
            return;

        serverList = createServerList();

        if (serverList == null) {
            fail(Strings.get("list.instances.badTarget", whichTarget));
            return;
        }
        // Require that we be a DAS
        if (!env.isDas()) {
            String msg = Strings.get("list.instances.onlyRunsOnDas");
            logger.warning(msg);
            fail(msg);
            return;
        }

        if (nostatus)
            noStatus(serverList);
        else
            yesStatus(serverList, timeoutInMsec, logger);

        report.setActionExitCode(ExitCode.SUCCESS);
    }

    private void noStatus(List<Server> serverList) {
        if (serverList.size() < 1) {
            report.setMessage(NONE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        boolean firstServer = true;

        for (Server server : serverList) {
            boolean clustered = server.getCluster() != null;

            if (standaloneonly && clustered)
                continue;

            String name = server.getName();

            if (notDas(name)) {
                if (firstServer)
                    firstServer = false;
                else
                    sb.append(EOL);

                sb.append(name);
                top.addProperty(name, "");
            }
        }
    }

    private boolean notDas(String name) {
        return !SystemPropertyConstants.DAS_SERVER_NAME.equals(name);
    }

    private void yesStatus(List<Server> serverList, int timeoutInMsec, Logger logger) {
        // Gather a list of InstanceInfo -- one per instance in domain.xml
        RemoteInstanceCommandHelper helper = new RemoteInstanceCommandHelper(habitat);

        for (Server server : serverList) {
            boolean clustered = server.getCluster() != null;

            if (standaloneonly && clustered)
                continue;

            String name = server.getName();

            if (name == null)
                continue;   // can this happen?!?

            Cluster cluster = domain.getClusterForInstance(name);
            String clusterName = (cluster != null) ? cluster.getName() : null;
            // skip DAS
            if (notDas(name)) {
                InstanceInfo ii = new InstanceInfo(
                        name, helper.getAdminPort(server), server.getHost(),
                        clusterName, logger, timeoutInMsec);
                infos.add(ii);
            }
        }
        if (infos.size() < 1) {
            report.setMessage(NONE);
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (InstanceInfo ii : infos) {
            if(first) 
                first = false;
            else
                sb.append(EOL);
            
            String name = ii.getName();
            String display = ii.isRunning() ? RUNNING_DISPLAY : NOT_RUNNING_DISPLAY;
            String value = ii.isRunning() ? RUNNING : NOT_RUNNING;
            
            sb.append(name).append(display);
            top.addProperty(name, value);
            if (ii.isRunning()) top.addProperty(name + ".uptime", "" + ii.getUptime());
        }

        if (verbose)
            report.setMessage(InstanceInfo.format(infos));
        else
            report.setMessage(sb.toString());
    }

    /*
     * return null means the whichTarget is garbage
     * return empty list means the whichTarget was an empty cluster
     */
    private List<Server> createServerList() {
        // 1. no whichTarget specified
        if (!StringUtils.ok(whichTarget))
            return allServers.getServer();

        ReferenceContainer rc = domain.getReferenceContainerNamed(whichTarget);
        // 2. Not a server or a cluster. Could be a config or a Node
        if (rc == null) {
            return getServersForNodeOrConfig();
        }
        else if (rc.isServer()) {
            List<Server> l = new LinkedList<Server>();
            l.add((Server) rc);
            return l;
        }
        else if (rc.isCluster()) { // can't be anything else currently! (June 2010)
            Cluster cluster = (Cluster) rc;
            return cluster.getInstances();
        }
        else
            return null;
    }

    private List<Server> getServersForNodeOrConfig() {
        if (whichTarget == null)
            throw new NullPointerException("impossible!");

        List<Server> list = getServersForNode();

        if (list == null)
            list = getServersForConfig();

        return list;
    }

    private List<Server> getServersForNode() {
        boolean foundNode = false;
        Nodes nodes = domain.getNodes();

        if (nodes != null) {
            List<Node> nodeList = nodes.getNode();
            if (nodeList != null) {
                for (Node node : nodeList) {
                    if (whichTarget.equals(node.getName())) {
                        foundNode = true;
                        break;
                    }
                }
            }
        }
        if (!foundNode)
            return null;
        else
            return domain.getInstancesOnNode(whichTarget);
    }

    private List<Server> getServersForConfig() {
        Config config = domain.getConfigNamed(whichTarget);

        if (config == null)
            return null;

        List<ReferenceContainer> rcs = domain.getReferenceContainersOf(config);
        List<Server> servers = new LinkedList<Server>();

        for (ReferenceContainer rc : rcs)
            if (rc.isServer())
                servers.add((Server) rc);

        return servers;
    }


    /*
     * false means error
     */
    private boolean validateParams() {
        // another sort of weird scenario is that if the whichTarget is set to "domain",
        // that means ALL instances in the domains.  To make life easier -- we just
        //set the whichTarget to zilch to signal all instances in domain

        if ("domain".equals(whichTarget))
            whichTarget = null;

        // standaloneonly AND a whichTarget are mutually exclusive
        if (standaloneonly && StringUtils.ok(whichTarget)) {
            fail(Strings.get("list.instances.targetWithStandaloneOnly"));
            return false;
        }

        // verbose is not allowed with nostatus.
        // It could be allowed in the future if desired but the table code needs
        // to change.
        if (verbose && nostatus) {
            fail(Strings.get("list.instances.verboseAndNoStatus"));
            return false;
        }

        // details details details!
        // if the whichTarget is the weird screwy "server" then fail.
        // TODO - we *could* show DAS status in the future but it's stupid
        // since this command ONLY runs on DAS -- it is obviously running!!

        if (!notDas(whichTarget)) {
            fail(Strings.get("list.instances.serverTarget"));
            return false;
        }
        return true;
    }

    // avoid ugly boilerplate...
    private void fail(String s) {
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        report.setMessage(s);
    }
}
