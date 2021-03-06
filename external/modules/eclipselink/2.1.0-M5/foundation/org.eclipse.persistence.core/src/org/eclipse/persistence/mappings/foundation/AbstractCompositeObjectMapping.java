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
package org.eclipse.persistence.mappings.foundation;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.*;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.exceptions.*;
import org.eclipse.persistence.internal.descriptors.*;
import org.eclipse.persistence.internal.helper.*;
import org.eclipse.persistence.internal.queries.JoinedAttributeManager;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedClassForName;
import org.eclipse.persistence.internal.sessions.*;
import org.eclipse.persistence.mappings.*;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.queries.*;

/**
 * Chunks of data from non-relational data sources can have an
 * embedded component objects. These can be
 * mapped using this mapping. The format of the embedded
 * data is determined by the reference descriptor.
 *
 * @author Big Country
 * @since TOPLink/Java 3.0
 */
public abstract class AbstractCompositeObjectMapping extends AggregateMapping {

    /** The aggregate object is stored in a single field. */
    protected DatabaseField field;

    /** Allows user defined conversion between the object attribute value and the database value. */
    protected Converter converter;
    
    /**
     * Default constructor.
     */
    public AbstractCompositeObjectMapping() {
        super();
    }

    /**
     * INTERNAL:
     * Cascade perform delete through mappings that require the cascade
     */
    public void cascadePerformRemoveIfRequired(Object object, UnitOfWorkImpl uow, Map visitedObjects) {
        //objects referenced by this mapping are not registered as they have
        // no identity, however mappings from the referenced object may need cascading.
        Object objectReferenced = getRealAttributeValueFromObject(object, uow);
        if (objectReferenced == null) {
            return;
        }
        if (!visitedObjects.containsKey(objectReferenced)) {
            visitedObjects.put(objectReferenced, objectReferenced);
            ObjectBuilder builder = getReferenceDescriptor(objectReferenced.getClass(), uow).getObjectBuilder();
            builder.cascadePerformRemove(objectReferenced, uow, visitedObjects);
        }
    }
    
    /**
     * INTERNAL:
     * Cascade discover and persist new objects during commit.
     */
    public void cascadeDiscoverAndPersistUnregisteredNewObjects(Object object, Map newObjects, Map unregisteredExistingObjects, Map visitedObjects, UnitOfWorkImpl uow) {
        Object objectReferenced = getRealAttributeValueFromObject(object, uow);
        if (objectReferenced != null) {
            ObjectBuilder builder = getReferenceDescriptor(objectReferenced.getClass(), uow).getObjectBuilder();
            builder.cascadeRegisterNewForCreate(objectReferenced, uow, visitedObjects);
        }
    }
    
    /**
     * INTERNAL:
     * Cascade registerNew for Create through mappings that require the cascade
     */
    public void cascadeRegisterNewIfRequired(Object object, UnitOfWorkImpl uow, Map visitedObjects) {
        //aggregate objects are not registered but their mappings should be.
        Object objectReferenced = getRealAttributeValueFromObject(object, uow);
        if (objectReferenced == null) {
            return;
        }
        if (!visitedObjects.containsKey(objectReferenced)) {
            visitedObjects.put(objectReferenced, objectReferenced);
            ObjectBuilder builder = getReferenceDescriptor(objectReferenced.getClass(), uow).getObjectBuilder();
            builder.cascadeRegisterNewForCreate(objectReferenced, uow, visitedObjects);
        }
    }

    /**
     * Return the fields mapped by the mapping.
     */
    protected Vector collectFields() {
        Vector fields = new Vector(1);
        fields.addElement(this.getField());
        return fields;
    }

    /**
     * INTERNAL:
     * Return the value of an attribute which this mapping represents for an object.
     */
    public Object getAttributeValueFromObject(Object object) throws DescriptorException {
        try {
            Object attributeValue = getAttributeAccessor().getAttributeValueFromObject(object);
            if (attributeValue == null) {
                return null;
            }
            return attributeValue;
        } catch (DescriptorException exception) {
            exception.setMapping(this);
            throw exception;
        }
    }
    
    /**
     * PUBLIC:
     * Return the converter on the mapping.
     * A converter can be used to convert between the object's value and database value of the attribute.
     */
    public Converter getConverter() {
        return converter;
    }

    /**
     * INTERNAL:
     * The aggregate object is held in a single field.
     */
    public DatabaseField getField() {
        return field;
    }

    /**
     * PUBLIC:
     * Indicates if there is a converter on the mapping.
     */
    public boolean hasConverter() {
        return getConverter() != null;
    }

    /**
     * INTERNAL:
     */
    public boolean isAbstractCompositeObjectMapping() {
        return true;
    }

    /**
     * INTERNAL:
     * Initialize the mapping.
     */
    public void initialize(AbstractSession session) throws DescriptorException {
        super.initialize(session);

        if (getField() == null) {
            throw DescriptorException.fieldNameNotSetInMapping(this);
        }
        
        setField(getDescriptor().buildField(getField()));
        setFields(collectFields());
        // initialize the converter - if necessary
        if (hasConverter()) {
            getConverter().initialize(this, session);
        }
    }

    /**
     * INTERNAL:
     * Set the value of the attribute mapped by this mapping.
     */
    public void setAttributeValueInObject(Object object, Object value) throws DescriptorException {
        // PERF: Direct variable access.
        try {
            this.attributeAccessor.setAttributeValueInObject(object, value);
        } catch (DescriptorException exception) {
            exception.setMapping(this);
            throw exception;
        }
    }
    
    /**
     * PUBLIC:
     * Set the converter on the mapping.
     * A converter can be used to convert between the object's value and database value of the attribute.
     */
    public void setConverter(Converter converter) {
        this.converter = converter;
    }
    
    /**
     * The aggregate object is held in a single field.
     */
    public void setField(DatabaseField field) {
        this.field = field;
    }

    /**
     * INTERNAL:
     * Extract and return value of the field from the object
     */
    public Object valueFromObject(Object object, DatabaseField field, AbstractSession session) throws DescriptorException {
        Object attributeValue = this.getAttributeValueFromObject(object);
        if(this.getConverter() != null) {
            this.getConverter().convertObjectValueToDataValue(attributeValue, session);
        }
        if (attributeValue == null) {
            return null;
        } else {
            return this.getObjectBuilder(attributeValue, session).extractValueFromObjectForField(attributeValue, field, session);
        }
    }

    /**
     * INTERNAL:
     * Extract and return the aggregate object from
     * the specified row.
     */
    public Object valueFromRow(AbstractRecord row, JoinedAttributeManager joinManager, ObjectBuildingQuery sourceQuery, AbstractSession executionSession) throws DatabaseException {
        Object fieldValue = row.get(this.getField());

        // BUG#2667762 there could be whitespace in the row instead of null
        if ((fieldValue == null) || (fieldValue instanceof String)) {
            return null;
        }

        // pretty sure we can ignore inheritance here:
        AbstractRecord nestedRow = this.getReferenceDescriptor().buildNestedRowFromFieldValue(fieldValue);

        ClassDescriptor descriptor = this.getReferenceDescriptor();
        if (descriptor.hasInheritance()) {
            Class nestedElementClass = descriptor.getInheritancePolicy().classFromRow(nestedRow, executionSession);
            descriptor = this.getReferenceDescriptor(nestedElementClass, executionSession);
        }
        ObjectBuilder objectBuilder = descriptor.getObjectBuilder();
        Object toReturn = buildCompositeObject(objectBuilder, nestedRow, sourceQuery, joinManager);
        if(getConverter() != null) {
            toReturn = getConverter().convertDataValueToObjectValue(toReturn, executionSession);
        }
        return buildCompositeObject(objectBuilder, nestedRow, sourceQuery, joinManager);
    }

    /**
     * INTERNAL:
     * Builds a shallow original object.  Only direct attributes and primary
     * keys are populated.  In this way the minimum original required for
     * instantiating a working copy clone can be built without placing it in
     * the shared cache (no concern over cycles).
     */
    public void buildShallowOriginalFromRow(AbstractRecord row, Object original, JoinedAttributeManager joinManager, ObjectBuildingQuery sourceQuery, AbstractSession executionSession) {
        Object fieldValue = row.get(this.getField());

        // BUG#2667762 there could be whitespace in the row instead of null
        if ((fieldValue == null) || (fieldValue instanceof String)) {
            return;
        }

        // pretty sure we can ignore inheritance here:
        AbstractRecord nestedRow = this.getReferenceDescriptor().buildNestedRowFromFieldValue(fieldValue);

        ClassDescriptor descriptor = this.getReferenceDescriptor();
        if (descriptor.hasInheritance()) {
            Class nestedElementClass = descriptor.getInheritancePolicy().classFromRow(nestedRow, executionSession);
            descriptor = this.getReferenceDescriptor(nestedElementClass, executionSession);
        }
        ObjectBuilder objectBuilder = descriptor.getObjectBuilder();

        // instead of calling buildCompositeObject, which calls either objectBuilder.
        // buildObject or buildNewInstance and buildAttributesIntoObject, do the
        // following always.  Since shallow original no concern over cycles or caching.
        Object element = objectBuilder.buildNewInstance();
        objectBuilder.buildAttributesIntoShallowObject(element, nestedRow, sourceQuery);

        setAttributeValueInObject(original, element);
    }

    protected abstract Object buildCompositeObject(ObjectBuilder objectBuilder, AbstractRecord nestedRow, ObjectBuildingQuery query, JoinedAttributeManager joinManger);

    /**
     * INTERNAL:
     * Build the value for the database field and put it in the
     * specified database row.
     */
    public void writeFromObjectIntoRow(Object object, AbstractRecord record, AbstractSession session) throws DescriptorException {
        if (this.isReadOnly()) {
            return;
        }
        Object attributeValue = this.getAttributeValueFromObject(object);
        if(getConverter() != null) {
            getConverter().convertObjectValueToDataValue(attributeValue, session);
        }
        if (attributeValue == null) {
            record.put(this.getField(), null);
        } else {
            Object fieldValue = buildCompositeRow(attributeValue, session, record);
            record.put(this.getField(), fieldValue);
        }
    }

    protected abstract Object buildCompositeRow(Object attributeValue, AbstractSession session, AbstractRecord record);

    /**
     * INTERNAL:
     * If it has changed, build the value for the database field and put it in the
     * specified database row.
     * If any part of the aggregate object has changed, the entire object is
     * written to the database row (i.e. partial updates are not supported).
     */
    public void writeFromObjectIntoRowForUpdate(WriteObjectQuery query, AbstractRecord row) throws DescriptorException {
        if (query.getSession().isUnitOfWork()) {
            if (this.compareObjects(query.getObject(), query.getBackupClone(), query.getSession())) {
                return;// nothing has changed
            }
        }
        this.writeFromObjectIntoRow(query.getObject(), row, query.getSession());
    }

    /**
     * INTERNAL:
     * Get the attribute value from the object and add the appropriate
     * values to the specified database row.
     */
    public void writeFromObjectIntoRowWithChangeRecord(ChangeRecord changeRecord, AbstractRecord row, AbstractSession session) throws DescriptorException {
        Object object = ((ObjectChangeSet)changeRecord.getOwner()).getUnitOfWorkClone();
        this.writeFromObjectIntoRow(object, row, session);
    }

    /**
     * INTERNAL:
     * Write fields needed for insert into the template for with null values.
     */
    public void writeInsertFieldsIntoRow(AbstractRecord record, AbstractSession session) {
        if (this.isReadOnly()) {
            return;
        }
        record.put(this.getField(), null);
    }
    
    /**
     * INTERNAL:
     * Convert all the class-name-based settings in this mapping to actual class-based
     * settings. This method is used when converting a project that has been built
     * with class names to a project with classes.
     * @param classLoader 
     */
    public void convertClassNamesToClasses(ClassLoader classLoader){
        Class referenceClass = null;
        if (getReferenceClassName() != null) {
            try{        	
	            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
	                try {
	                    referenceClass = (Class)AccessController.doPrivileged(new PrivilegedClassForName(getReferenceClassName(), true, classLoader));
	                } catch (PrivilegedActionException exception) {
	                    throw ValidationException.classNotFoundWhileConvertingClassNames(getReferenceClassName(), exception.getException());
	                }
	            } else {                
	                referenceClass = org.eclipse.persistence.internal.security.PrivilegedAccessHelper.getClassForName(getReferenceClassName(), true, classLoader);                
	            }        	
	        } catch (ClassNotFoundException exc){
	            throw ValidationException.classNotFoundWhileConvertingClassNames(getReferenceClassName(), exc);
	        }    
            setReferenceClass(referenceClass);
        }
    }
}
