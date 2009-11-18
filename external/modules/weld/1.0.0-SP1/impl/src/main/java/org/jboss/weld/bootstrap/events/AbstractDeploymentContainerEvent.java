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
package org.jboss.weld.bootstrap.events;

import java.lang.reflect.Type;

import org.jboss.weld.BeanManagerImpl;
import org.jboss.weld.DeploymentException;

/**
 * @author pmuir
 *
 */
public abstract class AbstractDeploymentContainerEvent extends AbstractContainerEvent
{

   protected AbstractDeploymentContainerEvent(BeanManagerImpl beanManager, Type rawType, Type[] actualTypeArguments)
   {
      super(beanManager, rawType, actualTypeArguments);
   }

   @Override
   protected void fire()
   {
      super.fire();
      if (!getErrors().isEmpty())
      {
         // FIXME communicate all the captured deployment errors in this exception
         throw new DeploymentException(getErrors().get(0));
      }
   }
   
}
