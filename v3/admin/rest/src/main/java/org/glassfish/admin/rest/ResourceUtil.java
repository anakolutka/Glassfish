/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
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


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.Set;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;

import org.glassfish.admin.rest.provider.MethodMetaData;
import org.glassfish.admin.rest.provider.ParameterMetaData;
import static org.glassfish.admin.rest.Util.*;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.api.Param;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;


/**
 * Resource utilities class. Used by resource templates,
 * <code>TemplateListOfResource</code> and <code>TemplateResource</code>
 *
 * @author Rajeshwar Patil
 */
public class ResourceUtil {
    //TODO this is copied from org.jvnet.hk2.config.Dom. If we are not able to encapsulate the conversion in Dom, need to make sure that the method convertName is refactored into smaller methods such that trimming of prefixes stops. We will need a promotion of HK2 for this.
    static final Pattern TOKENIZER;
    static {
        String pattern = or(
                split("x","X"),     // AbcDef -> Abc|Def
                split("X","Xx"),    // USArmy -> US|Army
                //split("\\D","\\d"), // SSL2 -> SSL|2
                split("\\d","\\D")  // SSL2Connector -> SSL|2|Connector
        );
        pattern = pattern.replace("x","\\p{Lower}").replace("X","\\p{Upper}");
        TOKENIZER = Pattern.compile(pattern);
    }

    private ResourceUtil() {
        
    }

    /**
     * Adjust the input parameters. In case of POST and DELETE methods, user
     * can provide name, id or DEFAULT parameter for primary parameter(i.e the
     * object to create or delete). This method is used to rename primary
     * parameter name to DEFAULT irrespective of what user provides.
     */
    public static void adjustParameters(HashMap<String, String> data) {
        if (data != null) {
            if (!(data.containsKey("DEFAULT"))) {
                boolean isRenamed = renameParameter(data, "name", "DEFAULT");
                if (!isRenamed) {
                    renameParameter(data, "id", "DEFAULT");
                }
            }
        }
    }

    /**
     * Adjust the input parameters. In case of POST and DELETE methods, user
     * can provide id or DEFAULT parameter for primary parameter(i.e the
     * object to create or delete). This method is used to rename primary
     * parameter name to DEFAULT irrespective of what user provides.
     */
    public static void defineDefaultParameters(HashMap<String, String> data) {
        if (data != null) {
            if (!(data.containsKey("DEFAULT"))) {
                renameParameter(data, "id", "DEFAULT");
            }
        }
    }


    /**
     * Returns the name of the command associated with
     * this resource,if any, for the given operation.
     * @param type the given resource operation
     * @return String the associated command name for the given operation.
     */
    public static String getCommand(RestRedirect.OpType type, ConfigBean configBean) {

        Class<? extends ConfigBeanProxy> cbp = null;
        try {
            cbp = (Class<? extends ConfigBeanProxy>)
                configBean.model.classLoaderHolder.get().loadClass(
                    configBean.model.targetTypeName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        RestRedirects restRedirects = cbp.getAnnotation(RestRedirects.class);
        if (restRedirects != null) {
            RestRedirect[] values = restRedirects.value();
            for (RestRedirect r : values) {
                if (r.opType().equals(type)) {
                    return r.commandName();
                }
            }
        }
        return null;
    }

    /**
     * Executes the specified __asadmin command.
     * @param commandName the command to execute
     * @param parameters the command parameters
     * @param habitat the habitat
     * @return ActionReport object with command execute status details.
     */
    public static ActionReport runCommand(String commandName,
            HashMap<String, String> parameters, Habitat habitat, String resultType) {
        CommandRunner cr = habitat.getComponent(CommandRunner.class);
        ActionReport ar = habitat.getComponent(ActionReport.class,resultType);
        ParameterMap p = new ParameterMap();
        for (Map.Entry<String,String> entry : parameters.entrySet()) {
            p.set(entry.getKey(), entry.getValue());
        }

        cr.getCommandInvocation(commandName, ar).parameters(p).execute();
        return ar;
    }

    /**
    * Executes the specified __asadmin command.
    * @param commandName the command to execute
    * @param parameters the command parameters
    * @param habitat the habitat
    * @return ActionReport object with command execute status details.
    */
    public static ActionReport runCommand(String commandName,
           Properties parameters, Habitat habitat, String typeOfResult) {
       CommandRunner cr = habitat.getComponent(CommandRunner.class);
       ActionReport ar = habitat.getComponent(ActionReport.class, typeOfResult);
        ParameterMap p = new ParameterMap();
        for (String prop : parameters.stringPropertyNames()) {
            p.set(prop, parameters.getProperty(prop));
        }

       cr.getCommandInvocation(commandName, ar).parameters(p).execute();
       return ar;
    }

    /**
     * Constructs and returns the resource method meta-data.
     * @param command the command associated with the resource method
     * @param habitat the habitat
     * @param logger the logger to use
     * @return MethodMetaData the meta-data store for the resource method.
     */
    public static MethodMetaData getMethodMetaData(String command, Habitat habitat,
            Logger logger) {
        return getMethodMetaData(command, Constants.MESSAGE_PARAMETER,
                habitat, logger);
    }

    /**
     * Constructs and returns the resource method meta-data.
     * @param command the command associated with the resource method
     * @param parameterType the type of parameter. Possible values are
     *        Constants.QUERY_PARAMETER and Constants.MESSAGE_PARAMETER
     * @param habitat the habitat
     * @param logger the logger to use
     * @return MethodMetaData the meta-data store for the resource method.
     */
    public static MethodMetaData getMethodMetaData(String command,
            int pamameterType, Habitat habitat, Logger logger) {
        return getMethodMetaData(command, null, pamameterType, habitat, logger);
    }

    /**
     * Constructs and returns the resource method meta-data.
     * @param command the command assocaited with the resource method
     * @param commandParamsToSkip the command parameters for which not to
     *        include the meta-data.
     * @param parameterType the type of parameter. Possible values are
     *        Constants.QUERY_PARAMETER and Constants.MESSAGE_PARAMETER
     * @param habitat the habitat
     * @param logger the logger to use
     * @return MethodMetaData the meta-data store for the resource method.
     */
    public static MethodMetaData getMethodMetaData(String command,
            HashMap<String, String> commandParamsToSkip, int parameterType,
                Habitat habitat, Logger logger) {
        MethodMetaData methodMetaData = new MethodMetaData();

        if (command != null) {
            Collection<CommandModel.ParamModel> params;
            if (commandParamsToSkip == null) {
                params = getParamMetaData(command, habitat, logger);
            } else {
                params = getParamMetaData(command, commandParamsToSkip.keySet(),
                    habitat, logger);
            }

            Iterator<CommandModel.ParamModel> iterator = params.iterator();
            CommandModel.ParamModel paramModel;
            while(iterator.hasNext()) {
                paramModel = iterator.next();
                Param param = paramModel.getParam();

                ParameterMetaData parameterMetaData = getParameterMetaData(paramModel);


                String parameterName =
                    (param.primary())?"id":paramModel.getName();

                // If the Param has an alias, use it instead of the name
                String alias = param.alias();
                if (alias != null && (!alias.isEmpty())) {
                    parameterName = alias;
                }

                if (parameterType == Constants.QUERY_PARAMETER) {
                    methodMetaData.putQureyParamMetaData(parameterName, parameterMetaData);
                } else {
                    //message parameter
                    methodMetaData.putParameterMetaData(parameterName, parameterMetaData);
                }
            }
        }

        return methodMetaData;
    }

    /**
     * Resolve command parameter value of $parent for the parameter
     * in the given map.
     * @param uriInfo the uri context to extract parent name value.
     */
    public static void resolveParentParamValue(HashMap<String, String> commandParams,
            UriInfo uriInfo) {

        String parent = getParentName(uriInfo);
        if (parent != null) {
            Set<String> keys = commandParams.keySet();
            Iterator<String> iterator = keys.iterator();
            String key;
            while (iterator.hasNext()) {
                key = iterator.next();
                if (commandParams.get(key).equals( Constants.PARENT_NAME_VARIABLE)) {
                    commandParams.put(key, parent);
                    break;
                }
            }
        }
    }

    /**
     * Constructs and returns the resource method meta-data. This method is
     * called to get meta-data in case of update method (POST).
     * @param configBean the config bean associated with the resource.
     * @return MethodMetaData the meta-data store for the resource method.
     */
    public static MethodMetaData getMethodMetaData(ConfigBean configBean) {
        return getMethodMetaData(configBean, Constants.MESSAGE_PARAMETER);
    }

    /**
     * Constructs and returns the resource method meta-data. This method is
     * called to get meta-data in case of update method (POST).
     * @param configBean the config bean associated with the resource.
     * @param parameterType the type of parameter. Possible values are
     *        Constants.QUERY_PARAMETER and Constants.MESSAGE_PARAMETER
     * @return MethodMetaData the meta-data store for the resource method.
     */
    public static MethodMetaData getMethodMetaData(ConfigBean configBean, int parameterType) {
        MethodMetaData methodMetaData = new MethodMetaData();

        if (configBean != null) {
            Class<? extends ConfigBeanProxy> configBeanProxy = null;
             try {
                configBeanProxy = (Class<? extends ConfigBeanProxy>)
                    configBean.model.classLoaderHolder.get().loadClass(configBean.model.targetTypeName);

                Set<String> attributeNames = configBean.model.getAttributeNames();
                for (String attributeName : attributeNames) {
                    String methodName = getAttributeMethodName(attributeName);
                    try {
                        Method method = configBeanProxy.getMethod(methodName);
                        Attribute attribute = method.getAnnotation(Attribute.class);
                        if (attribute != null) {
                            ParameterMetaData parameterMetaData = getParameterMetaData(attribute);

                            //camelCase the attributeName before passing out
                            attributeName = eleminateHypen(attributeName);
                            if (parameterType == Constants.QUERY_PARAMETER) {
                                methodMetaData.putQureyParamMetaData(attributeName, parameterMetaData);
                            } else {
                                //message parameter
                                methodMetaData.putParameterMetaData(attributeName, parameterMetaData);
                            }
                        }
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return methodMetaData;
    }

    public static MethodMetaData getMethodMetaData2(Dom parent, ConfigModel childModel, int parameterType) {
        MethodMetaData methodMetaData = new MethodMetaData();

        Class<? extends ConfigBeanProxy> configBeanProxy = null;
        try {
            configBeanProxy = (Class<? extends ConfigBeanProxy>) childModel.classLoaderHolder.get().loadClass(childModel.targetTypeName);

            Set<String> attributeNames = childModel.getAttributeNames();
            for (String attributeName : attributeNames) {
                String methodName = getAttributeMethodName(attributeName);
                try {
                    Method method = configBeanProxy.getMethod(methodName);
                    Attribute attribute = method.getAnnotation(Attribute.class);
                    if (attribute != null) {
                        ParameterMetaData parameterMetaData = getParameterMetaData(attribute);

                        //camelCase the attributeName before passing out
                        attributeName = eleminateHypen(attributeName);
                        if (parameterType == Constants.QUERY_PARAMETER) {
                            methodMetaData.putQureyParamMetaData(attributeName, parameterMetaData);
                        } else {
                            //message parameter
                            methodMetaData.putParameterMetaData(attributeName, parameterMetaData);
                        }
                    }
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return methodMetaData;
    }
    /**
     * Constructs and returns the parameter meta-data.
     * @param command the command assocaited with the resource method
     * @param habitat the habitat
     * @param logger the logger to use
     * @return Collection the meta-data for the parameter of the resource method.
     */
    public static Collection<CommandModel.ParamModel> getParamMetaData(
            String commandName, Habitat habitat, Logger logger) {
        CommandRunner cr = habitat.getComponent(CommandRunner.class);
        CommandModel cm = cr.getModel(commandName, logger);
        Collection<CommandModel.ParamModel> params = cm.getParameters();
        //print(params);
        return params;
    }

    /**
     * Constructs and returns the parameter meta-data.
     * @param command the command assocaited with the resource method
     * @param commandParamsToSkip the command parameters for which not to
     *        include the meta-data.
     * @param habitat the habitat
     * @param logger the logger to use
     * @return Collection the meta-data for the parameter of the resource method.
     */
    public static Collection<CommandModel.ParamModel> getParamMetaData(
            String commandName, Collection<String> commandParamsToSkip,
                Habitat habitat, Logger logger) {
        CommandRunner cr = habitat.getComponent(CommandRunner.class);
        CommandModel cm = cr.getModel(commandName, logger);
        Collection<String> parameterNames = cm.getParametersNames();

        ArrayList<CommandModel.ParamModel> metaData =
            new ArrayList<CommandModel.ParamModel>();
        CommandModel.ParamModel paramModel;
        for (String name : parameterNames) {
            paramModel = cm.getModelFor(name);
            String parameterName =
                (paramModel.getParam().primary())?"id":paramModel.getName();

            boolean skipParameter = false;
            try {
                skipParameter = commandParamsToSkip.contains(parameterName);
            } catch (Exception e) {
                String errorMessage =
                    localStrings.getLocalString("rest.metadata.skip.error",
                        "Parameter \"{0}\" may be redundant and not required.",
                            new Object[] {parameterName});
                Logger.getLogger(ResourceUtil.class.getName()).log(Level.INFO,
                    null, errorMessage);
                Logger.getLogger(ResourceUtil.class.getName()).log(Level.INFO,
                    null, e);
            }

            if (!skipParameter) {
                metaData.add(paramModel);
            }
        }

        //print(metaData);
        return metaData;
    }

    //removes entries with empty value from the given Map
    public static void purgeEmptyEntries(HashMap<String, String> data) {
        Set<String> keys = data.keySet();
        Iterator<String> iterator = keys.iterator();
        String key;
        while (iterator.hasNext()) {
            key = iterator.next();
            if ((data.get(key) == null) || (data.get(key).length() < 1)) {
                data.remove(key);
                iterator = keys.iterator();
            }
        }
     }

    /**
     * Constructs and returns the appropriate response object based on the client.
     * @param status the http status code for the response
     * @param message message for the response
     * @param requestHeaders request headers of the request
     * @return Response the response object to be returned to the client
     */
    public static Response getResponse(int status, String message,
            HttpHeaders requestHeaders, UriInfo uriInfo){
        if(isBrowser(requestHeaders)) {
            message = getHtml(message, uriInfo);
        }
        return Response.status(status).entity(message).build();
    }



    /**
     * <p>This method takes any query string parameters and adds them to the specified map.  This
     * is used, for example, with the delete operation when cascading deletes are required:</p>
     * <code style="margin-left: 3em">DELETE http://localhost:4848/.../foo?cascade=true</code>
     * <p>The reason we need to use query parameters versus "body" variables is the limitation
     * that HttpURLConnection has in this regard.
     *
     * @param data
     */
    public static void addQueryString(MultivaluedMap<String, String> qs, HashMap<String, String> data) {
        for (Map.Entry<String, List<String>> entry : qs.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                data.put(key, value); // TODO: Last one wins? Can't imagine we'll see List.size() > 1, but...
            }
        }
    }

    public static void addQueryString(MultivaluedMap<String, String> qs, Properties data) {
        for (Map.Entry<String, List<String>> entry : qs.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                data.put(key, value); // TODO: Last one wins? Can't imagine we'll see List.size() > 1, but...
            }
        }
    }

    //Construct parameter meta-data from the model
    private static ParameterMetaData getParameterMetaData(CommandModel.ParamModel paramModel) {
        Param param = paramModel.getParam();
        ParameterMetaData parameterMetaData = new ParameterMetaData();

        parameterMetaData.putAttribute(Constants.TYPE, getXsdType(paramModel.getType().toString()));
        parameterMetaData.putAttribute(Constants.OPTIONAL, Boolean.toString(param.optional()));
        parameterMetaData.putAttribute(Constants.DEFAULT_VALUE, param.defaultValue());
        parameterMetaData.putAttribute(Constants.ACCEPTABLE_VALUES, param.acceptableValues());

        return parameterMetaData;
    }

    //Construct parameter meta-data from the attribute annotation
    private static ParameterMetaData getParameterMetaData(Attribute attribute) {
        ParameterMetaData parameterMetaData = new ParameterMetaData();
        parameterMetaData.putAttribute(Constants.TYPE, getXsdType(attribute.dataType().toString()));
        parameterMetaData.putAttribute(Constants.OPTIONAL, Boolean.toString(!attribute.required()));
        if (!(attribute.defaultValue().equals("\u0000"))) {
            parameterMetaData.putAttribute(Constants.DEFAULT_VALUE, attribute.defaultValue());
        }
        parameterMetaData.putAttribute(Constants.KEY, Boolean.toString(attribute.key()));
        //FIXME - Currently, Attribute class does not provide acceptable values.
        //parameterMetaData.putAttribute(Contants.ACCEPTABLE_VALUES,
        //    getXsdType(attribute.acceptableValues()));

        return parameterMetaData;
    }

    //rename the given input parameter
    private static boolean renameParameter(HashMap<String, String> data,
        String parameterToRename, String newName) {
        if ((data.containsKey(parameterToRename))) {
            String value = data.get(parameterToRename);
            data.remove(parameterToRename);
            data.put(newName, value);
            return true;
        }
        return false;
    }

    //print given parameter meta-data.
    private static void print(Collection<CommandModel.ParamModel> params) {
        for (CommandModel.ParamModel pm : params) {
            System.out.println("Command Param: " + pm.getName());
            System.out.println("Command Param Type: " + pm.getType());
            System.out.println("Command Param Name: " + pm.getParam().name());
            System.out.println("Command Param Shortname: " + pm.getParam().shortName());
        }
    }

    //returns true only if the request is from browser
    private static boolean isBrowser(HttpHeaders requestHeaders) {
        boolean isClientAcceptsHtml = false;
        MediaType media = requestHeaders.getMediaType();
        java.util.List<String> acceptHeaders = 
            requestHeaders.getRequestHeader(HttpHeaders.ACCEPT);

        for (String header: acceptHeaders) {
            if (header.contains(MediaType.TEXT_HTML)) {
                isClientAcceptsHtml = true;
                break;
            }
        }

        if (media != null) {
            if ((media.equals(MediaType.APPLICATION_FORM_URLENCODED_TYPE)) &&
                    (isClientAcceptsHtml)) {
                return true;
            }
        }

        return false;
    }

    private static String getXsdType(String javaType) {
        if (javaType.indexOf(Constants.JAVA_STRING_TYPE) != -1)
            return Constants.XSD_STRING_TYPE;
        if (javaType.indexOf(Constants.JAVA_BOOLEAN_TYPE) != -1)
            return Constants.XSD_BOOLEAN_TYPE;
        if (javaType.indexOf(Constants.JAVA_INT_TYPE) != -1)
            return Constants.XSD_INT_TYPE;
        if (javaType.indexOf(Constants.JAVA_PROPERTIES_TYPE) != -1)
            return Constants.XSD_PROPERTIES_TYPE;
        return javaType;
    }

    private static String getAttributeMethodName(String attributeName) {
        return methodNameFromDtdName(attributeName, "get");
    }

    /**
     * Translates all param names in </code>sourceMap</code> that corresponds to camelCasedName of a command param into corresponding command param name
     * @param sourceMap - The input. Contains untranslated names
     * @param commandName - The command we want to translate for
     * @param habitat
     * @param logger
     * @return A HashMap<String, String> that contains (1) all the entries from <code>sourceMap<code> whose key matches camleCaseName of
     * one of the parameters of command. The key of this entry is translated to corresponding command param name (2)
     * All the remaining entries from sourceMap as it is.
     */
    public static HashMap<String, String> translateCamelCasedNamesToCommandParamNames(HashMap<String, String> sourceMap, String commandName, Habitat habitat, Logger logger) {
        //TODO after Bills changes to call toLowerCase() this translation might not be required as he will accept camleCased parameters. Remove this code then.
        CommandRunner cr = habitat.getComponent(CommandRunner.class);
        CommandModel cm = cr.getModel(commandName, logger);
        Collection<CommandModel.ParamModel> paramModels = cm.getParameters();
        HashMap<String, String> translatedMap = new HashMap<String, String>();
        for (CommandModel.ParamModel paramModel : paramModels) {
            Param param = paramModel.getParam(); 
            String camelCaseName = param.alias();
            if(sourceMap.containsKey(camelCaseName)) {
                String paramValue = sourceMap.remove(camelCaseName);
                translatedMap.put(paramModel.getName(), paramValue);
            }
        }
        //Copy over the remaining values from sourceMap
        translatedMap.putAll(sourceMap);

        return translatedMap;

    }

    private static String split(String lookback,String lookahead) {
        return "((?<="+lookback+")(?="+lookahead+"))";
    }
    private static String or(String... tokens) {
        StringBuilder buf = new StringBuilder();
        for (String t : tokens) {
            if(buf.length()>0)  buf.append('|');
            buf.append(t);
        }
        return buf.toString();
    }

    public static String convertToXMLName(String name) {
        // tokenize by finding 'x|X' and 'X|Xx' then insert '-'.
        StringBuilder buf = new StringBuilder(name.length()+5);
        for(String t : TOKENIZER.split(name)) {
            if(buf.length()>0) {
                buf.append('-');
            }
            buf.append(t.toLowerCase());
        }
        return buf.toString();
    }

    /**
     * @return A copy of given <code>sourceData</code> where key of each entry from it is converted to xml name
     */
    public static HashMap<String, String> translateCamelCasedNamesToXMLNames(Map<String, String> sourceData) {
        HashMap<String, String> convertedData = new HashMap<String, String>(sourceData.size());
        for (Map.Entry<String, String> entry : sourceData.entrySet()) {
            String camelCasedKeyName = entry.getKey();
            String xmlKeyName = convertToXMLName(camelCasedKeyName);
            convertedData.put(xmlKeyName, entry.getValue());
        }
        return convertedData;
    }
    /* we try to prefer html by default for all browsers (safari, chrome, firefox.
     * Same if the request is asking for "*"
     * among all the possible AcceptableMediaTypes
     * 
     */
    public static String getResultType(HttpHeaders requestHeaders) {
        String result = "html";
        String firstOne = null;
        List<MediaType> lmt = requestHeaders.getAcceptableMediaTypes();
        for (MediaType mt : lmt) {
            if (mt.getSubtype().equals("html")) {
                return result;
            }
            if (mt.getSubtype().equals("*")) {
                return result;
            }
            if (firstOne == null) { //default to the first one if many are there.
                firstOne = mt.getSubtype();
            }
        }

        if (firstOne != null) {
            return firstOne;
        } else {
            return result;
        }
    }
}