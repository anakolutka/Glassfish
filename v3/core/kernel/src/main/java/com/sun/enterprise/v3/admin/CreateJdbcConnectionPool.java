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
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.JdbcConnectionPool;

import java.util.HashMap;
import java.util.Properties;

/**
 * Create JDBC Connection Pool Command
 * 
 */
@Service(name="create-jdbc-connection-pool")
@Scoped(PerLookup.class)
@I18n("create.jdbc.connection.pool")
public class CreateJdbcConnectionPool implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateJdbcConnectionPool.class);    

    @Param(name="datasourceclassname")
    String datasourceclassname;

    @Param(name="restype", optional=true)
    String restype;

    @Param(name="steadypoolsize", optional=true)
    String steadypoolsize = "8";

    @Param(name="maxpoolsize", optional=true)
    String maxpoolsize = "32";
    
    @Param(name="maxwait", optional=true)
    String maxwait = "60000";

    @Param(name="poolresize", optional=true)
    String poolresize = "2";
    
    @Param(name="idletimeout", optional=true)
    String idletimeout = "300";
        
    @Param(name="isolationlevel", optional=true)
    String isolationlevel;
            
    @Param(name="isisolationguaranteed", optional=true)
    String isisolationguaranteed = Boolean.TRUE.toString();
                
    @Param(name="isconnectvalidatereq", optional=true)
    String isconnectvalidatereq = Boolean.FALSE.toString();
    
    @Param(name="validationmethod", optional=true)
    String validationmethod = "auto-commit";
    
    @Param(name="validationtable", optional=true)
    String validationtable;
    
    @Param(name="failconnection", optional=true)
    String failconnection = Boolean.FALSE.toString();
    
    @Param(name="allownoncomponentcallers", optional=true)
    String allownoncomponentcallers = Boolean.FALSE.toString();
    
    @Param(name="nontransactionalconnections", optional=true)
    String nontransactionalconnections = Boolean.FALSE.toString();
    
    @Param(name="validateatmostonceperiod", optional=true)
    String validateatmostonceperiod = "0";
    
    @Param(name="leaktimeout", optional=true)
    String leaktimeout = "0";
    
    @Param(name="leakreclaim", optional=true)
    String leakreclaim = Boolean.FALSE.toString();
    
    @Param(name="creationretryattempts", optional=true)
    String creationretryattempts = "0";
    
    @Param(name="creationretryinterval", optional=true)
    String creationretryinterval = "10";
    
    @Param(name="statementtimeout", optional=true)
    String statementtimeout = "-1";
    
    @Param(name="lazyconnectionenlistment", optional=true)
    String lazyconnectionenlistment = Boolean.FALSE.toString();
    
    @Param(name="lazyconnectionassociation", optional=true)
    String lazyconnectionassociation = Boolean.FALSE.toString();
    
    @Param(name="associatewiththread", optional=true)
    String associatewiththread = Boolean.FALSE.toString();
    
    @Param(name="matchconnections", optional=true)
    String matchconnections = Boolean.FALSE.toString();
    
    @Param(name="maxconnectionusagecount", optional=true)
    String maxconnectionusagecount = "0";
    
    @Param(name="wrapjdbcobjects", optional=true)
    String wrapjdbcobjects = Boolean.FALSE.toString();
    
    @Param(name="description", optional=true)
    String description;
    
    @Param(name="property", optional=true)
    Properties properties;
    
    @Param(name="jdbc_connection_pool_id", primary=true)
    String jdbc_connection_pool_id; 
  
    @Inject
    Resources resources;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
       final ActionReport report = context.getActionReport();

        HashMap attrList = new HashMap();
        attrList.put(ResourceConstants.CONNECTION_POOL_NAME, jdbc_connection_pool_id);
        attrList.put(ResourceConstants.DATASOURCE_CLASS, datasourceclassname);
        attrList.put(ServerTags.DESCRIPTION, description);
        attrList.put(ResourceConstants.RES_TYPE, restype);
        attrList.put(ResourceConstants.STEADY_POOL_SIZE, steadypoolsize);
        attrList.put(ResourceConstants.MAX_POOL_SIZE, maxpoolsize);
        attrList.put(ResourceConstants.MAX_WAIT_TIME_IN_MILLIS, maxwait);
        attrList.put(ResourceConstants.POOL_SIZE_QUANTITY, poolresize);
        attrList.put(ResourceConstants.IDLE_TIME_OUT_IN_SECONDS, idletimeout);
        attrList.put(ResourceConstants.TRANS_ISOLATION_LEVEL, isolationlevel);
        attrList.put(ResourceConstants.IS_ISOLATION_LEVEL_GUARANTEED, isisolationguaranteed);
        attrList.put(ResourceConstants.IS_CONNECTION_VALIDATION_REQUIRED, isconnectvalidatereq);
        attrList.put(ResourceConstants.CONNECTION_VALIDATION_METHOD, validationmethod);
        attrList.put(ResourceConstants.VALIDATION_TABLE_NAME, validationtable);
        attrList.put(ResourceConstants.CONN_FAIL_ALL_CONNECTIONS, failconnection);
        attrList.put(ResourceConstants.NON_TRANSACTIONAL_CONNECTIONS, nontransactionalconnections);
        attrList.put(ResourceConstants.ALLOW_NON_COMPONENT_CALLERS, allownoncomponentcallers);
        attrList.put(ResourceConstants.VALIDATE_ATMOST_ONCE_PERIOD, validateatmostonceperiod);
        attrList.put(ResourceConstants.CONNECTION_LEAK_TIMEOUT, leaktimeout);
        attrList.put(ResourceConstants.CONNECTION_LEAK_RECLAIM, leakreclaim);
        attrList.put(ResourceConstants.CONNECTION_CREATION_RETRY_ATTEMPTS, creationretryattempts);
        attrList.put(ResourceConstants.CONNECTION_CREATION_RETRY_INTERVAL, creationretryinterval);
        attrList.put(ResourceConstants.STATEMENT_TIMEOUT, statementtimeout);
        attrList.put(ResourceConstants.LAZY_CONNECTION_ASSOCIATION, lazyconnectionassociation);
        attrList.put(ResourceConstants.LAZY_CONNECTION_ENLISTMENT, lazyconnectionenlistment);
        attrList.put(ResourceConstants.ASSOCIATE_WITH_THREAD, associatewiththread);
        attrList.put(ResourceConstants.MATCH_CONNECTIONS, matchconnections);
        attrList.put(ResourceConstants.MAX_CONNECTION_USAGE_COUNT, maxconnectionusagecount);
        attrList.put(ResourceConstants.WRAP_JDBC_OBJECTS, wrapjdbcobjects);
        
        ResourceStatus rs;
 
        try {
            JDBCConnectionPoolManager connPoolMgr = new JDBCConnectionPoolManager();
            rs = connPoolMgr.create(resources, attrList, properties, jdbc_connection_pool_id);
        } catch(Exception e) {
            report.setMessage(localStrings.getLocalString("create.jdbc.connection.pool.fail",
                    "JDBC connection pool {0} creation failed", jdbc_connection_pool_id));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        if (rs.getStatus() == ResourceStatus.FAILURE) {
            ec = ActionReport.ExitCode.FAILURE;
            report.setMessage(localStrings.getLocalString("create.jdbc.connection.pool.fail",
                    "JDBC connection pool {0} creation failed", jdbc_connection_pool_id));
            if (rs.getException() != null)
                report.setFailureCause(rs.getException());
        } else {
            report.setMessage(localStrings.getLocalString("create.jdbc.connection.pool.success",
                    "JDBC connection pool {0} created successfully", jdbc_connection_pool_id));
        }
        report.setActionExitCode(ec);
    }
}