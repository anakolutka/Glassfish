/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.jdbc.admin.cli;

import com.sun.enterprise.config.serverbeans.JdbcResource;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.v3.common.PropsFileActionReporter;
import com.sun.enterprise.universal.glassfish.SystemPropertyConstants;
import com.sun.logging.LogDomains;
import com.sun.enterprise.v3.admin.CommandRunner;

import java.util.Properties;

import org.glassfish.api.admin.AdminCommandContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import org.jvnet.hk2.component.Habitat;
import org.glassfish.api.ActionReport;
import org.glassfish.tests.utils.Utils;
import org.glassfish.tests.utils.ConfigApiTest;
import org.jvnet.hk2.config.DomDocument;

/**
 *
 * @author Jennifer
 */
@Ignore // temporarily disabled
public class DeleteJdbcResourceTest extends ConfigApiTest {
    Habitat habitat = Utils.instance.getHabitat(this);
    private Resources resources = habitat.getComponent(Resources.class);
    private DeleteJdbcResource deleteCommand = null;
    private Properties parameters = new Properties();
    private AdminCommandContext context = null;
    private CommandRunner cr = null;
    
    public DomDocument getDocument(Habitat habitat) {

        return new TestDocument(habitat);
    }
    
    /**
     * Returns the DomainTest file name without the .xml extension to load the test configuration
     * from.
     *
     * @return the configuration file name
     */
    public String getFileName() {
        return "DomainTest";
    }

    @Before
    public void setUp() {
        assertTrue(resources!=null);
        
        // Create a JDBC Resource jdbc/foo for each test
        CreateJdbcResource createCommand = habitat.getComponent(CreateJdbcResource.class);
        assertTrue(createCommand!=null);
        
        parameters.setProperty("connectionpoolid", "DerbyPool");
        parameters.setProperty("DEFAULT", "jdbc/foo");
        
        context = new AdminCommandContext(
                LogDomains.getLogger(DeleteJdbcResourceTest.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter(), parameters);
        
        cr = new CommandRunner();
        cr.doCommand("create-jdbc-resource", createCommand, parameters, context.getActionReport());
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        // Setup for delete-jdbc-resource
        parameters.clear();
        deleteCommand = habitat.getComponent(DeleteJdbcResource.class);
        assertTrue(deleteCommand!=null);
    }

    @After
    public void tearDown() {
        // Cleanup any leftover jdbc/foo resource - could be success or failure depending on the test
        parameters.clear();
        parameters.setProperty("DEFAULT", "jdbc/foo");
        cr.doCommand("delete-jdbc-resource", deleteCommand, parameters, context.getActionReport());
    }

    /**
     * Test of execute method, of class DeleteJdbcResource.
     * delete-jdbc-resource jdbc/foo
     */
    @Test
    public void testExecuteSuccessDefaultTarget() {
        // Set operand
        parameters.setProperty("DEFAULT", "jdbc/foo");
        
        //Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("delete-jdbc-resource", deleteCommand, parameters, context.getActionReport());
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        //Check that the resource was deleted
        boolean isDeleted = true;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof JdbcResource) {
                JdbcResource jr = (JdbcResource)resource;
                if (jr.getJndiName().equals("jdbc/foo")) {
                    isDeleted = false;
                    logger.fine("JdbcResource config bean jdbc/foo is deleted.");
                    continue;
                }
            }
        }       
        assertTrue(isDeleted);
        logger.fine("msg: " + context.getActionReport().getMessage());
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        //Check that the resource ref was deleted
        Servers servers = habitat.getComponent(Servers.class);
        boolean isRefDeleted = true;
        for (Server server : servers.getServer()) {
            if (server.getName().equals(SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)) {
                for (ResourceRef ref : server.getResourceRef()) {
                    if (ref.getRef().equals("jdbc/foo")) {
                        isRefDeleted = false;
                        continue;
                    }
                }
            }
        }
        assertTrue(isRefDeleted);
    }
    
    /**
     * Test of execute method, of class DeleteJdbcResource.
     * delete-jdbc-resource --target server jdbc/foo
     */
    @Test
    public void testExecuteSuccessTargetServer() {
        // Set operand
        parameters.setProperty("target", "server");
        parameters.setProperty("DEFAULT", "jdbc/foo");
        
        //Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("delete-jdbc-resource", deleteCommand, parameters, context.getActionReport());
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        //Check that the resource was deleted
        boolean isDeleted = true;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof JdbcResource) {
                JdbcResource jr = (JdbcResource)resource;
                if (jr.getJndiName().equals("jdbc/foo")) {
                    isDeleted = false;
                    logger.fine("JdbcResource config bean jdbc/foo is deleted.");
                    continue;
                }
            }
        }       
        assertTrue(isDeleted);
        logger.fine("msg: " + context.getActionReport().getMessage());
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        //Check that the resource ref was deleted
        Servers servers = habitat.getComponent(Servers.class);
        boolean isRefDeleted = true;
        for (Server server : servers.getServer()) {
            if (server.getName().equals(SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)) {
                for (ResourceRef ref : server.getResourceRef()) {
                    if (ref.getRef().equals("jdbc/foo")) {
                        isRefDeleted = false;
                        continue;
                    }
                }
            }
        }
        assertTrue(isRefDeleted);
    }

    /**
     * Test of execute method, of class DeleteJdbcResource.
     * delete-jdbc-resource doesnotexist
     */
    @Test
    public void testExecuteFailDoesNotExist() {
        // Set operand
        parameters.setProperty("DEFAULT", "doesnotexist");
        
        //Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("delete-jdbc-resource", deleteCommand, parameters, context.getActionReport());
        
        // Check the exit code is FAILURE
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
        
        // Check the error message
        assertEquals("A JDBC resource named doesnotexist does not exist.", context.getActionReport().getMessage());
        logger.fine("msg: " + context.getActionReport().getMessage());
    }
    
    /**
     * Test of execute method, of class DeleteJdbcResource.
     * delete-jdbc-resource
     */
    @Test
    public void testExecuteFailNoOperand() {
        //Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("delete-jdbc-resource", deleteCommand, parameters, context.getActionReport());
        
        // Check the exit code is FAILURE
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
        // Check the error message
        assertEquals("Operand required.", context.getActionReport().getMessage());
        logger.fine("msg: " + context.getActionReport().getMessage());
    }
    
    /**
     * Test of execute method, of class DeleteJdbcResource.
     * delete-jdbc-resource --invalid jdbc/foo
     */
    @Test
    public void testExecuteFailInvalidOption() {
        // Set operand
        parameters.setProperty("invalid", "");
        parameters.setProperty("DEFAULT", "jdbc/foo");
        
        //Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("delete-jdbc-resource", deleteCommand, parameters, context.getActionReport());
        
        // Check the exit code is FAILURE
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
        // Check the error message
        assertEquals(" Invalid option: invalid", context.getActionReport().getMessage());
        logger.fine("msg: " + context.getActionReport().getMessage());
    }

    /**
     * Test of execute method, of class DeleteJdbcResource.
     * delete-jdbc-resource --target invalid jdbc/foo
     */
    @Test
    public void testExecuteFailInvalidTarget() {
        // Set operand
        parameters.setProperty("target", "invalid");
        parameters.setProperty("DEFAULT", "jdbc/foo");
        
        //Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("delete-jdbc-resource", deleteCommand, parameters, context.getActionReport());
        
        //Check that the resource was NOT deleted
        boolean isDeleted = true;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof JdbcResource) {
                JdbcResource jr = (JdbcResource)resource;
                if (jr.getJndiName().equals("jdbc/foo")) {
                    isDeleted = false;
                    logger.fine("JdbcResource config bean jdbc/foo is deleted.");
                    continue;
                }
            }
        }
        // Need bug fix in DeleteJdbcResource before uncommenting assertion
        //assertFalse(isDeleted);
        
        // Check the exit code is FAILURE
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
        // Check the error message
        // Need bug fix in DeleteJdbcResource before uncommenting assertion
        //assertEquals(" Invalid target: invalid", context.getActionReport().getMessage());
        logger.fine("msg: " + context.getActionReport().getMessage());
        
        //Check that the resource ref was NOT deleted
        Servers servers = habitat.getComponent(Servers.class);
        boolean isRefDeleted = true;
        for (Server server : servers.getServer()) {
            if (server.getName().equals(SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)) {
                for (ResourceRef ref : server.getResourceRef()) {
                    if (ref.getRef().equals("jdbc/foo")) {
                        isRefDeleted = false;
                        continue;
                    }
                }
            }
        }
        assertFalse(isRefDeleted);
    }
}
