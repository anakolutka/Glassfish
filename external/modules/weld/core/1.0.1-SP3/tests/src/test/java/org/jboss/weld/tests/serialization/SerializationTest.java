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
package org.jboss.weld.tests.serialization;

import java.io.Serializable;

import javax.enterprise.inject.IllegalProductException;

import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.weld.test.AbstractWeldTest;
import org.jboss.weld.test.Utils;
import org.testng.annotations.Test;

@Artifact
public class SerializationTest extends AbstractWeldTest
{

   @Test(description="WELD-363")
   public void testConversationManagerSerializable()
      throws Exception
   {
      TestConversationManager cMgr = getReference(TestConversationManager.class);
      
      assert cMgr.getConversationInstance() != null;
      assert cMgr.getConversationInstance().get() != null;
      
      Object deserialized = Utils.deserialize(Utils.serialize(cMgr));
      
      assert deserialized instanceof TestConversationManager;
      TestConversationManager deserializedCMgr = (TestConversationManager) deserialized;
      assert deserializedCMgr.getConversationInstance() != null;
      assert deserializedCMgr.getConversationInstance().get() != null;
   }
   
   @Test(description="http://lists.jboss.org/pipermail/weld-dev/2010-February/002265.html")
   public void testNonSerializableProductInjectedIntoSessionScopedBean()
   {
      try
      {
         getReference(LoggerConsumer.class).ping();
      }
      catch (Exception e) 
      {
         // If Logger isn't serializable, we get here 
         assert e instanceof IllegalProductException;
         return;
      }
      // If Logger is serializable we get here
      assert getReference(LoggerConsumer.class).getLog() instanceof Serializable;
   }
}
