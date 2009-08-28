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

package org.jboss.webbeans.injection.resolution;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import javax.inject.TypeLiteral;
import javax.inject.manager.Bean;

import org.jboss.webbeans.ManagerImpl;
import org.jboss.webbeans.bean.standard.EventBean;
import org.jboss.webbeans.bean.standard.InstanceBean;
import org.jboss.webbeans.introspector.AnnotatedItem;
import org.jboss.webbeans.metadata.BindingTypeModel;
import org.jboss.webbeans.metadata.MetaDataCache;
import org.jboss.webbeans.util.ListComparator;
import org.jboss.webbeans.util.collections.ConcurrentCache;

/**
 * Implementation of Web Beans type safe and name based bean resolution
 * 
 * @author Pete Muir
 */
public class Resolver
{
   private static final long serialVersionUID = 1L;

   private static final Class<AnnotatedItem<Object, Object>> ANNOTATED_ITEM_GENERIFIED_WITH_OBJECT_OBJECT = new TypeLiteral<AnnotatedItem<Object, Object>>(){}.getRawType();
   private static final Class<Set<Bean<Object>>> BEAN_SET_GENERIFIED_WITH_OBJECT = new TypeLiteral<Set<Bean<Object>>>(){}.getRawType();
   private static final Class<Set<Bean<?>>> BEAN_SET_GENERIFIED_WITH_WILDCARD = new TypeLiteral<Set<Bean<?>>>(){}.getRawType();
   
   // The resolved injection points
   private ConcurrentCache<ResolvableAnnotatedItem<?, ?>, Set<Bean<?>>> resolvedInjectionPoints;
   // The registerd injection points
   private Set<AnnotatedItem<?, ?>> injectionPoints;
   // The resolved names
   private ConcurrentCache<String, Set<Bean<?>>> resolvedNames;
   // The Web Beans manager
   private final ManagerImpl manager;
   private final Set<AnnotatedItemTransformer> transformers;

   /**
    * Constructor
    * 
    */
   public Resolver(ManagerImpl manager)
   {
      this.manager = manager;
      this.injectionPoints = new HashSet<AnnotatedItem<?, ?>>();
      this.resolvedInjectionPoints = new ConcurrentCache<ResolvableAnnotatedItem<?, ?>, Set<Bean<?>>>();
      this.resolvedNames = new ConcurrentCache<String, Set<Bean<?>>>();
      this.transformers = new HashSet<AnnotatedItemTransformer>();
      transformers.add(EventBean.TRANSFORMER);
      transformers.add(InstanceBean.TRANSFORMER);
   }
   /**
    * Add multiple injection points for later resolving using
    * {@link #registerInjectionPoint(AnnotatedItem)}. Useful during bootstrap.
    * 
    * @param elements The injection points to add
    */
   public void addInjectionPoints(Collection<? extends AnnotatedItem<?, ?>> elements)
   {
      injectionPoints.addAll(elements);
   }

   /**
    * Registers an injection point
    * 
    * @param <T>
    * @param <S>
    * @param element The injection point to add
    * @return A set of matching beans for the injection point
    */
   private <T, S> Set<Bean<T>> registerInjectionPoint(final ResolvableAnnotatedItem<T, S> element)
   {
      Callable<Set<Bean<T>>> callable = new Callable<Set<Bean<T>>>()
      {

         public Set<Bean<T>> call() throws Exception
         {
            return retainHighestPrecedenceBeans(getMatchingBeans(element, manager.getBeans()), manager.getEnabledDeploymentTypes());
         }

      };
      return resolvedInjectionPoints.putIfAbsent(element, callable);
   }

   /**
    * Reset all cached injection points. You must reset all cached injection
    * points when you add a bean to the manager
    */
   public void clear()
   {
      this.resolvedInjectionPoints = new ConcurrentCache<ResolvableAnnotatedItem<?, ?>, Set<Bean<?>>>();
      resolvedNames = new ConcurrentCache<String, Set<Bean<?>>>();
   }

   /**
    * Resolve all injection points added using
    * {@link #addInjectionPoints(Collection)}
    */
   public void resolveInjectionPoints()
   {
      for (final AnnotatedItem<? extends Object, ? extends Object> injectable : injectionPoints)
      {
         
         registerInjectionPoint(new ResolvableAnnotatedItem<Object, Object>()
         {
            private static final long serialVersionUID = 1L;

            @Override
            public AnnotatedItem<Object, Object> delegate()
            {
               return ANNOTATED_ITEM_GENERIFIED_WITH_OBJECT_OBJECT.cast(injectable); 
            }
         });
      }
   }

   /**
    * Get the possible beans for the given element
    * 
    * @param key The resolving criteria
    * @return An unmodifiable set of matching beans
    */
   public <T, S> Set<Bean<T>> get(final AnnotatedItem<T, S> key)
   {
      final AnnotatedItem<T, S> transformedElement = transformElement(key);
      
      Set<Bean<T>> beans = registerInjectionPoint(new ResolvableAnnotatedItem<T, S>()
      {

         @Override
         public AnnotatedItem<T, S> delegate()
         {
            return transformedElement;
         }

      });
      return Collections.unmodifiableSet(beans);
   }
   
   private <T, S> AnnotatedItem<T, S> transformElement(AnnotatedItem<T, S> element)
   {
      for (AnnotatedItemTransformer transformer : transformers)
      {
         element = transformer.transform(element);
      }
      return element;
   }

   /**
    * Get the possible beans for the given name
    * 
    * @param name The name to match
    * @return The set of matching beans
    */
   public Set<Bean<? extends Object>> get(final String name)
   {
      return resolvedNames.putIfAbsent(name, new Callable<Set<Bean<?>>>()
      {

         public Set<Bean<? extends Object>> call() throws Exception
         {
            
            Set<Bean<?>> beans = new HashSet<Bean<?>>();
            for (Bean<?> bean : manager.getBeans())
            {
               if ((bean.getName() == null && name == null) || (bean.getName() != null && bean.getName().equals(name)))
               {
                  beans.add(bean);
               }
            }
            return retainHighestPrecedenceBeans(beans, manager.getEnabledDeploymentTypes());
         }
         
         // Helper method to deal with dynamic casts being needed
         private Set<Bean<?>> retainHighestPrecedenceBeans(Set<Bean<?>> beans, List<Class<? extends Annotation>> enabledDeploymentTypes)
         {
            return BEAN_SET_GENERIFIED_WITH_WILDCARD.cast(Resolver.retainHighestPrecedenceBeans(BEAN_SET_GENERIFIED_WITH_OBJECT.cast(beans), enabledDeploymentTypes));
         }

      });
   }
   
   

   /**
    * Filters out the beans with the highest enabled deployment type
    * 
    * @param <T>
    * @param beans The beans to filter
    * @param enabledDeploymentTypes The enabled deployment types
    * @return The filtered beans
    */
   private static <T> Set<Bean<T>> retainHighestPrecedenceBeans(Set<Bean<T>> beans, List<Class<? extends Annotation>> enabledDeploymentTypes)
   {
      if (beans.size() > 0)
      {
         SortedSet<Class<? extends Annotation>> possibleDeploymentTypes = new TreeSet<Class<? extends Annotation>>(new ListComparator<Class<? extends Annotation>>(enabledDeploymentTypes));
         for (Bean<?> bean : beans)
         {
            possibleDeploymentTypes.add(bean.getDeploymentType());
         }
         possibleDeploymentTypes.retainAll(enabledDeploymentTypes);
         Set<Bean<T>> trimmed = new HashSet<Bean<T>>();
         if (possibleDeploymentTypes.size() > 0)
         {
            Class<? extends Annotation> highestPrecedencePossibleDeploymentType = possibleDeploymentTypes.last();

            for (Bean<T> bean : beans)
            {
               if (bean.getDeploymentType().equals(highestPrecedencePossibleDeploymentType))
               {
                  trimmed.add(bean);
               }
            }
         }
         return trimmed;
      }
      else
      {
         return beans;
      }
   }

   /**
    * Gets the matching beans for binding criteria from a list of beans
    * 
    * @param <T> The type of the beans
    * @param element The binding criteria
    * @param beans The beans to filter
    * @return A set of filtered beans
    */
   private <T> Set<Bean<T>> getMatchingBeans(AnnotatedItem<T, ?> element, List<Bean<?>> beans)
   {
      Set<Bean<T>> resolvedBeans = new HashSet<Bean<T>>();
      for (Bean<?> bean : beans)
      {
         if (element.isAssignableFrom(bean.getTypes()) && containsAllBindings(element, bean.getBindings()))
         {
            @SuppressWarnings("unchecked")
            Bean<T> b = (Bean<T>) bean;
            resolvedBeans.add(b);
         }
      }
      return resolvedBeans;
   }

   /**
    * Checks if binding criteria fulfill all binding types
    * 
    * @param element The binding criteria to check
    * @param bindings The binding types to check
    * @return True if all matches, false otherwise
    */
   private boolean containsAllBindings(AnnotatedItem<?, ?> element, Set<Annotation> bindings)
   {
      for (Annotation binding : element.getBindings())
      {
         BindingTypeModel<?> bindingType = manager.getServices().get(MetaDataCache.class).getBindingTypeModel(binding.annotationType());
         if (bindingType.getNonBindingTypes().size() > 0)
         {
            boolean matchFound = false;
            for (Annotation otherBinding : bindings)
            {
               if (bindingType.isEqual(binding, otherBinding))
               {
                  matchFound = true;
               }
            }
            if (!matchFound)
            {
               return false;
            }
         }
         else if (!bindings.contains(binding))
         {
            return false;
         }
      }
      return true;
   }

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
      buffer.append("Injection points: " + injectionPoints.size() + "\n");
      buffer.append("Resolved injection points: " + resolvedInjectionPoints.size() + "\n");
      buffer.append("Resolved names points: " + resolvedNames.size() + "\n");
      return buffer.toString();
   }

}
