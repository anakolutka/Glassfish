/*
 * $Header: /home/cvspublic/jakarta-commons/digester/src/java/org/apache/commons/digester/AbstractRulesImpl.java,v 1.1 2003/04/02 19:03:34 rdonkin Exp $
 * $Revision: 1.1 $
 * $Date: 2003/04/02 19:03:34 $
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


import java.util.List;


/**
 * <p><code>AbstractRuleImpl</code> provides basic services for <code>Rules</code> implementations.
 * Extending this class should make it easier to create a <code>Rules</code> implementation.</p>
 * 
 * <p><code>AbstractRuleImpl</code> manages the <code>Digester</code> 
 * and <code>namespaceUri</code> properties.
 * If the subclass overrides {@link #registerRule} (rather than {@link #add}),
 * then the <code>Digester</code> and <code>namespaceURI</code> of the <code>Rule</code>
 * will be set correctly before it is passed to <code>registerRule</code>.
 * The subclass can then perform whatever it needs to do to register the rule.</p>
 *
 * @author Robert Burrell Donkin
 * @version $Revision: 1.1 $ $Date: 2003/04/02 19:03:34 $
 */

abstract public class AbstractRulesImpl implements Rules {

    // ------------------------------------------------------------- Fields
    
    /** Digester using this <code>Rules</code> implementation */
    private Digester digester;
    /** Namespace uri to assoicate with subsequent <code>Rule</code>'s */
    private String namespaceURI;
    
    // ------------------------------------------------------------- Properties

    /**
     * Return the Digester instance with which this Rules instance is
     * associated.
     */
    public Digester getDigester() {
        return digester;
    }

    /**
     * Set the Digester instance with which this Rules instance is associated.
     *
     * @param digester The newly associated Digester instance
     */
    public void setDigester(Digester digester) {
        this.digester = digester;
    }

    /**
     * Return the namespace URI that will be applied to all subsequently
     * added <code>Rule</code> objects.
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }

    /**
     * Set the namespace URI that will be applied to all subsequently
     * added <code>Rule</code> objects.
     *
     * @param namespaceURI Namespace URI that must match on all
     *  subsequently added rules, or <code>null</code> for matching
     *  regardless of the current namespace URI
     */
    public void setNamespaceURI(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Registers a new Rule instance matching the specified pattern.
     * This implementation sets the <code>Digester</code> and the
     * <code>namespaceURI</code> on the <code>Rule</code> before calling {@link #registerRule}.
     *
     * @param pattern Nesting pattern to be matched for this Rule
     * @param rule Rule instance to be registered
     */
    public void add(String pattern, Rule rule) {
        // set up rule
        if (this.digester != null) {
            rule.setDigester(this.digester);
        }
        
        if (this.namespaceURI != null) {
            rule.setNamespaceURI(this.namespaceURI);
        }
        
        registerRule(pattern, rule);
        
    }
    
    /** 
     * Register rule at given pattern.
     * The the Digester and namespaceURI properties of the given <code>Rule</code>
     * can be assumed to have been set properly before this method is called.
     *
     * @param pattern Nesting pattern to be matched for this Rule
     * @param rule Rule instance to be registered
     */ 
    abstract protected void registerRule(String pattern, Rule rule);

    /**
     * Clear all existing Rule instance registrations.
     */
    abstract public void clear();


    /**
     * Return a List of all registered Rule instances that match the specified
     * nesting pattern, or a zero-length List if there are no matches.  If more
     * than one Rule instance matches, they <strong>must</strong> be returned
     * in the order originally registered through the <code>add()</code>
     * method.
     *
     * @param pattern Nesting pattern to be matched
     *
     * @deprecated Call match(namespaceURI,pattern) instead.
     */
    public List match(String pattern) {
        return match(namespaceURI, pattern);
    }


    /**
     * Return a List of all registered Rule instances that match the specified
     * nesting pattern, or a zero-length List if there are no matches.  If more
     * than one Rule instance matches, they <strong>must</strong> be returned
     * in the order originally registered through the <code>add()</code>
     * method.
     *
     * @param namespaceURI Namespace URI for which to select matching rules,
     *  or <code>null</code> to match regardless of namespace URI
     * @param pattern Nesting pattern to be matched
     */
    abstract public List match(String namespaceURI, String pattern);


    /**
     * Return a List of all registered Rule instances, or a zero-length List
     * if there are no registered Rule instances.  If more than one Rule
     * instance has been registered, they <strong>must</strong> be returned
     * in the order originally registered through the <code>add()</code>
     * method.
     */
    abstract public List rules();

}
