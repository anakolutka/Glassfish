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
package org.jboss.webbeans.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.jboss.webbeans.BeanManagerImpl;

/**
 * An EL-resolver against the named beans
 *  
 * @author Pete Muir
 */
public class WebBeansELResolverImpl extends ELResolver
{
   
   private static final class ValueHolder<T>
   {
      
      private T value;
      
      public T getValue()
      {
         return value;
      }
      
      public void setValue(T value)
      {
         this.value = value;
      }
      
   }
   
   private final BeanManagerImpl manager;
      
   public WebBeansELResolverImpl(BeanManagerImpl manager)
   {
      this.manager = manager;
   }

   /**
    * @see javax.el.ELResolver#getCommonPropertyType(ELContext, Object)
    */
   @Override
   public Class<?> getCommonPropertyType(ELContext context, Object base)
   {
      return null;
   }

   /**
    * @see javax.el.ELResolver#getFeatureDescriptors(ELContext, Object)
    */
   @Override
   public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base)
   {
      return null;
   }

   /**
    * @see javax.el.ELResolver#getType(ELContext, Object, Object)
    */
   @Override
   public Class<?> getType(ELContext context, Object base, Object property)
   {
      return null;
   }

   /**
    * @see javax.el.ELResolver#getValue(ELContext, Object, Object)
    */
   @Override
   public Object getValue(ELContext context, Object base, Object property)
   {
      if (property != null)
      {
         String propertyString = property.toString();
         Namespace namespace = null;
         if (base == null) 
         {
            if (manager.getRootNamespace().contains(propertyString))
            {
               context.setPropertyResolved(true);
               return manager.getRootNamespace().get(propertyString);
            }
         }
         else if (base instanceof Namespace)
         {
            namespace = (Namespace) base;
            // We're definitely the responsible party
            context.setPropertyResolved(true);
            if (namespace.contains(propertyString))
            {
               // There is a child namespace
               return namespace.get(propertyString);
            }
         }
         else
         {
            // let the standard EL resolver chain handle the property
            return null;
         }
         final String name;
         if (namespace != null)
         {
            // Try looking in the manager for a bean
            name = namespace.qualifyName(propertyString);
         }
         else
         {
            name = propertyString;
         }
         Object value = null;
         try
         {
            
            Bean<?> bean = manager.resolve(manager.getBeans(name));
            CreationalContext<?> creationalContext = manager.createCreationalContext(bean);
            if (bean != null)
            {
               value = manager.getReference(bean, creationalContext);
            }
            creationalContext.release();
         }
         catch (Exception e)
         {
            throw new RuntimeException("Error resolving property " + propertyString + " against base " + base, e);
         }
         if (value != null)
         {
            context.setPropertyResolved(true);
            return value;
         }
      }
      return null;
   }

   /**
    * @see javax.el.ELResolver#isReadOnly(ELContext, Object, Object)
    */
   @Override
   public boolean isReadOnly(ELContext context, Object base, Object property)
   {
      return false;
   }

   /**
    * @see javax.el.ELResolver#setValue(ELContext, Object, Object, Object)
    */
   @Override
   public void setValue(ELContext context, Object base, Object property, Object value)
   {
   }

}

