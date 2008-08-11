/*
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
 
/*
 * $Header: /m/jws/jmxcmd/src/com/sun/cli/jcmd/framework/ConfigureCmd.java,v 1.13 2005/05/20 00:50:52 llc Exp $
 * $Revision: 1.13 $
 * $Date: 2005/05/20 00:50:52 $
 */
 
package com.sun.cli.jcmd.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;

import com.sun.cli.jcmd.util.stringifier.IteratorStringifier;

import com.sun.cli.jcmd.util.misc.ClassUtil;
import com.sun.cli.jcmd.util.misc.StringifiedList;
import com.sun.cli.jcmd.util.cmd.IllegalOptionException;
import com.sun.cli.jcmd.util.cmd.ArgHelper;
import com.sun.cli.jcmd.util.cmd.IllegalOptionException;
import com.sun.cli.jcmd.util.cmd.OptionsInfo;
import com.sun.cli.jcmd.util.cmd.OptionsInfoImpl;
import com.sun.cli.jcmd.util.cmd.OperandsInfoImpl;
import com.sun.cli.jcmd.util.cmd.CmdInfos;
import com.sun.cli.jcmd.util.cmd.CmdInfo;
import com.sun.cli.jcmd.util.cmd.CmdInfoImpl;
import com.sun.cli.jcmd.util.stringifier.ArrayStringifier;
import com.sun.cli.jcmd.framework.CmdBase;
import com.sun.cli.jcmd.framework.CmdEnv;
import com.sun.cli.jcmd.framework.CmdHelp;
import com.sun.cli.jcmd.framework.CmdHelpImpl;
import com.sun.cli.jcmd.framework.CmdFactoryIniter;
import com.sun.cli.jcmd.framework.CmdSource;


/**
	Configures the CLI framework.
 */
public class ConfigureCmd extends CmdBase
{
		public
	ConfigureCmd( final CmdEnv env )
	{
		super( env );
	}
	
		public CmdHelp
	getHelp()
	{
		return( new ConfigureCmdHelp() );
	}
	
	public static final class ConfigureCmdHelp extends CmdHelpImpl
	{
			public
		ConfigureCmdHelp()	{ super( getCmdInfos() ); }
		
		
		static private final String CLASSNAME_SUFFIX	= " <classname>\n";
		
		static final String	SYNOPSIS	= "configure this CLI";
			
		static final String	CONFIGURE_TEXT		=
	"Manages CmdSource and classpaths for the CLI.\n\n" +
	"";

		public String	getName()		{	return( CONFIGURE_NAME ); }
		public String	getSynopsis()	{	return( formSynopsis( SYNOPSIS ) ); }
		public String	getText()		{	return( CONFIGURE_TEXT ); }
	}

	
	static final String	CONFIGURE_NAME			= "configure";
	static final String	LIST_CONFIG_NAME		= "list-config";
	static final String	ADD_CMD_SOURCE_NAME		= "add-cmd-source";
	static final String	ADD_CMD_NAME			= "add-cmd";
	static final String	REMOVE_CMD_SOURCE_NAME	= "remove-cmd-source";
	static final String	ADD_CLASSPATH_NAME		= "add-classpath";
	static final String	REMOVE_CLASSPATH_NAME	= "remove-classpath";
	
	private static final OperandsInfoImpl	CLASSNAME_OPERAND_INFO = 
		new OperandsInfoImpl( "classname", 1, 1 );
	
	private static final CmdInfo	LIST_CONFIG_INFO	= new CmdInfoImpl( LIST_CONFIG_NAME );
	private static final CmdInfo	ADD_CMD_SOURCE_INFO	= new CmdInfoImpl( ADD_CMD_SOURCE_NAME, CLASSNAME_OPERAND_INFO );
	private static final CmdInfo	ADD_CMD_INFO		= new CmdInfoImpl( ADD_CMD_NAME, CLASSNAME_OPERAND_INFO );
	private static final CmdInfo	REMOVE_CMD_SOURCE_INFO	= new CmdInfoImpl( REMOVE_CMD_SOURCE_NAME, CLASSNAME_OPERAND_INFO );
	private static final CmdInfo	ADD_CLASSPATH_INFO		= new CmdInfoImpl( ADD_CLASSPATH_NAME, CLASSNAME_OPERAND_INFO );
	private static final CmdInfo	REMOVE_CLASSPATH_INFO	= new CmdInfoImpl( REMOVE_CLASSPATH_NAME,CLASSNAME_OPERAND_INFO );
	
		public static CmdInfos
	getCmdInfos( )
	{
		return( new CmdInfos( new CmdInfo[]
			{
				LIST_CONFIG_INFO,
				ADD_CMD_SOURCE_INFO,
				ADD_CMD_INFO,
				REMOVE_CMD_SOURCE_INFO,
		 		ADD_CLASSPATH_INFO,
		 		REMOVE_CLASSPATH_INFO
		 	} )
		);
	}
	

		StringifiedList
	createCmdSourceList()
	{
		return( new StringifiedList( (String)envGet( CmdEnvKeys.COMMAND_SOURCES ) ) );
	}
	
		boolean
	isLegalClassname( String name )
	{
		// should test this properly, but at least make sure it doesn't have our delim
		return( name.indexOf( StringifiedList.DEFAULT_DELIM ) < 0 );
	}
	
		protected void
	displayCmds(  )
	{
		println( "Commands configured: " );
		final String	list	= IteratorStringifier.stringify( createCmdSourceList().iterator(), "\n" );
		
		println( (list.length() == 0) ? "<none>" : list );
	}
	
		protected void
	displayClasspath(  )
	{
		println( "Classpath: " + getClasspathList().toString() );
	}
	
	
	
		protected void
	displayConfig(  )
	{
		displayCmds();
		println( "" );
		displayClasspath();
	}
	
		CmdSource
	createCmdSource( String classname )
		throws ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		return( (CmdSource)ClassUtil.getClassFromName( classname ).newInstance() );
	}
	
		void
	loadCmdSource( CmdSource cmdSource )
		throws Exception
	{
		final CmdFactoryIniter	initer 		= new CmdFactoryIniter( getCmdFactory() );
		final String[][]		namesList	= initer.addMappings( cmdSource );
		
		final Class[]	classes	= cmdSource.getClasses();
		
		// report what we've added
		for( int i = 0; i < namesList.length; ++i )
		{
			final String[]	names	= namesList[ i ];
			
			println( "Added: " + classes[ i ].getName() + ", names = " +
				ArrayStringifier.stringify( names, ", " ) );
		}
	}
	
	private static final class CmdSourceCmd implements CmdSource
	{
		private final Class[]	mCmdClasses;
		
		public	CmdSourceCmd( Class[] cmdClasses )	{ mCmdClasses	= cmdClasses; }
		public	CmdSourceCmd( Class cmdClass )		{ this( new Class[] { cmdClass } ); }
		
		public Class[]	getClasses( )	{ return mCmdClasses; }
	}
	
		void
	addCmd( String commandClassname )
		throws Exception
	{
		final CmdSource	temp	= new CmdSourceCmd( ClassUtil.getClassFromName( commandClassname ));
		loadCmdSource( temp );
		println( "Cmd " + commandClassname + " loaded." );
	}
	
	
		void
	addCmdSource( String commandSourceClassname )
		throws Exception
	{
		final StringifiedList	list	= createCmdSourceList();
		
		if ( list.exists( commandSourceClassname ) )
		{
			println( "CmdSource already in list: " + commandSourceClassname );
		}
		else if ( ! isLegalClassname( commandSourceClassname ) )
		{
			printError( "Illegal classname: " + commandSourceClassname );
		}
		else
		{
			final CmdSource	cmdSource	= createCmdSource( commandSourceClassname );
		
			list.append( commandSourceClassname );
			envPut( CmdEnvKeys.COMMAND_SOURCES, list.toString(), true );
			
			loadCmdSource( cmdSource );
		}
	}
	
	
		void
	removeCmdSource()
		throws Exception
	{
		requireNumOperands( 1, "A classname must be specified");
		
		final String	classname	= getOperands()[ 0 ];
		final StringifiedList	list	= createCmdSourceList();
		
		if ( ! list.exists( classname ) )
		{
			println( "CmdSource not registered: " + classname );
		}
		else
		{
			// remove it from the preferences
			list.remove( classname );
			envPut( CmdEnvKeys.COMMAND_SOURCES, list.toString(), true );
			
			// remove it from the factory
			final CmdFactoryIniter	initer = new CmdFactoryIniter( getCmdFactory() );
			initer.removeMappings(createCmdSource( classname ) );

		}
	}
	
	private final static char	CLASSPATH_SEPARATOR	= ';';
		private String[]
	getClasspathParts()
	{
		final String	classpath	= getOperands()[ 0 ];
		final String[]	parts	= classpath.trim().split( "" + CLASSPATH_SEPARATOR );
		
		return( parts );
	}
	
		FrameworkClassLoader
	getClassLoader()
	{
		return( FrameworkClassLoader.getInstance() );
	}
	
		StringifiedList
	getClasspathList()
	{
		final String	s	= (String)envGet( CmdEnvKeys.ADDITIONAL_CLASSPATH );
		
		return( new StringifiedList( s, CLASSPATH_SEPARATOR ) );
	}
	
		private void
	addClasspath()
		throws WrongNumberOfOperandsException, MalformedURLException
	{
		final String[]	parts	= getClasspathParts();
		
		final StringifiedList	list	= getClasspathList();
		
		for( int i = 0; i < parts.length; ++i )
		{
			final String	part	= parts[ i ];
			
			final URL	url	= new File( part ).toURL();
			
			getClassLoader().addURL( url );
			
			list.append( part );
		}
		
		envPut( CmdEnvKeys.ADDITIONAL_CLASSPATH, list.toString(), true );
	}
	
		private void
	removeClasspath()
		throws WrongNumberOfOperandsException
	{
		requireNumOperands( 1, "A classpath must be specified");
		final String[]	parts	= getClasspathParts();
		
		final StringifiedList	list	= getClasspathList();
		
		for( int i = 0; i < parts.length; ++i )
		{
			final String	part	= parts[ i ];
			
			list.remove( part );
			println( "Removed: " + part );
		}
		
		envPut( CmdEnvKeys.ADDITIONAL_CLASSPATH, list.toString(), true );
	
		println( "Restart " + getCLIName() + " for the change to take effect." );
	}
	
		public static void
	setupFrameworkClassLoader( CmdEnv env )
	{
		final String			s		= (String)env.get( CmdEnvKeys.ADDITIONAL_CLASSPATH );
		final StringifiedList	list	= new StringifiedList( s, CLASSPATH_SEPARATOR );
		
		final CmdOutput		cmdOutput	= (CmdOutput)env.get( CmdEnvKeys.CMD_OUTPUT );
		
		final FrameworkClassLoader	loader	= FrameworkClassLoader.getInstance();
		if ( loader != null )
		{
			final String[]	parts	= list.toArray();
			for( int i = 0; i < parts.length; ++i )
			{
				try
				{
					final File	f	= new File( parts[ i ] );
					if ( f.exists() )
					{
						final URL	url	= f.toURL();
						
						loader.addURL( url );
					}
					else
					{
						cmdOutput.printError( "WARNING: classpath element does not exist: \"" + f.toString() + "\"" );
					}
				}
				catch( MalformedURLException e )
				{
					System.err.println( "ConfigureCmd.setupFrameworkClassLoader: can't add path: " + parts[ i ] );
				}
			}
		}
		else
		{
			//System.err.println( "ConfigureCmd.setupFrameworkClassLoader: can't get FrameworkClassLoader" );
		}
	}
	
	
		protected void
	executeInternal()
		throws Exception
	{
		final String	cmdName		= getSubCmdNameAsInvoked();
		
		if ( cmdName.equals( LIST_CONFIG_NAME ) )
		{
			displayConfig();
		}
		else if ( cmdName.equals( ADD_CMD_SOURCE_NAME ) )
		{
			addCmdSource( getOperands()[ 0 ] );
		}
		else if ( cmdName.equals( ADD_CMD_NAME ) )
		{
			addCmd( getOperands()[ 0 ] );
		}
		else if ( cmdName.equals( REMOVE_CMD_SOURCE_NAME ) )
		{
			removeCmdSource( );
		}
		else if ( cmdName.equals( ADD_CLASSPATH_NAME ) )
		{
			addClasspath( );
		}
		else if ( cmdName.equals( REMOVE_CLASSPATH_NAME ) )
		{
			removeClasspath( );
		}
		else
		{
			throw new IllegalUsageException( cmdName );
		}
	}
}






