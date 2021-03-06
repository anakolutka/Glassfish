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
 * Domain.java
 *
 * Created on March 17, 2004, 3:27 PM
 */

package com.sun.enterprise.tools.upgrade.transform.elements;

/**
 *
 * @author  prakash
 */
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;
import java.util.logging.*;

public class Domain extends BaseElement  {
    
    //start CR 6426419
    private String DEFAULT_APPLICATION_ROOT = "${com.sun.aas.instanceRoot}/applications";
    //end CR 6426419

    /** Creates a new instance of Domain */
    public Domain() {
    }
    /**
     * element - Domain of source
     * parentSource - Domain of source
     * parentResult - domain of target
     */
    public void transform(Element element, Element parentSource, Element parentResult){
        this.transferAttributes(element, parentResult, this.getNonTransferList(element));
        super.transform(element,  parentSource, parentResult);  
    }
    protected java.util.List getNonTransferList(Element element){
        java.util.Vector attrList = new java.util.Vector();

        //start CR 6426419
        //if(commonInfoModel.checkUpgradefrom8xpeto8xse() ||
          if(commonInfoModel.checkUpgrade8xto9x() || commonInfoModel.checkUpgrade9xto9x()) {
            String sourceApplRoot = (String)element.getAttribute("application-root");
            commonInfoModel.setSourceApplicationRoot(sourceApplRoot);
            if(!(sourceApplRoot.equals(DEFAULT_APPLICATION_ROOT))) {
                logger.log(Level.INFO, "New Application Root is " + sourceApplRoot);
            }
        } else {
            //7.x to 8.x scenario			    
            attrList.add("application-root");
        }
	//start CR 6396940
        //attrList.add("application-root");
	//end CR 6396940
        attrList.add("log-root");
        return attrList;
    }   
}
