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

import org.eclipse.persistence.jpa.internal.jpql.WordParser;

/**
 * The <b>LOCATE</b> function returns the position of a given string within a string, starting the
 * search at a specified position. It returns the first position at which the string was found as an
 * integer. The first argument is the string to be located; the second argument is the string to be
 * searched; the optional third argument is an integer that represents the string position at which
 * the search is started (by default, the beginning of the string to be searched). The first
 * position in a string is denoted by 1. If the string is not found, 0 is returned. The <b>LENGTH</b>
 * function returns the length of the string in characters as an integer.
 * <p>
 * <div nowrap><b>BNF:</b> <code>expression ::= LOCATE(string_primary, string_primary [, simple_arithmetic_expression])</code><p>
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
public final class LocateExpression extends AbstractTripleEncapsulatedExpression {

	/**
	 * Creates a new <code>LocateExpression</code>.
	 *
	 * @param parent The parent of this expression
	 */
	LocateExpression(AbstractExpression parent) {
		super(parent);
	}

	/**
	 * {@inheritDoc}
	 */
	public void accept(ExpressionVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPQLQueryBNF getQueryBNF() {
		return queryBNF(FunctionsReturningNumericsBNF.ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	boolean isThirdExpressionOptional() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPQLQueryBNF parameterExpressionBNF(int index) {
		switch (index) {
			case 3:  return queryBNF(SimpleArithmeticExpressionBNF.ID);
			default: return queryBNF(StringPrimaryBNF.ID);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	String parseIdentifier(WordParser wordParser) {
		return LOCATE;
	}
}