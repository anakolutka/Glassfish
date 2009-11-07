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
package org.jboss.weld.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Set;

import javax.decorator.Delegate;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;

import org.jboss.weld.BeanManagerImpl;
import org.jboss.weld.introspector.ForwardingWeldParameter;
import org.jboss.weld.introspector.WeldParameter;

public class ParameterInjectionPoint<T, X> extends ForwardingWeldParameter<T, X> implements WeldInjectionPoint<T, Object>
{

   private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

   public static <T, X> ParameterInjectionPoint<T, X> of(Bean<?> declaringBean, WeldParameter<T, X> parameter)
   {
      return new ParameterInjectionPoint<T, X>(declaringBean, parameter);
   }

   private final Bean<?> declaringBean;
   private final WeldParameter<T, X> parameter;
   private final boolean delegate;

   private ParameterInjectionPoint(Bean<?> declaringBean, WeldParameter<T, X> parameter)
   {
      this.declaringBean = declaringBean;
      this.parameter = parameter;
      this.delegate = isAnnotationPresent(Delegate.class) && declaringBean instanceof Decorator<?>;
   }

   @Override
   protected WeldParameter<T, X> delegate()
   {
      return parameter;
   }

   public Bean<?> getBean()
   {
      return declaringBean;
   }

   @Override
   public Set<Annotation> getQualifiers()
   {
      return delegate().getQualifiers();
   }

   public Member getJavaMember()
   {
      return delegate().getDeclaringCallable().getJavaMember();
   }

   public void inject(Object declaringInstance, Object value)
   {
      throw new UnsupportedOperationException();
   }

   @SuppressWarnings("unchecked")
   public T getValueToInject(BeanManagerImpl manager, CreationalContext<?> creationalContext)
   {
      return (T) manager.getInjectableReference(this, creationalContext);
   }

   public Annotated getAnnotated()
   {
      return delegate();
   }

   public boolean isDelegate()
   {
      return delegate;
   }

   public boolean isTransient()
   {
      // TODO Auto-generated method stub
      return false;
   }

   public Type getType()
   {
      return getBaseType();
   }

   public Member getMember()
   {
      return getJavaMember();
   }


}
