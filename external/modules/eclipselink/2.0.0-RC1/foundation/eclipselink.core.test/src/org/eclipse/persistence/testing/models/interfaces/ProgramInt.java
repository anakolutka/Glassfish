/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.models.interfaces;

import java.io.*;

/**
 * Commercial and show are programs.
 */
public interface ProgramInt extends Serializable {
    String getDescription();

    Number getDuration();

    String getName();

    void setDescription(String description);

    void setDuration(java.math.BigDecimal duration);

    void setName(String name);
}
