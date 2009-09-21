// $Id: ValidatorFactory.java 16754 2009-06-10 21:48:21Z epbernard $
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

/**
 * Factory returning initialized <code>Validator</code> instances.
 * Implementations are thread-safe
 * This object is typically cached and reused.
 *
 * @author Emmanuel Bernard
 */
public interface ValidatorFactory {
	/**
	 * @return Returns an initialized <code>Validator</code> instance using the default
	 * factory instances for message interpolator and traversable resolver.
	 * <p>
	 * Validator instances can be pooled and shared by the implementation.
	 * </p>
	 */
	Validator getValidator();

	/**
	 * Define the validator context and return a <code>Validator</code> compliant
	 * with this state.
	 *
	 * @return a <code>ValidatorContext</code>.
	 */
	ValidatorContext usingContext();

	/**
	 * Returns the <code>MessageInterpolator</code> instance configured at
	 * initialization time for the <code>ValidatorFactory<code>.
	 * This is the instance used by #getValidator().
	 *
	 * @return MessageInterpolator instance.
	 */
	MessageInterpolator getMessageInterpolator();

	/**
	 * Return an object of the specified type to allow access to the
	 * provider-specific API.  If the Bean Validation provider
	 * implementation does not support the specified class, the
	 * ValidationException is thrown. 
	 * @param type  the class of the object to be returned.
	 * @return an instance of the specified class
	 * @throws ValidationException if the provider does not
	 *         support the call.
	 */
	public <T> T unwrap(Class<T> type);
}
