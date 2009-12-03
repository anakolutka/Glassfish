/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.weld.tests.interceptors.ejb3model;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.weld.test.AbstractWeldTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * @author Marius Bogoevici
 */
@Artifact
public class Ejb3InterceptionModelTest extends AbstractWeldTest
{
   @BeforeMethod
   public void reset()
   {
      Ball.played = false;
      Goalkeeper.caught = false;
      Defender.defended = false;
      Ball.aroundInvoke = false;
   }

   @Test
   public void testSimpleInterceptor()
   {
      Bean bean = getCurrentManager().getBeans(Ball.class).iterator().next();
      CreationalContext creationalContext = getCurrentManager().createCreationalContext(bean);
      Ball ball = (Ball) bean.create(creationalContext);
      ball.shoot();
      assert Defender.defended;
      assert Ball.played;
      assert !Goalkeeper.caught;
      assert Ball.aroundInvoke;
   }


   @Test
   public void testSimpleInterceptor2()
   {
      Bean bean = getCurrentManager().getBeans(Ball.class).iterator().next();
      CreationalContext creationalContext = getCurrentManager().createCreationalContext(bean);
      Ball ball = (Ball) bean.create(creationalContext);
      ball.pass();
      assert Defender.defended;
      assert Ball.played;
      assert Goalkeeper.caught;
      assert Ball.aroundInvoke;
   }

   @Test
   public void testSimpleInterceptor3()
   {
      Bean bean = getCurrentManager().getBeans(Ball.class).iterator().next();
      CreationalContext creationalContext = getCurrentManager().createCreationalContext(bean);
      Ball ball = (Ball) bean.create(creationalContext);
      ball.lob();
      assert !Defender.defended;
      assert Ball.played;
      assert Goalkeeper.caught;
      assert Ball.aroundInvoke;
   }

}
