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
package org.jboss.weld.servlet.api;

import javax.servlet.ServletContext;

import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

/**
 * Allows the container to identify BDA in use for a request. This method will
 * be called, in the same thread as the request, every time Weld needs to
 * identify a request.
 * 
 * {@link ServletServices} is a per-deployment service.
 * 
 * @author pmuir
 *
 */
public interface ServletServices extends Service
{
   
   /**
    * Get the BDA for the current request. The ServletContext is provided for
    * context.
    * 
    * @param ctx
    * @return
    */
   public BeanDeploymentArchive getBeanDeploymentArchive(ServletContext ctx);

}
