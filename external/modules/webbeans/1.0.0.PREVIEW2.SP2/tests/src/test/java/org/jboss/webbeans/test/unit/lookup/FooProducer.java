package org.jboss.webbeans.test.unit.lookup;

import javax.enterprise.inject.Produces;

public class FooProducer
{
   
   @Produces @Special
   public FooBase<Baz> produce()
   {
      return new FooBase<Baz>();
   }

}
