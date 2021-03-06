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
*     bdoughan - August 7/2009 - 2.0 - Initial implementation
******************************************************************************/ 
package org.eclipse.persistence.testing.oxm.mappings.choicecollection.converter;

import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.mappings.XMLChoiceCollectionMapping;
import org.eclipse.persistence.oxm.mappings.XMLChoiceObjectMapping;
import org.eclipse.persistence.oxm.mappings.XMLDirectMapping;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.testing.oxm.mappings.choicecollection.Address;
import org.eclipse.persistence.testing.oxm.mappings.choicecollection.Employee;

public class EmployeeProject extends Project {

    public EmployeeProject() {
        addDescriptor(getEmployeeDescriptor());
        addDescriptor(getAddressDescriptor());
    }

    private XMLDescriptor getEmployeeDescriptor() {
        XMLDescriptor descriptor = new XMLDescriptor();
        descriptor.setJavaClass(Employee.class);
        descriptor.setDefaultRootElement("employee");

        XMLDirectMapping nameMapping = new XMLDirectMapping();
        nameMapping.setAttributeName("name");
        nameMapping.setXPath("name/text()");
        descriptor.addMapping(nameMapping);

        XMLChoiceCollectionMapping choiceMapping = new XMLChoiceCollectionMapping();
        choiceMapping.setAttributeName("choice");
        choiceMapping.addChoiceElement("street/text()", String.class);
        choiceMapping.addChoiceElement("address", Address.class);
        choiceMapping.addChoiceElement("integer/text()", Integer.class);
        choiceMapping.addChoiceElement("simpleAddress", Object.class);
        choiceMapping.setConverter(new WrapperConverter());
        descriptor.addMapping(choiceMapping);

        XMLDirectMapping phoneMapping = new XMLDirectMapping();
        phoneMapping.setAttributeName("phone");
        phoneMapping.setXPath("phone/text()");
        descriptor.addMapping(phoneMapping);

        return descriptor;
    }

    private XMLDescriptor getAddressDescriptor() {
        XMLDescriptor descriptor = new XMLDescriptor();
        descriptor.setJavaClass(Address.class);

        XMLDirectMapping streetMapping = new XMLDirectMapping();
        streetMapping.setAttributeName("street");
        streetMapping.setXPath("street/text()");
        descriptor.addMapping(streetMapping);

        XMLDirectMapping cityMapping = new XMLDirectMapping();
        cityMapping.setAttributeName("city");
        cityMapping.setXPath("city/text()");
        descriptor.addMapping(cityMapping);

        return descriptor;
    }

}