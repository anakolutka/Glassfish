/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package org.glassfish.admingui.devtests;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EnterpriseServerTest extends BaseSeleniumTestClass {
    public static final String TRIGGER_ADVANCED_APPLICATIONS_CONFIGURATION = "i18nc.domain.AppsConfigPageHelp";
    public static final String TRIGGER_GENERAL_INFORMATION = "i18n.instance.GeneralTitle";
    public static final String TRIGGER_ADVANCED_DOMAIN_ATTRIBUTES = "i18nc.domain.DomainAttrsPageTitleHelp";
    public static final String TRIGGER_SYSTEM_PROPERTIES = "i18n.common.AdditionalProperties"; // There is no page help on sysprops pages anymore, it seems
    public static final String TRIGGER_RESOURCES = "i18nc.resourcesTarget.pageTitleHelp";

    // Disabling this test.  I'm not sure where this is trying to go.  jdl 10/6/10
//    @Test
    public void testAdvancedApplicationsConfiguration() {
        final String property = generateRandomString();
        final String value = property + "value";
        final String description = property + "description";

        gotoDasPage();
        clickAndWait("propertyForm:serverInstTabs:advanced", TRIGGER_ADVANCED_APPLICATIONS_CONFIGURATION);
        selenium.type("propertyForm:propertySheet:propertSectionTextField:reloadIntervalProp:ReloadInterval", "5");
        selenium.type("propertyForm:propertySheet:propertSectionTextField:AdminTimeoutProp:AdminTimeout", "30");

        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");

        selenium.type("propertyForm:basicTable:rowGroup1:0:col2:col1St", property);
        selenium.type("propertyForm:basicTable:rowGroup1:0:col3:col1St", value);
        selenium.type("propertyForm:basicTable:rowGroup1:0:col4:col1St", description);

        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        clickAndWait("propertyForm:serverInstTabs:advanced:domainAttrs", TRIGGER_ADVANCED_DOMAIN_ATTRIBUTES);
        clickAndWait("propertyForm:serverInstTabs:advanced:appConfig", TRIGGER_ADVANCED_APPLICATIONS_CONFIGURATION);

        assertEquals("5", selenium.getValue("propertyForm:propertySheet:propertSectionTextField:reloadIntervalProp:ReloadInterval"));
        assertEquals("30", selenium.getValue("propertyForm:propertySheet:propertSectionTextField:AdminTimeoutProp:AdminTimeout"));
        
        assertTableRowCount("propertyForm:basicTable", count);
    }

    @Test
    public void testAdvancedDomainAttributes() {
        clickAndWait("treeForm:tree:nodes:nodes_link", TRIGGER_ADVANCED_DOMAIN_ATTRIBUTES);
        selenium.type("propertyForm:propertySheet:propertSectionTextField:localeProp:Locale", "fr");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        clickAndWait("propertyForm:domainTabs:appConfig", TRIGGER_ADVANCED_APPLICATIONS_CONFIGURATION);
        clickAndWait("propertyForm:domainTabs:domainAttrs", TRIGGER_ADVANCED_DOMAIN_ATTRIBUTES);

        assertEquals("fr", selenium.getValue("propertyForm:propertySheet:propertSectionTextField:localeProp:Locale"));
        selenium.type("propertyForm:propertySheet:propertSectionTextField:localeProp:Locale", "");
        selenium.click("propertyForm:propertyContentPage:topButtons:saveButton");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
    }

    @Test
    public void testSystemProperties() {
        final String property = generateRandomString();
        final String value = property + "value";
        final String description = property + "description";

        gotoDasPage();
        clickAndWait("propertyForm:serverInstTabs:serverInstProps", TRIGGER_SYSTEM_PROPERTIES);

        int count = addTableRow("propertyForm:sysPropsTable", "propertyForm:sysPropsTable:topActionsGroup1:addSharedTableButton");
        selenium.type("propertyForm:sysPropsTable:rowGroup1:0:col2:col1St", property);
        selenium.type("propertyForm:sysPropsTable:rowGroup1:0:overrideValCol:overrideVal", value);

        clickAndWait("propertyForm:SysPropsPage:topButtons:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        gotoDasPage();
        clickAndWait("propertyForm:serverInstTabs:serverInstProps", TRIGGER_SYSTEM_PROPERTIES);

        assertTableRowCount("propertyForm:sysPropsTable", count);
    }

    @Test
    public void testServerResourcesPage() {
        final String jndiName = "jdbcResource"+generateRandomString();
        final String description = "devtest test for server->resources page- " + jndiName;
        final String tableID = "propertyForm:resourcesTable";

        JdbcTest jdbcTest = new JdbcTest();
        jdbcTest.createJDBCResource(jndiName, description, "server", MonitoringTest.TARGET_SERVER_TYPE);
        
        gotoDasPage();
        clickAndWait("propertyForm:serverInstTabs:resources", TRIGGER_RESOURCES);
        assertTrue(selenium.isTextPresent(jndiName));

        int jdbcCount = getTableRowCountByValue(tableID, "JDBC Resources", "col3:type");
        int customCount = getTableRowCountByValue(tableID, "Custom Resources", "col3:type");

        selenium.select("propertyForm:resourcesTable:topActionsGroup1:filter_list", "label=Custom Resources");
        waitForTableRowCount(tableID, customCount);

        selenium.select("propertyForm:resourcesTable:topActionsGroup1:filter_list", "label=JDBC Resources");
        waitForTableRowCount(tableID, jdbcCount);

        selectTableRowByValue("propertyForm:resourcesTable", jndiName);
        waitForButtonEnabled("propertyForm:resourcesTable:topActionsGroup1:button1");
        selenium.click("propertyForm:resourcesTable:topActionsGroup1:button1");
        waitForButtonDisabled("propertyForm:resourcesTable:topActionsGroup1:button1");

        /*selenium.select("propertyForm:resourcesTable:topActionsGroup1:actions", "label=JDBC Resources");
        waitForPageLoad(JdbcTest.TRIGGER_NEW_JDBC_RESOURCE, true);
        clickAndWait("form:propertyContentPage:topButtons:cancelButton", JdbcTest.TRIGGER_JDBC_RESOURCES);*/

        jdbcTest.deleteJDBCResource(jndiName, "server", MonitoringTest.TARGET_SERVER_TYPE);
    }

    public void waitForTableRowCount(String tableID, int count) {
        for (int i = 0;; i++) {
            if (i >= 1000) {
                Assert.fail("timeout");
            }
            try {
                int tableCount = getTableRowCount(tableID);
                if (tableCount == count) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sleep(500);
        }
    }

    public void gotoDasPage() {
        clickAndWait("treeForm:tree:applicationServer:applicationServer_link", TRIGGER_GENERAL_INFORMATION);
    }
}