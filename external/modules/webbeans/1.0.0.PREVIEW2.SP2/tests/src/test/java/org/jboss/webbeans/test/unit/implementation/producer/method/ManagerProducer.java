package org.jboss.webbeans.test.unit.implementation.producer.method;

import javax.enterprise.inject.Current;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

class ManagerProducer
{
   
   @Current BeanManager beanManager;
   
   private static boolean injectionPointInjected;
   
   public static boolean isInjectionPointInjected()
   {
      return injectionPointInjected;
   }
   
   public static void setInjectionPointInjected(boolean injectionPointInjected)
   {
      ManagerProducer.injectionPointInjected = injectionPointInjected;
   }

   @Produces
   Integer create(InjectionPoint point)
   {
      injectionPointInjected = point != null;
      return 10;
   }

}
