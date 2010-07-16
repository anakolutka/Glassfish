/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.tests.extensions;

import org.jboss.testharness.impl.packaging.Artifact;
import org.jboss.testharness.impl.packaging.IntegrationTest;
import org.jboss.testharness.impl.packaging.Packaging;
import org.jboss.testharness.impl.packaging.PackagingType;
import org.jboss.testharness.impl.packaging.jsr299.Extension;
import org.jboss.weld.test.AbstractWeldTest;
import org.testng.annotations.Test;

@Artifact
@IntegrationTest
@Packaging(PackagingType.EAR)
@Extension("javax.enterprise.inject.spi.Extension")
public class ExtensionTest extends AbstractWeldTest
{
   
   @Test(description="WELD-234")
   public void testExtensionInjectableAsBean()
   {
      assert getReference(SimpleExtension.class).isObservedBeforeBeanDiscovery();
   }
   
   @Test(description="WELD-243")
   public void testContainerEventsOnlySentToExtensionBeans()
   {
      ExtensionObserver extensionObserver = getReference(ExtensionObserver.class);
      OtherObserver otherObserver = getReference(OtherObserver.class);
      
      assert extensionObserver.isBeforeBeanDiscovery();
      assert extensionObserver.isAllBeforeBeanDiscovery();
      assert !otherObserver.isBeforeBeanDiscovery();
      assert !otherObserver.isAllBeforeBeanDiscovery();
      
      assert extensionObserver.isAfterBeanDiscovery();
      assert extensionObserver.isAllAfterBeanDiscovery();
      assert !otherObserver.isAfterBeanDiscovery();
      assert !otherObserver.isAllAfterBeanDiscovery();
      
      assert extensionObserver.isProcessAnnotatedType();
      assert extensionObserver.isAllProcessAnnnotatedType();
      assert !otherObserver.isProcessAnnotatedType();
      assert !otherObserver.isAllProcessAnnotatedType();
      
      assert extensionObserver.isProcessBean();
      assert extensionObserver.isAllProcessBean();
      assert !otherObserver.isProcessBean();
      assert !otherObserver.isAllProcessBean();
      
      assert extensionObserver.isProcessInjectionTarget();
      assert extensionObserver.isAllProcessInjectionTarget();
      assert !otherObserver.isProcessInjectionTarget();
      assert !otherObserver.isAllProcessInjectionTarget();
      
      assert extensionObserver.isProcessManagedBean();
      assert extensionObserver.isAllProcessManagedBean();
      assert !otherObserver.isProcessManagedBean();
      assert !otherObserver.isAllProcessManagedBean();
      
      assert extensionObserver.isProcessObserverMethod();
      assert extensionObserver.isAllProcessObserverMethod();
      assert !otherObserver.isProcessObserverMethod();
      assert !otherObserver.isAllProcessObserverMethod();
      
      assert extensionObserver.isProcessProducer();
      assert extensionObserver.isAllProcessProducer();
      assert !otherObserver.isProcessProducer();
      assert !otherObserver.isAllProcessProducer();
      
      assert extensionObserver.isProcessProducerField();
      assert extensionObserver.isAllProcessProducerField();
      assert !otherObserver.isProcessProducerField();
      assert !otherObserver.isAllProcessProducerField();
      
      assert extensionObserver.isProcessProducerMethod();
      assert extensionObserver.isAllProcessProducerField();
      assert !otherObserver.isProcessProducerMethod();
      assert !otherObserver.isAllProcessProducerMethod();
      
      assert extensionObserver.isProcessSessionBean();
      assert extensionObserver.isAllProcessSessionBean();
      assert !otherObserver.isProcessSessionBean();
      assert !otherObserver.isAllProcessSessionBean();
      
      assert extensionObserver.isAfterDeploymentValidation();
      assert extensionObserver.isAllAfterDeploymentValidation();
      assert !otherObserver.isAfterDeploymentValidation();
      assert !otherObserver.isAllAfterDeploymentValidation();
      
   }
   
   @Test
   public void testInjectionTargetWrapped()
   {
      getReference(Capercaillie.class);
      assert Woodland.isPostConstructCalled();
      assert WoodlandExtension.isInjectCalled();
      assert WoodlandExtension.isPostConstructCalled();
      assert WoodlandExtension.isPreDestroyCalled();
      assert WoodlandExtension.isProduceCalled();
   }

}
