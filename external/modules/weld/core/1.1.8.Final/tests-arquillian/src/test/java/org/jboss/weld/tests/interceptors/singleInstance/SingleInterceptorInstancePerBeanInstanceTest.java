/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.weld.tests.interceptors.singleInstance;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.BeanArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class SingleInterceptorInstancePerBeanInstanceTest {

    @Inject
    private Lion lion;
    @Inject
    private Lioness lioness;
    @Inject
    private Tiger tiger;
    @Inject
    private Tigress tigress;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(BeanArchive.class).intercept(LionInterceptor.class, TigerInterceptor.class)
                .addPackage(SingleInterceptorInstancePerBeanInstanceTest.class.getPackage());
    }

    @Test
    public void testSingleInterceptorInstanceUsedForSubsequentInvocationWithCdiBinding() {
        assertEquals(tiger.foo(), tiger.foo());
        assertEquals(lion.foo(), lion.foo());
    }

    @Test
    public void testSingleInterceptorInstanceUsedForSubsequentInvocation() {
        assertEquals(lioness.foo(), lioness.foo());
        assertEquals(tigress.foo(), tigress.foo());
    }

    @Test
    public void testSingleInterceptorInstanceUsedForDifferentMethodsWithCdiBinding() {
        assertEquals(tiger.foo(), tiger.bar());
        assertEquals(lion.foo(), lion.bar());
    }

    @Test
    public void testSingleInterceptorInstanceUsedForDifferentMethods() {
        assertEquals(lioness.foo(), lioness.bar());
        assertEquals(tigress.foo(), tigress.bar());
    }

    @Test
    public void testSingleInterceptorInstanceUsedForMethodAndLifecycleCallbackWithCdiBinding() {
        assertEquals(lioness.getPostConstructInterceptor(), lioness.foo());
    }

    @Test
    public void testSingleInterceptorInstanceUsedForMethodAndLifecycleCallback() {
        assertEquals(lion.getPostConstructInterceptor(), lion.foo());
    }
}
