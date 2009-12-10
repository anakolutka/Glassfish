// $Id: ContainsField.java 17620 2009-10-04 19:19:28Z hardy.ferentschik $
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
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
package org.hibernate.validator.util;

import java.security.PrivilegedAction;

/**
 * @author Emmanuel Bernard
 */
public class ContainsField implements PrivilegedAction<Boolean> {
	private final Class<?> clazz;
	private final String property;

	public static ContainsField action(Class<?> clazz, String property) {
		return new ContainsField( clazz, property );
	}

	private ContainsField(Class<?> clazz, String property) {
		this.clazz = clazz;
		this.property = property;
	}

	public Boolean run() {
		try {
			clazz.getDeclaredField( property );
			return true;
		}
		catch ( NoSuchFieldException e ) {
			return false;
		}
	}
}
