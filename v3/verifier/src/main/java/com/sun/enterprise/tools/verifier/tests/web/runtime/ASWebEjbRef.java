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

package com.sun.enterprise.tools.verifier.tests.web.runtime;


import com.sun.enterprise.tools.verifier.tests.web.WebTest;
import com.sun.enterprise.tools.verifier.tests.web.WebCheck;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.deployment.runtime.common.EjbRef;
import com.sun.enterprise.deployment.WebBundleDescriptor;

//<addition author="irfan@sun.com" [bug/rfe]-id="4711198" >
/* Changed the result messages to reflect consistency between the result messages generated 
 * for the EJB test cases for SunONE specific deployment descriptors*/
//</addition>

public class ASWebEjbRef extends WebTest implements WebCheck {

    public Result check(WebBundleDescriptor descriptor) {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean oneFailed = false;
        String refName;
        EjbRef[] ejbRefs = (descriptor.getSunDescriptor()).getEjbRef();
        if (ejbRefs!=null && ejbRefs.length > 0) {
            for (int rep=0; rep<ejbRefs.length; rep++ ) {
                refName = ejbRefs[rep].getEjbRefName();
                if (validEjbRefName(refName,descriptor)) {
                    addGoodDetails(result, compName);
                    result.passed(smh.getLocalString
                            (getClass().getName() + ".passed",
                                    "PASSED [AS-WEB ejb-ref] ejb-ref-name [ {0} ] properly defined in the war file.",
                                    new Object[] {refName}));
                } else {
                    oneFailed = true;
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString
                            (getClass().getName() + ".failed",
                                    "FAILED [AS-WEB ejb-ref] ejb-ref-name [ {0} ] is not valid, either empty or not defined in web.xml.",
                                    new Object[] {refName}));
                }
            }
        } else {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    (getClass().getName() + ".notApplicable",
                            "NOT APPLICABLE [AS-WEB sun-web-app] ejb-ref element(s) not defined in the web archive [ {0} ].",
                            new Object[] {descriptor.getName()}));
            return result;
        }
        if (oneFailed)
        {
            result.setStatus(Result.FAILED);
        } else {
            addGoodDetails(result, compName);
            result.passed
                    (smh.getLocalString
                    (getClass().getName() + ".passed2",
                            "PASSED [AS-WEB sun-web-app] ejb-ref element(s) defined are valid within the web archive [ {0} ].",
                            new Object[] {descriptor.getName()} ));
        }
        return result;
    }

    boolean validEjbRefName(String name,WebBundleDescriptor descriptor){
        boolean valid =true;
        if(name !=null && name.length()!=0) {
            try{
                descriptor.getEjbReferenceByName(name);
            }
            catch(IllegalArgumentException e){
                valid=false;
            }
        }else{
            valid=false;
        }
        return valid;
    }
}
