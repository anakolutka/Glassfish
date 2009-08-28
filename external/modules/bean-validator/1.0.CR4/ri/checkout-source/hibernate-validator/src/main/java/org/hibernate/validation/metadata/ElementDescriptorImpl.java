// $Id: ElementDescriptorImpl.java 17137 2009-07-17 19:56:59Z epbernard $
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.validation.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;

import org.hibernate.validation.engine.groups.Group;
import org.hibernate.validation.engine.groups.GroupChain;
import org.hibernate.validation.engine.groups.GroupChainGenerator;

/**
 * Describe a validated element (class, field or property).
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ElementDescriptorImpl implements ElementDescriptor {
	private final Class<?> type;
	private final Set<ConstraintDescriptor<?>> constraintDescriptors = new HashSet<ConstraintDescriptor<?>>();

	public ElementDescriptorImpl(Class<?> type) {
		this.type = type;
	}

	public void addConstraintDescriptor(ConstraintDescriptorImpl constraintDescriptor) {
		constraintDescriptors.add( constraintDescriptor );
	}

	public boolean hasConstraints() {
		return constraintDescriptors.size() != 0;
	}

	public Class<?> getElementClass() {
		return type;
	}

	public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
		return Collections.unmodifiableSet( constraintDescriptors );
	}

	//FIXME remove when HV depends on BV post CR3 (typo in the method name)
	public Set<ConstraintDescriptor<?>> getUnorderdConstraintDescriptorsMatchingGroups(Class<?>... groups) {
		return getUnorderedConstraintDescriptorsMatchingGroups( groups );
	}

	public Set<ConstraintDescriptor<?>> getUnorderedConstraintDescriptorsMatchingGroups(Class<?>... groups) {
		Set<ConstraintDescriptor<?>> matchingDescriptors = new HashSet<ConstraintDescriptor<?>>();
		GroupChain groupChain = new GroupChainGenerator().getGroupChainFor( Arrays.asList( groups ) );
		Iterator<Group> groupIterator = groupChain.getGroupIterator();
		while ( groupIterator.hasNext() ) {
			Group g = groupIterator.next();
			addMatchingDescriptorsForGroup( g.getGroup(), matchingDescriptors );
		}
		return Collections.unmodifiableSet( matchingDescriptors );
	}

	private void addMatchingDescriptorsForGroup(Class<?> group, Set<ConstraintDescriptor<?>> matchingDescriptors) {
		for ( ConstraintDescriptor<?> descriptor : constraintDescriptors ) {
			if ( descriptor.getGroups().contains( group ) ) {
				matchingDescriptors.add( descriptor );
			}
		}
	}
}
