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

/**
	Keys for use with {@link HTTPServiceConfig#createHTTPListenerConfig}.
 */
public final class HTTPListenerConfigKeys extends CommonConfigKeys
{
	private	HTTPListenerConfigKeys()	{}
	
	/** Key for use with HTTPServiceConfig#createHTTPListenerConfig() */
	public final static String	ADDRESS_KEY				= "Address";
	/** Key for use with HTTPServiceConfig#createHTTPListenerConfig() */
	public final static String	PORT_KEY				= "Port";
	/** Key for use with HTTPServiceConfig#createHTTPListenerConfig() */
	public final static String	ENABLED_KEY				= "Enabled";
	/** Key for use with HTTPServiceConfig#createHTTPListenerConfig() */
	public final static String	SECURITY_ENABLED_KEY	= "SecurityEnabled";
	/** Key for use with HTTPServiceConfig#createHTTPListenerConfig() */
	public final static String	DEFAULT_VIRTUAL_SERVER_KEY	= "DefaultVirtualServer";
	/** Key for use with HTTPServiceConfig#createHTTPListenerConfig() */
	public final static String	ACCEPTOR_THREADS_KEY	= "AcceptorThreads";
	/** Key for use with HTTPServiceConfig#createHTTPListenerConfig() (Boolean) */
	public final static String	XPOWERED_BY_KEY			= "XpoweredBy";
	/** Key for use with HTTPServiceConfig#createHTTPListenerConfig() */
	public final static String	SERVER_NAME_KEY			= "ServerName";
	/** Key for use with HTTPServiceConfig#createHTTPListenerConfig() */
	public final static String	EXTERNAL_PORT_KEY		= "ExternalPort";
	/**
		Key for use with HTTPServiceConfig#createHTTPListenerConfig()
		Legal values are as defined in {@link HTTPListenerConfigFamilyValues}.
	 */
	public final static String	FAMILY_KEY				= "Family";
	/** Key for use with HTTPServiceConfig#createHTTPListenerConfig() */
	public final static String	BLOCKING_ENABLED_KEY	= "BlockingEnabled";
	/** Key for use with HTTPServiceConfig#createHTTPListenerConfig() */
	public final static String	REDIRECT_PORT_KEY		= "RedirectPort";
	
	/**
		Key for use with HTTPServiceConfig#createHTTPListenerConfig()
		Value should be a Map containing items keyed by keys in {@link SSLConfigKeys}
	*/
	public final static String	SSL_PARAMS_KEY			= "SSL";
}
