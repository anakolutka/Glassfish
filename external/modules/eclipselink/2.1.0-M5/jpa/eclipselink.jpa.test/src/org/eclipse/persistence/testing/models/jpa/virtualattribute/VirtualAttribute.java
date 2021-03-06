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

package org.eclipse.persistence.testing.models.jpa.virtualattribute;

import javax.persistence.*;
import static javax.persistence.GenerationType.*;

@Entity
@Table(name="CMP3_VIRTUAL")
public class VirtualAttribute  {

    private int id;
    private String description;
    
    @Id
    @GeneratedValue(strategy=TABLE, generator="VIRTUAL_ATTRIBUTE_TABLE_GENERATOR")
	@TableGenerator(
        name="VIRTUAL_ATTRIBUTE_TABLE_GENERATOR", 
        table="CMP3_VIRTUAL_SEQ", 
        pkColumnName="SEQ_NAME", 
        valueColumnName="SEQ_COUNT",
        pkColumnValue="VIRTUAL_ATTRIBUTE_SEQ"
    )
	@Column(name="CMP3_VIRTUALID")
    public int getId(){
        return id;
    }
    
    public void setId(int id){
        this.id = id;
    }
    
    public String getDescription(){
        return description;
    }
    
    public void setDescription(String description){
        this.description = description;
    }
}
