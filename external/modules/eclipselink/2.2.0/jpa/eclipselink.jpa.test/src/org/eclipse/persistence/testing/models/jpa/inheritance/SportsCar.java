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


package org.eclipse.persistence.testing.models.jpa.inheritance;

import javax.persistence.*;

@Entity
@EntityListeners(org.eclipse.persistence.testing.models.jpa.inheritance.listeners.SportsCarListener.class)
@Table(name="CMP3_SPORTS_CAR")
@DiscriminatorValue("SC")
@ExcludeDefaultListeners
public class SportsCar extends Car {
    public static int PRE_PERSIST_COUNT = 0;
    
    private int maxSpeed;

    @Column(name="MAX_SPEED")
    public int getMaxSpeed() {
        return maxSpeed;
    }

    @PrePersist
	public void prePersist() {
        ++PRE_PERSIST_COUNT;
	}
    
    public void setMaxSpeed(int speed) {
        maxSpeed = speed;
    }
}
