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
package org.jboss.webbeans.bean.ee.jms;

import java.io.Serializable;

import javax.context.Contextual;
import javax.context.CreationalContext;
import javax.inject.ExecutionException;
import javax.jms.JMSException;
import javax.jms.MessageProducer;

abstract class MessageProducerContextual<T extends MessageProducer> implements Contextual<T>, Serializable
{

   private static final long serialVersionUID = -4333311257129016113L;

   public T create(CreationalContext<T> creationalContext)
   {
      try
      {
         return createMessageProducer();
      }
      catch (JMSException e)
      {
         throw new ExecutionException("Error creating connection ", e);
      }
   }
   
   protected abstract  T createMessageProducer() throws JMSException;

   public void destroy(T instance)
   {
      try
      {
         instance.close();
      }
      catch (JMSException e)
      {
         throw new ExecutionException("Error creating connection ", e);
      }
   }
   
}