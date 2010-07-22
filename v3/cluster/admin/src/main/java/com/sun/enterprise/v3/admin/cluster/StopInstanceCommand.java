/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2010 Sun Microsystems, Inc. All rights reserved.
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

import com.sun.enterprise.admin.remote.RemoteAdminCommand;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.v3.admin.StopServer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Async;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.Cluster;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RuntimeType;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;

import java.util.Iterator;
import java.util.Collection;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.PerLookup;
import com.sun.enterprise.admin.util.RemoteInstanceCommandHelper;

/**
 * AdminCommand to stop the instance
 * server.
 * Shutdown of an instance.
 * This command only runs on DAS.  It calls the instance and asks it to
 * kill itself

 * @author Byron Nevins
 */
@Service(name = "stop-instance")
@Scoped(PerLookup.class)
@I18n("stop.instance.command")
@Cluster(RuntimeType.DAS)
public class StopInstanceCommand extends StopServer implements AdminCommand, PostConstruct {

    @Inject
    private Habitat habitat;
    @Inject
    Domain domain;
    @Inject
    private ServerEnvironment env;
    @Inject
    private ModulesRegistry registry;
    @Param(optional = true, defaultValue = "true")
    private Boolean force;
    @Param(optional = false, primary = true)
    private String instanceName;
    private Logger logger;
    private RemoteInstanceCommandHelper helper;
    private ActionReport report;
    private String errorMessage = null;

    public void execute(AdminCommandContext context) {
        report = context.getActionReport();
        logger = context.getLogger();

        if (env.isDas())
            errorMessage = callInstance();
        else
            errorMessage = Strings.get("stop.instance.notDas",
                    env.getRuntimeType().toString());

        if (errorMessage != null) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(errorMessage);
        }
        else {
            // todo -- verify it really stopped
            // waiting for IT for Vijay to get done -- API for status
            // we are GUARANTEED that instanceName is set...
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            report.setMessage(Strings.get("stop.instance.success",
                    instanceName));
        }
    }

    @Override
    public void postConstruct() {
        helper = new RemoteInstanceCommandHelper(habitat);
    }

    /**
     * return null if all went OK...
     *
     */
    private String callInstance() {
        String cmdName = "stop-instance";

        if (!StringUtils.ok(instanceName))
            return Strings.get("stop.instance.noInstanceName", cmdName);

        final Server instance = helper.getServer(instanceName);

        if (instance == null)
            return Strings.get("stop.instance.noSuchInstance", instanceName);

        String host = instance.getHost();

        if (host == null)
            return Strings.get("stop.instance.noHost", instanceName);

        int port = helper.getAdminPort(instance);

        if (port < 0)
            return Strings.get("stop.instance.noPort", instanceName);

        try {
            // TODO username password ????
            RemoteAdminCommand rac = new RemoteAdminCommand("_stop-instance",
                    host, port, false, "admin", null, logger);

            // notice how we do NOT send in the instance's name as an operand!!
            rac.executeCommand(new ParameterMap());
        }
        catch (CommandException ex) {
            return Strings.get("stop.instance.racError", instanceName,
                    ex.getLocalizedMessage());
        }

        return null;
    }
}
