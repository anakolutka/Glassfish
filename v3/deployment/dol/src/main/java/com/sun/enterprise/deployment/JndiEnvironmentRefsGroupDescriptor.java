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
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.EjbReferenceContainer;
import com.sun.enterprise.deployment.types.MessageDestinationReferenceContainer;
import com.sun.enterprise.deployment.types.ResourceReferenceContainer;
import com.sun.enterprise.deployment.util.LogDomains;
import org.glassfish.deployment.common.DeploymentUtils;
import static com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;

/**
 * Contains information about jndiEnvironmentRefsGroup.
 */ 

public abstract class JndiEnvironmentRefsGroupDescriptor extends Descriptor
        implements EjbReferenceContainer, ResourceReferenceContainer,
        MessageDestinationReferenceContainer, WritableJndiNameEnvironment
{
    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(JndiEnvironmentRefsGroupDescriptor.class);

    private static final Logger _logger = LogDomains.getLogger(DeploymentUtils.class, LogDomains.DPL_LOGGER);

    protected Map<CallbackType,
                Set<LifecycleCallbackDescriptor>> callbackDescriptors
        = new HashMap<CallbackType, Set<LifecycleCallbackDescriptor>>();

    protected EjbBundleDescriptor bundleDescriptor;

    protected Set environmentProperties;
    protected Set ejbReferences;
    protected Set jmsDestReferences;
    protected Set messageDestReferences;
    protected Set resourceReferences;
    protected Set serviceReferences;
    protected Set<EntityManagerFactoryReferenceDescriptor> 
        entityManagerFactoryReferences;
    protected Set<EntityManagerReferenceDescriptor> 
        entityManagerReferences;

    // callbacks
    public void addCallbackDescriptor(CallbackType type,
            LifecycleCallbackDescriptor llcDesc) {
        Set<LifecycleCallbackDescriptor> llcDescs =
            getCallbackDescriptors(type);
        boolean found = false;       
        for (LifecycleCallbackDescriptor llcD : llcDescs) {
            if ((llcDesc.getLifecycleCallbackClass() != null) &&
                llcDesc.getLifecycleCallbackClass().equals(
                    llcD.getLifecycleCallbackClass())) {
                found = true;
            }
        }

        if (!found) {
            llcDescs.add(llcDesc);
        }
    }

    public void addCallbackDescriptors(CallbackType type, 
                                  Set<LifecycleCallbackDescriptor> lccSet) {
        for (LifecycleCallbackDescriptor lcc : lccSet) {
            addCallbackDescriptor(type, lcc);
        }
    }

    public Set<LifecycleCallbackDescriptor> getCallbackDescriptors(
            CallbackType type)
    {
        Set<LifecycleCallbackDescriptor> lccDescs =
            callbackDescriptors.get(type);
        if (lccDescs == null) {
            lccDescs = new HashSet<LifecycleCallbackDescriptor>();
            callbackDescriptors.put(type, lccDescs);
        }
        return lccDescs;
    }

    public boolean hasCallbackDescriptor(CallbackType type) {
        return (getCallbackDescriptors(type).size() > 0);
    }

    public void addPostConstructDescriptor(LifecycleCallbackDescriptor lcDesc) {
        addCallbackDescriptor(CallbackType.POST_CONSTRUCT, lcDesc);
    }

    public LifecycleCallbackDescriptor getPostConstructDescriptorByClass(String className) {
        throw new UnsupportedOperationException();
    }

    public Set<LifecycleCallbackDescriptor> getPostConstructDescriptors() {
        return getCallbackDescriptors(CallbackType.POST_CONSTRUCT);
    }

    public void addPreDestroyDescriptor(LifecycleCallbackDescriptor lcDesc) {
        addCallbackDescriptor(CallbackType.PRE_DESTROY, lcDesc);
    }

    public LifecycleCallbackDescriptor getPreDestroyDescriptorByClass(String className) {
        throw new UnsupportedOperationException();
    }

    public Set<LifecycleCallbackDescriptor> getPreDestroyDescriptors() {
        return getCallbackDescriptors(CallbackType.PRE_DESTROY);
    }

    
    public EjbBundleDescriptor getEjbBundleDescriptor() {
	return this.bundleDescriptor;
    }
    
    public void setEjbBundleDescriptor(EjbBundleDescriptor bundleDescriptor) {
	this.bundleDescriptor = bundleDescriptor;
    }

    // ejb ref
    public void addEjbReferenceDescriptor(EjbReference ejbReference) {
	this.getEjbReferenceDescriptors().add(ejbReference);
	ejbReference.setReferringBundleDescriptor(getEjbBundleDescriptor());
    }

    public EjbReference getEjbReference(String name) {
	for (Iterator itr = this.getEjbReferenceDescriptors().iterator(); itr.hasNext();) {
	    EjbReference er = (EjbReference) itr.next();
	    if (er.getName().equals(name)) {
		return er;   
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoejbrefbyname",
                "This class has no ejb reference by the name of {0}",
                new Object[] {name}));
    }

    public Set getEjbReferenceDescriptors() {
	if (this.ejbReferences == null) {
	    this.ejbReferences = new OrderedSet();
	}
	return this.ejbReferences = new OrderedSet(this.ejbReferences);
    }

    public void removeEjbReferenceDescriptor(EjbReference ejbReference) {
	this.getEjbReferenceDescriptors().remove(ejbReference);
	ejbReference.setReferringBundleDescriptor(null);
    }

    // message destination ref
    public void addMessageDestinationReferenceDescriptor(MessageDestinationReferenceDescriptor msgDestReference) {
        if( getEjbBundleDescriptor() != null ) {
            msgDestReference.setReferringBundleDescriptor
                (getEjbBundleDescriptor());
        }
        this.getMessageDestinationReferenceDescriptors().add(msgDestReference);
    }

    public MessageDestinationReferenceDescriptor getMessageDestinationReferenceByName(String name) {
	for (Iterator itr = 
                 this.getMessageDestinationReferenceDescriptors().iterator(); 
             itr.hasNext();) {
	    MessageDestinationReferenceDescriptor mdr = 
                (MessageDestinationReferenceDescriptor) itr.next();
	    if (mdr.getName().equals(name)) {
		return mdr;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnomsgdestrefbyname",
                "This class has no message destination reference by the name of {0}",
                new Object[] {name}));
    }

    public Set getMessageDestinationReferenceDescriptors() {
        if( this.messageDestReferences == null ) {
            this.messageDestReferences = new OrderedSet();
        }
        return this.messageDestReferences = 
            new OrderedSet(this.messageDestReferences);
    }

    public void removeMessageDestinationReferenceDescriptor
        (MessageDestinationReferenceDescriptor msgDestRef) { 
        this.getMessageDestinationReferenceDescriptors().remove(msgDestRef);
    }

    // env property
    public void addEnvironmentProperty(EnvironmentProperty environmentProperty) {
	this.getEnvironmentProperties().add(environmentProperty);
    }

    public Set getEnvironmentProperties() {
	if (this.environmentProperties == null) {
	    this.environmentProperties = new OrderedSet();
	}
	return this.environmentProperties = new OrderedSet(this.environmentProperties);
    }

    public EnvironmentProperty getEnvironmentPropertyByName(String name) {
	for (Iterator itr = this.getEnvironmentProperties().iterator(); itr.hasNext();) {
	    EnvironmentProperty ev = (EnvironmentProperty) itr.next();
	    if (ev.getName().equals(name)) {
		return ev;   
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoenvpropertybyname",
                "This class has no environment property by the name of {0}",
                new Object[] {name}));
    }

    public void removeEnvironmentProperty(
			EnvironmentProperty environmentProperty) {
	this.getEnvironmentProperties().remove(environmentProperty);
    }

    // service ref
    public void addServiceReferenceDescriptor(
	        ServiceReferenceDescriptor serviceReference) {
        serviceReference.setBundleDescriptor(getEjbBundleDescriptor());
        this.getServiceReferenceDescriptors().add(serviceReference);
    }

    public Set getServiceReferenceDescriptors() {
        if( this.serviceReferences == null ) {
            this.serviceReferences = new OrderedSet();
        }
        return this.serviceReferences = new OrderedSet(this.serviceReferences);
    }

    public ServiceReferenceDescriptor getServiceReferenceByName(String name) {
	for (Iterator itr = this.getServiceReferenceDescriptors().iterator(); 
             itr.hasNext();) {
	    ServiceReferenceDescriptor srd = (ServiceReferenceDescriptor) 
                itr.next();
	    if (srd.getName().equals(name)) {
		return srd;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoservicerefbyname",
                "This class has no service reference by the name of {0}",
                new Object[] {name}));
    }

    public void removeServiceReferenceDescriptor(
		ServiceReferenceDescriptor serviceReference) {
        this.getServiceReferenceDescriptors().remove(serviceReference);
    }

    // resource ref
    public void addResourceReferenceDescriptor(
			ResourceReferenceDescriptor resourceReference) {
	this.getResourceReferenceDescriptors().add(resourceReference);
    }

    public Set getResourceReferenceDescriptors() {
	if (this.resourceReferences == null) {
	    this.resourceReferences = new OrderedSet();
	}
	return this.resourceReferences = new OrderedSet(this.resourceReferences);
    }

    public ResourceReferenceDescriptor getResourceReferenceByName(String name) {
	for (Iterator itr = this.getResourceReferenceDescriptors().iterator(); itr.hasNext();) {
	    ResourceReferenceDescriptor next = (ResourceReferenceDescriptor) itr.next();
	    if (next.getName().equals(name)) {
		return next;   
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoresourcerefbyname",
                "This class has no resource reference by the name of {0}",
                new Object[] {name}));
    }

    public void removeResourceReferenceDescriptor(
			ResourceReferenceDescriptor resourceReference) {
	this.getResourceReferenceDescriptors().remove(resourceReference);
    }

    // jms destination ref
    public void addJmsDestinationReferenceDescriptor(
		JmsDestinationReferenceDescriptor jmsDestinationReference) {
	this.getJmsDestinationReferenceDescriptors().add(jmsDestinationReference);
    }

    public Set getJmsDestinationReferenceDescriptors() {
	if (this.jmsDestReferences == null) {
	    this.jmsDestReferences = new OrderedSet();
	}
	return this.jmsDestReferences = new OrderedSet(this.jmsDestReferences);
    }

    public JmsDestinationReferenceDescriptor getJmsDestinationReferenceByName(String name) {
	for (Iterator itr = this.getJmsDestinationReferenceDescriptors().iterator(); itr.hasNext();) {
	    JmsDestinationReferenceDescriptor jdr = (JmsDestinationReferenceDescriptor) itr.next();
	    if (jdr.getName().equals(name)) {
		return jdr;   
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnojmsdestrefbyname",
                "This class has no resource environment reference by the name of {0}",
                new Object[] {name}));
    }

    public void removeJmsDestinationReferenceDescriptor(
		JmsDestinationReferenceDescriptor jmsDestinationReference) {
	this.getJmsDestinationReferenceDescriptors().remove(jmsDestinationReference);
    }

    // entity manager factory ref 
    public void addEntityManagerFactoryReferenceDescriptor(
                EntityManagerFactoryReferenceDescriptor reference) {
        if( getEjbBundleDescriptor() != null ) {
            reference.setReferringBundleDescriptor
                (getEjbBundleDescriptor());
        }
        this.getEntityManagerFactoryReferenceDescriptors().add(reference);
    }

    public Set<EntityManagerFactoryReferenceDescriptor> getEntityManagerFactoryReferenceDescriptors() {
        if( this.entityManagerFactoryReferences == null ) {
            this.entityManagerFactoryReferences = 
                new HashSet<EntityManagerFactoryReferenceDescriptor>();
        }
        return entityManagerFactoryReferences; 
    }

    public EntityManagerFactoryReferenceDescriptor getEntityManagerFactoryReferenceByName(String name) {
	for (EntityManagerFactoryReferenceDescriptor next :
             getEntityManagerFactoryReferenceDescriptors()) {

	    if (next.getName().equals(name)) {
		return next;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoentitymgrfactoryrefbyname",
                "This class has no entity manager factory reference by the name of {0}",
                new Object[] {name}));
    }

    //  entity manager ref
    public void addEntityManagerReferenceDescriptor(
                EntityManagerReferenceDescriptor reference) {
        if( getEjbBundleDescriptor() != null ) {
            reference.setReferringBundleDescriptor
                (getEjbBundleDescriptor());
        }
        this.getEntityManagerReferenceDescriptors().add(reference);
    }

    public Set<EntityManagerReferenceDescriptor> getEntityManagerReferenceDescriptors() {
        if( this.entityManagerReferences == null ) {
            this.entityManagerReferences = 
                new HashSet<EntityManagerReferenceDescriptor>();
        }
        return entityManagerReferences; 
    }

    public EntityManagerReferenceDescriptor getEntityManagerReferenceByName(String name) {
	for (EntityManagerReferenceDescriptor next :
             getEntityManagerReferenceDescriptors()) {

	    if (next.getName().equals(name)) {
		return next;
	    }
	}
	throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionhasnoentitymgrrefbyname",
                "This class has no entity manager reference by the name of {0}",
                new Object[] {name}));
    }

    public List<InjectionCapable> getInjectableResourcesByClass(String className) {
        throw new UnsupportedOperationException();
    }    

    public InjectionInfo getInjectionInfoByClass(String className) {
        throw new UnsupportedOperationException();
    }
}
