package org.jboss.webbeans.test.unit.bootstrap.multipleEnterprise;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.enterprise.inject.Named;

@Stateful
@Tame
@Named("Pongo")
class Hound implements HoundLocal
{ 
   @Remove
   public void bye() {
   }

}
