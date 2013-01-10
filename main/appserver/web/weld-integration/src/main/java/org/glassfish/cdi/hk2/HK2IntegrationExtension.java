/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.cdi.hk2;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

/**
 * The touch-point for hk2 integration with CDI
 * 
 * @author jwells
 *
 */
public class HK2IntegrationExtension implements Extension {
    private final HK2IntegrationUtilities utilities = new HK2IntegrationUtilities();
    private final HashMap<Long, ActiveDescriptor<?>> foundWithHK2 = new HashMap<Long, ActiveDescriptor<?>>();
    private final ServiceLocator locator = utilities.getApplicationServiceLocator();
    
    private static Annotation[] getHK2Qualifiers(InjectionPoint injectionPoint) {
        Set<Annotation> setQualifiers = injectionPoint.getQualifiers();
        
        Set<Annotation> retVal = new HashSet<Annotation>();
        
        for (Annotation anno : setQualifiers) {
            if (anno.annotationType().equals(Default.class)) continue;
            
            retVal.add(anno);
        }
        
        return retVal.toArray(new Annotation[retVal.size()]);
    }
    
    /**
     * Called by CDI, gathers up all of the injection points known to hk2
     * 
     * @param pit The injection target even from CDI
     */
    @SuppressWarnings("unused")
    private <T> void injectionTargetObserver(@Observes ProcessInjectionTarget<T> pit) {
        InjectionTarget<?> injectionTarget = pit.getInjectionTarget();
        Set<InjectionPoint> injectionPoints = injectionTarget.getInjectionPoints();
        
        for (InjectionPoint injectionPoint : injectionPoints) {
            Annotation qualifiers[] = getHK2Qualifiers(injectionPoint);
            
            ServiceHandle<?> handle = locator.getServiceHandle(injectionPoint.getType(), qualifiers);
            if (handle == null) continue;
            
            ActiveDescriptor<?> descriptor = handle.getActiveDescriptor();
            
            // using a map removes duplicates
            foundWithHK2.put(descriptor.getServiceId(), descriptor);
        }
    }
    
    /**
     * Called by CDI after going through all of the injection points.  For each
     * service known to hk2 adds a CDI bean
     * 
     * @param abd This is used just to mark the type of the event
     */
    @SuppressWarnings({ "unused", "unchecked", "rawtypes" })
    private void afterDiscoveryObserver(@Observes AfterBeanDiscovery abd) {
        for (ActiveDescriptor<?> descriptor : foundWithHK2.values()) {
            abd.addBean(new HK2CDIBean(locator, descriptor));
        }
    }
    
    /**
     * Called by CDI after it has been completely validated.  Will add the JIT resolver to HK2
     * with the BeanManager
     * 
     * @param event This is just to mark the type of the event
     * @param manager The manager that will be used to get references
     */
    @SuppressWarnings("unused")
    private void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager manager) {
        CDISecondChanceResolver jit = new CDISecondChanceResolver(locator, manager);
        
        ServiceLocatorUtilities.addOneConstant(locator, jit);
    }

    public String toString() {
        return "HK2IntegrationExtension(" + locator + "," + System.identityHashCode(this) + ")";
    }
}
