/*
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
 
/*
 * $Header: /m/jws/jmxcmd/src/com/sun/cli/jmxcmd/support/StringifierRegistryIniter.java,v 1.6 2004/03/13 01:47:23 llc Exp $
 * $Revision: 1.6 $
 * $Date: 2004/03/13 01:47:23 $
 */
package com.sun.cli.jmxcmd.support;


import com.sun.cli.jcmd.util.stringifier.StringifierRegistry;

/**
	Registers all jmxcmd Stringifiers
 */
public class StringifierRegistryIniter
	extends com.sun.cli.jcmd.util.stringifier.StringifierRegistryIniterImpl
	
{
		public
	StringifierRegistryIniter( StringifierRegistry registry )
	{
		super( registry );
		
		new com.sun.cli.jcmd.JCmdStringifierRegistryIniter( registry );
		new com.sun.cli.jmxcmd.util.jmx.stringifier.StringifierRegistryIniter( registry );
		
		add( ResultsForGetSet.class, new ResultsForGetSetStringifier( ) );
		add( InspectResult.class, new InspectResultStringifier( ) );
		add( InvokeResult.class, new InvokeResultStringifier( ) );
		
	}
}



