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
package com.sun.enterprise.v3.admin.commands;
import com.sun.enterprise.util.i18n.StringManager;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Collection;
import java.util.Date;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.progress.JobInfo;
import org.glassfish.api.admin.progress.JobInfos;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.security.services.common.SubjectUtil;
import org.jvnet.hk2.annotations.Service;


/**
 * This command will list the jobs related information
 * Currently it prints the jobId, name, time of execution,user and the state
 *
 * @author Bhakti Mehta
 */
@Service(name="list-jobs")
@PerLookup
@I18n("list-jobs")
public class ListJobsCommand implements AdminCommand,AdminCommandSecurity.AccessCheckProvider {
    
    private ActionReport report;

    @Inject
    private JobManager jobManagerService;

    protected final List<AccessRequired.AccessCheck> accessChecks = new ArrayList<AccessRequired.AccessCheck>();

    private final String JOBS_FILE = "jobs.xml";

    @Param(optional = true, primary = true)
    String jobID;

    @Inject
    private ServerEnvironment serverEnvironment;

    protected static final String TITLE_NAME = "NAME";
    protected static final String TITLE_JOBID = "JOB ID";
    protected static final String TITLE_TIME = "TIME";
    protected static final String TITLE_STATE = "STATE";
    protected static final String TITLE_USER = "USER";
    protected static final String TITLE_NONE = "Nothing to list.";
    public static final String NAME = "jobName";
    public static final String ID = "jobId";
    public static final String DATE = "executionDate";
    public static final String CODE = "exitCode";
    public static final String USER = "user";
    public static final String MESSAGE = "message";


    final private static StringManager localStrings =
            StringManager.getManager(ListJobsCommand.class);

    @Override
    public void execute(AdminCommandContext context) {



        List<JobInfo> jobInfoList = new ArrayList<JobInfo>();

        if (jobID != null) {
            Job oneJob = jobManagerService.get(jobID);
            JobInfo info = null;

            if (oneJob != null) {
                List<String> userList =  SubjectUtil.getUsernamesFromSubject(oneJob.getSubject());
                String message = oneJob.getActionReport() == null ? "" : oneJob.getActionReport().getMessage();
                info = new JobInfo(oneJob.getId(),oneJob.getName(),oneJob.getCommandExecutionDate(),oneJob.getState().name(),userList.get(0),message,oneJob.getJobsFile());

            }  else {
                if (jobManagerService.getCompletedJobs() != null) {
                    info = (JobInfo) jobManagerService.getCompletedJobForId(jobID);
                }
            }

          if (info != null && !skipJob(info.jobName)) {
              jobInfoList.add(info);
          }

        }  else {

            for (Iterator<Job> iterator = jobManagerService.getJobs(); iterator.hasNext(); ) {
                Job job = iterator.next();
                if (!skipJob(job.getName())) {
                    List<String> userList =  SubjectUtil.getUsernamesFromSubject(job.getSubject());
                    String message = job.getActionReport() == null ? "" : job.getActionReport().getMessage();
                    jobInfoList.add(new JobInfo(job.getId(),job.getName(),job.getCommandExecutionDate(),job.getState().name(),userList.get(0),message,job.getJobsFile()));
                }
            }

            JobInfos completedJobs = jobManagerService.getCompletedJobs();
            if (completedJobs != null ) {
                for (JobInfo info : completedJobs.getJobInfoList()) {
                    if (!skipJob(info.jobName)) {
                        jobInfoList.add(info);
                    }
                }
            }
        }

        display(jobInfoList,context);

    }

    public static boolean skipJob(String name) {
        return name == null || "attach".equals(name) || name.startsWith("_");
    }


    @Override
    public Collection<? extends AccessRequired.AccessCheck> getAccessChecks() {
        accessChecks.add(new AccessRequired.AccessCheck("jobs/job/$jobID","READ"));
        return accessChecks;
    }

    public void display(List<JobInfo> jobInfoList, AdminCommandContext context) {
        report = context.getActionReport();


        int longestName = TITLE_NAME.length();
        int longestJobId = TITLE_JOBID.length();
        int longestTime = TITLE_TIME.length();
        int longestState = TITLE_STATE.length();
        int longestUser = TITLE_USER.length();

        for (JobInfo job :jobInfoList) {
                   int  jobId = job.jobId.length();
                   int time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(job.commandExecutionDate).length();
                   int name = job.jobName.length();
                   int state = job.exitCode.length();
                   int user = job.user.length();

                   if (name > longestName)
                       longestName = name;
                   if (time > longestTime)
                       longestTime = time;
                   if (jobId > longestJobId)
                       longestJobId = jobId;
                   if (state> longestState)
                       longestState = state;

               }

               if (jobInfoList.size() < 1) {
                   report.setMessage(TITLE_NONE);

               }
               longestName += 2;
               longestJobId += 2;
               longestState += 2;
               longestTime += 2;
               longestUser += 2;


               String formattedLine =
                       "%-" + longestName
                               + "s %-" + longestJobId
                               + "s %-" + longestTime
                               + "s %-" + longestState
                               + "s %-" + longestUser
                               + "s";


               // no linefeed at the end!!!
               boolean first = true;
               MessagePart topMsg = report.getTopMessagePart();
               Properties properties = report.getExtraProperties();
               if (properties == null) {
                   properties = new Properties();
                   report.setExtraProperties(properties);
               }
               Collection<Map<String, Object>> details = new ArrayList<Map<String, Object>>();
               properties.put("jobs", details);
               for (JobInfo info : jobInfoList) {
                   if (first)    {
                       topMsg.setMessage(String.format(formattedLine, TITLE_NAME, TITLE_JOBID, TITLE_TIME, TITLE_STATE,TITLE_USER ));
                       first = false;
                   }

                   MessagePart msg = topMsg.addChild();
                   msg.setMessage(String.format(formattedLine, info.jobName, info.jobId,  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(info.commandExecutionDate), info.exitCode,info.user));
                   Map<String, Object> detail = new HashMap<String, Object>();
                   details.add(detail);
                   detail.put(NAME, info.jobName);
                   detail.put(ID, info.jobId);
                   detail.put(DATE, new Date(info.commandExecutionDate));
                   detail.put(CODE, info.exitCode);
                   detail.put(MESSAGE, info.message);
                   detail.put(USER, info.user);
               }

               report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

}


