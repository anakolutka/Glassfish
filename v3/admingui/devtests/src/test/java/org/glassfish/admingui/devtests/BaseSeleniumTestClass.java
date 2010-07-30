/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.admingui.devtests;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class BaseSeleniumTestClass {
    public static final String CURRENT_WINDOW = "selenium.browserbot.getCurrentWindow()";
    public static final String MSG_NEW_VALUES_SAVED = "New values successfully saved.";
    public static final String TRIGGER_COMMON_TASKS = "Please Register";
    public static final String TRIGGER_REGISTRATION_PAGE = "Receive patch information and bug updates, screencasts and tutorials, support and training offerings, and more";
    protected static Selenium selenium;
    protected static int TIMEOUT = 120;

    @BeforeClass
    public static void setUp() throws Exception {
        if (selenium == null) {
            String browser = getParameter("browser", "firefox");
            String port = getParameter("admin.port", "4848");
            String seleniumPort = getParameter("selenium.port", "4444");
            String baseUrl = "http://localhost:" + port;

            System.out.println("The GlassFish Admin console is at " + baseUrl +".  The Selenium server is listening on " + seleniumPort +
                    " and will use " + browser + " as the test browser.");

            selenium = new DefaultSelenium("localhost", Integer.parseInt(seleniumPort), "*" + browser, baseUrl);
            selenium.start();
            (new BaseSeleniumTestClass()).openAndWait("/common/index.jsf", TRIGGER_COMMON_TASKS); // Make sure the server has started and the user logged in
        }
    }

    @Before
    public void reset() {
        clickAndWait("treeForm:tree:registration:registration_link", TRIGGER_REGISTRATION_PAGE);
    }

    protected String generateRandomString() {
        SecureRandom random = new SecureRandom();

        return new BigInteger(130, random).toString(16);
    }

    protected int generateRandomNumber() {
        Random r = new Random();
        return Math.abs(r.nextInt()) + 1;
    }

    protected int generateRandomNumber(int max) {
        Random r = new Random();
        return Math.abs(r.nextInt(max - 1)) + 1;
    }

    protected <T> T selectRandomItem(T... items) {
        Random r = new Random();
        
        return items[r.nextInt(items.length)];
    }

    protected int getTableRowCount(String id) {
        String text = selenium.getText(id);
        int count = Integer.parseInt(text.substring(text.indexOf("(") + 1, text.indexOf(")")));

        return count;
    }

    protected void openAndWait(String url, String triggerText) {
        selenium.open(url);
        // wait for 2 minutes, as that should be enough time to insure that the admin console app has been deployed by the server
        waitForPageLoad(triggerText, TIMEOUT);
    }

    /**
     * Click the specified element and wait for the specified trigger text on the resulting page, timing out TIMEOUT seconds.
     *
     * @param triggerText
     */
    protected void clickAndWait(String id, String triggerText) {
        clickAndWait(id, triggerText, TIMEOUT);
    }

    protected void clickAndWait(String id, String triggerText, int seconds) {
        selenium.click(id);
        waitForPageLoad(triggerText, seconds);
    }

    
    protected void clickAndWaitForElement(String clickId, String elementId) {
        selenium.click(clickId);
        for (int second = 0; ; second++) {
            if (second >= 60) {
                Assert.fail("timeout");
            }
            try {
                if (selenium.isElementPresent(elementId)) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sleep(500);
        }
    }

    protected void clickAndWaitForButtonEnabled(String id) {
        selenium.click(id);
        waitForButtonEnabled(id);
    }

    
    // Argh!
    protected void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cause the test to wait for the page to load.  This will be used, for example, after an initial page load
     * (selenium.open) or after an Ajaxy navigation request has been made.
     *
     * @param triggerText The text that should appear on the page when it has finished loading
     * @param timeout     How long to wait (in seconds)
     */
    protected void waitForPageLoad(String triggerText, int timeout) {
        waitForPageLoad(triggerText, timeout, false);
    }

    protected void waitForPageLoad(String triggerText, boolean textShouldBeMissing) {
        waitForPageLoad(triggerText, TIMEOUT, textShouldBeMissing);
    }

    protected void waitForPageLoad(String triggerText, int timeout, boolean textShouldBeMissing) {
        for (int second = 0; ; second++) {
            if (second >= timeout) {
                Assert.fail("timeout");
            }
            try {
                if (!textShouldBeMissing) {
                    if (selenium.isTextPresent(triggerText)) {
                        break;
                    }
                } else {
                    if (!selenium.isTextPresent(triggerText)) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sleep(1000);
        }
    }

    protected void waitForButtonEnabled(String buttonId) {
        waitForCondition("document.getElementById('" + buttonId + "').disabled == false", 10000);
    }

    protected void waitForButtonDisabled(String buttonId) {
        String value = selenium.getEval(CURRENT_WINDOW + ".document.getElementById('" + buttonId + "').disabled");
        waitForCondition("document.getElementById('" + buttonId + "').disabled == true", 10000);
    }

    protected void waitForCondition(String js, int timeOutInMillis) {
        selenium.waitForCondition(CURRENT_WINDOW + "." + js, Integer.toString(timeOutInMillis));
    }

    protected void deleteRow(String buttonId, String tableId, String triggerText) {
        deleteRow(buttonId, tableId, triggerText, "col0", "col1");
    }

    protected void deleteRow(String buttonId, String tableId, String triggerText, String selectColId, String valueColId) {
        rowActionWithConfirm( buttonId,  tableId,  triggerText,  selectColId,  valueColId);
        waitForPageLoad(triggerText, true);
    }

     protected void rowActionWithConfirm(String buttonId, String tableId, String triggerText) {
        rowActionWithConfirm(buttonId, tableId, triggerText, "col0", "col1");
    }

    protected void rowActionWithConfirm(String buttonId, String tableId, String triggerText, String selectColId, String valueColId) {
        selenium.chooseOkOnNextConfirmation();
        selectTableRowByValue(tableId, triggerText, selectColId, valueColId);
        selenium.click(buttonId);
        if (selenium.isConfirmationPresent()) {
            selenium.getConfirmation();
        }
    }


    /**
     * This method will scan the all ths links for the link with the given text.  We can't rely on a link's position
     * in the table, as row order may vary (if, for example, a prior test run left data behind).  If the link is not
     * found, null is returned, so the calling code may need to check the return value prior to use.
     *
     * @param baseId
     * @param value
     * @return
     */
    protected String getLinkIdByLinkText(String baseId, String value) {
        String[] links = selenium.getAllLinks();

        for (String link : links) {
            if (link.startsWith(baseId)) {
                String linkText = selenium.getText(link);
                if (value.equals(linkText)) {
                    return link;
                }
            }
        }

        return null;
    }

    protected void selectTableRowByValue(String tableId, String value) {
        selectTableRowByValue(tableId, value, "col0", "col1");
    }

    protected void selectTableRowByValue(String tableId, String value, String selectColId, String valueColId) {
        try {
            int row = 0;
            while (true) { // iterate over any rows
                // Assume one row group for now and hope it doesn't bite us
                String text = selenium.getText(tableId + ":rowGroup1:" + row + ":" + valueColId);
                if (text.equals(value)) {
                    selenium.click(tableId + ":rowGroup1:" + row + ":" + selectColId + ":select");
                    return;
                }
                row++;
            }
        } catch (Exception e) {
            Assert.fail("The specified row was not found: " + value);
        }

    }

    protected String getTableRowByValue(String tableId, String value, String valueColId) {
        try {
            int row = 0;
            while (true) { // iterate over any rows
                // Assume one row group for now and hope it doesn't bite us
                String text = selenium.getText(tableId + ":rowGroup1:" + row + ":" + valueColId);
                if (text.equals(value)) {
                    return tableId + ":rowGroup1:" + row  ;
                }
                row++;
            }
        } catch (Exception e) {
            Assert.fail("The specified row was not found: " + value);
            return "";
        }
    }


    protected int addTableRow(String tableId, String buttonId) {
        return addTableRow(tableId, buttonId, "Additional Properties");
    }

    protected int addTableRow(String tableId, String buttonId, String countLabel) {
        int count = getTableRowCount(tableId);
        clickAndWait(buttonId, countLabel + " (" + (++count) + ")");
        return count;
    }

    protected void assertTableRowCount(String tableId, int count) {
        assertEquals(count, getTableRowCount(tableId));
    }

    // Look at all those params. Maybe this isn't such a hot idea.

    /**
     * @param resourceName
     * @param tableId
     * @param enableButtonId
     * @param statusID
     * @param backToTableButtonId
     * @param tableTriggerText
     * @param editTriggerText
     */
    protected void testEnableButton(String resourceName,
                                    String tableId,
                                    String enableButtonId,
                                    String statusID,
                                    String backToTableButtonId,
                                    String tableTriggerText,
                                    String editTriggerText) {
        testEnableDisableButton(resourceName, tableId, enableButtonId, statusID, backToTableButtonId, tableTriggerText, editTriggerText, "on");
    }

    protected void testDisableButton(String resourceName,
                                     String tableId,
                                     String disableButtonId,
                                     String statusId,
                                     String backToTableButtonId,
                                     String tableTriggerText,
                                     String editTriggerText) {
        testEnableDisableButton(resourceName, tableId, disableButtonId, statusId, backToTableButtonId, tableTriggerText, editTriggerText, "off");
    }

    private void testEnableDisableButton(String resourceName,
                                         String tableId,
                                         String enableButtonId,
                                         String statusId,
                                         String backToTableButtonId,
                                         String tableTriggerText,
                                         String editTriggerText,
                                         String state) {
        selectTableRowByValue(tableId, resourceName);
        waitForButtonEnabled(enableButtonId);
        selenium.click(enableButtonId);
        waitForButtonDisabled(enableButtonId);

        clickAndWait(getLinkIdByLinkText(tableId, resourceName), editTriggerText);
        assertEquals(state, selenium.getValue(statusId));
        clickAndWait(backToTableButtonId, tableTriggerText);
    }

    private static String getParameter(String paramName, String defaultValue) {
        String value = System.getenv(paramName);
        if (value == null) {
            value = System.getProperty(paramName);
        }
        if (value == null) {
            value = defaultValue;
        }

        return value;
    }
}
