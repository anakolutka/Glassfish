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

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import model.Address;
import model.Employee;
import model.Gender;

import org.eclipse.persistence.config.PessimisticLock;
import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.internal.helper.SerializationHelper;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.AttributeGroup;
import org.eclipse.persistence.sessions.CopyGroup;

public class Transactions {

    /**
     * New entities with new related related entities can be persisted using
     * <code>EntityManager.persist(newEntity)</code>. The cascade setting on the
     * mappings determine how the related entities are handled. In this case
     * Employee has its relationship to Address and PhoneNumber configured with
     * cascade-all so the associated new entities will also be persisted.
     */
    public Employee createUsingPersist(EntityManager em) {
        Employee emp = new Employee();
        emp.setFirstName("Sample");
        emp.setLastName("Employee");
        emp.setGender(Gender.Male);
        emp.setSalary(123456);

        Address address = new Address();
        emp.setAddress(address);

        emp.addPhoneNumber("Mobile", "613", "555-1212");

        em.persist(emp);

        em.flush();

        return emp;
    }

    /**
	 * 
	 */
    public Employee createUsingMerge(EntityManager em) {
        Employee emp = new Employee();
        emp.setFirstName("Sample");
        emp.setLastName("Employee");
        emp.setGender(Gender.Male);
        emp.setSalary(123456);

        Address address = new Address();
        emp.setAddress(address);

        emp.addPhoneNumber("Mobile", "613", "555-1212");

        // When merging the managed instance is returned from the call.
        // Further usage within the transaction must be done with this managed
        // entity.
        emp = em.merge(emp);

        em.flush();

        return emp;
    }

    /**
     * 
     * @param em
     * @throws Exception
     */
    public void pessimisticLocking(EntityManager em) throws Exception {

        // Find the Employee with the minimum ID
        int minId = Queries.minimumEmployeeId(em);

        em.getTransaction().begin();

        // Lock Employee using query with hint
        Employee emp = (Employee) em.createQuery("SELECT e FROM Employee e WHERE e.id = :ID").setParameter("ID", minId).setHint(QueryHints.PESSIMISTIC_LOCK, PessimisticLock.Lock).getSingleResult();

        emp.setSalary(emp.getSalary() - 1);

        em.flush();
    }

    /**
     * This example illustrates the use of a query returning an entity and data
     * from a related entity within a transaction. The returned entities are
     * managed and thus any changes are reflected in the database upon flush.
     * 
     * @param em
     * @throws Exception
     */
    public void updateEmployeeWithCity(EntityManager em) throws Exception {
        em.getTransaction().begin();

        List<Object[]> emps = em.createQuery("SELECT e, e.address.city FROM Employee e").getResultList();
        Employee emp = (Employee) emps.get(0)[0];
        emp.setSalary(emp.getSalary() + 1);

        em.flush();

        em.getTransaction().rollback();
    }

    /**
     * This example illustrates the use of a LoadPlan to retrieve a partial
     * Employee, serialize it (simulating detaching to another application tier)
     * and then after making changes merge it back.
     * 
     * @param em
     * @throws Exception
     */
    public Employee loadGroupSerializeMerge(EntityManager em) {
        // Search for an Employee with an Address and Phone Numbers
        TypedQuery<Employee> query = em.createQuery("SELECT e FROM Employee e WHERE e.address IS NOT NULL AND e.id IN (SELECT MIN(ee.id) FROM Employee ee)", Employee.class);

        // Load only its names and phone Numbers
        AttributeGroup group = new AttributeGroup();
        group.addAttribute("firstName");
        group.addAttribute("lastName");
        group.addAttribute("phoneNumbers");

        // Controls what is queried from the database initially
        query.setHint(QueryHints.FETCH_GROUP, group);
        // Controls what is populated in the results
        query.setHint(QueryHints.LOAD_GROUP, group);

        Employee emp = query.getSingleResult();

        System.out.println(">>> Fetch & Load query complete");

        // Detach Employee through Serialization
        Employee detachedEmp = (Employee) serialize(emp);

        // Modify the detached Employee inverting the names, adding a phone
        // number, and setting the salary
        detachedEmp.setFirstName(emp.getLastName());
        detachedEmp.setLastName(emp.getFirstName());
        detachedEmp.addPhoneNumber("TEST", "999", "999999");
        // NOte that salary was not part of the original FetchGroup
        detachedEmp.setSalary(123456.0);

        // Merge the detached employee
        em.merge(detachedEmp);

        System.out.println(">>> Sparse merge complete");

        // Flush the changes to the database
        em.flush();

        System.out.println(">>> Flush complete");

        return emp;
    }

    /**
     * TODO
     */
    public Employee copyGroupMerge(EntityManager em) {
        // Search for an Employee with an Address and Phone Numbers
        TypedQuery<Employee> query = em.createQuery("SELECT e FROM Employee e WHERE e.id IN (SELECT MIN(ee.id) FROM Employee ee)", Employee.class);

        Employee emp = query.getSingleResult();

        System.out.println(">>> Employee retrieved");

        // Copy only its names and phone Numbers
        AttributeGroup group = new CopyGroup();
        group.addAttribute("firstName");
        group.addAttribute("lastName");
        group.addAttribute("address");
        group.addAttribute("phoneNumbers");

        Employee empCopy = (Employee) em.unwrap(JpaEntityManager.class).copy(emp, group);
        System.out.println(">>> Employee copied");

        // Modify the detached Employee inverting the names, adding a phone
        // number, and setting the salary
        empCopy.setFirstName(emp.getLastName());
        empCopy.setLastName(emp.getFirstName());
        empCopy.getAddress().setCity(emp.getAddress().getCity() + "_");
        // Note that salary was not part of the original FetchGroup
        empCopy.setSalary(1.0);
        empCopy.addPhoneNumber("TEST", "999", "999999");

        // Merge the detached employee
        em.merge(empCopy);
        System.out.println(">>> Sparse merge complete");

        // Flush the changes to the database
        em.flush();

        System.out.println(">>> Flush complete");

        return emp;
    }

    private Object serialize(Serializable entity) {
        try {
            return SerializationHelper.clone(entity);
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize employee");
        }
    }
}
