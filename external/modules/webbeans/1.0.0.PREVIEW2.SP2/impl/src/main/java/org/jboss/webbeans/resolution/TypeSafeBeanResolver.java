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
package org.jboss.webbeans.resolution;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;

import org.jboss.webbeans.BeanManagerImpl;
import org.jboss.webbeans.bean.standard.EventBean;
import org.jboss.webbeans.bean.standard.InstanceBean;
import org.jboss.webbeans.util.Beans;
import org.jboss.webbeans.util.Reflections;

/**
 * @author pmuir
 *
 */
public class TypeSafeBeanResolver<T extends Bean<?>> extends TypeSafeResolver<T>
{

   private final BeanManagerImpl manager;
   public static final Set<ResolvableTransformer> TRANSFORMERS;
   
   static
   {
      TRANSFORMERS = new HashSet<ResolvableTransformer>();
      TRANSFORMERS.add(EventBean.TRANSFORMER);
      TRANSFORMERS.add(InstanceBean.TRANSFORMER);
   }

   public TypeSafeBeanResolver(BeanManagerImpl manager, Iterable<T> beans)
   {
      super(beans);
      this.manager = manager;
   }

   @Override
   protected boolean matches(Resolvable resolvable, T bean)
   {
      return Reflections.isAssignableFrom(resolvable.getTypeClosure(), bean.getTypes()) && Beans.containsAllBindings(resolvable.getBindings(), bean.getBindings(), manager);
   }
   
   /**
    * @return the manager
    */
   public BeanManagerImpl getManager()
   {
      return manager;
   }
   
   @Override
   protected Set<T> filterResult(Set<T> matched)
   {
      return Beans.retainHighestPrecedenceBeans(matched, manager.getEnabledDeploymentTypes());
   }

   @Override
   protected Iterable<ResolvableTransformer> getTransformers()
   {
      return TRANSFORMERS;
   }

   @Override
   protected Set<T> sortResult(Set<T> matched)
   {
      return matched;
   }

}
