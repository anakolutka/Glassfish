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
package com.sun.enterprise.admin.cli;

import com.sun.enterprise.cli.framework.CLILogger;
import com.sun.enterprise.cli.framework.CommandException;
import com.sun.enterprise.cli.framework.CommandValidationException;
import com.sun.enterprise.universal.glassfish.SystemPropertyConstants;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import java.io.*;
import java.net.*;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;

/**
 * A local StopDomain command
 * @author bnevins
 */
public class StopDomainCommand extends S1ASCommand {

    @Override
    public void runCommand() throws CommandException, CommandValidationException {
        // WBN weird -- validateOptions is NOT called by the framework?!?
        validateOptions();
        getDomainRootDir();
        getDomainXml();
        domainRootDir = SmartFile.sanitize(domainRootDir);
        Integer[] ports = null;

        try {
            MiniXmlParser parser = new MiniXmlParser(domainXml);
            Set<Integer> portsSet = parser.getAdminPorts();
            ports = portsSet.toArray(new Integer[portsSet.size()]);
        }
        catch (MiniXmlParserException ex) {
            throw new CommandValidationException(
                    strings.get("StopDomain.parserError", ex), ex);
        }

        // TODO -- it would be nice to know if it worked!  
        // If so use other port numbers

        int adminPort = ports[0];

        // Verify that the DAS is running and reachable
        if(!RemoteCommand.pingDASQuietly(adminPort))
            throw new CommandValidationException(strings.get("StopDomain.dasNotRunning"));
        
        try {
            CLILogger.getInstance().pushAndLockLevel(Level.WARNING);
            new RemoteCommand(getCmd("" + adminPort));
            waitForDeath(adminPort);
        }
        finally {
            CLILogger.getInstance().popAndUnlockLevel();
        }
    }

    @Override
    public boolean validateOptions() throws CommandValidationException {
        super.validateOptions();
        // get domainName
        if (!operands.isEmpty()) {
            domainName = (String) operands.firstElement();
        }

        // get domainsDir
        String domaindir = getOption("domaindir");

        if (ok(domaindir)) {
            domainsDir = new File(domaindir);
            if (!domainsDir.isDirectory()) {
                throw new CommandValidationException(
                        strings.get("StopDomain.badDomainsDir", domainsDir));
            }
        }

        return true;
    }
    
    ///// Private Methods /////
    private String[] getCmd(String port) {
        List<String> cmd = new ArrayList<String>();
        cmd.add("stop-domain");
        cmd.add("--" + PORT);
        cmd.add(port);
        addOption(cmd, USER);
        addOption(cmd, PASSWORDFILE);
        return cmd.toArray(new String[cmd.size()]);
    }
    
    private void addOption(List<String> cmd, String option) {
        //adds the option to list as "--option" followed by its value iff value is non-null
        // "value" is higher precedence than "defaultValue"
        String value = getOption(option);

        if ( value != null) {
            cmd.add("--" + option); //get it as long option, a suitable method is not available :(
            cmd.add(value);
        }
    }

    private void getDomainRootDir() throws CommandValidationException {
        if (domainsDir == null) {
            domainsDir = new File(getSystemProperty(SystemPropertyConstants.DOMAINS_ROOT_PROPERTY));
        }

        if (!domainsDir.isDirectory()) {
            throw new CommandValidationException(
                    strings.get("StopDomain.badDomainsDir", domainsDir));
        }

        if (domainName != null) {
            domainRootDir = new File(domainsDir, domainName);
        }
        else {
            domainRootDir = getTheOneAndOnlyDomain(domainsDir);
        }

        if (!domainRootDir.isDirectory()) {
            throw new CommandValidationException(
                    strings.get("StopDomain.badDomainDir", domainRootDir));
        }
    }

    private void getDomainXml() throws CommandValidationException {
        // root-dir/config/domain.xml
        domainXml = new File(domainRootDir, "config/domain.xml");

        if (!domainXml.canRead()) {
            throw new CommandValidationException(
                    strings.get("StopDomain.noDomainXml", domainXml));
        }
    }

    /**
     * It either throws an Exception or returns a valid directory
     * @param parent
     * @return
     * @throws com.sun.enterprise.cli.framework.CommandValidationException
     */
    private File getTheOneAndOnlyDomain(File parent) throws CommandValidationException {
        // look for subdirs in the parent dir -- there must be one and only one

        File[] files = parent.listFiles(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory();
            }
        });

        if (files == null || files.length == 0) {
            throw new CommandValidationException(
                    strings.get("noDomainDirs", parent));
        }

        if (files.length > 1) {
            throw new CommandValidationException(
                    strings.get("StopDomain.tooManyDomainDirs", parent));
        }

        return files[0];
    }

    private void waitForDeath(int adminPort) throws CommandException {
        long startWait = System.currentTimeMillis();
        Log.info("StopDomain.WaitDASDeath", domainName);
        boolean alive = true;

        while(!timedOut(startWait)) {
            if(!pingPort(adminPort)) {
                alive = false;
                break;
            }
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException ex) {
                // don't care
            }
        }

        if(alive) {
            throw new CommandException(strings.get("StopDomain.DASNotDead", 
                    domainName, (WAIT_FOR_DAS_TIME_MS / 1000)));
        }
    }

    /** 
     * This is no substitute for RemoteCommand.pingDAS() -- that command guarantees
     * that DAS is at the other end of the port.  This is a quick check that can
     * be used after verifying DAS is in fact listening on the port.
     * I ran into a problem where stop-domain hangs on pingDAS() after it shuts down,
     * waiting on a network timeout.  This ping is fast!
     * @param port the port to ping
     * @return true if the server socket is reachable
     */
    private boolean pingPort(int port) {
        try {
            String host = null;
            Socket s = new Socket(host, port);
            return true;
        } catch (Exception ex) {
            Log.finer("pingPort got Exception: " + ex);
            return false;
        }
    }

    private boolean timedOut(long startTime) {
        return (System.currentTimeMillis() - startTime) > WAIT_FOR_DAS_TIME_MS;
    }

    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }
    private File domainsDir;
    private File domainRootDir;
    private String domainName;
    private File domainXml;
    private final static LocalStringsImpl strings = new LocalStringsImpl(StopDomainCommand.class);
    private final static long WAIT_FOR_DAS_TIME_MS = 60000;
}
