/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client;

import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.appclient.client.acc.UserError;

/**
 *
 * Constructs a java command to launch the ACC with the correct agent and
 * command line arguments, based on the current operating environment and
 * the user's own command-line arguments.
 * <p>
 * The user might have specified JVM options as well as ACC options as
 * well as arguments to be passed to the client.  Further, we need to make
 * sure that the GlassFish extension libraries directories and endorsed
 * directories are included in java.ext.dirs and java.endorsed.dirs,
 * regardless of whether the user specified any explicitly.
 * <p>
 * This program emits a java command line that will run the ACC so that it
 * will launch the client.  The emitted command will need to look like this:
 * <pre>
 * {@code
 * java \
 *   (user-specified JVM options except classpath, cp, jar) \
 *   (settings for java.ext.dirs and java.endorsed.dirs) \
 *   -javaagent:(path-to-gf-client.jar)=(option string for our agent) \
 *   (main class setting: "-jar x.jar" or "a.b.Main" or "path-to-file.class")
 *   (arguments to be passed to the client)
 * }</pre>
 * <p>
 * 
 * @author Tim Quinn
 */
public class CLIBootstrap {

    private final static boolean isDebug = System.getenv("AS_DEBUG") != null;
    private final static String INPUT_ARGS = System.getenv("inputArgs");
    
    final static String ENV_VAR_PROP_PREFIX = "acc.";

    private final static String JAVA_NAME = (File.separatorChar == '/' ? "java" : "java.exe");

    /** options to the ACC that take a value */
    private final static String ACC_VALUED_OPTIONS_PATTERN = 
            "-mainclass|-name|-xml|-configxml|-user|-password|-passwordfile|-targetserver";

    /** options to the ACC that take no value */
    private final static String ACC_UNVALUED_OPTIONS_PATTERN =
            "-textauth|-noappinvoke|-usage|-help";

    private final static String JVM_VALUED_OPTIONS_PATTERN =
            "-classpath|-cp";

    private final static String INSTALL_ROOT_PROPERTY_EXPR = "-Dcom.sun.aas.installRoot=";
    private final static String SECURITY_POLICY_PROPERTY_EXPR = "-Djava.security.policy=";
    private final static String SECURITY_AUTH_LOGIN_CONFIG_PROPERTY_EXPR = "-Djava.security.auth.login.config=";
    private final static String SYSTEM_CLASS_LOADER_PROPERTY_EXPR =
            "-Djava.system.class.loader=org.glassfish.appclient.client.acc.agent.ACCAgentClassLoader";

    private final static String[] ENV_VARS = {
        "_AS_INSTALL", "APPCPATH", "VMARGS", "AS_JAVA", "JAVA_HOME", "PATH"};

    private static final LocalStringManager localStrings = new LocalStringManagerImpl(CLIBootstrap.class);

    private JavaSelector java;

    private GlassFishInfo gfInfo;

    /**
     * set up during init with various subtypes of command line elements
     */
    private CommandLineElement
            extDirs, endorsedDirs, accValuedOptions, accUnvaluedOptions,
            jvmPropertySettings, jvmValuedOptions, otherJVMOptions, arguments;

    /** arguments passed to the ACC Java agent */
    private final AgentArgs agentArgs = new AgentArgs();

    /** records how the user specifies the main class: -jar xxx.jar, -client xxx.jar, or a.b.MainClass */
    private final JVMMainOption jvmMainSetting = new JVMMainOption();

    /** command line elements from most specific to least specific matching pattern */
    private final List<CommandLineElement> elements = new ArrayList<CommandLineElement>();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            /*
             * Convert env vars to properties.  (This makes testing easier.)
             */
            envToProps();
            final CLIBootstrap boot = new CLIBootstrap();
            /*
             * Because of how Windows passes arguments, the calling Windows
             * script assigned the input arguments to an environment variable.
             * Parse that variable's value into the actual arguments.
             */
            if (INPUT_ARGS != null) {
                args = convertInputArgsVariable(INPUT_ARGS);
            }
            final String outputCommandLine = boot.run(args);
            if (isDebug) {
                System.err.println(outputCommandLine);
            }
            System.out.println(outputCommandLine);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        } catch (UserError ue) {
            ue.displayAndExit();
        }
    }

    private static String[] convertInputArgsVariable(final String inputArgs) {
        /*
         * The pattern matches a quoted string (double quotes around a string
         * containing no double quote) or a non-quoted string (a string containing
         * no white space or quotes).
         */
        final Pattern argPattern = Pattern.compile("\"([^\"]+)\"|([^\"\\s]+)");

        final Matcher matcher = argPattern.matcher(inputArgs);
        final List<String> argList = new ArrayList<String>();
        while (matcher.find()) {
            final String arg = (matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
            argList.add(arg);
            if (isDebug) {
                System.err.println("Captured argument " + arg);
            }
        }
        return argList.toArray(new String[argList.size()]);
    }

    private static void envToProps() {
        for (String envVar : ENV_VARS) {
            final String value = System.getenv(envVar);
            if (value != null) {
                System.setProperty(ENV_VAR_PROP_PREFIX + envVar, value);
            }
        }
    }

    CLIBootstrap() throws UserError {
        init();
        }

    private void init() throws UserError {
        java = chooseJava();
        gfInfo = new GlassFishInfo();

        /*
         * Assign the various command line element matchers.  See the
         * descriptions of each subtype for what each is used for.
         */
        extDirs = new OverridableDefaultedPathBasedOption(
            "-Djava.ext.dirs",
            java.ext().getAbsolutePath(),
            gfInfo.extPaths());

        endorsedDirs = new OverridableDefaultedPathBasedOption(
            "-Djava.endorsed.dirs",
            java.endorsed().getAbsolutePath(),
            gfInfo.endorsedPaths());

        accValuedOptions = new ACCValuedOption(ACC_VALUED_OPTIONS_PATTERN);

        accUnvaluedOptions = new ACCUnvaluedOption(ACC_UNVALUED_OPTIONS_PATTERN);

        jvmPropertySettings = new JVMOption("-D.*");

        jvmValuedOptions = new JVMValuedOption(JVM_VALUED_OPTIONS_PATTERN);

        otherJVMOptions = new JVMOption("-.*");

        arguments = new CommandLineElement(".*");

        initCommandLineElements();
    }

    /**
     * Populates the command line elements collection to contain the elements
     * from most specific matching pattern to least specific.
     */
    private void initCommandLineElements() {
        final List<CommandLineElement> result = new ArrayList<CommandLineElement>();

        /*
         * Add the elements in this order so the regex patterns will match
         * the correct elements.  In this arrangement, the patterns are from
         * most specific to most general.
         */
        result.add(extDirs);
        result.add(endorsedDirs);
        result.add(accValuedOptions);
        result.add(accUnvaluedOptions);
        result.add(jvmValuedOptions);
        result.add(jvmPropertySettings);
        result.add(jvmMainSetting);
        result.add(otherJVMOptions);
        result.add(arguments);
        elements.addAll(result);
    }

    private static String quote(final String s) {
        return '\"' + s + '\"';
    }

    /**
     * Manages the arguments which will be passed to the ACC Java agent.
     */
    private class AgentArgs {
        private final StringBuilder args = new StringBuilder("=mode=acscript");
        private char sep = ',';

        AgentArgs() {
            final String appcPath = System.getProperty(ENV_VAR_PROP_PREFIX + "APPCPATH");
            if (appcPath != null && appcPath.length() > 0) {
                add("appcpath=" + quote(appcPath));
            }
        }

        /**
         * Adds an item to the Java agent arguments.
         * @param item
         */
        final void add(final String item) {
            args.append(sep).append(item);
        }

        /**
         * Adds an ACC argument to the Java agent arguments.
         * @param accArg
         */
        final void addACCArg(final String accArg) {
            add("arg=" + accArg);
        }

        @Override
        public String toString() {
            return args.toString();
        }
    }

    /**
     * A command-line element.  Various subtypes have some different behavior
     * for some of the methods.
     */
    private class CommandLineElement {

        private class OptionValue {
            private String optionName;
            private String value;
        }

        private final Pattern pattern;
        Matcher matcher;

        /** allows multiple values; not all command line elements support this*/
        final List<String> values = new ArrayList<String>();

        CommandLineElement(String patternString) {
            pattern = Pattern.compile(patternString);
        }

        final boolean matchesPattern(final String element) {
            matcher = pattern.matcher(element);
            return matcher.matches();
        }

        boolean matches(final String element) {
            return matchesPattern(element);
        }

        /**
         * Processes the command line element at args[slot], possibly
         * consuming the next array element as well if appropriate.
         * @param args
         * @param slot
         * @return
         * @throws UserError
         */
        int processValue(String[] args, int slot) throws UserError {
            values.add(args[slot++]);
            return slot;
        }

        /**
         * Adds the command-line element to the Java agent arguments, if
         * appropriate.
         *
         * @param element
         */
        void addToAgentArgs(final String element) {
        }

        /**
         * Returns whether there is a next argument.
         * @param args
         * @param currentSlot
         * @return
         */
        boolean isNextArg(String[] args, int currentSlot) {
            return currentSlot < args.length - 1;
        }

        /**
         * Returns the next argument in the array, without advancing
         * the pointer into the array.
         *
         * @param args
         * @param currentSlot
         * @return
         */
        String nextArg(String[] args, int currentSlot){
            return args[currentSlot + 1];
        }

        /**
         * Makes sure that there is a next argument and that its value does
         * not start with a "-" which would indicate an option, rather than
         * the value for the option we are currently processing.
         *
         * @param args
         * @param currentSlot
         * @throws UserError
         */
        void ensureNonOptionNextArg(final String[] args, final int currentSlot) throws UserError {
            if ((currentSlot >= args.length - 1) || (args[currentSlot + 1].charAt(0) == '-')) {
                throw new UserError("Command line element " + args[currentSlot] + " requires non-option value");
            }
        }

        /**
         * Adds a representation for this command-line element to the output
         * command line.
         *
         * @param commandLine
         * @return
         */
        StringBuilder format(final StringBuilder commandLine) {
            return format(commandLine, true);
        }

        /**
         * Adds a representation for this command-line element to the output
         * command line, quoting the value if requested.
         *
         * @param commandLine
         * @param useQuotes
         * @return
         */
        StringBuilder format(final StringBuilder commandLine, boolean useQuotes) {
            boolean needSep = false;
            for (String value : values) {
                if (needSep) {
                    commandLine.append(',');
                }
                format(commandLine, useQuotes, value);
            }
            commandLine.append(' ');
            return commandLine;
        }

        /**
         * Adds a representation for the specified value to the output
         * command line, quoting the value if required and
         * @param commandLine
         * @param useQuotes
         * @param v
         * @return
         */
        StringBuilder format(final StringBuilder commandLine, 
                final boolean useQuotes, final String v) {
            if (commandLine.length() > 0) {
                commandLine.append(' ');
            }
            commandLine.append((useQuotes ? quote(v) : v));
            return commandLine;
        }
    }

    /**
     * A command-line option (an element which starts with "-").
     */
    private class Option extends CommandLineElement {

        Option(String patternString) {
            super(patternString);
        }
    }

    /**
     * A JVM command-line option. Only JVM options which appear before the
     * main class setting are propagated to the output command line as
     * JVM options.
     *
     */
    private class JVMOption extends Option {
        JVMOption(final String patternString) {
            super(patternString);
        }

        @Override
        boolean matches(final String element) {
            /*
             * Although the element might match the pattern (-.*) we do
             * not treat this as JVM option if we have already processed
             * the main class determinant.
             */
            return ( ! jvmMainSetting.isSet()) && super.matches(element);
        }
    }

    /**
     * ACC options match anywhere on the command line unless and until we
     * see "-jar xxx" in which case we impose the Java-style restriction that
     * anything which follows the specification of the main class is an
     * argument to be passed to the application.
     */
    private class ACCUnvaluedOption extends Option {
        ACCUnvaluedOption(final String patternString) {
            super(patternString);
        }

        @Override
        boolean matches(final String element) {
            return ( ! jvmMainSetting.isJarSetting()) && super.matches(element);
        }

        @Override
        int processValue(String[] args, int slot) throws UserError {
            final int result = super.processValue(args, slot);
            agentArgs.addACCArg(values.get(values.size() - 1));
            return result;
        }

        @Override
        StringBuilder format(final StringBuilder commandLine) {
            /*
             * We do not send ACC arguments to the Java command line.  They
             * are placed into the agent argument string instead.
             */
            return commandLine;
        }
    }

    /**
     * An option that takes a value as the next command line element.
     */
    private class ValuedOption extends Option {

        class OptionValue {
            private String option;
            private String value;

            OptionValue(final String option, final String value) {
                this.option = option;
                this.value = value;
            }
        }

        List<OptionValue> optValues = new ArrayList<OptionValue>();

        ValuedOption(final String patternString) {
            super(patternString);
        }
        
        @Override
        int processValue(String[] args, int slot) throws UserError {
            ensureNonOptionNextArg(args, slot);
            optValues.add(new OptionValue(args[slot++], args[slot++]));

            return slot;
        }
        
        @Override
        StringBuilder format(final StringBuilder commandLine) {
            for (OptionValue ov : optValues) {
                format(commandLine, false /* useQuotes */, ov.option);
                format(commandLine, true /* useQuotes */, ov.value);
            }
            return commandLine;
        }
    }

    private class JVMValuedOption extends ValuedOption {

        JVMValuedOption(final String patternString) {
            super(patternString);
        }

        @Override
        boolean matches(final String element) {
            return ( ! jvmMainSetting.isJarSetting()) && super.matches(element);
        }
    }

    /**
     * ACC options can appear until "-jar xxx" on the command line.
     */
    private class ACCValuedOption extends ValuedOption {
        ACCValuedOption(final String patternString) {
            super(patternString);
        }

        @Override
        boolean matches(final String element) {
            return ( ! jvmMainSetting.isJarSetting()) && super.matches(element);
        }

        @Override
        int processValue(String[] args, int slot) throws UserError {
            final int result = super.processValue(args, slot);
            final OptionValue newOptionValue = optValues.get(optValues.size() - 1);
            agentArgs.addACCArg(newOptionValue.option);
            agentArgs.addACCArg(quote(newOptionValue.value));
            return result;
        }

        @Override
        StringBuilder format(final StringBuilder commandLine) {
            /*
             * We do not send ACC arguments to the Java command line.  They
             * are placed into the agent argument string instead.
             */
            return commandLine;
        }
    }

    /**
     * Command line element(s) with which the user specified the client
     * to be run.  Note that once "-jar xxx" is specified then all
     * subsequent arguments are passed to the client as arguments.
     * Once "-client xxx" is specified then subsequent arguments are treated
     * as ACC options (if they match) or arguments to the client.
     */
    private class JVMMainOption extends CommandLineElement {
        private static final String JVM_MAIN_PATTERN =
                "-jar|-client|[^-][^\\s]*";

        private String introducer = null;

        JVMMainOption() {
            super(JVM_MAIN_PATTERN);
        }

        boolean isJarSetting() {
            return "-jar".equals(introducer);
        }

        boolean isClientSetting() {
            return "-client".equals(introducer);
        }

        boolean isClassSetting() {
            return ( ! isJarSetting() && ! isClientSetting() && isSet());
        }

        boolean isSet() {
            return ! values.isEmpty();
        }

        @Override
        boolean matches(String element) {
            /*
             * For backward compatibility, the -client element can appear
             * multiple times with the last appearance overriding earlier ones.
             */
            return  (( ! isSet()) ||
                     ( (isClientSetting() && element.equals("-client")))
                    )
                    && super.matches(element);
        }

        @Override
        int processValue(String[] args, int slot) throws UserError {
            /*
             * We only care about the most recent setting.
             */
            values.clear();

            /*
             * If arg[slot] is -jar or -client we expect the
             * next value to be the file.  Make sure there is
             * a next item and that it does not start with -.
             */
            if (args[slot].charAt(0) == '-') {
                if (nextLooksOK(args, slot)) {
                    introducer = args[slot++];
                    final int result = super.processValue(args, slot);
                    final String path = values.get(values.size() - 1);
                    final File clientSpec = new File(path);
                    if (clientSpec.isDirectory()) {
                        /*
                         * Record in the agent args that the user is launching
                         * a directory. Set the main class launch info to
                         * launch the ACC JAR.
                         */
                        agentArgs.add("client=dir=" + quote(clientSpec.getAbsolutePath()));
                        introducer = "-jar";
                        values.set(values.size() - 1, gfInfo.agentJarPath());
                    } else {
                        agentArgs.add("client=jar=" + path);
                        /*
                         * The client path is not a directory.  It should be a
                         * .jar or a .ear file.  If an EAR, then we want Java to
                         * launch our ACC jar.  If a JAR, then we will launch
                         * that JAR.
                         */
                        if (path.endsWith(".ear")) {
                            introducer = "-jar";
                            values.set(values.size() - 1, gfInfo.agentJarPath());
                        }
                    }
                    return result;
                } else {
                    throw new UserError("-jar or -client requires value but missing");
                }
            } else {
                /*
                 * This must be a main class specified on the command line.
                 */
                final int result = super.processValue(args, slot);
                agentArgs.add("client=class=" + values.get(values.size() - 1));
                return result;
                
            }
        }
        
        @Override
        StringBuilder format(final StringBuilder commandLine) {
            if (introducer != null) {
                /*
                 * In the generated command we always use "-jar" to indicate
                 * the JAR to be launched, even if the user specified "-client"
                 * on the appclient command line.
                 */
                super.format(commandLine, false /* useQuotes */, "-jar");
                return super.format(commandLine, true /* useQuotes */);
            }
            return super.format(commandLine, false /* useQuotes */);
        }

        private boolean nextLooksOK(final String[] args, final int slot) {
            return (isNextArg(args, slot) && (args[slot+1].charAt(0) != '-'));
        }
    }

    /**
     * A JVM option that uses values from the GlassFish installation plus default
     * value(s) from the Java installation.  If the user specifies one of these
     * options on the command line then we discard the Java installation values
     * and append the GlassFish values to the user's values.
     * <p>
     * This is used for handling java.ext.dirs and java.endorsed.dirs property
     * settings.  If the user does not specify the property then the user would
     * expect the Java-provided directories to be used. We need to
     * specify the GlassFish ones, so that means we need combine the GlassFish
     * ones and the default JVM ones explicitly.
     * <p>
     * On the other hand, if the user specifies the property then the JVM
     * defaults are out of play. We still need the GlassFish directories to be
     * used though.
     */
    private class OverridableDefaultedPathBasedOption extends JVMOption {

        private final String defaultValue;
        private final List<String> gfValues;
        private final String patternString;
        
        OverridableDefaultedPathBasedOption(final String pattern,
                final String defaultValue,
                final String... gfValues) {
            super(pattern);
            patternString = pattern;
            this.defaultValue = defaultValue;
            this.gfValues = Arrays.asList(gfValues);
        }
        
        
        @Override
        StringBuilder format(final StringBuilder commandLine) {
            final List<String> combinedValues = new ArrayList<String>();
            if (values.isEmpty()) {
                /*
                 * The user did not specify this property, so we use
                 * the GlassFish value(s) plus the JVM default.
                 */
                combinedValues.addAll(gfValues);
                combinedValues.add(defaultValue);
            } else {
                /*
                 * The user did specify this property, so we use
                 * the user's value plus the GlassFish value(s).
                 */
                combinedValues.addAll(gfValues);
            }
            commandLine.append(patternString).append("=");
            boolean needSep = false;
            for (String value : combinedValues) {
                if (needSep) {
                    commandLine.append(File.pathSeparatorChar);
                }
                commandLine.append(quote(value));
                needSep = true;
            }
            commandLine.append(' ');
            return commandLine;
        }
    }

    /**
     * Adds JVM properties for various ACC settings.
     * @param command
     */
    private void addProperties(final StringBuilder command) {

        command.append(' ').append(INSTALL_ROOT_PROPERTY_EXPR).append(quote(gfInfo.home().getAbsolutePath()));
        command.append(' ').append(SECURITY_POLICY_PROPERTY_EXPR).append(quote(gfInfo.securityPolicy().getAbsolutePath()));
        command.append(' ').append(SYSTEM_CLASS_LOADER_PROPERTY_EXPR);
        command.append(' ').append(SECURITY_AUTH_LOGIN_CONFIG_PROPERTY_EXPR).append(quote(gfInfo.loginConfig().getAbsolutePath()));
        
    }

    /**
     * Processes the user-provided command-line elements and creates the
     * resulting output string.
     *
     * @param args
     * @throws UserError
     */
    private String run(String[] args) throws UserError {


        java = chooseJava();
        gfInfo = new GlassFishInfo();

        final String[] augmentedArgs = new String[args.length + 2];
        augmentedArgs[0] = "-configxml";
        augmentedArgs[1] = gfInfo.configxml().getAbsolutePath();
        for (int i = 0; i < args.length; i++) {
            augmentedArgs[i + 2] = args[i];
        }

        /*
         * Process each command-line argument by the first CommandLineElement
         * which matches the argument.
         */
        for (int i = 0; i < augmentedArgs.length; ) {
            boolean isMatched = false;
            for (CommandLineElement cle : elements) {
                if (isMatched = cle.matches(augmentedArgs[i])) {
                    i = cle.processValue(augmentedArgs, i);
                    break;
                }
            }
            if ( ! isMatched) {
                throw new UserError("arg " + i + " = " + augmentedArgs[i] + " not recognized");
            }
        }
        
        final StringBuilder command = new StringBuilder(quote(java.javaExe.getAbsolutePath()));

        addProperties(command);

        jvmPropertySettings.format(command);
        otherJVMOptions.format(command);
        extDirs.format(command);
        endorsedDirs.format(command);

        /*
         * If the user did not specify a client then add the -usage option.
         */
        if ( ! jvmMainSetting.isSet()) {
            accUnvaluedOptions.processValue(new String[] {"-usage"}, 0);
        }
        accUnvaluedOptions.format(command);
        accValuedOptions.format(command);
        
        addAgentOption(command);

        jvmMainSetting.format(command);
        arguments.format(command);

        return command.toString();
    }

    /**
     * Adds the -javaagent option to the command line.
     * 
     * @param command
     */
    private void addAgentOption(final StringBuilder command) {
        command.append(' ')
                .append("-javaagent:")
                .append(quote(gfInfo.agentJarPath()))
                .append(agentArgs.toString());
    }

    /**
     * Encapsulates information about the GlassFish installation, mostly useful
     * directories within the installation.
     * <p>
     * Note that we use the property acc._AS_INSTALL to find the installation.
     */
    class GlassFishInfo {

        private final File home;
        private final File modules;
        private final File lib;
        private final File libAppclient;

        GlassFishInfo() {
            final String asInstallPath = System.getProperty(ENV_VAR_PROP_PREFIX + "_AS_INSTALL");
            if (asInstallPath == null || asInstallPath.length() == 0) {
                throw new IllegalArgumentException("_AS_INSTALL == null");
            }
            this.home = new File(asInstallPath);
            modules = new File(home, "modules");
            lib = new File(home, "lib");
            libAppclient = new File(lib, "appclient");
        }

        File home() {
            return home;
        }

        File modules() {
            return modules;
        }

        File lib() {
            return lib;
        }

        File configxml() {
            return new File(new File(home, "domains/domain1/config"), "sun-acc.xml");
        }

        String[] endorsedPaths() {
            return new String[] {
                    new File(lib, "endorsed").getAbsolutePath(),
                    new File(modules, "endorsed").getAbsolutePath()};
        }

        String extPaths() {
            return new File(lib, "ext").getAbsolutePath();
        }

        String agentJarPath() {
            return new File(lib, "gf-client.jar").getAbsolutePath();
        }

        File securityPolicy() {
            return new File(libAppclient, "client.policy");
        }

        File loginConfig() {
            return new File(libAppclient, "appclientlogin.conf");
        }
    }

    /**
     * Returns a JavaSelector for AS_JAVA, JAVA_HOME, or the PATH, based on
     * which of those environment variables was set and whether the setting
     * actually seems like a valid Java home directory.
     * <p>
     * Note that we look in that order, and if the user specifies AS_JAVA or
     * JAVA_HOME we will use that setting, even if it does not correspond to
     * a valid Java installation but there is one on the path.
     *
     * @return which Java was selected
     * @throws UserError
     */
    JavaSelector chooseJava() throws UserError {
        for (JavaSelector js : possibleJavaLocations) {
            js.init();
            if (js.isSpecifiedByUser()) {
                if ( ! js.isValid()) {
                    final String msg = localStrings.getLocalString(
                            CLIBootstrap.class,
                            "appclient.boot.noJavaWhereSpecified",
                            "Could not find an executable java {0} using {1}={2}",
                            new Object[] {
                                js.javaExe.getAbsolutePath(),
                                js.name(),
                                js.selectorValue});
                    throw new UserError(msg);
                }
                return js;
            }
        }
        throw new UserError("JAVA??");
    }

    /**
     * Describes the possible ways we can choose a Java instance for the user.
     */
    enum JavaSelector {
        AS_JAVA(),
        JAVA_HOME(),
        PATH() {

            @Override
            File javaExe(final String where) {
                final String[] pathElements = where.split(File.pathSeparator);
                for (String pathElement : pathElements) {
                    final File javaFile = new File(pathElement, JAVA_NAME);
                    if (javaFile.canExecute()) {
                        return javaFile;
                    }
                }
                /*
                 * Use a false but helpful name for error reporting
                 * if we didn't find java[.exe] anywhere on the path...which
                 * seems unlikely since this program itself is running.
                 */
                return new File("PATH", JAVA_NAME);
            }
        };

        
        protected String selectorValue;
        protected File javaExe;
        protected File javaHome;

        private JavaSelector() {
            init();
        }

        protected void init() {
            selectorValue = System.getProperty(ENV_VAR_PROP_PREFIX + name());
            if (isSpecifiedByUser()) {
                javaExe = javaExe(selectorValue);
                javaHome = isValid() ? javaExe.getParentFile().getParentFile() : null ;
            } else {
                javaExe = null;
                javaHome = null;
            }
        }

        protected boolean isSpecifiedByUser() {
            return selectorValue != null && selectorValue.length() > 0;
        }

        protected boolean isValid() {
            return javaExe != null && javaExe.canExecute();
        }

        protected File javaBinDir(final String home) {
            return new File(home, "bin");
        }

        File javaExe(final String where) {
            if (where == null) {
                return null;
            }
            return new File(javaBinDir(where), JAVA_NAME);
        }

        File endorsed() {
            return new File(lib(), "endorsed");
        }

        File ext() {
            return new File(lib(), "ext");
        }

        File lib() {
            return new File(javaHome, "lib");
        }
    }

    final JavaSelector[] possibleJavaLocations =
        {JavaSelector.AS_JAVA, JavaSelector.JAVA_HOME, JavaSelector.PATH};


    /**
     * Primarily for testing.  Causes the enum selectors to reinitialize to
     * account for any changes in system property settings.
     */
    void reset() throws UserError {
        java = chooseJava();
        init();
    }

}
