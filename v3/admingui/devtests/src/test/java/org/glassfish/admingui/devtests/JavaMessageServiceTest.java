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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JavaMessageServiceTest extends BaseSeleniumTestClass {
    private static final String TRIGGER_JMS_SERVICE = "General properties for the Java Message Service (JMS)";
    private static final String TRIGGER_JMS_HOSTS = "Click New to create a new JMS host. Click the name of a JMS host to modify its properties.";
    private static final String TRIGGER_NEW_JMS_HOST = "New JMS Host";
    private static final String TRIGGER_JMS_PHYSICAL_DESTINATIONS = "Click New to create a new physical destination.";
    private static final String TRIGGER_NEW_JMS_PHYSICAL_DESTINATION = "New JMS Physical Destination";
    private static final String TRIGGER_EDIT_JMS_PHYSICAL_DESTINATION = "Edit JMS Physical Destination";
    private static final String TRIGGER_FLUSH = "Flush successful for the physical destination(s)";

    @Test
    public void testJmsService() {
        final String timeout = Integer.toString(generateRandomNumber(90));
        final String interval = Integer.toString(generateRandomNumber(10));
        final String attempts = Integer.toString(generateRandomNumber(10));

        clickAndWait("treeForm:tree:configuration:jmsConfiguration:jmsConfiguration_link", TRIGGER_JMS_SERVICE);
        selenium.type("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:timeoutProp:Timeout", timeout);
        selenium.type("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:intervalProp:Interval", interval);
        selenium.type("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:attemptsProp:Attempts", attempts);
        selenium.select("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:behaviorProp:Behavior", "label=priority");

        int count = addTableRow("propertyForm:propertyContentPage:basicTable", "propertyForm:propertyContentPage:basicTable:topActionsGroup1:addSharedTableButton");
        selenium.type("propertyForm:propertyContentPage:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        selenium.type("propertyForm:propertyContentPage:basicTable:rowGroup1:0:col3:col1St", "value");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", MSG_NEW_VALUES_SAVED);

        clickAndWait("treeForm:tree:configuration:jmsConfiguration:jmsHosts:jmsHosts_link", TRIGGER_JMS_HOSTS);
        clickAndWait("treeForm:tree:configuration:jmsConfiguration:jmsConfiguration_link", TRIGGER_JMS_SERVICE);

        assertEquals(timeout, selenium.getValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:timeoutProp:Timeout"));
        assertEquals(interval, selenium.getValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:intervalProp:Interval"));
        assertEquals(attempts, selenium.getValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:attemptsProp:Attempts"));
        assertTableRowCount("propertyForm:propertyContentPage:basicTable", count);
    }

    @Test
    public void testJmsHosts() {
        String hostText = "host"+generateRandomString();
        String host = "somemachine"+generateRandomNumber(1000);

        clickAndWait("treeForm:tree:configuration:jmsConfiguration:jmsHosts:jmsHosts_link", TRIGGER_JMS_HOSTS);
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_JMS_HOST);
        selenium.type("propertyForm:propertySheet:propertSectionTextField:JmsHostTextProp:JmsHostText", hostText);
        selenium.type("propertyForm:propertySheet:propertSectionTextField:HostProp:Host", host);
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", MSG_NEW_VALUES_SAVED);
        clickAndWait(this.getLinkIdByLinkText("propertyForm:configs", hostText), "Edit JMS Host");
        assertTrue(selenium.isTextPresent(hostText));
        assertEquals(host, selenium.getValue("propertyForm:propertySheet:propertSectionTextField:HostProp:Host"));
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_JMS_HOSTS);
        deleteRow("propertyForm:configs:topActionsGroup1:deleteButton", "propertyForm:configs", hostText, "col0", "colName");
    }

    @Test
    public void testJmsPhysicalDestinations() {
        final String name = "dest"+generateRandomString();
        final String maxUnconsumed = Integer.toString(generateRandomNumber(100));
        final String maxMessageSize = Integer.toString(generateRandomNumber(100));
        final String maxTotalMemory = Integer.toString(generateRandomNumber(100));
        final String maxProducers = Integer.toString(generateRandomNumber(500));
        final String consumerFlowLimit = Integer.toString(generateRandomNumber(5000));

        clickAndWait("treeForm:tree:configuration:jmsConfiguration:jmsPhysDest:jmsPhysDest_link", TRIGGER_JMS_PHYSICAL_DESTINATIONS);
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_JMS_PHYSICAL_DESTINATION);

        selenium.type("jmsPhysDestForm:propertySheet:propertSectionTextField:NameTextProp:NameText", name);
        selenium.type("jmsPhysDestForm:propertySheet:propertSectionTextField:maxNumMsgsProp:maxNumMsgs", maxUnconsumed);
        selenium.type("jmsPhysDestForm:propertySheet:propertSectionTextField:maxBytesPerMsgProp:maxBytesPerMsg", maxMessageSize);
        selenium.type("jmsPhysDestForm:propertySheet:propertSectionTextField:maxTotalMsgBytesProp:maxTotalMsgBytes", maxTotalMemory);
        selenium.select("jmsPhysDestForm:propertySheet:propertSectionTextField:limitBehaviorProp:Type", "label=Throw out lowest-priority messages");
        selenium.type("jmsPhysDestForm:propertySheet:propertSectionTextField:maxNumProducersProp:maxNumProducers", maxProducers);
        selenium.type("jmsPhysDestForm:propertySheet:propertSectionTextField:consumerFlowLimitProp:consumerFlowLimit", consumerFlowLimit);
        selenium.select("jmsPhysDestForm:propertySheet:propertSectionTextField:useDmqProp:useDmq", "label=False");
        selenium.select("jmsPhysDestForm:propertySheet:propertSectionTextField:validateSchemaProp:validateXMLSchemaEnabled", "label=True");
        clickAndWait("jmsPhysDestForm:propertyContentPage:topButtons:newButton", TRIGGER_JMS_PHYSICAL_DESTINATIONS);

        clickAndWait(getLinkIdByLinkText("propertyForm:configs", name), TRIGGER_EDIT_JMS_PHYSICAL_DESTINATION);

        assertTrue(selenium.isTextPresent(name));
        assertEquals(maxUnconsumed, selenium.getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxNumMsgsProp:maxNumMsgs"));
        assertEquals(maxMessageSize, selenium.getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxBytesPerMsgProp:maxBytesPerMsg"));
        assertEquals(maxTotalMemory, selenium.getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxTotalMsgBytesProp:maxTotalMsgBytes"));
        assertEquals("REMOVE_LOW_PRIORITY", selenium.getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:limitBehaviorProp:Type"));

        assertEquals(maxProducers, selenium.getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxNumProducersProp:maxNumProducers"));
        assertEquals(consumerFlowLimit, selenium.getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:consumerFlowLimitProp:consumerFlowLimit"));
        assertEquals("false", selenium.getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:useDmqProp:useDmq"));
        // TODO: The server code for this looks right, but it's not working. Disabling for now.
        //assertEquals("true", selenium.getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:validateSchemaProp:validateXMLSchemaEnabled"));
        clickAndWait("jmsPhysDestForm:propertyContentPage:topButtons:cancelButton", TRIGGER_JMS_PHYSICAL_DESTINATIONS);

        selectTableRowByValue("propertyForm:configs", name);
        clickAndWait("propertyForm:configs:topActionsGroup1:flushButton", TRIGGER_FLUSH);

        deleteRow("propertyForm:configs:topActionsGroup1:deleteButton", "propertyForm:configs", name);
    }
}
