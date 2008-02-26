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


import com.sun.appserv.management.annotation.AMXConfigInfo;

/* @XmlType(name = "", propOrder = {
    "jmxConnector",
    "dasConfig",
    "property"
}) */
@AMXConfigInfo( amxInterface=com.sun.appserv.management.config.AdminServiceConfig.class, singleton=true)
@Configured
public interface AdminService extends ConfigBeanProxy, Injectable  {

    /**
     * Gets the value of the type property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getType();

    /**
     * Sets the value of the type property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the systemJmxConnectorName property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getSystemJmxConnectorName();

    /**
     * Sets the value of the systemJmxConnectorName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSystemJmxConnectorName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the jmxConnector property.
     * Objects of the following type(s) are allowed in the list
     * {@link JmxConnector }
     */
    @Element("jmx-connector")
    public List<JmxConnector> getJmxConnector();

    /**
     * Gets the value of the dasConfig property.
     *
     * @return possible object is
     *         {@link DasConfig }
     */
    @Element("das-config")
    public DasConfig getDasConfig();

    /**
     * Sets the value of the dasConfig property.
     *
     * @param value allowed object is
     *              {@link DasConfig }
     */
    public void setDasConfig(DasConfig value) throws PropertyVetoException;

    /**
     * Gets the value of the property property.
     * Objects of the following type(s) are allowed in the list
     * {@link Property }
     */
    @Element("property")
    public List<Property> getProperty();



}
