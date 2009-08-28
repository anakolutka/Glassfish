// $Id: DummyValidator.java 16058 2009-03-03 11:06:02Z hardy.ferentschik $
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
package org.hibernate.validation.constraints.validatorcontext;

import java.util.Map;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Hardy Ferentschik
 */
public class DummyValidator implements ConstraintValidator<Dummy, String> {

	private static boolean disableDefaultError;

	private static Map<String, String> errorMessages;


	public void initialize(Dummy parameters) {
	}

	public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
		if ( disableDefaultError ) {
			constraintValidatorContext.disableDefaultError();
		}

		if ( errorMessages != null ) {
			for ( Map.Entry<String, String> entry : errorMessages.entrySet() ) {
				constraintValidatorContext.addError( entry.getKey(), entry.getValue() );
			}
		}

		return false;
	}

	public static void disableDefaultError(boolean b) {
		disableDefaultError = b;
	}

	public static void setErrorMessages(Map<String, String> errorMessages) {
		DummyValidator.errorMessages = errorMessages;
	}
}