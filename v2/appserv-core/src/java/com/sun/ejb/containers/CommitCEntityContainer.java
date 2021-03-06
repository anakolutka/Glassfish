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
package com.sun.ejb.containers;

import java.lang.reflect.Method;
import java.io.*;
import java.rmi.RemoteException;

import javax.ejb.*;
import javax.transaction.*;

import javax.rmi.PortableRemoteObject;

import java.util.*;

import com.sun.ejb.*;
import com.sun.ejb.portable.ObjrefEnumeration;
import com.sun.enterprise.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.log.Log;
import com.sun.enterprise.appverification.factory.AppVerification;

 /*
  * This class implements the Commit-Option C as described in
  * the EJB Specification.
  *
  * The CommitOptionC Container extends Entity Container and
  * hence all the life cycle management is still in Entitycontainer
  *
  * @author Mahesh Kannan
  */

public class CommitCEntityContainer
    extends EntityContainer
{
    /**
     * This constructor is called from the JarManager when a Jar is deployed.
     * @exception Exception on error
     */
    protected CommitCEntityContainer(EjbDescriptor desc, ClassLoader loader)
        throws Exception
    {
        super(desc, loader);
    }
    
    protected EntityContextImpl getReadyEJB(Invocation inv) {
        Object primaryKey = inv.ejbObject.getKey();
        return activateEJBFromPool(primaryKey, inv);
    }
    
    protected void createReadyStore(int cacheSize, int numberOfVictimsToSelect,
            float loadFactor, long idleTimeout)
    {
        readyStore = null;
    }
    
    protected void createEJBObjectStores(int cacheSize,
            int numberOfVictimsToSelect, long idleTimeout) throws Exception
    {
        super.defaultCacheEJBO = false;
        super.createEJBObjectStores(cacheSize, numberOfVictimsToSelect, idleTimeout);
    }
    
    // called from releaseContext, afterCompletion
    protected void addReadyEJB(EntityContextImpl context) {
        passivateAndPoolEJB(context);
    }
    
    protected void destroyReadyStoreOnUndeploy() {
        readyStore = null;
    }
    
    protected void removeContextFromReadyStore(Object primaryKey,
            EntityContextImpl context)
    {
        // There is nothing to remove as we don't have a readyStore
    }
    
}

