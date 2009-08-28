package org.jboss.webbeans.test.unit.implementation.producer.method;

import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.webbeans.test.AbstractWebBeansTest;
import org.testng.annotations.Test;

@Artifact
public class NamedProducerTest extends AbstractWebBeansTest
{
   @Test
   public void testNamedProducer()
   {
      String[] iemon = (String[]) getCurrentManager().getInstanceByName("iemon");
      assert iemon.length == 3;
      String[] itoen = (String[]) getCurrentManager().getInstanceByName("itoen");
      assert itoen.length == 2;
   }

}
