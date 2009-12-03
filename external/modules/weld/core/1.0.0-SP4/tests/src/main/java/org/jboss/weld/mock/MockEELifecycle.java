/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.weld.mock;

import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.jboss.weld.validation.spi.ValidationServices;

public class MockEELifecycle extends MockServletLifecycle
{
   
   private static final TransactionServices MOCK_TRANSACTION_SERVICES = new MockTransactionServices();

   public MockEELifecycle()
   {
      super();
      getDeployment().getServices().add(TransactionServices.class, MOCK_TRANSACTION_SERVICES);
      getDeployment().getServices().add(SecurityServices.class, new MockSecurityServices());
      getDeployment().getServices().add(ValidationServices.class, new MockValidationServices());
      getDeployment().getServices().add(EjbServices.class, new MockEjBServices());
      getWar().getServices().add(EjbInjectionServices.class, new MockEjbInjectionServices());
      getWar().getServices().add(JpaInjectionServices.class, new MockJpaServices(getDeployment()));
      getWar().getServices().add(ResourceInjectionServices.class, new MockResourceServices());
   }
   
   @Override
   public Environment getEnvironment()
   {
      return Environments.EE_INJECT;
   }
   
}
