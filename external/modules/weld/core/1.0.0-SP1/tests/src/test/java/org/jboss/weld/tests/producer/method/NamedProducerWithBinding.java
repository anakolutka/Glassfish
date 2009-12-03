package org.jboss.weld.tests.producer.method;

import java.util.Date;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

/**
 * @author Dan Allen
 */
public class NamedProducerWithBinding {
   public @Produces @Important @Named Date getDate() {
      return new Date();
   }
}
