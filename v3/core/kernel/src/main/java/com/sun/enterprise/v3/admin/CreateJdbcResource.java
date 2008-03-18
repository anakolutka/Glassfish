/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package com.sun.enterprise.v3.admin;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PerLookup;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.universal.glassfish.SystemPropertyConstants;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.HashMap;
import java.util.Properties;

/**
 * Create JDBC Resource Command
 * 
 */
@Service(name="create-jdbc-resource")
@Scoped(PerLookup.class)
@I18n("create.jdbc.resource")
public class CreateJdbcResource implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateJdbcResource.class);    

    @Param(name="connectionpoolid")
    String connectionPoolId;

    @Param(optional=true)
    String enabled = Boolean.TRUE.toString();

    @Param(optional=true)
    String description;
    
    @Param(name="property", optional=true)
    Properties properties;
    
    @Param(optional=true)
    String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;

    @Param(name="jndi_name", primary=true)
    String jndiName;
    
    @Inject
    Resources resources;
    
    @Inject
    Server[] servers;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        Server targetServer = ResourceUtils.getTargetServer(servers, target);
        
        JDBCResourceManager jdbcMgr = new JDBCResourceManager();
        HashMap attrList = new HashMap();
        attrList.put(ResourceConstants.JNDI_NAME, jndiName);
        attrList.put(ResourceConstants.POOL_NAME, connectionPoolId);
        attrList.put(ServerTags.DESCRIPTION, description);
        attrList.put(ResourceConstants.ENABLED, enabled);
        ResourceStatus rs;
 
        try {
            rs = jdbcMgr.create(resources, attrList, properties, targetServer);
        } catch(Exception e) {
            report.setMessage(localStrings.getLocalString("create.jdbc.resource.failed",
                    "JDBC resource {0} creation failed", jndiName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        if (rs.getStatus() == ResourceStatus.FAILURE) {
            ec = ActionReport.ExitCode.FAILURE;
            report.setMessage(localStrings.getLocalString("create.jdbc.resource.failed",
                    "JDBC resource {0} creation failed", jndiName));
            if (rs.getException() != null)
                report.setFailureCause(rs.getException());
        } else {
            report.setMessage(localStrings.getLocalString("create.jdbc.resource.success",
                    "JDBC resource {0} created successfully", jndiName));
        }
        report.setActionExitCode(ec);
    }
}
