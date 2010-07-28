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

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.SshConnector;
import com.sun.enterprise.config.serverbeans.SshAuth;

import com.sun.enterprise.util.StringUtils;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import java.util.logging.Logger;
import java.io.File;

/**
 * Remote AdminCommand to validate a config Node. This command is run only on DAS.
 * This command does the following:
 *
 * If the node does not exist it returns an error
 *
 * If parameters are passed to the command, such as nodehost, then it verifies
 * the parameters passed to the command match what is in the config. If the
 * config does not match the passed parameters then it is an error -- unless
 * the config has no value.
 *
 * If the node exists, but does not have some of the attributes passed to
 * the command, then the node is updated with the values passed to the command.
 *
 * @author Joe Di Pol
 */
@Service(name = "_validate-node")
@I18n("validate.node")
@Scoped(PerLookup.class)
@Cluster({RuntimeType.DAS})
public class ValidateNodeCommand implements AdminCommand {

    @Inject
    private Nodes nodes;

    @Inject
    private CommandRunner cr;

    @Param(name="name", primary = true)
    private String name;

    @Param(name="nodedir", optional=true)
    private String nodedir;

    @Param(name="nodehost", optional=true)
    private String nodehost;

    @Param(name = "installdir", optional=true)
    private String installdir;

    @Param(name="sshport", optional=true)
    private String sshport;

    @Param(name="sshuser", optional=true)
    private String sshuser;

    @Param(name="sshnodehost", optional=true)
    private String sshnodehost;

    @Param(name="sshkeyfile", optional=true)
    private String sshkeyfile;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        logger.fine(Strings.get("Validating node {0}", name));
        Node node = nodes.getNode(name);
        if (node == null) {
            //node doesn't exist
            String msg = Strings.get("noSuchNode", name);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        try {
            validateNode(node);
        } catch (CommandValidationException e) {
            logger.warning(e.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
            return;
        }

        logger.fine(Strings.get("Node {0} is valid. Updating if needed", name));

        // What is there in the node is valid. Now go update anything that
        // was not there.
        CommandInvocation ci = cr.getCommandInvocation("_update-node", report);
        ParameterMap map = new ParameterMap();
        map.add("DEFAULT", name);
        map.add("installdir", installdir);
        map.add("nodehost", nodehost);
        map.add("nodedir", nodedir);
        map.add("sshport", sshport);
        map.add("sshuser", sshuser);
        map.add("sshkeyfile", sshkeyfile);
        ci.parameters(map);
        ci.execute();
    }

    public void validateNode(final Node node) throws
            CommandValidationException {

        String value = null;

        value = node.getNodeDir();
        validatePath("nodedir", nodedir, value);

        value = node.getNodeHost();
        validateHostname("nodehost", nodehost, value);

        value = node.getInstallDir();
        validatePath("installdir", installdir, value);

        SshConnector sshc = node.getSshConnector();

        if (sshc == null) {
            return;
        }

        value = sshc.getSshPort();
        validateString("sshport", sshport, value, false);

        value = sshc.getSshHost();
        validateHostname("sshnodehost", sshnodehost, value);

        SshAuth ssha = sshc.getSshAuth();

        if (ssha == null) {
            return;
        }

        value = ssha.getUserName();
        validateString("sshuser", sshuser, value, false);

        value = ssha.getKeyfile();
        validatePath("sshkeyfile", sshkeyfile, value);
    }

    private void validatePath(String propname, String value, String configValue)
            throws CommandValidationException {

        if (!StringUtils.ok(value) || !StringUtils.ok(configValue)) {
            // If no value was passed via the CLI then we don't check it since
            // the caller doesn't want it validated.
            // If no value exists in the config, then we don't check it since
            // we will update it.
            return;
        }

        File valueFile = new File(value);
        File configValueFile = new File(configValue);

        if ( !valueFile.equals(configValueFile) ) {
            throw new CommandValidationException(
                Strings.get("attribute.mismatch",
                               propname, value, configValue));
        }
    }

    private void validateHostname(String propname,
            String value, String configValue)
            throws CommandValidationException {

        // To validate if two hostnames match we just do a case insensitive
        // string comparison.
        validateString(propname, value, configValue, true);
    }

    private void validateString(String propname,
            String value, String configValue, boolean ignorecase)
            throws CommandValidationException {

        if (!StringUtils.ok(value) || !StringUtils.ok(configValue)) {
            // If no value was passed via the CLI then we don't check it since
            // the caller doesn't want it validated.
            // If no value exists in the config, then we don't check it since
            // we will update it.
            return;
        }

        boolean match = false;
        if (ignorecase) {
            match = value.equalsIgnoreCase(configValue);
        } else {
            match = value.equals(configValue);
        }

        if ( !match ) {
            throw new CommandValidationException(
                Strings.get("attribute.mismatch",
                            propname, value, configValue));
        }
    }
}