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

package com.sun.enterprise.connectors;

import com.sun.enterprise.connectors.util.ResourcesUtil;
import java.util.*;
import java.util.logging.*;
import javax.naming.*;
import com.sun.enterprise.server.*;

/**
 * AdminObject administration service. It performs the functionality of 
 * creating and deleting the Admin Objects
 * @author    Binod P.G and Srikanth P 
 */


public class ConnectorAdminObjectAdminServiceImpl extends 
               ConnectorServiceImpl implements ConnectorAdminService {


    public ConnectorAdminObjectAdminServiceImpl() {
        super();
    }
     
    public void addAdminObject (
            String appName,
            String connectorName,
            String jndiName,
            String adminObjectType,
            Properties props)
            throws ConnectorRuntimeException 
    {
        ActiveResourceAdapter ar = 
                    _registry.getActiveResourceAdapter(connectorName);
        if(ar == null) {
            ifSystemRarLoad(connectorName);
            ar = _registry.getActiveResourceAdapter(connectorName);
        }
        if (ar instanceof ActiveInboundResourceAdapter)  {
            ActiveInboundResourceAdapter air = 
                            (ActiveInboundResourceAdapter) ar;
            air.addAdminObject(appName,connectorName,jndiName,
                            adminObjectType, props);
        } else {
            ConnectorRuntimeException cre = new ConnectorRuntimeException(
                            "This adapter is not 1.5 compliant");
            _logger.log(Level.SEVERE, 
                            "rardeployment.non_1.5_compliant_rar",jndiName);
            throw cre;
        }
    }

    public void deleteAdminObject(String jndiName) 
                           throws ConnectorRuntimeException 
    {

        try {
            InitialContext ic = new InitialContext();
            ic.unbind(jndiName);
        }
        catch(NamingException ne) {
            ResourcesUtil resutil = ResourcesUtil.createInstance();
            if(resutil.adminObjectBelongsToSystemRar(jndiName)) {
                return;
            }
            if(ne instanceof NameNotFoundException){
                _logger.log(Level.FINE,
                  "rardeployment.admin_object_delete_failure",jndiName);
                _logger.log(Level.FINE,"", ne);
                return;
            }
            ConnectorRuntimeException cre = new ConnectorRuntimeException(
                           "Failed to delete admin object from jndi");
            cre.initCause(ne);
            _logger.log(Level.SEVERE,
                  "rardeployment.admin_object_delete_failure",jndiName);
            _logger.log(Level.SEVERE,"", cre);
            throw cre; 
        }
    }
}
