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

package com.sun.enterprise.admin.cli.optional;

import java.io.*;

import org.glassfish.api.admin.*;
import org.glassfish.api.Param;
import com.sun.enterprise.admin.cli.*;
import com.sun.enterprise.util.ObjectAnalyzer;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.backup.BackupRequest;

import com.sun.enterprise.util.io.DomainDirs;

/**
 * This is a local command for backing-up domains.
 * The Options:
 *  <ul>
 *  <li>domaindir
 *  </ul>
 * The Operand:
 *  <ul>
 *  <li>domain_name
 *  </ul>
 */

public abstract class BackupCommands extends LocalDomainCommand {

    BackupRequest   request;

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(BackupCommands.class);

    @Param(name = "verbose", optional = true)
    boolean verbose;
 
    @Param(name = "domain_name", primary = true, optional = true)
    String domainName;

    private String desc = null;

     /**
     * A method that checks the options and operand that the user supplied.
     * These tests are slightly different for different CLI commands
     */
    protected void checkOptions() throws CommandException {
        if (verbose && programOpts.isTerse())
            throw new CommandValidationException(
                strings.get("NoVerboseAndTerseAtTheSameTime"));

        if (domainDirParam == null || domainDirParam.length() <= 0) {
            
            try {
            
                domainDirParam = DomainDirs.getDefaultDomainsDir().getPath();
            } catch (IOException ioe) {
                throw new CommandException(ioe.getMessage());
            }
        }

        File domainsDirFile = new File(domainDirParam);

        // make sure domainsDir exists and is a directory
        if (!domainsDirFile.isDirectory()) {
            throw new CommandValidationException(
                strings.get("InvalidDomainPath", domainDirParam));
        }

        // if user hasn't specified domain_name, get the default one
        
        if (domainName == null)
            domainName = getDomainName();

    }

    protected void setDescription(String d) {
        desc = d;
    }
    
    protected void prepareRequest() {

        request = new BackupRequest(domainDirParam, domainName, desc);

        request.setTerse(programOpts.isTerse());
        request.setVerbose(verbose);
    }
 
    /*
     * Method to check if the file is writable directory
     */
    protected boolean isWritableDirectory(File domainFile) {
        boolean result = false;
        if (domainFile.isDirectory() || domainFile.canWrite()) {
            result = true;
        }
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + "\n" + ObjectAnalyzer.toString(this);
    }

}
