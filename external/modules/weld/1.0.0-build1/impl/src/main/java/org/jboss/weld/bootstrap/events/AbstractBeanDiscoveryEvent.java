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
import java.util.Map;

import org.jboss.weld.BeanManagerImpl;
import org.jboss.weld.bootstrap.BeanDeployment;
import org.jboss.weld.bootstrap.ExtensionBeanDeployerEnvironment;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.metadata.TypeStore;

/**
 * @author  pmuir
 */
public abstract class AbstractBeanDiscoveryEvent extends AbstractDefinitionContainerEvent
{
   
   private final Map<BeanDeploymentArchive, BeanDeployment> beanDeployments;
   private final Deployment deployment;
   private final ExtensionBeanDeployerEnvironment extensionBeanDeployerEnvironment;
   
   public AbstractBeanDiscoveryEvent(BeanManagerImpl deploymentManager, Type rawType, Map<BeanDeploymentArchive, BeanDeployment> beanDeployments, Deployment deployment, ExtensionBeanDeployerEnvironment extensionBeanDeployerEnvironment)
   {
      super(deploymentManager, rawType, EMPTY_TYPE_ARRAY);
      this.beanDeployments = beanDeployments;
      this.deployment = deployment;
      this.extensionBeanDeployerEnvironment = extensionBeanDeployerEnvironment;
   }
   
   /**
    * @return the beanDeployments
    */
   protected Map<BeanDeploymentArchive, BeanDeployment> getBeanDeployments()
   {
      return beanDeployments;
   }
   
   /**
    * @return the deployment
    */
   protected Deployment getDeployment()
   {
      return deployment;
   }
   
   protected TypeStore getTypeStore()
   {
      return getDeployment().getServices().get(TypeStore.class);
   }
   

   protected BeanDeployment getOrCreateBeanDeployment(Class<?> clazz)
   {
      BeanDeploymentArchive beanDeploymentArchive = getDeployment().loadBeanDeploymentArchive(clazz);
      if (beanDeploymentArchive == null)
      {
         throw new IllegalStateException("Unable to find Bean Deployment Archive for " + clazz);
      }
      else
      {
         if (getBeanDeployments().containsKey(beanDeploymentArchive))
         {
            return getBeanDeployments().get(beanDeploymentArchive);
         }
         else
         {
            BeanDeployment beanDeployment = new BeanDeployment(beanDeploymentArchive, getBeanManager(), getDeployment(), extensionBeanDeployerEnvironment, getDeployment().getServices());
            getBeanDeployments().put(beanDeploymentArchive, beanDeployment);
            return beanDeployment;
         }
      }
   }
   
}