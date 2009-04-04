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

package com.sun.enterprise.connectors.work.context;

import com.sun.enterprise.connectors.work.WorkCoordinator;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import org.glassfish.security.common.PrincipalImpl;
import org.glassfish.security.common.Group;
import org.jvnet.hk2.annotations.Service;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;

import javax.resource.spi.work.*;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.util.*;



/**
 * Handles work contexts submitted as part of the work instance
 *
 * @author Jagadish Ramu
 * @since GlassFish v3
 */
@Service
public class WorkContextHandler implements com.sun.appserv.connectors.internal.api.WorkContextHandler {

    //TODO V3 logstrings for entire class
    private static final List<Class<? extends WorkContext>> containerSupportedContexts =
            new ArrayList<Class<? extends WorkContext>>();


    static {
        containerSupportedContexts.add(TransactionContext.class);
        containerSupportedContexts.add(SecurityContext.class);
        containerSupportedContexts.add(HintsContext.class);

        containerSupportedContexts.add(CustomWorkContext_A.class);
        containerSupportedContexts.add(CustomWorkContext_B.class);
        containerSupportedContexts.add(CustomWorkContext_D.class);
    }

    private ConnectorRuntime runtime = null;

    public WorkContextHandler(){
    }
    
    public WorkContextHandler(ConnectorRuntime runtime) {
        this.runtime = runtime;
    }

    private Set<WorkContext> validContexts = new HashSet<WorkContext>();

    /**
     * indicates whether the provided workContextClass is supported by the container
     *
     * @param strict                 indicates whether the type-check need to be strict or not i.e.,
     *                               exact type or its super-class type
     * @param workContextClassName work context class name
     * @return boolean indicating whether the workContextClass is supported or not
     */
    public boolean isContextSupported(boolean strict, String workContextClassName) {
        boolean result = false;
        if (strict) {
            result = canContainerHandleSameContextType(workContextClassName);
        } else {
            result = canContainerHandleContext(workContextClassName);
        }
        return result;
    }

    /**
     * checks whether the container can handle the exact context type provided
     *
     * @param workContextClassName work context class name
     * @return boolean indicating whether the workContextClass is supported or not
     */
    private static boolean canContainerHandleSameContextType(String workContextClassName) {
        boolean result = false;
        for (Class workContextClass : containerSupportedContexts) {
            //TODO JSR-322-WORK-CONTEXT : Still need to do class.equals () ??
            Class clz = null;
            try {
                clz =  loadClass(workContextClassName);

            } catch (ClassNotFoundException cnfe) {
                debug(cnfe.toString());
                cnfe.printStackTrace();  //TODO V3 log & remove pST
                break;
            }
            if (workContextClass.equals(clz)) {
                result = true;
                debug("Container can handle the context [Strict] : " + workContextClassName);
                break;
            }
        }
        return result;
    }

    /**
     * checks whether the container can handle the contextClass in question.
     * If the exact class type is not supported, container will look for any of its super-class
     * If any of the super-class is available, it is indicated as 'can handle'
     *
     * @param contextClassName work context class name
     * @return boolean indicating whether the contextClass is supported or not
     */
    public static boolean canContainerHandleContext(String contextClassName) {
        boolean result = false;

        //JSR-322-WORK-CONTEXT First check whether exact 'context-type' is supported.
        if (!canContainerHandleSameContextType(contextClassName)) {

            Class context = null;
            try {
                context = loadClass(contextClassName);

            } catch (ClassNotFoundException e) {
                debug("Container cannot load the context class [isAssignable] : " + contextClassName + " ");
            }
            //TODO JSR-322-WORK-CONTEXT : can we use workContext.getName() ??
            for (Class workContextClass : containerSupportedContexts) {
                if (workContextClass.isAssignableFrom(context)) {
                    result = true;
                    debug("Container can handle the context [isAssignable] : " + contextClassName);
                    break;
                }
            }
        } else {
            result = true;
        }
        return result;
    }

    private static Class loadClass(String contextClassName) throws ClassNotFoundException {
        //context = ConnectorClassLoader.getInstance().loadClass(contextClassName);
        //TODO V3 not a clean way ?
        //return WorkContextHandler.class.getClassLoader().loadClass(contextClassName);
        return Thread.currentThread().getContextClassLoader().loadClass(contextClassName);
    }

    /**
     * validate the submitted work
     *
     * @param work work instance to be validated
     * @param ec   ExecutionContext
     * @throws WorkCompletedException when a submitted context is not supported
     * @throws WorkRejectedException  when validation fails
     */
    public void validateWork(Work work, ExecutionContext ec) throws WorkCompletedException, WorkRejectedException {
        //JSR-322-WORK-CONTEXT-REQ If work instance is a Work Context provider, handle them
        if (work instanceof WorkContextProvider) {

            //JSR-322-WORK-CONTEXT-REQ
            //TODO V3 hack - ec & getEC() test
            if (ec != null && getExecutionContext(work) != ec) {
                String errorMsg = "Work is an WorkContextProvider " +
                        "and ExecutionContext [ " + ec + " ] is not null";
                debug(errorMsg);
                throw new WorkRejectedException(errorMsg);
            }

            WorkContextProvider icp = (WorkContextProvider) work;

            List<WorkContext> contexts = icp.getWorkContexts();
            for (WorkContext ic : contexts) {

                WorkContextLifecycleListener listener = getListener(ic);

                //JSR-322-WORK-CONTEXT-REQ strict=false in the method below as the check has to be lenient.
                if (isContextSupported(false, ic.getClass().getName())) {
                    if (isUniqueSubmission(ic, validContexts)) {
                        validContexts.add(ic);
                    } else {
                        //JSR-322-WORK-CONTEXT-REQ If a particular IC type is submitted twice,
                        // container does not support it, fail work submission.
                        WorkCompletedException wce = new WorkCompletedException(
                                "duplicate work context for type : " + ic.getClass().getName());
                        wce.setErrorCode(WorkContextErrorCodes.DUPLICATE_CONTEXTS);
                        debug(wce.getMessage());
                        notifyContextSetupFailure(listener, WorkContextErrorCodes.DUPLICATE_CONTEXTS);
                        throw wce;
                    }
                } else {
                    //JSR-322-WORK-CONTEXT-REQ   unable to handle the work context or its generic type
                    // (any of its super types) container does not support it, fail work submission.
                    WorkCompletedException wce = new WorkCompletedException(
                            "Connector runtime cannot handle the following work context : "
                                    + ic.getClass().getName());
                    wce.setErrorCode(WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE);
                    debug(wce.getMessage());
                    notifyContextSetupFailure(listener, WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE);
                    throw wce;
                }
            }
        }
    }

    /**
     * check whether the work-context is a unique submission
     *
     * @param ic                work-context
     * @param supportedContexts supported contexts
     * @return boolean indicating whether the work-context submitted is unique
     */
    //TODO V3 - rework - check whether multiple implementations of same IC supported by the container is submitted.
    private boolean isUniqueSubmission(WorkContext ic, Collection<WorkContext> supportedContexts) {
        //TODO JSR-322-WORK-CONTEXT : can we use workContext.getName() ??
        for (WorkContext workContext : supportedContexts) {
            String workContextName = workContext.getClass().getName().toLowerCase();
            String icName = ic.getClass().getName().toLowerCase();
            if (workContextName.equalsIgnoreCase(icName)) {
                debug("Not a unique workContext submission : " + workContext.getClass().getName());
                return false;
            } else {
                //System.out.println(workContextName + " EQUALS " + icName);
            }
        }
        //return !(validContexts.contains(ic.getClass().getName().toLowerCase()));
        return true;
    }

    /**
     * setup the work context or execution context for the work
     *
     * @param ec ExecutionContext
     * @param wc Work coordinator
     */
    public void setupContext(ExecutionContext ec, WorkCoordinator wc) {
        boolean useExecutionContext = true;
        for (WorkContext ic : validContexts) {
            WorkContextLifecycleListener listener = getListener(ic);
            //TODO V3 Command Pattern ?
            if (ic instanceof TransactionContext) {
                useExecutionContext = false;
                setupTransactionWorkContext((TransactionContext) ic, listener);
            } else if (ic instanceof SecurityContext) {
                setupSecurityWorkContext((SecurityContext) ic, listener, wc.getRAName());
            } else if (ic instanceof HintsContext) {
                //TODO V3 handle hints context ?
            } else {
                Class<? extends WorkContext> claz = null;
                String className = ic.getClass().getName();
                if (!isContextSupported(true, className)) {
                    claz = getMostSpecificWorkContextSupported(ic);
                    if (claz == null) {
                        debug("Cannot handle work context [ " + className + " ]");
                        continue; //TODO V3 continue ?
                    }
                }
                setupCustomWorkContext(ic, listener, claz);

                //TODO V3 Handle custom work contexts supported by GlassFish
            }
        }

        //Transaction Context is not provided, so proceed with connector 1.5 way
        if (useExecutionContext) {
            try {
                setupExecutionContext(ec);
            } catch (WorkException we) {
                wc.setException(we);
            } catch (Exception e) {
                wc.setException(e);
            }
        }
    }

    /**
     * check whether the work-context is a work-context-lifecycle-listener and return the listener
     *
     * @param wc Work-Context
     * @return Work-Context-Lifecycle-Listener from the Work-Context
     */
    private WorkContextLifecycleListener getListener(WorkContext wc) {
        WorkContextLifecycleListener listener = null;
        //check whether the WorkContext has a listener.
        if (wc instanceof WorkContextLifecycleListener) {
            listener = (WorkContextLifecycleListener) wc;
        }
        return listener;
    }

    /**
     * handles custom work contexts
     *
     * @param ic       work-context
     * @param listener listener
     * @param claz     work context class
     */
    private void setupCustomWorkContext(WorkContext ic, WorkContextLifecycleListener listener,
                                          Class<? extends WorkContext> claz) {
        if (claz != null) {
            debug("setting customWorkContext for WorkContext [ " + ic.getClass().getName() + " ]" +
                    " using most specific support " +
                    "Work Context [ " + claz.getName() + " ]");
        } else {
            debug("setting exact customWorkContext for WorkContext [ " + ic.getClass().getName() + " ]  ");
        }
        notifyContextSetupComplete(listener);
    }

    /**
     * provide the most specific work context support for the work context in question
     *
     * @param ic work-context
     * @return supported work-context
     */
    private Class<? extends WorkContext> getMostSpecificWorkContextSupported(WorkContext ic) {

        List<Class> assignableClasses = new ArrayList<Class>();
        for (Class<? extends WorkContext> icClass : containerSupportedContexts) {
            if (icClass.isAssignableFrom(ic.getClass())) {
                assignableClasses.add(icClass);
            }
        }
        assignableClasses = sortBasedOnInheritence(assignableClasses);
        debug("most specific work context for [ " + ic.getClass().getName() + " ] " +
                "supported by container is [ " + assignableClasses.get(0).getName() + " ]");
        return assignableClasses.get(0);
    }

    /**
     * sort the classes based on inhertience
     *
     * @param assignableClasses list of classes
     * @return sorted classes list
     */
    private List<Class> sortBasedOnInheritence(List<Class> assignableClasses) {
        int size = assignableClasses.size();
        Class[] sortedClassesArray = new Class[size];

        for (Class claz : assignableClasses) {
            int count = getNumberOfAssignableClasses(claz, assignableClasses);
            sortedClassesArray[count - 1] = claz;
        }
        return Arrays.asList(sortedClassesArray);
    }

    /**
     * given a list of classes, provides the number of assignable (type) classes for the provided class
     *
     * @param claz              class
     * @param assignableClasses list of assiginable classes
     * @return number of assignable classes
     */
    private int getNumberOfAssignableClasses(Class claz, List<Class> assignableClasses) {
        int count = 0;
        for (Class assignableClass : assignableClasses) {
            if (claz.isAssignableFrom(assignableClass)) {
                ++count;
            }
        }
        return count;
    }

    /**
     * setup security work context for the work
     *
     * @param securityWorkContext security work context
     * @param listener              listener to be notified
     * @param raName                resource-adapter name
     */
    private void setupSecurityWorkContext(SecurityContext securityWorkContext,
                                            WorkContextLifecycleListener listener, String raName) {
        try {
            Subject executionSubject = new Subject();
            Subject serviceSubject = new Subject(); //TODO need to populate with server's credentials ?
            //boolean translationRequired = securityWorkContext.isTranslationRequired();
            //boolean translationRequired = false;
            Map securityMap = getSecurityWorkContextMap(raName);
            CallbackHandler handler = new ConnectorCallbackHandler(executionSubject, runtime.getCallbackHandler(), securityMap);

            securityWorkContext.setupSecurityContext(handler, executionSubject, serviceSubject);
            //SecurityContext.setCurrent(new SecurityContext(securityWorkContext.getSubject()));
            /*if (securityWorkContext instanceof GlassFishSecurityWorkContext) {
                if (!((GlassFishSecurityWorkContext) securityWorkContext).isTranslationRequired()) {
                    debug("translation NOT required");
                    SecurityContext.setCurrent(new SecurityContext(executionSubject));
                    //System.out.println("JSR322 :  Thread setting security context : " + Thread.currentThread().getName());
                } else {
                    debug("translation REQUIRED");
                    Subject subject = ((GlassFishSecurityWorkContext) securityWorkContext).getSubject(); //returns EIS domain principal
                    Principal appserverPrincipal = getAppserverDomainPrincipal(subject.getPrincipals());
                    //set the mapped appserver principal as distinguished caller principal so that appserver's
                    //security context will make use of it.
                    subject.getPublicCredentials().add(new DistinguishedPrincipalCredential(appserverPrincipal));
                    SecurityContext.setCurrent(new SecurityContext(subject));
                }
            }*/
//            handleIfTranslationRequired(securityWorkContext, listener, raName);

            notifyContextSetupComplete(listener);
        } catch (Exception e) {
            e.printStackTrace(); //TODO V3 log fine and remove pST
            notifyContextSetupFailure(listener, WorkContextErrorCodes.CONTEXT_SETUP_FAILED);
        }
    }

/*
    private void handleIfTranslationRequired(SecurityWorkContext securityWorkContext,
                                             WorkContextLifecycleListener listener, String raName) {
        Map eisASMap = getSecurityWorkContextMap(raName);
        if(eisASMap != null){
            getAppserverDomainPrincipal(securityWorkContext.get)
        }
    }
*/

    /**
     * get the security work context map (if any) for the resource-adapter
     * look for <[raname]-principals-map> & <[raname]-groups-map> jvm-options
     * to generate the map
     *
     * @param raName resource-adapter name
     * @return security-map
     */
    private Map getSecurityWorkContextMap(String raName) {
        HashMap eisASMap = new HashMap();

        String principalsMap = System.getProperty(raName + "-principals-map");
        if (principalsMap != null) {
            StringTokenizer tokenizer = new StringTokenizer(principalsMap, ",");
            while (tokenizer.hasMoreElements()) {
                String nameValue = (String) tokenizer.nextElement();
                if (nameValue != null && nameValue.contains("=")) {
                    int delimiterLocation = nameValue.indexOf("=");
                    String eisPrincipal = nameValue.substring(0, delimiterLocation);
                    String appserverPrincipal = nameValue.substring(delimiterLocation + 1);
                    eisASMap.put(new PrincipalImpl(eisPrincipal), new PrincipalImpl(appserverPrincipal));
                }
            }
        }

        //TODO V3 refactor (common code for principals & groups)
        String groupsMap = System.getProperty(raName + "-groups-map");
        if (groupsMap != null) {
            StringTokenizer tokenizer = new StringTokenizer(groupsMap, ",");
            while (tokenizer.hasMoreElements()) {
                String nameValue = (String) tokenizer.nextElement();
                if (nameValue != null && nameValue.contains("=")) {
                    int delimiterLocation = nameValue.indexOf("=");
                    String eisGroup = nameValue.substring(0, delimiterLocation);
                    String appserverGroup = nameValue.substring(delimiterLocation + 1);
                    eisASMap.put(new Group(eisGroup), new Group(appserverGroup));
                }
            }
            return eisASMap;
        }
        return null;
    }

/*
    private Principal getAppserverDomainPrincipal(Set principals) {
        for (Object p : principals) {
            Object appserverPrincipal = principalMap.get(p);
            if (appserverPrincipal != null && appserverPrincipal instanceof Principal) {
                debug("got mapped principal for EIS Principal [ " + p.toString() + " ]" + appserverPrincipal.toString());
                return (Principal) appserverPrincipal;
            }
        }
        return null; //could not find a mapped principal.
    }
*/

    /**
     * notify the work-context-listener that the context setup has failed
     * Error code provides specific information
     *
     * @param listener  listener to be notified
     * @param errorCode error-code
     */
    private void notifyContextSetupFailure(WorkContextLifecycleListener listener, String errorCode) {
        if (listener != null) {
            debug("notifying context setup failure");
            listener.contextSetupFailed(errorCode);
        }
    }

    /**
     * notify the work-context-listener that the context setup is complete
     *
     * @param listener listener to be notified
     */
    private void notifyContextSetupComplete(WorkContextLifecycleListener listener) {
        if (listener != null) {
            debug("notifying context setup complete");
            listener.contextSetupComplete();
        }
    }

    /**
     * setup transaction-work-context for the work
     *
     * @param tic      transaction-context
     * @param listener listener that has to be notified
     */
    private void setupTransactionWorkContext(TransactionContext tic,
                                               WorkContextLifecycleListener listener) {
        try {
            setupExecutionContext(tic);
            notifyContextSetupComplete(listener);
        } catch (Exception e) {
            notifyContextSetupFailure(listener, WorkContextErrorCodes.CONTEXT_SETUP_FAILED); //TODO
        }
    }

    /**
     * sets the execution context for the work (traditional, 1.5 way)
     *
     * @param ec ExecutionContext
     * @throws WorkException when unable to setup the execution context
     */
    private void setupExecutionContext(ExecutionContext ec) throws WorkException {
        JavaEETransactionManager tm = runtime.getTransactionManager();
        if (ec != null && ec.getXid() != null) {
            tm.recreate(ec.getXid(), ec.getTransactionTimeout());
        }
    }

    public static void debug(String message) {
        System.out.println("JSR-322 [Connector Container] [WorkContextHandler]: " + message);
    }

    /**
     * {@inheritDoc}
     */
    public static ExecutionContext getExecutionContext(Work work) {

        ExecutionContext ec = null;
        if (work instanceof WorkContextProvider) {
            WorkContextProvider icp = (WorkContextProvider) work;
            List<WorkContext> icList = icp.getWorkContexts();
            for (WorkContext ic : icList) {
                if (ic instanceof TransactionContext) {
                    ec = (TransactionContext) ic;
                    break;
                }
            }
        }
        return ec;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isContextSupported(Class contextClass) {
        return canContainerHandleSameContextType(contextClass.getClass().getName());
    }
}
