/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package org.glassfish.concurrent.runtime;

import com.sun.enterprise.security.integration.AppServSecurityContext;
import com.sun.enterprise.util.Utility;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.enterprise.concurrent.spi.ContextHandle;
import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;

import javax.enterprise.concurrent.ContextService;
import java.util.Map;

public class ContextSetupProviderImpl implements ContextSetupProvider {

    private transient AppServSecurityContext securityContext;
    private transient InvocationManager invocationManager;

    static final long serialVersionUID = 1L;

    static enum CONTEXT_TYPE {CLASSLOADING, SECURITY, NAMING}

    private boolean classloading, security, naming;

    public ContextSetupProviderImpl(InvocationManager invocationManager, AppServSecurityContext securityContext, CONTEXT_TYPE... contextTypes) {
        this.invocationManager = invocationManager;
        this.securityContext = securityContext;
        for (CONTEXT_TYPE contextType: contextTypes) {
            switch(contextType) {
                case CLASSLOADING:
                    classloading = true;
                    break;
                case SECURITY:
                    security = true;
                    break;
                case NAMING:
                    naming = true;
                    break;
            }
        }
    }

    @Override
    public ContextHandle saveContext(ContextService contextService) {
        return saveContext(contextService, null);
    }

    @Override
    public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        // Capture the current thread context
        ClassLoader contextClassloader = null;
        AppServSecurityContext currentSecurityContext = null;
        ComponentInvocation savedInvocation = null;
        if (classloading) {
            contextClassloader = Utility.getClassLoader();
        }
        if (security) {
            currentSecurityContext = securityContext.getCurrentSecurityContext();
        }
        ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
        savedInvocation = currentInvocation.clone();
        savedInvocation.instance = currentInvocation.instance;
        if (!naming) {
            savedInvocation.setJNDIEnvironment(null);
        }

        return new InvocationContext(savedInvocation, contextClassloader, currentSecurityContext);
    }

    @Override
    public ContextHandle setup(ContextHandle contextHandle) throws IllegalStateException {
        if (! (contextHandle instanceof InvocationContext)) {
            // TODO: log a warning message saying that we got passed an unknown ContextHandle
            return null;
        }
        // TODO: check whether the application component submitting the task is still running. Throw IllegalStateException if not.
        InvocationContext handle = (InvocationContext) contextHandle;
        ClassLoader resetClassLoader = null;
        AppServSecurityContext resetSecurityContext = null;
        if (handle.getContextClassLoader() != null) {
            resetClassLoader = Utility.setContextClassLoader(handle.getContextClassLoader());
        }
        if (handle.getSecurityContext() != null) {
            resetSecurityContext = securityContext.getCurrentSecurityContext();
            securityContext.setCurrentSecurityContext(handle.getSecurityContext());
        }
        if (handle.getInvocation() != null) {
            invocationManager.preInvoke(handle.getInvocation());
        }
        return new InvocationContext(handle.getInvocation(), resetClassLoader, resetSecurityContext);
    }

    @Override
    public void reset(ContextHandle contextHandle) {
        if (! (contextHandle instanceof InvocationContext)) {
            // TODO: log a warning message saying that we got passed an unknown ContextHandle
            return;
        }
        InvocationContext handle = (InvocationContext) contextHandle;
        if (handle.getContextClassLoader() != null) {
            Utility.setContextClassLoader(handle.getContextClassLoader());
        }
        if (handle.getSecurityContext() != null) {
            securityContext.setCurrentSecurityContext(handle.getSecurityContext());
        }
        if (handle.getInvocation() != null) {
            invocationManager.postInvoke(((InvocationContext)contextHandle).getInvocation());
        }
    }

    private void readObject(java.io.ObjectInputStream in) {
        //TODO- re-initialize these fields
        securityContext = null;
        invocationManager = null;
    }
}

