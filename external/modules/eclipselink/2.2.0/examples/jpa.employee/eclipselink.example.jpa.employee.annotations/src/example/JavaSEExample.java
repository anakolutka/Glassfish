/*******************************************************************************
 * Copyright (c) 2007, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *      dclarke - JPA Employee example using XML (bug 217884)
 ******************************************************************************/
package example;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import example.util.ExamplePropertiesLoader;

public class JavaSEExample {

    public static void main(String[] args) {
        Map<String, Object> properties = new HashMap<String, Object>();
        ExamplePropertiesLoader.loadProperties(properties);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("employee-xml", properties);

        EntityManager em = emf.createEntityManager();

        try {
            
            // Transactions examples
            Transactions transactions = new Transactions();
            
            // Create using persist
            em.getTransaction().begin();
            transactions.createUsingPersist(em);
            em.getTransaction().rollback();
            
            // Create using merge
            em.getTransaction().begin();
            transactions.createUsingMerge(em);
            em.getTransaction().rollback();

            // Serialize Partial and Merge
            em.getTransaction().begin();
            transactions.loadGroupSerializeMerge(em);
            em.getTransaction().rollback();
            em.clear();
            emf.getCache().evictAll();
            
            // Copy Partial and Merge
            em.getTransaction().begin();
            transactions.copyGroupMerge(em);
            em.getTransaction().rollback();
            em.clear();
            emf.getCache().evictAll();

        } finally {
            em.close();
            emf.close();
        }
    }
    
}
