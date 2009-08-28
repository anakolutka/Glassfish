// $Id: ElementDescriptor.java 16102 2009-03-06 22:23:09Z hardy.ferentschik $
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
package javax.validation;

import java.util.Set;

/**
 * Describes a validated element (class, field or property).
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface ElementDescriptor {

	/**
	 * @return <code>true</code> if at least one constraint declaration is present on the element, <code>false</code> otherwise.
	 */
	boolean hasConstraints();

	/**
	 * @return Statically defined returned type.
	 *
	 * @todo should it be Type or even completly removed
	 */
	Class<?> getType();

	/**
	 * Return all constraint descriptors for this element or an
	 * empty Set if none are present.
	 *
	 * @return Set of constraint descriptors for this element
	 */
	Set<ConstraintDescriptor<?>> getConstraintDescriptors();
}
