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
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.component.Injectable;

import java.beans.PropertyVetoException;
import java.io.Serializable;
import java.util.List;


/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */
@org.glassfish.api.amx.AMXConfigInfo( amxInterfaceName="com.sun.appserv.management.config.DiagnosticServiceConfig", singleton=true)
@Configured
public interface DiagnosticService extends ConfigBeanProxy, Injectable  {

    /**
     * Gets the value of the computeChecksum property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getComputeChecksum();

    /**
     * Sets the value of the computeChecksum property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setComputeChecksum(String value) throws PropertyVetoException;

    /**
     * Gets the value of the verifyConfig property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getVerifyConfig();

    /**
     * Sets the value of the verifyConfig property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setVerifyConfig(String value) throws PropertyVetoException;

    /**
     * Gets the value of the captureInstallLog property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getCaptureInstallLog();

    /**
     * Sets the value of the captureInstallLog property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCaptureInstallLog(String value) throws PropertyVetoException;

    /**
     * Gets the value of the captureSystemInfo property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getCaptureSystemInfo();

    /**
     * Sets the value of the captureSystemInfo property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCaptureSystemInfo(String value) throws PropertyVetoException;

    /**
     * Gets the value of the captureHadbInfo property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getCaptureHadbInfo();

    /**
     * Sets the value of the captureHadbInfo property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCaptureHadbInfo(String value) throws PropertyVetoException;

    /**
     * Gets the value of the captureAppDd property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getCaptureAppDd();

    /**
     * Sets the value of the captureAppDd property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCaptureAppDd(String value) throws PropertyVetoException;

    /**
     * Gets the value of the minLogLevel property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getMinLogLevel();

    /**
     * Sets the value of the minLogLevel property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMinLogLevel(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxLogEntries property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getMaxLogEntries();

    /**
     * Sets the value of the maxLogEntries property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMaxLogEntries(String value) throws PropertyVetoException;

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
