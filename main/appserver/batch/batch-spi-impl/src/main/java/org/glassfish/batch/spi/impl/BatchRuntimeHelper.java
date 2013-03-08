/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.batch.spi.impl;

import com.ibm.jbatch.spi.*;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Helper class to get values for Batch Runtime. Follows
 * zero-config rules by using default values when the
 * batch-runtime config object is not present in
 * domain.xml
 *
 * @author Mahesh Kannan
 */
@Service
@RunLevel(StartupRunLevel.VAL)
public class BatchRuntimeHelper
    implements PostConstruct {

    @Inject
    @Optional
    private BatchRuntimeConfiguration batchRuntimeConfiguration;

    @Inject
    private ServerContext serverContext;

    @Inject
    private GlassFishBatchSecurityHelper glassFishBatchSecurityHelper;

    @Inject
    private Logger logger;

    private static final String DEFAULT_DATA_SOURCE_LOOKUP_NAME = "jdbc/__TimerPool";

    private static final String DEFAULT_EXECUTOR_SERVICE_LOOKUP_NAME = "java:comp/DefaultManagedExecutorService";

    private AtomicBoolean initialized = new AtomicBoolean(false);

    private static final String CREATE_TABLE_DDL_NAME = "batch_";

    public void checkAndInitializeBatchRuntime() {
        if (!initialized.get()) {
            synchronized (this) {
                if (!initialized.get()) {
                    initialized.set(true);
//                    try {
//                        Java2DBProcessorHelper java2DBProcessorHelper = new Java2DBProcessorHelper(this.getClass().getSimpleName());
//                        File ddlDir = new File(serverContext.getInstallRoot(), "/lib");
//
//                        //Temporary fix till batch_{db_vendor}.sql is part of the distribution
//                        File sqlFile = new File(ddlDir, "batch_derby.sql");
//                        if (sqlFile.exists()) {
////                            java2DBProcessorHelper.executeDDLStatement(ddlDir.getCanonicalPath() + CREATE_TABLE_DDL_NAME, getDataSourceLookupName());
//                            java2DBProcessorHelper.executeDDLStatement(sqlFile, getDataSourceLookupName());
//                        } else {
//                            logger.log(Level.WARNING, sqlFile.getAbsolutePath() + " does NOT exist");
//                        }
//                        initialized.set(true);
//                    } catch (Exception ex) {
//                        logger.log(Level.SEVERE, "Exception during table creation ", ex);
//                    }
                }
            }
        }
    }

    @Override
    public void postConstruct() {
        System.out.println("** GlassFishBatchExecutorServiceProvider.postConstruct() called");
        BatchSPIManager batchSPIManager = BatchSPIManager.getInstance();
        batchSPIManager.registerExecutorServiceProvider(new GlassFishBatchExecutorServiceProvider());
        batchSPIManager.registerBatchSecurityHelper(glassFishBatchSecurityHelper);

        try {
            DatabaseConfigurationBean databaseConfigurationBean = new DatabaseConfigurationBean();
            databaseConfigurationBean.setJndiName(getDataSourceLookupName());
            databaseConfigurationBean.setSchema(getSchemaName());
            batchSPIManager.registerDatabaseConfigurationBean(databaseConfigurationBean);
        } catch (DatabaseAlreadyInitializedException daiEx) {
            daiEx.printStackTrace();
        }
    }

    public String getDataSourceLookupName() {
        return (batchRuntimeConfiguration != null && batchRuntimeConfiguration.getDataSourceLookupName() != null)
                    ? batchRuntimeConfiguration.getDataSourceLookupName() : DEFAULT_DATA_SOURCE_LOOKUP_NAME;
    }

    private String getSchemaName() {
        return "APP";
    }

    public String getExecutorServiceLookupName() {
        return (batchRuntimeConfiguration != null && batchRuntimeConfiguration.getExecutorServiceLookupName() != null)
                    ? batchRuntimeConfiguration.getExecutorServiceLookupName() : DEFAULT_EXECUTOR_SERVICE_LOOKUP_NAME;
    }

    private class GlassFishBatchExecutorServiceProvider
        implements ExecutorServiceProvider {

        private volatile ExecutorService executorService;

        @Override
        public ExecutorService getExecutorService() {
            checkAndInitializeBatchRuntime();
            System.out.println("** GlassFishBatchExecutorServiceProvider.getExecutorService() called ");
            if (executorService == null) {
                synchronized (this) {
                    if (executorService == null) {
                        try {
                            InitialContext initialContext = new InitialContext();
                            executorService = (ExecutorService) initialContext.lookup(getExecutorServiceLookupName());
                            System.out.println("** [INITIALIZED] GlassFishBatchExecutorServiceProvider.getExecutorService() ==> "
                                + executorService);
                        } catch (NamingException nEx) {
                            nEx.printStackTrace();
                        }
                    }
                }
            }
            return executorService;
        }
    }

}
