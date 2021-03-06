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
 * $Header: /cvs/glassfish/admin-cli/cli-api/src/java/com/sun/cli/jmx/support/ArgParserImpl.java,v 1.3 2005/12/25 03:45:45 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 03:45:45 $
 */
 

 
package com.sun.cli.jmx.support;

import java.lang.reflect.Array;
import java.util.ArrayList;



public final class ArgParserImpl implements ArgParser
{
	public static class ParseChars
	{
		public final static ParseChars	DEFAULT	= new ParseChars();
		
		public char			mArgDelim;
		
		public char			mEscapeChar;
		public String		mEscapableChars;
		public String		mEscapableCharsWithinLiteralString;
		public char			mArrayLeft;
		public char			mArrayRight;
		
		// non-configurable
		final static char	TYPECAST_BEGIN	= '(';
		final static char	TYPECAST_END	= ')';
		final static char	LITERAL_STRING_DELIM	= '\"';
		final static char	ARG_VALUE_DELIM		= '=';
		
			public void
		validate()
		{
			// escapable chars must contain the escape char
			assert( mEscapableChars.indexOf( mEscapeChar ) >= 0 );
			assert( mEscapableChars.indexOf( DEFAULT_ARRAY_LEFT ) >= 0 );
			
			// we do not allow configuration of type cast chars
			assert( mEscapableChars.indexOf( '(' ) >= 0 );
		}
		
			public
		ParseChars()
		{
			mArgDelim							= DEFAULT_ARG_DELIM;
			mEscapeChar							= DEFAULT_ESCAPE_CHAR;
			mEscapableChars						= DEFAULT_ESCAPABLE_CHARS;
			mEscapableCharsWithinLiteralString	= DEFAULT_ESCAPABLE_CHARS_WITHIN_LITERAL_STRING;
			mArrayLeft							= DEFAULT_ARRAY_LEFT;
			mArrayRight							= DEFAULT_ARRAY_RIGHT;
			
			validate();
		}
	};
	
	
	boolean					mNamedArgs	= false;
	String					mInput;
	int						mCurIndex;
	ParseChars				mParseChars;
	
	
		public
	ArgParserImpl( )
	{
		mParseChars	= ParseChars.DEFAULT;
	}
	
		public
	ArgParserImpl( final ParseChars	parseChars )
	{
		mParseChars	= parseChars;
	}
	
	
		private int
	inputRemainingCount()
	{
		return( mInput.length() - mCurIndex );
	}
	
		private static boolean
	isDigit( int theChar )
	{
		return( (theChar >= '0' && theChar <= '9') );
	}
	
		private static boolean
	isHexDigit( int theChar )
	{
		return( isDigit( theChar ) || (theChar >= 'a' && theChar <= 'f') || 
			(theChar >= 'A' && theChar <= 'F') );
	}
	
		private boolean
	IsEscapableChar( int theChar, String escapableChars )
	{
		return( escapableChars.indexOf( theChar ) >= 0 || theChar == mParseChars.mEscapeChar );
	}
	
		private int
	escapeCharToChar( int theChar )
	{
		int	result	= -1;
		
		if ( theChar == 'n' )
			result	= '\n';
		else if ( theChar == 'r'  )
			result	= '\r';
		else if ( theChar == 't'  )
			result	= '\t';
		else
			result	= theChar;
		
		return( result );
	}
	
	
	/*
		Get the next logical character, unescaping the input as necessary.
		
		@returns	the next character, or -1 if no more input
	 */
		private int
	nextChar( int escapeChar, String escapableChars )
	{
		int	result	= -1;
		
		final int	theChar	= nextLiteralChar();
		if ( theChar == escapeChar )
		{
			final int	theNextChar	= nextLiteralChar();
			if ( IsEscapableChar( theNextChar, escapableChars )  )
			{
				result	= escapeCharToChar( theNextChar );
			}
			else
			{
				// if valid hexadecimal, convert two hex digits to a number
				// otherwise emit the escape char and restart
				// should we allow unicode?
				result	= escapeChar;
				if ( isHexDigit( theNextChar ) && peekNextLiteralChar() > 0 )
				{
					final int theNextNextChar	= nextLiteralChar();
					if ( isHexDigit( theNextNextChar ) )
					{
						result	= (((int)theNextChar) << 4) + (int)theNextNextChar;
					}
				}
				else
				{
					backup( 1 );
				}
			}
		}
		else
		{
			result	= theChar;
		}
		
		return( result );
	}
	
		private int
	nextChar()
	{
		return( nextChar( mParseChars.mEscapeChar, mParseChars.mEscapableChars ) );
	}
	
		private int
	nextLiteralChar()
	{
		final int	result	= peekNextLiteralChar();
		if ( result >  0 )
		{
			advance( 1 );
		}
		
		return( result );
	}
	
		private String
	peekRemaining()
	{
		if ( inputRemainingCount() <= 0 )
			return( "" );
		
		return( mInput.substring( mCurIndex, mInput.length() ) );
	}
	
		private int
	peekNextLiteralChar()
	{
		if ( inputRemainingCount() <= 0 )
			return( -1 );
			
		final char	result	= mInput.charAt( mCurIndex );
		
		return( result );
	}
	
		private void
	backup( int count )
	{
		assert( count <= mCurIndex );
		
		mCurIndex	-= count;
	}
	
		private void
	advance( int count )
	{
		assert( count <= inputRemainingCount() );
		
		mCurIndex	+= count;
	}
	
		private void
	checkInputAvailable()
		throws ArgParserException
	{
		if ( inputRemainingCount() == 0 )
		{
			throw new ArgParserException( "expecting additional input" );
		}
	}
	
	
	/*
		Parse a named argument name. Named argument names are *not* escaped. Everything
		up to the '=' is the name. The name and the '=' are consumed.
		
		Named arguments may not have the "," character in them and usually should be
		
		@returns		a ParseResult for the cast
	 */
		private String
	ParseNamedArgName()
		throws ArgParserException
	{
		checkInputAvailable();
		
		final StringBuffer	buf	= new StringBuffer();
		
		boolean		foundDelim	= false;
		
		int		theChar	= 0;
		while ( (theChar = nextLiteralChar()) > 0 )
		{
			if ( theChar == mParseChars.ARG_VALUE_DELIM )
			{
				foundDelim	= true;
				break;
			}
			
			if ( ! Character.isJavaIdentifierPart( (char)theChar ) )
			{
				break;
			}
			
			buf.append( (char)theChar );
		}
		
		if ( ! foundDelim )
		{
			throw new ArgParserException(
				"named arguments must be of the form name=value: " + buf.toString() );
		}
		
		return( new String( buf ) );
	}
	
	/*
		Parse a named argument. The name argument must be of the form:
		<name>=<value>
		
		@returns		a ParseResult for the cast
	 */
		private ParseResult
	ParseNamedArg()
		throws ArgParserException
	{
		checkInputAvailable();
		
		final String		name	= ParseNamedArgName();
		
		ParseResult	result	= null;
		
		// special case--no more input
		if ( inputRemainingCount() > 0 )
		{
			result	= ParseNonNamedArg( "" + mParseChars.mArgDelim );
		}
		else
		{
			result	= new ParseResult( ParseResult.OTHER, "");
		}
		result.setName( name );
		
		return( result );
	}
	
		boolean
	isStringTypecast( String typecast )
	{
		return( typecast != null &&
			(typecast.equals( "String" ) || typecast.equals( "java.lang.String" ) ) );
	}
	
	/*
		Parse a non-named argument. 
		
		@returns		a ParseResult for the cast
	 */
		private ParseResult
	ParseNonNamedArg( String delimiters )
		throws ArgParserException
	{
		ParseResult	result	= null;
		
		String typeCast = ParseTypeCast();	// may be null--that's OK
		
		if ( isStringTypecast( typeCast ) )
		{
			if ( inputRemainingCount() == 0 )
			{
				// special case--a type cast followed by nothing is OK as an empty string
				result	= new ParseResult( ParseResult.LITERAL_STRING, "" );
				result.setTypeCast( typeCast );
				return( result );
			}
		}
		
		boolean	quotedString	= false;
		
		result = ParseLiteralString();
		if ( result != null )
		{
			// if a string X is quoted as "X" , this is equivalent (shorthand) for (String)X
			typeCast		= "String";
			quotedString	= true;
		}
		else
		{
			result	= ParseArray();
			if ( result == null )
			{
				result	= ParseRegular( delimiters );
			}
		}
		
		result.setTypeCast( typeCast );
		if ( isStringTypecast( typeCast ) && result.getType() != ParseResult.ARRAY )
		{
			result.setType( ParseResult.LITERAL_STRING );
			
			if (  (! quotedString) &&
				result.getData() instanceof String &&
				"null".equalsIgnoreCase( (String)result.getData() ) )
			{
				result.setData( null );
			}
		}
		
		// eat the delimiter
		final int	theChar	= nextLiteralChar();
		
		boolean	validDelimiter	= delimiters.indexOf( theChar ) >= 0 || theChar < 0;
		if ( ! validDelimiter )
		{
			throw new ArgParserException( "Syntax error: parsed this so far: " + mInput.substring( 1, mCurIndex ) );
		}
		
		return( result );
	}
	
	
	/*
		Parse a type-cast.  The type-cast must be properly formed with the the begin
		and end array type-cast characters ( and ).
		
		@returns		a ParseResult for the cast of null if not a type cast.
	 */
		private String
	ParseTypeCast( )
		throws ArgParserException
	{
		int		theChar	= nextLiteralChar();
		if ( theChar != mParseChars.TYPECAST_BEGIN )
		{
			backup( 1 );
			return( null );
		}
		
		final StringBuffer	buf	= new StringBuffer();
		
		theChar	= nextLiteralChar();
		if ( ! Character.isJavaIdentifierStart( (char)theChar ) )
		{
			final String	msg	= "type cast must contain a legal Java " +
			"identifier start character: " + peekRemaining();
			throw new ArgParserException( msg  );
		}
		buf.append( (char)theChar );
		
		boolean		foundDelim	= false;
		while ( (theChar = nextLiteralChar( )) > 0 )
		{
			if ( theChar == mParseChars.TYPECAST_END )
			{
				foundDelim	= true;
				break;
			}
			
			if ( theChar != '.' && ! Character.isJavaIdentifierPart( (char)theChar ) )
			{
				throw new ArgParserException( "type cast must contain a legal Java identifier part: " +
							(char)theChar );
			}
			
			buf.append( (char)theChar );
		}
		
		if ( ! foundDelim )
		{
			throw new ArgParserException( "type cast must be terminated by the ) character" );
		}
		
		final String		result	= new String( buf );
		
		if ( result.length() == 0 )
		{
			throw new ArgParserException( "type cast must contain a type" );
		}
		
		return( result );
	}
	
	
	/*
		Parse  characters that are not (1) array or (2) quote-delimited string.
		Type-cast (if any) has already been parsed.
		
		Note that the argument might be a string or it might be any of a variety of 
		numbers.
		
		@returns		a ParseResult
	 */
		private ParseResult
	ParseRegular( String delimiters )
		throws ArgParserException
	{
		checkInputAvailable();
		
		final StringBuffer	buf	= new StringBuffer();

		int theChar;
		while ( (theChar = peekNextLiteralChar() ) > 0 )
		{
			if ( delimiters.indexOf( theChar ) >= 0 )
			{
				break;
			}
			
			theChar	= nextChar();
			
			buf.append( (char)theChar );
		}
		
		final String		resultString	= new String( buf );
		
		final ParseResult	result	= new ParseResult( ParseResult.OTHER, resultString );
		
		return( result );
	
	}
	
	
	/*
		Parse an array.  The array must be properly formed with the the begin
		and end array characters { and }.
		
		@returns		a ParseResult for the array or null if not an array
	 */
		private ParseResult
	ParseArray( )
		throws ArgParserException
	{
		checkInputAvailable();
		
		// verify that it's an array
		int		theChar	= nextLiteralChar();
		if ( theChar !=  mParseChars.mArrayLeft )
		{
			backup( 1 );
			return( null );
		}
		
		final ArrayList	elems	= new ArrayList();
		
		boolean		endOfArrayFound	= false;
			
		if ( peekNextLiteralChar() == mParseChars.mArrayRight )
		{
			// empty array
			endOfArrayFound	= true;
			advance( 1 );
		}
		else
		{
			while ( (theChar = peekNextLiteralChar( )) > 0 )
			{
				final ParseResult	elem	= ParseNonNamedArg( "" +
						mParseChars.mArrayRight + mParseChars.mArgDelim );
				elems.add( elem );
				
				backup( 1 );
				if ( nextLiteralChar() == mParseChars.mArrayRight)
				{
					endOfArrayFound	= true;
					break;
				}
			}
		}
		
		if ( ! endOfArrayFound )
		{
			throw new ArgParserException( "end of array not found" );
		}
		
		final ParseResult []	resultsArray	= new ParseResult[ elems.size() ];
		elems.toArray( resultsArray );
		
		final ParseResult	result	= new ParseResult( ParseResult.ARRAY, resultsArray );
		
		return( result );
	
	}
	
	
	/*
		Parse a literal String.  The String must be properly formed with the the begin
		and end denoted by the double-quote character ".  The only escapable character
		within a literal string is the double-quote itself.
		
		@returns		a ParseResult for the String or null if not a literal String
	 */
		private ParseResult
	ParseLiteralString( )
		throws ArgParserException
	{
		checkInputAvailable();
		
		// verify that it's a literal string
		int		theChar	= nextLiteralChar();
		if ( theChar != mParseChars.LITERAL_STRING_DELIM )
		{
			backup( 1 );
			return( null );
		}
		
		final StringBuffer	buf	= new StringBuffer();
		
		boolean		endOfStringFound	= false;
		while ( (theChar = peekNextLiteralChar( )) > 0 )
		{
			if ( theChar == mParseChars.LITERAL_STRING_DELIM )
			{
				endOfStringFound	= true;
				advance( 1 );
				break;
			}
			
			theChar = nextChar( mParseChars.mEscapeChar, mParseChars.mEscapableCharsWithinLiteralString );
			buf.append( (char)theChar );
		}
		
		if ( ! endOfStringFound )
		{
			throw new ArgParserException( "literal string must be terminated by double quote \"" );
		}
		
		
		final String		resultString	= new String( buf );
		
		final ParseResult	result	= new ParseResult( ParseResult.LITERAL_STRING, resultString );
		
		return( result );
	}
	
	/*
		Parse any argument--named or not
		
		@returns		a ParseResult for the argument
	 */
		private ParseResult
	ParseArg()
		throws ArgParserException
	{

		checkInputAvailable();
		
		ParseResult	result	= null;
		
		if ( mNamedArgs )
		{
			result	= ParseNamedArg();
		}
		else
		{
			result	= ParseNonNamedArg( "" + mParseChars.mArgDelim );
		}
		
		return( result );
	}
	
	
		public String []
	ParseNames( String input  )
		throws ArgParserException
	{
		mInput		= input;
		mCurIndex	= 0;
		
		final ArrayList	list	= new ArrayList();
		
		StringBuffer	buf	= new StringBuffer();
		while ( true )
		{
			final int	theChar = nextLiteralChar();
			
			if ( theChar < 0 || theChar == mParseChars.mArgDelim )
			{
				list.add( new String( buf ) );
				buf.setLength( 0 );
				
				if ( theChar < 0 )
					break;
			}
			else
			{
				buf.append( (char)theChar );
			}
		}
		
		
		final String []	results	= (String [])list.toArray( new String [ list.size() ]);
		
		return( results );
	}
	
	 
		public ParseResult []
	Parse( String input, boolean namedArgs  )
		throws ArgParserException
	{
		mNamedArgs	= namedArgs;
		mInput		= input;
		mCurIndex	= 0;
		
		final ArrayList	results	= new ArrayList();
		
		while ( inputRemainingCount() > 0 )
		{
			final ParseResult	nextArg	= ParseArg( );
			assert( nextArg != null );
			
			results.add( nextArg );
		}
		
		
		final ParseResult [] arrayResults 	= new ParseResult [ results.size( ) ];
		results.toArray( arrayResults );
		
		return( arrayResults );
	}
	
		private static void
	p( Object o )
	{
		System.out.println( o.toString() );
	}
}

