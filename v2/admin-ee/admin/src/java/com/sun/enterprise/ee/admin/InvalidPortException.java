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

package com.sun.enterprise.ee.admin;

import java.util.ArrayList;
import com.sun.enterprise.util.i18n.StringManager;

/**
 * An exception containing a list of port conflicts with their resolution
 * (i.e. chosen replacements). 
 */
public class InvalidPortException extends PortInUseException
{            
    /**
     *
     * @param conflictingPorts The list of conflicting ports of type PortInUse.
     */    
    public InvalidPortException(ArrayList conflictingPorts) {  
        super();
        _inUse = conflictingPorts;
    }    
       
    /**
     * Formats a string of all the conflicting ports. A port will be "conflicting" for
     * one of two reasons: 1)The port is actually in use (i.e. someone's listening) OR
     * 2)The port is being used by another server instance with the same node agent 
     * in domain.xml
     * @return
     */    
    public String getMessage() {
        PortInUse portInUse;
        String result = "";
        final int numPorts = _inUse.size();
        for (int i = 0; i < numPorts; i++) {            
            portInUse = (PortInUse)_inUse.get(i);                              
            result += _strMgr.getString("invalidPort",
                new Object[] {portInUse.getPropertyName()});
            if (i < numPorts - 1) {
                result += "\n";
            }
        }
        return result;
    }
}
