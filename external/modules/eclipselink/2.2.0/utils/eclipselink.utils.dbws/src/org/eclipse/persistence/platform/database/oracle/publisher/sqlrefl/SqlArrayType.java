/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Mike Norman - from Proof-of-concept, become production code
 ******************************************************************************/
package org.eclipse.persistence.platform.database.oracle.publisher.sqlrefl;

//EclipseLink imports
import org.eclipse.persistence.platform.database.oracle.publisher.visit.PublisherVisitor;
import org.eclipse.persistence.platform.database.oracle.publisher.visit.PublisherWalker;

public class SqlArrayType extends SqlCollectionType {

    public SqlArrayType(SqlName sqlName, boolean generateMe, SqlType parentType,
        SqlReflector reflector) {
        super(sqlName, OracleTypes.ARRAY, generateMe, parentType, reflector);
    }

    /**
     * Determines if this Type represents an Array type.
     * <p/>
     */
    public boolean isArray() {
        // INFEASIBLE
        return true;
    }

    public void accept(PublisherVisitor v) {
        ((PublisherWalker)v).visit(this);
    }
}
