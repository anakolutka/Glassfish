/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * Header Notice in each file and include the License file 
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.  
 * If applicable, add the following below the CDDL Header, 
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.impl.Utils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.v3.common.ActionReporter;
import com.sun.enterprise.v3.common.HTMLActionReporter;
import com.sun.enterprise.v3.common.PropsFileActionReporter;
import com.sun.grizzly.tcp.Request;
import com.sun.grizzly.tcp.Response;
import com.sun.grizzly.tcp.http11.InternalOutputBuffer;
import com.sun.grizzly.util.buf.ByteChunk;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.container.Adapter;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listen to admin commands...
 * @author dochez
 */
@Service
public class AdminAdapter implements Adapter {
    
    public final static String PREFIX_URI="/__asadmin";
    public final static Logger logger = LogDomains.getLogger(LogDomains.ADMIN_LOGGER);
    public final static LocalStringManagerImpl adminStrings = new LocalStringManagerImpl(AdminAdapter.class);
    public final static String GFV3 = "gfv3";
    private final static String GET = "GET";
    private final static String POST = "POST";    

    @Inject
    ModulesRegistry modulesRegistry;
    
    @Inject
    CommandRunner commandRunner;

        
    /**
     * Call the service method, and notify all listeners
     *
     * @exception Exception if an error happens during handling of
     *   the request. Common errors are:
     *   <ul><li>IOException if an input/output error occurs and we are
     *   processing an included servlet (otherwise it is swallowed and
     *   handled by the top level error handler mechanism)
     *       <li>ServletException if a servlet throws an exception and
     *  we are processing an included servlet (otherwise it is swallowed
     *  and handled by the top level error handler mechanism)
     *  </ul>
     *  Tomcat should be able to handle and log any other exception ( including
     *  runtime exceptions )
     */
    public void service(Request req, Response res)
        throws Exception {
        
        Utils.getDefaultLogger().finer("Admin adapter !");
        Utils.getDefaultLogger().finer("Received something on " + req.requestURI());        
        Utils.getDefaultLogger().finer("QueryString = " + req.queryString());

        // so far, I only use HTMLActionReporter, but I should really look at
        // the request client.
        ActionReporter report;
        if (req.getHeader("User-Agent").startsWith("hk2")) {
            report = new PropsFileActionReporter();
        } else {
            report = new HTMLActionReporter();
        }
        
        doCommand(req, report);
        
        InternalOutputBuffer outputBuffer = (InternalOutputBuffer) res.getOutputBuffer();
        res.setStatus(200);
        res.setContentType("text/html");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        report.writeReport(bos);
        res.setContentLength(bos.size());
        outputBuffer.flush();
        outputBuffer.realWriteBytes(bos.toByteArray(), 0, bos.size());

        res.finish();
    }

    private void doCommand(Request req, ActionReport report) {

        String requestURI = req.requestURI().toString();
        if (!requestURI.startsWith(PREFIX_URI)) {
            String msg = adminStrings.getLocalString("adapter.panic",
                    "Wrong request landed in AdminAdapter {0}", requestURI);
            report.setMessage(msg);
            Utils.getDefaultLogger().info(msg);
            return;
        }
        
         // wbn handle no command and no slash-suffix
        String command = "";
        
        if(requestURI.length() > PREFIX_URI.length() + 1)
            command = requestURI.substring(PREFIX_URI.length()+1);

        final Properties parameters =  extractParameters(req.queryString().toString());
        try {
            if (req.method().toString().equalsIgnoreCase(GET)) {
                logger.fine("***** AdminAdapter GET  *****");
                commandRunner.doCommand(command, parameters, report);            
            }
            else if (req.method().toString().equalsIgnoreCase(POST)) {
                logger.fine("***** AdminAdapter POST *****");
                if (parameters.get("path")!=null) {
                    try {
                        final String uploadFile = doUploadFile(req, report, parameters.getProperty("path"));
                        parameters.setProperty("path", uploadFile);
                        commandRunner.doCommand(command, parameters, report);
                    }
                    catch (IOException ioe) {
                        logger.log(Level.WARNING, ioe.getMessage());
                            //log the exception message to server log
                            //client recieves error message embedded in report object
                    }
                }
            }
        } catch (Throwable t) {
            /*
             * Must put the error information into the report
             * for the client to see it.
             */
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(t);
            report.setMessage(t.getLocalizedMessage());
            report.setActionDescription("Last-chance AdminAdapter exception handler");
        }
    }


    /**
     * Finish the response and recycle the request/response tokens. Base on
     * the connection header, the underlying socket transport will be closed
     */   
    public void afterService(Request req, Response res) throws Exception {
        
    }


    /**
     * Notify all container event listeners that a particular event has
     * occurred for this Adapter.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param data Event data
     */
    public void fireAdapterEvent(String type, Object data) {

    }

    /**
     * Returns the context root for this adapter
     *
     * @return context root
     */
    public String getContextRoot() {
        return PREFIX_URI;
    }

    /**
     * uploads request from client and save the content in <os temp dir>/gfv3/<fileName>
     * @param req to process
     * @param report back to the client
     * @param fileName to save client request
     * @return <os temp dir>/gfv3/<fileName>
     * @throws IOException if upload file cannot be created
     */
    private String doUploadFile(final Request req, final ActionReport report, final String fileName)
        throws IOException
    {
        final String localTmpDir = System.getProperty("java.io.tmpdir");
        final File gfv3Folder = new File (localTmpDir, GFV3);
        File uploadFile = null;
        FileOutputStream fos = null;
        String uploadFilePath = null;

        try {
            if (!gfv3Folder.exists()) {
                gfv3Folder.mkdirs();
            }
            uploadFile = new File (gfv3Folder, fileName);
                //check for pre-existing file
            if (uploadFile.exists()) {
                if (!uploadFile.delete()) {
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage("cannot delete existing file: " + uploadFile);
                    throw new IOException("cannot delete existing file: " + uploadFile);
                }
            }
            
            uploadFilePath = uploadFile.getCanonicalPath();
            fos = new FileOutputStream(uploadFile);
            ByteChunk bc = new ByteChunk(1024*64);
            com.sun.grizzly.tcp.InputBuffer ib = req.getInputBuffer();
            for (int ii=req.doRead(bc); ii>0; ii=req.doRead(bc)) {
                fos.write(bc.getBytes(), bc.getOffset(), ii);
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            report.setMessage("upload file successful: " + uploadFilePath);
        }
        catch (Exception e) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("upload file failed: " + uploadFilePath);
            report.setFailureCause(e);
            throw new IOException("upload file failed: " + uploadFilePath);
        }
        finally {
           if (fos != null) fos.close();
        }
        return uploadFilePath;
    }

        /**
         *  extract parameters from URI and save it in Properties obj
         *  
         *  @params requestString string URI to extract
         *
         *  @returns Properties
         */
    Properties extractParameters(final String requestString) {
        // extract parameters...
        final Properties parameters = new Properties();
        StringTokenizer stoken = new StringTokenizer(requestString, "?");
        while (stoken.hasMoreTokens()) {
            String token = stoken.nextToken();
            if (token.indexOf("=")==-1)
                continue;
            String paramName = token.substring(0, token.lastIndexOf("="));
            String value = token.substring(token.lastIndexOf("=")+1);
            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.log(Level.WARNING, adminStrings.getLocalString("adapter.param.decode",
                        "Cannot decode parameter {0} = {1}"));
            }
            parameters.setProperty(paramName, value);
        }

        // Dump parameters...
        if (logger.isLoggable(Level.FINER)) {
            for (Object key : parameters.keySet()) {
              logger.finer("Key " + key + " = " + parameters.getProperty((String) key));
            }
        }
        return parameters;
    }
}
