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
 *      dclarke - Oracle Spatial Example (Bug 211007) Initial Contribution
 ******************************************************************************/
package testing.spatial.simple;

import org.junit.Test;

/**
 * SQL samples from C:\oracle\db\10.2\md\demo\examples\eginsert.sql Note: Table
 * re-named from TEST81 to SIMPLE_SPATIAL
 */
public class CreateTests extends SpatialTestCase {

    @Test
    public void populate_SRID_0() throws Exception {
        populateSamples(getEntityManager(), 0);
    }

    //@Test
    public void populate_SRID_1338() throws Exception {
        populateSamples(getEntityManager(), 1338);
        
    }

}
