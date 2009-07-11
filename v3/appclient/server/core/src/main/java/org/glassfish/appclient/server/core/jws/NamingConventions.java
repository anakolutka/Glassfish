/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.appclient.server.core.jws;

/**
 *
 * @author tjquinn
 */
public class NamingConventions {
    private static final String JWSAPPCLIENT_PREFIX = "/___JWSappclient";

    public static final String JWSAPPCLIENT_SYSTEM_PREFIX =
            JWSAPPCLIENT_PREFIX + "/___system";

    private  static final String JWSAPPCLIENT_APP_PREFIX =
            JWSAPPCLIENT_PREFIX + "/___app";

    private static final String JWSAPPCLIENT_DOMAIN_PREFIX =
            JWSAPPCLIENT_PREFIX + "/___domain";

    public static final String DYN_PREFIX = "___dyn";

    public static String contextRootForAppAdapter(final String appName) {
        /*
         * No trailing slash for the context root to use for registering
         * with Grizzly.
         */
        return NamingConventions.JWSAPPCLIENT_APP_PREFIX + "/" + appName;
    }

    public static String domainContentURIString(final String domainRelativeURIString) {
        return JWSAPPCLIENT_DOMAIN_PREFIX + "/" + domainRelativeURIString;
    }

    public static String generatedEARFacadeName(final String earName) {
        return generatedEARFacadePrefix(earName) + ".jar";
    }

    public static String generatedEARFacadePrefix(final String earName) {
        return earName + "Client";
    }

    public static String anchorSubpathForNestedClient(final String clientName) {
        /*
         * We need to add the trailing slash here because we don't want to
         * put it in the template.  Otherwise we'd have an extra slash
         * where we don't want one when we're launching a stand-alone client.
         */
        return clientName + "Client/";
    }

    public static String systemJNLPURI() {
        return DYN_PREFIX + "/___system.jnlp";
    }

}
