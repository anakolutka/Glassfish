
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
package org.glassfish.virtualization.commands;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.logging.Level;

/**
 * Registers a new virtual machine to this serverPool master.
 */
@Service(name="register-virtual-machine")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class RegisterVirtualMachine implements AdminCommand {

    @Param
    String group;

    @Param
    String machine;

    @Param
    String address;

    @Param
    String sshUser;

    @Param
    String installDir;

    @Param
    String cluster;

    @Param(primary = true)
    String virtualMachine;
    /* not ssh is for JRVE type of regsitration, since JRVE has limited ssh support.
     * 
     */
    @Param(optional = true, defaultValue = "false")
    boolean notssh;

    @Inject
    IAAS groups;

    @Inject
    RuntimeContext rtContext;

    @Override
    public void execute(AdminCommandContext context) {
        if (group==null) {
            context.getActionReport().failure(RuntimeContext.logger, "LibVirtGroup name cannot be null");
            return;
        }
        ServerPool targetGroup = groups.byName(group);
        if (targetGroup==null) {
            context.getActionReport().failure(RuntimeContext.logger, "Cannot find serverPool " + group);
            return;
        }
        try {
            VirtualMachine vm = targetGroup.vmByName(virtualMachine);
            if (vm!=null) {
                vm.setAddress(address);
                ActionReport report = context.getActionReport();
                // Node name is group_machine_virtualMachine;
                final String vmName = group + "_" + machine + "_" + virtualMachine;
                if (!notssh) { //default, i.e ssh = true
                    // create-node-ssh --nodehost $ip_address --installdir $GLASSFISH_HOME $node_name
                    rtContext.executeAdminCommand(report, "create-node-ssh", vmName, "nodehost", address,
                            "sshUser", sshUser, "installdir", installDir);

                    if (report.hasFailures()) {
                        return;
                    }
                    rtContext.executeAdminCommand(report, "create-instance", vmName + "Instance", "node", vmName,
                            "cluster", cluster);
                } else { //JRVE case, we jsut need a call to _create-node-implicit
                    rtContext.executeAdminCommand(report, "_create-node-implicit", address, "name", vmName,
                            "installdir", installDir);
                }
            }
        } catch(VirtException e) {
            RuntimeContext.logger.log(Level.SEVERE, e.getMessage(),e);
            context.getActionReport().failure(RuntimeContext.logger, e.getMessage());
        }


    }
}
