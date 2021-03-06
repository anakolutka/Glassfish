/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests;

import java.lang.reflect.Method;
import java.util.*;

import junit.framework.Test;

import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.testing.framework.*;

/**
 * This class create test runs, i.e. models of model to allow all tests to be run a once.
 */
public class TestRunModel extends TestModel {
    protected DatabaseLogin login;
    protected DatabaseLogin oldLogin;
    protected boolean usesNativeMode = false;
    protected boolean isLight = true;
    protected boolean isAll = false;
    protected Vector testList;

    public TestRunModel() {
        // Setup as LRG by default.
        setName("LRGTestModel");
        setDescription("This model runs all of the LRG tests.");
    }

    /**
     * You must add new tests to this method.
     * If the new tests should be part of SRG as well then contact QA to update the SRG model.
     */
    public void addTests() {
        if (!getTests().isEmpty()) {
            return;
        }
        Vector tests = new Vector();

        if (isLight) {        
            tests.addElement("org.eclipse.persistence.testing.tests.workbenchintegration.MappingWMIntegrationStoredProcedureTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.workbenchintegration.MappingWorkbenchIntegrationTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.mapping.MappingTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.directmap.DirectMapMappingModel");
            tests.addElement("org.eclipse.persistence.testing.tests.feature.FeatureTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.feature.FeatureTestModelWithoutBinding");
            tests.addElement("org.eclipse.persistence.testing.tests.feature.TopLinkBatchUpdatesTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.feature.JDBCBatchUpdatesTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.feature.ParameterizedBatchUpdatesTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.feature.NativeBatchWritingTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.feature.EmployeeJoinFetchTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.types.TypeTestModelWithAccessors");
            tests.addElement("org.eclipse.persistence.testing.tests.types.TypeTestModelWithOutAccessors");
            tests.addElement("org.eclipse.persistence.testing.tests.conversion.ConversionManagerTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.conversion.ConversionManagerTestModelWithoutBinding");
            tests.addElement("org.eclipse.persistence.testing.tests.employee.EmployeeBasicTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.unidirectional.UnidirectionalEmployeeBasicTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.onetoonejointable.OneToOneJoinTableEmployeeBasicTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.orderedlist.OrderListTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.insurance.InsuranceBasicTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.insurance.InsuranceObjectRelationalTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.legacy.LegacyTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.aggregate.AggregateTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.inheritance.InheritanceTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.ownership.OwnershipTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.mapping.OuterJoinWithMultipleTablesTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.interfaces.InterfaceWithTablesTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.interfaces.InterfaceWithoutTablesTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.optimisticlocking.OptimisticLockingTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.relationshipmaintenance.RelationshipsTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.jpql.JPQLTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.simultaneous.SimultaneousTestsModel");
            tests.addElement("org.eclipse.persistence.testing.tests.writing.ComplexUpdateAndUnitOfWorkTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.unitofwork.UnitOfWorkClientSessionTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.unitofwork.UnitOfWorkIsolatedClientSessionTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.unitofwork.UnitOfWorkIsolatedAlwaysTestModel");
            tests.add("org.eclipse.persistence.testing.tests.unitofwork.UnitOfWorkSynchNewObjectsClientSessionTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.unitofwork.transactionisolation.UnitOfWorkTransactionIsolationTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.transparentindirection.TransparentIndirectionModel");
            tests.addElement("org.eclipse.persistence.testing.tests.collections.CollectionsTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.collections.map.MapCollectionsTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.customsqlstoredprocedures.CustomSQLTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.validation.ValidationModel");
            tests.addElement("org.eclipse.persistence.testing.tests.readonly.ReadOnlyTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.forceupdate.FUVLTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.sessionsxml.SessionsXMLBasicTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.sessionsxml.SessionsXMLTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.unitofwork.changeflag.EmployeeChangeTrackingTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.jms.JMSTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.helper.HelperTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.schemaframework.AutoTableGeneratorBasicTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.schemaframework.StoredProcedureGeneratorModel");
            tests.addElement("org.eclipse.persistence.testing.tests.proxyindirection.ProxyIndirectionTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.localization.LocalizationTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.security.SecurityTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.history.HistoryTestRunModel");
            tests.addElement("org.eclipse.persistence.testing.tests.isolatedsession.IsolatedSessionTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.unitofwork.writechanges.UnitOfWorkWriteChangesTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.identitymaps.cacheinvalidation.CacheExpiryModel");
            tests.addElement("org.eclipse.persistence.testing.tests.identitymaps.cacheinvalidation.EmployeeTimeToLiveTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.identitymaps.cacheinvalidation.EmployeeDailyExpiryTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.nondeferredwrites.NonDeferredWritesTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.weaving.SimpleWeavingTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.multipletable.MultipleTableModel");
            tests.addElement("org.eclipse.persistence.testing.tests.distributedcache.DistributedCacheModel");
            tests.addElement("org.eclipse.persistence.testing.tests.tableswithspacesmodel.EmployeeWithSpacesTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.optimization.queryandsqlcounting.QueryAndSQLCountingTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.identitymaps.cache.CacheIdentityMapTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.failover.FailoverTestModel");
        }

        // ** All new tests should be in light, unless they require specific db/config support
        // or take a really long time, or need some kind of manual verification.        
        if (isAll) {
            // Requires specific classpath.
            tests.addElement("org.eclipse.persistence.testing.tests.classpath.ClassPathTestModel");

            // Requires user "scott" unlocked and granted special privileges on oracle database
            tests.addElement("org.eclipse.persistence.testing.tests.feature.NativeModeCreatorTestModel");

            // Requires usage of Japanese machine and database.
            tests.addElement("org.eclipse.persistence.testing.tests.nls.japanese.NLSMappingWorkbenchIntegrationTestModel");

            // Requires specific LAB databases.
            tests.addElement("org.eclipse.persistence.testing.tests.sessionbroker.BrokerTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.sessionbroker.MultipleClientBrokersTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.sessionbroker.RMISessionBrokerRemoteModel");
            tests.addElement("org.eclipse.persistence.testing.tests.sessionbroker.ServerBrokerTestModel");
 
            // Requires remote config.
            tests.addElement("org.eclipse.persistence.testing.tests.remote.RMIRemoteModel");
            tests.addElement("org.eclipse.persistence.testing.tests.remote.rmi.IIOP.RMIIIOPRemoteModel");
            tests.addElement("org.eclipse.persistence.testing.tests.remote.suncorba.SunCORBARemoteModel");
            tests.addElement("org.eclipse.persistence.testing.tests.distributedservers.DistributedSessionBrokerServersModel");
            tests.addElement("org.eclipse.persistence.testing.tests.distributedservers.rcm.RCMDistributedServersModel");

            // Can take a long time, can deadlock.
            tests.addElement("org.eclipse.persistence.testing.tests.clientserver.ClientServerTestModel");

            // Requires EIS datasources config.
            tests.addElement("org.eclipse.persistence.testing.tests.eis.cobol.CobolTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.eis.xmlfile.XMLFileTestModel");
            
            // PLSQL
            tests.addElement("org.eclipse.persistence.testing.tests.plsql.PLSQLTestModel");
            tests.addElement("org.eclipse.persistence.testing.tests.plsql.PLSQLXMLTestModel");
        }

        for (int index = 0; index < tests.size(); ++index) {
            try {
                addTest((TestModel)Class.forName((String)tests.elementAt(index)).newInstance());
            } catch (Throwable exception) {
                System.out.println("Failed to set up " + tests.elementAt(index) + " \n" + exception);
                //exception.printStackTrace();
            }
        }

        // Sort the tests alphabetically.
        Collections.sort(this.getTests(), new Comparator() {
                public int compare(Object left, Object right) {
                    return Helper.getShortClassName(left.getClass()).compareTo(Helper.getShortClassName(right.getClass()));
                }
            }
        );
        testList = tests;
    }

    /**
     * Return all of the models for the testing tool.
     * To facilitate exporting the testing browser outside of visual age this mehtod has been modiied
     * to create the tests reflectively that way if a particular test fails it will not prevent the rest of the tests from building
     */
    public static Vector buildAllModels() {
        Vector models = new Vector();

        try {
            models.add(Class.forName("org.eclipse.persistence.testing.tests.SRGTestModel").newInstance());
        } catch (Exception exception) {
            System.out.println("Failed to set up org.eclipse.persistence.testing.tests.SRGTestModel" + " \n" + exception);
        }
        models.add(buildLRGTestModel());
        models.add(buildNonLRGTestModel());
        models.add(buildOracleTestModel());
        models.add(buildJPATestModel());
        models.add(buildPerformanceTestModel());

        Vector manualTest = new Vector();
        manualTest.add("org.eclipse.persistence.testing.tests.stress.StressTestModel");
        manualTest.add("org.eclipse.persistence.testing.tests.manual.ManualVerificationModel");

        TestModel manual = new TestModel();
        manual.setName("Manual Tests");
        for (int index = 0; index < manualTest.size(); ++index) {
            try {
                manual.addTest((TestModel)Class.forName((String)manualTest.elementAt(index)).newInstance());
            } catch (Exception exception) {
                System.out.println("Failed to set up " + manualTest.elementAt(index) + " \n" + exception);
            }
        }
        models.add(manual);

        return models;
    }

    /**
     * Build and return a model of all JPA tests.
     */
    public static TestModel buildJPATestModel() {
        List tests = new ArrayList();
        tests.add("org.eclipse.persistence.testing.tests.jpa.AllCMP3TestRunModel");
                    
        TestModel model = new TestModel();
        model.setName("JPA Tests");
        for (int index = 0; index < tests.size(); ++index) {
            try {
                model.addTest((TestModel)Class.forName((String)tests.get(index)).newInstance());
            } catch (Throwable exception) {
                System.out.println("Failed to set up " + tests.get(index) + " \n" + exception);
            }
        }
        return model;
    }    
    
    /**
     * Build and return a model of all Oracle specific tests.
     */
    public static TestModel buildOracleTestModel() {
        List tests = new ArrayList();
        tests.add("org.eclipse.persistence.testing.tests.OracleTestModel");
        tests.add("org.eclipse.persistence.testing.tests.OracleJPATestSuite");
        // Requires specific oracle database/driver (oci).
        tests.add("org.eclipse.persistence.testing.tests.xdb.XDBTestModel");
        tests.add("org.eclipse.persistence.testing.tests.xdb.XDBTestModelMWIntegration");
        tests.add("org.eclipse.persistence.testing.tests.unwrappedconnection.UnwrapConnectionXDBTestModel");
        
        TestModel model = new TestModel();
        model.setName("Oracle Tests");
        for (int index = 0; index < tests.size(); ++index) {
            Class cls;
            try {
                cls = Class.forName((String)tests.get(index));
                if(TestModel.class.isAssignableFrom(cls)) {
                    model.addTest((TestModel)cls.newInstance());
                } else {
                    Method suite = cls.getDeclaredMethod("suite", new Class[]{});
                    model.addTest((Test)suite.invoke(null, new Object[]{}));
                }
            } catch (Throwable exception) {
                System.out.println("Failed to set up " + tests.get(index) + " \n" + exception);
            }
        }
        return model;
    }
    
    /**
     * Return the JUnit suite to allow JUnit runner to find it.
     */
    public static junit.framework.TestSuite suite() {
        return buildLRGTestModel();
    }
    
    public static TestRunModel buildLRGTestModel() {
        TestRunModel model = new TestRunModel();
        model.setName("LRGTestModel");
        model.setDescription("This model runs all of the LRG tests.");
        model.isLight = true;
        model.addTests();

        return model;
    }

    public static TestRunModel buildAllTestModels() {
        TestRunModel model = new TestRunModel();
        model.setName("AllTestModels");
        model.setDescription("This model runs all of the tests under a single database, without client/server and JTS.");
        model.isLight = true;
        model.isAll = true;
        model.addTests();

        return model;
    }

    public static TestRunModel buildNonLRGTestModel() {
        TestRunModel model = new TestRunModel();
        model.setName("NonLRGTestModel");
        model.setDescription("This model includes all of the tests not in the LRG.");
        model.isLight = false;
        model.isAll = true;
        model.addTests();

        return model;
    }

    /**
     *  Created by Edwin Tang and used for BatchTestRunner
     */
    public static Vector buildLRGTestList() {
        TestRunModel model = new TestRunModel();
        model.isLight = true;
        model.addTests();
        return model.testList;
    }

    /**
     *  Created by Edwin Tang and used for BatchTestRunner
     */
    public static Vector buildAllTestModelsList() {
        TestRunModel model = new TestRunModel();
        model.isLight = true;
        model.isAll = true;
        model.addTests();
        return model.testList;
    }

    /**
     *  Created by Edwin Tang and used for BatchTestRunner
     */
    public static Vector buildNonLRGTestList() {
        TestRunModel model = new TestRunModel();
        model.isLight = false;
        model.isAll = true;
        model.addTests();
        return model.testList;
    }

    /**
     * Build and return a model of all performance tests.
     */
    public static TestModel buildPerformanceTestModel() {
        Vector performanceTests = new Vector();
        performanceTests.add("org.eclipse.persistence.testing.tests.performance.PerformanceComparisonModel");
        performanceTests.add("org.eclipse.persistence.testing.tests.performance.PerformanceTestModel");
        performanceTests.add("org.eclipse.persistence.testing.tests.performance.PerformanceTestModelRun");
        performanceTests.add("org.eclipse.persistence.testing.tests.performance.ConcurrencyComparisonTestModel");
        performanceTests.add("org.eclipse.persistence.testing.tests.performance.ConcurrencyRegressionTestModel");
        performanceTests.add("org.eclipse.persistence.testing.tests.performance.JavaPerformanceComparisonModel");
        performanceTests.add("org.eclipse.persistence.testing.tests.jpa.performance.JPAPerformanceTestModel");
        performanceTests.add("org.eclipse.persistence.testing.tests.jpa.memory.JPAMemoryTestModel");
                    
        TestModel performanceModel = new TestModel();
        performanceModel.setName("Performance Tests");
        for (int index = 0; index < performanceTests.size(); ++index) {
            try {
                performanceModel.addTest((TestModel)Class.forName((String)performanceTests.elementAt(index)).newInstance());
            } catch (Exception exception) {
                System.out.println("Failed to set up " + performanceTests.elementAt(index) + " \n" + exception);
            }
        }
        return performanceModel;
    }
    
    /**
     * Reset to the old login.
     */
    public void reset() {
        // Change the login if specified.
        if (login != null) {
            getDatabaseSession().logout();
            getDatabaseSession().login(oldLogin);
        }

        getExecutor().initializeConfiguredSystems();
    }

    /**
     * Allow the login to be configured.
     */
    public void setup() {
        // Change the login if specified.
        if (login != null) {
            oldLogin = getSession().getLogin();
            DatabaseLogin newLogin = (DatabaseLogin)login.clone();
            getDatabaseSession().logout();
            getDatabaseSession().login((DatabaseLogin)newLogin.clone());
        }

        // Change to native mode if specified.
        if (usesNativeMode) {
            getSession().getLogin().setUsesNativeSQL(true);
            getSession().getLogin().useNativeSequencing();
        }
    }
}
