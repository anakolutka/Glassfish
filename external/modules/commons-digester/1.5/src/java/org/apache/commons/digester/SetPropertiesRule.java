/*
 * $Header: /home/cvspublic/jakarta-commons/digester/src/java/org/apache/commons/digester/SetPropertiesRule.java,v 1.11 2003/04/16 11:23:50 jstrachan Exp $
 * $Revision: 1.11 $
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


import java.util.HashMap;

import org.apache.commons.beanutils.BeanUtils;
import org.xml.sax.Attributes;


/**
 * <p>Rule implementation that sets properties on the object at the top of the
 * stack, based on attributes with corresponding names.</p>
 *
 * <p>This rule supports custom mapping of attribute names to property names.
 * The default mapping for particular attributes can be overridden by using 
 * {@link #SetPropertiesRule(String[] attributeNames, String[] propertyNames)}.
 * This allows attributes to be mapped to properties with different names.
 * Certain attributes can also be marked to be ignored.</p>
 *
 * @author Craig McClanahan
 * @version $Revision: 1.11 $ $Date: 2003/04/16 11:23:50 $
 */

public class SetPropertiesRule extends Rule {


    // ----------------------------------------------------------- Constructors


    /**
     * Default constructor sets only the the associated Digester.
     *
     * @param digester The digester with which this rule is associated
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #SetPropertiesRule()} instead.
     */
    public SetPropertiesRule(Digester digester) {

        this();

    }
    

    /**
     * Base constructor.
     */
    public SetPropertiesRule() {

        // nothing to set up 

    }
    
    /** 
     * <p>Convenience constructor overrides the mapping for just one property.</p>
     *
     * <p>For details about how this works, see
     * {@link #SetPropertiesRule(String[] attributeNames, String[] propertyNames)}.</p>
     *
     * @param attributeName map this attribute 
     * @param propertyName to a property with this name
     */
    public SetPropertiesRule(String attributeName, String propertyName) {
        
        attributeNames = new String[1];
        attributeNames[0] = attributeName;
        propertyNames = new String[1];
        propertyNames[0] = propertyName;
    }
    
    /** 
     * <p>Constructor allows attribute->property mapping to be overriden.</p>
     *
     * <p>Two arrays are passed in. 
     * One contains the attribute names and the other the property names.
     * The attribute name / property name pairs are match by position
     * In order words, the first string in the attribute name list matches
     * to the first string in the property name list and so on.</p>
     *
     * <p>If a property name is null or the attribute name has no matching
     * property name, then this indicates that the attibute should be ignored.</p>
     * 
     * <h5>Example One</h5>
     * <p> The following constructs a rule that maps the <code>alt-city</code>
     * attribute to the <code>city</code> property and the <code>alt-state</code>
     * to the <code>state</code> property. 
     * All other attributes are mapped as usual using exact name matching.
     * <code><pre>
     *      SetPropertiesRule(
     *                new String[] {"alt-city", "alt-state"}, 
     *                new String[] {"city", "state"});
     * </pre></code>
     *
     * <h5>Example Two</h5>
     * <p> The following constructs a rule that maps the <code>class</code>
     * attribute to the <code>className</code> property.
     * The attribute <code>ignore-me</code> is not mapped.
     * All other attributes are mapped as usual using exact name matching.
     * <code><pre>
     *      SetPropertiesRule(
     *                new String[] {"class", "ignore-me"}, 
     *                new String[] {"className"});
     * </pre></code>
     *
     * @param attributeNames names of attributes to map
     * @param proeprtyNames names of properties mapped to
     */
    public SetPropertiesRule(String[] attributeNames, String[] propertyNames) {
        // create local copies
        this.attributeNames = new String[attributeNames.length];
        for (int i=0, size=attributeNames.length; i<size; i++) {
            this.attributeNames[i] = attributeNames[i];
        }
        
        this.propertyNames = new String[propertyNames.length];
        for (int i=0, size=propertyNames.length; i<size; i++) {
            this.propertyNames[i] = propertyNames[i];
        } 
    }
        
    // ----------------------------------------------------- Instance Variables
    
    /** 
     * Attribute names used to override natural attribute->property mapping
     */
    private String [] attributeNames;
    /** 
     * Property names used to override natural attribute->property mapping
     */    
    private String [] propertyNames;


    // --------------------------------------------------------- Public Methods


    /**
     * Process the beginning of this element.
     *
     * @param context The associated context
     * @param attributes The attribute list of this element
     */
    public void begin(Attributes attributes) throws Exception {
        
        // Build a set of attribute names and corresponding values
        HashMap values = new HashMap();
        
        // set up variables for custom names mappings
        int attNamesLength = 0;
        if (attributeNames != null) {
            attNamesLength = attributeNames.length;
        }
        int propNamesLength = 0;
        if (propertyNames != null) {
            propNamesLength = propertyNames.length;
        }
        
        
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            if ("".equals(name)) {
                name = attributes.getQName(i);
            }
            String value = attributes.getValue(i);
            
            // we'll now check for custom mappings
            for (int n = 0; n<attNamesLength; n++) {
                if (name.equals(attributeNames[n])) {
                    if (n < propNamesLength) {
                        // set this to value from list
                        name = propertyNames[n];
                    
                    } else {
                        // set name to null
                        // we'll check for this later
                        name = null;
                    }
                    break;
                }
            } 
            
            if (digester.log.isDebugEnabled()) {
                digester.log.debug("[SetPropertiesRule]{" + digester.match +
                        "} Setting property '" + name + "' to '" +
                        value + "'");
            }
            if (name != null) {
                values.put(name, value);
            } 
        }

        // Populate the corresponding properties of the top object
        Object top = digester.peek();
        if (digester.log.isDebugEnabled()) {
            digester.log.debug("[SetPropertiesRule]{" + digester.match +
                    "} Set " + top.getClass().getName() + " properties");
        }
        BeanUtils.populate(top, values);


    }


    /**
     * <p>Add an additional attribute name to property name mapping.
     * This is intended to be used from the xml rules.
     */
    public void addAlias(String attributeName, String propertyName) {
        
        // this is a bit tricky.
        // we'll need to resize the array.
        // probably should be synchronized but digester's not thread safe anyway
        if (attributeNames == null) {
            
            attributeNames = new String[1];
            attributeNames[0] = attributeName;
            propertyNames = new String[1];
            propertyNames[0] = propertyName;        
            
        } else {
            int length = attributeNames.length;
            String [] tempAttributes = new String[length + 1];
            for (int i=0; i<length; i++) {
                tempAttributes[i] = attributeNames[i];
            }
            tempAttributes[length] = attributeName;
            
            String [] tempProperties = new String[length + 1];
            for (int i=0; i<length && i< propertyNames.length; i++) {
                tempProperties[i] = propertyNames[i];
            }
            tempProperties[length] = propertyName;
            
            propertyNames = tempProperties;
            attributeNames = tempAttributes;
        }        
    }
  

    /**
     * Render a printable version of this Rule.
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("SetPropertiesRule[");
        sb.append("]");
        return (sb.toString());

    }


}
