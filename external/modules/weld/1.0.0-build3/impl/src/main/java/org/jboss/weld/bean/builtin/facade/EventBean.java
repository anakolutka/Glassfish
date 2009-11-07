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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;

import org.jboss.weld.BeanManagerImpl;
import org.jboss.weld.event.EventImpl;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;
import org.jboss.weld.resolution.ResolvableTransformer;
import org.jboss.weld.util.collections.Arrays2;

public class EventBean extends AbstractFacadeBean<Event<?>>
{

   private static final Class<Event<?>> TYPE = new TypeLiteral<Event<?>>() {}.getRawType();
   private static final Set<Type> DEFAULT_TYPES = Arrays2.<Type>asSet(TYPE, Object.class);
   private static final Annotation ANY = new AnyLiteral();
   private static final Default DEFAULT = new DefaultLiteral();
   private static final Set<Annotation> DEFAULT_BINDINGS = new HashSet<Annotation>(Arrays.asList(ANY));
   public static final ResolvableTransformer TRANSFORMER = new FacadeBeanResolvableTransformer(TYPE);
   
   public EventBean(BeanManagerImpl manager)
   {
      super(Event.class.getSimpleName(), manager);
   }

   @Override
   public Class<Event<?>> getType()
   {
      return TYPE;
   }

   @Override
   public Class<?> getBeanClass()
   {
      return EventImpl.class;
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
   protected Event<?> newInstance(Type type, InjectionPoint ip)
   {
      Set<Annotation> qualifiers = new HashSet<Annotation>(ip.getQualifiers());
      if (!ip.getAnnotated().isAnnotationPresent(Default.class))
      {
         // Remove any @Default if it doesn't appear on the injection point
         qualifiers.remove(DEFAULT);
      }
      return EventImpl.of(type, getManager(), qualifiers);
   }
   
   @Override
   public String toString()
   {
      return "Built-in implicit javax.event.Event bean";
   }
   
}
