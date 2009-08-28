// $Id: MetaConstraint.java 17096 2009-07-15 12:28:36Z hardy.ferentschik $// $Id: MetaConstraint.java 17096 2009-07-15 12:28:36Z hardy.ferentschik $
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,  
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.validation.metadata;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.metadata.ConstraintDescriptor;

import org.hibernate.validation.engine.ConstraintTree;
import org.hibernate.validation.engine.GlobalExecutionContext;
import org.hibernate.validation.engine.LocalExecutionContext;
import org.hibernate.validation.util.ReflectionHelper;

/**
 * Instances of this class abstract the constraint type  (class, method or field constraint) and give access to
 * meta data about the constraint. This allows a unified handling of constraints in the validator imlpementation.
 *
 * @author Hardy Ferentschik
 */
public class MetaConstraint<T, A extends Annotation> {

	/**
	 * The member the constraint was defined on.
	 */
	private final Member member;

	/**
	 * The JavaBeans name for this constraint.
	 */
	private final String propertyName;

	/**
	 * Describes on which level (<code>TYPE</code>, <code>METHOD</code>, <code>FIELD</code>) the constraint was
	 * defined on.
	 */
	private final ElementType elementType;

	/**
	 * The class of the bean hosting this constraint.
	 */
	private final Class<T> beanClass;

	/**
	 * The constraint tree created from the constraint annotation.
	 */
	private final ConstraintTree<A> constraintTree;

	public MetaConstraint(Class<T> beanClass, ConstraintDescriptor<A> constraintDescriptor) {
		this.elementType = ElementType.TYPE;
		this.member = null;
		this.propertyName = "";
		this.beanClass = beanClass;
		constraintTree = new ConstraintTree<A>( constraintDescriptor );
	}

	public MetaConstraint(Member member, Class<T> beanClass, ConstraintDescriptor<A> constraintDescriptor) {
		if ( member instanceof Method ) {
			this.elementType = ElementType.METHOD;
		}
		else if ( member instanceof Field ) {
			this.elementType = ElementType.FIELD;
		}
		else {
			throw new IllegalArgumentException( "Non allowed member type: " + member );
		}
		this.member = member;
		this.propertyName = ReflectionHelper.getPropertyName( member );
		this.beanClass = beanClass;
		constraintTree = new ConstraintTree<A>( constraintDescriptor );
	}


	/**
	 * @return Returns the list of groups this constraint is part of. This might include the default group even when
	 *         it is not explicitly specified, but part of the redefined default group list of the hosting bean.
	 */
	public Set<Class<?>> getGroupList() {
		return constraintTree.getDescriptor().getGroups();
	}

	public ConstraintDescriptor getDescriptor() {
		return constraintTree.getDescriptor();
	}

	public Class<T> getBeanClass() {
		return beanClass;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public ElementType getElementType() {
		return elementType;
	}

	public ConstraintTree getConstraintTree() {
		return constraintTree;
	}

	public <T, U, V> boolean validateConstraint(GlobalExecutionContext<T> executionContext, LocalExecutionContext<U, V> localExecutionContext) {
		List<ConstraintViolation<T>> constraintViolations = new ArrayList<ConstraintViolation<T>>();
		localExecutionContext.setElementType( elementType );
		constraintTree.validateConstraints(
				typeOfAnnoatedElement(), executionContext, localExecutionContext, constraintViolations
		);
		if ( constraintViolations.size() > 0 ) {
			executionContext.addConstraintFailures( constraintViolations );
			return false;
		}
		return true;
	}

	/**
	 * @param o the object from which to retrieve the value.
	 *
	 * @return Returns the value for this constraint from the specified object. Depending on the type either the value itself
	 *         is returned of method or field access is used to access the value.
	 */
	public Object getValue(Object o) {
		switch ( elementType ) {
			case TYPE: {
				return o;
			}
			default: {
				return ReflectionHelper.getValue( member, o );
			}
		}
	}

	private Type typeOfAnnoatedElement() {
		Type t;
		switch ( elementType ) {
			case TYPE: {
				t = beanClass;
				break;
			}
			default: {
				t = ReflectionHelper.typeOf( member );
				if ( t instanceof Class && ( ( Class ) t ).isPrimitive() ) {
					t = ReflectionHelper.boxedTyp( t );
				}
			}
		}
		return t;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MetaConstraint" );
		sb.append( "{propertyName='" ).append( propertyName ).append( '\'' );
		sb.append( ", elementType=" ).append( elementType );
		sb.append( ", beanClass=" ).append( beanClass );
		sb.append( '}' );
		return sb.toString();
	}
}
