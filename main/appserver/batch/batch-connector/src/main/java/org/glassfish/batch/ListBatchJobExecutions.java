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
@Service(name = "list-batch-job-executions")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.batch.job.executions")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "list-batch-job-executions",
                description = "List Batch Job Executions")
})
public class ListBatchJobExecutions
        extends AbstractListCommand {

    private static final String JOB_NAME = "jobName";

    private static final String EXECUTION_ID = "executionId";

    private static final String BATCH_STATUS = "batchStatus";

    private static final String EXIT_STATUS = "exitStatus";

    private static final String START_TIME = "startTime";

    private static final String END_TIME = "endTime";

    private static final String JOB_PARAMETERS = "jobParameters";

    private static final String STEP_COUNT = "stepCount";

    @Param(name = "executionid", primary = true)
    String executionId;

    @Override
    protected void executeCommand(AdminCommandContext context, Properties extraProps) {

        ColumnFormatter columnFormatter = new ColumnFormatter(getDisplayHeaders());
        List<Map<String, Object>> jobExecutions = new ArrayList<>();
        extraProps.put("list-batch-jobs", jobExecutions);
        for (JobExecution je : findJobExecutions()) {
            jobExecutions.add(handleJob(je, columnFormatter));
        }
        context.getActionReport().setMessage(columnFormatter.toString());
    }

    @Override
    protected final String[] getSupportedHeaders() {
        return new String[]{
                JOB_NAME, EXECUTION_ID, START_TIME, END_TIME, BATCH_STATUS, EXIT_STATUS, JOB_PARAMETERS, STEP_COUNT
        };
    }

    @Override
    protected final String[] getTerseHeaders() {
        return new String[]{JOB_NAME, EXECUTION_ID, START_TIME, END_TIME, BATCH_STATUS, EXIT_STATUS};
    }

    @Override
    protected String[] getLongHeaders() {
        return getSupportedHeaders();
    }

    private boolean isSimpleMode() {
        for (String h : getOutputHeaders()) {
            if (!JOB_NAME.equals(h)) {
                return false;
            }
        }
        return true;
    }

    private List<JobExecution> findJobExecutions() {
        List<JobExecution> jobExecutions = new ArrayList<>();
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        JobExecution jobExecution = jobOperator.getJobExecution(Long.valueOf(executionId));
        if (jobExecution != null)
            jobExecutions.add(jobExecution);

        return jobExecutions;
    }

//    private static List<JobExecution> getJobExecutionForInstance(long instId) {
//        JobOperator jobOperator = BatchRuntime.getJobOperator();
//        JobInstance jobInstance = null;
//        for (String jn : jobOperator.getJobNames()) {
//            List<JobInstance> exe = jobOperator.getJobInstances(jn, 0, Integer.MAX_VALUE - 1);
//            if (exe != null) {
//                for (JobInstance ji : exe) {
//                    if (ji.getInstanceId() == instId) {
//                        jobInstance = ji;
//                        break;
//                    }
//                }
//            }
//        }
//        List<JobExecution> jobExecutionList = BatchRuntime.getJobOperator().getJobExecutions(jobInstance);
//        return jobExecutionList == null
//                ? new ArrayList<JobExecution>() : jobExecutionList;
//    }

    private Map<String, Object> handleJob(JobExecution je, ColumnFormatter columnFormatter) {
        Map<String, Object> jobInfo = new HashMap<>();

        int jobParamIndex = -1;
        StringTokenizer st = new StringTokenizer("", "");
        String[] cfData = new String[getOutputHeaders().length];
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        for (int index = 0; index < getOutputHeaders().length; index++) {
            Object data = null;
            switch (getOutputHeaders()[index]) {
                case JOB_NAME:
                    data = jobOperator.getJobInstance(je.getInstanceId()).getJobName();
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
                    data = je.getStartTime();
                    break;
                case END_TIME:
                    data = je.getEndTime();
                    break;
                case JOB_PARAMETERS:
                    data = je.getJobParameters() == null ? new Properties() : je.getJobParameters();
                    jobParamIndex = index;
                    ColumnFormatter cf = new ColumnFormatter(new String[]{"KEY", "VALUE"});
                    for (Map.Entry e : ((Properties) data).entrySet())
                        cf.addRow(new String[]{e.getKey().toString(), e.getValue().toString()});
                    st = new StringTokenizer(cf.toString(), "\n");
                    break;
                case STEP_COUNT:
                    long exeId = Long.valueOf(executionId);
                    data = jobOperator.getStepExecutions(exeId) == null
                        ? 0 : jobOperator.getStepExecutions(exeId).size();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown header: " + getOutputHeaders()[index]);
            }
            jobInfo.put(getOutputHeaders()[index], data);
            cfData[index] = (jobParamIndex != index)
                    ? data.toString()
                    : (st.hasMoreTokens()) ? st.nextToken() : "";
        }
        columnFormatter.addRow(cfData);

        cfData = new String[getOutputHeaders().length];
        for (int i = 0; i < getOutputHeaders().length; i++)
            cfData[i] = "";
        while (st.hasMoreTokens()) {
            cfData[jobParamIndex] = st.nextToken();
            columnFormatter.addRow(cfData);
        }

        return jobInfo;
    }
}
