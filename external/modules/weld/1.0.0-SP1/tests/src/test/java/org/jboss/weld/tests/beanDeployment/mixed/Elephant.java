package org.jboss.weld.tests.beanDeployment.mixed;

import javax.ejb.Remove;
import javax.ejb.Stateful;

@Stateful
class Elephant implements ElephantLocal
{
   
   @Remove
   public void remove1()
   {
      
   }
   
   @Remove
   public void remove2()
   {
      
   }

}
