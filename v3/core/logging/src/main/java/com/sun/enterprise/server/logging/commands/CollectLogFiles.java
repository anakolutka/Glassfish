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

package com.sun.enterprise.server.logging.commands;

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.server.logging.GFFileHandler;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: naman
 * Date: 6 Jul, 2010
 * Time: 3:27:24 PM
 * To change this template use File | Settings | File Templates.
 */
@Cluster({RuntimeType.DAS})
@Service(name = "collect-log-files")
@Scoped(PerLookup.class)
@I18n("collect.log.files")
public class CollectLogFiles implements AdminCommand {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CollectLogFiles.class);

    private static final Logger logger =
            LogDomains.getLogger(CollectLogFiles.class, LogDomains.CORE_LOGGER);

    @Param(optional = true)
    String target = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;

    @Param
    private String outputFilePath;

    @Inject
    Domain domain;

    @Inject
    private Habitat habitat;

    @Inject
    GFFileHandler gf;


    public void execute(AdminCommandContext context) {

        try {

            final ActionReport report = context.getActionReport();

            File tempFile = File.createTempFile("download", "tmp");
            tempFile.delete();
            tempFile.mkdirs();

            Properties props = initFileXferProps();

            Server targetServer = domain.getServerNamed(target);


            List<String> instancesForReplication = new ArrayList<String>();

            File outputFile = new File(outputFilePath);
            if(!outputFile.exists()){
                boolean created = outputFile.mkdir();
                if(!created) {
                    report.setMessage("Outputfilepath Doen not exists. Please enter correct value for Outputfilepath.");
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);                    
                }
            }

            if (targetServer != null && targetServer.isDas()) {
                File file = new File(outputFilePath + File.separator + "server");
                file.mkdir();

                File logFile = gf.getCurrentLogFile();

                File toFile = new File(file, logFile.getName());

                FileInputStream from = null;
                FileOutputStream to = null;

                from = new FileInputStream(logFile);
                to = new FileOutputStream(toFile);
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = from.read(buffer)) != -1)
                    to.write(buffer, 0, bytesRead); // write

                if (!toFile.exists()) {
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                }
            } else {
                if (targetServer != null) {
                    instancesForReplication.add(target);
                } else {
                    com.sun.enterprise.config.serverbeans.Cluster cluster = domain.getClusterNamed(target);
                    if (cluster != null) {
                        instancesForReplication.add(target);
                    }
                }

                ActionReport.ExitCode result = ClusterOperationUtil.replicateCommand("_get-log-file",
                        FailurePolicy.Error,
                        FailurePolicy.Error,
                        instancesForReplication,
                        context,
                        new ParameterMap(),
                        habitat, tempFile);

                Payload.Outbound outboundPayload = context.getOutboundPayload();
                for (File instanceSubDir : tempFile.listFiles()) {
                    for (File fileFromInstance : instanceSubDir.listFiles()) {

                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "About to download artifact " + fileFromInstance.getAbsolutePath());
                        }
                        outboundPayload.attachFile(
                                "application/octet-stream",
                                tempFile.toURI().relativize(fileFromInstance.toURI()),
                                "files",
                                props,
                                fileFromInstance);
                    }
                }
            }

            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
        catch (Exception e) {
            final String errorMsg = localStrings.getLocalString(
                    "download.errDownloading", "Error while downloading generated files");
            logger.log(Level.SEVERE, errorMsg, e);
            ActionReport report = context.getActionReport();
            boolean reportErrorsInTopReport = false;
            if (!reportErrorsInTopReport) {
                report = report.addSubActionsReport();
                report.setActionExitCode(ActionReport.ExitCode.WARNING);
            } else {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
            report.setMessage(errorMsg);
            report.setFailureCause(e);
        }
    }

    private Properties initFileXferProps() {
        final Properties props = new Properties();
        props.setProperty("file-xfer-root", outputFilePath.replace("\\", "/"));
        return props;
    }
}
