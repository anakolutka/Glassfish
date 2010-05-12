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
package org.jboss.weld.tests.beanDeployment.mixed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.Bean;

import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.testharness.impl.packaging.Packaging;
import org.jboss.testharness.impl.packaging.PackagingType;
import org.jboss.weld.bean.ManagedBean;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.test.AbstractWeldTest;
import org.testng.annotations.Test;

@Artifact
@Packaging(PackagingType.EAR)
public class BootstrapTest extends AbstractWeldTest
{
   
   @Test(groups="bootstrap")
   public void testMultipleEnterpriseAndSimpleBean()
   {
      List<Bean<?>> beans = getCurrentManager().getBeans();
      Map<Class<?>, Bean<?>> classes = new HashMap<Class<?>, Bean<?>>();
      for (Bean<?> bean : beans)
      {
         if (bean instanceof RIBean)
         {
            classes.put(((RIBean<?>) bean).getType(), bean);
         }
      }
      assert classes.containsKey(Hound.class);
      assert classes.containsKey(Elephant.class);
      assert classes.containsKey(Panther.class);
      assert classes.containsKey(Tiger.class);
      assert classes.containsKey(Tuna.class);
      assert classes.containsKey(Salmon.class);
      assert classes.containsKey(SeaBass.class);
      assert classes.containsKey(Sole.class);
      
      assert classes.get(Hound.class) instanceof SessionBean;
      assert classes.get(Elephant.class) instanceof SessionBean;
      assert classes.get(Panther.class) instanceof SessionBean;
      assert classes.get(Tiger.class) instanceof SessionBean;
      assert classes.get(Tuna.class) instanceof ManagedBean;
      assert classes.get(Salmon.class) instanceof ManagedBean;
      assert classes.get(SeaBass.class) instanceof ManagedBean;
      assert classes.get(Sole.class) instanceof ManagedBean;
   }
   
}
