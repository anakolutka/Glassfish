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
package org.jboss.webbeans.bootstrap;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;

import org.jboss.webbeans.BeanManagerImpl;
import org.jboss.webbeans.literal.BindingTypeLiteral;
import org.jboss.webbeans.literal.InterceptorBindingTypeLiteral;
import org.jboss.webbeans.literal.ScopeTypeLiteral;
import org.jboss.webbeans.metadata.TypeStore;

public class BeforeBeanDiscoveryImpl implements BeforeBeanDiscovery
{
   
   private final TypeStore typeStore;
   private final BeanDeployer beanDeployer;
   
   public BeforeBeanDiscoveryImpl(BeanManagerImpl manager, BeanDeployer beanDeployer)
   {
      this.typeStore = manager.getServices().get(TypeStore.class);
      this.beanDeployer = beanDeployer;
   }

   public void addBindingType(Class<? extends Annotation> bindingType)
   {
      typeStore.add(bindingType, new BindingTypeLiteral());
   }

   public void addInterceptorBindingType(Class<? extends Annotation> bindingType)
   {
      typeStore.add(bindingType, new InterceptorBindingTypeLiteral());
   }

   public void addScopeType(Class<? extends Annotation> scopeType,
         boolean normal, boolean passivating)
   {
      typeStore.add(scopeType, new ScopeTypeLiteral(normal, passivating));
   }

   public void addStereotype(Class<? extends Annotation> stereotype,
         Annotation... stereotypeDef)
   {
      throw new UnsupportedOperationException();
   }
   
   public void addAnnotatedType(AnnotatedType<?> type)
   {
      beanDeployer.addClass(type);
   }

}
