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

import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import com.sun.enterprise.admin.cli.*;
import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.DomainsManager;
import com.sun.enterprise.admin.servermgmt.pe.PEDomainsManager;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.appserv.management.client.prefs.LoginInfoStoreFactory;
import com.sun.appserv.management.client.prefs.LoginInfoStore;
import com.sun.appserv.management.client.prefs.StoreException;
import java.io.File;
import java.io.IOException;

/**
 *  This is a local command that deletes a domain.
 */
@Service(name = "delete-domain")
@Scoped(PerLookup.class)
public final class DeleteDomainCommand extends LocalDomainCommand {

    @Param(name = "domain_name", primary = true)
    private String domainName0;

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(DeleteDomainCommand.class);

    // this is single threaded code, deliberately avoiding volatile/atomic
    private int adminPort;


    /**
     */
    @Override
    protected void validate()
            throws CommandException, CommandValidationException  {
        setDomainName(domainName0);
        super.validate();
        adminPort = super.getAdminPort();
    }
 
    /**
     */
    @Override
    protected int executeCommand()
            throws CommandException, CommandValidationException {

        try {            
            DomainConfig domainConfig =
                new DomainConfig(getDomainName(), getDomainsDir().getPath());
            checkRunning();
            checkRename();
            DomainsManager manager = new PEDomainsManager();
            manager.deleteDomain(domainConfig);
            // By default, do as what v2 does -- don't delete the entry -
            // might need a revisit (Kedar: 09/16/2009)
            //deleteLoginInfo();
        } catch (Exception e) {
	    throw new CommandException(e.getLocalizedMessage());
        }

	logger.printDetailMessage(strings.get("DomainDeleted", getDomainName()));
        return 0;
    }

    private void checkRunning() throws CommandException {
        if (super.isRunning(adminPort)) {
            String msg = strings.get("domain.is.running", getDomainName(),
                                        getDomainRootDir());
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Check that the domain directory can be renamed, to increase the likelyhood
     * that it can be deleted.
     */
    private void checkRename() throws CommandException {
        boolean ok = true;
        try {
            File root = getDomainsDir();
            File domdir = new File(root, getDomainName());
            File tmpdir = File.createTempFile("del-", "", root);

            ok = tmpdir.delete() && domdir.renameTo(tmpdir) &&
                    tmpdir.renameTo(domdir);
        } catch (IOException ioe) {
            ok = false;
        }
       if (!ok) {
           String msg = strings.get("domain.fileinuse", getDomainName(),
                   getDomainRootDir());
           throw new IllegalStateException(msg);
       }
    }

    /**
     * This method will delete the entry in the .asadminpass file if exists
     */
    private void deleteLoginInfo() throws CommandException, StoreException {
        LoginInfoStore store = LoginInfoStoreFactory.getDefaultStore();
        // the host is always "localhost" in this case
        store.remove("localhost", adminPort);
    }
}
