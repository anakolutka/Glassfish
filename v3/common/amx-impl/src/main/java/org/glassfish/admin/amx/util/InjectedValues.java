/*
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
package org.glassfish.admin.amx.util;

import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Singleton;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.PreDestroy;

import org.glassfish.api.Async;

import javax.management.MBeanServer;
import com.sun.enterprise.v3.server.ServerEnvironmentImpl;


/**
    Utility class that gets various useful values injected into it for use
    by other AMX facilities which don't have injection available to themselves.
 */
@Service
@Async
//@Scoped(Singleton.class)
public final class InjectedValues
    implements  PostConstruct, PreDestroy
{
    private static void debug( final String s ) { System.out.println(s); }
    
    @Inject
    private MBeanServer mMBeanServer;
    public MBeanServer getMBeanServer() { return mMBeanServer; }
    
    @Inject
    private ServerEnvironmentImpl mServerEnvironment;
    public ServerEnvironmentImpl getServerEnvironment() { return mServerEnvironment; }
    
    private static volatile InjectedValues INSTANCE = null;
    
    public InjectedValues()
    {
        debug( "InjectedValues.InjectedValues()" );
    }
    
    public static InjectedValues getInstance() { return INSTANCE; }
    
    public void postConstruct()
    {
        debug( "InjectedValues.postConstruct");
        if ( INSTANCE != null && this != INSTANCE )
        {
            debug( "InjectedValues.postConstruct(): WARNING: more than one instance has been created" ); 
        }
        INSTANCE = this;
    }

    public void preDestroy() {
        debug( "InjectedValues.preDestroy");
    }

   // public Startup.Lifecycle getLifecycle() { return Startup.Lifecycle.SERVER; }
}













