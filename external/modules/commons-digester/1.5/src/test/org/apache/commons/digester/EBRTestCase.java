/*
 * $Header: /home/cvspublic/jakarta-commons/digester/src/test/org/apache/commons/digester/EBRTestCase.java,v 1.7 2003/03/12 22:10:51 rdonkin Exp $
 * $Revision: 1.7 $
 * $Date: 2003/03/12 22:10:51 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */


package org.apache.commons.digester;


import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * <p> Runs standard tests for RulesBase as well as tests of extensions.
 *
 * @author Robert Burrell Donkin <robertdonkin@mac.com>
 * @version $Revision: 1.7 $ $Date: 2003/03/12 22:10:51 $
 */


public class EBRTestCase extends RulesBaseTestCase {


    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new instance of this test case.
     *
     * @param name Name of the test case
     */
    public EBRTestCase(String name) {

        super(name);
    }


    // -------------------------------------------------- Overall Test Methods

    /**
     * <p> This should be overriden by subclasses.
     *
     * @return the matching rules to be tested.
     */
    protected Rules createMatchingRulesForTest() {

        return new ExtendedBaseRules();
    }


    /**
     * Return the tests included in this test suite.
     */
    public static Test suite() {

        return (new TestSuite(EBRTestCase.class));

    }


    /**
     * Basic test of parent matching rules.
     * A parent match matches any child of a particular kind of parent.
     * A wild parent has a wildcard prefix.
     * This method tests non-universal wildcards.
     */
    public void testBasicParentMatch() {

        // clear any existing rules
        digester.getRules().clear();

        assertEquals("Initial rules list is empty",
                0, digester.getRules().rules().size());

        // Set up rules
        // since these are all NON-UNIVERSAL matches
        // only expect one match at each stage
        digester.addRule("alpha/beta/gamma/delta", new TestRule("exact"));
        digester.addRule("*/beta/gamma/epsilon", new TestRule("wild_child"));
        digester.addRule("alpha/beta/gamma/?", new TestRule("exact_parent"));
        digester.addRule("*/beta/gamma/?", new TestRule("wild_parent"));


        List list = null;
        Iterator it = null;

        // this should match just the exact since this has presidence
        list = digester.getRules().match(null, "alpha/beta/gamma/delta");

        // all three rules should match
        assertEquals("Testing basic parent mismatch (A)", 1, list.size());

        it = list.iterator();
        assertEquals("Testing basic parent mismatch (B)", "exact", ((TestRule) it.next()).getIdentifier());


        // we don't have an exact match for this child so we should get the exact parent
        list = digester.getRules().match(null, "alpha/beta/gamma/epsilon");

        // all three rules should match
        assertEquals("Testing basic parent mismatch (C)", 1, list.size());

        it = list.iterator();
        assertEquals("Testing basic parent mismatch (D)", "exact_parent", ((TestRule) it.next()).getIdentifier());


        // wild child overrides wild parent
        list = digester.getRules().match(null, "alpha/omega/beta/gamma/epsilon");

        // all three rules should match
        assertEquals("Testing basic parent mismatch (E)", 1, list.size());

        it = list.iterator();
        assertEquals("Testing basic parent mismatch (F)", "wild_child", ((TestRule) it.next()).getIdentifier());


        // nothing else matches so return wild parent
        list = digester.getRules().match(null, "alpha/omega/beta/gamma/zeta");

        // all three rules should match
        assertEquals("Testing basic parent mismatch (G)", 1, list.size());

        it = list.iterator();
        assertEquals("Testing basic parent mismatch (H)", "wild_parent", ((TestRule) it.next()).getIdentifier());


        // clean up
        digester.getRules().clear();

    }


    /**
     * Basic test of universal matching rules.
     * Universal rules act independent.
     */
    public void testBasicUniversal() {

        // clear any existing rules
        digester.getRules().clear();

        assertEquals("Initial rules list is empty",
                0, digester.getRules().rules().size());

        // Set up rules
        // set up universal matches against non-universal ones
        digester.addRule("alpha/beta/gamma", new TestRule("exact"));
        digester.addRule("*/beta/gamma", new TestRule("non_wild_head"));
        digester.addRule("!*/beta/gamma", new TestRule("universal_wild_head"));
        digester.addRule("!alpha/beta/gamma/?", new TestRule("universal_wild_child"));
        digester.addRule("alpha/beta/gamma/?", new TestRule("non_wild_child"));
        digester.addRule("alpha/beta/gamma/epsilon", new TestRule("exact2"));
        digester.addRule("alpha/epsilon/beta/gamma/zeta", new TestRule("exact3"));
        digester.addRule("*/gamma/?", new TestRule("non_wildhead_child"));
        digester.addRule("!*/epsilon/beta/gamma/?", new TestRule("universal_wildhead_child"));


        List list = null;
        Iterator it = null;

        // test universal wild head
        list = digester.getRules().match(null, "alpha/beta/gamma");

        assertEquals("Testing universal wildcard mismatch (A)", 2, list.size());

        it = list.iterator();
        assertEquals("Testing universal wildcard mismatch (B)", "exact", ((TestRule) it.next()).getIdentifier());
        assertEquals("Testing universal wildcard mismatch (C)", "universal_wild_head", ((TestRule) it.next()).getIdentifier());


        // test universal parent
        list = digester.getRules().match(null, "alpha/beta/gamma/epsilon");

        assertEquals("Testing universal wildcard mismatch (D)", 2, list.size());

        it = list.iterator();
        assertEquals("Testing universal wildcard mismatch (E)", "universal_wild_child", ((TestRule) it.next()).getIdentifier());
        assertEquals("Testing universal wildcard mismatch (F)", "exact2", ((TestRule) it.next()).getIdentifier());

        // test universal parent
        list = digester.getRules().match(null, "alpha/beta/gamma/zeta");

        assertEquals("Testing universal wildcard mismatch (G)", 2, list.size());

        it = list.iterator();
        assertEquals("Testing universal wildcard mismatch (H)", "universal_wild_child", ((TestRule) it.next()).getIdentifier());
        assertEquals("Testing universal wildcard mismatch (I)", "non_wild_child", ((TestRule) it.next()).getIdentifier());


        // test wildcard universal parent
        list = digester.getRules().match(null, "alpha/epsilon/beta/gamma/alpha");

        assertEquals("Testing universal wildcard mismatch (J)", 2, list.size());

        it = list.iterator();
        assertEquals("Testing universal wildcard mismatch (K)", "non_wildhead_child", ((TestRule) it.next()).getIdentifier());
        assertEquals("Testing universal wildcard mismatch (L)", "universal_wildhead_child", ((TestRule) it.next()).getIdentifier());

        // test wildcard universal parent
        list = digester.getRules().match(null, "alpha/epsilon/beta/gamma/zeta");

        assertEquals("Testing universal wildcard mismatch (M)", 2, list.size());

        it = list.iterator();
        assertEquals("Testing universal wildcard mismatch (M)", "exact3", ((TestRule) it.next()).getIdentifier());
        assertEquals("Testing universal wildcard mismatch (O)", "universal_wildhead_child", ((TestRule) it.next()).getIdentifier());


        // clean up
        digester.getRules().clear();

    }


    /**
     * Basic test of wild matches.
     * A universal will match matches anything!
     * A non-universal will match matches anything not matched by something else.
     * This method tests non-universal and universal wild matches.
     */
    public void testWildMatch() {

        // clear any existing rules
        digester.getRules().clear();

        assertEquals("Initial rules list is empty",
                0, digester.getRules().rules().size());

        // Set up rules
        // The combinations a little large to test everything but we'll pick a couple and try them.
        digester.addRule("*", new TestRule("basic_wild"));
        digester.addRule("!*", new TestRule("universal_wild"));
        digester.addRule("alpha/beta/gamma/delta", new TestRule("exact"));
        digester.addRule("*/beta/gamma/?", new TestRule("wild_parent"));


        List list = null;
        Iterator it = null;

        // The universal wild will always match whatever else does
        list = digester.getRules().match(null, "alpha/beta/gamma/delta");

        // all three rules should match
        assertEquals("Testing wild mismatch (A)", 2, list.size());

        it = list.iterator();
        assertEquals("Testing wild mismatch (B)", "universal_wild", ((TestRule) it.next()).getIdentifier());
        assertEquals("Testing wild mismatch (C)", "exact", ((TestRule) it.next()).getIdentifier());


        // The universal wild will always match whatever else does
        list = digester.getRules().match(null, "alpha/beta/gamma/epsilon");

        assertEquals("Testing wild mismatch (D)", 2, list.size());

        it = list.iterator();
        assertEquals("Testing wild mismatch (E)", "universal_wild", ((TestRule) it.next()).getIdentifier());
        assertEquals("Testing wild mismatch (F)", "wild_parent", ((TestRule) it.next()).getIdentifier());


        // The universal wild will always match whatever else does
        // we have no other non-universal matching so this will match the non-universal wild as well
        list = digester.getRules().match(null, "alpha/gamma");

        assertEquals("Testing wild mismatch (G)", 2, list.size());

        it = list.iterator();
        assertEquals("Testing wild mismatch (H)", "basic_wild", ((TestRule) it.next()).getIdentifier());
        assertEquals("Testing wild mismatch (I)", "universal_wild", ((TestRule) it.next()).getIdentifier());


        // clean up
        digester.getRules().clear();

    }
    
    

    /**
     * Basic test of wild matches.
     * A universal will match matches anything!
     * A non-universal will match matches anything not matched by something else.
     * This method tests non-universal and universal wild matches.
     */
    public void testRootTailMatch() {

        // clear any existing rules
        digester.getRules().clear();

        assertEquals("Initial rules list is empty",
                0, digester.getRules().rules().size());

        // Set up rules
        // The combinations a little large to test everything but we'll pick a couple and try them.
        digester.addRule("*/a", new TestRule("a_tail"));


        List list = null;
        Iterator it = null;

        list = digester.getRules().match(null, "a");

        assertEquals("Testing tail wrong size (A)", 1, list.size());
        assertEquals("Testing tail mismatch (B)", "a_tail", ((TestRule) list.get(0)).getIdentifier());


        list = digester.getRules().match(null, "beta/a");

        assertEquals("Testing tail wrong size (C)", 1, list.size());
        assertEquals("Testing tail mismatch (D)", "a_tail", ((TestRule) list.get(0)).getIdentifier());

        list = digester.getRules().match(null, "be/aaa");

        assertEquals("Testing tail no matches (E)", 0, list.size());
        
        list = digester.getRules().match(null, "aaa");

        assertEquals("Testing tail no matches (F)", 0, list.size());
        
        list = digester.getRules().match(null, "a/beta");

        assertEquals("Testing tail no matches (G)", 0, list.size());

        // clean up
        digester.getRules().clear();

    }
    
    public void testAncesterMatch() throws Exception {
        System.out.println("Starting ancester match...");
        // test fixed root ancester
        digester.getRules().clear();
        
        digester.addRule("!a/b/*", new TestRule("uni-a-b-star"));
        digester.addRule("a/b/*", new TestRule("a-b-star"));
        digester.addRule("a/b/c", new TestRule("a-b-c"));
        digester.addRule("a/b/?", new TestRule("a-b-child"));
        
        List
        list = digester.getRules().match(null, "a/b/c");
        
        assertEquals("Simple ancester matches (1)", 2, list.size());
        assertEquals("Univeral ancester mismatch (1)", "uni-a-b-star" , ((TestRule) list.get(0)).getIdentifier());
        assertEquals("Parent precedence failure", "a-b-c" , ((TestRule) list.get(1)).getIdentifier());
        
        list = digester.getRules().match(null, "a/b/b");        
        assertEquals("Simple ancester matches (2)", 2, list.size());
        assertEquals("Univeral ancester mismatch (2)", "uni-a-b-star" , ((TestRule) list.get(0)).getIdentifier());
        assertEquals("Child precedence failure", "a-b-child" , ((TestRule) list.get(1)).getIdentifier());
        
        list = digester.getRules().match(null, "a/b/d");        
        assertEquals("Simple ancester matches (3)", 2, list.size());
        assertEquals("Univeral ancester mismatch (3)", "uni-a-b-star" , ((TestRule) list.get(0)).getIdentifier());
        assertEquals("Ancester mismatch (1)", "a-b-child" , ((TestRule) list.get(1)).getIdentifier());

        list = digester.getRules().match(null, "a/b/d/e/f");        
        assertEquals("Simple ancester matches (4)", 2, list.size());
        assertEquals("Univeral ancester mismatch (4)", "uni-a-b-star" , ((TestRule) list.get(0)).getIdentifier());
        assertEquals("Ancester mismatch (2)", "a-b-star" , ((TestRule) list.get(1)).getIdentifier());
        
        // test wild root ancester
        digester.getRules().clear();

        digester.addRule("!*/a/b/*", new TestRule("uni-star-a-b-star"));
        digester.addRule("*/b/c/*", new TestRule("star-b-c-star"));
        digester.addRule("*/b/c/d", new TestRule("star-b-c-d"));
        digester.addRule("a/b/c", new TestRule("a-b-c"));
        
        list = digester.getRules().match(null, "a/b/c");  
        assertEquals("Wild ancester match (1)", 2, list.size());
        assertEquals(
                    "Univeral ancester mismatch (5)", 
                    "uni-star-a-b-star" , 
                    ((TestRule) list.get(0)).getIdentifier());
        assertEquals("Match missed (1)", "a-b-c" , ((TestRule) list.get(1)).getIdentifier());
        
        list = digester.getRules().match(null, "b/c");  
        assertEquals("Wild ancester match (2)", 1, list.size());
        assertEquals("Match missed (2)", "star-b-c-star" , ((TestRule) list.get(0)).getIdentifier());    
        
        list = digester.getRules().match(null, "a/b/c/d"); 
        assertEquals("Wild ancester match (3)", 2, list.size());
        assertEquals("Match missed (3)", "uni-star-a-b-star" , ((TestRule) list.get(0)).getIdentifier());    
        assertEquals("Match missed (4)", "star-b-c-d" , ((TestRule) list.get(1)).getIdentifier());
        
        list = digester.getRules().match(null, "b/b/c/e/d"); 
        assertEquals("Wild ancester match (2)", 1, list.size());
        assertEquals("Match missed (5)", "star-b-c-star" , ((TestRule) list.get(0)).getIdentifier());    
        System.out.println("Finished ancester match.");
    }
}
