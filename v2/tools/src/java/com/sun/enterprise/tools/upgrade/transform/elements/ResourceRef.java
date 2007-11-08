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
 * ApplicationRef.java
 *
 * Created on April 12, 2004, 3:37 PM
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

public class ResourceRef extends GenericElement {
    
    /** Creates a new instance of ApplicationRef */
    public ResourceRef() {
    }
    /**
     * element - resource-ref
     * parentSource - server or cluster
     * parentResult - server or cluster
     */
    public void transform(Element element, Element parentSource, Element parentResult){
        if(parentSource.getTagName().equals("cluster")){
            super.transform(element,parentSource,parentResult);
        }else{
            // If parent is server instead of cluster, the GenericResource adds resource ref to target server for AS7.x source
            if(super.commonInfoModel.getSourceVersion().equals(com.sun.enterprise.tools.upgrade.common.UpgradeConstants.VERSION_7X))
                return;
            //Added for CR 6363168
            if(element.getAttribute("ref").equals("jdbc/PointBase"))
                return;
            super.transform(element,parentSource,parentResult);
        }
    }
    protected java.util.List getInsertElementStructure(Element element, Element parentEle){
        // resource-ref is an element in cluster and server.  In cluster there are 3 elements after this.  
        // In server there are only two after this.
        java.util.List insertStrucure = com.sun.enterprise.tools.upgrade.transform.ElementToObjectMapper.getMapper().getInsertElementStructure(element.getTagName());
        String parentName = parentEle.getTagName();
        if(parentName != null){
            if(parentName.equals("cluster")){
                insertStrucure.add(0,"application-ref");
            }
        }
        return insertStrucure;
    }
    
}
