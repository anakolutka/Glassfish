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

import static org.jboss.weld.logging.Category.BEAN;
import static org.jboss.weld.logging.LoggerFactory.loggerFactory;
import static org.jboss.weld.logging.messages.BeanMessage.CREATING_BEAN;
import static org.jboss.weld.logging.messages.BeanMessage.QUALIFIERS_USED;
import static org.jboss.weld.logging.messages.BeanMessage.USING_DEFAULT_NAME;
import static org.jboss.weld.logging.messages.BeanMessage.USING_DEFAULT_QUALIFIER;
import static org.jboss.weld.logging.messages.BeanMessage.USING_NAME;
import static org.jboss.weld.logging.messages.BeanMessage.USING_SCOPE_FROM_STEREOTYPE;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.decorator.Delegate;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Named;
import javax.inject.Qualifier;

import org.jboss.weld.BeanManagerImpl;
import org.jboss.weld.Container;
import org.jboss.weld.DefinitionException;
import org.jboss.weld.bootstrap.BeanDeployerEnvironment;
import org.jboss.weld.injection.WeldInjectionPoint;
import org.jboss.weld.introspector.WeldAnnotated;
import org.jboss.weld.introspector.WeldField;
import org.jboss.weld.introspector.WeldParameter;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.literal.DefaultLiteral;
import org.jboss.weld.metadata.cache.MergedStereotypes;
import org.jboss.weld.metadata.cache.MetaAnnotationStore;
import org.jboss.weld.util.Reflections;
import org.slf4j.cal10n.LocLogger;

/**
 * An abstract bean representation common for all beans
 * 
 * @author Pete Muir
 * 
 * @param <T> the type of bean
 * @param <S> the Class<?> of the bean type
 */
public abstract class AbstractBean<T, S> extends RIBean<T>
{

   private static final Annotation ANY_LITERAL = new AnyLiteral();
   private static final Annotation CURRENT_LITERAL = new DefaultLiteral();

   private boolean proxyable;

   // Logger
   private static final LocLogger log = loggerFactory().getLogger(BEAN);
   // The binding types
   protected Set<Annotation> bindings;
   // The name
   protected String name;
   // The scope type
   protected Class<? extends Annotation> scopeType;
   // The merged stereotypes
   private MergedStereotypes<T, S> mergedStereotypes;
   // Is it a policy, either defined by stereotypes or directly?
   private boolean policy;
   // The type
   protected Class<T> type;
   // The API types
   protected Set<Type> types;
   // The injection points
   private Set<WeldInjectionPoint<?, ?>> injectionPoints;
   private Set<WeldInjectionPoint<?, ?>> delegateInjectionPoints;
   
   // The @New injection points
   private Set<WeldInjectionPoint<?, ?>> newInjectionPoints;
   
   // If the type a primitive?
   private boolean primitive;
   // The Bean manager
   protected BeanManagerImpl manager;

   private boolean _serializable;

   private boolean initialized;

   

   protected boolean isInitialized()
   {
      return initialized;
   }

   /**
    * Constructor
    * 
    * @param manager The Bean manager
    */
   public AbstractBean(String idSuffix, BeanManagerImpl manager)
   {
      super(idSuffix, manager);
      this.manager = manager;
      this.injectionPoints = new HashSet<WeldInjectionPoint<?, ?>>();
      this.delegateInjectionPoints = new HashSet<WeldInjectionPoint<?,?>>();
      this.newInjectionPoints = new HashSet<WeldInjectionPoint<?,?>>();
   }

   /**
    * Initializes the bean and its metadata
    */
   @Override
   public void initialize(BeanDeployerEnvironment environment)
   {
      if (isSpecializing())
      {
         preSpecialize(environment);
         specialize(environment);
         postSpecialize();
      }
      initDefaultBindings();
      initPrimitive();
      log.trace(CREATING_BEAN, getType());
      initName();
      initScopeType();
      initSerializable();
      initProxyable();
      checkDelegateInjectionPoints();
   }
   
   protected void initStereotypes()
   {
      mergedStereotypes = new MergedStereotypes<T, S>(getAnnotatedItem().getMetaAnnotations(Stereotype.class), manager);
   }

   protected void checkDelegateInjectionPoints()
   {
      if (this.delegateInjectionPoints.size() > 0)
      {
         throw new DefinitionException("Cannot place @Decorates at an injection point which is not on a Decorator " + this);
      }
   }
   
   protected void addInjectionPoint(WeldInjectionPoint<?, ?> injectionPoint)
   {
      if (injectionPoint.isAnnotationPresent(Delegate.class))
      {
         this.delegateInjectionPoints.add(injectionPoint);
      }
      if (injectionPoint.isAnnotationPresent(New.class))
      {
         this.newInjectionPoints.add(injectionPoint);
      }
      injectionPoints.add(injectionPoint);
   }
   
   protected void addInjectionPoints(Iterable<? extends WeldInjectionPoint<?, ?>> injectionPoints)
   {
      for (WeldInjectionPoint<?, ?> injectionPoint : injectionPoints)
      {
         addInjectionPoint(injectionPoint);
      }
   }

   protected Set<WeldInjectionPoint<?, ?>> getDelegateInjectionPoints()
   {
      return delegateInjectionPoints;
   }

   /**
    * Initializes the API types
    */
   protected void initTypes()
   {
      if (getAnnotatedItem().isAnnotationPresent(Typed.class))
      {
         this.types = getTypedTypes(getAnnotatedItem().getTypeClosureAsMap(), getAnnotatedItem().getJavaClass(), getAnnotatedItem().getAnnotation(Typed.class));
      }
      else
      {
         this.types = getAnnotatedItem().getTypeClosure();
         if (getType().isInterface())
         {
            this.types.add(Object.class);
         }
      }
   }
   
   protected static Set<Type> getTypedTypes(Map<Class<?>, Type> typeClosure, Class<?> rawType, Typed typed)
   {
      Set<Type> types = new HashSet<Type>();
      for (Class<?> specifiedClass : typed.value())
      {
         if (!typeClosure.containsKey(specifiedClass))
         {
            throw new DefinitionException("@Typed class " + specifiedClass.getName() + " is not present in the type hierarchy " + rawType);
         }
         else
         {
            types.add(typeClosure.get(specifiedClass));
         }
      }
      return types;
   }

   /**
    * Initializes the binding types
    */
   protected void initBindings()
   {
      this.bindings = new HashSet<Annotation>();
      this.bindings.addAll(getAnnotatedItem().getMetaAnnotations(Qualifier.class));
      initDefaultBindings();
      log.trace(QUALIFIERS_USED, bindings, this);
   }

   protected void initDefaultBindings()
   {
      if (bindings.size() == 0)
      {
         log.trace(USING_DEFAULT_QUALIFIER, this);
         this.bindings.add(CURRENT_LITERAL);
      }
      if (bindings.size() == 1)
      {
         if (bindings.iterator().next().annotationType().equals(Named.class))
         {
            log.trace(USING_DEFAULT_QUALIFIER, this);
            this.bindings.add(CURRENT_LITERAL);
         }
      }
      this.bindings.add(ANY_LITERAL);
   }

   protected void initPolicy()
   {
      if (getAnnotatedItem().isAnnotationPresent(Alternative.class))
      {
         this.policy = true;
      }
      else
      {
         this.policy = getMergedStereotypes().isPolicy();
      }
   }

   /**
    * Initializes the name
    */
   protected void initName()
   {
      boolean beanNameDefaulted = false;
      if (getAnnotatedItem().isAnnotationPresent(Named.class))
      {
         String javaName = getAnnotatedItem().getAnnotation(Named.class).value();
         if ("".equals(javaName))
         {
            beanNameDefaulted = true;
         }
         else
         {
            log.trace(USING_NAME, javaName, this);
            this.name = javaName;
            return;
         }
      }

      if (beanNameDefaulted || getMergedStereotypes().isBeanNameDefaulted())
      {
         this.name = getDefaultName();
         log.trace(USING_DEFAULT_NAME, name, this);
         return;
      }
   }

   protected void initProxyable()
   {
      proxyable = getAnnotatedItem().isProxyable();
   }

   /**
    * Initializes the primitive flag
    */
   protected void initPrimitive()
   {
      this.primitive = Reflections.isPrimitive(getType());
   }

   private boolean checkInjectionPointsAreSerializable()
   {
      boolean passivating = manager.getServices().get(MetaAnnotationStore.class).getScopeModel(this.getScope()).isPassivating();
      for (WeldInjectionPoint<?, ?> injectionPoint : getAnnotatedInjectionPoints())
      {
         Annotation[] bindings = injectionPoint.getMetaAnnotationsAsArray(Qualifier.class);
         Bean<?> resolvedBean = manager.getBeans(injectionPoint.getJavaClass(), bindings).iterator().next();
         if (passivating)
         {
            if (Dependent.class.equals(resolvedBean.getScope()) && !Reflections.isSerializable(resolvedBean.getBeanClass()) && (((injectionPoint instanceof WeldField<?, ?>) && !((WeldField<?, ?>) injectionPoint).isTransient()) || (injectionPoint instanceof WeldParameter<?, ?>)))
            {
               return false;
            }
         }
      }
      return true;
   }

   /**
    * Initializes the scope type
    */
   protected abstract void initScopeType();

   protected boolean initScopeTypeFromStereotype()
   {
      Set<Annotation> possibleScopeTypes = getMergedStereotypes().getPossibleScopeTypes();
      if (possibleScopeTypes.size() == 1)
      {
         this.scopeType = possibleScopeTypes.iterator().next().annotationType();
         log.trace(USING_SCOPE_FROM_STEREOTYPE, scopeType, this, getMergedStereotypes());
         return true;
      }
      else if (possibleScopeTypes.size() > 1)
      {
         throw new DefinitionException("All stereotypes must specify the same scope OR a scope must be specified on " + getAnnotatedItem());
      }
      else
      {
         return false;
      }
   }

   protected void postSpecialize()
   {
      if (getAnnotatedItem().isAnnotationPresent(Named.class) && getSpecializedBean().getAnnotatedItem().isAnnotationPresent(Named.class))
      {
         throw new DefinitionException("Cannot put name on specializing and specialized class " + getAnnotatedItem());
      }
      this.bindings.addAll(getSpecializedBean().getQualifiers());
      if (isSpecializing() && getSpecializedBean().getAnnotatedItem().isAnnotationPresent(Named.class))
      {
         this.name = getSpecializedBean().getName();
      }
      manager.getSpecializedBeans().put(getSpecializedBean(), this);
   }

   protected void preSpecialize(BeanDeployerEnvironment environment)
   {

   }

   protected void specialize(BeanDeployerEnvironment environment)
   {

   }

   /**
    * Returns the annotated item the bean represents
    *
    * @return The annotated item
    */
   public abstract WeldAnnotated<T, S> getAnnotatedItem();

   /**
    * Gets the binding types
    * 
    * @return The set of binding types
    * 
    * @see org.jboss.weld.bean.BaseBean#getQualifiers()
    */
   public Set<Annotation> getQualifiers()
   {
      return bindings;
   }

   /**
    * Gets the default name of the bean
    * 
    * @return The default name
    */
   protected abstract String getDefaultName();

   @Override
   public abstract AbstractBean<?, ?> getSpecializedBean();

   @Override
   public Set<WeldInjectionPoint<?, ?>> getAnnotatedInjectionPoints()
   {
      return injectionPoints;
   }
   
   public Set<WeldInjectionPoint<?, ?>> getNewInjectionPoints()
   {
      return newInjectionPoints;
   }

   /**
    * Gets the merged stereotypes of the bean
    * 
    * @return The set of merged stereotypes
    */
   protected MergedStereotypes<T, S> getMergedStereotypes()
   {
      return mergedStereotypes;
   }

   /**
    * Gets the name of the bean
    * 
    * @return The name
    * 
    * @see org.jboss.weld.bean.BaseBean#getName()
    */
   public String getName()
   {
      return name;
   }

   /**
    * Gets the scope type of the bean
    * 
    * @return The scope type
    * 
    * @see org.jboss.weld.bean.BaseBean#getScope()
    */
   public Class<? extends Annotation> getScope()
   {
      return scopeType;
   }

   /**
    * Gets the type of the bean
    * 
    * @return The type
    */
   @Override
   public Class<T> getType()
   {
      return type;
   }

   /**
    * Gets the API types of the bean
    * 
    * @return The set of API types
    * 
    * @see org.jboss.weld.bean.BaseBean#getTypeClosure()
    */
   public Set<Type> getTypes()
   {
      return types;
   }

   /**
    * Indicates if bean is nullable
    * 
    * @return True if nullable, false otherwise
    * 
    * @see org.jboss.weld.bean.BaseBean#isNullable()
    */
   public boolean isNullable()
   {
      return !isPrimitive();
   }

   /**
    * Indicates if bean type is a primitive
    * 
    * @return True if primitive, false otherwise
    */
   @Override
   public boolean isPrimitive()
   {
      return primitive;
   }

   public boolean isSerializable()
   {
      // TODO WTF - why are we not caching the serializability of injection
      // points!
      return _serializable && checkInjectionPointsAreSerializable();
   }

   protected void initSerializable()
   {
      _serializable = Reflections.isSerializable(type);
   }

   @Override
   public boolean isProxyable()
   {
      return proxyable;
   }

   @Override
   public boolean isDependent()
   {
      return Dependent.class.equals(getScope());
   }
   
   public boolean isNormalScoped()
   {
      return Container.instance().deploymentServices().get(MetaAnnotationStore.class).getScopeModel(getScope()).isNormal();
   }
   
   public boolean isAlternative()
   {
      return policy;
   }

   @Override
   public boolean isSpecializing()
   {
      return getAnnotatedItem().isAnnotationPresent(Specializes.class);
   }

   public Set<Class<? extends Annotation>> getStereotypes()
   {
      return mergedStereotypes.getStereotypes();
   }

}
