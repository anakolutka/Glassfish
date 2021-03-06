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
package com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2;

import java.lang.reflect.Method;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import java.lang.ClassLoader;
import com.sun.enterprise.tools.verifier.tests.*;

/**
 * Select methods must not be exposed through the bean home or remote interface
 *
 * @author  Jerome Dochez
 * @version 
 */
public class SelectMethodExposition extends SelectMethodTest {

     /**
     * <p>
     * run an individual test against a declared ejbSelect method
     * </p>
     * 
     * @param m is the ejbSelect method
     * @param descriptor is the entity declaring the ejbSelect
     * @param result is where to put the result
     * 
     * @return true if the test passes
     */
    ComponentNameConstructor compName = null;

    protected boolean runIndividualSelectTest(Method m, EjbCMPEntityDescriptor descriptor, Result result) {
        boolean allIsWell = true;
	compName = getVerifierContext().getComponentNameConstructor();
	//  String methodReturnType = m.getReturnType().getName();
    	if(descriptor.getRemoteClassName() != null && !"".equals(descriptor.getRemoteClassName()) &&
	   descriptor.getHomeClassName() != null && !"".equals(descriptor.getHomeClassName()))
	    allIsWell =  commonToBothInterfaces(descriptor.getHomeClassName(),descriptor.getRemoteClassName(),descriptor, result, m);
	if(allIsWell == true) {
	    if(descriptor.getLocalClassName() != null && !"".equals(descriptor.getLocalClassName()) &&
	       descriptor.getLocalHomeClassName() != null && !"".equals(descriptor.getLocalHomeClassName()))
		allIsWell =  commonToBothInterfaces(descriptor.getLocalHomeClassName(),descriptor.getLocalClassName(),descriptor, result, m);
	}
	return allIsWell;
    }  

 /** 
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @param ejbHome for the Home interface of the Ejb. 
     * @param result Result of the test
     * @param remote Remote/Local interface
     * @param m Method
     * @return boolean the results for this assertion i.e if a test has failed or not
     */

    private boolean commonToBothInterfaces(String home,String remote,EjbDescriptor descriptor, Result result, Method m) {
	try {
            // we must not find this method exposed in the home or remote interface
	    Context context = getVerifierContext();
	    ClassLoader jcl = context.getClassLoader();
            Method m1 = getMethod(Class.forName(home, false,
                                 getVerifierContext().getClassLoader()),m.getName(), m.getParameterTypes());
            Method m2 = getMethod(Class.forName(remote, false,
                                 getVerifierContext().getClassLoader()), m.getName(), m.getParameterTypes());
            if (m1 == null && m2 == null) {
		result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.addGoodDetails(smh.getLocalString
			  ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.SelectMethodExposition.passed",
			   "[ {0} ] is not declared in the home or remote interface",
			   new Object[] {m.getName()}));
                return true;
            } else {
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.addErrorDetails(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.SelectMethodExposition.failed",
                    "Error : [ {0} ] is declared in the home or remote interface",
                    new Object[] {m.getName()}));
                return false;
            }
        } catch (ClassNotFoundException e) {
	    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.addErrorDetails(smh.getLocalString
		     ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.SelectMethodExposition.failedException",
		      "Error: home or remote interface not found.",
		      new Object[] {}));
            Verifier.debug(e);
            return false;
        }
    }         
}
