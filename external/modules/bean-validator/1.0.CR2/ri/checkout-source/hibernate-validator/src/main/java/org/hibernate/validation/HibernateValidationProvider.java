// $Id: HibernateValidationProvider.java 16395 2009-04-22 10:59:38Z hardy.ferentschik $
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
package org.hibernate.validation;

import javax.validation.Configuration;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ConfigurationState;
import javax.validation.spi.ValidationProvider;

import org.hibernate.validation.engine.ConfigurationImpl;
import org.hibernate.validation.engine.HibernateValidatorConfiguration;
import org.hibernate.validation.engine.ValidatorFactoryImpl;

/**
 * Default implementation of <code>ValidationProvider</code> within Hibernate validator.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class HibernateValidationProvider implements ValidationProvider {

	/**
	 * {@inheritDoc}
	 */
	public boolean isSuitable(Class<? extends Configuration<?>> builderClass) {
		return builderClass == HibernateValidatorConfiguration.class;
	}

	public <T extends Configuration<T>> T createSpecializedConfiguration(BootstrapState state, Class<T> configurationClass) {
		if ( !isSuitable( configurationClass ) ) {
			throw new ValidationException(
					"Illegal call to createSpecializedConfiguration() for a non suitable provider"
			);
		}
		//cast protected by isSuitable call
		return configurationClass.cast( new ConfigurationImpl( this ) );
	}

	/**
	 * {@inheritDoc}
	 */
	public Configuration<?> createGenericConfiguration(BootstrapState state) {
		return new ConfigurationImpl( state );
	}

	/**
	 * {@inheritDoc}
	 */
	public ValidatorFactory buildValidatorFactory(ConfigurationState configurationState) {
		return new ValidatorFactoryImpl( configurationState );
	}
}
