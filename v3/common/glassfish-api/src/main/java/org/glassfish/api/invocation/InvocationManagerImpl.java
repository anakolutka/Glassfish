/*
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.api.invocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Singleton;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.Habitat;

@Service
@Scoped(Singleton.class)
public class InvocationManagerImpl
        implements InvocationManager {

    static public boolean debug;

    // This TLS variable stores an ArrayList. 
    // The ArrayList contains ComponentInvocation objects which represent
    // the stack of invocations on this thread. Accesses to the ArrayList
    // dont need to be synchronized because each thread has its own ArrayList.
    private InheritableThreadLocal<InvocationArray<ComponentInvocation>> frames;

    @Inject
    Habitat habitat;

    public InvocationManagerImpl() {

        frames = new InheritableThreadLocal<InvocationArray<ComponentInvocation>>() {
            protected InvocationArray initialValue() {
                return new InvocationArray();
            }

            // if this is a thread created by user in servlet's service method
            // create a new ComponentInvocation with transaction
            // set to null and instance set to null
            // so that the resource won't be enlisted or registered
            protected InvocationArray<ComponentInvocation> childValue(InvocationArray<ComponentInvocation> parentValue) {
                // always creates a new ArrayList
                InvocationArray<ComponentInvocation> result = new InvocationArray<ComponentInvocation>();
                InvocationArray<ComponentInvocation> v = parentValue;
                if (v.size() > 0 && v.outsideStartup()) {
                    // get current invocation
                    ComponentInvocation parentInv = v.get(v.size() - 1);
                    /*
                    TODO: The following is ugly. The logic of what needs to be in the
                      new ComponentInvocation should be with the respective container
                    */
                    if (parentInv.getInvocationType() == ComponentInvocationType.SERVLET_INVOCATION) {

                        ComponentInvocation inv = new ComponentInvocation();
                        inv.componentId = parentInv.getComponentId();
                        inv.setComponentInvocationType(parentInv.getInvocationType());
                        inv.instance = null;
                        inv.container = parentInv.getContainerContext();
                        inv.transaction = null;
                        result.add(inv);
                    } else if (parentInv.getInvocationType() != ComponentInvocationType.EJB_INVOCATION) {
                        // Push a copy of invocation onto the new result
                        // ArrayList
                        ComponentInvocation cpy = new ComponentInvocation();
                        cpy.componentId = parentInv.getComponentId();
                        cpy.setComponentInvocationType(parentInv.getInvocationType());
                        cpy.instance = parentInv.getInstance();
                        cpy.container = parentInv.getContainerContext();
                        cpy.transaction = parentInv.getTransaction();
                        result.add(cpy);
                    }

                }
                return result;
            }
        };
    }

    public <T extends ComponentInvocation> void preInvoke(T inv)
            throws InvocationException {

        InvocationArray<ComponentInvocation> v = frames.get();
        if (inv.getInvocationType() == ComponentInvocationType.SERVICE_STARTUP) {
            v.setInvocationAttribute(ComponentInvocationType.SERVICE_STARTUP);
            return;
        }

        int beforeSize = v.size();
        ComponentInvocation prevInv = beforeSize > 0 ? v.get(beforeSize - 1) : null;

        // if ejb call EJBSecurityManager, for servlet call RealmAdapter
        ComponentInvocationType invType = inv.getInvocationType();

        // dochez : we use habitat lookup at each call, if this proves to be a bottleneck (don't think so,
        // since it's just one hashMap lookup, we could consider caching this.
        Collection<ComponentInvocationHandler> handlers = null;
        if (habitat!=null) {
            handlers = habitat.getAllByContract(ComponentInvocationHandler.class);
        }
        if (handlers!=null) {
            for (ComponentInvocationHandler handler : handlers) {
                handler.beforePreInvoke(invType, prevInv, inv);
            }
        }

        //push this invocation on the stack
        v.add(inv);

        if (handlers!=null) {
            for (ComponentInvocationHandler handler : handlers) {
                handler.afterPreInvoke(invType, prevInv, inv);
            }
        }

    }

    public <T extends ComponentInvocation> void postInvoke(T inv)
            throws InvocationException {

        // Get this thread's ArrayList
        InvocationArray<ComponentInvocation> v = frames.get();
        if (inv.getInvocationType() == ComponentInvocationType.SERVICE_STARTUP) {
            v.setInvocationAttribute(ComponentInvocationType.UN_INITIALIZED);
            return;
        }

        int beforeSize = v.size();
        if (beforeSize == 0) {
            throw new InvocationException();
        }

        ComponentInvocation prevInv = beforeSize > 1 ? v.get(beforeSize - 2) : null;
        ComponentInvocation curInv = v.get(beforeSize - 1);

        // same lazy look up, room for optimization
        Collection<ComponentInvocationHandler> handlers = null;
        if (habitat!=null) {
            handlers = habitat.getAllByContract(ComponentInvocationHandler.class);
        }

        try {
            ComponentInvocationType invType = inv.getInvocationType();

            if (handlers!=null) {
                for (ComponentInvocationHandler handler : handlers) {
                    handler.beforePostInvoke(invType, prevInv, curInv);
                }
            }

        } finally {
            // pop the stack
            v.remove(beforeSize - 1);

            if (handlers!=null) {
                for (ComponentInvocationHandler handler : habitat.getAllByContract(ComponentInvocationHandler.class)) {
                    handler.afterPostInvoke(inv.getInvocationType(), prevInv, inv);
                }
            }
        }

    }

    /**
     * return true iff no invocations on the stack for this thread
     */
    public boolean isInvocationStackEmpty() {
        ArrayList v = frames.get();
        return ((v == null) || (v.size() == 0));
    }

    /**
     * return the Invocation object of the component
     * being called
     */
    public <T extends ComponentInvocation> T getCurrentInvocation() {
        ArrayList v = (ArrayList) frames.get();
        int size = v.size();
        if (size == 0) {
            return null;
        }
        return (T) v.get(size - 1);
    }

    /**
     * return the Inovcation object of the caller
     * return null if none exist (e.g. caller is from
     * another VM)
     */
    public <T extends ComponentInvocation> T getPreviousInvocation()
            throws InvocationException {

        ArrayList v = frames.get();
        int i = v.size();
        if (i < 2) return null;
        return (T) v.get(i - 2);
    }

    public List getAllInvocations() {
        return frames.get();
    }

    class InvocationArray<T extends ComponentInvocation> extends java.util.ArrayList<T> {
        private ComponentInvocationType invocationAttribute;

        public void setInvocationAttribute(ComponentInvocationType attribute) {
            this.invocationAttribute = attribute;
        }

        public ComponentInvocationType getInvocationAttribute() {
            return invocationAttribute;
        }

        public boolean outsideStartup() {
            return getInvocationAttribute()
                    != ComponentInvocationType.SERVICE_STARTUP;
        }
    }
}






