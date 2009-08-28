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
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Represents a Class
 * 
 * @author Pete Muir
 * 
 */
public interface WBClass<T> extends WBType<T>
{

   /**
    * Gets all fields on the type
    * 
    * @return A set of abstracted fields
    */
   public Set<WBField<?>> getFields();
   
   /**
    * Gets all fields on the type
    * 
    * @return A set of abstracted fields
    */
   public Set<WBMethod<?>> getMethods();

   /**
    * Get a field by name
    * 
    * @param <F> the expected type of the field
    * @param fieldName the field name
    * @param expectedType the expected type of the field
    * @return the field
    */
   public <F> WBField<F> getDeclaredField(String fieldName, WBClass<F> expectedType);

   /**
    * Gets all fields which are annotated with the given annotation type on this
    * class and all super classes
    * 
    * @param annotationType The annotation to match
    * @return A set of abstracted fields with the given annotation. Returns an
    *         empty set if there are no matches
    */
   public Set<WBField<?>> getAnnotatedFields(Class<? extends Annotation> annotationType);

   /**
    * Gets all fields which are annotated with the given annotation type on this
    * class only.
    * 
    * @param annotationType The annotation to match
    * @return A set of abstracted fields with the given annotation. Returns an
    *         empty set if there are no matches
    */
   public Set<WBField<?>> getDeclaredAnnotatedFields(Class<? extends Annotation> annotationType);

   /**
    * Gets all fields which are meta-annotated with metaAnnotationType
    * 
    * @param metaAnnotationType The meta annotation to match
    * @return A set of abstracted fields with the given meta-annotation. Returns
    *         an empty set if there are no matches
    */
   public Set<WBField<?>> getMetaAnnotatedFields(Class<? extends Annotation> metaAnnotationType);

   /**
    * Gets all constructors which are annotated with annotationType
    * 
    * @param annotationType The annotation type to match
    * @return A set of abstracted fields with the given annotation. Returns an
    *         empty set if there are no matches
    */
   public Set<WBConstructor<T>> getAnnotatedConstructors(Class<? extends Annotation> annotationType);

   /**
    * Gets all constructors
    * 
    * @return A set of abstracted constructors
    */
   public Set<WBConstructor<T>> getConstructors();

   /**
    * Gets the no-args constructor
    * 
    * @return The no-args constructor, or null if not defined
    */
   public WBConstructor<T> getNoArgsConstructor();

   /**
    * Get the constructor which matches the argument list provided
    * 
    * @param parameterTypes the parameters of the constructor
    * @return the matching constructor, or null if not defined
    */
   public WBConstructor<T> getDeclaredConstructor(ConstructorSignature signature);

   /**
    * Gets all methods annotated with annotationType
    * 
    * @param annotationType The annotation to match
    * @return A set of abstracted methods with the given annotation. Returns an
    *         empty set if there are no matches
    */
   public Set<WBMethod<?>> getAnnotatedMethods(Class<? extends Annotation> annotationType);

   /**
    * Gets all methods annotated with annotationType
    * 
    * @param annotationType The annotation to match
    * @return A set of abstracted methods with the given annotation. Returns an
    *         empty set if there are no matches
    */
   public Set<WBMethod<?>> getDeclaredAnnotatedMethods(Class<? extends Annotation> annotationType);

   /**
    * Find the annotated method for a given methodDescriptor
    * 
    * @param methodDescriptor
    * @return
    * 
    * TODO Replace with AnnotatedMethod variant
    */
   @Deprecated
   public WBMethod<?> getMethod(Method method);

   /**
    * Get a method by name
    * 
    * @param <M> the expected return type
    * @param signature the name of the method
    * @param expectedReturnType the expected return type
    * @return the method, or null if it doesn't exist
    */
   public <M> WBMethod<M> getDeclaredMethod(MethodSignature signature, WBClass<M> expectedReturnType);
   
   /**
    * Get a method by name
    * 
    * @param <M> the expected return type
    * @param signature the name of the method
    * @return the method, or null if it doesn't exist
    */
   public <M> WBMethod<M> getMethod(MethodSignature signature);

   // TODO Replace with AnnotatedMethod variant
   @Deprecated
   public WBMethod<?> getDeclaredMethod(Method method);

   /**
    * Gets all with parameters annotated with annotationType
    * 
    * @param annotationType The annotation to match
    * @return A set of abstracted methods with the given annotation. Returns an
    *         empty set if there are no matches
    */
   public Set<WBMethod<?>> getMethodsWithAnnotatedParameters(Class<? extends Annotation> annotationType);

   /**
    * Gets all with constructors annotated with annotationType
    * 
    * @param annotationType The annotation to match
    * @return A set of abstracted constructors with the given annotation. Returns an
    *         empty set if there are no matches
    */
   public Set<WBConstructor<?>> getConstructorsWithAnnotatedParameters(Class<? extends Annotation> annotationType);

   /**
    * Gets all with parameters annotated with annotationType
    * 
    * @param annotationType The annotation to match
    * @return A set of abstracted methods with the given annotation. Returns an
    *         empty set if there are no matches
    */
   public Set<WBMethod<?>> getDeclaredMethodsWithAnnotatedParameters(Class<? extends Annotation> annotationType);

   /**
    * Gets the superclass.
    * 
    * @return The abstracted superclass, null if there is no superclass
    */
   public WBClass<?> getSuperclass();

   /**
    * Determine if this is a non-static member class
    *
    * @return true if this is a non-static member  
    */
   public boolean isNonStaticMemberClass();

   public boolean isParameterizedType();

   public boolean isAbstract();

   public boolean isEnum();

   public <S> S cast(Object object);

   public <U> WBClass<? extends U> asSubclass(WBClass<U> clazz);

}