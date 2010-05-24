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
package org.jboss.weld.conversation;

import static org.jboss.weld.logging.Category.CONVERSATION;
import static org.jboss.weld.logging.LoggerFactory.loggerFactory;
import static org.jboss.weld.logging.messages.BeanManagerMessage.CONTEXT_NOT_ACTIVE;
import static org.jboss.weld.logging.messages.ConversationMessage.BEGIN_CALLED_ON_LONG_RUNNING_CONVERSATION;
import static org.jboss.weld.logging.messages.ConversationMessage.DEMOTED_LRC;
import static org.jboss.weld.logging.messages.ConversationMessage.END_CALLED_ON_TRANSIENT_CONVERSATION;
import static org.jboss.weld.logging.messages.ConversationMessage.PROMOTED_TRANSIENT;
import static org.jboss.weld.logging.messages.ConversationMessage.SWITCHED_CONVERSATION;

import java.io.Serializable;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.weld.Container;
import org.jboss.weld.context.ContextLifecycle;
import org.jboss.weld.context.ContextNotActiveException;
import org.jboss.weld.exceptions.ForbiddenStateException;
import org.slf4j.cal10n.LocLogger;

/**
 * The current conversation implementation
 * 
 * @author Nicklas Karlsson
 * @see javax.enterprise.context.Conversation
 */
@RequestScoped
@Named("javax.enterprise.context.conversation")
@Default
public class ConversationImpl implements Conversation, Serializable
{

   /**
    * Eclipse generated UID.
    */
   private static final long serialVersionUID = 5262382965141841363L;

   private static final LocLogger log = loggerFactory().getLogger(CONVERSATION);

   // The conversation ID
   private String id;
   // The original conversation ID (if any)
   private String originalId;
   // Is the conversation long-running?
   private boolean _transient = true;
   // The timeout in milliseconds
   private long timeout;

   /**
    * Creates a new conversation
    */
   public ConversationImpl() {}
   
   protected void checkConversationActive()
   {
      if (!Container.instance().services().get(ContextLifecycle.class).getConversationContext().isActive())
      {
         throw new ContextNotActiveException(CONTEXT_NOT_ACTIVE, "@ConversationScoped");
      }
   }

   /**
    * Creates a new conversation from an existing one.
    * 
    * @param conversation The old conversation
    */
   public ConversationImpl(ConversationImpl conversation)
   {
      this.id = conversation.getUnderlyingId();
      this._transient = conversation.isTransient();
      this.timeout = conversation.getTimeout();
   }

   /**
    * Initializes a new conversation
    * 
    * @param conversationIdGenerator The conversation ID generator
    * @param timeout The conversation inactivity timeout
    */
   @Inject
   public void init(ConversationIdGenerator conversationIdGenerator, @ConversationInactivityTimeout long timeout)
   {
      this.id = conversationIdGenerator.nextId();
      this.timeout = timeout;
      this._transient = true;
   }

   public void begin()
   {
      checkConversationActive();
      if (!isTransient())
      {
         throw new ForbiddenStateException(BEGIN_CALLED_ON_LONG_RUNNING_CONVERSATION);
      }
      log.debug(PROMOTED_TRANSIENT, id);
      this._transient = false;
   }

   public void begin(String id)
   {
      checkConversationActive();
      // Store away the (first) change to the conversation ID. If the original
      // conversation was long-running,
      // we might have to place it back for termination once the request is
      // over.
      if (originalId == null)
      {
         originalId = id;
      }
      this.id = id;
      begin();
   }

   public void end()
   {
      checkConversationActive();
      if (isTransient())
      {
         throw new ForbiddenStateException(END_CALLED_ON_TRANSIENT_CONVERSATION);
      }
      log.debug(DEMOTED_LRC, id);
      this._transient = true;
   }

   public String getId()
   {
      checkConversationActive();
      if (!isTransient())
      {
         return id;
      }
      else
      {
         return null;
      }
   }

   /**
    * Get the Conversation Id, regardless of whether the conversation is long
    * running or transient, needed for internal operations
    * 
    * @return the id
    */
   public String getUnderlyingId()
   {
      return id;
   }

   public long getTimeout()
   {
      checkConversationActive();
      return timeout;
   }

   public void setTimeout(long timeout)
   {
      checkConversationActive();
      this.timeout = timeout;
   }

   /**
    * Assumes the identity of another conversation
    * 
    * @param conversation The new conversation
    * 
    */
   public void switchTo(ConversationImpl conversation)
   {
      log.debug(SWITCHED_CONVERSATION, this, conversation);
      id = conversation.id;
      this._transient = conversation._transient;
      timeout = conversation.timeout;
   }

   @Override
   public String toString()
   {
      return "ID: " + id + ", transient: " + isTransient() + ", timeout: " + timeout + "ms";
   }

   /**
    * Gets the original ID of the conversation
    * 
    * @return The id
    */
   public String getOriginalId()
   {
      return originalId;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof ConversationImpl)
      {
         ConversationImpl that = (ConversationImpl) obj;
         return (id == null || that.getUnderlyingId() == null) ? false : id.equals(that.getUnderlyingId());
      }
      else
      {
         return false;
      }
   }

   @Override
   public int hashCode()
   {
      return id == null ? super.hashCode() : id.hashCode();
   }

   public boolean isTransient()
   {
      checkConversationActive();
      return _transient;
   }
   
   
}
