package org.glassfish.admingui.devtests;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: jasonlee
 * Date: Mar 3, 2010
 * Time: 11:09:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class JdbcTest extends BaseSeleniumTestClass {

    @Test
    public void testPoolEdit() {
        openAndWait("/common/commonTask.jsf", "Common Tasks");
        selenium.click("treeForm:tree:resources:JDBC:connectionPoolResources:amxppdomainresourcestypejdbc-connection-poolname__TimerPool:link");

        waitForPageLoad("Edit JDBC Connection Pool");
        selenium.click("propertyForm:propertyContentPage:ping");
        waitForPageLoad("Ping Succeeded");
    }

    @Test
    public void testCreatingConnectionPool() {
        openAndWait("/jdbc/jdbcConnectionPools.jsf", "JDBC Connection Pools");
        selenium.click("propertyForm:poolTable:topActionsGroup1:newButton");
        waitForPageLoad("New JDBC Connection Pool (Step 1 of 2)");

        selenium.type("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:jndiProp:name","TestPool");
        selenium.select("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:resTypeProp:resType", "label=javax.sql.DataSource");
        selenium.select("propertyForm:propertyContentPage:propertySheet:generalPropertySheet:dbProp:db", "label=Derby");
        selenium.click("propertyForm:propertyContentPage:topButtons:nextButton");
        waitForPageLoad("New JDBC Connection Pool (Step 2 of 2)");

        selenium.type("form2:sheet:generalSheet:descProp:desc", "devtest test connection pool");
        selenium.click("form2:propertyContentPage:topButtons:finishButton");
        waitForPageLoad("Pools (3)");
        assertTrue(selenium.isTextPresent("TestPool") && selenium.isTextPresent("devtest test connection pool"));

        selenium.chooseOkOnNextConfirmation();
        selenium.click("propertyForm:poolTable:rowGroup1:1:col0:select");
        selenium.click("propertyForm:poolTable:topActionsGroup1:button1");
        waitForPageLoad("Pools (2)");
        selenium.getConfirmation();
        assertFalse(selenium.isTextPresent("TestPool") && selenium.isTextPresent("devtest test connection pool"));
    }
    
    @Test
    public void testJdbcResources() {
		openAndWait("/jdbc/jdbcResources.jsf", "JDBC Resources");
		selenium.click("propertyForm:resourcesTable:topActionsGroup1:newButton");
        waitForPageLoad("New JDBC Resource");

		selenium.type("propertyForm:propertySheet:propertSectionTextField:nameNew:name", "jdbc/testResource");
		selenium.type("propertyForm:propertySheet:propertSectionTextField:descProp:desc", "Test Resource");
		selenium.click("propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        waitForPageLoad("Additional Properties (1)");

		selenium.type("propertyForm:basicTable:rowGroup1:0:col2:col1St", "testProp");
		selenium.type("propertyForm:basicTable:rowGroup1:0:col3:col1St", "testValue");
		selenium.type("propertyForm:basicTable:rowGroup1:0:col4:col1St", "test description");
		selenium.click("propertyForm:propertyContentPage:topButtons:newButton");
        waitForPageLoad("Resources (3)");

        assertTrue(selenium.isTextPresent("jdbc/testResource"));
		assertTrue(selenium.isTextPresent("Test Resource"));
		selenium.click("propertyForm:resourcesTable:rowGroup1:2:col1:link");
        waitForPageLoad("Edit JDBC Resource");

        assertEquals("testProp", selenium.getValue("propertyForm:basicTable:rowGroup1:0:col2:col1St"));
		assertEquals("testValue", selenium.getValue("propertyForm:basicTable:rowGroup1:0:col3:col1St"));
		assertEquals("test description", selenium.getValue("propertyForm:basicTable:rowGroup1:0:col4:col1St"));

		selenium.click("propertyForm:propertySheet:propertSectionTextField:statusProp:enabled");
		selenium.click("propertyForm:propertyContentPage:topButtons:saveButton");
		waitForPageLoad("New values successfully saved.");

		selenium.click("propertyForm:propertyContentPage:topButtons:cancelButton");
        waitForPageLoad("Resources (3)");
		assertTrue(selenium.isTextPresent("false"));

/*
		selenium.click("propertyForm:resourcesTable:rowGroup1:2:col0:select");
		selenium.click("propertyForm:resourcesTable:topActionsGroup1:button2");
        selenium.waitForCondition("document.getElementById('propertyForm:resourcesTable:rowGroup1:2:col22:typeCol').innerHTML == 'true'", "2500");
        
		selenium.click("propertyForm:resourcesTable:rowGroup1:2:col0:select");
		selenium.click("propertyForm:resourcesTable:topActionsGroup1:button3");
		assertTrue(selenium.isElementPresent("propertyForm:resourcesTable:rowGroup1:2:col22:typeCol"));
        selenium.waitForCondition("document.getElementById('propertyForm:resourcesTable:rowGroup1:2:col22:typeCol').innerHTML == 'false'", "2500");
*/

        selenium.chooseOkOnNextConfirmation();
		selenium.click("propertyForm:resourcesTable:rowGroup1:2:col0:select");
		selenium.click("propertyForm:resourcesTable:topActionsGroup1:button1");
        selenium.getConfirmation();
		waitForPageLoad("Resources (2)");
    }
}