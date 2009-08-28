// $Id: ValidationProvider.java 16790 2009-06-16 02:56:04Z epbernard $
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
package javax.validation.spi;

import javax.validation.Configuration;
import javax.validation.ValidatorFactory;

/**
 * Contract between the validation bootstrap mechanism and the provider engine.
 * <p/>
 * Implementations must have a public no-arg constructor. The construction of a provider
 * should be as "lightweight" as possible.
 *
 * <code>T</code> represents the provider specific Configuration subclass
 * which typically host provider's additional configuration methods.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface ValidationProvider<T extends Configuration<T>> {

	/**
	 * Returns a Configuration instance implementing <code>T</code>,
	 * the <code>Configuration</code> subinterface.
	 * The returned Configuration instance must use the current provider (<code>this</code>)
	 * to build the ValidatorFactory instance.
	 * <p/>
	 *
	 * @param state bootstrap state
	 *
	 * @return specific Configuration implementation
	 */
	T createSpecializedConfiguration(BootstrapState state);

	/**
	 * Returns a Configuration instance. This instance is not bound to
	 * use the current provider. The choice of provider follows the algorithm described
	 * in {@link javax.validation.Configuration}
	 * <p/>
	 * The ValidationProviderResolver used by <code>Configuration</code>
	 * is provided by <code>state</code>.
	 * If null, the default ValidationProviderResolver is used.
	 *
	 * @param state bootstrap state
	 *
	 * @return Non specialized Configuration implementation
	 */
	Configuration<?> createGenericConfiguration(BootstrapState state);

	/**
	 * Build a ValidatorFactory using the current provider implementation. The
	 * ValidatorFactory is assembled and follows the configuration passed
	 * via ConfigurationState.
	 * <p>
	 * The returned ValidatorFactory is properly initialized and ready for use.
	 * </p>
	 *
	 * @param configurationState the configuration descriptor
	 *
	 * @return the instanciated ValidatorFactory
	 * @throws javax.validation.ValidationException if the ValidatorFactory cannot be built
	 */
	ValidatorFactory buildValidatorFactory(ConfigurationState configurationState);
}