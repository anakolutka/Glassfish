/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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

import com.sun.enterprise.module.impl.Utils;
import com.sun.enterprise.module.common_impl.LogHelper;
import com.sun.enterprise.universal.collections.ManifestUtils;
import com.sun.enterprise.universal.glassfish.AdminCommandResponse;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import java.io.*;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Async;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.InjectionManager;
import org.jvnet.hk2.component.UnsatisfiedDepedencyException;
import com.sun.enterprise.universal.BASE64Decoder;
/**
 * Encapsulates the logic needed to execute a server-side command (for example,  
 * a descendant of AdminCommand) including injection of argument values into the 
 * command.  
 * 
 * @author dochez
 * @author tjquinn
 */
@Service
public class CommandRunner {
    
    public final static LocalStringManagerImpl adminStrings = new LocalStringManagerImpl(CommandRunner.class);
    public final static Logger logger = LogDomains.getLogger(CommandRunner.class, LogDomains.ADMIN_LOGGER);

    private static final String ASADMIN_CMD_PREFIX = "AS_ADMIN_";
    @Inject
    Habitat habitat;

    /**
     * Executes a command by name.
     * <p>
     * The commandName parameter value should correspond to the name of a 
     * command that is a service with that name.
     * @param commandName the command to execute
     * @param parameters name/value pairs to be passed to the command
     * @param report will hold the result of the command's execution
     */
    public ActionReport doCommand(final String commandName, final Properties parameters, final ActionReport report) {

        return doCommand(commandName, parameters, report, null);
    }
    
    /**
     * Executes a command by name.
     * <p>
     * The commandName parameter value should correspond to the name of a 
     * command that is a service with that name.
     * @param commandName the command to execute
     * @param parameters name/value pairs to be passed to the command
     * @param report will hold the result of the command's execution
     * @param uploadedFiles files uploaded from the client
     */
    public ActionReport doCommand(final String commandName, final Properties parameters, 
            final ActionReport report, List<File> uploadedFiles) {

        final AdminCommand handler = getCommand(commandName, report, logger);
        if (handler==null) {
            return report;
        }
        return doCommand(commandName, handler, parameters, report, uploadedFiles);
    }

    /**
     * Executes the provided command object.
     * @param commandName name of the command (used for logging and reporting)
     * @param command the command service to execute
     * @param parameters name/value pairs to be passed to the command
     * @param report will hold the result of the command's execution
     */
    
    public ActionReport doCommand(
            final String commandName, 
            final AdminCommand command, 
            final Properties parameters, 
            final ActionReport report) {
        return doCommand(commandName, command, parameters, report, null);
    }
        
    /**
     * Executes the provided command object.
     * @param commandName name of the command (used for logging and reporting)
     * @param command the command service to execute
     * @param parameters name/value pairs to be passed to the command
     * @param report will hold the result of the command's execution
     * @param uploadedFiles files uploaded from the client
     */
    
    public ActionReport doCommand(
            final String commandName, 
            final AdminCommand command, 
            final Properties parameters, 
            final ActionReport report,
            final List<File> uploadedFiles) {
        if (parameters.get("help")!=null) {
            InputStream in = getManPage(commandName, command);
            String manPage = encodeManPage(in);

            if(manPage != null) {
                report.getTopMessagePart().addProperty("MANPAGE", manPage);
            }
            else {
                report.getTopMessagePart().addProperty(AdminCommandResponse.GENERATED_HELP, "true");
                getHelp(commandName, command, report);
            }
            return report;
        }
        report.setActionDescription(commandName + " AdminCommand");

        final AdminCommandContext context = new AdminCommandContext(
                LogDomains.getLogger(CommandRunner.class, LogDomains.ADMIN_LOGGER),
                report, parameters, uploadedFiles);                                                 

        // initialize the injector.
        InjectionManager injectionMgr =  new InjectionManager<Param>() {

            @Override
            protected boolean isOptional(Param annotation) {
                return annotation.optional();
            }

            protected Object getValue(Object component, AnnotatedElement target, Class type) throws ComponentException {
                // look for the name in the list of parameters passed.
                Param param = target.getAnnotation(Param.class);
                String acceptable = param.acceptableValues();
                String paramName = getParamName(param, target);
                if (param.primary()) {
                    // this is the primary parameter for the command
                    String value = parameters.getProperty("DEFAULT");
                    if (value!=null) {
                        // let's also copy this value to the command with a real name.
                        parameters.setProperty(paramName, value);
                        return convertStringToObject(paramName, type, value);
                    }
                }
                String paramValueStr = getParamValueString(parameters, param,
                                                           target);

                if(ok(acceptable)&& ok(paramValueStr)) {
                    String[] ss = acceptable.split(",");
                    boolean ok = false;
                    
                    for(String s : ss) {
                        if(paramValueStr.equals(s.trim())) {
                            ok = true;
                            break;
                        }
                    }
                    if(!ok)
                        throw new UnacceptableValueException(
                            adminStrings.getLocalString("adapter.command.unacceptableValue", 
                            "Invalid parameter: {0}.  Its value is {1} but it isn''t one of these acceptable values: {2}",
                            paramName,
                            paramValueStr,
                            acceptable));
                }
                if (paramValueStr != null) {
                    return convertStringToObject(paramName, type, paramValueStr);
                }
                //return default value
                return getParamField(component, target);
            }
        };

        LocalStringManagerImpl localStrings = new LocalStringManagerImpl(command.getClass());

        // Let's get the command i18n key
        I18n i18n = command.getClass().getAnnotation(I18n.class);
        String i18n_key = "";
        if (i18n!=null) {
            i18n_key = i18n.value();
        }

        // inject
        try {
            injectionMgr.inject(command, Param.class);
            if (!skipValidation(command)) {
                validateParameters(command, parameters);
            }
        } catch (UnsatisfiedDepedencyException e) {
            Param param = e.getUnsatisfiedElement().getAnnotation(Param.class);
            String paramName = getParamName(param, e.getUnsatisfiedElement());            
            String paramDesc = getParamDescription(localStrings, i18n_key, paramName, e.getUnsatisfiedElement());
            final String usage = getUsageText(command);                                
            String errorMsg;
            if (param.primary()) {
                errorMsg = adminStrings.getLocalString("commandrunner.operand.required",
                                                       "Operand required.");
            }
            else if (param.password() == true) {
                errorMsg = adminStrings.getLocalString("adapter.param.missing.passwordfile", "{0} command requires the passwordfile parameter containing {1} entry.", commandName, paramName);
            }
            else if (paramDesc!=null) {
                errorMsg = adminStrings.getLocalString("admin.param.missing",
                                                       "{0} command requires the {1} parameter : {2}", commandName, paramName, paramDesc);
                
            }
            else {
                errorMsg = adminStrings.getLocalString("admin.param.missing.nodesc",
                        "{0} command requires the {1} parameter", commandName, paramName);
            }
            logger.severe(errorMsg);            
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(errorMsg);
            report.setFailureCause(e);
            ActionReport.MessagePart childPart = report.getTopMessagePart().addChild();
            childPart.setMessage(usage);
            return report;
        } catch (ComponentException e) {
            // if the cause is UnacceptableValueException -- we want the message
            // from it.  It is wrapped with a less useful Exception
            
            Exception exception = e;
            Throwable cause = e.getCause();
            if(cause != null && (cause instanceof UnacceptableValueException)) {
                // throw away the wrapper.
                exception = (Exception)cause;
            }
            logger.severe(exception.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(exception.getMessage());
            report.setFailureCause(exception);
            ActionReport.MessagePart childPart = report.getTopMessagePart().addChild();
            childPart.setMessage(getUsageText(command));
            return report;
        }

        // the command may be an asynchronous command, so we need to check
        // for the @Async annotation.
        Async async = command.getClass().getAnnotation(Async.class);
        if (async==null) {
            try {
                command.execute(context);
            } catch(Throwable e) {
                logger.log(Level.SEVERE,
                        adminStrings.getLocalString("adapter.exception","Exception in command execution : ", e), e);
                report.setMessage(e.toString());
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
            }
        } else {
            Thread t = new Thread() {
                public void run() {
                    try {
                        command.execute(context);
                    } catch (RuntimeException e) {
                        logger.log(Level.SEVERE,e.getMessage(), e);
                    }
                }
            };
            t.setPriority(async.priority());
            t.start();
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            report.setMessage(
                    adminStrings.getLocalString("adapter.command.launch", "Command {0} was successfully initiated asynchronously.", commandName));
        }
        return context.getActionReport();
    }

    protected String getParamDescription(LocalStringManagerImpl localStrings, String i18nKey, String paramName, AnnotatedElement annotated) {

        I18n i18n = annotated.getAnnotation(I18n.class);
        String paramDesc;
        if (i18n==null) {
            paramDesc = localStrings.getLocalString(i18nKey+"."+paramName, "");
        } else {
            paramDesc = localStrings.getLocalString(i18n.value(), "");
        }
        if (paramDesc==null) {
            paramDesc = "";
//            paramDesc = adminStrings.getLocalString("adapter.nodesc", "no description provided");
        }
        return paramDesc;        
    }

        /**
         * get the Param name.  First it checks if the annotated Param
         * includes a the name, if not then get the name from the field.
         *
         * @param - Param class annotation
         * @annotated - annotated element
         * @return the name of the param
         */
    String getParamName(Param param, AnnotatedElement annotated) {
        if (param.name().equals("")) {
            if (annotated instanceof Field) {
                return ((Field) annotated).getName();
            }
            if (annotated instanceof Method) {
                return ((Method) annotated).getName().substring(3).toLowerCase();
            }
        } else if (param.password() == true) {
            return ASADMIN_CMD_PREFIX + param.name().toUpperCase();
        } else {
            return param.name();
        }
        return "";
    }

    
        /**
         * get the param value.  checks if the param (option) value
         * is defined on the command line (URL passed by the client)
         * by calling getPropertiesValue method.  If not, then check
         * for the shortName.  If param value is not given by the
         * shortName (short option) then if the default valu is
         * defined.
         * 
         * @param parameters - parameters from the command line.
         * @param param - from the annotated Param
         * @param target - annotated element
         *
         * @return param value
         */
    String getParamValueString(final Properties parameters,
                               final Param param,
                               final AnnotatedElement target) {
        String paramValueStr = getPropertiesValue(parameters,
                                                  getParamName(param, target),
                                                  true);
        if (paramValueStr == null) {
                //check for shortName
            paramValueStr = parameters.getProperty(param.shortName());
        }
            //if paramValueStr is still null, then check to
            //see if the defaultValue is defined
        if (paramValueStr == null) {
            final String defaultValue = param.defaultValue();
            paramValueStr = (defaultValue.equals(""))?null:defaultValue;
        }
        return paramValueStr;
    }


        /**
         * get the value of the field.  This value is defined in the
         * annotated Param declaration.  For example:
         * <code>
         * @Param(optional=true)
         * String name="server"
         * </code>
         * The Field, name's value, "server" is returned.
         *
         * @param component - command class object
         * @param annotated - annotated element
         *
         * @return the annotated Field value
         */
    Object getParamField(final Object component,
                         final AnnotatedElement annotated) {
        try {
            if (annotated instanceof Field) {
                Field field = (Field)annotated;
                field.setAccessible(true);
                return ((Field) annotated).get(component);
            }
        }
        catch (Exception e) {
                //unable to get the field value, may not be defined
                //return null instead.
            return null;
        }
        return null;
    }

        /**
         * convert the String parameter to the specified type.
         * For example if type is Properties and the String
         * value is: name1=value1:name2=value2:...
         * then this api will convert the String to a Properties
         * class with the values {name1=name2, name2=value2, ...}
         *
         * @param type - the type of class to convert
         * @param paramValStr - the String value to convert
         *
         * @return Object
         */
        Object convertStringToObject(String paramName, Class type, String paramValStr) {
            Object paramValue = paramValStr;
            if (type.isAssignableFrom(String.class)) {
                paramValue = paramValStr;
            } else if (type.isAssignableFrom(Properties.class)) {
                paramValue = convertStringToProperties(paramValStr);
            } else if (type.isAssignableFrom(List.class)) {
                paramValue = convertStringToList(paramValStr);
            } else if (type.isAssignableFrom(Boolean.class)) {
                paramValue = convertStringToBoolean(paramName, paramValStr);
            } else if (type.isAssignableFrom(String[].class)) {
                paramValue = convertStringToStringArray(paramValStr);
            } else if (type.isAssignableFrom(File.class)) {
                return new File(paramValStr);
            }
            return paramValue;
        }


    /**
         *  Searches for the property with the specified key in this property list.
         *  The method returns null if the property is not found.
         *  @see java.util.Properties#getProperty(java.lang.String)
         *
         *  @param props - the property to search in
         *  @param key - the property key
         *  @param ignoreCase - true to search the key ignoring case
         *                      false otherwise
         *  @return the value in this property list with the specified key value.
         */
    String getPropertiesValue(final Properties props, final String key,
                              final boolean ignoreCase) {
        BASE64Decoder base64Decoder = new BASE64Decoder();
        if (ignoreCase) {
            for (Object propObj : props.keySet()) {
                final String propName = (String)propObj;
                if (propName.equalsIgnoreCase(key)) {
                    try {
                    if (propName.startsWith(ASADMIN_CMD_PREFIX))
                        return new String(base64Decoder.decodeBuffer(
                                props.getProperty(propName)));
                    } catch (IOException e) {
                        // ignore for now. Not much can be done anyway.
                        // todo: improve this error condition reporting
                    }
                    return props.getProperty(propName);
                }
            }
        }
        return props.getProperty(key);
    }

    
    /**
     * Return Command handlers from the lookup or if not found in the lookup,
     * look at META-INF/services implementations and add them to the lookup
     * @param commandName the request handler's command name
     * @param report the reporting facility
     * @return the admin command handler if found
     *
     */
    private AdminCommand getCommand(String commandName, ActionReport report, Logger logger) {

        AdminCommand command = null;
        try {
            command = habitat.getComponent(AdminCommand.class, commandName);
        } catch(ComponentException e) {
           e.printStackTrace();
        }
        if (command==null) {
            String msg;
            
            if(!ok(commandName))
                msg = adminStrings.getLocalString("adapter.command.nocommand", "No command was specified.");
            else {
                msg = adminStrings.getLocalString("adapter.command.notfound", "Command {0} not found", commandName);
                    //set cause to CommandNotFoundException so that asadmin
                    //displays the closest matching commands
                report.setFailureCause(new CommandNotFoundException(msg));
            }
            report.setMessage(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            LogHelper.getDefaultLogger().info(msg);
        }
        return command;
    }

         /**
          * get the usage-text of the command.
          * check if <command-name>.usagetext is defined in LocalString.properties
          * if defined, then use the usagetext from LocalString.properties else
          * generate the usagetext from Param annotations in the command class.
          *
          * @param command class
          *
          * @return usagetext
          */
    String getUsageText(AdminCommand command) {
        StringBuffer usageText = new StringBuffer();
        I18n i18n = command.getClass().getAnnotation(I18n.class);
        String i18nKey = null;
        
        final LocalStringManagerImpl lsm  = new LocalStringManagerImpl(command.getClass());
        if (i18n!=null) {
            i18nKey = i18n.value();
        }
        if (i18nKey != null) {
            usageText.append(lsm.getLocalString(i18nKey+".usagetext",
                                                generateUsageText(command)));
        }
        else {
            return generateUsageText(command);
        }
        return usageText.toString();
    }

         /**
          * generate the usage-text from the annotated Param in the command class
          *
          * @param command class
          *
          * @return generated usagetext
          */
    private String generateUsageText(AdminCommand command) {
        StringBuffer usageText = new StringBuffer();
        usageText.append("Usage: ");
        usageText.append(command.getClass().getAnnotation(Service.class).name());
        usageText.append(" ");
        StringBuffer operand = new StringBuffer();
        for (Field f : command.getClass().getDeclaredFields()) {
            final Param param = f.getAnnotation(Param.class);
            if (param==null) {
                continue;
            }
            final String paramName = getParamName(param, f);
                //do not want to display password as an option
            if (paramName.startsWith(ASADMIN_CMD_PREFIX))
                continue;
            final boolean optional = param.optional();
            final Class<?> ftype = f.getType();
            Object fvalue = null;
            String fvalueString = null;
            try {
                f.setAccessible(true);
                fvalue = f.get(command);
                if(fvalue != null)
                    fvalueString = fvalue.toString();
            }
            catch(Exception e) {
                // just leave it as null...
            }
            // this is a param.
            if (param.primary()) {
                if (optional) {
                    operand.append("[").append(paramName).append("] ");
                }
                else {
                    operand.append(paramName).append(" ");
                }
                continue;
            }
            if (optional) { usageText.append("["); }
            usageText.append("--").append(paramName);

            if (ok(param.defaultValue())) {
                usageText.append("=").append(param.defaultValue());
                if(optional) { usageText.append("] "); }
                else { usageText.append(" "); }
            }
            else if (ftype.isAssignableFrom(String.class)) {
                    //check if there is a default value assigned
                if (ok(fvalueString)) {
                    usageText.append("=").append(fvalueString);
                    if (optional) { usageText.append("] "); }
                    else { usageText.append(" "); }
                } else {
                    usageText.append("=").append(paramName);
                    if (optional) { usageText.append("] "); }
                    else { usageText.append(" "); }
                }
            }
            else if (ftype.isAssignableFrom(Boolean.class)) {
                // note: There is no defaultValue for this param.  It might
                // hava  value -- but we don't care -- it isn't an official
                // default value.
                    usageText.append("=").append("true|false");
                    if (optional) { usageText.append("] "); }
                    else { usageText.append(" "); }
            }
            else {
                usageText.append("=").append(paramName);
                if (optional) { usageText.append("] "); }
                else { usageText.append(" "); }
            }
        }//for
        usageText.append(operand);
        return usageText.toString();
    }


    public void getHelp(String commandName, AdminCommand command, ActionReport report) {
        report.setActionDescription(commandName + " help");
        LocalStringManagerImpl localStrings = new LocalStringManagerImpl(command.getClass());
        // Let's get the command i18n key
        I18n i18n = command.getClass().getAnnotation(I18n.class);
        String i18nKey = "";

        if (i18n!=null) {
            i18nKey = i18n.value();
        }
        report.setMessage(commandName + " - " + localStrings.getLocalString(i18nKey, ""));
        report.getTopMessagePart().addProperty("SYNOPSIS", getUsageText(command));
        for (Field f : command.getClass().getDeclaredFields()) {
            addParamUsage(report, localStrings, i18nKey, f);
        }
        for (Method m : command.getClass().getDeclaredMethods()) {
            addParamUsage(report, localStrings, i18nKey, m);
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    public InputStream getManPage(String commandName, AdminCommand command) {
        // bnevins -- too bad there is no AdminCommand baseclass.  We could make it
        // do the work but, alas, there is no such thing.
        Class clazz = command.getClass();
        Package pkg = clazz.getPackage();
        String manPage = pkg.getName().replace('.', '/');
        manPage += "/" + commandName + ".1";
        ClassLoader loader = clazz.getClassLoader();
        InputStream in = loader.getResourceAsStream(manPage);
        return in;
    }

    private void addParamUsage(ActionReport report, LocalStringManagerImpl localStrings, String i18nKey, AnnotatedElement annotated) {
        Param param = annotated.getAnnotation(Param.class);
        if (param!=null) {
             // this is a param.
            String paramName = getParamName(param, annotated);
            //do not want to display password in the usage
            if (paramName.startsWith(ASADMIN_CMD_PREFIX))
                return;
            if (param.primary()) {
                //if primary then it's an operand
                report.getTopMessagePart().addProperty(paramName+"_operand", getParamDescription(localStrings, i18nKey, paramName, annotated));
            } else {
                report.getTopMessagePart().addProperty(paramName, getParamDescription(localStrings, i18nKey, paramName, annotated));
            }
        }
    }

    
    private boolean ok(String s) {
        return s != null && s.length() > 0;
    }


    /**
     * validate the paramters with the Param annotation.  If parameter is not defined
     * as a Param annotation then it's an invalid option.  If parameter's key is "DEFAULT"
     * then it's a operand.
     *
     * @param command - command class
     * @param parameter - parameters from URL
     *
     * @throws ComponentException if option is invalid
     */
    void validateParameters(final AdminCommand command, final Properties parameters)
        throws ComponentException {
        
        final java.util.Enumeration e = parameters.propertyNames();
        //loop through parameters and make sure they are part of the Param declared field
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement();
            if (key == null)
                continue; // this should really be an assertion
            //DEFAULT is the operand and it's a valid Parameter
            if ("DEFAULT".equals(key) || key.startsWith(ASADMIN_CMD_PREFIX)) {
                continue;
            }
            
            //check if key is a valid Param Field
            boolean validOption = false;
                //loop through the Param field in the command class
                //if either field name or the param name is equal to
                //key then it's a valid option
            for (Field field : command.getClass().getDeclaredFields()) {
                final Param param = field.getAnnotation(Param.class);
                if (param == null)     continue;
                
                if (key.startsWith(ASADMIN_CMD_PREFIX)) {
                    validOption = true;
                    continue;
                }
                if (field.getName().equals(key) ||
                    param.name().equals(key) ) {
                    validOption=true;
                    break;
                }
            }
            if (!validOption) {
                throw new ComponentException(" Invalid option: " + key);
            }
        }
    }
    
         /**
         * convert a String to a Boolean
         * null --> true
         * "" --> true
         * case insensitive "true" --> true
         * case insensitive "false" --> false
         * anything else --> throw Exception
         * @param paramName - the name of the param
         * @param s - the String to convert
         * @return Boolean
         */
    Boolean convertStringToBoolean(String paramName, String s) {
        if(!ok(s))
            return true;
        
        if(s.equalsIgnoreCase(Boolean.TRUE.toString()))
            return true;

        if(s.equalsIgnoreCase(Boolean.FALSE.toString()))
            return false;
        
        String msg = adminStrings.getLocalString(
                "adapter.command.unacceptableBooleanValue",
                "Invalid parameter: {0}.  This boolean option must be set " +
                    "(case insensitive) to true or false.  Its value was set to {1}",
                paramName, s);
                
        throw new UnacceptableValueException(msg);
    }

        /**
         * convert a String with the following format to Properties:
         * name1=value1:name2=value2:name3=value3:...
         * The Properties object contains elements:
         * {name1=value1, name2=value2, name3=value3, ...}
         *
         * @param listString - the String to convert
         * @return Properties containing the elements in String
         */
    Properties convertStringToProperties(String propsString) {
        final Properties properties = new Properties();
        if (propsString != null) {
            ParamTokenizer stoken = new ParamTokenizer(propsString, ":");
            while (stoken.hasMoreTokens()) {
                String token = stoken.nextToken();
                if (token.indexOf("=")==-1)
                    continue;
                final ParamTokenizer nameTok = new ParamTokenizer(token, "=");
                if (nameTok.countTokens() == 2) {
                    properties.setProperty(nameTok.nextTokenWithoutEscapeAndQuoteChars(),
                                       nameTok.nextTokenWithoutEscapeAndQuoteChars());
                } else {
                    throw new IllegalArgumentException(adminStrings.getLocalString("InvalidPropertySyntax", "Invalid property syntax."));
                }
            }
        }
        return properties;
    }

        /**
         * convert a String with the following format to List<String>:
         * string1:string2:string3:...
         * The List object contains elements: string1, string2, string3, ...
         *
         * @param listString - the String to convert
         * @return List containing the elements in String
         */
    List<String> convertStringToList(String listString) {
        List<String> list = new java.util.ArrayList();
        if (listString != null) {
            final ParamTokenizer ptoken = new ParamTokenizer(listString, ":");
            while (ptoken.hasMoreTokens()) {
                String token = ptoken.nextTokenWithoutEscapeAndQuoteChars();
                list.add(token);
            }
        }
        return list;
    }

        /**
         * convert a String with the following format to String Array:
         * string1,string2,string3,...
         * The String Array contains: string1, string2, string3, ...
         *
         * @param arrayString - the String to convert
         * @return String[] containing the elements in String
         */
    String[] convertStringToStringArray(String arrayString) {
        final ParamTokenizer paramTok = new ParamTokenizer(arrayString,",");
        String[] strArray = new String[paramTok.countTokens()];
        int ii=0;
        while (paramTok.hasMoreTokens()) 
        {
            strArray[ii++] = paramTok.nextTokenWithoutEscapeAndQuoteChars();
        }
        return strArray;
    }

        /**
         * check if the variable, "skipParamValidation" is defined in the command
         * class.  If defined and set to true, then parameter validation will be
         * skipped from that command.
         * This is used mostly for command referencing.  For example list-applications
         * command references list-components command and you don't want to define
         * the same params from the class that implements list-components. 
         *
         * @param command - AdminCommand class
         * @return true if to skip param validation, else return false.
         */
    boolean skipValidation(AdminCommand command) {
            try {
                final Field f = command.getClass().getDeclaredField("skipParamValidation");
                f.setAccessible(true);
                if (f.getType().isAssignableFrom(boolean.class)) {
                    return f.getBoolean(command);
                }
            } catch (NoSuchFieldException e) {
                return false;
            } catch (IllegalAccessException e) {
                return false;
            }
            //all else return false
            return false;
        }

    // bnevins Apr 8, 2008
    private String encodeManPage(InputStream in) {
        final String eolToken = ManifestUtils.EOL_TOKEN;
        
        try {
            if(in == null)
                return null;

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            StringBuilder sb = new StringBuilder();
            
            while((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(eolToken);
            }
            return sb.toString();
        }
        catch (Exception ex) {
            return null;
        }
    }
}



