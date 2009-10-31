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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;

import org.jboss.weld.BeanManagerImpl;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.bootstrap.BeanDeployerEnvironment;
import org.jboss.weld.injection.WeldInjectionPoint;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;

public abstract class AbstractBuiltInBean<T> extends RIBean<T>
{
   
   private static final String ID_PREFIX = "Built-in";
   
   private static final Annotation[] DEFAULT_BINDING_ARRAY = { new DefaultLiteral(), new AnyLiteral() };
   private static final Set<Annotation> DEFAULT_BINDING = new HashSet<Annotation>(Arrays.asList(DEFAULT_BINDING_ARRAY));
   
   protected AbstractBuiltInBean(String idSuffix, BeanManagerImpl manager)
   {
      super(new StringBuilder().append(ID_PREFIX).append(BEAN_ID_SEPARATOR).append(idSuffix).toString(), manager);
   }
   
   @Override
   public void initialize(BeanDeployerEnvironment environment)
   {
      // No-op
   }

   
   public Set<Annotation> getQualifiers()
   {
      return DEFAULT_BINDING;
   }
   
   public Class<? extends Annotation> getScope()
   {
      return Dependent.class;
   }
   
   @Override
   public RIBean<?> getSpecializedBean()
   {
      return null;
   }
   
   public String getName()
   {
      return null;
   }
   
   public Set<Class<? extends Annotation>> getStereotypes()
   {
      return Collections.emptySet();
   }

   @Override
   public Set<WeldInjectionPoint<?, ?>> getAnnotatedInjectionPoints()
   {
      return Collections.emptySet();
   }
   
   public boolean isNullable()
   {
      return true;
   }
   
   @Override
   public boolean isPrimitive()
   {
      return false;
   }
   
   @Override
   public boolean isSpecializing()
   {
      return false;
   }
   
   public boolean isAlternative()
   {
      return false;
   }

   @Override
   public boolean isProxyable()
   {
      return false;
   }
   
   @Override
   public String getDescription()
   {
      return "Built-in bean " + getClass().getSimpleName();
   }
   
}
