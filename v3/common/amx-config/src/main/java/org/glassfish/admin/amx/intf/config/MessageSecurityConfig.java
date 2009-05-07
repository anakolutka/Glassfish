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
package org.glassfish.admin.amx.intf.config;

import org.glassfish.admin.amx.base.Singleton;


import java.util.Map;

/**
Configuration for the &lt;message-security-config&gt; element.
 */
public interface MessageSecurityConfig
        extends NamedConfigElement
{

    public static final String AMX_TYPE = "message-security";
    /** Key for use with {@link SecurityServiceConfig#createMessageSecurityConfig}.  Name of the default provider. */
    public static final String DEFAULT_PROVIDER_KEY = "DefaultProvider";
    /** Key for use with {@link SecurityServiceConfig#createMessageSecurityConfig}. Name of the default client provider */
    public static final String DEFAULT_CLIENT_PROVIDER_KEY = "DefaultClientProvider";
    /** one of the legal values for auth-layer */
    public static final String AUTH_LAYER_HTTP_SERVLET = "HttpServlet";
    /** one of the legal values for auth-layer */
    public static final String AUTH_LAYER_SOAP = "SOAP";

    /**
    One of the values defined by {@link MessageLayerValues}.
     */
    public String getAuthLayer();

    public String getDefaultClientProvider();

    public void setDefaultClientProvider(final String value);

    public String getDefaultProvider();

    public void setDefaultProvider(final String value);

    /**
    @return A map of ProviderConfig MBean proxies keyed on provider-id.
     */
    public Map<String, ProviderConfig> getProvider();
}
