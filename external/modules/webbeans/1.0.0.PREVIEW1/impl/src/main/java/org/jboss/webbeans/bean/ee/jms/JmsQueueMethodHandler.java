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

import javax.inject.ExecutionException;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.jboss.webbeans.CurrentManager;
import org.jboss.webbeans.messaging.spi.JmsServices;

/**
 * @author Pete Muir
 *
 */
class JmsQueueMethodHandler extends JmsMethodHandler<QueueConnection, QueueSession, QueueSender, QueueReceiver>
{

   private static final long serialVersionUID = -1498128944732531956L;
   
   private final ConnectionContextual<QueueConnection> connectionContexual;
   private final SessionContextual<QueueSession> sessionContextual;
   private final MessageProducerContextual<QueueSender> messageProducerContextual;
   private final MessageConsumerContextual<QueueReceiver> messageConsumerContextual;

   public JmsQueueMethodHandler(String jndiName, String mappedName)
   {
      super(jndiName, mappedName);
      final JmsServices jmsServices = CurrentManager.rootManager().getServices().get(JmsServices.class);
      this.connectionContexual = new ConnectionContextual<QueueConnection>()
      {

         private static final long serialVersionUID = 7830020942920371399L;

         @Override
         protected QueueConnection createConnection() throws JMSException
         {
            return jmsServices.getQueueConnectionFactory().createQueueConnection();
         }

      };
      this.sessionContextual = new SessionContextual<QueueSession>()
      {

         private static final long serialVersionUID = -5964106446504141417L;

         @Override
         protected QueueSession createSession() throws JMSException
         {
            return createSessionFromConnection(connectionContexual);
         }

      };
      this.messageProducerContextual = new MessageProducerContextual<QueueSender>()
      {

         private static final long serialVersionUID = 3215720243380210179L;

         @Override
         protected QueueSender createMessageProducer() throws JMSException
         {
            Queue queue = jmsServices.resolveDestination(getJndiName(), getMappedName());
            try
            {
               return createSessionFromConnection(connectionContexual).createSender(queue);
            }
            catch (JMSException e)
            {
               throw new ExecutionException("Error creating QueueSender", e);
            }
         }

      };
      this.messageConsumerContextual = new MessageConsumerContextual<QueueReceiver>()
      {

         private static final long serialVersionUID = -5461921479716229659L;

         @Override
         protected QueueReceiver createMessageConsumer() throws JMSException
         {
            Queue queue = jmsServices.resolveDestination(getJndiName(), getMappedName());
            try
            {
               return createSessionFromConnection(connectionContexual).createReceiver(queue);
            }
            catch (JMSException e)
            {
               throw new ExecutionException("Error creating QueueReceiver", e);
            }
         }

      };
   }

   @Override
   protected QueueSession createSessionFromConnection(ConnectionContextual<QueueConnection> connectionContexual)
   {
      try
      {
         return getConnection(connectionContexual).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
      }
      catch (JMSException e)
      {
         throw new ExecutionException("Error creating session", e);
      }
   }

   @Override
   protected ConnectionContextual<QueueConnection> getConnectionContextual()
   {
      return connectionContexual;
   }

   @Override
   protected MessageConsumerContextual<QueueReceiver> getMessageConsumerContextual()
   {
      return messageConsumerContextual;
   }

   @Override
   protected MessageProducerContextual<QueueSender> getMessageProducerContextual()
   {
      return messageProducerContextual;
   }

   @Override
   protected SessionContextual<QueueSession> getSessionContextual()
   {
      return sessionContextual;
   }
   
   

}
