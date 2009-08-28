// $Id: Group.java 16031 2009-02-25 13:09:51Z hardy.ferentschik $
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
package org.hibernate.validation.engine.groups;

import javax.validation.groups.Default;

/**
 * Encapsulates a single group.
 *
 * @author Hardy Ferentschik
 */
public class Group {
	private Class<?> group;
	private Class<?> sequence;

	public Group(Class<?> group) {
		this( group, null );
	}

	public Group(Class<?> group, Class<?> sequence) {
		this.group = group;
		this.sequence = sequence;
	}

	public Class<?> getGroup() {
		return group;
	}

	public boolean partOfSequence() {
		return sequence != null;
	}

	public Class<?> getSequence() {
		return sequence;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Group group1 = ( Group ) o;

		if ( group != null ? !group.equals( group1.group ) : group1.group != null ) {
			return false;
		}
		if ( sequence != null ? !sequence.equals( group1.sequence ) : group1.sequence != null ) {
			return false;
		}

		return true;
	}

	public boolean isDefaultGroup() {
		return getGroup().getName().equals( Default.class.getName() );
	}

	@Override
	public int hashCode() {
		int result = group != null ? group.hashCode() : 0;
		result = 31 * result + ( sequence != null ? sequence.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "Group{" +
				"groups=" + group +
				", sequence=" + sequence +
				'}';
	}
}
