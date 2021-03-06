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
 * $Header: /cvs/glassfish/admin-cli/cli-api/src/java/com/sun/cli/jmx/cmd/InspectCmd.java,v 1.3 2005/12/25 03:45:36 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 03:45:36 $
 */
 
package com.sun.cli.jmx.cmd;


import com.sun.cli.jmx.support.InspectRequest;
import com.sun.cli.jmx.support.InspectResult;
import com.sun.cli.jmx.support.CLISupportMBeanProxy;
import com.sun.cli.util.stringifier.*;

public class InspectCmd extends JMXCmd
{
		public
	InspectCmd( final CmdEnv env )
	{
		super( env );
		
		mStringifier	= new SmartStringifier( "\n\n", false );
	}
	
		public String
	getUsage()
	{
		return( CmdStrings.INSPECT_HELP.toString() );
	}
	
		int
	getNumRequiredOperands()
	{
		// require 1, by default
		return( 0 );
	}
	
	
	static private final String	OPTIONS_INFO	=
		"all summary constructors attributes,1 operations,1 notifications,1 nodescription";
		
	
		ArgHelper.OptionsInfo
	getOptionInfo()
		throws ArgHelper.IllegalOptionException
	{
		return( new ArgHelperOptionsInfo( OPTIONS_INFO ) );
	}
	
	
		void
	handle_inspect( String [] targets, final InspectRequest request)
		throws Exception
	{
		final InspectResult []	results	= getProxy().mbeanInspect( request, targets );
		
		if ( results.length == 0 )
		{
			println( "<nothing inspected>" );
		}
		else
		{
			final String	msg	= ArrayStringifier.stringify( results, "\n\n");
			println( msg );
		}
	}
	
		void
	handle_inspect( final String [] targets )
		throws Exception
	{
		final InspectRequest	request	= new InspectRequest( true );
		
		request.includeDescription	= ! getBoolean( "noDescription", Boolean.TRUE ).booleanValue();
			
		if ( getBoolean( "all", Boolean.FALSE ).booleanValue() )
		{
			// should already be setup to get everything
		}
		else
		{
			request.includeSummary		= getBoolean( "summary", Boolean.TRUE ).booleanValue();
			request.constructors		= getBoolean( "constructors", Boolean.FALSE ).booleanValue();
		
			request.attrs			= getString( "attributes", null );
			request.operations		= getString( "operations", null );
			request.notifications	= getString( "notifications", null );
		}
		
		handle_inspect( targets, request );
	}
	
		void
	handle_ops( final String [] targets )
		throws Exception
	{
		final InspectRequest	request	= new InspectRequest( false );
		request.operations	= "*";
		
		handle_inspect( targets, request );
	}
	
		void
	handle_attrs( final String [] targets )
		throws Exception
	{
		final InspectRequest	request	= new InspectRequest( false );
		request.attrs	= "*";
		
		handle_inspect( targets, request );
	}
	
	
		public static String []
	getNames( )
	{
		return( new String [] { "inspect", "i", "ops", "attrs" } );
	}
	
		void
	executeInternal()
		throws Exception
	{
		final String	cmd	= getCmdNameAsInvoked();
		final String []	targets	= getTargets();
		
		if ( targets == null )
		{
			printError( "No targets have been specified" );
			return;
		}
		
		establishProxy();
		if ( cmd.equalsIgnoreCase( "inspect" ) )
		{
			handle_inspect( targets );
		}
		else if ( cmd.equalsIgnoreCase( "ops" ) )
		{
			handle_ops( targets );
		}
		else if ( cmd.equalsIgnoreCase( "attrs" ) )
		{
			handle_attrs( targets );
		}
	}
}

