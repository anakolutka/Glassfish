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
package org.jboss.webbeans.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.ScopeType;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Initializer;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.deployment.DeploymentType;
import javax.enterprise.inject.deployment.Production;
import javax.enterprise.inject.spi.Decorator;

import org.jboss.webbeans.BeanManagerImpl;
import org.jboss.webbeans.DefinitionException;
import org.jboss.webbeans.bean.proxy.DecoratorProxyMethodHandler;
import org.jboss.webbeans.bootstrap.BeanDeployerEnvironment;
import org.jboss.webbeans.injection.FieldInjectionPoint;
import org.jboss.webbeans.injection.MethodInjectionPoint;
import org.jboss.webbeans.injection.ParameterInjectionPoint;
import org.jboss.webbeans.introspector.WBClass;
import org.jboss.webbeans.introspector.WBMethod;
import org.jboss.webbeans.introspector.WBParameter;
import org.jboss.webbeans.log.LogProvider;
import org.jboss.webbeans.log.Logging;
import org.jboss.webbeans.util.Beans;
import org.jboss.webbeans.util.Proxies;
import org.jboss.webbeans.util.Strings;

/**
 * An abstract bean representation common for class-based beans
 * 
 * @author Pete Muir
 * 
 * @param <T>
 * @param <E>
 */
public abstract class AbstractClassBean<T> extends AbstractBean<T, Class<T>>
{
   // Logger
   private static final LogProvider log = Logging.getLogProvider(AbstractClassBean.class);
   // The item representation
   protected WBClass<T> annotatedItem;
   // The injectable fields
   private Set<FieldInjectionPoint<?>> injectableFields;
   // The initializer methods
   private Set<MethodInjectionPoint<?>> initializerMethods;
   private Set<String> dependencies;
   
   private List<Decorator<?>> decorators;
   
   private final String id;
   private Class<T> proxyClassForDecorators;
   
   private final ThreadLocal<Integer> decoratorStackPosition;

   /**
    * Constructor
    * 
    * @param type The type
    * @param manager The Web Beans manager
    */
   protected AbstractClassBean(WBClass<T> type, BeanManagerImpl manager)
   {
      super(manager);
      this.annotatedItem = type;
      this.id = createId(getClass().getSimpleName() + "-" + type.getName());
      this.decoratorStackPosition = new ThreadLocal<Integer>()
      {
         
         @Override
         protected Integer initialValue()
         {
            return 0;
         }
         
      };
   }

   /**
    * Initializes the bean and its metadata
    */
   @Override
   public void initialize(BeanDeployerEnvironment environment)
   {
      initInitializerMethods();
      super.initialize(environment);
      checkBeanImplementation();
      initDecorators();
      checkType();
      initProxyClassForDecoratedBean();
   }
   
   protected void checkType()
   {
      
   }
   
   protected void initDecorators()
   {
      this.decorators = getManager().resolveDecorators(getTypes(), getBindings());
   }
   
   public boolean hasDecorators()
   {
      return this.decorators != null && this.decorators.size() > 0;
   }
   
   protected void initProxyClassForDecoratedBean()
   {
      if (hasDecorators())
      {
         Set<Type> types = new LinkedHashSet<Type>(getTypes());
         ProxyFactory proxyFactory = Proxies.getProxyFactory(types);
   
         @SuppressWarnings("unchecked")
         Class<T> proxyClass = proxyFactory.createClass();
   
         this.proxyClassForDecorators = proxyClass;
      }
   }
   
   protected T applyDecorators(T instance, CreationalContext<T> creationalContext)
   {
      if (hasDecorators())
      {
         List<SerializableBeanInstance<DecoratorBean<Object>, Object>> decoratorInstances = new ArrayList<SerializableBeanInstance<DecoratorBean<Object>,Object>>();
         boolean outside = decoratorStackPosition.get().intValue() == 0;
         try
         {
            int i = decoratorStackPosition.get();
            while (i < decorators.size())
            {
               Decorator<?> decorator = decorators.get(i);
               if (decorator instanceof DecoratorBean)
               {
                  decoratorStackPosition.set(++i);
                  decoratorInstances.add(new SerializableBeanInstance<DecoratorBean<Object>, Object>((DecoratorBean) decorator, getManager().getReference(decorator, Object.class, creationalContext)));
               }
               else
               {
                  throw new IllegalStateException("Cannot operate on non container provided decorator " + decorator);
               }
            }
         }
         finally
         {
            if (outside)
            {
               decoratorStackPosition.remove();
            }
         }
         try
         {
            T proxy = proxyClassForDecorators.newInstance();
            ((ProxyObject) proxy).setHandler(new DecoratorProxyMethodHandler(decoratorInstances, instance));
            return proxy;
         }
         catch (InstantiationException e)
         {
            throw new RuntimeException("Could not instantiate decorator proxy for " + toString(), e);
         }
         catch (IllegalAccessException e)
         {
            throw new RuntimeException("Could not access bean correctly when creating decorator proxy for " + toString(), e);
         }
      }
      else
      {
         return instance;
      }
   }
   
   public List<Decorator<?>> getDecorators()
   {
      return Collections.unmodifiableList(decorators);
   }

   /**
    * Injects bound fields
    * 
    * @param instance The instance to inject into
    */
   protected void injectBoundFields(T instance, CreationalContext<T> creationalContext)
   {
      for (FieldInjectionPoint<?> injectableField : injectableFields)
      {
         injectableField.inject(instance, manager, creationalContext);
      }
   }
   
   /**
    * Calls all initializers of the bean
    * 
    * @param instance The bean instance
    */
   protected void callInitializers(T instance, CreationalContext<T> creationalContext)
   {
      for (MethodInjectionPoint<?> initializer : getInitializerMethods())
      {
         initializer.invoke(instance, manager, creationalContext, CreationException.class);
      }
   }

   /**
    * Initializes the bean type
    */
   protected void initType()
   {
      log.trace("Bean type specified in Java");
      this.type = getAnnotatedItem().getJavaClass();
      this.dependencies = new HashSet<String>();
      for (Class<?> clazz = type.getSuperclass(); clazz != Object.class; clazz = clazz.getSuperclass())
      {
         dependencies.add(clazz.getName());
      }
   }

   /**
    * Initializes the injection points
    */
   protected void initInjectionPoints()
   {
      injectableFields = new HashSet<FieldInjectionPoint<?>>(Beans.getFieldInjectionPoints(annotatedItem, this));
      super.injectionPoints.addAll(injectableFields);
      for (WBMethod<?> initializer : getInitializerMethods())
      {
         for (WBParameter<?> parameter : initializer.getParameters())
         {
            injectionPoints.add(ParameterInjectionPoint.of(this, parameter));
         }
      }
   }

   /**
    * Initializes the initializer methods
    */
   protected void initInitializerMethods()
   {
      initializerMethods = new HashSet<MethodInjectionPoint<?>>();
      for (WBMethod<?> method : annotatedItem.getAnnotatedMethods(Initializer.class))
      {
         if (method.isStatic())
         {
            throw new DefinitionException("Initializer method " + method.toString() + " cannot be static on " + getAnnotatedItem());
         }
         else if (method.getAnnotation(Produces.class) != null)
         {
            throw new DefinitionException("Initializer method " + method.toString() + " cannot be annotated @Produces on " + getAnnotatedItem());
         }
         else if (method.getAnnotatedParameters(Disposes.class).size() > 0)
         {
            throw new DefinitionException("Initializer method " + method.toString() + " cannot have parameters annotated @Disposes on " + getAnnotatedItem());
         }
         else if (method.getAnnotatedParameters(Observes.class).size() > 0)
         {
            throw new DefinitionException("Initializer method " + method.toString() + " cannot be annotated @Observes on " + getAnnotatedItem());
         }
         else
         {
            initializerMethods.add(MethodInjectionPoint.of(this, method));
         }
      }
   }

   @Override
   protected void initScopeType()
   {
      for (WBClass<?> clazz = getAnnotatedItem(); clazz != null; clazz = clazz.getSuperclass())
      {
         Set<Annotation> scopeTypes = clazz.getDeclaredMetaAnnotations(ScopeType.class);
         scopeTypes = clazz.getDeclaredMetaAnnotations(ScopeType.class);
         if (scopeTypes.size() == 1)
         {
            if (getAnnotatedItem().isAnnotationPresent(scopeTypes.iterator().next().annotationType()))
            {
               this.scopeType = scopeTypes.iterator().next().annotationType();
               log.trace("Scope " + scopeType + " specified by annotation");
            }
            break;
         }
         else if (scopeTypes.size() > 1)
         {
            throw new DefinitionException("At most one scope may be specified on " + getAnnotatedItem());
         }
      }

      if (this.scopeType == null)
      {
         initScopeTypeFromStereotype();
      }

      if (this.scopeType == null)
      {
         this.scopeType = Dependent.class;
         log.trace("Using default @Dependent scope");
      }
   }

   @Override
   protected void initDeploymentType()
   {
      for (WBClass<?> clazz = getAnnotatedItem(); clazz != null; clazz = clazz.getSuperclass())
      {
         Set<Annotation> deploymentTypes = clazz.getDeclaredMetaAnnotations(DeploymentType.class);
         if (deploymentTypes.size() == 1)
         {
            if (getAnnotatedItem().isAnnotationPresent(deploymentTypes.iterator().next().annotationType()))
            {
               this.deploymentType = deploymentTypes.iterator().next().annotationType();
               log.trace("Deployment type " + deploymentType + " specified by annotation");
            }
            break;
         }
         else if (deploymentTypes.size() > 1)
         {
            throw new DefinitionException("At most one deployment type may be specified");
         }
      }

      if (this.deploymentType == null)
      {
         initDeploymentTypeFromStereotype();
      }

      if (this.deploymentType == null)
      {
         this.deploymentType = getDefaultDeploymentType();
         log.trace("Using default @Production deployment type");
         return;
      }
   }

   /**
    * Validates the bean implementation
    */
   protected void checkBeanImplementation() {}

   @Override
   protected void preSpecialize(BeanDeployerEnvironment environment)
   {
      super.preSpecialize(environment);
      if (getAnnotatedItem().getSuperclass() == null || getAnnotatedItem().getSuperclass().getJavaClass().equals(Object.class))
      {
         throw new DefinitionException("Specializing bean must extend another bean " + toString());
      }
   }

   /**
    * Gets the annotated item
    * 
    * @return The annotated item
    */
   @Override
   public WBClass<T> getAnnotatedItem()
   {
      return annotatedItem;
   }

   /**
    * Gets the default name
    * 
    * @return The default name
    */
   @Override
   protected String getDefaultName()
   {
      String name = Strings.decapitalize(getAnnotatedItem().getSimpleName());
      log.trace("Default name of " + type + " is " + name);
      return name;
   }

   /**
    * Gets the annotated methods
    * 
    * @return The set of annotated methods
    */
   public Set<? extends MethodInjectionPoint<?>> getInitializerMethods()
   {
      return initializerMethods;
   }
   
   // TODO maybe a better way to expose this?
   public Set<String> getSuperclasses()
   {
      return dependencies;
   }

   /**
    * Gets a string representation
    * 
    * @return The string representation
    */
   @Override
   public String toString()
   {
      return "AbstractClassBean " + getName();
   }

   @Override
   /*
    * Gets the default deployment type
    * 
    * @return The default deployment type
    */
   protected Class<? extends Annotation> getDefaultDeploymentType()
   {
      return Production.class;
   }
   
   @Override
   public String getId()
   {
      return id;
   }

}
