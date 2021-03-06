/*
 
Derby - Class org.apache.derbyTesting.functionTests.tests.replicationTests.ShutdownSlave
 
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at
 
   http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 
 */

package org.apache.derbyTesting.functionTests.tests.replicationTests;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.derbyTesting.junit.BaseJDBCTestCase;
import org.apache.derbyTesting.junit.TestConfiguration;

/**
 * Test (master) behaviour after killing the slave server.
 */
public class ShutdownSlaveServerByOsKill extends ShutdownSlave
{
    
    public ShutdownSlaveServerByOsKill(String testcaseName)
    {
        super(testcaseName);
    }
        
    
    public static Test suite()
    throws Exception
    {
        System.out.println("**** ShutdownSlaveServerByOsKill.suite()");
        System.out.println("'ShutdownSlaveServerByOsKill' can not be run outside the 'ReplicationRun' framework.");
        
        setEnv();
        
        TestSuite suite = new TestSuite("ShutdownSlaveServerByOsKill");
        suite.addTest(ShutdownSlaveServerByOsKill.suite(masterServerHost, masterServerPort));
        return (Test)suite;
    }
    /**
     * Adds this class to the *existing server* suite.
     */
    public static Test suite(String serverHost, int serverPort)
    throws IOException
    {
        System.out.println("*** ShutdownSlaveServerByOsKill.suite("+serverHost+","+serverPort+")");
        
        Test t = TestConfiguration.existingServerSuite(ShutdownSlaveServerByOsKill.class,false,serverHost,serverPort);
        System.out.println("*** Done TestConfiguration.existingServerSuite(ShutdownSlaveServerByOsKill.class,false,"
                +serverHost+":"+serverPort+")");
        return t;
    }
    
    /**
     * Test shut down slave server during replication.
     *
     * @throws SQLException, IOException, InterruptedException
     */
    public void testShutdownSlave()
    throws SQLException, IOException, InterruptedException
    {
        shutdown(null, false, true); // -,-,true: Use OS kill on server!
        
        // Check master state:...
    }

}
