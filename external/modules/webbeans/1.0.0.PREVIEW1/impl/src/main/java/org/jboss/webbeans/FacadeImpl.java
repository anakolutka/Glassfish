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

package org.jboss.webbeans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.inject.DuplicateBindingTypeException;

import org.jboss.webbeans.metadata.MetaDataCache;

/**
 * Common implementation for binding-type-based helpers
 * 
 * @author Gavin King
 * 
 * @param <T>
 */
public abstract class FacadeImpl<T>
{
   
   private static final Annotation[] EMPTY_BINDINGS = new Annotation[0];
   
   // The binding types the helper operates on
   protected final Set<? extends Annotation> bindings;
   // The Web Beans manager
   private final ManagerImpl manager;
   // The type of the operation
   protected final Type type;

   /**
    * Constructor
    * 
    * @param type The event type
    * @param manager The Web Beans manager
    * @param bindings The binding types
    */
   protected FacadeImpl(Type type, ManagerImpl manager, Set<? extends Annotation> bindings)
   {
      this.manager = manager;
      this.type = type;
      this.bindings = bindings;
   }

   /**
    * Gets a string representation
    * 
    * @return A string representation
    */
   @Override
   public String toString()
   {
      return "Abstract facade implmentation";
   }
   
   protected Annotation[] mergeInBindings(Annotation... newBindings)
   {
      Set<Annotation> result = new HashSet<Annotation>();
      result.addAll(bindings);
      for (Annotation newAnnotation : newBindings)
      {
         if (!getManager().getServices().get(MetaDataCache.class).getBindingTypeModel(newAnnotation.annotationType()).isValid())
         {
            throw new IllegalArgumentException(newAnnotation + " is not a binding for " + this);
         }
         if (result.contains(newAnnotation))
         {
            throw new DuplicateBindingTypeException(newAnnotation + " is already present in the bindings list for " + this);
         }
         result.add(newAnnotation);
      }
      return result.toArray(EMPTY_BINDINGS);
   }

   protected ManagerImpl getManager()
   {
      return manager.getCurrent();
   }

}