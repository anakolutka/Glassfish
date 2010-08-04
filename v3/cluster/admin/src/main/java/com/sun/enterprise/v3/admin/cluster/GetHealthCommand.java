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

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.gms.bootstrap.GMSAdapter;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import org.glassfish.gms.bootstrap.HealthHistory;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The get-health command that lists the health status of
 * instances within a cluster.
 * Currently only works when GMS is enabled. To implement the
 * non-GMS case, look for the else block with comment "non-gms"
 */
@Service(name="get-health")
@I18n("get.health.command")
@Scoped(PerLookup.class)
public class GetHealthCommand implements AdminCommand {

    @Inject
    private Domain domain;
    @Inject
    private ServerEnvironment env;
    @Inject
    GMSAdapterService gmsAdapterService;

    @Param(optional=false, primary=true)
    @I18n("get.health.cluster.name")
    private String clusterName;
    
    private ActionReport report;
    private Logger logger;
    private GMSAdapter gmsAdapter;

    @Override
    public void execute(AdminCommandContext context) {
        report = context.getActionReport();
        logger = context.getLogger();
        logger.log(Level.INFO, Strings.get("get.health.called", clusterName));

        // output will be handled within this method
        if (!checkEnvAndParams()) {
            return;
        }

        /*
         * Check that gms is enabled for this cluster. Could also check
         * gmsAdapterService.isGmsEnabled() and
         * domain.getClusterNamed(clusterName).getGmsEnabled(), but that
         * should be redundant. If the GMSAdapter exists for the cluster,
         * we can use GMS.
         */
        gmsAdapter = gmsAdapterService.getGMSAdapterByName(clusterName);
        if (gmsAdapter != null) {
            getHealthWithGMS();
        } else {
            // if someone wants to implement the non-gms case, here's where
            setFail(Strings.get("get.health.noGMS", clusterName));
        }

    }

    // return false for any failures
    private boolean checkEnvAndParams() {

        // first check that we're the DAS
        if (!env.isDas()) {
            return setFail(Strings.get("get.health.onlyRunsOnDas"));
        }

        // check that cluster exists
        Cluster cluster = domain.getClusterNamed(clusterName);
        if (cluster == null) {
            return setFail(Strings.get("get.health.noCluster", clusterName));
        }

        // ok to go
        return true;
    }

    /*
     * Simply get the HealthHistory object from GMSAdapter and output
     * the information.
     */
    private void getHealthWithGMS() {
        StringBuilder result = new StringBuilder();
        HealthHistory history = gmsAdapter.getHealthHistory();
        if (history == null) {
            setFail(Strings.get("get.health.noHistoryError"));
            return;
        }
        String formatState = "Instance %s %s";
        String formatTime = " since %s"; 
        for(String name: history.getInstances()) {
            HealthHistory.InstanceHealth ih = history.getHealthByInstance(name);
            result.append(String.format(formatState, name, ih.state));
            if (HealthHistory.NOTIME != ih.time) {
                result.append(String.format(formatTime,
                    new Date(ih.time).toString()));
            }
            result.append("\n");
        }
        report.setMessage(result.toString());
    }

    // come fail away, come fail away, come fail away with me....
    private boolean setFail(String message) {
        logger.log(Level.WARNING, message);
        report.setMessage(message);
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        return false;
    }
}
