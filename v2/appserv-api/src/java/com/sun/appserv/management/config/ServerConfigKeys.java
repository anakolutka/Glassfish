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
	Keys for use with {@link DomainConfig#createStandaloneServerConfig} and 
	{@link DomainConfig#createClusteredServerConfig}
 */
public final class ServerConfigKeys
{
	private	ServerConfigKeys()	{}
	
    /** Key for the system property that would be used to assign port value for the listener named http-listener-1**/
        public static final String  HTTP_LISTENER_1_PORT_KEY = PropertiesAccess.PROPERTY_PREFIX +"HTTP_LISTENER_PORT";
        
    /** Key for the system property that would be used to assign port value for the listener named http-listener-2**/
        public static final String  HTTP_LISTENER_2_PORT_KEY = PropertiesAccess.PROPERTY_PREFIX +"HTTP_SSL_LISTENER_PORT";
        
    /** Key for the system property that would be used to assign port value for the iiop listener named orb-listener-1**/
        public static final String  ORB_LISTENER_1_PORT_KEY = PropertiesAccess.PROPERTY_PREFIX +"IIOP_LISTENER_PORT";
        
    /** Key for the system property that would be used to assign port value for the iiop listener named admin-listener-port **/
        public static final String  ADMIN_LISTENER_PORT_KEY = PropertiesAccess.PROPERTY_PREFIX +"HTTP_ADMIN_LISTENER_PORT";
        
    /** Key for the system property that would be used to assign port value for the secure iiop listener named SSL**/
        public static final String  SSL_PORT_KEY = PropertiesAccess.PROPERTY_PREFIX +"IIOP_SSL_LISTENER_PORT";
        
    /** Key for the system property that would be used to assign port value for the secure client-auth
     * supporting SSL enabled iiop listenerlistener named SSL_MUTUALAUTH**/
        public static final String  SSL_MUTUALAUTH_PORT_KEY = PropertiesAccess.PROPERTY_PREFIX +"IIOP_SSL_MUTUALAUTH_PORT";
        
    /** Key for the system property that would be used to assign port value for the secure iiop listener named JMX_SYSTEM_CONNECTOR**/
        public static final String  JMX_SYSTEM_CONNECTOR_PORT_KEY= PropertiesAccess.PROPERTY_PREFIX +"JMX_SYSTEM_CONNECTOR_PORT";
        
    /** Key for the system property that would be used to assign port value for the secure iiop listener named JMX_SYSTEM_CONNECTOR**/
        public static final String  JMS_PROVIDER_PORT_KEY   = PropertiesAccess.PROPERTY_PREFIX +"JMS_PROVIDER_PORT";
}
