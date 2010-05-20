/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.bootstrap.events;

import java.lang.reflect.Member;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;

import org.jboss.weld.bean.AbstractProducerBean;
import org.jboss.weld.manager.BeanManagerImpl;


public class ProcessProducerImpl<X, T> extends AbstractDefinitionContainerEvent implements ProcessProducer<X, T>
{
   
   public static <X, T> void fire(BeanManagerImpl beanManager, AbstractProducerBean<X, T, Member> producer)
   {
      new ProcessProducerImpl<X, T>(beanManager, (AnnotatedMember<X>) producer.getWeldAnnotated(), producer) {}.fire();
   }
   
   private final AnnotatedMember<X> annotatedMember;
   private AbstractProducerBean<X, T, ?> bean;

   public ProcessProducerImpl(BeanManagerImpl beanManager, AnnotatedMember<X> annotatedMember, AbstractProducerBean<X, T, ?> bean)
   {
      super(beanManager, ProcessProducer.class, new Type[] { bean.getWeldAnnotated().getDeclaringType().getBaseType(), bean.getWeldAnnotated().getBaseType() });
      this.bean = bean;
      this.annotatedMember = annotatedMember;
   }

   public void addDefinitionError(Throwable t)
   {
      getErrors().add(t);
   }

   public AnnotatedMember<X> getAnnotatedMember()
   {
      return annotatedMember;
   }

   public Producer<T> getProducer()
   {
      return bean.getProducer();
   }

   public void setProducer(Producer<T> producer)
   {
      this.bean.setProducer(producer);
   }

}
