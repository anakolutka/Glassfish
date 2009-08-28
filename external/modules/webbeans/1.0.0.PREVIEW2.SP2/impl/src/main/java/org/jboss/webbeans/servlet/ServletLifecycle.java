/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
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
package org.jboss.webbeans.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jboss.webbeans.CurrentManager;
import org.jboss.webbeans.context.ContextLifecycle;
import org.jboss.webbeans.context.RequestContext;
import org.jboss.webbeans.context.SessionContext;
import org.jboss.webbeans.context.api.BeanStore;
import org.jboss.webbeans.context.api.helpers.ConcurrentHashMapBeanStore;
import org.jboss.webbeans.log.LogProvider;
import org.jboss.webbeans.log.Logging;

/**
 * Implementation of the Web Beans lifecycle that can react to servlet events
 * and drives the Session, Conversation and Request (for Servlet requests)
 * lifecycle
 * 
 * @author Pete Muir
 * @author Nicklas Karlsson
 */
public class ServletLifecycle
{

   private final ContextLifecycle lifecycle;
   
   public static final String REQUEST_ATTRIBUTE_NAME = ServletLifecycle.class.getName() + ".requestBeanStore";

   private static LogProvider log = Logging.getLogProvider(ServletLifecycle.class);
   
   /**
    * 
    */
   public ServletLifecycle(ContextLifecycle lifecycle)
   {
      this.lifecycle = lifecycle;
   }

   /**
    * Begins a session
    * 
    * @param session The HTTP session
    */
   public void beginSession(HttpSession session)
   {
   }

   /**
    * Ends a session, setting up a mock request if necessary
    * 
    * @param session The HTTP session
    */
   public void endSession(HttpSession session)
   {
      if (SessionContext.instance().isActive())
      {
         lifecycle.endSession(session.getId(), SessionContext.instance().getBeanStore());
      }
      else if (RequestContext.instance().isActive())
      {
         lifecycle.endSession(session.getId(), restoreSessionContext(session));
      }
      else
      {
         BeanStore mockRequest = new ConcurrentHashMapBeanStore();
         lifecycle.beginRequest("endSession-" + session.getId(), mockRequest);
         lifecycle.endSession(session.getId(), restoreSessionContext(session));
         lifecycle.endRequest("endSession-" + session.getId(), mockRequest);
      }
   }

   /**
    * Restore the session from the underlying session object. Also allow the
    * session to be injected by the Session manager
    * 
    * @param session
    * @return the session bean store
    */
   protected BeanStore restoreSessionContext(HttpServletRequest request)
   {
      BeanStore sessionBeanStore = new HttpRequestSessionBeanStore(request);
      HttpSession session = request.getSession(false);
      lifecycle.restoreSession(session == null ? "Inactive session" : session.getId(), sessionBeanStore);
      if (session != null)
      {
         CurrentManager.rootManager().getInstanceByType(HttpSessionManager.class).setSession(session);
      }
      return sessionBeanStore;
   }
   
   protected BeanStore restoreSessionContext(HttpSession session)
   {
      BeanStore beanStore = new HttpSessionBeanStore(session);
      lifecycle.restoreSession(session.getId(), beanStore);
      CurrentManager.rootManager().getInstanceByType(HttpSessionManager.class).setSession(session);
      return beanStore;
   }

   /**
    * Begins a HTTP request
    * 
    * Sets the session into the session context
    * 
    * @param request The request
    */
   public void beginRequest(HttpServletRequest request)
   {
      if (request.getAttribute(REQUEST_ATTRIBUTE_NAME) == null)
      {
         BeanStore beanStore = new ConcurrentHashMapBeanStore();
         request.setAttribute(REQUEST_ATTRIBUTE_NAME, beanStore);
         lifecycle.beginRequest(request.getRequestURI(), beanStore);
         restoreSessionContext(request);
      }
   }

   /**
    * Ends a HTTP request
    * 
    * @param request The request
    */
   public void endRequest(HttpServletRequest request)
   {
      if (request.getAttribute(REQUEST_ATTRIBUTE_NAME) != null)
      {
         BeanStore beanStore = (BeanStore) request.getAttribute(REQUEST_ATTRIBUTE_NAME);
         if (beanStore == null)
         {
            throw new IllegalStateException("Cannot obtain request scoped beans from the request");
         }
         lifecycle.endRequest(request.getRequestURI(), beanStore);
         request.removeAttribute(REQUEST_ATTRIBUTE_NAME);
         SessionContext.instance().setActive(false);
         SessionContext.instance().setBeanStore(null);
      }
   }

}
