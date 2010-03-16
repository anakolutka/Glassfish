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
package org.jboss.weld.tests.unit.environments.servlet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;

import org.jboss.weld.bean.ManagedBean;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.mock.MockServletLifecycle;
import org.jboss.weld.mock.TestContainer;
import org.jboss.weld.test.Utils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ServletEnvironmentTest
{
   
   private TestContainer container;
   private BeanManagerImpl manager;
   
   @BeforeClass
   public void beforeClass() throws Throwable
   {
      container = new TestContainer(new MockServletLifecycle(), Arrays.asList(Animal.class, DeadlyAnimal.class, DeadlySpider.class, DeadlyAnimal.class, Hound.class, HoundLocal.class, Salmon.class, ScottishFish.class, SeaBass.class, Sole.class, Spider.class, Tarantula.class, TarantulaProducer.class, Tuna.class), null);
      container.startContainer();
      container.ensureRequestActive();
      manager = container.getBeanManager();
   }
   
   @AfterClass(alwaysRun=true)
   public void afterClass() throws Exception
   {
      container.stopContainer();
      container = null;
      manager = null;
   }
   
   @Test
   public void testSimpleBeans()
   {
      Map<Class<?>, Bean<?>> beans = new HashMap<Class<?>, Bean<?>>();
      for (Bean<?> bean : manager.getBeans())
      {
         if (bean instanceof RIBean<?>)
         {
            beans.put(((RIBean<?>) bean).getType(), bean);
         }
      }
      assert beans.containsKey(Tuna.class);
      assert beans.containsKey(Salmon.class);
      assert beans.containsKey(SeaBass.class);
      assert beans.containsKey(Sole.class);
      
      assert beans.get(Tuna.class) instanceof ManagedBean<?>;
      assert beans.get(Salmon.class) instanceof ManagedBean<?>;
      assert beans.get(SeaBass.class) instanceof ManagedBean<?>;
      assert beans.get(Sole.class) instanceof ManagedBean<?>;
      Utils.getReference(manager, Sole.class, new AnnotationLiteral<Whitefish>() {}).ping();
   }
   
   @Test
   public void testProducerMethodBean()
   {
      Map<Class<?>, Bean<?>> beans = new HashMap<Class<?>, Bean<?>>();
      for (Bean<?> bean : manager.getBeans())
      {
         if (bean instanceof RIBean<?>)
         {
            beans.put(((RIBean<?>) bean).getType(), bean);
         }
      }
      assert beans.containsKey(TarantulaProducer.class);
      assert beans.containsKey(Tarantula.class);
      
      beans.get(TarantulaProducer.class);
      
      assert beans.get(TarantulaProducer.class) instanceof ManagedBean<?>;
      Utils.getReference(manager, Tarantula.class, new AnnotationLiteral<Tame>() {}).ping();
   }
   
   public void testSingleEnterpriseBean()
   {
      List<Bean<?>> beans = manager.getBeans();
      Map<Class<?>, Bean<?>> classes = new HashMap<Class<?>, Bean<?>>();
      for (Bean<?> bean : beans)
      {
         if (bean instanceof RIBean<?>)
         {
            classes.put(((RIBean<?>) bean).getType(), bean);
         }
      }
      assert classes.containsKey(Hound.class);
      assert classes.get(Hound.class) instanceof ManagedBean<?>;
      Utils.getReference(manager, HoundLocal.class, new AnnotationLiteral<Tame>() {}).ping();
   }
   
}
