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
package com.sun.appserv.management.config;

import java.util.Map;

import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.base.Container;
import com.sun.appserv.management.base.Singleton;


/**
	 Configuration for the &lt;jms-service&gt; element.
*/

public interface JMSServiceConfig
	extends ConfigElement, Container, PropertiesAccess, Singleton
{
/** The j2eeType as returned by {@link com.sun.appserv.management.base.AMX#getJ2EEType}. */
	public static final String	J2EE_TYPE	= XTypes.JMS_SERVICE_CONFIG;

	public String	getAddressListBehavior();
	public void	    setAddressListBehavior( final String value );

	public String	getAddressListIterations();
	public void	    setAddressListIterations( final String value );

	public String	getDefaultJMSHost();
	public void	    setDefaultJMSHost( final String value );

	public String	getInitTimeoutInSeconds();
	public void	    setInitTimeoutInSeconds( final String value );

	public String	getMQScheme();
	public void	    setMQScheme( final String value );

	public String	getMQService();
	public void	    setMQService( final String value );

	public String	getReconnectAttempts();
	public void	    setReconnectAttempts( final String value );

	public boolean	getReconnectEnabled();
	public void	    setReconnectEnabled( final boolean value );

	public String	getReconnectIntervalInSeconds();
	public void	    setReconnectIntervalInSeconds( final String value );

	public String	getStartArgs();
	public void	    setStartArgs( final String value );

	public String	getType();
	public void	    setType( final String value );
	
	/**
		Calls Container.getContaineeMap( XTypes.JMS_HOST_CONFIG ).
		@return Map of JMSHostConfig MBean proxies, keyed by name.
		@see com.sun.appserv.management.base.Container#getContaineeMap
	 */
	public Map<String,JMSHostConfig>			getJMSHostConfigMap();
	/**
		Creates a new jms-host element.

		@param name The name (id) of the jms-host.
		@param optional A map of optional params keyed on the attribute keys
		defined here. (eg:- HOST_KEY).
		@return A proxy to the JMSHostConfig MBean.
		@see JMSHostConfigKeys
	 */
	public JMSHostConfig	createJMSHostConfig( String name, Map<String,String> optional );

	/**
		Removes a jms-host element.

		@param name The name (id) of the jms-host.
	 */
	public void			removeJMSHostConfig( String name );
}






