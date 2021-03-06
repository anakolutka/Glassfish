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

package com.sun.enterprise.config.serverbeans.validation;

import java.util.Vector;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.sun.enterprise.config.serverbeans.validation.tests.StaticTest;

/**
   Class which contains Meta data for all types of attributes which is present in Validation Descriptor
   *  XML File
   *
   *  Sample
   *      <attribute name=<Name> type="address" />
   *      <attribute name=<Name> type="integer" range="low,high" />
   *      <attribute name=<Name> type="string"  max-length="length" />
    
   @author Srinivas Krishnan
   @version 2.0
*/

/* Class for attribute type address (IP Address) */

public class AttrAddress extends AttrType {
    
    public AttrAddress(String name, String type, boolean optional) {
        super(name,type, optional);
    }
    
    public void validate(Object o, ValidationContext valCtx) {
        super.validate(o, valCtx); // call to common validator first
        String address = null;
        if(o == null)
            return;
        if(o.equals("")) {
            valCtx.result.failed(valCtx.smh.getLocalString(getClass().getName() + ".invalidNullStrAddress",
                                                             "Attribute({0}=null) :  Null address not permitted", new Object[]{valCtx.attrName}));
            return;
        }
        address = (String)o;
        try {
            StaticTest.checkIPAddress(address);
        } catch(IllegalArgumentException e) {
            valCtx.result.failed(valCtx.smh.getLocalString(getClass().getName() + ".addressNotAscii",
                                                             "Attribute({0}={1}) :  Invalid address syntax - {1}",
                                                             new Object[]{valCtx.attrName,address}));
        } catch(Exception u) {
            valCtx.result.failed(valCtx.smh.getLocalString(getClass().getName() + ".invalidAddress",
                                                             "Attribute({0}={1}) :  Invalid address syntax - {1}",
                                                             new Object[]{valCtx.attrName,address}));
        }
    }
    
}
