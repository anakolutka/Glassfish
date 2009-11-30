package org.jboss.weld.tests.beanDeployment.managed.multiple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.Bean;

import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.weld.bean.ManagedBean;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.test.AbstractWeldTest;
import org.testng.annotations.Test;

@Artifact
public class BootstrapTest extends AbstractWeldTest
{
   
   @Test(groups="bootstrap")
   public void testMultipleSimpleBean()
   {
      List<Bean<?>> beans = getCurrentManager().getBeans();
      Map<Class<?>, Bean<?>> classes = new HashMap<Class<?>, Bean<?>>();
      for (Bean<?> bean : beans)
      {
         if (bean instanceof RIBean)
         {
            classes.put(((RIBean<?>) bean).getType(), bean);
         }
      }
      assert classes.containsKey(Tuna.class);
      assert classes.containsKey(Salmon.class);
      assert classes.containsKey(SeaBass.class);
      assert classes.containsKey(Sole.class);
      
      assert classes.get(Tuna.class) instanceof ManagedBean;
      assert classes.get(Salmon.class) instanceof ManagedBean;
      assert classes.get(SeaBass.class) instanceof ManagedBean;
      assert classes.get(Sole.class) instanceof ManagedBean;
   }
   
}
