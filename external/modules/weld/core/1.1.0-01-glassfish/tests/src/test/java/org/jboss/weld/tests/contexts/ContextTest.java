/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.tests.contexts;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Conversation;

import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.weld.Container;
import org.jboss.weld.context.ContextLifecycle;
import org.jboss.weld.test.AbstractWeldTest;
import org.testng.annotations.Test;

@Artifact
public class ContextTest extends AbstractWeldTest
{

   @Test(description = "WELD-348")
   public void testCallToConversationWithContextNotActive()
   {
      boolean alreadyActive = false;
      try
      {
         alreadyActive = Container.instance().services().get(ContextLifecycle.class).isConversationActive();
         if (alreadyActive)
         {
            Container.instance().services().get(ContextLifecycle.class).getConversationContext().setActive(false);
         }
         try
         {
            getReference(Conversation.class).getId();
            assert false;
         }
         catch (ContextNotActiveException e) 
         {
            // Expected
         }
         catch (Exception e) 
         {
            assert false;
         }
         try
         {
            getReference(Conversation.class).getTimeout();
            assert false;
         }
         catch (ContextNotActiveException e) 
         {
            // Expected
         }
         catch (Exception e) 
         {
            assert false;
         }
         try
         {
            getReference(Conversation.class).begin();
            assert false;
         }
         catch (ContextNotActiveException e) 
         {
            // Expected
         }
         catch (Exception e) 
         {
            assert false;
         }
         try
         {
            getReference(Conversation.class).begin("foo");
            assert false;
         }
         catch (ContextNotActiveException e) 
         {
            // Expected
         }
         catch (Exception e) 
         {
            assert false;
         }
         try
         {
            getReference(Conversation.class).end();
            assert false;
         }
         catch (ContextNotActiveException e) 
         {
            // Expected
         }
         catch (Exception e) 
         {
            assert false;
         }
         try
         {
            getReference(Conversation.class).isTransient();
            assert false;
         }
         catch (ContextNotActiveException e) 
         {
            // Expected
         }
         catch (Exception e) 
         {
            assert false;
         }
         try
         {
            getReference(Conversation.class).setTimeout(0);
            assert false;
         }
         catch (ContextNotActiveException e) 
         {
            // Expected
         }
         catch (Exception e) 
         {
            assert false;
         }
      }
      finally
      {
         if (alreadyActive)
         {
            Container.instance().services().get(ContextLifecycle.class).getConversationContext().setActive(true);
         }
      }
      
   }
   
   
   
}
