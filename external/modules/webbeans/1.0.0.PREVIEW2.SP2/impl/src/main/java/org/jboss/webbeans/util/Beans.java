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
package org.jboss.webbeans.util;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.decorator.Decorates;
import javax.enterprise.inject.BindingType;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;

import org.jboss.webbeans.BeanManagerImpl;
import org.jboss.webbeans.DefinitionException;
import org.jboss.webbeans.bean.EnterpriseBean;
import org.jboss.webbeans.bean.RIBean;
import org.jboss.webbeans.injection.FieldInjectionPoint;
import org.jboss.webbeans.introspector.WBClass;
import org.jboss.webbeans.introspector.WBField;
import org.jboss.webbeans.metadata.cache.BindingTypeModel;
import org.jboss.webbeans.metadata.cache.MetaAnnotationStore;
import org.jboss.webbeans.util.collections.ListComparator;

/**
 * Helper class for bean inspection
 * 
 * @author Pete Muir
 *
 */
public class Beans
{

   /**
    * Indicates if a bean is passivating
    * 
    * @param bean The bean to inspect
    * @return True if passivating, false otherwise
    */
   public static boolean isPassivatingBean(Bean<?> bean, BeanManagerImpl manager)
   {
      if (bean instanceof EnterpriseBean)
      {
         return ((EnterpriseBean<?>) bean).getEjbDescriptor().isStateful();
      }
      else
      {
         return manager.getServices().get(MetaAnnotationStore.class).getScopeModel(bean.getScopeType()).isPassivating();
      }
   }

   /**
    * Indicates if a bean is proxyable
    * 
    * @param bean The bean to test
    * @return True if proxyable, false otherwise
    */
   public static boolean isBeanProxyable(Bean<?> bean)
   {
      if (bean instanceof RIBean)
      {
         return ((RIBean<?>) bean).isProxyable();
      }
      else
      {
         return Proxies.isTypesProxyable(bean.getTypes());
      }
   }

   public static Set<FieldInjectionPoint<?>> getFieldInjectionPoints(WBClass<?> annotatedItem, Bean<?> declaringBean)
   {
      Set<FieldInjectionPoint<?>> injectableFields = new HashSet<FieldInjectionPoint<?>>();
      for (WBField<?> annotatedField : annotatedItem.getMetaAnnotatedFields(BindingType.class))
      {
         addFieldInjectionPoint(annotatedField, injectableFields, declaringBean);
      }
      for (WBField<?> annotatedField : annotatedItem.getAnnotatedFields(Decorates.class))
      {
         addFieldInjectionPoint(annotatedField, injectableFields, declaringBean);
      }
      return injectableFields;
   }
   
   private static void addFieldInjectionPoint(WBField<?> annotatedField, Set<FieldInjectionPoint<?>> injectableFields, Bean<?> declaringBean)
   {
      if (!annotatedField.isAnnotationPresent(Produces.class))
      {
         if (annotatedField.isStatic())
         {
            throw new DefinitionException("Don't place binding annotations on static fields " + annotatedField);
         }
         if (annotatedField.isFinal())
         {
            throw new DefinitionException("Don't place binding annotations on final fields " + annotatedField);
         }
         FieldInjectionPoint<?> fieldInjectionPoint = FieldInjectionPoint.of(declaringBean, annotatedField);
         injectableFields.add(fieldInjectionPoint);
      }
   }
   
   /**
    * Checks if binding criteria fulfill all binding types
    * 
    * @param element The binding criteria to check
    * @param bindings2 The binding types to check
    * @return True if all matches, false otherwise
    */
   public static boolean containsAllBindings(Set<Annotation> bindings1, Set<Annotation> bindings2, BeanManagerImpl manager)
   {
      for (Annotation binding : bindings1)
      {
         BindingTypeModel<?> bindingType = manager.getServices().get(MetaAnnotationStore.class).getBindingTypeModel(binding.annotationType());
         boolean matchFound = false;
         // TODO Something wrong with annotation proxy hashcode in JDK/AnnotationLiteral hashcode, so always do a full check, don't use contains
         for (Annotation otherBinding : bindings2)
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
      return true;
   }
   

   /**
    * Retains only beans which have deployment type X.
    * 
    * The deployment type X is
    * 
    * @param <T>
    * @param beans The beans to filter
    * @param enabledDeploymentTypes The enabled deployment types
    * @return The filtered beans
    */
   public static <T extends Bean<?>> Set<T> retainHighestPrecedenceBeans(Set<T> beans, List<Class<? extends Annotation>> enabledDeployentTypes)
   {
      if (beans.size() > 0)
      {
         SortedSet<Class<? extends Annotation>> possibleDeploymentTypes = new TreeSet<Class<? extends Annotation>>(new ListComparator<Class<? extends Annotation>>(enabledDeployentTypes));
         for (Bean<?> bean : beans)
         {
            possibleDeploymentTypes.add(bean.getDeploymentType());
         }
         possibleDeploymentTypes.retainAll(enabledDeployentTypes);
         Set<T> trimmed = new HashSet<T>();
         if (possibleDeploymentTypes.size() > 0)
         {
            Class<? extends Annotation> highestPrecedencePossibleDeploymentType = possibleDeploymentTypes.last();

            for (T bean : beans)
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
   
}
