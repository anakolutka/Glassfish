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
package org.glassfish.admin.rest;

import java.util.HashMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import com.sun.jersey.spi.container.ContainerRequest;
import javax.ws.rs.DELETE;
import org.glassfish.admin.rest.provider.CommandResourceGetResult;



import org.glassfish.api.ActionReport;

/**
 *
 * @author ludovic champenois ludo@dev.java.net
 * Code moved from generated classes to here. Gen code inherits from this template class
 * that contains the logic for mapped commands RS Resources
 *
 */
public class TemplateCommandDeleteResource extends TemplateExecCommand{

    public TemplateCommandDeleteResource(String resourceName, String commandName, String commandMethod, String commandAction, String commandDisplayName, HashMap<String, String> m, boolean b) {
        super ( resourceName,  commandName,  commandMethod,commandAction,commandDisplayName,  m,  b);
        parameterType = Constants.MESSAGE_PARAMETER;
    }

    @DELETE
    @Consumes({
        MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML,
        MediaType.APPLICATION_FORM_URLENCODED})
    public Response executeCommand(HashMap<String, String> data) {
        try {
            if (data.containsKey("error")) {
                String errorMessage = localStrings.getLocalString("rest.request.parsing.error",
                        "Unable to parse the input entity. Please check the syntax.");
                return ResourceUtil.getResponse(400, /*parsing error*/
                        errorMessage, requestHeaders, uriInfo);
            }

            if (commandParams != null) {
//formulate parent-link attribute for this command resource
//Parent link attribute may or may not be the id/target attribute
                if (isLinkedToParent) {
                    ResourceUtil.resolveParentParamValue(commandParams, uriInfo);
                }

                data.putAll(commandParams);
            }

            ResourceUtil.addQueryString(((ContainerRequest) requestHeaders).getQueryParameters(), data);
            ResourceUtil.adjustParameters(data);
            ResourceUtil.purgeEmptyEntries(data);
                        String typeOfResult = requestHeaders.getAcceptableMediaTypes().get(0).getSubtype();

            ActionReport actionReport = ResourceUtil.runCommand(commandName, data, RestService.getHabitat(),typeOfResult);
            ActionReport.ExitCode exitCode = actionReport.getActionExitCode();

            if (exitCode == ActionReport.ExitCode.SUCCESS) {
                String successMessage = localStrings.getLocalString("rest.request.success.message",
                        "{0} of {1} executed successfully.", new Object[]{commandMethod, uriInfo.getAbsolutePath()});
                return ResourceUtil.getResponse(200, /*200 - ok*/
                        successMessage, requestHeaders, uriInfo);
            }

            String errorMessage = actionReport.getMessage();
            return ResourceUtil.getResponse(400, /*400 - bad request*/
                    errorMessage, requestHeaders, uriInfo);
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
//Handle POST request without any entity(input).
//Do not care what the Content-Type is.

    @DELETE
    public Response executeCommand() {
        try {
            return executeCommand(new HashMap<String, String>());
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
//hack-1 : support delete method for html
//Currently, browsers do not support delete method. For html media,
//delete operations can be supported through POST. Redirect html
//client POST request for delete operation to DELETE method.

//In case of delete command reosurce, we will also create post method
//which simply forwards the request to delete method. Only in case of
//html client delete request is routed through post. For other clients
//delete request is directly handled by delete method.
    @POST
    @Consumes({
        MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML,
        MediaType.APPLICATION_FORM_URLENCODED})
    public Response hack(HashMap<String, String> data) {
        if ((data.containsKey("operation"))
                && (data.get("operation").equals("__deleteoperation"))) {
            data.remove("operation");
        }
        return executeCommand(data);
    }

    @GET
    @Produces({
        MediaType.TEXT_HTML,
        MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public CommandResourceGetResult get() {
        try {
            return new CommandResourceGetResult(resourceName, commandName, commandDisplayName, commandMethod, commandAction, options());
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
