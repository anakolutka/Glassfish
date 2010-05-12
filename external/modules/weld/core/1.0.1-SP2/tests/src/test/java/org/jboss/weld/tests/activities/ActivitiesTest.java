/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.tests.activities;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.util.AnnotationLiteral;

import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.weld.bean.ForwardingBean;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.test.AbstractWeldTest;
import org.jboss.weld.util.collections.Arrays2;
import org.testng.annotations.Test;

/**
 * 
 * Spec version: 20090519
 * 
 */
@Artifact
public class ActivitiesTest extends AbstractWeldTest
{

   private static final Set<Annotation> DEFAULT_QUALIFIERS = Collections.<Annotation> singleton(DefaultLiteral.INSTANCE);

   private Bean<?> createDummyBean(BeanManager beanManager, final Type injectionPointType)
   {
      final Set<InjectionPoint> injectionPoints = new HashSet<InjectionPoint>();
      final Set<Type> types = new HashSet<Type>();
      final Set<Annotation> bindings = new HashSet<Annotation>();
      bindings.add(new AnnotationLiteral<Tame>()
      {
      });
      types.add(Object.class);
      final Bean<?> bean = new Bean<Object>()
      {

         public Set<Annotation> getQualifiers()
         {
            return bindings;
         }

         public Set<InjectionPoint> getInjectionPoints()
         {
            return injectionPoints;
         }

         public String getName()
         {
            return null;
         }

         public Class<? extends Annotation> getScope()
         {
            return Dependent.class;
         }

         public Set<Type> getTypes()
         {
            return types;
         }

         public boolean isNullable()
         {
            return false;
         }

         public Object create(CreationalContext<Object> creationalContext)
         {
            return null;
         }

         public void destroy(Object instance, CreationalContext<Object> creationalContext)
         {

         }

         public Class<?> getBeanClass()
         {
            return Object.class;
         }

         public boolean isAlternative()
         {
            return false;
         }

         public Set<Class<? extends Annotation>> getStereotypes()
         {
            return Collections.emptySet();
         }

      };
      InjectionPoint injectionPoint = new InjectionPoint()
      {

         public Bean<?> getBean()
         {
            return bean;
         }

         public Set<Annotation> getQualifiers()
         {
            return DEFAULT_QUALIFIERS;
         }

         public Member getMember()
         {
            return null;
         }

         public Type getType()
         {
            return injectionPointType;
         }

         public Annotated getAnnotated()
         {
            return null;
         }

         public boolean isDelegate()
         {
            return false;
         }

         public boolean isTransient()
         {
            return false;
         }

      };
      injectionPoints.add(injectionPoint);
      return bean;
   }

   private static class DummyContext implements Context
   {

      public <T> T get(Contextual<T> contextual)
      {
         return null;
      }

      public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext)
      {
         return null;
      }

      public Class<? extends Annotation> getScope()
      {
         return Dummy.class;
      }

      public boolean isActive()
      {
         return false;
      }

   }

   @Test
   public void testBeanBelongingToParentActivityBelongsToChildActivity()
   {
      assert getBeans(Cow.class).size() == 1;
      Contextual<?> bean = getBeans(Cow.class).iterator().next();
      BeanManager childActivity = getCurrentManager().createActivity();
      assert childActivity.getBeans(Cow.class).size() == 1;
      assert childActivity.getBeans(Cow.class).iterator().next().equals(bean);
   }

   @Test
   public void testBeanBelongingToParentActivityCanBeInjectedIntoChildActivityBean()
   {
      assert getBeans(Cow.class).size() == 1;
      Contextual<?> bean = getBeans(Cow.class).iterator().next();
      BeanManagerImpl childActivity = getCurrentManager().createActivity();
      Bean<?> dummyBean = createDummyBean(childActivity, Cow.class);
      childActivity.addBean(dummyBean);
      assert childActivity.getInjectableReference(dummyBean.getInjectionPoints().iterator().next(), childActivity.createCreationalContext(dummyBean)) != null;
   }

   @Test
   public void testObserverBelongingToParentActivityBelongsToChildActivity()
   {
      assert getCurrentManager().resolveObserverMethods(new NightTime()).size() == 1;
      ObserverMethod<?> observer = getCurrentManager().resolveObserverMethods(new NightTime()).iterator().next();
      BeanManager childActivity = getCurrentManager().createActivity();
      assert childActivity.resolveObserverMethods(new NightTime()).size() == 1;
      assert childActivity.resolveObserverMethods(new NightTime()).iterator().next().equals(observer);
   }

   @Test
   public void testObserverBelongingToParentFiresForChildActivity()
   {
      Fox.setObserved(false);
      BeanManager childActivity = getCurrentManager().createActivity();
      childActivity.fireEvent(new NightTime());
      assert Fox.isObserved();
   }

   @Test
   public void testContextObjectBelongingToParentBelongsToChild()
   {
      Context context = new DummyContext()
      {

         @Override
         public boolean isActive()
         {
            return true;
         }

      };
      getCurrentManager().addContext(context);
      BeanManager childActivity = getCurrentManager().createActivity();
      assert childActivity.getContext(Dummy.class) != null;
   }

   @Test
   public void testBeanBelongingToChildActivityCannotBeInjectedIntoParentActivityBean()
   {
      assert getBeans(Cow.class).size() == 1;
      BeanManagerImpl childActivity = getCurrentManager().createActivity();
      Bean<?> dummyBean = createDummyBean(childActivity, Cow.class);
      childActivity.addBean(dummyBean);
      assert getBeans(Object.class, new AnnotationLiteral<Tame>()
      {
      }).size() == 0;
   }

   @Test(expectedExceptions = UnsatisfiedResolutionException.class)
   public void testInstanceProcessedByParentActivity()
   {
      Context dummyContext = new DummyContext();
      getCurrentManager().addContext(dummyContext);
      assert getBeans(Cow.class).size() == 1;
      final Bean<Cow> bean = getBeans(Cow.class).iterator().next();
      BeanManagerImpl childActivity = getCurrentManager().createActivity();
      final Set<Annotation> bindingTypes = new HashSet<Annotation>();
      bindingTypes.add(new AnnotationLiteral<Tame>()
      {
      });
      childActivity.addBean(new ForwardingBean<Cow>()
      {

         @Override
         protected Bean<Cow> delegate()
         {
            return bean;
         }

         @Override
         public Set<Annotation> getQualifiers()
         {
            return bindingTypes;
         }

         @Override
         public Set<Class<? extends Annotation>> getStereotypes()
         {
            return Collections.emptySet();
         }

      });
      getReference(Field.class).get();
   }

   @Test
   public void testObserverBelongingToChildDoesNotFireForParentActivity()
   {

      BeanManagerImpl childActivity = getCurrentManager().createActivity();
      ObserverMethod<NightTime> observer = new ObserverMethod<NightTime>()
      {

         public void notify(NightTime event)
         {
            assert false;
         }

         public Class<?> getBeanClass()
         {
            return NightTime.class;
         }

         public Set<Annotation> getObservedQualifiers()
         {
            return Arrays2.asSet(AnyLiteral.INSTANCE, DefaultLiteral.INSTANCE);
         }

         public Type getObservedType()
         {
            return NightTime.class;
         }

         public Reception getReception()
         {
            return Reception.ALWAYS;
         }

         public TransactionPhase getTransactionPhase()
         {
            return TransactionPhase.IN_PROGRESS;
         }

      };
      // TODO Fix this test to use an observer method in a child activity
      childActivity.addObserver(observer);
      getCurrentManager().fireEvent(new NightTime());
   }

}
