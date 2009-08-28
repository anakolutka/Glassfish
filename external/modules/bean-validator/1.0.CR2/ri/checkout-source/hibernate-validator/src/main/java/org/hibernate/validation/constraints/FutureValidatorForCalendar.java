// $Id: FutureValidatorForCalendar.java 15851 2009-02-02 15:40:50Z hardy.ferentschik $
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
package org.hibernate.validation.constraints;

import java.util.Calendar;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.Past;

/**
 * Check that the <code>java.util.Calendar</code> passed to be validated is in
 * the future.
 *
 * @author Alaa Nassef
 */
public class FutureValidatorForCalendar implements ConstraintValidator<Past, Calendar> {

	public void initialize(Past constraintAnnotation) {
	}

	public boolean isValid(Calendar cal, ConstraintValidatorContext constraintValidatorContext) {
		//null values are valid
		if ( cal == null ) {
			return true;
		}
		return cal.after( Calendar.getInstance() );
	}
}
