// $Id: MultipleMinMax.java 16156 2009-03-13 09:46:38Z hardy.ferentschik $
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
package org.hibernate.validation.engine.validatorresolution;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * @author Hardy Ferentschik
 */
public class MultipleMinMax {
	@Min(10l)
	@Max(20l)
	Number number;

	@Min(10l)
	@Max(20l)
	String stringNumber;

	public MultipleMinMax(String stringNumber, Number number) {
		this.stringNumber = stringNumber;
		this.number = number;
	}
}
