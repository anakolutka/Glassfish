package org.jboss.weld.bootstrap.api.test;

import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.injection.spi.ResourceInjectionServices;

public class MockResourceServices extends MockService implements ResourceInjectionServices
{
   
   public Object resolveResource(InjectionPoint injectionPoint)
   {
      return null;
   }
   
   public Object resolveResource(String jndiName, String mappedName)
   {
      return null;
   }
   
}
