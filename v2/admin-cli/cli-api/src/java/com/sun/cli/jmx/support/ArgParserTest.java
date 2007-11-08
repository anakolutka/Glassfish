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
 * $Header: /cvs/glassfish/admin-cli/cli-api/src/java/com/sun/cli/jmx/support/ArgParserTest.java,v 1.3 2005/12/25 03:45:45 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 03:45:45 $
 */
 
package com.sun.cli.jmx.support;

import junit.framework.TestCase;

import com.sun.cli.util.stringifier.*;

import com.sun.cli.jmx.support.ParseResult;
import com.sun.cli.jmx.support.ParseResultStringifier;

public final class ArgParserTest extends TestCase
{
		void
	dm( Object o )
	{
		System.out.println( o.toString() );
	}
	
		public
	ArgParserTest()
	{
	}
	
		public void
	testCreate()
	{
		new ArgParserImpl();
	}
	
		public void
	testEmpty()
		throws ArgParserException
	{
		final ArgParserImpl	ap	= new ArgParserImpl( );
		
		final ParseResult []	results	= ap.Parse( "", false  );
		
		assertEquals( 0, results.length );
	}
	
		ParseResult
	stringWithCast( String str )
	{
		return( new ParseResult( ParseResult.LITERAL_STRING, str, "String"  ) ); 
	}
	
		ParseResult
	stringWithoutCast( String str )
	{
		return( new ParseResult( ParseResult.OTHER, str ) ); 
	}
		ParseResult
	otherWithCast( String str, String cast )
	{
		return( new ParseResult( ParseResult.OTHER, str, cast  ) ); 
	}
		ParseResult
	otherWithoutCast( String str )
	{
		return( new ParseResult( ParseResult.OTHER, str) ); 
	}

		String
	quoteString( String s )
	{
		return( '\"' + s + '\"' );
	}

	
		public void
	testSingleNonNamedStringWithoutCast()
		throws ArgParserException
	{
		final ParseResult []	results	= new ArgParserImpl().Parse( "hello", false  );
		
		assertEquals( 1, results.length );
		assertEquals( stringWithoutCast( "hello" ), results[ 0 ] );
	}
	
	
		public void
	testSingleNonNamedStringWithCast()
		throws ArgParserException
	{
		final ParseResult []	results	= new ArgParserImpl().Parse( "(String)hello", false  );
		
		assertEquals( 1, results.length );
		assertEquals( stringWithCast( "hello" ), results[ 0 ] );
	}
	
		public void
	testEmptyStringWithCast()
		throws ArgParserException
	{
		final ParseResult []	results	= new ArgParserImpl().Parse( "(String)", false  );
		
		assertEquals( 1, results.length );
		assertEquals( stringWithCast( "" ), results[ 0 ] );
	}
	
		public void
	testMultipleStringsWithCast()
		throws ArgParserException
	{
		final String	input	= "(String)s1,\"s2\",(String)s3";
		final ParseResult []	results	= new ArgParserImpl().Parse( input, false  );
		
		assertEquals( 3, results.length );
		final ParseResult []	expected = 
		{
			stringWithCast( "s1" ),
			stringWithCast( "s2" ),
			stringWithCast( "s3" ),
		};
		assertEquals( expected[ 0 ], results[ 0 ] );
		assertEquals( expected[ 1 ], results[ 1 ] );
		assertEquals( expected[ 2 ], results[ 2 ] );
	}
	
	final static String	TAB_STR	= "\\t";
	final static String	NEWLINE_STR	= "\\n";
	final static String	RETURN_STR	= "\\r";
	final static String	ESCAPE_CHAR_STR	= "\\\\";
	
		public void
	testEscapeCharsInUnquotedString()
		throws ArgParserException
	{
		final String	input	= TAB_STR + NEWLINE_STR + RETURN_STR + ESCAPE_CHAR_STR;
		final ParseResult []	results	= new ArgParserImpl().Parse( input, false  );
		
		assertEquals( 1, results.length );
		assertEquals( stringWithoutCast( "\t\n\r\\"  ), results[ 0 ] );
	}
	
		public void
	testEscapeCharsInQuotedString()
		throws ArgParserException
	{
		final String	input	= TAB_STR + NEWLINE_STR + RETURN_STR + ESCAPE_CHAR_STR;
		final ParseResult []	results	= new ArgParserImpl().Parse( quoteString( input ), false  );
		
		assertEquals( 1, results.length );
		assertEquals( stringWithCast( "\t\n\r\\"  ), results[ 0 ] );
	}
	
		public void
	testEmptyArray()
		throws ArgParserException
	{
		final String	input	= "{}";
		final ParseResult []	results	= new ArgParserImpl().Parse( input, false  );
		
		assertEquals( 1, results.length );
		final ParseResult	expected	=
				new ParseResult( ParseResult.ARRAY, new ParseResult [ 0 ] );
		assertEquals( expected, results[ 0 ] );
	}
	
		public void
	testMultiArray()
		throws ArgParserException
	{
		final String	input	= "{\"hello\",1,(String)there}";
		final ParseResult []	results	= new ArgParserImpl().Parse( input, false  );
		
		assertEquals( 1, results.length );
		
		final ParseResult []	expectedArrayElements = 
		{
			stringWithCast( "hello" ),
			otherWithoutCast( "1" ),
			stringWithCast( "there" ),
		};
		final ParseResult	expected	= new ParseResult( ParseResult.ARRAY, expectedArrayElements );
		
		assertEquals( 1, results.length );
		assertEquals( expected, results[ 0 ] );
	}

		private void
	testExpr( boolean namedArgs, String expr )
		throws Exception
	{
		boolean	success	= false;
		
		final ParseResult []	results	= new ArgParserImpl().Parse( expr, namedArgs );
	}
	
		public void
	testNonNamedSuccessCases( )
		throws Exception
	{
		// test valid cases
		boolean	success;
		
		final String []	expressions	= new String []
		{
			"1",
			"999.0003777",
			"\"\"",	// empty String
			")",	// a valid String
			"}",	// a valid String
			"=",	// a valid String
			"(boolean)true",
			"(Boolean)false",
			"hello",
			"(String)hello",
			"\\(String)",
			"\",,,,,,,,,\"",
			"\"hello\"",
			"1,2,3,4,5,6,\"hello\\,,,,,goodbye\",hello\\,goodbye",
			"{}",
			"{,,,,}",
			"{1}",
			"{1,2}",
			"{1,2,3,4,5,6,hello}",
			"{1,2,3,4,5,6,{hello,world}}",
			"(Object){(int)1,(long)2,(Integer)3,4,5,6,(String){(String)\"hello\",world}}"
		};
		final int numExprs	= expressions.length;
		
		final String	testName	= "argName";
		
		String	togetherList	= "";
		String	togetherNamed	= "";
		for( int i = 0; i < numExprs; ++i )
		{
			final String	testString	= expressions[ i ];
			
			try
			{
				testExpr( false, testString );
			}
			catch( Exception e )
			{
				fail( "ERROR: case failed: " + expressions[ i ] );
			}
			
			togetherList	= togetherList + testString + ",";
		}
		
		testExpr( false, togetherList );
		testExpr( false, "{" + togetherList + "}" );
	}
	
	
		public void
	testNamedSuccessCases( )
		throws Exception
	{
		// test valid cases
		boolean	success;
		
		final String []	expressions	= new String []
		{
			"test=hello\\,world",
			"hello=1,world=there",
			"arg1=1,arg2=2,arg3=3,argString=(String)hello\\,world",
			"count=1",
			"name=server1,cluster=cluster1,type=instance",
			"empty1=,empty2=,empty3="
		};
		final int numExprs	= expressions.length;
		
		String togetherList	= "";
		for( int i = 0; i < numExprs; ++i )
		{
			final String	testString	= expressions[ i ];
			
			try
			{
				testExpr( true, testString );
			}
			catch( Exception e )
			{
				fail( "ERROR: case failed: " + expressions[ i ] + "\n" + e.toString() );
			}
			
			togetherList	= togetherList + testString + ",";
		}
		
		testExpr( false, togetherList );
	}
	
		public void
	TestFailureCases()
		throws Exception
	{
		boolean	success;
		
		final String []	failureExpressions	= new String []
		{
			"{",
			"(",
			"()",
			"(a-b-c)",
			"(.abc)",
			"({",
			"(int)",
			"\"\"\"",
			"\"",
		};
		final int numFailureExprs	= failureExpressions.length;
		
		String together	= "{";
		for( int i = 0; i < numFailureExprs; ++i )
		{
			try
			{
				testExpr( false, failureExpressions[ i ] );
			}
			catch( Exception e )
			{
				fail( "ERROR: case failed: " + failureExpressions[ i ] + "\n" + e.toString() );
			}
			
			together	= together + failureExpressions[ i ] + ",";
		}
		together = together + "}";
		
		try
		{
			testExpr( false, together );
		}
		catch( Exception e )
		{
			fail( "ERROR: case failed: " + together + "\n" + e.toString() );
		}
	}
}

