/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.enterprise.admin.remote;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;

import org.jvnet.hk2.component.*;
import com.sun.enterprise.module.*;
import com.sun.enterprise.module.single.StaticModulesRegistry;

import org.glassfish.api.admin.*;
import org.glassfish.api.admin.CommandModel.ParamModel;

import com.sun.appserv.management.client.prefs.LoginInfo;
import com.sun.appserv.management.client.prefs.LoginInfoStore;
import com.sun.appserv.management.client.prefs.LoginInfoStoreFactory;
import com.sun.appserv.management.client.prefs.StoreException;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.FileUtils;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.GFBase64Encoder;
import com.sun.enterprise.admin.util.CommandModelData;
import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import com.sun.enterprise.admin.util.CommandModelData.ParamData;
import com.sun.enterprise.admin.util.AuthenticationInfo;
import com.sun.enterprise.admin.util.HttpConnectorAddress;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.net.NetUtils;
import org.glassfish.admin.payload.PayloadFilesManager;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.admin.Payload;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Utility class for executing remote admin commands.
 * Each instance of RemoteAdminCommand represents a particular
 * remote command on a particular remote server accessed using
 * particular credentials.  The instance can be reused to execute
 * the same command multiple times with different arguments.
 * <p>
 * Arguments to the command are supplied using a ParameterMap
 * passed to the executeCommand method.
 * ParameterMap is a MultiMap where each key can have multiple
 * values, although this class only supports a single value for
 * each option.  Operands for the command are stored as the option
 * named "DEFAULT" and can have multiple values.
 * <p>
 * Before a command can be executed, the metadata for the command
 * (in the form of a CommandModel) is required.  The getCommandModel
 * method will fetch the metadata from the server, save it, and
 * return it.  If the CommandModel for a command is known
 * independently (e.g., stored in a local cache, or known a priori),
 * it can be set using the setCommandModel method.  If the
 * metadata isn't known when the exectureCommand method is
 * called, it will fetch the metadata from the server before executing
 * the command.
 * <p>
 * Any files returned by the command will be stored in the current
 * directory.  The setFileOutputDirectory method can be used to control
 * where returned files are saved.
 */
public class RemoteAdminCommand {

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(RemoteAdminCommand.class);

    private static final String QUERY_STRING_INTRODUCER = "?";
    private static final String QUERY_STRING_SEPARATOR = "&";
    private static final String ADMIN_URI_PATH = "/__asadmin/";
    private static final String COMMAND_NAME_REGEXP =
                                    "^[a-zA-Z_][-a-zA-Z0-9_]*$";
    private static final String READ_TIMEOUT = "AS_ADMIN_READTIMEOUT";
    private static final int defaultReadTimeout; // read timeout for URL conns

    private String              responseFormatType = "hk2-agent";
    private OutputStream        userOut;
    // return output string rather than printing it
    private String              output;
    private Map<String, String> attrs;
    private boolean             doUpload = false;
    private boolean             addedUploadOption = false;
    private Payload.Outbound    outboundPayload;
    private String              usage;
    private File                fileOutputDir;

    // constructor parameters
    protected String            name;
    protected String            host;
    protected int               port;
    protected boolean           secure;
    protected String            user;
    protected String            password;
    protected Logger            logger;

    // executeCommand parameters
    protected ParameterMap      options;
    protected List<String>      operands;

    private CommandModel        commandModel;
    private StringBuilder       metadataErrors; // XXX
    private int                 readTimeout = defaultReadTimeout;
    private int                 connectTimeout = -1;

    private List<Header>        requestHeaders = new ArrayList<Header>();

    /*
     * Set a default read timeout for URL connections.
     */
    static {
        String rt = System.getProperty(READ_TIMEOUT);
        if (rt == null)
            rt = System.getenv(READ_TIMEOUT);
        if (rt != null)
            defaultReadTimeout = Integer.parseInt(rt);
        else
            defaultReadTimeout = 10 * 60 * 1000;       // 10 minutes
    }

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
     * are supplied later using the execute method in the superclass.
     */
    public RemoteAdminCommand(String name, String host, int port,
            boolean secure, String user, String password, Logger logger)
            throws CommandException {
        this.name = name;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.user = user;
        this.password = password;
        this.logger = logger;
        checkName();
    }

    /**
     * Make sure the command name is legitimate and
     * won't allow any URL spoofing attacks.
     */
    private void checkName() throws CommandException {
        if (!name.matches(COMMAND_NAME_REGEXP))
            throw new CommandException("Illegal command name: " + name);
            // XXX - I18N
    }

    /**
     * Set the response type used in requests to the server.
     * The response type is sent in the User-Agent HTTP header
     * and tells the server what format of response to produce.
     */
    public void setResponseFormatType(String responseFormatType) {
        this.responseFormatType = responseFormatType;
    }

    /**
     * If set, the raw response from the command is written to the
     * specified stream.
     */
    public void setUserOut(OutputStream userOut) {
        this.userOut = userOut;
    }

    /**
     * Set the CommandModel used by this command.  Normally the
     * CommandModel will be fetched from the server using the
     * getCommandModel method, which will also save the CommandModel
     * for further use.  If the CommandModel is known in advance, it
     * can be set with this method and avoid the call to the server.
     */
    public void setCommandModel(CommandModel commandModel) {
        this.commandModel = commandModel;
    }

    /**
     * Set the read timeout for the URLConnection.
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Set the connect timeout for the URLConnection.
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Get the CommandModel for the command from the server.
     * If the CommandModel hasn't been set, it's fetched from
     * the server.
     *
     * @return the model for the command
     * @throws CommandException if the server can't be contacted
     */
    public CommandModel getCommandModel() throws CommandException {
        if (commandModel == null) {
            try {
                fetchCommandModel();
            } catch (AuthenticationException ex) {
                /*
                 * Failed to authenticate to server.
                 * If we can update our authentication information
                 * (e.g., by prompting the user for the username
                 * and password), try again.
                 */
                if (updateAuthentication())
                    fetchCommandModel();
                else
                    throw ex;
                logger.finer("Update authentication worked");
            }
        }
        return commandModel;
    }

    /**
     * Set the directory in which any returned files will be stored.
     * The default is the user's home directory.
     */
    public void setFileOutputDirectory(File dir) {
        fileOutputDir = dir;
    }

    /**
     * Return a modifiable list of headers to be added to the request.
     */
    public List<Header> headers() {
        return requestHeaders;
    }

    /**
     * Run the command using the specified arguments.
     * Return the output of the command.
     */
    public String executeCommand(ParameterMap opts) throws CommandException {
        // first, make sure we have the command model
        getCommandModel();

        options = opts;
        operands = options.get("DEFAULT");

        try {
            initializeDoUpload();

            // if uploading, we need a payload
            if (doUpload)
                outboundPayload = PayloadImpl.Outbound.newInstance();

            StringBuilder uriString = new StringBuilder(ADMIN_URI_PATH).
                    append(name).append(QUERY_STRING_INTRODUCER);
            GFBase64Encoder encoder = new GFBase64Encoder();
            ParamModel operandParam = null;
            for (ParamModel opt : commandModel.getParameters()) {
                if (opt.getParam().primary()) {
                    operandParam = opt;
                    continue;
                }
                String paramName = opt.getName();
                // XXX - no multi-value support
                String paramValue = options.getOne(paramName);
                if (paramValue == null) // perhaps it's set in the environment?
                    paramValue = getFromEnvironment(paramName);
                if (paramValue == null) {
                    /*
                     * Option still not set.  Note that we ignore the default
                     * value and don't send it explicitly on the assumption
                     * that the server will supply the default value itself.
                     *
                     * If the missing option is required, that's an error,
                     * which should never happen here because validate()
                     * should check it first.
                     */
                    if (!opt.getParam().optional())
                        throw new CommandException(strings.get("missingOption",
                                paramName));
                    // optional param not set, skip it
                    continue;
                }
                if (opt.getType() == File.class) {
                    addFileOption(uriString, paramName, paramValue);
                } else if (opt.getParam().password()) {
                    addStringOption(uriString, paramName,
                                encoder.encode(paramValue.getBytes()));
                } else
                    addStringOption(uriString, paramName, paramValue);
            }

            // add operands
            for (String operand : operands) {
                if (operandParam.getType() == File.class)
                    addFileOption(uriString, "DEFAULT", operand);
                else
                    addStringOption(uriString, "DEFAULT", operand);
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
        return output;
    }

    /**
     * After a successful command execution, the attributes returned
     * by the command are saved.  This method returns those saved
     * attributes.
     */
    public Map<String, String> getAttributes() {
        return attrs;
    }

    /**
     * Return true if we're successful in collecting new information
     * (and thus the caller should try the request again).
     * Subclasses can override to (e.g.) collect updated authentication
     * information by prompting the user.
     * The implementation in this class returns false, indicating that the
     * authentication information was not updated.
     */
    protected boolean updateAuthentication() {
        return false;
    }

    /**
     * Subclasses can override to supply parameter values from environment.
     * The implementation in this class returns null, indicating that the
     * name is not available in the environment.
     */
    protected String getFromEnvironment(String name) {
        return null;
    }

    /**
     * Called when a non-secure connection attempt fails and it appears
     * that the server requires a secure connection.
     * Subclasses can override to indicate that the connection should
     * The implementation in this class returns false, indicating that the
     * connection should not be retried.
     */
    protected boolean retryUsingSecureConnection(String host, int port) {
        return false;
    }

    /**
     * Return the error message to be used in the AuthenticationException.
     * Subclasses can override to provide a more detailed message, for
     * example, indicating the source of the password that failed.
     * The implementation in this class returns a default error message.
     */
    protected String reportAuthenticationException() {
        return strings.get("InvalidCredentials", user);
    }

    /**
     * Actually execute the remote command.
     */
    private void executeRemoteCommand(String uri) throws CommandException {
        doHttpCommand(uri, chooseRequestMethod(), new HttpCommand() {
            public void doCommand(final HttpURLConnection urlConnection)
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

            // add any user-specified headers
            for (Header h : requestHeaders)
                urlConnection.addRequestProperty(h.getName(), h.getValue());

            urlConnection.connect();
            if (doUpload) {
                outboundPayload.writeTo(urlConnection.getOutputStream());
                outboundPayload = null; // no longer needed
            }
            InputStream in = urlConnection.getInputStream();

            String responseContentType = urlConnection.getContentType();

            Payload.Inbound inboundPayload =
                PayloadImpl.Inbound.newInstance(responseContentType, in);

            PayloadFilesManager downloadedFilesMgr =
                new PayloadFilesManager.Perm(fileOutputDir, null, logger,
                    new PayloadFilesManager.ActionReportHandler() {
                        @Override
                        public void handleReport(InputStream reportStream)
                                                throws Exception {
                            handleResponse(options, reportStream,
                                urlConnection.getResponseCode(), userOut);
                        }
                    });
            try {
                downloadedFilesMgr.processParts(inboundPayload);
            } catch (CommandException cex) {
                throw cex;
            } catch (Exception ex) {
                throw new CommandException(ex);
            }
            }
        });
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
                            host, port, secure);
            logger.finer("URI: " + uriString);
            logger.finer("URL: " + url.toString());
            logger.finer("URL: " +
                    url.toURL(uriString.toString()).toString());
            logger.finer("Using auth info: User: " + user +
                ", Password: " + (ok(password) ? "<non-null>" : "<null>"));
            if (user != null || password != null)
                url.setAuthenticationInfo(
                    new AuthenticationInfo(user, password));

            urlConnection = (HttpURLConnection)
                    url.openConnection(uriString.toString());
            urlConnection.setRequestProperty("User-Agent", responseFormatType);
            urlConnection.setRequestProperty(
                    HttpConnectorAddress.AUTHORIZATION_KEY,
                    url.getBasicAuthString());
            urlConnection.setRequestMethod(httpMethod);
            urlConnection.setReadTimeout(readTimeout);
            if (connectTimeout >= 0)
                urlConnection.setConnectTimeout(connectTimeout);
            cmd.doCommand(urlConnection);
            logger.finer("doHttpCommand succeeds");

        } catch (ConnectException ce) {
            logger.finer("doHttpCommand: connect exception " + ce);
            // this really means nobody was listening on the remote server
            // note: ConnectException extends IOException and tells us more!
            String msg = strings.get("ConnectException", host, port + "");
            throw new CommandException(msg, ce);
        } catch (UnknownHostException he) {
            logger.finer("doHttpCommand: host exception " + he);
            // bad host name
            String msg = strings.get("UnknownHostException", host);
            throw new CommandException(msg, he);
        } catch (SocketException se) {
            logger.finer("doHttpCommand: socket exception " + se);
            try {
                boolean serverAppearsSecure = NetUtils.isSecurePort(host, port);
                if (serverAppearsSecure && !secure) {
                    if (retryUsingSecureConnection(host, port)) {
                        // retry using secure connection
                        secure = true;
                        try {
                            doHttpCommand(uriString, httpMethod, cmd);
                        } finally {
                            secure = false;
                        }
                        return;
                    }
                }
                throw new CommandException(se);
            } catch(IOException io) {
                // XXX - logger.printExceptionStackTrace(io);
                throw new CommandException(io);
            }
        } catch (SSLException se) {
            logger.finer("doHttpCommand: SSL exception " + se);
            try {
                boolean serverAppearsSecure = NetUtils.isSecurePort(host, port);
                if (!serverAppearsSecure && secure) {
                    logger.severe(strings.get("ServerIsNotSecure",
                                                host, port + ""));
                }
                throw new CommandException(se);
            } catch(IOException io) {
                // XXX - logger.printExceptionStackTrace(io);
                throw new CommandException(io);
            }
        } catch (IOException e) {
            logger.finer("doHttpCommand: IO exception " + e);
            String msg = "I/O Error: " + e.getMessage();
            if (urlConnection != null) {
                try {
                    int rc = urlConnection.getResponseCode();
                    if (HttpURLConnection.HTTP_UNAUTHORIZED == rc) {
                        msg = reportAuthenticationException();
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
            logger.finer("doHttpCommand: exception " + e);
            // XXX - logger.printExceptionStackTrace(e);
            throw new CommandException(e);
        }
    }

    /**
     * Get the usage text.
     * If we got usage information from the server, use it.
     *
     * @return usage text
     */
    public String getUsage() {
        return usage;
    }

    /**
     * Adds a single option expression to the URI.  Appends a '?' in preparation
     * for the next option.
     *
     * @param uriString the URI composed so far
     * @param option the option expression to be added
     * @return the URI so far, including the newly-added option
     */
    private StringBuilder addStringOption(StringBuilder uriString, String name,
            String option) {
        try {
            String encodedOption = URLEncoder.encode(option, "UTF-8");
            uriString.append(name).
                append('=').
                append(encodedOption).
                append(QUERY_STRING_SEPARATOR);
        } catch (UnsupportedEncodingException e) {
            // XXX - should never happen
            logger.severe("Error encoding value for: " + name +
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
        logger.finer("FILE PARAM: " + optionName + " = " + f);
        // attach the file to the payload
        if (doUpload)
            outboundPayload.attachFile(FILE_PAYLOAD_MIME_TYPE,
                URI.create(f.getName()),
                optionName,
                null,
                f);
        if (f != null) {
            // if we are about to upload it -- give just the name
            // o/w give the full path
            String pathToPass = (doUpload ? f.getName() : f.getPath());
            addStringOption(uriString, optionName, pathToPass);
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

    private void handleResponse(ParameterMap params,
            InputStream in, int code, OutputStream userOut)
            throws IOException, CommandException {
        if (userOut == null) {
            handleResponse(params, in, code);
        } else {
            FileUtils.copyStream(in, userOut);
        }
    }

    private void handleResponse(ParameterMap params,
            InputStream in, int code) throws IOException, CommandException {
        RemoteResponseManager rrm = null;

        try {
            rrm = new RemoteResponseManager(in, code, logger);
            rrm.process();
        } catch (RemoteSuccessException rse) {
            // save results
            output = rse.getMessage();
            attrs = rrm.getMainAtts();
            return;
        } catch (RemoteException rfe) {
            // XXX - gross
            if (rfe.getRemoteCause().indexOf("CommandNotFoundException") > 0) {
                // CommandNotFoundException from the server, then display
                // the closest matching commands
                throw new InvalidCommandException(rfe.getMessage());
            }
            throw new CommandException(
                        "remote failure: " + rfe.getMessage(), rfe);
        }
    }

    /**
     * Fetch the command metadata from the remote server.
     */
    private void fetchCommandModel() throws CommandException {

        // XXX - there should be a "help" command, that returns XML output
        //StringBuilder uriString = new StringBuilder(ADMIN_URI_PATH).
                //append("help").append(QUERY_STRING_INTRODUCER);
        //addStringOption(uriString, "DEFAULT", name);
        StringBuilder uriString = new StringBuilder(ADMIN_URI_PATH).
                append(name).append(QUERY_STRING_INTRODUCER);
        addStringOption(uriString, "Xhelp", "true");

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
                        metadataErrors = new StringBuilder();
                        commandModel =
                                parseMetadata(partIt.next().getInputStream(),
                                metadataErrors);
                        logger.finer(
                            "fetchCommandModel: got command opts: " +
                            commandModel);
                        isReportProcessed = true;
                    } else {
                        partIt.next();  // just throw it away
                    }
                }
            }
        });
        if (commandModel == null)
            throw new InvalidCommandException(metadataErrors.toString()); // XXX
    }

    /**
     * Parse the XML metadata for the command on the input stream.
     *
     * @param in the input stream
     * @return the set of ValidOptions
     */
    private CommandModel parseMetadata(InputStream in, StringBuilder errors) {
        if (logger.isLoggable(Level.FINER)) { // XXX - assume "debug" == "FINER"
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                FileUtils.copyStream(in, baos);
            } catch (IOException ex) { }
            in = new ByteArrayInputStream(baos.toByteArray());
            String response = baos.toString();
            logger.finer("------- RAW METADATA RESPONSE ---------");
            logger.finer(response);
            logger.finer("------- RAW METADATA RESPONSE ---------");
        }

        CommandModelData cm = new CommandModelData(name, null);
        boolean sawFile = false;
        try {
            DocumentBuilder d =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = d.parse(in);
            NodeList cmd = doc.getElementsByTagName("command");
            Node cmdnode = cmd.item(0);
            if (cmdnode == null) {
                Node report = doc.getElementsByTagName("action-report").item(0);
                String cause = getAttr(report.getAttributes(), "failure-cause");
                if (cause != null)
                    errors.append(cause);
                // no command info, must be invalid command or something
                // wrong with command implementation
                return null;
            }
            NamedNodeMap cmdattrs = cmdnode.getAttributes();
            usage = getAttr(cmdattrs, "usage");
            String dashOk = getAttr(cmdattrs, "unknown-options-are-operands");
            if (dashOk != null)
                cm.dashOk = Boolean.parseBoolean(dashOk);
            NodeList opts = doc.getElementsByTagName("option");
            for (int i = 0; i < opts.getLength(); i++) {
                Node n = opts.item(i);
                NamedNodeMap attrs = n.getAttributes();
                String sn = getAttr(attrs, "short");
                String def = getAttr(attrs, "default");
                String obs = getAttr(attrs, "obsolete");
                ParamModelData opt = new ParamModelData(
                        getAttr(attrs, "name"),
                        typeOf(getAttr(attrs, "type")),
                        Boolean.parseBoolean(getAttr(attrs, "optional")),
                        def,
                        ok(sn) ? sn : null,
			ok(obs) ? Boolean.parseBoolean(obs) : false);
                if (getAttr(attrs, "type").equals("PASSWORD")) {
                    opt.param._password = true;
                    opt.description = getAttr(attrs, "description");
                }
                cm.add(opt);
                if (opt.getType() == File.class)
                    sawFile = true;
            }
            // should be only one operand item
            opts = doc.getElementsByTagName("operand");
            for (int i = 0; i < opts.getLength(); i++) {
                Node n = opts.item(i);
                NamedNodeMap attrs = n.getAttributes();
                Class<?> type = typeOf(getAttr(attrs, "type"));
                if (type == File.class)
                    sawFile = true;
                int min = Integer.parseInt(getAttr(attrs, "min"));
                String max = getAttr(attrs, "max");
                if (max.equals("unlimited"))
                    type = List.class;
                ParamModelData pm = new ParamModelData(
                    getAttr(attrs, "name"), type, min == 0, null);
                pm.param._primary = true;
                cm.add(pm);
            }

            /*
             * If one of the options or operands is a FILE,
             * make sure there's also a --upload option available.
             * XXX - should only add it if it's not present
             * XXX - should just define upload parameter on remote command
             */
            if (sawFile) {
                cm.add(new ParamModelData("upload", Boolean.class,
                        true, null));
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
        return cm;
    }

    private Class<?> typeOf(String type) {
        if (type.equals("STRING"))
            return String.class;
        else if (type.equals("BOOLEAN"))
            return Boolean.class;
        else if (type.equals("FILE"))
            return File.class;
        else if (type.equals("PASSWORD"))
            return String.class;
        else if (type.equals("PROPERTIES"))
            return Properties.class;
        else
            return String.class;
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
    private void initializeDoUpload() throws CommandException {
        boolean sawFile = false;
        boolean sawDirectory = false;
        for (Map.Entry<String, List<String>> param : options.entrySet()) {
            String paramName = param.getKey();
            if (paramName.equals("DEFAULT"))    // operands handled below
                continue;
            ParamModel opt = commandModel.getModelFor(paramName);
            if (opt != null && opt.getType() == File.class) {
                sawFile = true;
                // if any FILE parameter is a directory, turn off doUpload
                String filename = param.getValue().get(0);
                File file = new File(filename);
                if (file.isDirectory())
                    sawDirectory = true;
            }
        }

        // now check the operands for files
        ParamModel operandParam = getOperandModel();
        if (operandParam != null && operandParam.getType() == File.class) {
            for (String filename : operands) {
                sawFile = true;
                // if any FILE parameter is a directory, turn off doUpload
                File file = new File(filename);
                if (file.isDirectory())
                    sawDirectory = true;
            }
        }

        if (sawFile) {
            // found a FILE param, is doUpload set?
            String upString = getOption("upload");
            if (ok(upString))
                doUpload = Boolean.parseBoolean(upString);
            else
                doUpload = !isLocal(host);
            if (sawDirectory && doUpload) {
                // oops, can't upload directories
                logger.finer("--upload=" + upString +
                                            ", doUpload=" + doUpload);
                throw new CommandException(strings.get("CantUploadDirectory"));
            }
        }

        if (addedUploadOption) {
            logger.finer("removing --upload option");
            //options.remove("upload");    // remove it
            // XXX - no remove method, have to copy it
            ParameterMap noptions = new ParameterMap();
            for (Map.Entry<String, List<String>> e : options.entrySet()) {
                if (!e.getKey().equals("upload"))
                    noptions.set(e.getKey(), e.getValue());
            }
            options = noptions;
        }

        logger.finer("doUpload set to " + doUpload);
    }

    /**
     * Does the given hostname represent the local host?
     */
    private static boolean isLocal(String hostname) {
        if (hostname.equalsIgnoreCase("localhost"))     // the common case
            return true;
        try {
            // let NetUtils do the hard work
            InetAddress ia = InetAddress.getByName(hostname);
            return NetUtils.isLocal(ia.getHostAddress());
        } catch (UnknownHostException ex) {
            /*
             * Sometimes people misconfigure their name service and they
             * can't even look up the name of their own machine.
             * Too bad.  We just give up and say it's not local.
             */
            return false;
        }
    }

    /**
     * Get the ParamModel that corresponds to the operand
     * (primary parameter).  Return null if none.
     */
    private ParamModel getOperandModel() {
        for (ParamModel pm : commandModel.getParameters()) {
            if (pm.getParam().primary())
                return pm;
        }
        return null;
    }

    /**
     * Get an option value, that might come from the command line
     * or from the environment.  Return the default value for the
     * option if not otherwise specified.
     */
    private String getOption(String name) {
        String val = options.getOne(name);
        if (val == null)
            val = getFromEnvironment(name);
        if (val == null) {
            // no value, find the default
            ParamModel opt = commandModel.getModelFor(name);
            // if no value was specified and there's a default value, return it
            if (opt != null) {
                String def = opt.getParam().defaultValue();
                if (ok(def))
                    val = def;
            }
        }
        return val;
    }

    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }
}
