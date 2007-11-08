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

package com.sun.ejb.base.container.util;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.enterprise.config.ConfigException;
import com.sun.enterprise.config.serverbeans.*;

import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.runtime.IASEjbExtraDescriptors;
import com.sun.enterprise.deployment.runtime.BeanCacheDescriptor;

import com.sun.enterprise.server.ServerContext;
import com.sun.enterprise.server.ApplicationServer;
import com.sun.enterprise.util.Utility;

import com.sun.logging.LogDomains;

/**
 * A util class to read the bean cache related entries from 
 *  domain.xml and sun-ejb-jar.xml
 *
 * @author Mahesh Kannan
 */
public class CacheProperties {

    protected static final Logger _logger =
        LogDomains.getLogger(LogDomains.EJB_LOGGER);
        
    private int maxCacheSize ;
    private int numberOfVictimsToSelect ;
    private int cacheIdleTimeoutInSeconds ;
    private int removalTimeoutInSeconds;

    private String victimSelectionPolicy;
        
    public CacheProperties(EjbDescriptor desc) {
        
        try {
            BeanCacheDescriptor beanCacheDes = null;
            Config cfg = null;
         
            IASEjbExtraDescriptors iased = desc.getIASEjbExtraDescriptors();
            if( iased != null) {
                beanCacheDes = iased.getBeanCache();
            }
        
            EjbContainer ejbContainer = null;
            ServerContext sc = ApplicationServer.getServerContext();
            cfg = ServerBeansFactory.getConfigBean(sc.getConfigContext());
            ejbContainer = cfg.getEjbContainer();

            loadProperties(ejbContainer, beanCacheDes);
            //container.setMonitorOn(ejbContainer.isMonitoringEnabled());
        }  catch (ConfigException ex) {
            _logger.log(Level.SEVERE, "", ex);
        }
    }   

    public int getMaxCacheSize() {
	return this.maxCacheSize;
    }

    public int getNumberOfVictimsToSelect() {
	return this.numberOfVictimsToSelect;
    }

    public int getCacheIdleTimeoutInSeconds() {
	return this.cacheIdleTimeoutInSeconds;
    }

    public int getRemovalTimeoutInSeconds() {
	return this.removalTimeoutInSeconds;
    }

    public String getVictimSelectionPolicy() {
	return this.victimSelectionPolicy;
    }
        
    public String toString() {
	StringBuffer sbuf = new StringBuffer();
	sbuf.append("maxSize: ").append(maxCacheSize)
	    .append("; victims: ").append(numberOfVictimsToSelect)
	    .append("; idleTimeout: ").append(cacheIdleTimeoutInSeconds)
	    .append("; removalTimeout: ").append(removalTimeoutInSeconds)
	    .append("; policy: ").append(victimSelectionPolicy);

	return sbuf.toString();
    }
        
    private void loadProperties(EjbContainer ejbContainer, 
	BeanCacheDescriptor beanCacheDes)
    {
	numberOfVictimsToSelect = 
	    new Integer(ejbContainer.getCacheResizeQuantity()).intValue();

	maxCacheSize =
	    new Integer(ejbContainer.getMaxCacheSize()).intValue();

	cacheIdleTimeoutInSeconds = new Integer(
		ejbContainer.getCacheIdleTimeoutInSeconds()).intValue();

	removalTimeoutInSeconds =
	    new Integer(ejbContainer.getRemovalTimeoutInSeconds()).intValue();

	victimSelectionPolicy = ejbContainer.getVictimSelectionPolicy();

	if (beanCacheDes != null) {
	    int temp = 0;
	    if ((temp =  beanCacheDes.getResizeQuantity()) != -1) {
		this.numberOfVictimsToSelect = temp;
	    }
	    if ((temp = beanCacheDes.getMaxCacheSize()) != -1) {
		this.maxCacheSize = temp;
	    }                              
	    if ((temp = beanCacheDes.getCacheIdleTimeoutInSeconds()) != -1){
		this.cacheIdleTimeoutInSeconds = temp;
	    }                
	    if ((temp = beanCacheDes.getRemovalTimeoutInSeconds()) != -1) {
		this.removalTimeoutInSeconds = temp;
	    }
	    if (( beanCacheDes.getVictimSelectionPolicy()) != null) {
		this.victimSelectionPolicy = 
		    beanCacheDes.getVictimSelectionPolicy();
	    }
	}
    }

}
   
