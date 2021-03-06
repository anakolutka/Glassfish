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
 * FilterMapping.java
 *
 * Created on November 29, 2000, 2:59 PM
 */

package com.sun.enterprise.tools.verifier.tests.web;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.enterprise.deployment.ServletFilterDescriptor;
import com.sun.enterprise.deployment.ServletFilterMappingDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;

/**
 * Check that all the mappings for the declated filters are correct.
 * 
 * @author  Jerome Dochez
 * @version 1.0
 */
public class FilterMapping extends WebTest {
    Result result;
    ComponentNameConstructor compName;
    /**
     * Check that the mappings for all filters are correct
     * 
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {
        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();
        Enumeration filterEnum = descriptor.getServletFilterDescriptors().elements();
        if (filterEnum.hasMoreElements()) {
            while (filterEnum.hasMoreElements()) {
                ServletFilterDescriptor filter = (ServletFilterDescriptor) filterEnum.nextElement();
                hasValidMapping(descriptor, filter.getName());
            }
        }
        if (result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                            "All filter mappings are correct"));
        }
        return result;
    }

    private void hasValidMapping(WebBundleDescriptor descriptor, String filterName) {
        Enumeration filtermapperEnum = descriptor.getServletFilterMappingDescriptors().elements();
        if (filtermapperEnum.hasMoreElements()) {
            ServletFilterMappingDescriptor filterMapper = null;
            boolean mappingFound = false;
            do {
                filterMapper = (ServletFilterMappingDescriptor)filtermapperEnum.nextElement();
                String filterMapping = filterMapper.getName();
                mappingFound = filterName.equals(filterMapping);
            } while (!mappingFound && filtermapperEnum.hasMoreElements());

            if (mappingFound) {
                List<String> urlPatterns = filterMapper.getURLPatterns();
                for(String url : urlPatterns) {
                    if (!((url.startsWith("/")) ||
                            ((url.startsWith("/")) && (url.endsWith("/*"))) ||
                            (url.startsWith("*.")))) {
                        addErrorDetails(result, compName);
                        result.failed(smh.getLocalString
                                (getClass().getName() + ".failed",
                                "Filter Mapping for [ {0} ] has invalid " +
                                "url-mapping [ {1} ] ",
                                new Object[] {filterName, url} ));
                    }
                }
                List<String> servletsInFilter = filterMapper.getServletNames();
                List<String> servletsInWAR = new ArrayList<String>();

                if(servletsInFilter.size() > 0) {
                    Set servletDescriptor = descriptor.getServletDescriptors();
                    Iterator itr = servletDescriptor.iterator();
                    // test the servlets in this .war
                    while (itr.hasNext()) {
                        WebComponentDescriptor servlet = (WebComponentDescriptor) itr.next();
                        servletsInWAR.add(servlet.getCanonicalName());
                    }
                    if (!(servletsInWAR != null && servletsInWAR.containsAll(servletsInFilter))) {
                        addErrorDetails(result, compName);
                        result.failed(smh.getLocalString
                                (getClass().getName() + ".failed1",
                                "Filter Mapping for [ {0} ] has invalid servlet-name",
                                new Object[] {filterName}));
                    }
                }
            }
        }
    }
}
