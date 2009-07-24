/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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


package org.glassfish.webbeans.ejb;

import org.jboss.webbeans.ejb.spi.EjbServices;

import org.jboss.webbeans.ejb.api.SessionObjectReference;

import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.webbeans.ejb.spi.EjbDescriptor;
import com.sun.enterprise.deployment.EjbSessionDescriptor;


import java.util.Vector;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.jvnet.hk2.component.Habitat;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.lang.annotation.Annotation;

import org.glassfish.ejb.api.EjbContainerServices;

/**
 */
public class EjbServicesImpl implements EjbServices
{

    private Set<com.sun.enterprise.deployment.EjbDescriptor> ejbDescs;
    private Habitat habitat;

    public EjbServicesImpl(Habitat h, Set<com.sun.enterprise.deployment.EjbDescriptor> ejbs) {
        habitat = h;
        ejbDescs = ejbs;
    }

    /**
    * Resolve the value for the given @EJB injection point
    * 
    * @param injectionPoint
    *           the injection point metadata
    * @return an instance of the EJB
    * @throws IllegalArgumentException
    *            if the injection point is not annotated with @EJB, or, if the
    *            injection point is a method that doesn't follow JavaBean
    *            conventions
   
    */
    public Object resolveEjb(InjectionPoint injectionPoint) {

        EjbContainerServices containerServices = habitat.getByContract(EjbContainerServices.class);

        // Look for @EJB annotation.  (Do it by class name matching to avoid direct dependency
        // from this module on javax.ejb)
        Annotation ejbAnnotation = null;
/*
        for(Annotation annotation : injectionPoint.getAnnotations()) {
            if( annotation.annotationType().getName().equals("javax.ejb.EJB")) {
                ejbAnnotation = annotation;
                break;
            }
        }

        if( ejbAnnotation == null ) {
            throw new IllegalArgumentException("injection point is not annotated with @EJB " +
                injectionPoint);
        }
*/

        return containerServices.resolveRemoteEjb(ejbAnnotation, injectionPoint.getMember());

    }
   
   /**
    * Request a reference to an EJB session object from the container. If the
    * EJB being resolved is a stateful session bean, the container should ensure
    * the session bean is created before this method returns.
    * 
    * @param ejbDescriptor the ejb to resolve
    * @return a reference to the session object
    */
    public SessionObjectReference resolveEjb(EjbDescriptor<?> ejbDescriptor) {

        SessionObjectReference sessionObj = null;

        // TODO API needs to provide client type information
        // For now, just grab the first known client view and use that to get the
        // portable global jndi name
        String globalJndiName = getDefaultGlobalJndiName(ejbDescriptor);
        if( globalJndiName != null ) {
            try {

                InitialContext ic = new InitialContext();

                Object ejbRef = ic.lookup(globalJndiName);

                EjbContainerServices containerServices = habitat.getByContract(EjbContainerServices.class);

                sessionObj = new SessionObjectReferenceImpl(containerServices, ejbRef);

            } catch(NamingException ne) {
//                throw new IllegalStateException("Error resolving session object reference for ejb name " +
//                        ejbDescriptor.getEjbName() + " and jndi name " + globalJndiName, ne);
            }
        }  else {
//            throw new IllegalArgumentException("Not enough type information to resolve ejb for " +
//                " ejb name " + ejbDescriptor.getEjbName());
        }

	    return sessionObj;

    }
   
   /**
    * Resolve a remote EJB reference. At least one of the parameters will not be
    * null.
    * 
    * @param jndiName the JNDI name
    * @param mappedName the mapped name
    * @param ejbLink the EJB link name
    * @return the remote EJB reference
    * @throws IllegalStateException
    *            if no EJBs can be resolved for injection
    * @throws IllegalArgumentException
    *            if jndiName, mappedName and ejbLink are null
    */
    public Object resolveRemoteEjb(String jndiName, String mappedName, String ejbLink) {
        Object remoteRef = null;

        if( (jndiName == null) && (mappedName == null) && (ejbLink == null) ) {
            throw new IllegalArgumentException("All linking arguments are null");
        }

        String lookupString = jndiName;

	    try {

            InitialContext ic = new InitialContext();

            if( lookupString == null ) {

                // TODO Need extra processing of ejbLink.  Also need interface in the
                // case that ejbLink is provided

                lookupString = (mappedName != null) ? mappedName :
                        "java:module/" + ejbLink;
            }

            remoteRef = ic.lookup(lookupString);

        } catch(NamingException ne) {
             throw new IllegalStateException("Error resolving session object reference for name " +
                       lookupString, ne);
        }

        return remoteRef;
    }
   
   /**
    * Gets a descriptor for each EJB 
    * 
    * @return the EJB descriptors
    */
    public Iterable<EjbDescriptor<?>> discoverEjbs() {

        Set<EjbDescriptor<?>> ejbs = new HashSet<EjbDescriptor<?>>();

        for(com.sun.enterprise.deployment.EjbDescriptor next : ejbDescs) {

            EjbDescriptorImpl wbEjbDesc = new EjbDescriptorImpl(next);
            ejbs.add(wbEjbDesc);

        }

       return ejbs;
    }

    private String getDefaultGlobalJndiName(EjbDescriptor ejbDesc) {

        EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor)
                ((EjbDescriptorImpl) ejbDesc).getEjbDescriptor();

        String clientView = null;

        if( sessionDesc.isLocalBean() ) {
            clientView = sessionDesc.getEjbClassName();
        } else if( sessionDesc.getLocalBusinessClassNames().size() == 1) {
            clientView = sessionDesc.getLocalBusinessClassNames().iterator().next();
        } else if( sessionDesc.getRemoteBusinessClassNames().size() == 1) {
            clientView = sessionDesc.getRemoteBusinessClassNames().iterator().next();
        }

        return (clientView != null) ? sessionDesc.getPortableJndiName(clientView) : null;

    }
}
