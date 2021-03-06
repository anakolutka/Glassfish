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
package org.eclipse.persistence.mappings.querykeys;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.internal.helper.DatabaseTable;


/**
 * <p>
 * <b>Purpose</b>:Represents a 1-m join query.
 */
public class OneToManyQueryKey extends ForeignReferenceQueryKey {

    /**
     * INTERNAL:
     * override the isCollectionQueryKey() method in the superclass to return true.
     * @return boolean
     */
    public boolean isCollectionQueryKey() {
        return true;
    }

    /**
     * INTERNAL:
     * override the isOneToManyQueryKey() method in the superclass to return true.
     * @return boolean
     */
    public boolean isOneToManyQueryKey() {
        return true;
    }

    /**
     * PUBLIC:
     * Returns the reference table.
     */
    public DatabaseTable getRelationTable(ClassDescriptor referenceDescriptor) {
        return null;
    }
}
