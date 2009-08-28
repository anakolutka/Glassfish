// $Id: ResourceBundleMessageInterpolatorTest.java 17242 2009-08-06 13:04:10Z hardy.ferentschik $
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
package org.hibernate.validation.engine.messageinterpolation;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import javax.validation.MessageInterpolator;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.hibernate.validation.engine.MessageInterpolatorContext;
import org.hibernate.validation.engine.ResourceBundleMessageInterpolator;
import org.hibernate.validation.metadata.ConstraintDescriptorImpl;
import org.hibernate.validation.metadata.ConstraintHelper;
import org.hibernate.validation.util.annotationfactory.AnnotationDescriptor;
import org.hibernate.validation.util.annotationfactory.AnnotationFactory;

/**
 * Tests for message interpolation.
 *
 * @author Hardy Ferentschik
 */
public class ResourceBundleMessageInterpolatorTest {

	private ResourceBundleMessageInterpolator interpolator;
	private NotNull notNull;
	private ConstraintDescriptorImpl<NotNull> notNullDescriptor;
	private Size size;
	private ConstraintDescriptorImpl<Size> sizeDescriptor;

	@BeforeTest
	public void setUp() {
		// Create some annotations for testing using AnnotationProxies
		AnnotationDescriptor<NotNull> descriptor = new AnnotationDescriptor<NotNull>( NotNull.class );
		notNull = AnnotationFactory.create( descriptor );
		notNullDescriptor = new ConstraintDescriptorImpl<NotNull>(
				notNull, new ConstraintHelper()
		);

		AnnotationDescriptor<Size> sizeAnnotationDescriptor = new AnnotationDescriptor<Size>( Size.class );
		size = AnnotationFactory.create( sizeAnnotationDescriptor );
		sizeDescriptor = new ConstraintDescriptorImpl<Size>(
				size, new ConstraintHelper()
		);
	}

	@Test
	public void testSuccessfulInterpolation() {
		interpolator = new ResourceBundleMessageInterpolator( new TestResourceBundle() );
		MessageInterpolator.Context context = new MessageInterpolatorContext( notNullDescriptor, null );
		String expected = "message interpolation successfull";
		String actual = interpolator.interpolate( "{simple.key}", context );
		assertEquals( actual, expected, "Wrong substitution" );

		expected = "message interpolation successfull message interpolation successfull";
		actual = interpolator.interpolate( "{simple.key} {simple.key}", context );
		assertEquals( actual, expected, "Wrong substitution" );

		expected = "The message interpolation successfull completed";
		actual = interpolator.interpolate( "The {simple.key} completed", context );
		assertEquals( actual, expected, "Wrong substitution" );

		expected = "{{simple.key}}";
		actual = interpolator.interpolate( "{{simple.key}}", context );
		assertEquals( actual, expected, "Wrong substitution" );
	}

	@Test
	public void testMessageLiterals() {

		interpolator = new ResourceBundleMessageInterpolator( new TestResourceBundle() );
		MessageInterpolator.Context context = new MessageInterpolatorContext( notNullDescriptor, null );

		String expected = "{";
		String actual = interpolator.interpolate( "\\{", context );
		assertEquals( actual, expected, "Wrong substitution" );

		expected = "}";
		actual = interpolator.interpolate( "\\}", context );
		assertEquals( actual, expected, "Wrong substitution" );

		expected = "\\";
		actual = interpolator.interpolate( "\\", context );
		assertEquals( actual, expected, "Wrong substitution" );
	}

	@Test
	public void testUnSuccessfulInterpolation() {
		interpolator = new ResourceBundleMessageInterpolator( new TestResourceBundle() );
		MessageInterpolator.Context context = new MessageInterpolatorContext( notNullDescriptor, null );

		String expected = "foo";  // missing {}
		String actual = interpolator.interpolate( "foo", context );
		assertEquals( actual, expected, "Wrong substitution" );

		expected = "#{foo  {}";
		actual = interpolator.interpolate( "#{foo  {}", context );
		assertEquals( actual, expected, "Wrong substitution" );
	}

	@Test
	public void testUnkownTokenInterpolation() {
		interpolator = new ResourceBundleMessageInterpolator( new TestResourceBundle() );
		MessageInterpolator.Context context = new MessageInterpolatorContext( notNullDescriptor, null );

		String expected = "{bar}";  // unkown token {}
		String actual = interpolator.interpolate( "{bar}", context );
		assertEquals( actual, expected, "Wrong substitution" );
	}

	@Test
	public void testKeyWithDashes() {
		interpolator = new ResourceBundleMessageInterpolator( new TestResourceBundle() );
		MessageInterpolator.Context context = new MessageInterpolatorContext( notNullDescriptor, null );

		String expected = "message interpolation successfull";  // unkown token {}
		String actual = interpolator.interpolate( "{key-with-dashes}", context );
		assertEquals( actual, expected, "Wrong substitution" );
	}

	@Test
	public void testKeyWithSpaces() {
		interpolator = new ResourceBundleMessageInterpolator( new TestResourceBundle() );
		MessageInterpolator.Context context = new MessageInterpolatorContext( notNullDescriptor, null );

		String expected = "message interpolation successfull";  // unkown token {}
		String actual = interpolator.interpolate( "{key with spaces}", context );
		assertEquals( actual, expected, "Wrong substitution" );
	}

	@Test
	public void testDefaultInterpolation() {
		interpolator = new ResourceBundleMessageInterpolator( new TestResourceBundle() );
		MessageInterpolator.Context context = new MessageInterpolatorContext( notNullDescriptor, null );

		String expected = "may not be null";
		String actual = interpolator.interpolate( notNull.message(), context );
		assertEquals( actual, expected, "Wrong substitution" );

		expected = "size must be between 0 and 2147483647";  // unkown token {}
		context = new MessageInterpolatorContext( sizeDescriptor, null );
		actual = interpolator.interpolate( size.message(), context );
		assertEquals( actual, expected, "Wrong substitution" );
	}

	@Test
	public void testMessageInterpolationWithLocale() {
		interpolator = new ResourceBundleMessageInterpolator();

		String expected = "kann nicht null sein";
		MessageInterpolator.Context context = new MessageInterpolatorContext( notNullDescriptor, null );
		String actual = interpolator.interpolate( notNull.message(), context, Locale.GERMAN );
		assertEquals( actual, expected, "Wrong substitution" );
	}

	@Test
	public void testFallbackToDefaultLocale() {
		interpolator = new ResourceBundleMessageInterpolator();
		MessageInterpolator.Context context = new MessageInterpolatorContext( notNullDescriptor, null );

		String expected = "may not be null";
		String actual = interpolator.interpolate( notNull.message(), context, Locale.JAPAN );
		assertEquals( actual, expected, "Wrong substitution" );
	}

	@Test
	public void testUserResourceBundle() {
		interpolator = new ResourceBundleMessageInterpolator();
		MessageInterpolator.Context context = new MessageInterpolatorContext( notNullDescriptor, null );

		String expected = "no puede ser null";
		String actual = interpolator.interpolate( notNull.message(), context, new Locale( "es", "ES" ) );
		assertEquals( actual, expected, "Wrong substitution" );
	}

	/**
	 * HV-102
	 */
	@Test
	public void testRecursiveMessageInterpoliation() {
		AnnotationDescriptor<Max> descriptor = new AnnotationDescriptor<Max>( Max.class );
		descriptor.setValue( "message", "{replace.in.user.bundle1}" );
		descriptor.setValue( "value", 10l );
		Max max = AnnotationFactory.create( descriptor );


		ConstraintDescriptorImpl<Max> constraintDescriptor = new ConstraintDescriptorImpl<Max>(
				max, new ConstraintHelper()
		);

		interpolator = new ResourceBundleMessageInterpolator( new TestResourceBundle() );
		MessageInterpolator.Context context = new MessageInterpolatorContext( constraintDescriptor, null );

		String expected = "{replace.in.default.bundle2}";
		String actual = interpolator.interpolate( max.message(), context );
		assertEquals(
				actual, expected, "Within default bundle replacement parameter evauation should not be recursive!"
		);
	}

	/**
	 * HV-182
	 */
	@Test
	public void testCorrectMessageInterpolationIfParameterCannotBeReplaced() {
		AnnotationDescriptor<Max> descriptor = new AnnotationDescriptor<Max>( Max.class );
		String message = "Message should stay unchanged since {fubar} is not replacable";
		descriptor.setValue( "message", message );
		descriptor.setValue( "value", 10l );
		Max max = AnnotationFactory.create( descriptor );


		ConstraintDescriptorImpl<Max> constraintDescriptor = new ConstraintDescriptorImpl<Max>(
				max, new ConstraintHelper()
		);

		interpolator = new ResourceBundleMessageInterpolator( new TestResourceBundle() );
		MessageInterpolator.Context context = new MessageInterpolatorContext( constraintDescriptor, null );

		String actual = interpolator.interpolate( max.message(), context );
		assertEquals(
				actual, message, "The message should not have changed."
		);
	}

	/**
	 * A dummy resource bundle which can be passed to the constructor of ResourceBundleMessageInterpolator to replace
	 * the user specified resource bundle.
	 */
	class TestResourceBundle extends ResourceBundle implements Enumeration<String> {
		private Map<String, String> testResources;
		Iterator<String> iter;

		public TestResourceBundle() {
			testResources = new HashMap<String, String>();
			// add some test messages
			testResources.put( "simple.key", "message interpolation successfull" );
			testResources.put( "key-with-dashes", "message interpolation successfull" );
			testResources.put( "key with spaces", "message interpolation successfull" );
			testResources.put( "replace.in.user.bundle1", "{replace.in.user.bundle2}" );
			testResources.put( "replace.in.user.bundle2", "{replace.in.default.bundle1}" );

			iter = testResources.keySet().iterator();
		}

		public Object handleGetObject(String key) {
			return testResources.get( key );
		}

		public Enumeration<String> getKeys() {
			return this;
		}

		public boolean hasMoreElements() {
			return iter.hasNext();
		}

		public String nextElement() {
			if ( hasMoreElements() ) {
				return iter.next();
			}
			else {
				throw new NoSuchElementException();
			}
		}
	}
}
