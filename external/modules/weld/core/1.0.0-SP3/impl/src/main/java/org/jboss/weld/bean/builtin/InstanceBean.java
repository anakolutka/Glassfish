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
package org.jboss.weld.bean.builtin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Provider;

import org.jboss.weld.BeanManagerImpl;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.resolution.ResolvableTransformer;
import org.jboss.weld.util.collections.Arrays2;

public class InstanceBean extends AbstractFacadeBean<Instance<?>>
{

   private static final Class<Instance<?>> INSTANCE_TYPE = new TypeLiteral<Instance<?>>() {}.getRawType();
   private static final Class<Provider<?>> PROVIDER_TYPE = new TypeLiteral<Provider<?>>() {}.getRawType();
   private static final Set<Type> DEFAULT_TYPES = Arrays2.<Type>asSet(INSTANCE_TYPE, PROVIDER_TYPE, Object.class);
   private static final Any ANY = new AnyLiteral();
   private static final Set<Annotation> DEFAULT_BINDINGS = new HashSet<Annotation>(Arrays.asList(ANY));
   public static final ResolvableTransformer INSTANCE_TRANSFORMER = new FacadeBeanResolvableTransformer(INSTANCE_TYPE);
   public static final ResolvableTransformer PROVIDER_TRANSFORMER = new FacadeBeanResolvableTransformer(PROVIDER_TYPE);
   
   public InstanceBean(BeanManagerImpl manager)
   {
      super(Instance.class.getSimpleName(), manager);
   }

   @Override
   public Class<Instance<?>> getType()
   {
      return INSTANCE_TYPE;
   }

   @Override
   public Class<?> getBeanClass()
   {
      return InstanceImpl.class;
   }

   public Set<Type> getTypes()
   {
      return DEFAULT_TYPES;
   }
   
   @Override
   public Set<Annotation> getQualifiers()
   {
      return DEFAULT_BINDINGS;
   }

   @Override
   protected Instance<?> newInstance(InjectionPoint injectionPoint)
   {
      return InstanceImpl.of(injectionPoint, getManager());
   }
   
   @Override
   public String toString()
   {
      return "Built-in implicit javax.inject.Instance bean";
   }
   
}
