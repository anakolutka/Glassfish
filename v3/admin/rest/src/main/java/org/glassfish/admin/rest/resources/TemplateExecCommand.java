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

package org.glassfish.admin.rest.resources;

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.glassfish.admin.rest.CliFailureException;
import org.glassfish.admin.rest.ResourceUtil;
import org.glassfish.admin.rest.RestService;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.provider.MethodMetaData;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ParameterMap;

/**
 *
 * @author ludo
 */
public class TemplateExecCommand {
   public final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(TemplateExecCommand.class);
    @Context
    protected HttpHeaders requestHeaders;
    @Context
    protected UriInfo uriInfo;
    protected String resourceName;
    protected String commandName;
    protected String commandDisplayName;
    protected String commandMethod;
    protected String commandAction;
//    protected HashMap<String, String> commandParams = null;
    protected boolean isLinkedToParent = false;
    /* * parameterType the type of parameter. Possible values are
     *        Constants.QUERY_PARAMETER and Constants.MESSAGE_PARAMETER
     *
     */
    protected int parameterType;
    public TemplateExecCommand(String resourceName, String commandName, String commandMethod, String commandAction, String commandDisplayName, 
             boolean isLinkedToParent) {
        this.resourceName = resourceName;
        this.commandName = commandName;
        this.commandMethod = commandMethod;
        this.commandAction = commandAction;
        this.commandDisplayName = commandDisplayName;
        this.isLinkedToParent = isLinkedToParent;

    }
    @OPTIONS
    @Produces({
        MediaType.APPLICATION_JSON,
        "text/html;qs=2",
        MediaType.APPLICATION_XML})
    public OptionsResult options() {
        OptionsResult optionsResult = new OptionsResult(resourceName);
        try {
            //command method metadata
            MethodMetaData methodMetaData = ResourceUtil.getMethodMetaData(
                    commandName, getCommandParams(), parameterType , RestService.getHabitat(), RestService.logger);
            optionsResult.putMethodMetaData(commandMethod, methodMetaData);
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return optionsResult;
    }

    protected ActionReportResult executeCommand(ParameterMap data) {
        ActionReport actionReport = ResourceUtil.runCommand(commandName, data, RestService.getHabitat(),
                ResourceUtil.getResultType(requestHeaders));
        ActionReport.ExitCode exitCode = actionReport.getActionExitCode();
        ActionReportResult results = new ActionReportResult(commandName, actionReport, options());

        if (exitCode != ActionReport.ExitCode.FAILURE) {
            results.setStatusCode(200); /*200 - ok*/
        } else {
	    Throwable ex = actionReport.getFailureCause();
	    ex = (ex == null) ?
		new CliFailureException(actionReport.getMessage()) :
		new CliFailureException(actionReport.getMessage(), ex);
            throw (RuntimeException) ex;
        }

        return results;
    }

    /*override it
     *
     * 
     */
    protected HashMap<String, String> getCommandParams() {
        return null;
    }
    protected void processCommandParams(ParameterMap data) {
        HashMap<String, String> commandParams = getCommandParams();
        if (commandParams != null) {
            //formulate parent-link attribute for this command resource
            //Parent link attribute may or may not be the id/target attribute
            if (isLinkedToParent) {
                ResourceUtil.resolveParentParamValue(commandParams, uriInfo);
            }
            for (Map.Entry<String, String> entry : commandParams.entrySet()) {
                data.add(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void addQueryString(MultivaluedMap<String, String> qs, ParameterMap data) {
        for (Map.Entry<String, List<String>> entry : qs.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                data.add(key, value);
            }
        }
    }

    protected void adjustParameters(ParameterMap data) {
        if (data != null) {
            if (!(data.containsKey("DEFAULT"))) {
                boolean isRenamed = renameParameter(data, "name", "DEFAULT");
                if (!isRenamed) {
                    renameParameter(data, "id", "DEFAULT");
                }
            }
        }
    }

    protected boolean renameParameter(ParameterMap data, String parameterToRename, String newName) {

        if ((data.containsKey(parameterToRename))) {
            List<String> value = data.get(parameterToRename);
            data.remove(parameterToRename);
            data.set(newName, value);
            return true;
        }
        return false;
    }

    protected void purgeEmptyEntries(ParameterMap data) {
        Set<Entry<String, List<String>>> entries = data.entrySet();
        for (Entry<String, List<String>> entry : entries) {
            if ((entry.getValue() == null) || (entry.getValue().isEmpty())) {
                data.remove(entry.getKey());// CME ?
            }
        }
    }
}
