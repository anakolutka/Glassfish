/*******************************************************************************
 * Copyright (c) 2006, 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation
 *
 ******************************************************************************/
package org.eclipse.persistence.jpa.jpql.spi;

/**
 * The external representation of a Java class constructor.
 *
 * @see IType
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
public interface IConstructor extends IExternalForm {

	/**
	 * Returns the list of {@link ITypeDeclaration} representing the parameter types. If this is the
	 * default constructor, then an empty array should be returned.
	 *
	 * @return The list of parameter types or an empty list
	 */
	ITypeDeclaration[] getParameterTypes();
}