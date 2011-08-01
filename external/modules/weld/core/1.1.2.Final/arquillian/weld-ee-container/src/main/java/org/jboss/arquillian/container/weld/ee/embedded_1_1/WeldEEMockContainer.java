/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.arquillian.container.weld.ee.embedded_1_1;

import static org.jboss.arquillian.container.weld.ee.embedded_1_1.Utils.findArchiveId;
import static org.jboss.arquillian.container.weld.ee.embedded_1_1.Utils.findBeanClasses;
import static org.jboss.arquillian.container.weld.ee.embedded_1_1.Utils.findBeansXml;

import org.jboss.arquillian.container.weld.ee.embedded_1_1.mock.TestContainer;
import org.jboss.arquillian.protocol.local.LocalMethodExecutor;
import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.DeployableContainer;
import org.jboss.arquillian.spi.DeploymentException;
import org.jboss.arquillian.spi.LifecycleException;
import org.jboss.arquillian.spi.event.container.AfterDeploy;
import org.jboss.arquillian.spi.event.container.BeforeUnDeploy;
import org.jboss.arquillian.spi.event.suite.After;
import org.jboss.arquillian.spi.event.suite.Before;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.classloader.ShrinkWrapClassLoader;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.manager.api.WeldManager;

/**
 * WeldEEMockConainer
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class WeldEEMockContainer implements DeployableContainer
{
   public void setup(Context context, Configuration configuration)
   {
   }

   public void start(Context context) throws LifecycleException
   {
   }

   public ContainerMethodExecutor deploy(Context context, Archive<?> archive) throws DeploymentException
   {  
      boolean enableConversation = context.get(Configuration.class)
                                          .getContainerConfig(WeldEEMockConfiguration.class)
                                          .isEnableConversationScope();
      
      ShrinkWrapClassLoader classLoader = new ShrinkWrapClassLoader(archive.getClass().getClassLoader(), archive);
      ContextClassLoaderManager classLoaderManager = new ContextClassLoaderManager(classLoader);
      classLoaderManager.enable();
      
      TestContainer container = new TestContainer(findArchiveId(archive), findBeansXml(archive), findBeanClasses(archive, classLoader));
      Bootstrap bootstrap = container.getBootstrap();

      context.add(ContextClassLoaderManager.class, classLoaderManager);

      container.startContainer();
      
      context.add(TestContainer.class, container);
      context.add(Bootstrap.class, bootstrap);
      // Assume a flat structure
      context.add(WeldManager.class, container.getBeanManager(container.getDeployment().getBeanDeploymentArchives().iterator().next()));

      context.register(AfterDeploy.class, new SessionLifeCycleCreator());
      context.register(BeforeUnDeploy.class, new SessionLifeCycleDestroyer());
      
      context.register(Before.class, new RequestLifeCycleCreator());

      if(enableConversation)
      {
         context.register(Before.class, new ConversationLifeCycleCreator());         
         context.register(After.class, new ConversationLifeCycleDestroyer());
      }
      context.register(After.class, new RequestLifeCycleDestroyer());
      
      return new LocalMethodExecutor();
   }

   public void undeploy(Context context, Archive<?> archive) throws DeploymentException
   {
      TestContainer container = context.get(TestContainer.class);
      if(container != null)
      {
         container.stopContainer();
      }
      ContextClassLoaderManager classLoaderManager = context.get(ContextClassLoaderManager.class);
      classLoaderManager.disable();
   }

   public void stop(Context context) throws LifecycleException
   {
   }
}
