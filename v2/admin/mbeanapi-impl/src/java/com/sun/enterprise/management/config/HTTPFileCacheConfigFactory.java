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
 
/*
 * $Header: /cvs/glassfish/admin/mbeanapi-impl/src/java/com/sun/enterprise/management/config/HTTPFileCacheConfigFactory.java,v 1.4 2006/03/09 20:30:38 llc Exp $
 * $Revision: 1.4 $
 * $Date: 2006/03/09 20:30:38 $
 */
package com.sun.enterprise.management.config;

import java.util.Map;
import java.util.Set;
import java.util.Collections;

import javax.management.ObjectName;
import javax.management.AttributeList;

import com.sun.enterprise.management.support.oldconfig.OldHTTPServiceMBean;

import com.sun.appserv.management.util.misc.GSetUtil;
import com.sun.appserv.management.config.HTTPFileCacheConfigKeys;


final class HTTPFileCacheConfigFactory extends ConfigFactory
{
	private final OldHTTPServiceMBean	mOldHTTPServiceMBean;
	
		public
	HTTPFileCacheConfigFactory( final ConfigFactoryCallback	callbacks )
	{
		super( callbacks );
		
		mOldHTTPServiceMBean	=
			getOldConfigProxies().getOldHTTPServiceMBean( getConfigName() );
	}
	
	private final Set<String>	LEGAL_OPTIONAL_KEYS	= 
		GSetUtil.newUnmodifiableStringSet(
		HTTPFileCacheConfigKeys.GLOBALLY_ENABLED_KEY,
		HTTPFileCacheConfigKeys.FILE_CACHING_ENABLED_KEY,
		HTTPFileCacheConfigKeys.MAX_AGE_IN_SECONDS_KEY,
		HTTPFileCacheConfigKeys.MEDIUM_FILE_SIZE_LIMIT_IN_BYTES_KEY,
		HTTPFileCacheConfigKeys.MEDIUM_FILE_SPACE_IN_BYTES_KEY,
		HTTPFileCacheConfigKeys.SMALL_FILE_SIZE_LIMIT_IN_BYTES_KEY,
		HTTPFileCacheConfigKeys.SMALL_FILE_SPACE_IN_BYTES_KEY,
		HTTPFileCacheConfigKeys.FILE_TRANSMISSION_ENABLED_KEY,
		HTTPFileCacheConfigKeys.MAX_FILES_COUNT_KEY,
		HTTPFileCacheConfigKeys.HASH_INIT_SIZE_KEY );
	
	    protected Set<String>
	getLegalOptionalCreateKeys()
	{
		return( LEGAL_OPTIONAL_KEYS );
	}
	
		public ObjectName	
	create( final Map<String,String> optional )
	{
		final Map<String,String>	params	= initParams( optional );

		final ObjectName	amxName	= createChild( params );
		
		return( amxName );
	}

		protected void	
	internalRemove( final ObjectName objectName )
	{
		mOldHTTPServiceMBean.removeHttpFileCache();
	}

		protected ObjectName
	createOldChildConfig( AttributeList attrs )
	{
		return mOldHTTPServiceMBean.createHttpFileCache( attrs );
	}
}




