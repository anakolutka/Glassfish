/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.util;

import static org.jboss.weld.logging.messages.UtilMessage.EVENT_TYPE_NOT_ALLOWED;
import static org.jboss.weld.logging.messages.UtilMessage.TYPE_PARAMETER_NOT_ALLOWED_IN_EVENT_TYPE;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.jboss.weld.exceptions.ForbiddenArgumentException;
import org.jboss.weld.util.reflection.HierarchyDiscovery;

/**
 * @author pmuir
 *
 */
public class Observers
{
   
   public static void checkEventObjectType(Type eventType)
   {
      Type[] types;
      Type resolvedType = new HierarchyDiscovery(eventType).getResolvedType();
      if (resolvedType instanceof Class<?>)
      {
         types = new Type[0];
      }
      else if (resolvedType instanceof ParameterizedType)
      {
         types = ((ParameterizedType) resolvedType).getActualTypeArguments();
      }
      else
      {
         throw new ForbiddenArgumentException(EVENT_TYPE_NOT_ALLOWED, resolvedType);
      }
      for (Type type : types)
      {
         if (type instanceof TypeVariable<?>)
         {
            throw new ForbiddenArgumentException(TYPE_PARAMETER_NOT_ALLOWED_IN_EVENT_TYPE, resolvedType);
         }
      }
   }
   
   public static void checkEventObjectType(Object event)
   {
      checkEventObjectType(event.getClass());
      
   }
}
