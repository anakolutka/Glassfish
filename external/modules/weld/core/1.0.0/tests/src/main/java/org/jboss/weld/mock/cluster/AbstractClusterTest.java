package org.jboss.weld.mock.cluster;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

import org.jboss.weld.BeanManagerImpl;
import org.jboss.weld.bootstrap.api.SingletonProvider;
import org.jboss.weld.context.ContextLifecycle;
import org.jboss.weld.context.api.BeanStore;
import org.jboss.weld.mock.MockEELifecycle;
import org.jboss.weld.mock.TestContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class AbstractClusterTest
{

   @BeforeClass
   public void beforeClass()
   {
      SingletonProvider.reset();
      SingletonProvider.initialize(new SwitchableSingletonProvider());
   }
   
   @AfterClass
   public void afterClass()
   {
      SingletonProvider.reset();
   }

   protected TestContainer bootstrapContainer(int id, Collection<Class<?>> classes)
   {
      // Bootstrap container
      SwitchableSingletonProvider.use(id);
      
      TestContainer container = new TestContainer(new MockEELifecycle(), classes, null);
      container.startContainer();
      container.ensureRequestActive();
      
      return container;
   }
   
   protected void use(int id)
   {
      SwitchableSingletonProvider.use(id);
   }

   protected void replicateSession(int fromId, BeanManagerImpl fromBeanManager, int toId, BeanManagerImpl toBeanManager) throws Exception
   {
      // Mimic replicating the session
      BeanStore sessionBeanStore = fromBeanManager.getServices().get(ContextLifecycle.class).getSessionContext().getBeanStore();
      byte[] bytes = serialize(sessionBeanStore);
      use(toId);
      BeanStore replicatedSessionBeanStore = (BeanStore) deserialize(bytes);
      toBeanManager.getServices().get(ContextLifecycle.class).getSessionContext().setBeanStore(replicatedSessionBeanStore);
      use(fromId);
   }

   protected byte[] serialize(Object instance) throws IOException
   {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bytes);
      out.writeObject(instance);
      return bytes.toByteArray();
   }

   protected Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException
   {
      ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
      return in.readObject();
   }

}