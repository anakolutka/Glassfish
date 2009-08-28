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
package org.jboss.webbeans.jsf;

import javax.context.Conversation;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpSession;

import org.jboss.webbeans.CurrentManager;
import org.jboss.webbeans.context.ConversationContext;
import org.jboss.webbeans.conversation.ConversationManager;
import org.jboss.webbeans.log.LogProvider;
import org.jboss.webbeans.log.Logging;
import org.jboss.webbeans.servlet.ConversationBeanStore;
import org.jboss.webbeans.servlet.HttpSessionManager;

/**
 * A phase listener for propagating conversation id over postbacks through a
 * hidden component
 * 
 * @author Nicklas Karlsson
 * 
 */
public class WebBeansPhaseListener implements PhaseListener
{
   // The logging provider
   private static LogProvider log = Logging.getLogProvider(WebBeansPhaseListener.class);

   /**
    * Run before a given phase
    * 
    * @param phaseEvent The phase event
    */
   public void beforePhase(PhaseEvent phaseEvent)
   {
      if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE))
      {
         beforeRenderReponse();
      }
   }

   /**
    * Run before the response is rendered
    */
   private void beforeRenderReponse()
   {
      log.trace("In before render response phase");
      Conversation conversation = CurrentManager.rootManager().getInstanceByType(Conversation.class);
      if (conversation.isLongRunning())
      {
         PhaseHelper.propagateConversation(conversation.getId());
      }
      else
      {
         PhaseHelper.stopConversationPropagation();
      }
   }

   /**
    * Run after a given phase
    * 
    * @param phaseEvent The phase event
    */
   public void afterPhase(PhaseEvent phaseEvent)
   {
      if (phaseEvent.getPhaseId().equals(PhaseId.RESTORE_VIEW))
      {
         afterRestoreView();
      }
      else if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE))
      {
         afterRenderResponse();
      }
      
      if(phaseEvent.getFacesContext().getResponseComplete())
      {
         afterResponseComplete();
      }      
   }

   /**
    * Run after the response is complete
    */
   private void afterResponseComplete()
   {
      log.trace("Post-response complete");
      CurrentManager.rootManager().getInstanceByType(ConversationManager.class).cleanupConversation();
   }

   /**
    * Run after the view is restored
    */
   private void afterRestoreView()
   {
      log.trace("In after restore view phase");
      HttpSession session = PhaseHelper.getHttpSession();
      CurrentManager.rootManager().getInstanceByType(HttpSessionManager.class).setSession(session);
      CurrentManager.rootManager().getInstanceByType(ConversationManager.class).beginOrRestoreConversation(PhaseHelper.getConversationId());
      String cid = CurrentManager.rootManager().getInstanceByType(Conversation.class).getId();
      ConversationContext.instance().setBeanStore(new ConversationBeanStore(session, cid));
      ConversationContext.instance().setActive(true);
   }

   /**
    * Run after the response is rendered
    */
   private void afterRenderResponse()
   {
      log.trace("In after render reponse phase");
      CurrentManager.rootManager().getInstanceByType(ConversationManager.class).cleanupConversation();
      ConversationContext.instance().setActive(false);
   }

   public PhaseId getPhaseId()
   {
      return PhaseId.ANY_PHASE;
   }

}
