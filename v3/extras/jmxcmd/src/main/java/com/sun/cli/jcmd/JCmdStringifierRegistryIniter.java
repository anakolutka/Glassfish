/*
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
 
/*
 * $Header: /m/jws/jmxcmd/src/com/sun/cli/jcmd/JCmdStringifierRegistryIniter.java,v 1.3 2004/03/13 01:47:19 llc Exp $
 * $Revision: 1.3 $
 * $Date: 2004/03/13 01:47:19 $
 */
 
package com.sun.cli.jcmd;

import com.sun.cli.jcmd.util.stringifier.StringifierRegistryIniter;
import com.sun.cli.jcmd.util.stringifier.StringifierRegistryIniterImpl;
import com.sun.cli.jcmd.util.stringifier.StringifierRegistry;

/**
	Registers all JCmd-specific Stringifiers.
 */
public class JCmdStringifierRegistryIniter extends StringifierRegistryIniterImpl
{
		public
	JCmdStringifierRegistryIniter( StringifierRegistry registry )
	{
		super( registry );
		
		// no JCmd-specific initers yet
	}
	
}



