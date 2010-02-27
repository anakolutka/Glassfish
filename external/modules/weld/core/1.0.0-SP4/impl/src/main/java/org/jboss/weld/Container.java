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
package org.jboss.weld;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.weld.bootstrap.BeanDeployment;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.Singleton;
import org.jboss.weld.bootstrap.api.SingletonProvider;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

/**
 * A Weld application container
 * 
 * @author pmuir
 *
 */
public class Container
{
   
   /**
    * Container status
    * @author pmuir
    *
    */
   public enum Status
   {
      /**
       * The container has not been started
       */
      STOPPED(false),
      /**
       * The container is starting
       */
      STARTING(false),
      /**
       * The container has started and beans have been deployed
       */
      INITIALIZED(true),
      /**
       * The deployment has been validated
       */
      VALIDATED(true),
      /**
       * The container has been shutdown
       */
      SHUTDOWN(false);
      
      private Status(boolean available)
      {
         this.available = available;
      }
      
      final boolean available;
      
      /**
       * Whether the container is available for use
       * 
       * @return
       */
      public boolean isAvailable()
      {
         return available;
      }
   }
   
   private final static Singleton<Container> instance;
   
   static
   {
      instance = SingletonProvider.instance().create(Container.class);
   }
   
   /**
    * Get the container for the current application deployment
    * 
    * @return
    */
   public static Container instance()
   {
      return instance.get();
   }
   
   public static boolean available()
   {
      return instance.isSet() && instance() != null && instance().getStatus().isAvailable();
   }
   

   /**
    * Initialize the container for the current application deployment
    * 
    * @param deploymentManager
    * @param deploymentServices
    */
   public static void initialize(BeanManagerImpl deploymentManager, ServiceRegistry deploymentServices)
   {
      Container instance = new Container(deploymentManager, deploymentServices);
      Container.instance.set(instance);
   }
   
   // The deployment bean manager
   private final BeanManagerImpl deploymentManager;
   
   // A map of managers keyed by ID, used for activities and serialization
   private final Map<String, BeanManagerImpl> managers;
   
   // A map of BDA -> bean managers
   private final Map<BeanDeploymentArchive, BeanManagerImpl> beanDeploymentArchives;
   
   private final ServiceRegistry deploymentServices;
   
   private Status status = Status.STOPPED;
   
   public Container(BeanManagerImpl deploymentManager, ServiceRegistry deploymentServices)
   {
      this.deploymentManager = deploymentManager;
      this.managers = new ConcurrentHashMap<String, BeanManagerImpl>();
      this.managers.put(deploymentManager.getId(), deploymentManager);
      this.beanDeploymentArchives = new ConcurrentHashMap<BeanDeploymentArchive, BeanManagerImpl>();
      this.deploymentServices = deploymentServices;
   }

   /**
    * Cause the container to be cleaned up, including all registered bean 
    * managers, and all deployment services
    */
   public void cleanup()
   {
      // TODO We should probably cleanup the bean managers for activities?
      managers.clear();
      
      for (BeanManagerImpl beanManager : beanDeploymentArchives.values())
      {
         beanManager.cleanup();
      }
      beanDeploymentArchives.clear();
      
      deploymentServices.cleanup();
      deploymentManager.cleanup();
   }
   
   /**
    * Gets the manager for this application deployment
    * 
    */
   public BeanManagerImpl deploymentManager()
   {
      return deploymentManager;
   }
   public Map<BeanDeploymentArchive, BeanManagerImpl> beanDeploymentArchives()
   {
      return beanDeploymentArchives;
   }
   
   /**
    * Get the activity manager for a given key
    * 
    * @param key
    * @return
    */
   public BeanManagerImpl activityManager(String key)
   {
      return managers.get(key);
   }
   
   /**
    * Add an activity
    * 
    * @param manager
    * @return
    */
   public String addActivity(BeanManagerImpl manager)
   {
      String id = manager.getId();
      if (manager.getId() == null)
      {
         throw new IllegalArgumentException("Bean manager must not be null " + manager.toString());
      }
      managers.put(id, manager);
      return id;
   }
   
   /**
    * Get the services for this application deployment
    * 
    * @return the deploymentServices
    */
   public ServiceRegistry deploymentServices()
   {
      return deploymentServices;
   }

   /**
    * Add sub-deployment units to the container
    * 
    * @param beanDeployments
    */
   public void putBeanDeployments(Map<BeanDeploymentArchive, BeanDeployment> beanDeployments)
   {
      for (Entry<BeanDeploymentArchive, BeanDeployment> entry : beanDeployments.entrySet())   
      {
         beanDeploymentArchives.put(entry.getKey(), entry.getValue().getBeanManager());
         addActivity(entry.getValue().getBeanManager());
      }
   }
   
   public Status getStatus()
   {
      return status;
   }
   
   public void setStatus(Status status)
   {
      this.status = status;
   }

}
