// $Id: CustomConsistentUserValidator.java 16287 2009-04-09 16:07:22Z hardy.ferentschik $
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
package org.hibernate.validation.engine.xml;

import javax.validation.ConstraintValidatorContext;

import org.slf4j.Logger;

import org.hibernate.validation.util.LoggerFactory;

/**
 * @author Hardy Ferentschik
 */
public class CustomConsistentUserValidator extends ConsistentUserValidator {
	private static final Logger log = LoggerFactory.make();

	public boolean isValid(User user, ConstraintValidatorContext constraintValidatorContext) {
		log.debug( "is valid in CustomConsistentUserValidator is called." );
		return super.isValid( user, constraintValidatorContext );
	}
}
