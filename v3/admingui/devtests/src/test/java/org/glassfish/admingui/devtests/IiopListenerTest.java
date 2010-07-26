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

public class IiopListenerTest extends BaseSeleniumTestClass {
    private static final String TRIGGER_IIOP_LISTENERS = "A number of IIOP listeners can be configured for an ORB";
    private static final String TRIGGER_NEW_IIOP_LISTENER = "New IIOP Listener";
    private static final String TRIGGER_EDIT_IIOP_LISTENER = "Edit IIOP Listener";
    private static final String TRIGGER_ORB = "The Object Request Broker (ORB) is the central component of CORBA";
    private static final String TRIGGER_ORB_SUCCESS = "New values successfully saved";

    @Test
    public void testAddIiopListener() {
        final String iiopName = "testIiopListener" + generateRandomString();
        final String networkAddress = "0.0.0.0";
        final String listenerPort = "1234";

        clickAndWait("treeForm:tree:configuration:orb:iiopListeners:iiopListeners_link", TRIGGER_IIOP_LISTENERS);
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_IIOP_LISTENER);
        selenium.type("propertyForm:propertySheet:generalSettingsSetion:IiopNameTextProp:IiopNameText", iiopName);
        selenium.type("propertyForm:propertySheet:generalSettingsSetion:NetwkAddrProp:NetwkAddr", networkAddress);
        selenium.type("propertyForm:propertySheet:generalSettingsSetion:ListenerPortProp:ListenerPort", listenerPort);

        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");
        selenium.type("propertyForm:basicTable:rowGroup1:0:col2:col1St", "a");
        selenium.type("propertyForm:basicTable:rowGroup1:0:col3:col1St", "b");
        selenium.type("propertyForm:basicTable:rowGroup1:0:col4:col1St", "c");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_IIOP_LISTENERS);
        assertTrue(selenium.isTextPresent(iiopName));

        clickAndWait(getLinkIdByLinkText("propertyForm:configs", iiopName), TRIGGER_EDIT_IIOP_LISTENER);
        assertEquals(networkAddress, selenium.getValue("propertyForm:propertySheet:generalSettingsSetion:NetwkAddrProp:NetwkAddr"));
        assertEquals(listenerPort, selenium.getValue("propertyForm:propertySheet:generalSettingsSetion:ListenerPortProp:ListenerPort"));

        assertTableRowCount("propertyForm:basicTable", count);

        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_IIOP_LISTENERS);

        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", iiopName);
    }

    @Test
    public void testORB() {
        final String totalConn = "1048";
        final String maxMsgSize = "2048";
        clickAndWait("treeForm:tree:configurations:server-config:orb:orb_link", TRIGGER_ORB);
        selenium.type("form1:propertySheet:propertySectionTextField:TotalConnsProp:TotalConns", totalConn);
        selenium.select("form1:propertySheet:propertySectionTextField:MaxMsgSizeProp:MaxMsgSize", "label="+maxMsgSize);
        clickAndWait("form1:propertyContentPage:topButtons:saveButton", TRIGGER_ORB_SUCCESS);
        assertEquals(totalConn, selenium.getValue("form1:propertySheet:propertySectionTextField:TotalConnsProp:TotalConns"));
        assertEquals(maxMsgSize, selenium.getValue("form1:propertySheet:propertySectionTextField:MaxMsgSizeProp:MaxMsgSize"));

        clickAndWaitForButtonEnabled("form1:propertyContentPage:loadDefaultsButton");
        assertEquals("1024", selenium.getValue("form1:propertySheet:propertySectionTextField:TotalConnsProp:TotalConns"));
        assertEquals("1024", selenium.getValue("form1:propertySheet:propertySectionTextField:MaxMsgSizeProp:MaxMsgSize"));
        
    }
}