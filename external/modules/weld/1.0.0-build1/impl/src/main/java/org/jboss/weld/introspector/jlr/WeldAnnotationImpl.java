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
package org.jboss.weld.introspector.jlr;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.weld.introspector.AnnotationStore;
import org.jboss.weld.introspector.WeldAnnotation;
import org.jboss.weld.introspector.WeldClass;
import org.jboss.weld.introspector.WeldMethod;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.Names;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

/**
 * Represents an annotated annotation
 * 
 * This class is immutable and therefore threadsafe
 * 
 * @author Pete Muir
 * 
 * @param <T>
 */
public class WeldAnnotationImpl<T extends Annotation> extends WeldClassImpl<T> implements WeldAnnotation<T>
{

   // The annotated members map (annotation -> member with annotation)
   private final SetMultimap<Class<? extends Annotation>, WeldMethod<?, ?>> annotatedMembers;
   // The implementation class of the annotation
   private final Class<T> clazz;
   // The set of abstracted members
   private final Set<WeldMethod<?, ?>> members;
   
   private final Map<String, WeldMethod<?, ?>> namedMembers;

   // Cached string representation
   private final String toString;
   
   public static <A extends Annotation> WeldAnnotation<A> of(Class<A> annotationType, ClassTransformer classTransformer)
   {
      return new WeldAnnotationImpl<A>(annotationType, classTransformer);
   }

   /**
    * Constructor
    * 
    * Initializes the superclass with the built annotation map
    * 
    * @param annotationType The annotation type
    */
   protected WeldAnnotationImpl(Class<T> annotationType, ClassTransformer classTransformer)
   {
      super(annotationType, annotationType, null, AnnotationStore.of(annotationType, classTransformer.getTypeStore().get(annotationType), classTransformer.getTypeStore().get(annotationType), classTransformer.getTypeStore()), classTransformer);
      this.clazz = annotationType;
      this.toString = "class " + Names.classToString(getDelegate());
      members = new HashSet<WeldMethod<?, ?>>();
      annotatedMembers = Multimaps.newSetMultimap(new HashMap<Class<? extends Annotation>, Collection<WeldMethod<?, ?>>>(), new Supplier<Set<WeldMethod<?, ?>>>()
      {
   
          public Set<WeldMethod<?, ?>> get()
          {
             return new HashSet<WeldMethod<?, ?>>();
          }
         
      });
      this.namedMembers = new HashMap<String, WeldMethod<?, ?>>();
      for (Method member : clazz.getDeclaredMethods())
      {
         WeldMethod<?, ?> annotatedMethod = WeldMethodImpl.of(member, this, classTransformer);
         members.add(annotatedMethod);
         for (Annotation annotation : annotatedMethod.getAnnotations())
         {
            annotatedMembers.put(annotation.annotationType(), annotatedMethod);
         }
         namedMembers.put(annotatedMethod.getName(), annotatedMethod);
      }
   }

   /**
    * Gets all members of the annotation
    * 
    * Initializes the members first if they are null
    * 
    * @return The set of abstracted members
    * 
    * @see org.jboss.weld.introspector.WeldAnnotation#getMembers()
    */
   public Set<WeldMethod<?, ?>> getMembers()
   {
      return Collections.unmodifiableSet(members);
   }

   /**
    * Returns the annotated members with a given annotation type
    * 
    * If the annotated members are null, they are initialized first.
    * 
    * @param annotationType The annotation type to match
    * @return The set of abstracted members with the given annotation type
    *         present. An empty set is returned if no matches are found
    * 
    * @see org.jboss.weld.introspector.WeldAnnotation#getAnnotatedMembers(Class)
    */
   public Set<WeldMethod<?, ?>> getAnnotatedMembers(Class<? extends Annotation> annotationType)
   {
      return Collections.unmodifiableSet(annotatedMembers.get(annotationType));
   }
   
   public <A> WeldMethod<A, ?> getMember(String memberName, WeldClass<A> expectedType)
   {
      return (WeldMethod<A, ?>) namedMembers.get(memberName);
   }
   
   /**
    * Gets a string representation of the annotation
    * 
    * @return A string representation
    */
   @Override
   public String toString()
   {
      return toString;
   }

   @Override
   public Class<T> getDelegate()
   {
      return clazz;
   }
}
