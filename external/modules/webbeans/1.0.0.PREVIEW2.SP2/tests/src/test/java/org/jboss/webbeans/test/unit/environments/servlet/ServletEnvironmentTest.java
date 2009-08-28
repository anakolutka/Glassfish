package org.jboss.webbeans.test.unit.environments.servlet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.AnnotationLiteral;
import javax.enterprise.inject.spi.Bean;

import org.jboss.webbeans.BeanManagerImpl;
import org.jboss.webbeans.CurrentManager;
import org.jboss.webbeans.bean.RIBean;
import org.jboss.webbeans.bean.SimpleBean;
import org.jboss.webbeans.mock.MockBeanDeploymentArchive;
import org.jboss.webbeans.mock.MockServletLifecycle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class ServletEnvironmentTest
{
   
   private MockServletLifecycle lifecycle;
   private BeanManagerImpl manager;
   
   @BeforeClass
   public void beforeClass() throws Throwable
   {
      lifecycle = new MockServletLifecycle(); 
      lifecycle.initialize();
      MockBeanDeploymentArchive archive = lifecycle.getDeployment().getArchive();
      archive.setBeanClasses(Arrays.asList(Animal.class, DeadlyAnimal.class, DeadlySpider.class, DeadlyAnimal.class, Hound.class, HoundLocal.class, Salmon.class, ScottishFish.class, SeaBass.class, Sole.class, Spider.class, Tarantula.class, TarantulaProducer.class, Tuna.class));
      lifecycle.beginApplication();
      lifecycle.beginSession();
      lifecycle.beginRequest();
      manager = CurrentManager.rootManager();
   }
   
   @AfterClass(alwaysRun=true)
   public void afterClass() throws Exception
   {
      lifecycle.endRequest();
      lifecycle.endSession();
      lifecycle.endApplication();
      lifecycle = null;
   }
   
   public void testSimpleBeans()
   {
      Map<Class<?>, Bean<?>> beans = new HashMap<Class<?>, Bean<?>>();
      for (Bean<?> bean : manager.getBeans())
      {
         if (bean instanceof RIBean)
         {
            beans.put(((RIBean<?>) bean).getType(), bean);
         }
      }
      assert beans.containsKey(Tuna.class);
      assert beans.containsKey(Salmon.class);
      assert beans.containsKey(SeaBass.class);
      assert beans.containsKey(Sole.class);
      
      assert beans.get(Tuna.class) instanceof SimpleBean;
      assert beans.get(Salmon.class) instanceof SimpleBean;
      assert beans.get(SeaBass.class) instanceof SimpleBean;
      assert beans.get(Sole.class) instanceof SimpleBean;
      manager.getInstanceByType(Sole.class, new AnnotationLiteral<Whitefish>() {}).ping();
   }
   
   public void testProducerMethodBean()
   {
      Map<Class<?>, Bean<?>> beans = new HashMap<Class<?>, Bean<?>>();
      for (Bean<?> bean : manager.getBeans())
      {
         if (bean instanceof RIBean)
         {
            beans.put(((RIBean<?>) bean).getType(), bean);
         }
      }
      assert beans.containsKey(TarantulaProducer.class);
      assert beans.containsKey(Tarantula.class);
      
      Bean<?> bean = beans.get(TarantulaProducer.class);
      
      assert beans.get(TarantulaProducer.class) instanceof SimpleBean;
      manager.getInstanceByType(Tarantula.class, new AnnotationLiteral<Tame>() {}).ping();
   }
   
   public void testSingleEnterpriseBean()
   {
      List<Bean<?>> beans = manager.getBeans();
      Map<Class<?>, Bean<?>> classes = new HashMap<Class<?>, Bean<?>>();
      for (Bean<?> bean : beans)
      {
         if (bean instanceof RIBean)
         {
            classes.put(((RIBean<?>) bean).getType(), bean);
         }
      }
      assert classes.containsKey(Hound.class);
      assert classes.get(Hound.class) instanceof SimpleBean;
      manager.getInstanceByType(HoundLocal.class, new AnnotationLiteral<Tame>() {}).ping();
   }
   
}
