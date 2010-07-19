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
package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.io.InstanceDirs;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import org.glassfish.api.Param;
import org.glassfish.api.admin.*;

import com.sun.enterprise.admin.cli.*;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.universal.io.SmartFile;

/**
 * A base class for local commands that manage a local server instance.
 * This base class is used by a LOT of other classes.  There is a big comment before
 * each section of the file -- sorted by visibility.
 * If you specifically want a method to be overridden -- make it protected but
 * not final.
 * final protected means -- "call me but don't override me".  This convention is
 * to make things less confusing.
 * If you add a method or change whether it is final -- move it to the right section.
 *
 * Default instance file structure.
 * ||---- <GlassFish Install Root>
 *          ||---- nodeagents (nodeDirRoot, --nodedir)
 *                  ||---- <node-1> (nodeDirChild, --node)
 *                          || ---- agent
 *                                  || ---- config
 *                                          | ---- das.properties
 *                          || ---- <instance-1> (instanceDir)
 *                                  ||---- config
 *                                  ||---- applications
 *                                  ||---- java-web-start
 *                                  ||---- generated
 *                                  ||---- lib
 *                                  ||---- docroot
 *                          || ---- <instance-2> (instanceDir)
 *                  ||---- <node-2> (nodeDirChild)
 *
 * @author Byron Nevins
 */
// -----------------------------------------------------------------------
// ----------------   public methods here   --------------- --------------
// -----------------------------------------------------------------------

public abstract class LocalInstanceCommand extends LocalServerCommand {
    @Param(name = "nodedir", optional = true, alias="agentdir")
    protected String nodeDir;           // nodeDirRoot
    @Param(name = "node", optional=true, alias="nodeagent")
    protected String node;
    // subclasses decide whether it's optional, required, or not allowed
    //@Param(name = "instance_name", primary = true, optional = true)
    protected String instanceName;
    protected File nodeDirRoot;         // the parent dir of all node(s)
    protected File nodeDirChild;        // the specific node dir
    protected File instanceDir;         // the specific instance dir
    private InstanceDirs instanceDirs;
    @Override
    protected void validate()
            throws CommandException, CommandValidationException {
        initInstance();
    }
    
// -----------------------------------------------------------------------
// -------- protected methods where overriding is allowed here -----------
// -----------------------------------------------------------------------

    /** 
     * override this method if your class does NOT want to create directories
     * @param f the directory to create
     */
    protected boolean mkdirs(File f) {
        return f.mkdirs();
    }
    
    protected void initInstance() throws CommandException {
        /* node dir - parent directory of all node(s) */
        String nodeDirRootPath = null;  
        if(ok(nodeDir))
            nodeDirRootPath = nodeDir;
        else // node dir = <install-root>/nodeagents
            nodeDirRootPath = getNodeDirRootDefault();

        nodeDirRoot = new File(nodeDirRootPath);
        mkdirs(nodeDirRoot);

        if(!nodeDirRoot.isDirectory()) {
            throw new CommandException(
                    Strings.get("Instance.badNodeDir", nodeDirRoot));
        }

        /* <node_dir>/<node> */
        if(node != null) {
            nodeDirChild = new File(nodeDirRoot, node);
        }
        else {
            nodeDirChild = getTheOneAndOnlyNode(nodeDirRoot);
        }

        /* <node_dir>/<node>/<instance name> */
        if(instanceName != null) {
            instanceDir = new File(nodeDirChild, instanceName);
            mkdirs(instanceDir);
        }
        else {
            instanceDir = getTheOneAndOnlyInstance(nodeDirChild);
            instanceName = instanceDir.getName();
        }

        if(!instanceDir.isDirectory()) {
            throw new CommandException(
                    Strings.get("Instance.badInstanceDir", instanceDir));
        }
        nodeDirChild = SmartFile.sanitize(nodeDirChild);
        instanceDir = SmartFile.sanitize(instanceDir);

        try {
            instanceDirs = new InstanceDirs(instanceDir);
            setServerDirs(instanceDirs.getServerDirs());
            //setServerDirs(instanceDirs.getServerDirs(), checkForSpecialFiles());
        }
        catch (IOException e) {
            throw new CommandException(e);
        }

        logger.printDebugMessage("nodeDirChild: " + nodeDirChild);
        logger.printDebugMessage("instanceDir: " + instanceDir);
    }

    protected final InstanceDirs getInstanceDirs() {
        return instanceDirs;
    }

// -----------------------------------------------------------------------
// -------- protected methods where overriding is NOT allowed here -----------
// -----------------------------------------------------------------------

    /**
     * Set the programOpts based on the das.properties file.
     */
    protected final void setDasDefaults(File propfile) throws CommandException {
        Properties dasprops = new Properties();
        FileInputStream fis = null;
        try {
            // read properties and set them in programOpts
            // properties are:
            // agent.das.port
            // agent.das.host
            // agent.das.isSecure
            // agent.das.user           XXX - not in v2?
            fis = new FileInputStream(propfile);
            dasprops.load(fis);
            fis.close();
            fis = null;
            String p;
            p = dasprops.getProperty("agent.das.host");
            if (p != null)
                programOpts.setHost(p);
            p = dasprops.getProperty("agent.das.port");
            int port = -1;
            if (p != null)
                port = Integer.parseInt(p);
            p = dasprops.getProperty("agent.das.protocol");
            if (p != null && p.equals("rmi_jrmp")) {
                programOpts.setPort(updateDasPort(dasprops, port, propfile));
            } else if (p == null || p.equals("http"))
                programOpts.setPort(port);
            else
                throw new CommandException(Strings.get("Instance.badProtocol",
                                                    propfile.toString(), p));
            p = dasprops.getProperty("agent.das.isSecure");
            if (p != null)
                programOpts.setSecure(Boolean.parseBoolean(p));
            p = dasprops.getProperty("agent.das.user");
            if (p != null)
                programOpts.setUser(p);
            // XXX - what about the DAS admin password?
        } catch (IOException ioex) {
            throw new CommandException(
                        Strings.get("Instance.cantReadDasProperties",
                                    propfile.getPath()));
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException cex) {
                    // ignore it
                }
            }
        }
    }

    final protected void whackFilesystem() throws CommandException {
        File whackee = getServerDirs().getServerDir();

        if (whackee == null || !whackee.isDirectory()) {
            throw new CommandException(Strings.get("DeleteInstance.noWhack",
                    whackee));
        }

        FileUtils.whack(whackee);

        if (whackee.isDirectory())
            throw new CommandException(Strings.get("DeleteInstance.badWhack",
                    whackee));
    }

    /**
     * Gets the GlassFish installation root (using property com.sun.aas.installRoot),
     * first from asenv.conf.  If that's not available, then from java.lang.System.
     *
     * @return path of GlassFish install root
     * @throws CommandException if the GlassFish install root is not found
     */
    protected String getInstallRootPath() throws CommandException {
        String installRootPath = getSystemProperty(
                SystemPropertyConstants.INSTALL_ROOT_PROPERTY);

        if(!StringUtils.ok(installRootPath))
            installRootPath = System.getProperty(
                    SystemPropertyConstants.INSTALL_ROOT_PROPERTY);

        if(!StringUtils.ok(installRootPath))
            throw new CommandException("noInstallDirPath");
        return installRootPath;
    }

// -----------------------------------------------------------------------
// -------- everything below here is private    --------------------------
// -----------------------------------------------------------------------

    /**
     * Update DAS port from an old V2 das.properties file.
     * If the old port is the standard jrmp port, just use the new
     * standard http port.  Otherwise, prompt for the new port number
     * if possible.  In any event, try to rewrite the das.properties
     * file with the new values.
     */
    private int updateDasPort(Properties dasprops, int port, File propfile) {
        Console cons;
        if (port == 8686) {     // the old JRMP port
            logger.printMessage(
                Strings.get("Instance.oldDasProperties",
                    propfile.toString(), Integer.toString(port),
                    Integer.toString(programOpts.getPort())));
            port = programOpts.getPort();
        } else if ((cons = System.console()) != null) {
            String line = cons.readLine("%s",
                Strings.get("Instance.oldDasPropertiesPrompt",
                    propfile.toString(), Integer.toString(port),
                    Integer.toString(programOpts.getPort())));
            while (line != null && line.length() > 0) {
                try {
                    port = Integer.parseInt(line);
                    if (port > 0 && port <= 65535)
                        break;
                } catch (NumberFormatException nfex) {
                }
                line = cons.readLine(Strings.get("Instance.reenterPort"),
                    Integer.toString(programOpts.getPort()));
            }
        } else {
            logger.printMessage(
                Strings.get("Instance.oldDasPropertiesWrong",
                    propfile.toString(), Integer.toString(port),
                    Integer.toString(programOpts.getPort())));
            port = programOpts.getPort();
        }
        dasprops.setProperty("agent.das.protocol", "http");
        dasprops.setProperty("agent.das.port", Integer.toString(port));
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(propfile));
            dasprops.store(bos,
                "Domain Administration Server Connection Properties");
            bos.close();
            bos = null;
        } catch (IOException ex2) {
            logger.printMessage(
                Strings.get("Instance.dasPropertiesUpdateFailed"));
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException cex) {
                    // ignore it
                }
            }
        }
        // whether we were able to update the file or not, keep going
        logger.printDebugMessage("New DAS port number: " + port);
        return port;
    }

    private File getTheOneAndOnlyNode(File parent) throws CommandException {
        // look for subdirs in the parent dir -- there must be one and only one
        // or there can be zero in which case we create one-and-only

        File[] files = parent.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });

        // ERROR:  more than one node dir child
        if(files.length > 1) {
            throw new CommandException(
                    Strings.get("tooManyNodes", parent));
        }

        // the usual case -- one node dir child
        if(files.length == 1)
            return files[0];

        /*
         * If there is no existing node dir child -- create one!
         * If the instance is on the same machine as DAS, use "localhost" as the node dir child
         */
        try {
            String dashost = null;
            if (programOpts != null) {
                dashost = programOpts.getHost();
            }
            String hostname = InetAddress.getLocalHost().getHostName();
            if (hostname.equals(dashost) || "localhost".equals(dashost)) {
                hostname = "localhost";
            }
            File f = new File(parent, hostname);

            if(!f.mkdirs() || !f.isDirectory()) // for instance there is a regular file with that name
                throw new CommandException(Strings.get("cantCreateNodeDirChild", f));

            return f;
        }
        catch (UnknownHostException ex) {
            throw new CommandException(Strings.get("cantGetHostName", ex));
        }
    }

    private File getTheOneAndOnlyInstance(File parent) throws CommandException {
        // look for subdirs in the parent dir -- there must be one and only one

        File[] files = parent.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });

        if(files == null || files.length == 0) {
            throw new CommandException(
                    Strings.get("Instance.noInstanceDirs", parent));
        }

        // expect two - the "agent" directory and the instance directory
        if(files.length > 2) {
            throw new CommandException(
                    Strings.get("Instance.tooManyInstanceDirs", parent));
        }

        for(File f : files) {
            if(!f.getName().equals("agent"))
                return f;
        }
        throw new CommandException(
                Strings.get("Instance.noInstanceDirs", parent));
    }

    /**
     * Return the default value for nodeDirRoot, first checking if com.sun.aas.agentRoot
     * was specified in asenv.conf and returning this value. If not specified,
     * then the defaut value is the {GlassFish_Install_Root}/nodeagents.
     * nodeDirRoot is the parent directory of the node(s).
     *
     * @return String default nodeDirRoot - parent directory of node(s)
     * @throws CommandException if the GlassFish install root is not found
     */
    private String getNodeDirRootDefault() throws CommandException {
        String nodeDirDefault = getSystemProperty(
                SystemPropertyConstants.AGENT_ROOT_PROPERTY);

        if(StringUtils.ok(nodeDirDefault))
            return nodeDirDefault;

        String installRootPath = getInstallRootPath();
        return installRootPath + "/" + "nodeagents";
    }

}
