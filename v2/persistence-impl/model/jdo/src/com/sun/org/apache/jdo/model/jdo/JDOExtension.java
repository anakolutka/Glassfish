/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the "License").  You may not use this file except 
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt or 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html. 
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * HEADER in each file and include the License file at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable, 
 * add the following below this CDDL HEADER, with the 
 * fields enclosed by brackets "[]" replaced with your 
 * own identifying information: Portions Copyright [yyyy] 
 * [name of copyright owner]
 */

/*
 * Copyright 2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.sun.org.apache.jdo.model.jdo;

import com.sun.org.apache.jdo.model.ModelException;

/**
 * A JDOExtension instance represents a JDO vendor specific extension.
 * 
 * @author Michael Bouschen
 */
public interface JDOExtension 
{
    /**
     * Returns the vendor name of this vendor extension.
     */
    public String getVendorName();

    /**
     * Sets the vendor name for this vendor extension.
     * @exception ModelException if impossible
     */
    public void setVendorName(String vendorName)
        throws ModelException;
    
    /**
     * Returns the key of this vendor extension.
     */
    public String getKey();

    /**
     * Sets the key for this vendor extension.
     * @exception ModelException if impossible
     */
    public void setKey(String key)
        throws ModelException;
    
    /**
     * Returns the value of this vendor extension.
     */
    public Object getValue();

    /**
     * Sets the value for this vendor extension.
     * @exception ModelException if impossible
     */
    public void setValue(Object value)
        throws ModelException;
}
