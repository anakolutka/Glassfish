/*
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
*
* The contents of this file are subject to the terms of either the GNU
* General Public License Version 2 only ("GPL") or the Common Development
* and Distribution License("CDDL") (collectively, the "License").  You
* may not use this file except in compliance with the License. You can obtain
* a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
* or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
* language governing permissions and limitations under the License.
*
* When distributing the software, include this License Header Notice in each
* file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
* Sun designates this particular file as subject to the "Classpath" exception
* as provided by Sun in the GPL Version 2 section of the License file that
* accompanied this code.  If applicable, add the following below the License
* Header, with the fields enclosed by brackets [] replaced by your own
* identifying information: "Portions Copyrighted [year]
* [name of copyright owner]"
*
* Contributor(s):
*
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
package com.sun.enterprise.naming.impl;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PostConstruct;
import org.glassfish.internal.api.Init;

import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.naming.NamingException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Field;

/**
 * Init service to register our InitialContextFactoryBuilder with the JDK
 *
 * @author Jerome Dochez
 */
@Service
public class ServicesHookup implements Init, PostConstruct {

    @Inject
    InitialContextFactoryBuilder myBuilder;

    @Inject
    Logger logger;

    public void postConstruct() {
        setGFInitialContextFactoryBuilder();
    }


    private void setGFInitialContextFactoryBuilder() {
        try {
            Field f = NamingManager.class.getDeclaredField("initctx_factory_builder");
            final InitialContextFactoryBuilder bldr = myBuilder;

            final Field finalF = f;
            java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedExceptionAction() {
                        public java.lang.Object run() throws Exception {
                            if (!finalF.isAccessible()) {
                                finalF.setAccessible(true);
                            }
                            finalF.set(null, bldr);
                            logger.log(Level.INFO, "**GFNaming==> InitialContextFactoryBuilder set");
                            return null;
                        }
                    });
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Couldn't set NamingManager.setInitialContextFactoryBuilder by reflection", ex);
            try {
                NamingManager.setInitialContextFactoryBuilder(myBuilder);
            } catch (NamingException nEx) {
                logger.log(Level.SEVERE, "Cannot install initial context factory builder", nEx);
            }
        }
    }


}
