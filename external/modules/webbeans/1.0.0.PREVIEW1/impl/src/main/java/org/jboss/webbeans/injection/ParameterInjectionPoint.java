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
package org.jboss.webbeans.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Set;

import javax.context.CreationalContext;
import javax.inject.manager.Bean;

import org.jboss.webbeans.ManagerImpl;
import org.jboss.webbeans.introspector.AnnotatedParameter;
import org.jboss.webbeans.introspector.ForwardingAnnotatedParameter;

public class ParameterInjectionPoint<T> extends ForwardingAnnotatedParameter<T> implements AnnotatedInjectionPoint<T, Object>
{
   
   private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];
   
   public static <T> ParameterInjectionPoint<T> of(Bean<?> declaringBean, AnnotatedParameter<T> parameter)
   {
      return new ParameterInjectionPoint<T>(declaringBean, parameter);
   }
   
   private final Bean<?> declaringBean;
   private final AnnotatedParameter<T> parameter;

   private ParameterInjectionPoint(Bean<?> declaringBean, AnnotatedParameter<T> parameter)
   {
      this.declaringBean = declaringBean;
      this.parameter = parameter;
   }

   @Override
   protected AnnotatedParameter<T> delegate()
   {
      return parameter;
   }

   public Annotation[] getAnnotations()
   {
      return delegate().getAnnotationStore().getAnnotations().toArray(EMPTY_ANNOTATION_ARRAY);
   }

   public Bean<?> getBean()
   {
      return declaringBean;
   }

   public Set<Annotation> getBindings()
   {
      return delegate().getBindings();
   }

   public Member getMember()
   {
      return delegate().getDeclaringMember().getMember();
   }
   
   public void inject(Object declaringInstance, Object value)
   {
      throw new UnsupportedOperationException();
   }
   
   public T getValueToInject(ManagerImpl manager, CreationalContext<?> creationalContext)
   {
      return manager.<T>getInstanceToInject(this, creationalContext);
   }

}
