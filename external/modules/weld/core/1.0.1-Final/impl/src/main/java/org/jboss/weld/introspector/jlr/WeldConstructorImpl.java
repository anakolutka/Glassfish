/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.introspector.jlr;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedParameter;

import org.jboss.weld.exceptions.DefinitionException;
import org.jboss.weld.introspector.ConstructorSignature;
import org.jboss.weld.introspector.WeldClass;
import org.jboss.weld.introspector.WeldConstructor;
import org.jboss.weld.introspector.WeldParameter;
import org.jboss.weld.logging.messages.ReflectionMessage;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.reflection.HierarchyDiscovery;
import org.jboss.weld.util.reflection.Reflections;
import org.jboss.weld.util.reflection.SecureReflections;

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
public class WeldConstructorImpl<T> extends AbstractWeldCallable<T, T, Constructor<T>> implements WeldConstructor<T>
{

   // The underlying constructor
   private final Constructor<T> constructor;

   // The list of parameter abstractions
   private final List<WeldParameter<?, T>> parameters;
   // The mapping of annotation -> parameter abstraction
   private final ListMultimap<Class<? extends Annotation>, WeldParameter<?, T>> annotatedParameters;

   private final ConstructorSignature signature;

   public static <T> WeldConstructor<T> of(Constructor<T> constructor, WeldClass<T> declaringClass, ClassTransformer classTransformer)
   {
      return new WeldConstructorImpl<T>(constructor, constructor.getDeclaringClass(), constructor.getDeclaringClass(), null, new HierarchyDiscovery(constructor.getDeclaringClass()).getTypeClosure(), buildAnnotationMap(constructor.getAnnotations()), buildAnnotationMap(constructor.getDeclaredAnnotations()), declaringClass, classTransformer);
   }

   public static <T> WeldConstructor<T> of(AnnotatedConstructor<T> annotatedConstructor, WeldClass<T> declaringClass, ClassTransformer classTransformer)
   {
      return new WeldConstructorImpl<T>(annotatedConstructor.getJavaMember(), annotatedConstructor.getJavaMember().getDeclaringClass(), annotatedConstructor.getBaseType(), annotatedConstructor, annotatedConstructor.getTypeClosure(), buildAnnotationMap(annotatedConstructor.getAnnotations()), buildAnnotationMap(annotatedConstructor.getAnnotations()), declaringClass, classTransformer);
   }

   /**
    * Constructor
    * 
    * Initializes the superclass with the build annotations map
    * 
    * @param constructor The constructor method
    * @param declaringClass The declaring class
    */
   private WeldConstructorImpl(Constructor<T> constructor, final Class<T> rawType, final Type type, AnnotatedConstructor<T> annotatedConstructor, Set<Type> typeClosure, Map<Class<? extends Annotation>, Annotation> annotationMap, Map<Class<? extends Annotation>, Annotation> declaredAnnotationMap, WeldClass<T> declaringClass, ClassTransformer classTransformer)
   {
      super(annotationMap, declaredAnnotationMap, classTransformer, constructor, rawType, type, typeClosure, declaringClass);
      this.constructor = constructor;

      this.parameters = new ArrayList<WeldParameter<?, T>>();
      annotatedParameters = Multimaps.newListMultimap(new HashMap<Class<? extends Annotation>, Collection<WeldParameter<?, T>>>(), new Supplier<List<WeldParameter<?, T>>>()
      {

         public List<WeldParameter<?, T>> get()
         {
            return new ArrayList<WeldParameter<?, T>>();
         }

      });

      Map<Integer, AnnotatedParameter<?>> annotatedTypeParameters = new HashMap<Integer, AnnotatedParameter<?>>();

      if (annotatedConstructor != null)
      {
         for (AnnotatedParameter<?> annotated : annotatedConstructor.getParameters())
         {
            annotatedTypeParameters.put(annotated.getPosition(), annotated);
         }
      }

      // If the class is a (non-static) member class, its constructors
      // parameterTypes array will prefix the
      // outer class instance, whilst the genericParameterTypes array isn't
      // prefix'd
      int nesting = Reflections.getNesting(declaringClass.getJavaClass());
      if (annotatedConstructor == null)
      {
         for (int i = 0; i < constructor.getParameterTypes().length; i++)
         {
            int gi = i - nesting;
            if (constructor.getParameterAnnotations()[i].length > 0)
            {
               Class<? extends Object> clazz = constructor.getParameterTypes()[i];
               Type parameterType;
               if (constructor.getGenericParameterTypes().length > gi && gi >= 0)
               {
                  parameterType = constructor.getGenericParameterTypes()[gi];
               }
               else
               {
                  parameterType = clazz;
               }
               WeldParameter<?, T> parameter = WeldParameterImpl.of(constructor.getParameterAnnotations()[i], clazz, parameterType, this, i, classTransformer);
               this.parameters.add(parameter);
               for (Annotation annotation : parameter.getAnnotations())
               {
                  if (MAPPED_PARAMETER_ANNOTATIONS.contains(annotation.annotationType()))
                  {
                     annotatedParameters.put(annotation.annotationType(), parameter);
                  }
               }
            }
            else
            {
               Class<? extends Object> clazz = constructor.getParameterTypes()[i];
               Type parameterType;
               if (constructor.getGenericParameterTypes().length > gi && gi >= 0)
               {
                  parameterType = constructor.getGenericParameterTypes()[gi];
               }
               else
               {
                  parameterType = clazz;
               }
               WeldParameter<?, T> parameter = WeldParameterImpl.of(new Annotation[0], (Class<Object>) clazz, parameterType, this, i, classTransformer);
               this.parameters.add(parameter);
            }
         }
      }
      else
      {
         if (annotatedConstructor.getParameters().size() != constructor.getParameterTypes().length)
         {
            throw new DefinitionException(ReflectionMessage.INCORRECT_NUMBER_OF_ANNOTATED_PARAMETERS_METHOD, annotatedConstructor.getParameters().size(), annotatedConstructor, annotatedConstructor.getParameters(), Arrays.asList(constructor.getParameterTypes()));
         }
         else
         {
            for (AnnotatedParameter<T> annotatedParameter : annotatedConstructor.getParameters())
            {
               WeldParameter<?, T> parameter = WeldParameterImpl.of(annotatedParameter.getAnnotations(), constructor.getParameterTypes()[annotatedParameter.getPosition()], annotatedParameter.getBaseType(), this, annotatedParameter.getPosition(), classTransformer);
               this.parameters.add(parameter);
               for (Annotation annotation : parameter.getAnnotations())
               {
                  if (MAPPED_PARAMETER_ANNOTATIONS.contains(annotation.annotationType()))
                  {
                     annotatedParameters.put(annotation.annotationType(), parameter);
                  }
               }
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
    * @see org.jboss.weld.introspector.WeldConstructor#getWeldParameters()
    */
   public List<WeldParameter<?, T>> getWeldParameters()
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
    * @see org.jboss.weld.introspector.WeldConstructor#getWeldParameters(Class)
    */
   public List<WeldParameter<?, T>> getWeldParameters(Class<? extends Annotation> annotationType)
   {
      return Collections.unmodifiableList(annotatedParameters.get(annotationType));
   }

   /**
    * Creates a new instance
    * 
    * @param beanManager The Bean manager
    * @return An instance
    * @throws InvocationTargetException
    * @throws IllegalAccessException
    * @throws InstantiationException
    * @throws IllegalArgumentException
    * 
    * @see org.jboss.weld.introspector.WeldConstructor#newInstance(BeanManagerImpl)
    */
   public T newInstance(Object... parameters) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
   {
      return SecureReflections.ensureAccessible(getDelegate()).newInstance(parameters);
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

      if (super.equals(other) && other instanceof WeldConstructor)
      {
         WeldConstructor<?> that = (WeldConstructor<?>) other;
         return this.getJavaMember().equals(that.getJavaMember()) && this.getWeldParameters().equals(that.getWeldParameters());
      }
      return false;
   }

   /**
    * Gets a string representation of the constructor
    * 
    * @return A string representation
    */
   @Override
   public String toString()
   {
      return new StringBuilder().append("constructor ").append(constructor.toString()).toString();
   }

   public ConstructorSignature getSignature()
   {
      return signature;
   }

   public List<AnnotatedParameter<T>> getParameters()
   {
      return Collections.unmodifiableList((List) parameters);
   }
   
   public boolean isGeneric()
   {
      return getJavaMember().getTypeParameters().length > 0;
   }

}
