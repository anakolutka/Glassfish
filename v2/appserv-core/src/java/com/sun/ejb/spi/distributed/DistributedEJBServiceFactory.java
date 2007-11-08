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

package com.sun.ejb.spi.distributed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.logging.LogDomains;

import java.util.concurrent.ConcurrentHashMap;

public class DistributedEJBServiceFactory
    implements DistributedEJBService 
{

    private static final Logger _logger = 
        LogDomains.getLogger(LogDomains.EJB_LOGGER);
    
    protected static DistributedEJBService distributedEJBService = null;
    protected static DistributedEJBTimerService distributedEJBTimerService = null;

    private static DistributedReadOnlyBeanService _distributedReadOnlyBeanService
        = new DistributedReadOnlyBeanServiceImpl();
    public static DistributedEJBService getDistributedEJBService() {
        if(distributedEJBService == null) {
            distributedEJBService = new DistributedEJBServiceFactory();
        } 

        return distributedEJBService;
    }

    protected DistributedEJBServiceFactory() {
        distributedEJBService = this;
    }


    public static void setDistributedEJBTimerService( 
        DistributedEJBTimerService distribEJBTimerService ) {
        
        getDistributedEJBService();

        //The distributedEJBTimerService is the EJBTimerService that should
        //be assigned as part of the server startup. Also this code makes sense
        //in the appserv-core part of the land. But since this method is on the
        //interface even the appserv-core-ee code might call it. Need to safeguard
        //against this possibility.
        if( null == distributedEJBTimerService ) {
            distributedEJBTimerService = distribEJBTimerService;
        }
    }

    /**
     *--------------------------------------------------------------
     * Methods to be implemented for DistributedEJBService
     *--------------------------------------------------------------
     */
    public int migrateTimers( String serverId ) {
        int result = 0;
        if (distributedEJBTimerService != null) {
            result = distributedEJBTimerService.migrateTimers( serverId );
        } else {
            //throw new IllegalStateException("EJB Timer service is null. "
                    //+ "Cannot migrate timers for: " + serverId);
        }
        
        return result;
    }

    public String[] listTimers( String[] serverIds ) {
        String[] result = new String[serverIds.length];
        if (distributedEJBTimerService != null) {
            result = distributedEJBTimerService.listTimers( serverIds );
        } else {
            //FIXME: Should throw IllegalStateException
            for (int i=0; i<serverIds.length; i++) {
                result[i] = "0";
            }
            //throw new com.sun.enterprise.admin.common.exception.AFException("EJB Timer service is null. "
                    //+ "Cannot list timers.");
        }
        
        return result;
    }

    public void setPerformDBReadBeforeTimeout( boolean defaultDBReadValue ) {
        if( null != distributedEJBTimerService ) {
            distributedEJBTimerService.setPerformDBReadBeforeTimeout( 
                                           defaultDBReadValue );
        } else {
            // Should we ensure that the EJBTimerService can not be null
            // in the case of SE/EE
        }
    }

    public DistributedReadOnlyBeanService getDistributedReadOnlyBeanService() {
        return _distributedReadOnlyBeanService;
    }

} //DistributedEJBServiceFactory.java

