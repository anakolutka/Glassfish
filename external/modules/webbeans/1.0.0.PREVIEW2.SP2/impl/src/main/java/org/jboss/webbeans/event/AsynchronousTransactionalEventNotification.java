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
package org.jboss.webbeans.event;

import org.jboss.webbeans.context.DependentContext;
import org.jboss.webbeans.log.Log;
import org.jboss.webbeans.log.Logging;

/**
 * @author David Allen
 *
 */
public class AsynchronousTransactionalEventNotification<T> extends DeferredEventNotification<T>
{
   private static Log log = Logging.getLog(DeferredEventNotification.class);

   public AsynchronousTransactionalEventNotification(T event, ObserverImpl<T> observer)
   {
      super(event, observer);
   }

   @Override
   public void run()
   {
      // Let the event be deferred again as just an asynchronous event
      DependentContext.instance().setActive(true);
      try
      {
         log.trace("Sending event [" + event + "] asynchronously to transaction observer " + observer);
         observer.sendEventAsynchronously(event);
      }
      catch (Exception e)
      {
         log.error("Failure while queuing observer for event [" + event + "]", e);
      }
      finally
      {
         DependentContext.instance().setActive(false);
      }
   }

}
