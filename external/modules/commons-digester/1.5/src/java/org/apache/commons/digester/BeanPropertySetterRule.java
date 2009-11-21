/*
 * $Header: /home/cvspublic/jakarta-commons/digester/src/java/org/apache/commons/digester/BeanPropertySetterRule.java,v 1.12 2003/04/16 11:23:50 jstrachan Exp $
 * $Revision: 1.12 $
 * $Date: 2003/04/16 11:23:50 $
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


import java.beans.PropertyDescriptor;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.PropertyUtils;


/**
 * <p> Rule implements sets a bean property on the top object
 * to the body text.</p>
 *
 * <p> The property set:</p>
 * <ul><li>can be specified when the rule is created</li>
 * <li>or can match the current element when the rule is called.</li></ul>
 *
 * <p> Using the second method and the {@link ExtendedBaseRules} child match
 * pattern, all the child elements can be automatically mapped to properties
 * on the parent object.</p>
 *
 * @author Robert Burrell Donkin
 * @version $Revision: 1.12 $ $Date: 2003/04/16 11:23:50 $
 */

public class BeanPropertySetterRule extends Rule {


    // ----------------------------------------------------------- Constructors


    /**
     * <p>Construct rule that sets the given property from the body text.</p>
     *
     * @param digester associated <code>Digester</code>
     * @param propertyName name of property to set
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #BeanPropertySetterRule(String propertyName)} instead.
     */
    public BeanPropertySetterRule(Digester digester, String propertyName) {

        this(propertyName);

    }

    /**
     * <p>Construct rule that automatically sets a property from the body text.
     *
     * <p> This construct creates a rule that sets the property
     * on the top object named the same as the current element.
     *
     * @param digester associated <code>Digester</code>
     *     
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #BeanPropertySetterRule()} instead.
     */
    public BeanPropertySetterRule(Digester digester) {

        this();

    }

    /**
     * <p>Construct rule that sets the given property from the body text.</p>
     *
     * @param propertyName name of property to set
     */
    public BeanPropertySetterRule(String propertyName) {

        this.propertyName = propertyName;

    }

    /**
     * <p>Construct rule that automatically sets a property from the body text.
     *
     * <p> This construct creates a rule that sets the property
     * on the top object named the same as the current element.
     */
    public BeanPropertySetterRule() {

        this((String)null);

    }
    
    // ----------------------------------------------------- Instance Variables


    /**
     * Set this property on the top object.
     */
    protected String propertyName = null;


    /**
     * The body text used to set the property.
     */
    protected String bodyText = null;


    // --------------------------------------------------------- Public Methods


    /**
     * Process the body text of this element.
     *
     * @param namespace the namespace URI of the matching element, or an 
     *   empty string if the parser is not namespace aware or the element has
     *   no namespace
     * @param name the local name if the parser is namespace aware, or just 
     *   the element name otherwise
     * @param text The text of the body of this element
     */
    public void body(String namespace, String name, String text)
        throws Exception {

        // log some debugging information
        if (digester.log.isDebugEnabled()) {
            digester.log.debug("[BeanPropertySetterRule]{" +
                    digester.match + "} Called with text '" + text + "'");
        }

        bodyText = text.trim();

    }


    /**
     * Process the end of this element.
     *
     * @param namespace the namespace URI of the matching element, or an 
     *   empty string if the parser is not namespace aware or the element has
     *   no namespace
     * @param name the local name if the parser is namespace aware, or just 
     *   the element name otherwise
     *
     * @exception NoSuchMethodException if the bean does not
     *  have a writeable property of the specified name
     */
    public void end(String namespace, String name) throws Exception {

        String property = propertyName;

        if (property == null) {
            // If we don't have a specific property name,
            // use the element name.
            property = name;
        }

        // Get a reference to the top object
        Object top = digester.peek();

        // log some debugging information
        if (digester.log.isDebugEnabled()) {
            digester.log.debug("[BeanPropertySetterRule]{" + digester.match +
                    "} Set " + top.getClass().getName() + " property " +
                               property + " with text " + bodyText);
        }

        // Force an exception if the property does not exist
        // (BeanUtils.setProperty() silently returns in this case)
        if (top instanceof DynaBean) {
            DynaProperty desc =
                ((DynaBean) top).getDynaClass().getDynaProperty(property);
            if (desc == null) {
                throw new NoSuchMethodException
                    ("Bean has no property named " + property);
            }
        } else /* this is a standard JavaBean */ {
            PropertyDescriptor desc =
                PropertyUtils.getPropertyDescriptor(top, property);
            if (desc == null) {
                throw new NoSuchMethodException
                    ("Bean has no property named " + property);
            }
        }

        // Set the property (with conversion as necessary)
        BeanUtils.setProperty(top, property, bodyText);

    }


    /**
     * Clean up after parsing is complete.
     */
    public void finish() throws Exception {

        bodyText = null;

    }


    /**
     * Render a printable version of this Rule.
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("BeanPropertySetterRule[");
        sb.append("propertyName=");
        sb.append(propertyName);
        sb.append("]");
        return (sb.toString());

    }

}
