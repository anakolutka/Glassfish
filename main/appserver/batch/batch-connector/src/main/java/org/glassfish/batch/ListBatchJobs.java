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
package org.glassfish.batch;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.batch.operations.JobOperator;
import javax.batch.operations.exception.*;
import javax.batch.operations.exception.SecurityException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import java.util.*;

/**
 * Command to list batch jobs info
 *
 *         1      *             1      *
 * jobName --------> instanceId --------> executionId
 *
 * @author Mahesh Kannan
 */
@Service(name = "list-batch-jobs")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.batch.jobs")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "list-batch-jobs",
                description = "List Batch Jobs")
})
public class ListBatchJobs
        extends AbstractListCommand {

    private static final String JOB_NAME = "jobName";

    private static final String INSTANCE_COUNT = "instanceCount";

    private static final String INSTANCE_ID = "instanceId";

    private static final String EXECUTION_ID = "executionId";

    private static final String BATCH_STATUS = "batchStatus";

    private static final String EXIT_STATUS = "exitStatus";

    private static final String START_TIME = "startTime";

    private static final String END_TIME = "endTime";

    @Param(name = "jobname", optional = true, primary = true)
    String jobName;


    @Override
    protected void executeCommand(AdminCommandContext context, Properties extraProps)
        throws Exception {

        ColumnFormatter columnFormatter = new ColumnFormatter(getDisplayHeaders());
        if (isSimpleMode()) {
            extraProps.put("simpleMode", true);
            extraProps.put("listBatchJobs", findSimpleJobInfo(columnFormatter));
        } else {
            extraProps.put("simpleMode", false);
            List<Map<String, Object>> jobExecutions = new ArrayList<>();
            extraProps.put("listBatchJobs", jobExecutions);
            for (JobExecution je : findJobExecutions()) {
                jobExecutions.add(handleJob(je, columnFormatter));
            }
        }
        context.getActionReport().setMessage(columnFormatter.toString());
    }

    @Override
    protected final String[] getSupportedHeaders() {
        return new String[]{
                JOB_NAME, INSTANCE_COUNT, INSTANCE_ID, EXECUTION_ID, BATCH_STATUS,
                START_TIME, END_TIME, EXIT_STATUS
        };
    }

    @Override
    protected final String[] getTerseHeaders() {
        return new String[]{JOB_NAME, INSTANCE_COUNT};
    }

    @Override
    protected String[] getLongHeaders() {
        return getSupportedHeaders();
    }

    private boolean isSimpleMode() {
        for (String h : getOutputHeaders()) {
            if (!JOB_NAME.equals(h) && !INSTANCE_COUNT.equals(h)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Integer> findSimpleJobInfo(ColumnFormatter columnFormatter)
        throws javax.batch.operations.exception.SecurityException {

        Map<String, Integer> jobToInstanceCountMap = new HashMap<>();
        Set<String> jobNames = new HashSet<>();
        if (jobName != null)
            jobNames.add(jobName);
        else
            jobNames = BatchRuntime.getJobOperator().getJobNames();
        if (jobNames != null) {
            for (String jName : jobNames) {
                int size = getOutputHeaders().length;
                String[] data = new String[size];
                int instCount = BatchRuntime.getJobOperator().getJobInstanceCount(jName);
                for (int index = 0; index < size; index++) {
                    switch (getOutputHeaders()[index]) {
                        case JOB_NAME:
                            data[index] = jName;
                            break;
                        case INSTANCE_COUNT:
                            data[index] = "" + instCount;
                            break;
                        default:
                            throw new InternalError("Cannot handle " + getOutputHeaders()[index] + " in simple mode");
                    }
                }
                columnFormatter.addRow(data);
                jobToInstanceCountMap.put(jName, instCount);
            }
        }

        return jobToInstanceCountMap;
    }

    private List<JobExecution> findJobExecutions()
        throws SecurityException {
        List<JobExecution> jobExecutions = new ArrayList<>();
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        if (jobName != null) {
            List<JobInstance> exe = jobOperator.getJobInstances(jobName, 0, Integer.MAX_VALUE - 1);
            if (exe != null) {
                for (JobInstance ji : exe) {
                    jobExecutions.addAll(jobOperator.getExecutions(ji));
                }
            }
        } else {
            Set<String> jobNames = jobOperator.getJobNames();
            if (jobNames != null) {
                for (String jn : jobOperator.getJobNames()) {
                    List<JobInstance> exe = jobOperator.getJobInstances(jn, 0, Integer.MAX_VALUE - 1);
                    if (exe != null) {
                        for (JobInstance ji : exe) {
                            jobExecutions.addAll(jobOperator.getExecutions(ji));
                        }
                    }
                }
            }
        }

        return jobExecutions;
    }

    private Map<String, Object> handleJob(JobExecution je, ColumnFormatter columnFormatter)
        throws  SecurityException {
        Map<String, Object> jobInfo = new HashMap<>();

        String[] cfData = new String[getOutputHeaders().length];
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        for (int index = 0; index < getOutputHeaders().length; index++) {
            Object data = null;
            switch (getOutputHeaders()[index]) {
                case JOB_NAME:
                    data = jobOperator.getJobInstance(je.getInstanceId()).getJobName();
                    break;
                case INSTANCE_COUNT:
                    data = jobOperator.getJobInstanceCount(jobOperator.getJobInstance(je.getInstanceId()).getJobName());
                    break;
                case INSTANCE_ID:
                    data = je.getInstanceId();
                    break;
                case EXECUTION_ID:
                    data = je.getExecutionId();
                    break;
                case BATCH_STATUS:
                    data = je.getBatchStatus();
                    break;
                case EXIT_STATUS:
                    data = je.getExitStatus();
                    break;
                case START_TIME:
                    data = je.getStartTime().getTime();
                    cfData[index] = je.getStartTime().toString();
                    break;
                case END_TIME:
                    data = je.getEndTime().getTime();
                    cfData[index] = je.getEndTime().toString();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown header: " + getOutputHeaders()[index]);
            }
            jobInfo.put(getOutputHeaders()[index], data);
            if (cfData[index] == null)
                cfData[index] = data.toString();
        }
        columnFormatter.addRow(cfData);

        return jobInfo;
    }
}
