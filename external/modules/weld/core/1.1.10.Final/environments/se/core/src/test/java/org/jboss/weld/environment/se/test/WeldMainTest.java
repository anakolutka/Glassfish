/**
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.weld.environment.se.test;

import org.jboss.weld.environment.se.ShutdownManager;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.environment.se.test.beans.CustomEvent;
import org.jboss.weld.environment.se.test.beans.InitObserverTestBean;
import org.jboss.weld.environment.se.test.beans.MainTestBean;
import org.jboss.weld.environment.se.test.beans.ObserverTestBean;
import org.jboss.weld.environment.se.test.beans.ParametersTestBean;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Royle
 */
public class WeldMainTest {

    /**
     * Test the alternate API for boting WeldContainer from an SE app.
     */
    @Test
    public void testInitialize() {

        WeldContainer weld = new Weld().initialize();

        MainTestBean mainTestBean = weld.instance().select(MainTestBean.class).get();
        assertNotNull(mainTestBean);

        ParametersTestBean paramsBean = mainTestBean.getParametersTestBean();
        assertNotNull(paramsBean);
        assertNotNull(paramsBean.getParameters());

        shutdownManager(weld);
    }

    /**
     * Test the firing of observers using the alternate API for boting WeldContainer from an SE app.
     */
    @Test
    public void testObservers() {
        InitObserverTestBean.reset();
        ObserverTestBean.reset();


        WeldContainer weld = new Weld().initialize();
        weld.event().select(CustomEvent.class).fire(new CustomEvent());

        assertTrue(ObserverTestBean.isBuiltInObserved());
        assertTrue(ObserverTestBean.isCustomObserved());

        // moved as per WELD-949
        assertTrue(ObserverTestBean.isInitObserved());
        assertTrue(InitObserverTestBean.isInitObserved());
    }

    private void shutdownManager(WeldContainer weld) {
        ShutdownManager shutdownManager = weld.instance().select(ShutdownManager.class).get();
        shutdownManager.shutdown();
    }
}
