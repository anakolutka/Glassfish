package org.jboss.webbeans.test.unit.implementation.proxy;

import javax.enterprise.inject.spi.Bean;

import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.webbeans.test.AbstractWebBeansTest;
import org.testng.annotations.Test;

@Artifact
public class ProxyTest extends AbstractWebBeansTest
{
   
   @Test(description="WBRI-122")
   public void testImplementationClassImplementsSerializable()
   {
      Bean<?> bean = getCurrentManager().resolve(getCurrentManager().getBeans("foo"));
      getCurrentManager().getReference(bean, Object.class, getCurrentManager().createCreationalContext(bean));
   }
   
}
