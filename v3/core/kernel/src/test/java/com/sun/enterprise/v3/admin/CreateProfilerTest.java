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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.Profiler;
import com.sun.enterprise.v3.common.PropsFileActionReporter;
import com.sun.logging.LogDomains;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.Properties;
import com.sun.enterprise.config.serverbeans.Property;

import org.glassfish.api.admin.AdminCommandContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;
import org.jvnet.hk2.component.Habitat;
import org.glassfish.api.ActionReport;
import org.glassfish.tests.utils.Utils;
import org.glassfish.tests.utils.ConfigApiTest;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Prashanth
 */
@Ignore
public class CreateProfilerTest extends ConfigApiTest {
    // Get Resources config bean
    Habitat habitat = Utils.instance.getHabitat(this);
    private JavaConfig javaConfig = habitat.getComponent(JavaConfig.class);
    private CreateProfiler command = null;
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
        assertTrue(javaConfig!=null);
        
        // Get an instance of the CreateProfiler command
        command = habitat.getComponent(CreateProfiler.class);
        assertTrue(command!=null);
        
        context = new AdminCommandContext(
                LogDomains.getLogger(CreateProfilerTest.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter(), parameters);
        
        cr = new CommandRunner();
    }

    @After
    public void tearDown() throws TransactionFailure {
       // Delete the created profiler
       ConfigSupport.apply(new SingleConfigCode<JavaConfig>() {
            public Object run(JavaConfig param) throws PropertyVetoException, TransactionFailure {
                if (param.getProfiler() != null){
                    param.setProfiler(null);
                }
                return null;
            }                        
        }, javaConfig);

        parameters.clear();
    }
    
    /**
     * Test of execute method, of class CreateProfiler.
     * asadmin create-profiler --nativelibrarypath "myNativeLibraryPath"
     *          --enabled=true --classpath "myProfilerClasspath" testProfiler
     */
    @Test
    public void testExecuteSuccess() {
        // Set the options and operand to pass to the command
        parameters.setProperty("classpath", "myProfilerClasspath");
        parameters.setProperty("enabled", "true");
        parameters.setProperty("nativelibrarypath", "myNativeLibraryPath");
        parameters.setProperty("property","a=x:b=y:c=z");
        parameters.setProperty("DEFAULT", "testProfiler");
        

        //Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("create-profiler", command, parameters, context.getActionReport());
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        //Check that the profiler is created
        boolean isCreated = false;
        int propertyCount = 0;
        Profiler profiler = javaConfig.getProfiler();
        if (profiler.getName().equals("testProfiler")) {
            assertEquals("myProfilerClasspath", profiler.getClasspath());
            assertEquals("true", profiler.getEnabled());
            assertEquals("myNativeLibraryPath", profiler.getNativeLibraryPath());
            List<Property> properties = profiler.getProperty();
            for (Property property:properties){
                if (property.getName().equals("a")) assertEquals("x",property.getValue());
                if (property.getName().equals("b")) assertEquals("y",property.getValue());
                if (property.getName().equals("c")) assertEquals("z",property.getValue());
                propertyCount++;
            }
            isCreated = true;
            logger.fine("Profiler element myProfiler is created.");
        }
        assertTrue(isCreated);
        assertEquals(propertyCount, 3);
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        // Check the success message
        //assertEquals("Command create-profiler executed successfully.", context.getActionReport().getMessage());
        logger.fine("msg: " + context.getActionReport().getMessage());               
    }
    
    /**
     * Test of execute method, of class CreateProfiler with default values.
     * asadmin create-profiler --nativelibrarypath "myNativeLibraryPath"
     *          --enabled=true --classpath "myProfilerClasspath" testProfiler
     */
    @Test
    public void testExecuteSuccessDefaultValues() {
        // Only pass the required option and operand
        assertTrue(parameters.isEmpty());
        parameters.setProperty("DEFAULT", "myProfilerAllDefaults");
        

        //Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("create-profiler", command, parameters, context.getActionReport());
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        //Check that the resource was created
        boolean isCreated = false;
        Profiler profiler = javaConfig.getProfiler();
        if (profiler.getName().equals("myProfilerAllDefaults")) {
            //assertEquals("myProfilerClasspath", profiler.getClasspath());
            assertEquals("true", profiler.getEnabled());
            //assertEquals("nativelibrarypath", profiler.getNativeLibraryPath());
            isCreated = true;
            logger.fine("Profiler element myProfilerAllDefaults is created.");
        }
        assertTrue(isCreated);
        
        // Check the success message
        //assertEquals("Command create-profiler executed successfully.", context.getActionReport().getMessage());
        logger.fine("msg: " + context.getActionReport().getMessage());    
    }

    /**
     * Test of execute method, creating a new when there is already one.
     * asadmin create-profiler --nativelibrarypath "myNativeLibraryPath"
     *          --enabled=true --classpath "myProfilerClasspath" testProfiler
     */
    @Test
    public void testExecuteSuccessUpdateExisting() {
        assertTrue(parameters.isEmpty());
        parameters.setProperty("DEFAULT", "testProfiler");
        

        //Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("create-profiler", command, parameters, context.getActionReport());
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        parameters.clear();
        
        //Create another profiler, see if it overrides the existing one
        parameters.setProperty("DEFAULT", "testProfilerNew");
        

        //Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("create-profiler", command, parameters, context.getActionReport());
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        //Check that the resource was created
        boolean isCreated = false;
        Profiler profiler = javaConfig.getProfiler();
        if (profiler.getName().equals("testProfilerNew")) {
            //assertEquals("myProfilerClasspath", profiler.getClasspath());
            assertEquals("true", profiler.getEnabled());
            //assertEquals("nativelibrarypath", profiler.getNativeLibraryPath());
            isCreated = true;
            logger.fine("Profiler element testProfilerNew is created.");
        }
        assertTrue(isCreated);
        
        // Check the success message
        //assertEquals("Command create-profiler executed successfully.", context.getActionReport().getMessage());
        logger.fine("msg: " + context.getActionReport().getMessage());    
    }

    /**
     * Test of execute method, of class CreateProfiler when enabled set to junk
     * asadmin create-profiler --nativelibrarypath "myNativeLibraryPath"
     *          --enabled=true --classpath "myProfilerClasspath" testProfiler
     */
    @Test
    public void testExecuteFailInvalidOptionEnabled() {
        // Set invalid enabled option value: --enabled junk
        //parameters.clear();
        assertTrue(parameters.isEmpty());
        parameters.setProperty("enabled", "junk");
        parameters.setProperty("DEFAULT", "myProfiler");
        
        // Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("create-profiler", command, parameters, context.getActionReport());
        
        // Check the exit code is Failure - test fails, need bug fix before uncommenting
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());

        // Check the error message - test fails
        assertEquals("Invalid parameter: enabled.  This boolean option must be set (case insensitive) to true or false.  Its value was set to junk", 
                        context.getActionReport().getMessage());
    }
    
    /**
     * Test of execute method, of class CreateProfiler when enabled has no value
     * asadmin create-profiler --nativelibrarypath "myNativeLibraryPath"
     *          --enabled=true --classpath "myProfilerClasspath" testProfiler
     */
    @Test
    public void testExecuteSuccessNoValueOptionEnabled() {
        // Set enabled without a value:  --enabled
        assertTrue(parameters.isEmpty());
        parameters.setProperty("enabled", "");
        parameters.setProperty("DEFAULT", "testProfiler");
        
        // Call CommandRunner.doCommand(..) to execute the command
        cr.doCommand("create-profiler", command, parameters, context.getActionReport());
        
        //Check that the profiler is created
        boolean isCreated = false;
        Profiler profiler = javaConfig.getProfiler();
        if (profiler.getName().equals("testProfiler")) {
            assertEquals("true", profiler.getEnabled());
            isCreated = true;
            logger.fine("msg: " + context.getActionReport().getMessage());    
        }
        assertTrue(isCreated);
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        // Check the success message
        //assertEquals("Command create-profiler executed successfully.", context.getActionReport().getMessage());
        logger.fine("msg: " + context.getActionReport().getMessage());               
    }
}
