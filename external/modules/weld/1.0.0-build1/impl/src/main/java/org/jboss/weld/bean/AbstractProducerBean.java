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
package org.jboss.weld.bean;

import static org.jboss.weld.messages.BeanMessages.USING_DEFAULT_SCOPE;
import static org.jboss.weld.messages.BeanMessages.USING_SCOPE;
import static org.jboss.weld.util.log.Categories.BEAN;
import static org.jboss.weld.util.log.LoggerFactory.loggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.inject.Inject;
import javax.inject.Scope;

import org.jboss.weld.BeanManagerImpl;
import org.jboss.weld.DefinitionException;
import org.jboss.weld.bootstrap.BeanDeployerEnvironment;
import org.jboss.weld.introspector.WeldMember;
import org.jboss.weld.metadata.cache.MetaAnnotationStore;
import org.jboss.weld.util.Beans;
import org.jboss.weld.util.Names;
import org.jboss.weld.util.Reflections;
import org.slf4j.cal10n.LocLogger;

/**
 * The implicit producer bean
 * 
 * @author Gavin King
 * 
 * @param <T>
 * @param <S>
 */
public abstract class AbstractProducerBean<X, T, S extends Member> extends AbstractReceiverBean<X, T, S>
{
   private static final LocLogger log = loggerFactory().getLogger(BEAN);
   
   private Producer<T> producer;

   /**
    * Constructor
    * @param declaringBean The declaring bean
    * @param manager The Bean manager
    */
   public AbstractProducerBean(String idSuffix, AbstractClassBean<X> declaringBean, BeanManagerImpl manager)
   {
      super(idSuffix, declaringBean, manager);
   }

   @Override
   public abstract WeldMember<T, X, S> getAnnotatedItem();

   @Override
   // Overriden to provide the class of the bean that declares the producer method/field
   public Class<?> getBeanClass()
   {
      return getDeclaringBean().getBeanClass();
   }

   /**
    * Initializes the API types
    */
   @Override
   protected void initTypes()
   {
      if (getType().isArray() || getType().isPrimitive())
      {
         Set<Type> types = new HashSet<Type>();
         types.add(getType());
         types.add(Object.class);
         super.types = types;
      }
      else
      {
         super.initTypes();
      }
   }

   /**
    * Initializes the type
    */
   protected void initType()
   {
      try
      {
         this.type = getAnnotatedItem().getJavaClass();
      }
      catch (ClassCastException e)
      {
         Type type = Beans.getDeclaredBeanType(getClass());
         throw new RuntimeException(" Cannot cast producer type " + getAnnotatedItem().getJavaClass() + " to bean type " + (type == null ? " unknown " : type), e);
      }
   }

   /**
    * Validates the producer method
    */
   protected void checkProducerReturnType()
   {
      if (getAnnotatedItem().getBaseType() instanceof TypeVariable<?>)
      {
         throw new DefinitionException("Return type must be concrete " + getAnnotatedItem().getBaseType());
      }
      if (getAnnotatedItem().getBaseType() instanceof WildcardType)
      {
         throw new DefinitionException("Return type must be concrete " + getAnnotatedItem().getBaseType());
      }
      for (Type type : getAnnotatedItem().getActualTypeArguments())
      {
         if (!(type instanceof Class))
         {
            throw new DefinitionException("Producer type cannot be parameterized with type parameter or wildcard:\n" + this.getAnnotatedItem());
         }
      }
   }

   /**
    * Initializes the bean and its metadata
    */
   @Override
   public void initialize(BeanDeployerEnvironment environment)
   {
      getDeclaringBean().initialize(environment);
      super.initialize(environment);
      checkProducerReturnType();
   }
   
   @Override
   public Set<InjectionPoint> getInjectionPoints()
   {
      return getProducer().getInjectionPoints();
   }

   /**
    * Validates the return value
    * 
    * @param instance The instance to validate
    */
   protected void checkReturnValue(T instance)
   {
      if (instance == null && !isDependent())
      {
         throw new IllegalProductException("Cannot return null from a non-dependent producer method");
      }
      else if (instance != null)
      {
         boolean passivating = manager.getServices().get(MetaAnnotationStore.class).getScopeModel(getScope()).isPassivating();
         if (passivating && !Reflections.isSerializable(instance.getClass()))
         {
            throw new IllegalProductException("Producers cannot declare passivating scope and return a non-serializable class");
         }
         InjectionPoint injectionPoint = manager.getCurrentInjectionPoint();
         if (injectionPoint == null)
         {
            return;
         }
         if (!Reflections.isSerializable(instance.getClass()) && Beans.isPassivationCapableBean(injectionPoint.getBean()))
         {
            if (injectionPoint.getMember() instanceof Field)
            {
               if (!Reflections.isTransient(injectionPoint.getMember()) && instance != null && !Reflections.isSerializable(instance.getClass()))
               {
                  throw new IllegalProductException("Producers cannot produce non-serializable instances for injection into non-transient fields of passivating beans\n\nProducer: " + this.toString() + "\nInjection Point: " + injectionPoint.toString());
               }
            }
            else if (injectionPoint.getMember() instanceof Method)
            {
               Method method = (Method) injectionPoint.getMember();
               if (method.isAnnotationPresent(Inject.class))
               {
                  throw new IllegalProductException("Producers cannot produce non-serializable instances for injection into parameters of intializers of beans declaring passivating scope. Bean " + toString() + " being injected into " + injectionPoint.toString());
               }
               if (method.isAnnotationPresent(Produces.class))
               {
                  throw new IllegalProductException("Producers cannot produce non-serializable instances for injection into parameters of producer methods declaring passivating scope. Bean " + toString() + " being injected into " + injectionPoint.toString());
               }
            }
            else if (injectionPoint.getMember() instanceof Constructor)
            {
               throw new IllegalProductException("Producers cannot produce non-serializable instances for injection into parameters of constructors of beans declaring passivating scope. Bean " + toString() + " being injected into " + injectionPoint.toString());
            }
         }
      }
   }

   @Override
   protected void initScopeType()
   {
      Set<Annotation> scopeAnnotations = new HashSet<Annotation>();
      scopeAnnotations.addAll(getAnnotatedItem().getMetaAnnotations(Scope.class));
      scopeAnnotations.addAll(getAnnotatedItem().getMetaAnnotations(NormalScope.class));
      if (scopeAnnotations.size() > 1)
      {
         throw new DefinitionException("At most one scope may be specified");
      }
      if (scopeAnnotations.size() == 1)
      {
         this.scopeType = scopeAnnotations.iterator().next().annotationType();
         log.trace(USING_SCOPE, scopeType, this);
         return;
      }

      initScopeTypeFromStereotype();

      if (this.scopeType == null)
      {
         this.scopeType = Dependent.class;
         log.trace(USING_DEFAULT_SCOPE, this);
      }
   }
   
   @Override
   protected void initSerializable()
   {
      // No-op
   }

   @Override
   public boolean isSerializable()
   {
      return true;
   }
   
   /**
    * This operation is *not* threadsafe, and should not be called outside bootstrap
    * 
    * @param producer
    */
   public void setProducer(Producer<T> producer)
   {
      this.producer = producer;
   }
   
   public Producer<T> getProducer()
   {
      return producer;
   }

   /**
    * Creates an instance of the bean
    * 
    * @returns The instance
    */
   public T create(final CreationalContext<T> creationalContext)
   {
      try
      {
         T instance = getProducer().produce(creationalContext);
         checkReturnValue(instance);
         return instance;
      }
      finally
      {
         if (getDeclaringBean().isDependent())
         {
            creationalContext.release();
         }
      }
   }

   /**
    * Gets a string representation
    * 
    * @return The string representation
    */
   @Override
   public String getDescription()
   {
      StringBuilder buffer = new StringBuilder();
      buffer.append("Annotated " + Names.scopeTypeToString(getScope()));
      if (getName() == null)
      {
         buffer.append("unnamed producer bean");
      }
      else
      {
         buffer.append("simple producer bean '" + getName() + "'");
      }
      buffer.append(" [" + getBeanClass().getName() + "] for class type [" + getType().getName() + "] API types " + getTypes() + ", binding types " + getQualifiers());
      return buffer.toString();
   }

}