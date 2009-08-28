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

package javax.enterprise.inject.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.Contextual;

public interface Bean<T> extends Contextual<T>
{

   /**
    * The client-visible types of a bean
    * 
    * @return the bean types
    */
   public Set<Type> getTypes();

   /**
    * The bindings of a bean
    * 
    * @return the bindings
    */
   public Set<Annotation> getBindings();

   /**
    * The scope of a bean
    * 
    * @return the scope
    */
   public Class<? extends Annotation> getScopeType();

   /**
    * The deployment type of a bean
    * 
    * @return the deployment type
    */
   public Class<? extends Annotation> getDeploymentType();

   /**
    * The name of a bean
    * 
    * @return the name
    */
   public String getName();

   /**
    * The serializability of a bean
    * 
    * @return true if the bean is serializable
    */
   public boolean isSerializable();

   /**
    * The nullability of a bean
    * 
    * @return true if the bean is nullable
    */
   public boolean isNullable();

   /**
    * The injection points of a bean
    * 
    * @return the injection points of a bean
    */
   public Set<InjectionPoint> getInjectionPoints();

}