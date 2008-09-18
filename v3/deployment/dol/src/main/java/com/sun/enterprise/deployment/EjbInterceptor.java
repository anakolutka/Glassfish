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

package com.sun.enterprise.deployment;

import java.util.logging.*;
import com.sun.logging.*;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.deployment.util.LogDomains;
import org.glassfish.deployment.common.DeploymentUtils;
import static com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;

/**
 * Contains information about 1 ejb interceptor.
 */ 

public class EjbInterceptor extends JndiEnvironmentRefsGroupDescriptor
{
    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(EjbInterceptor.class);

    private static final Logger _logger = LogDomains.getLogger(DeploymentUtils.class, LogDomains.DPL_LOGGER);

    private Set<LifecycleCallbackDescriptor> aroundInvokeDescriptors;
    private String interceptorClassName;

    // true if the AroundInvoke/Callback methods for this 
    // descriptor were defined on the bean class itself (or one of its
    // super-classes).  false if the methods are defined
    // on a separate interceptor class (or one of its super-classes).  
    private boolean fromBeanClass = false;

    public String getInterceptorClassName() {
        return interceptorClassName;
    }

    public void setInterceptorClassName(String className) {
        interceptorClassName = className;
    }

    public Set<LifecycleCallbackDescriptor> getAroundInvokeDescriptors() {
        if (aroundInvokeDescriptors == null) {
            aroundInvokeDescriptors =
                new HashSet<LifecycleCallbackDescriptor>(); 
        }
        return aroundInvokeDescriptors;
    }

    /**
     * Some clients need the AroundInvoke methods for this inheritance
     * hierarchy in the spec-defined "least derived --> most derived" order.
     */
    public List<LifecycleCallbackDescriptor> getOrderedAroundInvokeDescriptors
        (ClassLoader loader) throws Exception {

        return orderDescriptors(getAroundInvokeDescriptors(), loader);

    }

    public void setFromBeanClass(boolean flag) {
        fromBeanClass = flag;
    }

    public boolean getFromBeanClass() {
        return fromBeanClass;
    }

    public void addAroundInvokeDescriptor(LifecycleCallbackDescriptor aroundInvokeDesc) {
        Set<LifecycleCallbackDescriptor> aroundInvokeDescs =
            getAroundInvokeDescriptors();
        boolean found = false;       
        for (LifecycleCallbackDescriptor ai : aroundInvokeDescs) {
            if ((aroundInvokeDesc.getLifecycleCallbackClass() != null) &&
                aroundInvokeDesc.getLifecycleCallbackClass().equals(
                    ai.getLifecycleCallbackClass())) {
                found = true;
            }
        }

        if (!found) {
            aroundInvokeDescs.add(aroundInvokeDesc);
        }
    }

    public void addAroundInvokeDescriptors(
        Set<LifecycleCallbackDescriptor> aroundInvokes) {
        for (LifecycleCallbackDescriptor ai : aroundInvokes) {
            addAroundInvokeDescriptor(ai);
        }
    }

    public boolean hasAroundInvokeDescriptor() {
        return (getAroundInvokeDescriptors().size() > 0);
    }

    /**
     * Some clients need the Callback methods for this inheritance
     * hierarchy in the spec-defined "least derived --> most derived" order.
     */
    public List<LifecycleCallbackDescriptor> getOrderedCallbackDescriptors
        (CallbackType type, ClassLoader loader) throws Exception {

        return orderDescriptors(getCallbackDescriptors(type), loader);
    }

    public void addPostActivateDescriptor(LifecycleCallbackDescriptor lcDesc) {
        addCallbackDescriptor(CallbackType.POST_ACTIVATE, lcDesc);
    }

    public void addPrePassivateDescriptor(LifecycleCallbackDescriptor lcDesc) {
        addCallbackDescriptor(CallbackType.PRE_PASSIVATE, lcDesc);
    }

    /**
     * Order a set of lifecycle method descriptors for a particular
     * inheritance hierarchy with highest precedence assigned to the
     * least derived class. 
     */
    private List<LifecycleCallbackDescriptor> orderDescriptors
        (Set<LifecycleCallbackDescriptor> lcds, ClassLoader loader) 
        throws Exception 
    {

        LinkedList<LifecycleCallbackDescriptor> orderedDescs =
            new LinkedList<LifecycleCallbackDescriptor>();

        Map<String, LifecycleCallbackDescriptor> map =
            new HashMap<String, LifecycleCallbackDescriptor>();

        for(LifecycleCallbackDescriptor next : lcds) {
            map.put(next.getLifecycleCallbackClass(), next);
        }

        Class nextClass = loader.loadClass(getInterceptorClassName());

        while((nextClass != Object.class) && (nextClass != null)) {
            String nextClassName = nextClass.getName();
            if( map.containsKey(nextClassName) ) {
                LifecycleCallbackDescriptor lcd = map.get(nextClassName);
                orderedDescs.addFirst(lcd);
            }

            nextClass = nextClass.getSuperclass();
        }


        return orderedDescs;

    }

    public String toString() {
        return "EjbInterceptor class = " + getInterceptorClassName();
    }
}
