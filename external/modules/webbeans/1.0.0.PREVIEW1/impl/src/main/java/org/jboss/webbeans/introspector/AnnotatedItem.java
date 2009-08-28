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

package org.jboss.webbeans.introspector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Stereotype;
import javax.context.ScopeType;
import javax.inject.BindingType;
import javax.inject.DeploymentType;

/**
 * AnnotatedItem provides a uniform access to the annotations on an annotated
 * item defined either in Java or XML
 * 
 * @author Pete Muir
 * 
 */
public interface AnnotatedItem<T, S>
{
   
// The set of meta-annotations to map
   @SuppressWarnings("unchecked")
   public static final Set<Class<? extends Annotation>> MAPPED_METAANNOTATIONS = new HashSet<Class<? extends Annotation>>(Arrays.asList(BindingType.class, DeploymentType.class, Stereotype.class, ScopeType.class));
   
   /**
    * Gets all annotations on the item
    * 
    * @return A set of annotations. Returns an empty set if there are no
    *         matches.
    */
   public <A extends Annotation> Set<A> getAnnotationsAsSet();

   /**
    * Gets all annotations which are annotated with the given meta annotation
    * type
    * 
    * @param The meta annotation to match
    * @return A set of matching meta-annotations. Returns an empty set if there
    *         are no matches.
    */
   public Set<Annotation> getMetaAnnotations(Class<? extends Annotation> metaAnnotationType);
   
   /**
    * Gets all annotations which are declared on this annotated item 
    * with the given meta annotation type
    * 
    * @param The meta annotation to match
    * @return A set of matching meta-annotations. Returns an empty set if there
    *         are no matches.
    */
   public Set<Annotation> getDeclaredMetaAnnotations(Class<? extends Annotation> metaAnnotationType);

   /**
    * Gets all annotations which are annotated with the given meta annotation
    * type
    * 
    * @param The meta annotation to match
    * @return An array of matching meta-annotations. Returns an empty array if
    *         there are no matches.
    */
   public Annotation[] getMetaAnnotationsAsArray(Class<? extends Annotation> metaAnnotationType);

   /**
    * Gets the binding types for this element
    * 
    * @returns A set of binding types present on the type. Returns an empty set
    *          if there are no matches.
    * @deprecated This reflection type should not know about JSR-299 binding types
    */
   public Set<Annotation> getBindings();

   /**
    * Gets the binding types for this element
    * 
    * @returns An array of binding types present on the type. Returns an empty
    *          array if there are no matches.
    * @deprecated This reflection type should not know about JSR-299 binding types
    */
   public Annotation[] getBindingsAsArray();

   /**
    * Gets an annotation for the annotation type specified.
    * 
    * @param annotationType The annotation to match
    * @return An annotation if found, null if the annotation wasn't present.
    */
   public <A extends Annotation> A getAnnotation(Class<A> annotationType);
   
   public Set<? extends Type> getFlattenedTypeHierarchy();

   /**
    * Indicates if an annotation type specified is present
    * 
    * @param annotationType The annotation to match
    * @return True if present, false if not
    */
   public boolean isAnnotationPresent(Class<? extends Annotation> annotationType);
   
   /**
    * Indicates if an annotation type specified is present
    * 
    * @param annotationType The annotation to match
    * @return True if present, false if not
    */
   public boolean isDeclaredAnnotationPresent(Class<? extends Annotation> annotationType);

   /**
    * Gets the type of the element
    * 
    * @return The type of the element
    */
   public Class<T> getRawType();
   
   public Type getType();

   /**
    * Extends Java Class assignability such that actual type parameters are also
    * considered
    * 
    * @param that The other item to check assignability against
    * @return True if assignable, false otherwise.
    */
   public boolean isAssignableFrom(AnnotatedItem<?, ?> that);

   /**
    * Checks if any of the types provided are assignable to this, using
    * the extended assignability algorithm provided by AnnotatedItem.
    * 
    * The types are assumed to contain their own actual type parameterization.
    * 
    * @param types The set of types to match
    * @return True if assignable, false otherwise.
    */
   public boolean isAssignableFrom(Set<? extends Type> types);

   /**
    * Gets the actual type arguments for any parameterized types that this
    * AnnotatedItem represents.
    * 
    * @return An array of type arguments
    */
   public Type[] getActualTypeArguments();

   /**
    * Indicates if this AnnotatedItem represents a static element
    * 
    * @return True if static, false otherwise
    */
   public boolean isStatic();

   /**
    * Indicates if this AnnotatedItem represents a final element    
    * @return True if final, false otherwise
    */
   public boolean isFinal();

   /**
    * Indicates if this AnnotatedItem can be proxyed
    * 
    * @return True if proxyable, false otherwise
    */
   public boolean isProxyable();
   
   /**
    * Indicates if this annotated item is public
    * 
    * @return if public, returns true
    */
   public boolean isPublic();

   /**
    * Gets the name of this AnnotatedItem
    * 
    * If it is not possible to determine the name of the underling element, a
    * IllegalArgumentException is thrown
    * 
    * @return The name
    */
   public String getName();
   
   public AnnotationStore getAnnotationStore();
   
   public boolean isParameterizedType();

}