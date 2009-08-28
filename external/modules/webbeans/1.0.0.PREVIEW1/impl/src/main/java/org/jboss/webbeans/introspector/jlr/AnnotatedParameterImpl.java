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
import java.lang.reflect.Type;
import java.util.Set;

import javax.inject.BindingType;
import javax.inject.manager.Manager;

import org.jboss.webbeans.introspector.AnnotatedMember;
import org.jboss.webbeans.introspector.AnnotatedParameter;
import org.jboss.webbeans.introspector.AnnotationStore;

/**
 * Represents a parameter
 * 
 * This class is immutable and therefore threadsafe
 * 
 * @author Pete Muir
 * 
 * @param <T>
 */
public class AnnotatedParameterImpl<T> extends AbstractAnnotatedItem<T, Object> implements AnnotatedParameter<T>
{
   
   // The final state
   private final boolean _final = false;
   // The static state
   private final boolean _static = false;
   private final boolean _public = false;
   private final AnnotatedMember<?, ?> declaringMember;

   // Cached string representation
   private String toString;
   
   public static <T> AnnotatedParameter<T> of(Annotation[] annotations, Class<T> rawType, Type type, AnnotatedMember<?, ?> declaringMember)
   {
      return new AnnotatedParameterImpl<T>(annotations, rawType, type, declaringMember);
   }

   /**
    * Constructor
    * 
    * @param annotations The annotations array
    * @param type The type of the parameter
    */
   protected AnnotatedParameterImpl(Annotation[] annotations, Class<T> rawType, Type type, AnnotatedMember<?, ?> declaringMember)
   {
      super(AnnotationStore.of(annotations, annotations), rawType, type);
      this.declaringMember = declaringMember;
   }

   /**
    * Gets the delegate
    * 
    * @return The delegate (null)
    * 
    * @see org.jboss.webbeans.introspector.AnnotatedItem#getDelegate()
    */
   public Object getDelegate()
   {
      return null;
   }

   /**
    * Indicates if the parameter is final
    * 
    * @return True if final, false otherwise
    * 
    * @see org.jboss.webbeans.introspector.AnnotatedItem#isFinal()
    */
   public boolean isFinal()
   {
      return _final;
   }

   /**
    * Indicates if the parameter is static
    * 
    * @return True if static, false otherwise
    * 
    * @see org.jboss.webbeans.introspector.AnnotatedItem#isStatic()
    */
   public boolean isStatic()
   {
      return _static;
   }
   
   public boolean isPublic()
   {
      return _public;
   }

   /**
    * Gets the current value
    * 
    * @param manager The Web Beans manager
    * @return the value
    * 
    * @see org.jboss.webbeans.introspector.AnnotatedParameter
    */
   public T getValue(Manager manager)
   {
      return manager.getInstanceByType(getRawType(), getMetaAnnotationsAsArray(BindingType.class));
   }

   /**
    * Gets the name of the parameter
    * 
    * @throws IllegalArgumentException (not supported)
    * 
    * @see org.jboss.webbeans.introspector.AnnotatedItem#getName()
    */
   public String getName()
   {
      throw new IllegalArgumentException("Unable to determine name of parameter");
   }

   /**
    * Gets a string representation of the parameter
    * 
    * @return A string representation
    */
   @Override
   public String toString()
   {
      if (toString == null)
      {
         StringBuilder buffer = new StringBuilder();
         buffer.append("Annotated parameter ");
         if (_static)
            buffer.append("static ");
         if (_final)
            buffer.append("final ");
         buffer.append(getRawType().getName());
         buffer.append(" for operation ");
         buffer.append(getDeclaringMember().toString());
         toString = buffer.toString();
      }
      return toString;
   }

   public AnnotatedMember<?, ?> getDeclaringMember()
   {
      return declaringMember;
   }
   
   public AnnotatedParameter<T> wrap(Set<Annotation> annotations)
   {
      throw new UnsupportedOperationException();
   }
   
}
