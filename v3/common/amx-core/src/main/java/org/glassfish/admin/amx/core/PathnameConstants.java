/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.amx.core;

import org.glassfish.admin.amx.annotation.Stability;
import org.glassfish.admin.amx.annotation.Taxonomy;
import org.glassfish.admin.amx.base.Pathnames;

/**
    Constants and regex related to pathnames.
    <p>
    The root part (leading "/") is not included in the parts list returned
    by {@link parts}.
    <p>
    Wildcarding is basic: a '*" means "0 or more characters" (a '*' is converted to
    '.*' for regex purposes).
 * @see Pathnames
 * @see PathnameParser
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
public final class PathnameConstants
{
    private PathnameConstants() {}
    
    /** delimiter between parts of a path */
    public static final char SEPARATOR = '/';
    
    /**
        Wildcard charcter, the '*' (not a regex expression).
        Usage is similar to usage in a shell, the '*' means "zero or more
    */
    public static final String MATCH_ZERO_OR_MORE = "*";

    /** subscript left character, subscripts must be a character pair for grammar reasons */
    public static final char SUBSCRIPT_LEFT = '[';
    /** subscript right character, subscripts must be a character pair for grammar reasons */
    public static final char SUBSCRIPT_RIGHT = ']';
    
    /**
        The characters legal to use as the type portion of a pathname,
        expressed as regex compatible string, but without enclosing square brackets.
    */
    public static final String LEGAL_CHAR_FOR_TYPE = "$a-zA-Z0-9._-";
    
    /** Regex pattern for one legal character (in square braces). */
    public static final String LEGAL_CHAR_FOR_TYPE_PATTERN = "[**" + LEGAL_CHAR_FOR_TYPE + "]";
    
    /** Regex pattern for one legal character (in square braces), wildcard allowed */
   // public static final String LEGAL_CHAR_FOR_TYPE_WILD_PATTERN = "[" + LEGAL_CHAR_FOR_TYPE + "*]";
    
     /** regex pattern denoting a legal type, grouping () surrounding it */
    public static final String LEGAL_TYPE_PATTERN = "(" + LEGAL_CHAR_FOR_TYPE_PATTERN + LEGAL_CHAR_FOR_TYPE_PATTERN + "*)";
    
     /** regex pattern denoting a legal type, with wildcards, grouping () surrounding it */
   // public static String LEGAL_TYPE_WILD_PATTERN = "(" + LEGAL_CHAR_FOR_TYPE_WILD_PATTERN + "*)";

    /**
        The characters legal to use as a name.  A name may be zero length, and it may include
        the {@link SEPARATOR} character. However, it may not include the right square brace, because
        that character terminates a subscript.
        JMX ObjectNames might have additional restrictions.
    */
    public static final String LEGAL_CHAR_FOR_NAME = "^" + SUBSCRIPT_RIGHT;

    /** Regex pattern for one legal name character (in square braces). */
    public static final String LEGAL_CHAR_FOR_NAME_PATTERN = "[" + LEGAL_CHAR_FOR_NAME + "]";

    /** Regex pattern for one legal name character (in square braces). */
   // public static final String LEGAL_CHAR_FOR_NAME_WILD_PATTERN = "[" + LEGAL_CHAR_FOR_NAME + "*]";

     /** regex pattern denoting a legal name */
    public static final String LEGAL_NAME_PATTERN = LEGAL_CHAR_FOR_NAME_PATTERN + "*";

     /** regex pattern denoting a legal name, with wildcards */
   // public static final String LEGAL_NAME_WILD_PATTERN = LEGAL_CHAR_FOR_NAME_WILD_PATTERN + "*";
    
}













