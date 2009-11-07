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
package org.jboss.weld.bean.builtin.facade;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.weld.BeanManagerImpl;
import org.jboss.weld.metadata.cache.MetaAnnotationStore;

/**
 * Common implementation for binding-type-based helpers
 * 
 * @author Gavin King
 * 
 * @param <T>
 */
public abstract class AbstractFacade<T, X> implements Serializable
{
   
   private static final long serialVersionUID = 8710258788495459128L;

   private static final Annotation[] EMPTY_BINDINGS = new Annotation[0];
   
   // The binding types the helper operates on
   private final Set<? extends Annotation> bindings;
   // The Bean manager
   private final BeanManagerImpl manager;
   // The type of the operation
   private final Type type;

   /**
    * 
    * @param type The event type
    * @param manager The Bean manager
    * @param bindings The binding types
    */
   protected AbstractFacade(Type type, BeanManagerImpl manager, Set<? extends Annotation> bindings)
   {
      this.manager = manager;
      this.type = type;
      // Need to make sure the Set is serializable, some sets from Google Collections aren't
      // TODO Work out how to not do this
      this.bindings = new HashSet<Annotation>(bindings);
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
         if (!getManager().getServices().get(MetaAnnotationStore.class).getBindingTypeModel(newAnnotation.annotationType()).isValid())
         {
            throw new IllegalArgumentException(newAnnotation + " is not a binding for " + this);
         }
         if (result.contains(newAnnotation))
         {
            throw new IllegalArgumentException(newAnnotation + " is already present in the bindings list for " + this);
         }
         result.add(newAnnotation);
      }
      return result.toArray(EMPTY_BINDINGS);
   }

   protected BeanManagerImpl getManager()
   {
      return manager.getCurrent();
   }
   
   protected Set<? extends Annotation> getBindings()
   {
      return Collections.unmodifiableSet(bindings);
   }
   
   protected Type getType()
   {
      return type;
   }

}