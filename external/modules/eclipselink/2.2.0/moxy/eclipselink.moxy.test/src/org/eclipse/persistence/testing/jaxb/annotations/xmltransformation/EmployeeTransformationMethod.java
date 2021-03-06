/*******************************************************************************
 * Copyright (c) 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Oracle = 2.2 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.annotations.xmltransformation;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.eclipse.persistence.oxm.annotations.XmlReadTransformer;
import org.eclipse.persistence.oxm.annotations.XmlTransformation;
import org.eclipse.persistence.oxm.annotations.XmlWriteTransformer;
import org.eclipse.persistence.oxm.annotations.XmlWriteTransformers;
import org.eclipse.persistence.sessions.Record;

@XmlRootElement(name="employee")
public class EmployeeTransformationMethod {
    public String name;
    
    @XmlTransformation
    @XmlReadTransformer(method = "buildAttributeValue")
    @XmlWriteTransformers({
        @XmlWriteTransformer(method = "getStartTime", xmlPath= "normal-hours/start-time/text()"),
        @XmlWriteTransformer(method = "getEndTime", xmlPath="normal-hours/end-time/text()")
    })
    public String[] normalHours;
    
    @XmlTransient
    public String getStartTime() {
        return normalHours[0];
    }
    
    @XmlTransient
    public String getEndTime() {
        return normalHours[1];
    }
    public boolean equals(Object obj) {
        if(!(obj instanceof EmployeeTransformationMethod)) {
            return false;
        }
        EmployeeTransformationMethod emp = (EmployeeTransformationMethod)obj;
        if(getStartTime() == emp.getStartTime() && getEndTime() == emp.getEndTime()) {
            return true;
        }
        return (getStartTime().equalsIgnoreCase(emp.getStartTime()) && getEndTime().equalsIgnoreCase(emp.getEndTime()) && name.equalsIgnoreCase(emp.name));

    }
    
    public String[] buildAttributeValue(Record record) {
        String startTime = (String)record.get("normal-hours/start-time/text()");
        String endTime = (String)record.get("normal-hours/end-time/text()");
        
        return new String[]{startTime, endTime};        
    }

}