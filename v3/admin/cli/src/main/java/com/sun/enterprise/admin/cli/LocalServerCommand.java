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
package com.sun.enterprise.admin.cli;

import com.sun.enterprise.admin.cli.remote.RemoteCommand;
import com.sun.enterprise.security.store.PasswordAdapter;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import java.io.*;
import java.io.File;
import com.sun.enterprise.util.io.ServerDirs;
import java.net.*;
import java.security.KeyStore;
import java.util.*;
import java.util.Set;
import org.glassfish.api.admin.CommandException;

/**
 * A class that's supposed to capture all the behavior common to operation
 * on a "local" server.
 * It's getting fairly complicated thus the "section headers" comments.
 * This class plays two roles, <UL><LI>a place for putting common code - which
 * are final methods.  A parent class that is communicating with its own unknown
 * sub-classes.  These are non-final methods
 *
 * @author Byron Nevins
 */
public abstract class LocalServerCommand extends CLICommand {
    ////////////////////////////////////////////////////////////////
    /// Section:  protected methods that are OK to override
    ////////////////////////////////////////////////////////////////

    /**
     * Override this method and return false to turn-off the file validation.
     * E.g. it demands that config/domain.xml be present.  In special cases like
     * Synchronization -- this is how you turn off the testing.
     * @return true - do the checks, false - don't do the checks
     */
    protected boolean checkForSpecialFiles() {
        return true;
    }

    ////////////////////////////////////////////////////////////////
    /// Section:  protected methods that are notOK to override.
    ////////////////////////////////////////////////////////////////

    /**
     * Returns the admin port of the local domain. Note that this method should
     * be called only when you own the domain that is available on accessible
     * file system.
     *
     * @return an integer that represents admin port
     * @throws CommandException in case of parsing errors
     */
    protected final int getAdminPort()
            throws CommandException {
        // default:  DAS which always has the name "server"
        return getAdminPort("server");
    }

    /**
     * Returns the admin port of a particular server. Note that this method should
     * be called only when you own the server that is available on accessible
     * file system.
     *
     * @return an integer that represents admin port
     * @throws CommandException in case of parsing errors
     */
    protected final int getAdminPort(String serverName)
            throws CommandException {

        try {
            MiniXmlParser parser = new MiniXmlParser(getDomainXml(), serverName);
            Set<Integer> portsSet = parser.getAdminPorts();

            if(portsSet.size() > 0)
                return portsSet.iterator().next();
            else
                throw new CommandException("admin port not found");
        }
        catch (MiniXmlParserException ex) {
            throw new CommandException("admin port not found", ex);
        }
    }

    protected final void setServerDirs(ServerDirs sd) {
        serverDirs = sd;
    }

    protected final void resetServerDirs() throws IOException {
        serverDirs = serverDirs.refresh();
    }

    protected final ServerDirs getServerDirs() {
        return serverDirs;
    }

    protected final File getDomainXml() {
        return serverDirs.getDomainXml();
    }

    /**
     * Checks if the create-domain was created using --savemasterpassword flag
     * which obtains security by obfuscation! Returns null in case of failure
     * of any kind.
     * @return String representing the password from the JCEKS store named
     *          master-password in domain folder
     */
    protected final String readFromMasterPasswordFile() {
        File mpf = getMasterPasswordFile();
        if(mpf == null)
            return null;   // no master password  saved
        try {
            PasswordAdapter pw = new PasswordAdapter(mpf.getAbsolutePath(),
                    "master-password".toCharArray()); // fixed key
            return pw.getPasswordForAlias("master-password");
        }
        catch (Exception e) {
            logger.printDebugMessage("master password file reading error: "
                    + e.getMessage());
            return null;
        }
    }

    protected final boolean verifyMasterPassword(String mpv) {
        // only tries to open the keystore
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(getJKS());
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(fis, mpv.toCharArray());
            return true;
        }
        catch (Exception e) {
            logger.printDebugMessage(e.getMessage());
            return false;
        }
        finally {
            try {
                if(fis != null)
                    fis.close();
            }
            catch (IOException ioe) {
                // ignore, I know ...
            }
        }
    }

    /**
     * Get the master password, either from a password file or
     * by asking the user.
     */
    protected final String getMasterPassword() throws CommandException {
        // Sets the password into the launcher info.
        // Yes, returning master password as a string is not right ...
        final int RETRIES = 3;
        long t0 = System.currentTimeMillis();
        String mpv = passwords.get(CLIConstants.MASTER_PASSWORD);
        if(mpv == null) { //not specified in the password file
            mpv = "changeit";  //optimization for the default case -- see 9592
            if(!verifyMasterPassword(mpv)) {
                mpv = readFromMasterPasswordFile();
                if(!verifyMasterPassword(mpv)) {
                    mpv = retry(RETRIES);
                }
            }
        }
        else { // the passwordfile contains AS_ADMIN_MASTERPASSWORD, use it
            if(!verifyMasterPassword(mpv))
                mpv = retry(RETRIES);
        }
        long t1 = System.currentTimeMillis();
        logger.printDebugMessage("Time spent in master password extraction: "
                + (t1 - t0) + " msec");       //TODO
        return mpv;
    }

    /**
     * See if the server is alive and is the one at the specified directory.
     *
     * @return true if it's the DAS at this domain directory
     */
    protected final boolean isThisServer(File ourDir, String directoryKey) {
        if(!ok(directoryKey))
            throw new NullPointerException();

        ourDir = getUniquePath(ourDir);
        logger.printDebugMessage("Check if server is at location " + ourDir);

        try {
            RemoteCommand cmd =
                    new RemoteCommand("__locations", programOpts, env);
            Map<String, String> attrs =
                    cmd.executeAndReturnAttributes(new String[]{"__locations"});
            String theirDirPath = attrs.get(directoryKey);
            logger.printDebugMessage("Remote server has root directory " + theirDirPath);

            if(ok(theirDirPath)) {
                File theirDir = getUniquePath(new File(theirDirPath));
                return theirDir.equals(ourDir);
            }
            return false;
        }
        catch (Exception ex) {
            return false;
        }
    }
    /**
     * There is sometimes a need for subclasses to know if a
     * <code> local domain </code> is running. An example of such a command is
     * change-master-password command. The stop-domain command also needs to
     * know if a domain is running <i> without </i> having to provide user
     * name and password on command line (this is the case when I own a domain
     * that has non-default admin user and password) and want to stop it
     * without providing it.
     * <p>
     * In such cases, we need to know if the domain is running and this method
     * provides a way to do that.
     *
     * @return boolean indicating whether the server is running
     */
    protected final boolean isRunning(String host, int port) {
        Socket server = null;
        try {
            server = new Socket(host, port);
            return true;
        }
        catch (Exception ex) {
            logger.printDebugMessage("\nisRunning got exception: " + ex);
            return false;
        }
        finally {
            if(server != null) {
                try {
                    server.close();
                }
                catch (IOException ex) {
                }
            }
        }
    }

    /**
     * convenience method for the local machine
     */
    protected final boolean isRunning(int port) {
        return isRunning(null, port);
    }
    /**
     * Is the server still running?
     * This is only called when we're hanging around waiting for the server to die.
     */
    protected final  boolean isRunning() {
        File pf = getServerDirs().getPidFile();

        if(pf != null) {
            return pf.exists();
        }
        else
            return isRunning(programOpts.getHost(), // remote case
                    programOpts.getPort());
    }

    ////////////////////////////////////////////////////////////////
    /// Section:  private methods
    ////////////////////////////////////////////////////////////////
    private final File getJKS() {
        if(serverDirs == null)
            return null;

        File mp = new File(new File(serverDirs.getServerDir(), "config"), "keystore.jks");
        if(!mp.canRead())
            return null;
        return mp;
    }

    private File getMasterPasswordFile() {

        if(serverDirs == null)
            return null;

        File mp = new File(serverDirs.getServerDir(), "master-password");
        if(!mp.canRead())
            return null;

        return mp;
    }

    private String retry(int times) throws CommandException {
        String mpv;
        // prompt times times
        for(int i = 0; i < times; i++) {
            // XXX - I18N
            String prompt = strings.get("mp.prompt", (times - i));
            mpv = super.readPassword(prompt);
            if(mpv == null)
                throw new CommandException(strings.get("no.console"));
            // ignore retries :)
            if(verifyMasterPassword(mpv))
                return mpv;
            if(i < (times - 1))
                logger.printMessage(strings.get("retry.mp"));
            // make them pay for typos?
            //Thread.currentThread().sleep((i+1)*10000);
        }
        throw new CommandException(strings.get("mp.giveup", times));
    }

    private File getUniquePath(File f) {
        try {
            f = f.getCanonicalFile();
        }
        catch (IOException ioex) {
            f = SmartFile.sanitize(f);
        }
        return f;
    }

    ////////////////////////////////////////////////////////////////
    /// Section:  private variables
    ////////////////////////////////////////////////////////////////

    private ServerDirs serverDirs;
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(LocalDomainCommand.class);
}


