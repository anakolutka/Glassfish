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


package com.sun.enterprise.tools.verifier.tests;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.apiscan.classfile.ClosureCompilerImpl;
import com.sun.enterprise.deployment.Descriptor;

import java.util.Collection;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class ClassContainsNativeMethod
        extends VerifierTest implements VerifierCheck {
    public Result check(Descriptor descriptor) {
        ComponentNameConstructor compName =
                getVerifierContext().getComponentNameConstructor();
        Result result = getInitializedResult();
        result.setStatus(Result.PASSED);
        ClosureCompilerImpl cc = ClosureCompilerImpl.class.cast(
                getVerifierContext().getClosureCompiler());
        Collection<String> nativeMethods = cc.getNativeMethods();
        if(!nativeMethods.isEmpty()) {
            addWarningDetails(result, compName);
            result.warning(smh.getLocalString(getClass().getName() + ".warning",
                    "Supplied below is the list of method names " +
                    "(in the format <package.classname>.<methodName>) " +
                    "that are defined as native methods and used by the application:\n"));
            for(String m : nativeMethods) {
                result.warning("\n\t" + m);
            }
            result.warning(smh.getLocalString(getClass().getName() + ".suggestion",
                    "Please make sure that they are implemented on all operating systems."));
        }
        return result;
    }
}
