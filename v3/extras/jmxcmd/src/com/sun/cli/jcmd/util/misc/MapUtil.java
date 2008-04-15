/*
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the "License").  You may not use this file except 
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt or 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html. 
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * HEADER in each file and include the License file at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable, 
 * add the following below this CDDL HEADER, with the 
 * fields enclosed by brackets "[]" replaced with your 
 * own identifying information: Portions Copyright [yyyy] 
 * [name of copyright owner]
 */
package com.sun.cli.jcmd.util.misc;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import com.sun.cli.jcmd.util.stringifier.SmartStringifier;


public final class MapUtil
{
		private
	MapUtil( )
	{
		// disallow instantiation
	}
	
	    public static <K extends Object, V extends Object> V
	getWithDefault(
	    final Map<K,V> m, final K key, final V defaultValue )
	{
	    return m.containsKey( key ) ? m.get( key ) : defaultValue;
	}
	
	
		public static Object[]
	getKeyObjects( final Map<Object,Object> m)
	{
		return( SetUtil.toArray( m.keySet() ) );
	}
	
		public static String[]
	getKeyStrings( final Map<?,?> m)
	{
		return( GSetUtil.toSortedStringArray( m.keySet() ) );
	}
	
	/**
		Create a new Map consisting of a single key/value pair.
	 */
		public static <V extends Object> Map<String,V>
	newMap( 
		final String key,
		final V      value )
	{
		final Map<String,V>	m	= new HashMap<String,V>();
		
		m.put( key, value );
		
		return( m );
	}
	
	/**
		Create a new Map consisting of a single key/value pair.
	 */
		public static <K extends Object, V extends Object> Map<K,V>
	newMap( 
		final Map<K,V>	m1,
		final Map<K,V>	m2 )
	{
		final Map<K,V>	m	= new HashMap<K,V>();
		
		if ( m1 != null )
		{
			m.putAll( m1 );
		}
		if ( m2 != null )
		{
			m.putAll( m2 );
		}
		
		return( m );
	}
	
	
	/**
		Create a new Map and insert the specified mappings as found in 'mappings'.
		The even-numbered entries are the keys, and the odd-numbered entries are
		the values.
	 */
		public static Map<Object,Object>
	newMap( final Object[] mappings )
	{
		if ( (mappings.length % 2) != 0 )
		{
			throw new IllegalArgumentException( "mappings must have even length" );
		}
		
		final Map<Object,Object>	m	= new HashMap<Object,Object>();
		
		for( int i = 0; i < mappings.length; i += 2 )
		{
		    final Object key   = mappings[ i ];
		    final Object value = mappings[ i+1 ];
		    
			m.put( key, value );
		}
		
		return( m );
	}
	
	
	/**
		Remove all entries keyed by 'keys'
	 */
		public static void
	removeAll( 
		final Map		m,
		final String[]	keys )
	{
		for( int i = 0; i < keys.length; ++i )
		{
			m.remove( keys[ i ] );
		}
	}
	
		public static boolean
	mapsEqual(
		final Map	m1,
		final Map	m2 )
	{
		if ( m1 == m2 )
		{
			return( true );
		}
		
		boolean	equal	= false;
		
		if ( m1.size() == m2.size() &&
			m1.keySet().equals( m2.keySet() ) )
		{
			equal	= true;
			
			final Iterator	iter	= m1.keySet().iterator();
			while ( iter.hasNext() )
			{
				final Object	key	= iter.next();
				final Object	value1	= m1.get( key );
				final Object	value2	= m2.get( key );
				
				if ( ! CompareUtil.objectsEqual( value1, value2 ) )
				{
					equal	= false;
					break;
				}
			}
		}
		
		return( equal );
	}
	
	
		public static <K extends Object, V extends Object> Map<K,V>
	newMapNoNullValues( final Map<K,V> m )
	{
		final Map<K,V>	result	= new HashMap<K,V>();
		
		for( final K key : m.keySet() )
		{
			final V	value	= m.get( key );
			
			if ( value != null )
			{
				result.put( key, value );
			}
		}
		
		return( result );
	}
	
		public static String
	toString( final Map m )
	{
		return( toString( m, "," ) );
	}
	
		public static String
	toString( final Map m, final String separator )
	{
		if ( m == null )
		{
			return( "null" );
		}
		
		final StringBuffer	buf	= new StringBuffer();
		
		final String[]  keyStrings  = getKeyStrings( m );
		for( final String key : keyStrings )
		{
			final Object	value	= m.get( key );
			
			buf.append( key );
			buf.append( "=" );
			buf.append( SmartStringifier.toString( value ) );
			buf.append( separator );
		}
		if ( buf.length() != 0 )
		{
			// strip trailing separator
			buf.setLength( buf.length() - separator.length() );
		}
		
		return( buf.toString() );
	}
	
		public static <K extends Object, V extends Object> Set<K>
	getNullValueKeys( final Map<K,V> m)
	{
		final Set<K>    s	= new HashSet<K>();
		
		for( final K key : m.keySet() )
		{
			if ( m.get( key ) == null )
			{
				s.add( key );
			}
		}
		return( s );
	}
	
	
	    public static Map<String,Object>
	toMap( final Properties props )
	{
	    final Map<String,Object> m    = new HashMap<String,Object>();
	    
	    for( final Object key : props.keySet() )
	    {
	        m.put( (String)key, props.get( key ) );
	    }
	    
	    return m;
	}
	
	    public static <K,V> void
	removeAll( final Map<K,V> m, final Set<K> s )
	{
	    for( final K key : s )
	    {
	        m.remove( key );
	    }
	}
}






