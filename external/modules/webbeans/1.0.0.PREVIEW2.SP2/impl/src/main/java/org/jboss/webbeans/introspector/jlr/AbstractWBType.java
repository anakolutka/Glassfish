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

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import org.jboss.webbeans.introspector.AnnotationStore;
import org.jboss.webbeans.introspector.WBClass;
import org.jboss.webbeans.introspector.WBType;
import org.jboss.webbeans.resources.ClassTransformer;
import org.jboss.webbeans.util.Reflections;

/**
 * Represents an abstract annotated type
 * 
 * This class is immutable, and therefore threadsafe
 * 
 * @author Pete Muir
 * 
 * @param <T>
 */
public abstract class AbstractWBType<T> extends AbstractWBAnnotated<T, Class<T>> implements WBType<T>
{
   
   // The superclass abstraction of the type
   private final WBClass<?> superclass;
   // The name of the type
   private final String name;
   
   private final String _simpleName;
   
   // Cached string representation
   private String toString;
   private final boolean _public;
   private final boolean _private;

   /**
    * Constructor
    * 
    * @param annotationMap The annotation map
    */
   public AbstractWBType(AnnotationStore annotatedItemHelper, Class<T> rawType, Type type, ClassTransformer classTransformer)
   {
      super(annotatedItemHelper, rawType, type);
      this.name = rawType.getName();
      this._simpleName = rawType.getSimpleName();
      if (rawType.getSuperclass() != null)
      {
         this.superclass = classTransformer.loadClass(rawType.getSuperclass());
      }
      else
      {
         this.superclass = null;
      }
      this._public = Modifier.isFinal(rawType.getModifiers());
      _private = Modifier.isPrivate(rawType.getModifiers());
   }

   /**
    * Indicates if the type is static
    * 
    * @return True if static, false otherwise
    * 
    * @see org.jboss.webbeans.introspector.WBAnnotated#isStatic()
    */
   public boolean isStatic()
   {
      return Reflections.isStatic(getDelegate());
   }

   /**
    * Indicates if the type if final
    * 
    * @return True if final, false otherwise
    * 
    * @see org.jboss.webbeans.introspector.WBAnnotated#isFinal()
    */
   public boolean isFinal()
   {
      return Reflections.isFinal(getDelegate());
   }
   
   public boolean isPublic()
   {
      return _public;
   }

   /**
    * Gets the name of the type
    * 
    * @returns The name
    * 
    * @see org.jboss.webbeans.introspector.WBAnnotated#getName()
    */
   public String getName()
   {
      return name;
   }

   /**
    * Gets the superclass abstraction of the type
    * 
    * @return The superclass abstraction
    */
   public WBClass<?> getSuperclass()
   {
      return superclass;
   }
   
   public boolean isEquivalent(Class<?> clazz)
   {
      return getDelegate().equals(clazz);
   }
   
   public boolean isPrivate()
   {
      return _private;
   }

   /**
    * Gets a string representation of the type
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
      toString = "Abstract annotated type " + getName();
      return toString;
   }
   
   public String getSimpleName()
   {
      return _simpleName;
   }

}
