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
package org.jboss.webbeans.introspector;

/**
 * AnnotatedType provides a uniform access to a type defined either in Java or
 * XML
 * 
 * @author Pete Muir
 * @param <T>
 */
public interface WBType<T> extends WBAnnotated<T, Class<T>>
{

   /**
    * Gets the superclass of the type
    * 
    * @return The abstracted superclass
    */
   public WBType<?> getSuperclass();

   /**
    * Check if this is equivalent to a java class
    * @param clazz The Java class
    * @return true if equivalent
    */
   public boolean isEquivalent(Class<?> clazz);

   public String getSimpleName();

}
