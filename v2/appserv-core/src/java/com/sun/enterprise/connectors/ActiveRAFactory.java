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

package com.sun.enterprise.connectors;
import java.util.logging.*;
import javax.resource.spi.ResourceAdapter;
import com.sun.logging.LogDomains;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.connectors.system.*;
import com.sun.enterprise.connectors.util.JmsRaUtil;


/**
 * Factory creating Active Resource adapters.
 *
 * @author  Binod P.G
 */
public class ActiveRAFactory {
    static Logger _logger = LogDomains.getLogger(LogDomains.RSR_LOGGER);     

    /**
     * Creates an active resource adapter.
     *
     * @param cd Deployment descriptor object for connectors.
     * @param moduleName Module name of the resource adapter.
     * @param loader Class Loader,
     * @param writeSunDescriptor Boolean indicating whether sundescriptor
     *        need to be written or not.
     * @return An instance of <code> ActiveResourceAdapter </code> object.
     * @throws ConnectorRuntimeException.
     */
    public static ActiveResourceAdapter createActiveResourceAdapter(
        ConnectorDescriptor cd, String moduleName, ClassLoader loader) 
        throws ConnectorRuntimeException{

        ActiveResourceAdapter activeResourceAdapter = null;
        int environment = ConnectorRuntime.getRuntime().getEnviron();
        ResourceAdapter ra = null;
        String raClass = cd.getResourceAdapterClass();

        try {

            // If raClass is available, load it...
            if (raClass != null && !raClass.equals("")) {
                if(environment == ConnectorRuntime.SERVER) {
                    ra = (ResourceAdapter) 
                          loader.loadClass(raClass).newInstance();
                } else {
                    ra = (ResourceAdapter)Class.forName(raClass).newInstance();
                }

            }

            /*
             * If any special handling is required for the system resource 
             * adapter, then ActiveResourceAdapter implementation for that
             * RA should implement additional functionality by extending
             * ActiveInboundResourceAdapter or ActiveOutboundResourceAdapter.
             *
             * For example ActiveJmsResourceAdapter extends 
             * ActiveInboundResourceAdapter.
             */
            if (moduleName.equals(ConnectorConstants.DEFAULT_JMS_ADAPTER)) {
                // Upgrade jms resource adapter, if necessary before starting 
                // the RA.
		try {
                	JmsRaUtil raUtil = new JmsRaUtil();
                	raUtil.upgradeIfNecessary();
		}
		catch (Throwable t) {
            	_logger.log(Level.FINE,"Cannot upgrade jmsra"+ t.getMessage());
		}

                activeResourceAdapter = new ActiveJmsResourceAdapter(
                                                 ra,cd,moduleName,loader);
            } else if (raClass.equals(""))  {
                activeResourceAdapter = new ActiveOutboundResourceAdapter(
                                 cd,moduleName,loader);
            } else {
                activeResourceAdapter = new ActiveInboundResourceAdapter(
                                                 ra,cd,moduleName,loader);
            }
	     
        } catch (ClassNotFoundException Ex) {
            ConnectorRuntimeException cre = new ConnectorRuntimeException(
                                             "Error in creating active RAR");
            cre.initCause(Ex);
            _logger.log(Level.SEVERE,"rardeployment.class_not_found",raClass);
            _logger.log(Level.SEVERE,"",cre);
            throw cre; 
        } catch (InstantiationException Ex) {
            ConnectorRuntimeException cre = new ConnectorRuntimeException(
                                             "Error in creating active RAR");
            cre.initCause(Ex);
            _logger.log(Level.SEVERE,"rardeployment.class_instantiation_error",
                                    raClass);
            _logger.log(Level.SEVERE,"",cre);
            throw cre; 
        } catch (IllegalAccessException Ex) {
            ConnectorRuntimeException cre = new ConnectorRuntimeException(
                                             "Error in creating active RAR");
            cre.initCause(Ex);
            _logger.log(Level.SEVERE,"rardeployment.illegalaccess_error",
                         raClass);
            _logger.log(Level.SEVERE,"",cre);
            throw cre; 
        } 

        return activeResourceAdapter;

    }


}
