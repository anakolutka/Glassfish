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
package org.jboss.webbeans.context;

import javax.context.Contextual;

/**
 * A representation of a contextual bean plus associated instance
 * 
 * @author Pete Muir
 */
public class ContextualInstance<T>
{
   // The contextual object
   private Contextual<T> contextual;
   // The instance
   private T instance;

   /**
    * Protected constructor
    * 
    * @param contextual The contextual item
    * @param instance The instance
    */
   protected ContextualInstance(Contextual<T> contextual, T instance)
   {
      this.contextual = contextual;
      this.instance = instance;
   }

   /**
    * Static constructor wrapper
    * 
    * @param <T> The type of the contextual item
    * @param contextual The contextual item
    * @param instance The instance
    * @return A new ContextualInstance from the given parameters
    */
   public static <T> ContextualInstance<T> of(Contextual<T> contextual, T instance)
   {
      return new ContextualInstance<T>(contextual, instance);
   }

   /**
    * Destroys the instance by passing it to the destroy method of the
    * contextual item
    */
   public void destroy()
   {
      contextual.destroy(instance);
   }

}
