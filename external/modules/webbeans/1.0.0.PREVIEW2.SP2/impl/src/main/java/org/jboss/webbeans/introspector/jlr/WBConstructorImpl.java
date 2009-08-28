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
package org.jboss.webbeans.introspector.jlr;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.jboss.webbeans.BeanManagerImpl;
import org.jboss.webbeans.introspector.AnnotationStore;
import org.jboss.webbeans.introspector.ConstructorSignature;
import org.jboss.webbeans.introspector.WBConstructor;
import org.jboss.webbeans.introspector.WBParameter;
import org.jboss.webbeans.introspector.WBType;
import org.jboss.webbeans.resources.ClassTransformer;
import org.jboss.webbeans.util.Names;

import com.google.common.base.Supplier;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

/**
 * Represents an annotated constructor
 * 
 * This class is immutable, and therefore threadsafe
 * 
 * @author Pete Muir
 * 
 * @param <T>
 */
public class WBConstructorImpl<T> extends AbstractWBMember<T, Constructor<T>> implements WBConstructor<T>
{
   
   // The underlying constructor
   private final Constructor<T> constructor;

   // The list of parameter abstractions
   private final List<WBParameter<?>> parameters;
   // The mapping of annotation -> parameter abstraction
   private final ListMultimap<Class<? extends Annotation>, WBParameter<?>> annotatedParameters;

   // The declaring class abstraction
   private final WBType<T> declaringClass;
   
   private final ConstructorSignature signature;

   // Cached string representation
   private String toString;
   
   public static <T> WBConstructor<T> of(Constructor<T> constructor, WBType<T> declaringClass, ClassTransformer classTransformer)
   {
      return new WBConstructorImpl<T>(constructor, declaringClass, classTransformer);
   }

   /**
    * Constructor
    * 
    * Initializes the superclass with the build annotations map
    * 
    * @param constructor The constructor method
    * @param declaringClass The declaring class
    */
   protected WBConstructorImpl(Constructor<T> constructor, WBType<T> declaringClass, ClassTransformer classTransformer)
   {
      super(AnnotationStore.of(constructor, classTransformer.getTypeStore()), constructor, constructor.getDeclaringClass(), constructor.getDeclaringClass());
      this.constructor = constructor;
      this.declaringClass = declaringClass;

      this.parameters = new ArrayList<WBParameter<?>>();
      annotatedParameters = Multimaps.newListMultimap(new HashMap<Class<? extends Annotation>, Collection<WBParameter<?>>>(), new Supplier< List<WBParameter<?>>>()
      {
         
         public List<WBParameter<?>> get()
         {
            return new ArrayList<WBParameter<?>>();
         }
        
      });
      
      for (int i = 0; i < constructor.getParameterTypes().length; i++)
      {
         if (constructor.getParameterAnnotations()[i].length > 0)
         {
            Class<?> clazz = constructor.getParameterTypes()[i];
            Type type = constructor.getGenericParameterTypes()[i];
            WBParameter<?> parameter = WBParameterImpl.of(constructor.getParameterAnnotations()[i], clazz, type, this, classTransformer);
            parameters.add(parameter);

            for (Annotation annotation : parameter.getAnnotations())
            {
               annotatedParameters.put(annotation.annotationType(), parameter);
            }
         }
         else
         {
            Class<?> clazz = constructor.getParameterTypes()[i];
            Type type;
            if (constructor.getGenericParameterTypes().length > i)
            {
               type = constructor.getGenericParameterTypes()[i];
            }
            else
            {
               type = clazz;
            }
            WBParameter<?> parameter = WBParameterImpl.of(new Annotation[0], clazz, type, this, classTransformer);
            parameters.add(parameter);

            for (Annotation annotation : parameter.getAnnotations())
            {
               annotatedParameters.put(annotation.annotationType(), parameter);
            }
         }
      }
      this.signature = new ConstructorSignatureImpl(this);
   }

   /**
    * Gets the constructor
    * 
    * @return The constructor
    */
   public Constructor<T> getAnnotatedConstructor()
   {
      return constructor;
   }

   /**
    * Gets the delegate (constructor)
    * 
    * @return The delegate
    */
   @Override
public Constructor<T> getDelegate()
   {
      return constructor;
   }

   /**
    * Gets the abstracted parameters
    * 
    * If the parameters are null, initalize them first
    * 
    * @return A list of annotated parameter abstractions
    * 
    * @see org.jboss.webbeans.introspector.WBConstructor#getParameters()
    */
   public List<WBParameter<?>> getParameters()
   {
      return Collections.unmodifiableList(parameters);
   }

   /**
    * Gets parameter abstractions with a given annotation type.
    * 
    * If the parameters are null, they are initializes first.
    * 
    * @param annotationType The annotation type to match
    * @return A list of matching parameter abstractions. An empty list is
    *         returned if there are no matches.
    * 
    * @see org.jboss.webbeans.introspector.WBConstructor#getAnnotatedParameters(Class)
    */
   public List<WBParameter<?>> getAnnotatedParameters(Class<? extends Annotation> annotationType)
   {
      return Collections.unmodifiableList(annotatedParameters.get(annotationType));
   }

   /**
    * Creates a new instance
    * 
    * @param manager The Web Beans manager
    * @return An instance
    * @throws InvocationTargetException 
    * @throws IllegalAccessException 
    * @throws InstantiationException 
    * @throws IllegalArgumentException 
    * 
    * @see org.jboss.webbeans.introspector.WBConstructor#newInstance(BeanManagerImpl)
    */
   public T newInstance(Object... parameters) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
   {
      return getDelegate().newInstance(parameters);
   }

   /**
    * The overridden equals operation
    * 
    * @param other The instance to compare to
    * @return True if equal, false otherwise
    */
   @Override
   public boolean equals(Object other)
   {

      if (super.equals(other) && other instanceof WBConstructor)
      {
         WBConstructor<?> that = (WBConstructor<?>) other;
         return this.getDeclaringType().equals(that.getDeclaringType()) && this.getParameters().equals(that.getParameters());
      }
      return false;
   }

   /**
    * The overridden hashcode
    * 
    * Gets the hash code from the delegate
    * 
    * @return The hash code
    */
   @Override
   public int hashCode()
   {
      return getDelegate().hashCode();
   }

   /**
    * Gets the declaring class
    * 
    * @return The declaring class
    */
   public WBType<T> getDeclaringType()
   {
      return declaringClass;
   }

   /**
    * Gets a string representation of the constructor
    * 
    * @return A string representation
    */
   @Override
   public String toString()
   {
      if (toString != null)
      {
         return toString;
      }
      toString = "Annotated constructor " + Names.constructorToString(constructor);
      return toString;
   }
   
   public ConstructorSignature getSignature()
   {
      return signature;
   }

}
