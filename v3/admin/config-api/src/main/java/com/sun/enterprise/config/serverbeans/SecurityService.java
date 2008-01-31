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



package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.component.Injectable;

import java.beans.PropertyVetoException;
import java.io.Serializable;
import java.util.List;


/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "authRealm",
    "jaccProvider",
    "auditModule",
    "messageSecurityConfig",
    "property"
}) */
@Configured
public interface SecurityService extends ConfigBeanProxy, Injectable  {

    /**
     * Gets the value of the defaultRealm property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getDefaultRealm();

    /**
     * Sets the value of the defaultRealm property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDefaultRealm(String value) throws PropertyVetoException;

    /**
     * Gets the value of the defaultPrincipal property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getDefaultPrincipal();

    /**
     * Sets the value of the defaultPrincipal property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDefaultPrincipal(String value) throws PropertyVetoException;

    /**
     * Gets the value of the defaultPrincipalPassword property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getDefaultPrincipalPassword();

    /**
     * Sets the value of the defaultPrincipalPassword property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDefaultPrincipalPassword(String value) throws PropertyVetoException;

    /**
     * Gets the value of the anonymousRole property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getAnonymousRole();

    /**
     * Sets the value of the anonymousRole property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAnonymousRole(String value) throws PropertyVetoException;

    /**
     * Gets the value of the auditEnabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getAuditEnabled();

    /**
     * Sets the value of the auditEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAuditEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the jacc property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getJacc();

    /**
     * Sets the value of the jacc property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setJacc(String value) throws PropertyVetoException;

    /**
     * Gets the value of the auditModules property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getAuditModules();

    /**
     * Sets the value of the auditModules property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAuditModules(String value) throws PropertyVetoException;

    /**
     * Gets the value of the activateDefaultPrincipalToRoleMapping property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getActivateDefaultPrincipalToRoleMapping();

    /**
     * Sets the value of the activateDefaultPrincipalToRoleMapping property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setActivateDefaultPrincipalToRoleMapping(String value) throws PropertyVetoException;

    /**
     * Gets the value of the mappedPrincipalClass property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getMappedPrincipalClass();

    /**
     * Sets the value of the mappedPrincipalClass property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMappedPrincipalClass(String value) throws PropertyVetoException;

    /**
     * Gets the value of the authRealm property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the authRealm property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuthRealm().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link AuthRealm }
     */
    @Element(required=true)
    public List<AuthRealm> getAuthRealm();

    /**
     * Gets the value of the jaccProvider property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the jaccProvider property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getJaccProvider().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link JaccProvider }
     */
    @Element(required=true)
    public List<JaccProvider> getJaccProvider();

    /**
     * Gets the value of the auditModule property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the auditModule property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuditModule().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link AuditModule }
     */
    @Element
    public List<AuditModule> getAuditModule();

    /**
     * Gets the value of the messageSecurityConfig property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the messageSecurityConfig property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMessageSecurityConfig().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link MessageSecurityConfig }
     */
    @Element
    public List<MessageSecurityConfig> getMessageSecurityConfig();

    /**
     * Gets the value of the property property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the property property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProperty().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Property }
     */
    @Element("property")
    public List<Property> getProperty();



}
