/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.enterprise.admin.cli.remote;

import com.sun.appserv.management.client.prefs.LoginInfo;
import com.sun.appserv.management.client.prefs.LoginInfoStore;
import com.sun.appserv.management.client.prefs.LoginInfoStoreFactory;
import com.sun.appserv.management.client.prefs.StoreException;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.FileUtils;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.GFBase64Encoder;
import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.enterprise.admin.cli.*;
import com.sun.enterprise.admin.cli.util.*;
import com.sun.enterprise.cli.framework.ValidOption;
import com.sun.enterprise.cli.framework.CommandValidationException;
import com.sun.enterprise.cli.framework.CommandException;
import com.sun.enterprise.cli.framework.InvalidCommandException;
import com.sun.enterprise.util.net.NetUtils;
import org.glassfish.admin.payload.PayloadFilesManager;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.admin.Payload;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * A remote command handled by the asadmin CLI.
 */
public class RemoteCommand extends CLICommand {

    private static final LocalStringsImpl   strings =
            new LocalStringsImpl(RemoteCommand.class);

    private static final String QUERY_STRING_INTRODUCER = "?";
    private static final String QUERY_STRING_SEPARATOR = "&";
    private static final String ADMIN_URI_PATH = "/__asadmin/";
    private static final String COMMAND_NAME_REGEXP =
                                    "^[a-zA-Z][-a-zA-Z0-9_]*$";

    private String                          responseFormatType = "hk2-agent";
    private OutputStream                    userOut; // XXX - never set
    // return output string rather than printing it
    private boolean                         returnOutput = false;
    private String                          output;
    private boolean                         doUpload = false;
    private boolean                         addedUploadOption = false;
    private Payload.Outbound                outboundPayload;

    /**
     * content-type used for each file-transfer part of a payload to or from
     * the server
     */
    private static final String FILE_PAYLOAD_MIME_TYPE =
            "application/octet-stream";

    /**
     * Interface to enable factoring out common HTTP connection management code.
     */
    interface HttpCommand {
        public void doCommand(HttpURLConnection urlConnection)
                throws CommandException, IOException;
    }

    /**
     * Construct a new remote command object.  The command and arguments
     * are supplied later using the parse method.
     */
    public RemoteCommand(String name, ProgramOptions po, Environment env)
            throws CommandException {
        super(name, po, env);

        /*
         * Make sure the command name is legitimate and
         * won't allow any URL spoofing attacks.
         */
        if (!name.matches(COMMAND_NAME_REGEXP))
            throw new CommandException("Illegal command name: " + name);
    }

    @Override
    protected void prepare() throws CommandException {
        try {
            if (!programOpts.isOptionsSet()) {
                /*
                 * asadmin options and command options are intermixed.
                 * Parse the entire command line for asadmin options,
                 * removing them from the command line, and ignoring
                 * unknown options.
                 */
                Parser rcp = new Parser(argv, 0,
                                ProgramOptions.getValidOptions(), true);
                Map<String, String> params = rcp.getOptions();
                // program options may change
                programOpts = new ProgramOptions(params, env);
                initializeLogger();
                initializePasswords();
                List<String> operands = rcp.getOperands();
                argv = operands.toArray(new String[operands.size()]);
                // warn about deprecated use of program options
                if (params.size() > 0) {
                    // at least one program option specified after command name
                    Set<String> names = params.keySet();
                    String[] na = names.toArray(new String[names.size()]);
                    System.out.println("Deprecated syntax: " + name +
                            ", Options: " + Arrays.toString(na));
                    // XXX - recommend correct syntax
                }
            }

            initializeAuth();

            /*
             * If this is a help request, we don't need the command
             * metadata and we throw away all the other options and
             * fake everything else.
             */
            if (programOpts.isHelp()) {
                commandOpts = new HashSet<ValidOption>();
                ValidOption opt = new ValidOption("help", "BOOLEAN",
                        ValidOption.OPTIONAL, "false");
                opt.setShortName("?");
                commandOpts.add(opt);
                return;
            }
            // XXX - "asadmin --host localhost command --help" won't work

            /*
             * Find the metadata for the command.
             */
            /*
            commandOpts = cache.get(name, ts);
            if (commandOpts == null)
                // goes to server
             */
            try {
                fetchCommandMetadata();
            } catch (AuthenticationException ex) {
                /*
                 * Failed to authenticate to server.
                 * If we can update our authentication information
                 * (e.g., by prompting the user for the username
                 * and password), try again.
                 */
                if (updateAuthentication())
                    fetchCommandMetadata();
                else
                    throw ex;
            }
            if (commandOpts == null)
                throw new CommandException("Unknown command: " + name);
        } catch (CommandException cex) {
            throw cex;
        } catch (Exception e) {
            throw new CommandException(e.getMessage());
        }
    }

    /**
     * Runs the command using the specified arguments.
     */
    @Override
    protected int executeCommand()
            throws CommandException, CommandValidationException {

        try {
            initializeDoUpload();

            // if uploading, we need a payload
            if (doUpload)
                outboundPayload = PayloadImpl.Outbound.newInstance();

            StringBuilder uriString = new StringBuilder(ADMIN_URI_PATH).
                    append(name).append(QUERY_STRING_INTRODUCER);
            GFBase64Encoder encoder = new GFBase64Encoder();
            for (Map.Entry<String, String> param : options.entrySet()) {
                String paramName = param.getKey();
                String paramValue = param.getValue();

                // if we know what the command options are, we process the
                // parameters by type here
                ValidOption opt = getOption(paramName);
                if (opt == null) {      // XXX - should never happen
                    String msg = strings.get("unknownOption",
                            name, paramName);
                    throw new CommandException(msg);
                }
                if (opt.getType().equals("FILE")) {
                    addFileOption(uriString, paramName, paramValue);
                } else if (opt.getType().equals("PASSWORD")) {
                    addOption(uriString, paramName,
                                encoder.encode(paramValue.getBytes()));
                } else
                    addOption(uriString, paramName, paramValue);
            }

            // add operands
            for (String operand : operands) {
                if (operandType.equals("FILE"))
                    addFileOption(uriString, "DEFAULT", operand);
                else
                    addOption(uriString, "DEFAULT", operand);
            }

            // remove the last character, whether it was "?" or "&"
            uriString.setLength(uriString.length() - 1);
            try {
                executeRemoteCommand(uriString.toString());
            } catch (AuthenticationException ex) {
                /*
                 * Failed to authenticate to server.
                 * If we can update our authentication information
                 * (e.g., by prompting the user for the username
                 * and password), try again.
                 */
                if (updateAuthentication())
                    executeRemoteCommand(uriString.toString());
                else
                    throw ex;
            }
        } catch (IOException ioex) {
            // possibly an error caused while reading or writing a file?
            throw new CommandException("I/O Error", ioex);
        }
        return SUCCESS;
    }

    /**
     * Actually execute the remote command.
     */
    private void executeRemoteCommand(String uri) throws CommandException {
        doHttpCommand(uri, chooseRequestMethod(), new HttpCommand() {
            public void doCommand(HttpURLConnection urlConnection)
                    throws CommandException, IOException {

            if (doUpload) {
                /*
                 * If we are uploading anything then set the content-type
                 * and add the uploaded part(s) to the payload.
                 */
                urlConnection.setChunkedStreamingMode(0); // use default value
                urlConnection.setRequestProperty("Content-Type",
                        outboundPayload.getContentType());
            }
            urlConnection.connect();
            if (doUpload) {
                outboundPayload.writeTo(urlConnection.getOutputStream());
                outboundPayload = null; // no longer needed
            }
            InputStream in = urlConnection.getInputStream();

            String responseContentType = urlConnection.getContentType();

            Payload.Inbound inboundPayload =
                PayloadImpl.Inbound.newInstance(responseContentType, in);

            boolean isReportProcessed = false;
            PayloadFilesManager downloadedFilesMgr =
                    new PayloadFilesManager.Perm();
            Iterator<Payload.Part> partIt = inboundPayload.parts();
            while (partIt.hasNext()) {
                /*
                 * The report will always come first among the parts of
                 * the payload.  Be sure to process the report right away
                 * so any following data parts will be accessible.
                 */
                if (!isReportProcessed) {
                    handleResponse(options, partIt.next().getInputStream(),
                            urlConnection.getResponseCode(), userOut);
                    isReportProcessed = true;
                } else {
                    processDataPart(downloadedFilesMgr, partIt.next());
                }
            }
            }
        });
    }

    /**
     * Execute the command and return the output as a string
     * instead of writing it out.
     */
    public String executeAndReturnOutput(String... args)
            throws CommandException, CommandValidationException {
        /*
         * Tellthe low level output processing to just save the output
         * string instead of writing it out.  Yes, this is pretty gross.
         */
        returnOutput = true;
        execute(args);
        return output;
    }

    /**
     * Set up an HTTP connection, call cmd.doCommand to do all the work,
     * and handle common exceptions.
     *
     * @param uriString     the URI to connect to
     * @param httpMethod    the HTTP method to use for the connection
     * @param cmd           the HttpCommand object
     * @throws CommandException if anything goes wrong
     */
    private void doHttpCommand(String uriString, String httpMethod,
            HttpCommand cmd) throws CommandException {
        HttpURLConnection urlConnection = null;
        try {
            HttpConnectorAddress url = new HttpConnectorAddress(
                            programOpts.getHost(), programOpts.getPort(),
                            programOpts.isSecure());
            logger.printDebugMessage("URI: " + uriString.toString());
            logger.printDebugMessage("URL: " + url.toString());
            logger.printDebugMessage("URL: " +
                    url.toURL(uriString.toString()).toString());
            url.setAuthenticationInfo(
                new AuthenticationInfo(programOpts.getUser(),
                                        programOpts.getPassword()));

            urlConnection = (HttpURLConnection)
                    url.openConnection(uriString.toString());
            urlConnection.setRequestProperty("User-Agent", responseFormatType);
            urlConnection.setRequestProperty(
                    HttpConnectorAddress.AUTHORIZATION_KEY,
                    url.getBasicAuthString());
            urlConnection.setRequestMethod(httpMethod);
            cmd.doCommand(urlConnection);

        } catch (ConnectException ce) {
            // this really means nobody was listening on the remote server
            // note: ConnectException extends IOException and tells us more!
            String msg = strings.get("ConnectException",
                    programOpts.getHost(), programOpts.getPort() + "");
            throw new CommandException(msg, ce);
        } catch (UnknownHostException he) {
            // bad host name
            String msg = strings.get("UnknownHostException",
                                        programOpts.getHost());
            throw new CommandException(msg, he);
        } catch (SocketException se) {
            try {
                boolean serverAppearsSecure = NetUtils.isSecurePort(
                                programOpts.getHost(), programOpts.getPort());
                if (serverAppearsSecure != programOpts.isSecure()) {
                    String msg = strings.get("ServerMaybeSecure",
                            programOpts.getHost(), programOpts.getPort() + "");
                    logger.printError(msg);
                    throw new CommandException(se);
                }
            } catch(IOException io) {
                logger.printExceptionStackTrace(io);
                throw new CommandException(io);
            }
        } catch (IOException e) {
            String msg = "Unknown I/O Error";
            if (urlConnection != null) {
                try {
                    int rc = urlConnection.getResponseCode();
                    if (HttpURLConnection.HTTP_UNAUTHORIZED == rc) {
                        msg = strings.get("InvalidCredentials",
                                            programOpts.getUser());
                        throw new AuthenticationException(msg);
                    } else {
                        msg = "Status: " + rc;
                    }
                } catch (IOException ioex) {
                    // ignore it
                }
            }
            throw new CommandException(msg, e);
        } catch (Exception e) {
            logger.printExceptionStackTrace(e);
            throw new CommandException(e);
        }
    }

    /**
     * Return the name of the command.
     * 
     * @return  the command name
     */
    public String getCommandName() {
        return name;
    }

    private void processDataPart(final PayloadFilesManager downloadedFilesMgr,
            final Payload.Part part) throws IOException {
        /*
         * Remaining parts are typically files to be downloaded.
         */
        Properties partProps = part.getProperties();
        String dataRequestType = partProps.getProperty("data-request-type");
        if (dataRequestType.equals("file-xfer")) {
            /*
             * Treat this part as a downloaded file.  The
             * server is responsible for setting the part's file name
             * to be a valid URI, relative to the file-xfer-root or absolute,
             * that will deliver the file according to the user's
             * wishes.
             */
            downloadedFilesMgr.extractFile(part);
        }
    }

    /**
     * Adds a single option expression to the URI.  Appends a '?' in preparation
     * for the next option.
     *
     * @param uriString the URI composed so far
     * @param option the option expression to be added
     * @return the URI so far, including the newly-added option
     */
    private StringBuilder addOption(StringBuilder uriString, String name,
            String option) {
        try {
            String encodedOption = URLEncoder.encode(option, "UTF-8");
            uriString.append(name).
                append('=').
                append(encodedOption).
                append(QUERY_STRING_SEPARATOR);
        } catch (UnsupportedEncodingException e) {
            // XXX - should never happen
            logger.printError("Error encoding value for: " + name +
                    ", Value:" + option + ", parameter value will be ignored");
        }
        return uriString;
    }
    
    /**
     * Adds an option for a file argument, passing the name (for uploads) or the
     * path (for no-upload) operations. 
     *
     * @param uriString the URI string so far
     * @param optionName the option which takes a path or name
     * @param filename the name of the file
     * @return the URI string
     * @throws java.io.IOException
     */
    private StringBuilder addFileOption(
            StringBuilder uriString,
            String optionName,
            String filename) throws IOException {
        File f = SmartFile.sanitize(new File(filename));
        logger.printDebugMessage("FILE PARAM: " + optionName + " = " + f);
        // attach the file to the payload
        if (doUpload)
            outboundPayload.attachFile(FILE_PAYLOAD_MIME_TYPE,
                f.toURI(),
                optionName,
                null,
                f);
        if (f != null) {
            // if we are about to upload it -- give just the name
            // o/w give the full path
            String pathToPass = (doUpload ? f.getName() : f.getPath());
            addOption(uriString, optionName, pathToPass);
        }
        return uriString;
    }
    
    /**
     * Decide what request method to use in building the HTTP request.
     * @return the request method appropriate to the current command and options
     */
    private String chooseRequestMethod() {
        // XXX - should be part of command metadata
        if (doUpload) {
            return "POST";
        } else {
            return "GET";
        }
    }
    
    private void handleResponse(Map<String, String> params,
            InputStream in, int code, OutputStream userOut)
            throws IOException, CommandException {
        if (userOut == null) {
            handleResponse(params, in, code);
        } else {
            FileUtils.copyStream(in, userOut);
        }
    }

    private void handleResponse(Map<String, String> params,
            InputStream in, int code) throws IOException, CommandException {
        RemoteResponseManager rrm = null;

        try {
            rrm = new RemoteResponseManager(in, code);
            rrm.process();
        } catch (RemoteSuccessException rse) {
            if (returnOutput)
                output = rse.getMessage();
            else
                logger.printMessage(rse.getMessage());
            return;
        } catch (RemoteException rfe) {
            // XXX - gross
            if (rfe.getRemoteCause().indexOf("CommandNotFoundException")>0) {
                    // CommandNotFoundException from the server, then display
                    // the closest matching commands
                throw new CommandException(rfe.getMessage(),
                        new InvalidCommandException());
            }
            throw new CommandException(
                        "remote failure: " + rfe.getMessage(), rfe);
        }
    }

    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Remote command: ").append(name);
        // XXX - include more information if known
        return sb.toString();
    }

    /**
     * Get the metadata for the command from the server.
     *
     * @return the options for the command
     * @throws CommandException if the server can't be contacted
     */
    protected void fetchCommandMetadata() throws CommandException {

        // XXX - there should be a "help" command, that returns XML output
        //StringBuilder uriString = new StringBuilder(ADMIN_URI_PATH).
                //append("help").append(QUERY_STRING_INTRODUCER);
        //addOption(uriString, "DEFAULT", name);
        StringBuilder uriString = new StringBuilder(ADMIN_URI_PATH).
                append(name).append(QUERY_STRING_INTRODUCER);
        addOption(uriString, "Xhelp", "true");

        // remove the last character, whether it was "?" or "&"
        uriString.setLength(uriString.length() - 1);

        doHttpCommand(uriString.toString(), "GET", new HttpCommand() {
            public void doCommand(HttpURLConnection urlConnection)
                    throws CommandException, IOException {

                //urlConnection.setRequestProperty("Accept: ", "text/xml");
                urlConnection.setRequestProperty("User-Agent", "metadata");
                urlConnection.connect();
                InputStream in = urlConnection.getInputStream();

                String responseContentType = urlConnection.getContentType();
                Payload.Inbound inboundPayload =
                    PayloadImpl.Inbound.newInstance(responseContentType, in);

                boolean isReportProcessed = false;
                Iterator<Payload.Part> partIt = inboundPayload.parts();
                while (partIt.hasNext()) {
                    /*
                     * There should be only one part, which should be the
                     * metadata, but skip any other parts just in case.
                     */
                    if (!isReportProcessed) {
                        commandOpts =
                                parseMetadata(partIt.next().getInputStream());
                        isReportProcessed = true;
                    } else {
                        partIt.next();  // just throw it away
                    }
                }
            }
        });
    }

    /**
     * Parse the XML metadata for the command on the input stream.
     *
     * @param in the input stream
     * @return the set of ValidOptions
     */
    private Set<ValidOption> parseMetadata(InputStream in) {
        Set<ValidOption> valid = new HashSet<ValidOption>();
        boolean sawFile = false;
        try {
            DocumentBuilder d =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = d.parse(in);
            NodeList opts = doc.getElementsByTagName("option");
            for (int i = 0; i < opts.getLength(); i++) {
                Node n = opts.item(i);
                NamedNodeMap attrs = n.getAttributes();
                ValidOption opt = new ValidOption(
                        getAttr(attrs, "name"),
                        getAttr(attrs, "type"),
                        Boolean.parseBoolean(getAttr(attrs, "optional")) ?
                            ValidOption.OPTIONAL : ValidOption.REQUIRED,
                        getAttr(attrs, "default"));
                opt.setShortName(getAttr(attrs, "short"));
                valid.add(opt);
                if (opt.getType().equals("FILE"))
                    sawFile = true;
            }
            // should be only one operand item
            opts = doc.getElementsByTagName("operand");
            for (int i = 0; i < opts.getLength(); i++) {
                Node n = opts.item(i);
                NamedNodeMap attrs = n.getAttributes();
                operandType = getAttr(attrs, "type");
                operandMin = Integer.parseInt(getAttr(attrs, "min"));
                String max = getAttr(attrs, "max");
                if (max.equals("unlimited"))
                    operandMax = Integer.MAX_VALUE;
                else
                    operandMax = Integer.parseInt(max);
                if (operandType.equals("FILE"))
                    sawFile = true;
            }

            /*
             * If one of the options or operands is a FILE,
             * make sure there's also a --upload option available.
             * XXX - should only add it if it's not present
             * XXX - should just define upload parameter on remote command
             */
            if (sawFile) {
                valid.add(new ValidOption("upload", "BOOLEAN",
                        ValidOption.OPTIONAL, "false"));
                addedUploadOption = true;
            }
        } catch (ParserConfigurationException pex) {
            // ignore for now
            return null;
        } catch (SAXException sex) {
            // ignore for now
            return null;
        } catch (IOException ioex) {
            // ignore for now
            return null;
        }
        return valid;
    }

    /**
     * Return the value of a named attribute, or null if not set.
     */
    private static String getAttr(NamedNodeMap attrs, String name) {
        Node n = attrs.getNamedItem(name);
        if (n != null)
            return n.getNodeValue();
        else
            return null;
    }

    /**
     * Search all the parameters that were actually specified to see
     * if any of them are FILE type parameters.  If so, check for the
     * "--upload" option.
     */
    private void initializeDoUpload() {
        boolean sawFile = false;
        boolean sawDirectory = false;
        for (Map.Entry<String, String> param : options.entrySet()) {
            String paramName = param.getKey();
            ValidOption opt = getOption(paramName);
            if (opt != null && opt.getType().equals("FILE")) {
                sawFile = true;
                // if any FILE parameter is a directory, turn off doUpload
                String filename = param.getValue();
                File file = new File(filename);
                if (file.isDirectory())
                    sawDirectory = true;
            }
        }

        // now check the operands for files
        if (operandType != null && operandType.equals("FILE")) {
            for (String filename : operands) {
                sawFile = true;
                // if any FILE parameter is a directory, turn off doUpload
                File file = new File(filename);
                if (file.isDirectory())
                    sawDirectory = true;
            }
        }

        if (sawFile && !sawDirectory) {
            // found a FILE param, is doUpload set?
            String upString = options.get("upload");
            if (ok(upString))
                doUpload = Boolean.parseBoolean(upString);
            else
                doUpload = true;        // defaults to true
        }

        if (addedUploadOption)
            options.remove("upload");    // XXX - remove it
    }

    /**
     * Get the ValidOption descriptor for the named option.
     *
     * @param name  the option name
     * @return      the ValidOption descriptor
     */
    private ValidOption getOption(String name) {
        for (ValidOption opt : commandOpts)
            if (opt.getName().equals(name))
                return opt;
        return null;
    }

    /**
     * If we're interactive, prompt for a new username and password.
     * Return true if we're successful in collecting new information
     * (and thus the caller should try the request again).
     */
    protected boolean updateAuthentication() {
        Console cons;
        if (programOpts.isInteractive() && (cons = System.console()) != null) {
            cons.printf("%s ",
                strings.get("AdminUserPrompt", programOpts.getUser()));
            String user = cons.readLine();
            if (user == null)
                return false;
            String password = readPassword(strings.get("AdminPasswordPrompt"));
            if (password == null)
                return false;
            if (user.length() > 0)      // if none entered, don't change
                programOpts.setUser(user);
            programOpts.setPassword(password);
            return true;
        }
        return false;
    }

    private void initializeAuth() throws CommandException {
        LoginInfo li = null;
        
        try {
            LoginInfoStore store = LoginInfoStoreFactory.getDefaultStore();
            li = store.read(programOpts.getHost(), programOpts.getPort());
        } catch (StoreException se) {
            logger.printDebugMessage(
                    "Login info could not be read from ~/.asadminpass file");
        }

        // initialize user name

        if (programOpts.getUser() == null && li != null) {
            // not on command line and in .asadminpass
            programOpts.setUser(li.getUser());
        }

        // initialize password

        // this is for asadmin-login command's special processing.        
        // XXX - does this work?
        String password = programOpts.getPassword();
        /*
        if (options.get("password") != null) {
            password = options.get("password");
            options.remove("password");
            //encodedPasswords.put(CLIUtil.ENV_PREFIX+"PASSWORD",password);
            //base64encode(encodedPasswords);
        }
        */

        if (!ok(password) && li != null) {
            // not in passwordfile and in .asadminpass
            password = li.getPassword();
        }                
        programOpts.setPassword(password);
    }
}
