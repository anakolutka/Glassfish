
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

/*
 * ConnectorHandlers.java
 *
 * Created on Sept 1, 2006, 8:32 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
/**
 *
 */
package org.glassfish.admingui.jdbc.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;




public class ConnectorsHandlers {

    /** Creates a new instance of ConnectorsHandler */
    public ConnectorsHandlers() {
    }


    /**
     *	<p> This handler creates a ConnectorConnection Pool to be used in the wizard
     */
    @Handler(id = "getConnectorConnectionPoolWizard", input = {
@HandlerInput(name = "fromStep2", type = Boolean.class),
@HandlerInput(name = "fromStep1", type = Boolean.class),
@HandlerInput(name = "attrMap", type = Map.class),
@HandlerInput(name = "poolName", type = String.class),
@HandlerInput(name = "resAdapter", type = String.class)
}, output = {
@HandlerOutput(name = "connectionDefinitions", type = List.class)
})
    public static void getConnectorConnectionPoolWizard(HandlerContext handlerCtx) {
        Boolean fromStep2 = (Boolean) handlerCtx.getInputValue("fromStep2");
        Boolean fromStep1 = (Boolean) handlerCtx.getInputValue("fromStep1");
        if ((fromStep2 != null) && fromStep2) {
            //valueMap is already in session map, we don't want to change anything.
            Map extra = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPoolExtra");
            String resAdapter = (String) extra.get("ResourceAdapterName");
            List defs = getConnectionDefinitions(resAdapter);
            handlerCtx.setOutputValue("connectionDefinitions", defs);
        } else if ((fromStep1 != null) && fromStep1) {
            //this is from Step 1 where the page is navigated when changing the dropdown of resource adapter.
            //since the dropdown is immediate, the wizardPoolExtra map is not updated yet, we need
            //to update it manually and also set the connection definition map according to this resource adapter.
            String resAdapter = (String) handlerCtx.getInputValue("resAdapter");
            String poolName = (String) handlerCtx.getInputValue("poolName");
            if (resAdapter == null || resAdapter.equals("")) {
                handlerCtx.setOutputValue("connectionDefinitions", new ArrayList());
            } else {
                Map extra = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPoolExtra");
                extra.put("ResourceAdapterName", resAdapter);
                extra.put("Name", poolName);
                List defs = getConnectionDefinitions(resAdapter);
                handlerCtx.setOutputValue("connectionDefinitions", defs);
            }
        } else {
            Map extra = new HashMap();
            Map attrMap = (Map) handlerCtx.getInputValue("attrMap");
            //Map attrMap = (Map)proxyHandlers.getDefaultProxyAttrsMap("v3:pp=/domain,type=resources", "connector-connection-pool");
            handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("valueMap", attrMap);
            handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("wizardPoolExtra", extra);
        }
    }

    /**
     *	<p> updates the wizard map
     */
    @Handler(id = "updateConnectorConnectionPoolWizard")
    public static void updateConnectorConnectionPoolWizard(HandlerContext handlerCtx) {
        Map extra = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPoolExtra");
        String resAdapter = (String) extra.get("ResourceAdapterName");
        String definition = (String) extra.get("ConnectionDefinitionName");

        String previousDefinition = (String) extra.get("previousDefinition");
        String previousResAdapter = (String) extra.get("previousResAdapter");

        if (definition.equals(previousDefinition) && resAdapter.equals(previousResAdapter)) {
        //User didn't change defintion and adapter, keep the properties table content the same.
        } else {
            // To Do fo V3
            //if (!GuiUtil.isEmpty(definition) && !GuiUtil.isEmpty(resAdapter)) {
            //    Properties props = getConnectorConnectionPoolProps("getMCFConfigProps", resAdapter, "connection-definition-name", definition);
            //    handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("wizardPoolProperties", props);
            //}
            extra.put("previousDefinition", definition);
            extra.put("previousResAdapter", resAdapter);
        }
    }
    
    /**
     *	<p> updates the wizard map properties on step 2
     */
    @Handler(id = "updateConnectorConnectionPoolWizardStep2")
    public static void updateConnectorConnectionPoolWizardStep2(HandlerContext handlerCtx) {
        Map extra = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPoolExtra");
        Map attrs = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("valueMap");

        String resAdapter = (String) extra.get("ResourceAdapterName");
        String definition = (String) extra.get("ConnectionDefinitionName");
        String name = (String) extra.get("Name");

        attrs.put("Name", name);
        attrs.put("ConnectionDefinitionName", definition);
        attrs.put("ResourceAdapterName", resAdapter);
    }  


    private static List getConnectionDefinitions(String resAdapter) {
        ArrayList defs = new ArrayList();
        if (resAdapter == null || resAdapter.equals("")) {
            return defs;
        }
        /* TODO-V3
        
        }
         */
        return defs;
    }

}
 
