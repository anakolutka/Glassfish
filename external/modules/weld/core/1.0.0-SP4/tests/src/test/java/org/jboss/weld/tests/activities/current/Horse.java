package org.jboss.weld.tests.activities.current;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

class Horse
{
   
   @Inject BeanManager beanManager;
   
   public BeanManager getManager()
   {
      return beanManager;
   }
   
}
