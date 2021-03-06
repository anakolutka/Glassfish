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
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.queries;

import org.eclipse.persistence.testing.framework.*;
import org.eclipse.persistence.queries.*;
import org.eclipse.persistence.testing.models.legacy.*;

public class ConformResultsWithSelectionObjectTest extends ConformResultsInUnitOfWorkTest {
    Object selectionObject;

    public void buildConformQuery() {
        conformedQuery = new ReadObjectQuery();
        ((ReadObjectQuery)conformedQuery).setSelectionObject(selectionObject);
        conformedQuery.conformResultsInUnitOfWork();
    }

    public void prepareTest() {
        selectionObject = new Employee();
        ((Employee)selectionObject).firstName = "Bobert";
        ((Employee)selectionObject).lastName = "Schmit";
        unitOfWork.registerObject(selectionObject);
    }

    public void verify() {
        if (result == null) {
            throw new TestErrorException("object existed in unit of work but not returned in query");
        }
    }
}
