package org.jboss.webbeans.test.unit.lookup;

import java.util.List;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.TypeLiteral;

import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.webbeans.literal.CurrentLiteral;
import org.jboss.webbeans.test.AbstractWebBeansTest;
import org.testng.annotations.Test;

@Artifact
public class LookupInstanceTest extends AbstractWebBeansTest
{
  
   
   @Test
   public void testLookupInstance() throws Exception
   {
      assert createContextualInstance(new TypeLiteral<Instance<List<?>>>(){}.getRawType(), new CurrentLiteral()) == null; 
   }
   
}
