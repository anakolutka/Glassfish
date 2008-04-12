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

import com.sun.enterprise.universal.collections.CollectionUtils;
import com.sun.enterprise.universal.collections.ManifestUtils;
import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.sun.enterprise.cli.framework.*;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * A local ListCommands Command
 * Hacked in for TP2
 * 
 * @author bnevins
 */
public class ListCommandsCommand extends S1ASCommand {
    @Override
    public void runCommand() throws CommandException, CommandValidationException {
        // WBN weird -- validateOptions is NOT called by the framework?!?
        validateOptions();
        getRemoteCommands();
        getLocalCommands();
        
        if(!remoteOnly)
            printLocalCommands();
        
        if(!localOnly)
            printRemoteCommands();
    }

    @Override
    public boolean validateOptions() throws CommandValidationException {
        super.validateOptions();
        
        port = getOption("port");
        host = getOption("host");
        localOnly = getBooleanOption("localonly");
        remoteOnly = getBooleanOption("remoteonly");
        
        if(localOnly && remoteOnly) {
            throw new CommandValidationException(strings.get("listCommands.notBoth"));
        }
        return true;
    }

    private void getLocalCommands() throws CommandValidationException {
        CLIDescriptorsReader r = CLIDescriptorsReader.getInstance();
        Iterator<ValidCommand> it = r.getCommandsList().getCommands();
        List<String> names = new ArrayList<String>();
        
        while(it.hasNext()) {
            names.add(it.next().getName());
        }
        localCommands = names.toArray(new String[names.size()]);
    }

    private void getRemoteCommands() throws CommandException {
        try {
            CLILogger.getInstance().pushAndLockLevel(Level.WARNING);
            RemoteCommand rc = new RemoteCommand(getRemoteArgs());
            // throw away everything but the main atts
            Map<String,String> mainAtts = rc.getServerResponse().get(ManifestUtils.MAIN_ATTS);
            String cmds = mainAtts.get("children");
            remoteCommands = cmds.split(";");
        }
        catch(CommandException e) {
            throw e;
        }
        catch(Exception e) {
            throw new CommandException(strings.get("listCommands.errorRemote", e.getMessage()));
        }
        finally {
            CLILogger.getInstance().popAndUnlockLevel();
        }
    }

    private String[] getRemoteArgs() {
        List<String> list = new ArrayList<String>(5);
        list.add("list-commands");
        
        if(ok(port)) {
            list.add("--port");
            list.add(port);
        }
        if(ok(host)) {
            list.add("--host");
            list.add(host);
        }
        return list.toArray(new String[list.size()]);
    }
    
    private static boolean ok(String s) {
        return s != null && s.length() > 0 && !s.equals("null");
    }

    private void printLocalCommands() {
        logger.printMessage("********** Local Commands **********");
        
        for(String s : localCommands) {
            logger.printMessage(s);
        }
    }

    private void printRemoteCommands() {
        logger.printMessage("********** Remote Commands **********");
        
        for(String s : remoteCommands) {
            logger.printMessage(s);
        }
    }
    
    String[] remoteCommands;
    String[] localCommands;
    String port;
    String host;
    boolean localOnly;
    boolean remoteOnly;
    private final static LocalStringsImpl strings = new LocalStringsImpl(ListCommandsCommand.class);
    private final static CLILogger logger = CLILogger.getInstance();

}
