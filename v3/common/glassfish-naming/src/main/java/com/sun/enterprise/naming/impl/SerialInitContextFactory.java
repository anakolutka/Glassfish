/*
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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

import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.api.naming.NamingObjectsProvider;
import org.jvnet.hk2.component.Habitat;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements the JNDI SPI InitialContextFactory interface used to create
 * the InitialContext objects. It creates an instance of the serial context.
 */

public class SerialInitContextFactory implements InitialContextFactory {


    private Hashtable defaultEnv;
    private final Habitat habitat;

    private boolean useS1ASCtxFactory;

    private static boolean initialized = false;

    /**
     * Default constructor. Creates an ORB if one is not already created.
     */
    public SerialInitContextFactory(Hashtable environemnt, Habitat habitat) {

        this.defaultEnv = environemnt;
        this.habitat = habitat;

    }
    
    /**
     * Create the InitialContext object.
     */
    public Context getInitialContext(Hashtable env) throws NamingException {

        //Another Big TODO Sync with useS1ASCtxFactory

        // this lock needs to be reentrant as the lookup of the NamingObjectsProvider
        // will most likely trigger access to the naming manager and the serial init context.
        synchronized(SerialInitContextFactory.class) {
            if (!initialized) {
                // this must be set first as we don't want to get into infinite loop while
                // doing the first initialization
                initialized=true;

                // this should force the initialization of the resources providers
                if (habitat!=null) {
                    for (NamingObjectsProvider provider : habitat.getAllByContract(NamingObjectsProvider.class)) {
                        System.out.println("Provider " + provider);
                    }
                }
            }
        }

        if (env != null) {
            return new SerialContext(env, habitat);
        } else {
            return new SerialContext(defaultEnv, habitat);
        }
    }
}
