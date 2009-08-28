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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Represents a Class Constructor
 * 
 * @author Pete Muir
 * 
 */
public interface AnnotatedConstructor<T> extends AnnotatedMember<T, Constructor<T>>
{

   /**
    * Gets all parameters to the constructor
    * 
    * @return A set of abstracted parameters. Returns an empty set if there are
    *         no parameters
    */
   public List<? extends AnnotatedParameter<?>> getParameters();

   /**
    * Gets all parameters to the constructor which are annotated with
    * annotationType
    * 
    * @param annotationType A annotation to match
    * @return A list of abstracted parameters with the given annotation type.
    *         Returns an empty set if there are no matches.
    */
   public List<AnnotatedParameter<?>> getAnnotatedParameters(Class<? extends Annotation> annotationType);

   /**
    * Creates a new instance of the class, using this constructor
    * 
    * @return The created instance
    */
   public T newInstance(Object... parameters) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException;

   /**
    * Gets the declaring class of the annotation
    * 
    * @return An abstraction of the declaring class
    */
   public AnnotatedType<T> getDeclaringClass();
   
   public ConstructorSignature getSignature();

}
