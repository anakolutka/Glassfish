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
package org.eclipse.persistence.jpa.internal.jpql.parser;

/**
 * The query BNF for an arithmetic term expression.
 *
 * <div nowrap><b>BNF:</b> <code>arithmetic_term ::= arithmetic_factor | arithmetic_term { * | / } arithmetic_factor</code><p>
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
public final class ArithmeticTermBNF extends JPQLQueryBNF {

	/**
	 * The unique identifier of this BNF rule.
	 */
	public static final String ID = "arithmetic_term";

	/**
	 * Creates a new <code>ArithmeticTermBNF</code>.
	 */
	ArithmeticTermBNF() {
		super(ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	String getFallbackBNFId() {
		return ArithmeticFactorBNF.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean handleAggregate() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void initialize() {
		super.initialize();

		registerExpressionFactory(ArithmeticExpressionFactory.ID);

		registerChild(ArithmeticFactorBNF.ID);
	}
}