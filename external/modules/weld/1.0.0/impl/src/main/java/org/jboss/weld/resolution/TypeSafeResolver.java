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
package org.jboss.weld.resolution;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.jboss.weld.util.collections.ConcurrentCache;

/**
 * Implementation of type safe bean resolution
 * 
 * @author Pete Muir
 * @author Marius Bogoevici
 */
public abstract class TypeSafeResolver<R extends Resolvable,T>
{
   private static final long serialVersionUID = 1L;
   
   private static abstract class MatchingResolvable extends ForwardingResolvable
   {
      
      public static MatchingResolvable of(final Resolvable resolvable)
      {
         return new MatchingResolvable()
         {

            @Override
            protected Resolvable delegate()
            {
               return resolvable;
            }

         };
      }
      
      @Override
      public boolean equals(Object obj)
      {
         if (obj instanceof Resolvable)
         {
            Resolvable that = (Resolvable) obj;
            return this.getTypeClosure().equals(that.getTypeClosure()) && this.getQualifiers().equals(that.getQualifiers());
         }
         else
         {
            return false;
         }
      }
      
   }
   
   // The resolved injection points
   private ConcurrentCache<MatchingResolvable, Set<T>> resolved;
   
   // The beans to search
   private final Iterable<? extends T> iterable;

   /**
    * Constructor
    * 
    */
   public TypeSafeResolver(Iterable<? extends T> allBeans)
   {
      this.iterable = allBeans;
      this.resolved = new ConcurrentCache<MatchingResolvable, Set<T>>();
      
   }

   /**
    * Reset all cached resolutions
    */
   public void clear()
   {
      this.resolved = new ConcurrentCache<MatchingResolvable, Set<T>>();
   }

   /**
    * Get the possible beans for the given element
    * 
    * @param key The resolving criteria
    * @return An unmodifiable set of matching beans
    */
   public Set<T> resolve(Resolvable key)
   {
      final MatchingResolvable resolvable = MatchingResolvable.of(transform(key));
      
      Callable<Set<T>> callable = new Callable<Set<T>>()
      {
         public Set<T> call() throws Exception
         {
            return sortResult(filterResult(findMatching(resolvable)));
         }

      };
      Set<T> beans = resolved.putIfAbsent(resolvable, callable);
      return Collections.unmodifiableSet(beans);
   }
   
   protected Resolvable transform(Resolvable resolvable)
   {
      for (ResolvableTransformer transformer : getTransformers())
      {
         resolvable = transformer.transform(resolvable);
      }
      return resolvable;
   }
   
   protected abstract Iterable<ResolvableTransformer> getTransformers();
   
   protected abstract Set<T> filterResult(Set<T> matched);

   protected abstract Set<T> sortResult(Set<T> matched);

   /**
    * Gets the matching beans for binding criteria from a list of beans
    * 
    * @param <T> The type of the beans
    * @param element The binding criteria
    * @param beans The beans to filter
    * @return A set of filtered beans
    */
   private Set<T> findMatching(MatchingResolvable resolvable)
   {
      Set<T> result = new HashSet<T>();
      for (T bean : iterable)
      {
         if (matches((R)resolvable.delegate(), bean))
         {
            result.add(bean);
         }
      }
      return result;
   }

   protected abstract boolean matches(R resolvable, T t);

   /**
    * Gets a string representation
    * 
    * @return A string representation
    */
   @Override
   public String toString()
   {
      StringBuilder buffer = new StringBuilder();
      buffer.append("Resolver\n");
      buffer.append("Resolved injection points: " + resolved.size() + "\n");
      return buffer.toString();
   }

}
