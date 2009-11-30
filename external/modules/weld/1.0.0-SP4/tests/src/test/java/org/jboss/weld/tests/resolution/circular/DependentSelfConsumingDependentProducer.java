package org.jboss.weld.tests.resolution.circular;

import javax.enterprise.inject.Produces;

class DependentSelfConsumingDependentProducer
{
   @SelfConsumingDependent Violation violation;
   
   @Produces @SelfConsumingDependent
   public Violation produceViolation() {
      return new Violation();
   }
   
   public void ping() {
      
   }
} 