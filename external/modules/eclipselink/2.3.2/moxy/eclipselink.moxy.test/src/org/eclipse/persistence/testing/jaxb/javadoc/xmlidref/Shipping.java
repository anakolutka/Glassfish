/*******************************************************************************
 * Copyright (c) 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Praba Vijayaratnam - 2.3 - initial implementation
 ******************************************************************************/ 
package org.eclipse.persistence.testing.jaxb.javadoc.xmlidref;

import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="shipping")
public class Shipping {

    @XmlIDREF
    public Customer customer;
    
    public void setCustomer(Customer customer){
    	this.customer = customer;
    }

    
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Shipping)) {
            return false;
        }
        Shipping s = (Shipping) obj;
        if (this.customer == null) {
            return s.customer == null;
        }
        if (s.customer == null) {
            return false;
        }
        return true;
    }
}
