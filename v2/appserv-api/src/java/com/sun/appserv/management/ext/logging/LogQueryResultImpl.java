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
package com.sun.appserv.management.ext.logging;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Date;

import java.util.logging.Level;

import java.io.Serializable;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.OpenDataException;

import com.sun.appserv.management.util.jmx.OpenMBeanUtil;
import com.sun.appserv.management.util.misc.ArrayUtil;
import com.sun.appserv.management.util.misc.ObjectUtil;
import com.sun.appserv.management.util.stringifier.ArrayStringifier;

/**
    <b>INTERNAL USE ONLY--not part of the API</b>
	@since AS 9.0
 */
public final class LogQueryResultImpl
    implements LogQueryResult
{
    private String[]         mFieldNames;
    private LogQueryEntry[]  mEntries;
    
        public
    LogQueryResultImpl(
        final String[]          fieldNames,
        final LogQueryEntry[]   entries )
    {
        mFieldNames   = fieldNames;
        mEntries      = entries;
    }
    
    /**
        Instantiate using result from {@link Logging#queryServerLog}.
        The first Object[] is a String[] of the field names.
        Subsequent Object[] are the data values.
     */
        public 
    LogQueryResultImpl( final List<Serializable[]> records )
    {
        mFieldNames   = (String[])records.get( 0 );
        
        mEntries    = new LogQueryEntry[ records.size() - 1 ];
        for( int i = 1; i < mEntries.length; ++i )
        {
            mEntries[ i ]   = new LogQueryEntryImpl( records.get( i ) );
        }
    }
    
        public String[]
    getFieldNames()
    {
        return mFieldNames;
    }
    
        public LogQueryEntry[]
    getEntries()
    {
        return mEntries;
    }
    
    private static final String    FIELD_DELIM = "\t";
    private static final String    NEWLINE = System.getProperty( "line.separator" );;
    /**
        Output a tab-delimited String with a header line. Each
        subsequent line represents another log record.
     */
        public String
    toString()
    {
        final StringBuilder builder = new StringBuilder();
        
        for( final String s : getFieldNames() )
        {
            builder.append( s );
            builder.append( FIELD_DELIM );
        }
        builder.replace( builder.length() - 1, builder.length(), NEWLINE );
        
        for ( final LogQueryEntry entry : getEntries() )
        {
            final Object[]  fields  = entry.getFields();
            for( final Object o : fields )
            {
                builder.append( o.toString() );
                builder.append( FIELD_DELIM );
            }
            builder.replace( builder.length() - 1, builder.length(), NEWLINE );
        }
        
        return builder.toString();
    }
    
 	    public int
 	hashCode()
 	{
 	    return ObjectUtil.hashCode( getFieldNames(), getEntries() );
 	}
    
        public boolean
    equals( final Object rhs )
    {
        boolean equal   = rhs instanceof LogQueryResult;
        
        if ( equal )
        {
            final LogQueryResult    r   = (LogQueryResult)rhs;
            
            equal   = ArrayUtil.arraysEqual( getFieldNames(), r.getFieldNames() ) &&
                      ArrayUtil.arraysEqual( getEntries(), r.getEntries() );
                        
        }
        
        return equal;
    }
    
 
}





