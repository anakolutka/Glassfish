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
package com.sun.enterprise.naming.factory;

import com.sun.enterprise.ComponentInvocation;
import com.sun.enterprise.InvocationManager;
import com.sun.enterprise.Switch;
import com.sun.enterprise.connectors.*;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.resource.ResourceInstaller;
import com.sun.logging.LogDomains;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * An object factory to handle creation of administered object
 *
 * @author	Qingqing Ouyang
 *
 */
public class AdministeredObjectFactory implements ObjectFactory {

    private static Logger logger = 
    LogDomains.getLogger(LogDomains.RSR_LOGGER);

    //required by ObjectFactory
    public AdministeredObjectFactory() {}

    public Object getObjectInstance(Object obj, 
				    Name name, 
				    Context nameCtx,
				    Hashtable env) throws Exception {

	Reference ref = (Reference) obj;
	logger.fine("AdministeredObjectFactory: " + ref + 
		    " Name:" + name);	

        //String jndiName = (String) ref.get(0).getContent();
        AdministeredObjectResource aor = 
            (AdministeredObjectResource) ref.get(0).getContent();
        String jndiName = aor.getName();
        String moduleName = aor.getResourceAdapter();
	
	ConnectorRuntime runtime = ConnectorRuntime.getRuntime();
    
        //If call fom application client, start resource adapter lazily.
        //todo: Similar code in ConnectorObjectFactory - to refactor.
        if(runtime.getEnviron() == ConnectorRuntime.CLIENT) {
            ConnectorDescriptor connectorDescriptor = null; 
            try {        
                Context ic = new InitialContext();              
                String descriptorJNDIName = ConnectorAdminServiceUtils.
                    getReservePrefixedJNDINameForDescriptor(moduleName);
                connectorDescriptor = (ConnectorDescriptor)ic.lookup(descriptorJNDIName); 
            } catch(NamingException ne) {
                logger.log(Level.FINE, "Failed to look up ConnectorDescriptor " +
                                            "from JNDI", moduleName); 
                throw new ConnectorRuntimeException("Failed to look up " +
                                                "ConnectorDescriptor from JNDI");
            }
            runtime.createActiveResourceAdapter(connectorDescriptor, moduleName,
                            null);
        }
    
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (runtime.checkAccessibility(moduleName, loader) == false) {
	    throw new NamingException("Only the application that has the embedded resource" + 
	                               "adapter can access the resource adapter");
	}

	logger.fine("[AdministeredObjectFactory] ==> Got AdministeredObjectResource = " + aor);    

	return aor.createAdministeredObject(null);
    }

}
