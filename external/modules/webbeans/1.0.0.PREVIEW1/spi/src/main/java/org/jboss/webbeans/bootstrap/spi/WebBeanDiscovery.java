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

package org.jboss.webbeans.bootstrap.spi;

import java.net.URL;

import org.jboss.webbeans.bootstrap.api.Service;

/**
 * A container should implement this interface to allow Web Beans to
 * discover the beans to deploy
 * 
 * @author Pete Muir
 *
 */
public interface WebBeanDiscovery extends Service
{
   
   /**
    * Gets list of all classes in classpath archives with META-INF/beans.xml (or
    * for WARs WEB-INF/beans.xml) files
    * 
    * @return An iterable over the classes 
    */
   public Iterable<Class<?>> discoverWebBeanClasses();
   
   /**
    * Gets a list of all deployment descriptors in the app classpath
    * 
    * @return An iterable over the beans.xml files 
    */
   public Iterable<URL> discoverWebBeansXml();
   
}
