// $Id: ValidatorFactoryImpl.java 17860 2009-10-28 12:14:31Z hardy.ferentschik $
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
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
package org.hibernate.validator.engine;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ConfigurationState;

import org.hibernate.validator.metadata.AnnotationIgnores;
import org.hibernate.validator.metadata.BeanMetaDataCache;
import org.hibernate.validator.metadata.BeanMetaDataImpl;
import org.hibernate.validator.metadata.ConstraintDescriptorImpl;
import org.hibernate.validator.metadata.ConstraintHelper;
import org.hibernate.validator.metadata.ConstraintOrigin;
import org.hibernate.validator.metadata.MetaConstraint;
import org.hibernate.validator.util.ReflectionHelper;
import org.hibernate.validator.xml.XmlMappingParser;

/**
 * Factory returning initialized <code>Validator</code> instances.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class ValidatorFactoryImpl implements ValidatorFactory {

	private final MessageInterpolator messageInterpolator;
	private final TraversableResolver traversableResolver;
	private final ConstraintValidatorFactory constraintValidatorFactory;
	private final ConstraintHelper constraintHelper;
	private final BeanMetaDataCache beanMetaDataCache;

	public ValidatorFactoryImpl(ConfigurationState configurationState) {
		this.messageInterpolator = configurationState.getMessageInterpolator();
		this.constraintValidatorFactory = configurationState.getConstraintValidatorFactory();
		this.traversableResolver = configurationState.getTraversableResolver();
		this.constraintHelper = new ConstraintHelper();
		this.beanMetaDataCache = new BeanMetaDataCache();

		initBeanMetaData( configurationState.getMappingStreams() );
	}

	public Validator getValidator() {
		return usingContext().getValidator();
	}

	public MessageInterpolator getMessageInterpolator() {
		return messageInterpolator;
	}

	public TraversableResolver getTraversableResolver() {
		return traversableResolver;
	}

	public ConstraintValidatorFactory getConstraintValidatorFactory() {
		return constraintValidatorFactory;
	}

	public <T> T unwrap(Class<T> type) {
		throw new ValidationException( "Type " + type + " not supported" );
	}

	public ValidatorContext usingContext() {
		return new ValidatorContextImpl(
				constraintValidatorFactory,
				messageInterpolator,
				traversableResolver,
				constraintHelper,
				beanMetaDataCache
		);
	}

	private <T> void initBeanMetaData(Set<InputStream> mappingStreams) {

		XmlMappingParser mappingParser = new XmlMappingParser( constraintHelper );
		mappingParser.parse( mappingStreams );

		Set<Class<?>> processedClasses = mappingParser.getProcessedClasses();
		AnnotationIgnores annotationIgnores = mappingParser.getAnnotationIgnores();
		for ( Class<?> clazz : processedClasses ) {
			@SuppressWarnings("unchecked")
			Class<T> beanClass = ( Class<T> ) clazz;
			BeanMetaDataImpl<T> metaData = new BeanMetaDataImpl<T>(
					beanClass, constraintHelper, annotationIgnores, beanMetaDataCache
			);

			List<Class<?>> classes = new ArrayList<Class<?>>();
			ReflectionHelper.computeClassHierarchy( beanClass, classes );
			for ( Class<?> classInHierarchy : classes ) {
				if ( processedClasses.contains( classInHierarchy ) ) {
					addXmlConfiguredConstraintToMetaData( mappingParser, beanClass, classInHierarchy, metaData );
				}
			}

			if ( !mappingParser.getDefaultSequenceForClass( beanClass ).isEmpty() ) {
				metaData.setDefaultGroupSequence( mappingParser.getDefaultSequenceForClass( beanClass ) );
			}

			beanMetaDataCache.addBeanMetaData( beanClass, metaData );
		}
	}

	@SuppressWarnings("unchecked")
	private <T, A extends Annotation> void addXmlConfiguredConstraintToMetaData(XmlMappingParser mappingParser, Class<T> rootClass, Class<?> hierarchyClass, BeanMetaDataImpl<T> metaData) {
		for ( MetaConstraint<?, ? extends Annotation> constraint : mappingParser.getConstraintsForClass( hierarchyClass ) ) {
			ConstraintOrigin definedIn = definedIn( rootClass, hierarchyClass );
			ConstraintDescriptorImpl<A> descriptor = new ConstraintDescriptorImpl<A>(
					( A ) constraint.getDescriptor().getAnnotation(),
					constraintHelper,
					constraint.getElementType(),
					definedIn
			);
			MetaConstraint<T, A> newMetaConstraint = new MetaConstraint<T, A>(
					rootClass, constraint.getMember(), descriptor
			);
			metaData.addMetaConstraint( hierarchyClass, newMetaConstraint );
		}

		for ( Member m : mappingParser.getCascadedMembersForClass( hierarchyClass ) ) {
			metaData.addCascadedMember( m );
		}
	}

	private ConstraintOrigin definedIn(Class<?> rootClass, Class<?> hierarchyClass) {
		if ( hierarchyClass.equals( rootClass ) ) {
			return ConstraintOrigin.DEFINED_LOCALLY;
		}
		else {
			return ConstraintOrigin.DEFINED_IN_HIERARCHY;
		}
	}
}
