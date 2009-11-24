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
package org.jboss.weld.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;

import org.jboss.interceptor.util.InterceptionUtils;
import org.jboss.weld.BeanManagerImpl;
import org.jboss.weld.bootstrap.BeanDeployerEnvironment;
import org.jboss.weld.introspector.WeldField;
import org.jboss.weld.util.Names;

/**
 * Represents a producer field
 * 
 * @author Pete Muir
 * 
 * @param <T>
 */
public class ProducerField<X, T> extends AbstractProducerBean<X, T, Field>
{
   // The underlying field
   private WeldField<T, X> field;
   
   /**
    * Creates a producer field
    * 
    * @param field The underlying method abstraction
    * @param declaringBean The declaring bean abstraction
    * @param manager the current manager
    * @return A producer field
    */
   public static <X, T> ProducerField<X, T> of(WeldField<T, X> field, AbstractClassBean<X> declaringBean, BeanManagerImpl manager)
   {
      return new ProducerField<X, T>(field, declaringBean, manager);
   }

   /**
    * Constructor
    * 
    * @param method The producer field abstraction
    * @param declaringBean The declaring bean
    * @param manager The Bean manager
    */
   protected ProducerField(WeldField<T, X> field, AbstractClassBean<X> declaringBean, BeanManagerImpl manager)
   {
      super(new StringBuilder().append(ProducerField.class.getSimpleName()).append(BEAN_ID_SEPARATOR).append(declaringBean.getAnnotatedItem().getName()).append(".").append(field.getName()).toString(), declaringBean, manager);
      this.field = field;
      initType();
      initTypes();
      initBindings();
      initStereotypes();
      initAlternative();
   }
   
   @Override
   public void initialize(BeanDeployerEnvironment environment)
   {
      if (!isInitialized())
      {
         super.initialize(environment);
         setProducer(new Producer<T>()
         {

            public void dispose(T instance)
            {
               defaultDispose(instance);
            }

            public Set<InjectionPoint> getInjectionPoints()
            {
               return (Set) getAnnotatedInjectionPoints();
            }

            public T produce(CreationalContext<T> creationalContext)
            {
               // unwrap if we have a proxy
               return field.get(InterceptionUtils.getRawInstance(getReceiver(creationalContext)));
            }
            
         });
      }
   }
   
   protected void defaultDispose(T instance)
   {
      // No disposal by default
   }

   public void destroy(T instance, CreationalContext<T> creationalContext)
   {
      getProducer().dispose(instance);
   }

   /**
    * Gets the annotated item representing the field
    * 
    * @return The annotated item
    */
   @Override
   public WeldField<T, X> getAnnotatedItem()
   {
      return field;
   }

   /**
    * Returns the default name
    * 
    * @return The default name
    */
   @Override
   protected String getDefaultName()
   {
      return field.getPropertyName();
   }

   /**
    * Gets a string representation
    * 
    * @return The string representation
    */
   @Override
   public String getDescription()
   {
      StringBuilder buffer = new StringBuilder();
      buffer.append("Annotated " + Names.scopeTypeToString(getScope()));
      if (getName() == null)
      {
         buffer.append("unnamed producer field bean");
      }
      else
      {
         buffer.append("simple producer field bean '" + getName() + "'");
      }
      buffer.append(" [" + getBeanClass().getName() + "] for class type [" + getType().getName() + "] API types " + getTypes() + ", binding types " + getQualifiers());
      return buffer.toString();
   }
   
   @Override
   public AbstractBean<?, ?> getSpecializedBean()
   {
      return null;
   }
   
   @Override
   public boolean isSpecializing()
   {
      return false;
   }

   @Override
   public Set<Class<? extends Annotation>> getStereotypes()
   {
      return Collections.emptySet();
   }

}
