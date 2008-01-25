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

package com.sun.enterprise.naming.impl;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;

import org.jvnet.hk2.component.Singleton;

import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.invocation.ComponentInvocation;

import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.api.naming.JNDIBinding;
import org.glassfish.api.naming.NamingObjectProxy;

import com.sun.enterprise.naming.util.LogFacade;

import javax.naming.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

/**
 * This is the manager that handles all naming operations including
 * publishObject as well as binding environment props, resource and ejb
 * references in the namespace.
 */

@Service
@Scoped(Singleton.class)
public final class  GlassfishNamingManagerImpl
        implements GlassfishNamingManager {

    static Logger _logger = LogFacade.getLogger();

    public static final String IIOPOBJECT_FACTORY =
            "com.sun.enterprise.naming.util.IIOPObjectFactory";

    @Inject
    InvocationManager invMgr;

    public static final String JAVA_COMP_STRING = "java:comp/env/";

    private InitialContext initialContext;
    private InitialContext cosContext;

    private NameParser nameParser = new SerialNameParser();

    private Hashtable namespaces;


    public GlassfishNamingManagerImpl() throws NamingException {
        this(new InitialContext());
    }

    //Used only for Junit Testing
    void setInvocationManager(InvocationManager invMgr) {
        this.invMgr = invMgr;
    }

    /**
     * Create the naming manager. Creates a new initial context.
     */
    public GlassfishNamingManagerImpl(InitialContext ic)
            throws NamingException {
        initialContext = ic;
        namespaces = new Hashtable();

        JavaURLContext.setNamingManager(this);
    }


    /**
     * Get the initial naming context.
     */
    public Context getInitialContext() {
        return initialContext;
    }


    public NameParser getNameParser() {
        return nameParser;
    }

    /**
     * Get cosContext which is the root of the COSNaming namespace. Setting
     * java.naming.corba.orb is necessary to prevent the COSNaming context from
     * creating its own ORB instance.
     */

    public void setCosContext(InitialContext cosCtx) {
        this.cosContext = cosCtx;
    }

    public InitialContext getCosContext() {
        return cosContext;
    }

    /**
     * Publish a name in the naming service.
     *
     * @param name   Name that the object is bound as.
     * @param obj    Object that needs to be bound.
     * @param rebind flag
     * @throws javax.naming.NamingException if there is a naming exception.
     */
    public void publishObject(String name, Object obj, boolean rebind)
            throws NamingException {
        Name nameobj = new CompositeName(name);
        publishObject(nameobj, obj, rebind);
    }

    /**
     * Publish a name in the naming service.
     *
     * @param name   Name that the object is bound as.
     * @param obj    Object that needs to be bound.
     * @param rebind flag
     * @throws javax.naming.NamingException if there is a naming exception.
     */
    public void publishObject(Name name, Object obj, boolean rebind)
            throws NamingException {

        Object serialObj = obj;

        if (isCOSNamingObj(obj)) {

            // Create any COS naming sub-contexts in name
            // that don't already exist.
            createSubContexts(name, getCosContext());

            if (rebind) {
                getCosContext().rebind(name, obj);
            } else {
                getCosContext().bind(name, obj);
            }

            // Bind a reference to it in the SerialContext using
            // the same name. This is needed to allow standalone clients
            // to lookup the object using the same JNDI name.
            // It is also used from bindObjects while populating ejb-refs in
            // the java:comp namespace.
            serialObj = new Reference("reference",
                    new StringRefAddr("url", name.toString()),
                    IIOPOBJECT_FACTORY, null);
        } // End if -- CORBA object

        if (rebind) {
            initialContext.rebind(name, serialObj);
        } else {
            initialContext.bind(name, serialObj);
        }
    }

    /**
     * Remove an object from the naming service.
     *
     * @param name Name that the object is bound as.
     * @throws Exception
     */
    public void unpublishObject(String name)
            throws NamingException {

        Object obj = null;
        try {
            initialContext.lookup(name);
        } catch (Exception ex) {
            _logger.log(Level.INFO, "**NMImpl::unpublishObject() " + ex);
        }

        if (isCOSNamingObj(obj)) {
            getCosContext().unbind(name);
        }

        initialContext.unbind(name);
    }


    /**
     * Remove an object from the naming service.
     *
     * @param name Name that the object is bound as.
     * @throws Exception
     */
    public void unpublishObject(Name name) throws NamingException {
        this.unpublishObject(name.toString());
    }

    /**
     * Create any sub-contexts in name that don't already exist.
     *
     * @param name    Name containing sub-contexts to create
     * @param rootCtx in which sub-contexts should be created
     * @throws Exception
     */
    private void createSubContexts(Name name, Context rootCtx) throws NamingException {

        int numSubContexts = name.size() - 1;
        Context currentCtx = rootCtx;
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Creating sub contexts for " + name);
        }

        for (int subCtxIndex = 0; subCtxIndex < numSubContexts; subCtxIndex++) {
            String subCtxName = name.get(subCtxIndex);
            try {

                Object obj = currentCtx.lookup(subCtxName);

                if (obj == null) {
                    // @@@ thought it should throw NameNotFound when
                    // context doesn't exist...
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "name == null");
                    }
                    // Doesn't exist so create it.
                    Context newCtx = currentCtx.createSubcontext(subCtxName);
                    currentCtx = newCtx;
                } else if (obj instanceof Context) {
                    // OK -- no need to create it.
                    currentCtx = (Context) obj;
                } else {
                    // Context name clashes with existing object.
                    throw new NameAlreadyBoundException(subCtxName);
                }
            }
            catch (NameNotFoundException e) {
                _logger.log(Level.FINE, "name not found", e);

                // Doesn't exist so create it.
                Context newCtx = currentCtx.createSubcontext(subCtxName);
                currentCtx = newCtx;
            }
        } // End for -- each sub-context

        return;
    }

    private HashMap getComponentNameSpace(String componentId)
            throws NamingException {
        // Note: HashMap is not synchronized. The namespace is populated
        // at deployment time by a single thread, and then on there are
        // no structural modifications (i.e. no keys added/removed).
        // So the namespace doesnt need to be synchronized.
        HashMap namespace = (HashMap) namespaces.get(componentId);
        if (namespace == null) {
            namespace = new HashMap();
            namespaces.put(componentId, namespace);

            // put entries for java:, java:comp and java:comp/env
            namespace.put("java:", new JavaURLContext("java:", null));
            namespace.put("java:comp", new JavaURLContext("java:comp", null));
            namespace.put("java:comp/env",
                    new JavaURLContext("java:comp/env", null));
        }

        return namespace;
    }

    /**
     * This method binds them in the component's java:comp
     * namespace.
     */
    public void bindToComponentNamespace(String appName,
                                         String componentId, String name, Object value)
            throws NamingException {
        HashMap namespace = getComponentNameSpace(componentId);

        String logicalJndiName = JAVA_COMP_STRING + name;

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,
                    "naming.bind Binding name:{0}",
                    new Object[]{logicalJndiName});
        }

        if (namespace.put(logicalJndiName, value) != null) {
            _logger.log(Level.WARNING,
                    "naming.alreadyexists" +
                            "Reference name [{0}] already exists in {1}",
                    new Object[]{name, appName});
        }

        bindIntermediateContexts(namespace, logicalJndiName);
    }

    /**
     * This method enumerates the env properties, ejb and resource references
     * etc for a J2EE component and binds them in the component's java:comp
     * namespace.
     */
    public void bindToComponentNamespace(String appName,
                                         String componentId, Collection<? extends JNDIBinding> bindings)
            throws NamingException {

        for (JNDIBinding binding : bindings) {
            bindToComponentNamespace(appName, componentId,
                    binding.getName(), binding.getValue());
        }
    }

    private void bindIntermediateContexts(HashMap namespace, String name)
            throws NamingException {
        // for each component of name, put an entry into namespace
        name = name.substring("java:comp/".length());
        StringTokenizer toks = new StringTokenizer(name, "/", false);
        String partialName = "java:comp";
        while (toks.hasMoreTokens()) {
            String tok = toks.nextToken();
            partialName = partialName + "/" + tok;
            if (namespace.get(partialName) == null) {

                namespace.put(partialName,
                        new JavaURLContext(partialName, null));
            }
        }
    }


    /**
     * This method enumerates the env properties, ejb and resource references
     * and unbinds them from the java:comp namespace.
     */
    public void unbindObjects(String componentId) throws NamingException {
        namespaces.remove(componentId); // remove local namespace cache
    }


    /**
     * Recreate a context for java:comp/env or one of its sub-contexts given the
     * context name.
     */
    public Context restoreJavaCompEnvContext(String contextName)
            throws NamingException {
        if (!contextName.startsWith("java:")) {
            throw new NamingException("Invalid context name [" + contextName
                    + "]. Name must start with java:");
        }

        return new JavaURLContext(contextName, null);
    }

    public Object lookup(String name) throws NamingException {
        return lookup(name, null);
    }

    /**
     * This method is called from SerialContext class. The serialContext
     * instance that was created by the appclient's Main class is passed so that
     * stickiness is preserved. Called from javaURLContext.lookup, for java:comp
     * names.
     */
    public Object lookup(String name, SerialContext serialContext)
            throws NamingException {
        _logger.fine("serialcontext in GlassfishNamingManager.." + serialContext);
        Context ic = null;

        if (serialContext != null) {
            ic = serialContext;
        } else {
            ic = initialContext;
        }

        // initialContext is used as ic in case of PE while
        // serialContext is used as ic in case of EE/SE
        if (_logger.isLoggable(Level.FINE))
            _logger.log(Level.FINE, "GlassfishNamingManager : looking up name : "
                    + name);

        // Get the component id and namespace to lookup
        String componentId = getComponentId();

        HashMap namespace = (HashMap) namespaces.get(componentId);

        Object obj = namespace.get(name);

        if (obj == null)
            throw new NameNotFoundException("No object bound to name " + name);

        if (obj instanceof NamingObjectProxy) {
            NamingObjectProxy namingProxy = (NamingObjectProxy) obj;
            obj = namingProxy.create(ic);
        }

        return obj;
    }

    public NamingEnumeration list(String name) throws NamingException {
        ArrayList list = listNames(name);
        return new NamePairsEnum(this, list.iterator());
    }

    public NamingEnumeration listBindings(String name) throws NamingException {
        ArrayList list = listNames(name);
        return new BindingsIterator(this, list.iterator());
    }

    private ArrayList listNames(String name) throws NamingException {
        // Get the component id and namespace to lookup
        String componentId = getComponentId();
        HashMap namespace = (HashMap) namespaces.get(componentId);

        Object obj = namespace.get(name);

        if (obj == null)
            throw new NameNotFoundException("No object bound to name " + name);

        if (!(obj instanceof JavaURLContext))
            throw new NotContextException(name + " cannot be listed");

        // This iterates over all names in entire component namespace,
        // so its a little inefficient. The alternative is to store
        // a list of bindings in each javaURLContext instance.
        ArrayList list = new ArrayList();
        Iterator itr = namespace.keySet().iterator();
        if (!name.endsWith("/"))
            name = name + "/";
        while (itr.hasNext()) {
            String key = (String) itr.next();
            // Check if key begins with name and has only 1 component extra
            // (i.e. no more slashes)
            if (key.startsWith(name) && key.indexOf('/', name.length()) == -1)
                list.add(key);
        }
        return list;
    }

    /**
     * Get the component id from the Invocation Manager.
     *
     * @return the component id as a string.
     */
    private String getComponentId() throws NamingException {
        String id = null;

        ComponentInvocation ci = invMgr.getCurrentInvocation();
        if (ci == null) {
            throw new NamingException("Invocation exception: Got null ComponentInvocation ");
        }

        try {
            id = ci.getComponentId();
            if (id == null) {
                NamingException nameEx = new NamingException(
                        "Invocation exception: ComponentId is null");
                throw nameEx;
            }
        } catch (Throwable th) {
            NamingException ine = new NamingException("Invocation exception: " + th);
            ine.initCause(th);
            throw ine;
        }

        return id;
    }

    private boolean isCOSNamingObj(Object obj) {
        return ((obj instanceof java.rmi.Remote) || (obj instanceof org.omg.CORBA.Object));
    }

}

// Class for enumerating bindings
class BindingsIterator implements NamingEnumeration {
    GlassfishNamingManagerImpl nm;

    Iterator names;

    BindingsIterator(GlassfishNamingManagerImpl nm, Iterator names) {
        this.nm = nm;
        this.names = names;
    }

    public boolean hasMoreElements() {
        return names.hasNext();
    }

    public boolean hasMore() throws NamingException {
        return hasMoreElements();
    }

    public Object nextElement() {
        if (names.hasNext()) {
            try {
                String name = (String) names.next();
                return new Binding(name, nm.lookup(name));
            } catch (Exception ex) {
                throw new RuntimeException("Exception during lookup: " + ex);
            }
        } else
            return null;
    }

    public Object next() throws NamingException {
        return nextElement();
    }

    // New API for JNDI 1.2
    public void close() throws NamingException {
        throw new OperationNotSupportedException("close() not implemented");
    }
}
