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
package com.sun.appserv.management.client;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.io.IOException;

import javax.net.ssl.X509TrustManager;


/**
	This TrustManager applies no logic as to whether its peer
	certificate is trusted; it trusts all peers.
	This is a security risk, and should be used only for convenience
	in testing or where security is explicitly not an issue.
 */
public final class TrustAnyTrustManager implements X509TrustManager
{
	private static TrustAnyTrustManager		INSTANCE	= null;
	private static TrustAnyTrustManager[]	INSTANCE_ARRAY	= null;
	
	private	TrustAnyTrustManager()	{}
	
	/**
		Get an instance; only one is ever created.
	 */
		public static synchronized TrustAnyTrustManager
	getInstance()
	{
		if ( INSTANCE == null )
		{
			INSTANCE		= new TrustAnyTrustManager();
			INSTANCE_ARRAY	= new TrustAnyTrustManager[] { INSTANCE };
		}
		return( INSTANCE );
	}
	
	/**
		Calls getInstance() and returns an array containing it.
	 */
		public static TrustAnyTrustManager[]
	getInstanceArray()
	{
		getInstance();
		return( INSTANCE_ARRAY );
	}
	
	/**
	    WARNING: does nothing; all clients are always trusted.
	 */
		public void
	checkClientTrusted( X509Certificate[] chain, String authType)
		throws CertificateException
	{
		//trace( "checkClientTrusted, authType = " + authType +
		//	", " + SmartStringifier.toString( chain ) );
	}
	
	/**
	    WARNING: does nothing; all servers are always trusted.
	 */
		public void
	checkServerTrusted( X509Certificate[] chain, String authType)
		throws CertificateException
	{
		//trace( "checkServerTrusted, authType = " + authType +
		//	", " + SmartStringifier.toString( chain ) );
	}
	
	/**
	    @return an empty array.
	 */
		public X509Certificate[]
	getAcceptedIssuers()
	{
		return( new X509Certificate[ 0 ] );
	}
	
		public String
	toString()
	{
		return( "TrustAnyTrustManager--trusts all certificates with no check whatsoever" );
	}
}