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

package org.jboss.weld.tests.generic;

import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.weld.test.AbstractWeldTest;
import org.testng.annotations.Test;

/**
 * @author Marius Bogoevici
 */
@Artifact
public class GenericBeanTest extends AbstractWeldTest
{

   @Test
   public void testGenericBean()
   {
      TestBean testBean = getReference(TestBean.class);
      assert "Hello".equals(testBean.echo("Hello"));
      assert Integer.valueOf(1).equals(testBean.echo(1));
      Subclass subclassInstance = new Subclass();
      assert subclassInstance == testBean.echo(subclassInstance);
      assert subclassInstance == testBean.echo((BaseClass)subclassInstance);
      BaseClass baseInstance = new BaseClass();
      assert baseInstance == testBean.echo(baseInstance);
   }

}