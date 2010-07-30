/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.admin.rest;

import com.sun.jersey.api.client.ClientResponse;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jasonlee
 */
public class NetworkListenerTest extends RestTestBase {
    protected static final String URL_PROTOCOL = "/domain/configs/config/server-config/network-config/protocols/protocol";
    @Test
    public void createHttpListener() {
        final String redirectProtocolName = "http-redirect"; //protocol_" + generateRandomString();
        final String portUniProtocolName = "pu-protocol"; //protocol_" + generateRandomString();

        final String redirectFilterName = "redirect-filter"; //filter_" + generateRandomString();
        final String finderName1 = "http-finder"; //finder" + generateRandomString();
        final String finderName2 = "http-redirect"; //finder" + generateRandomString();

        try {
            post("/domain/set", new HashMap<String, String>() {{
                put("configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.protocol", "http-listener-1");
            }});
            delete(URL_PROTOCOL + "/" + portUniProtocolName);
            delete(URL_PROTOCOL + "/" + redirectProtocolName);
// asadmin commands taken from: http://www.antwerkz.com/port-unification-in-glassfish-3-part-1/
//        asadmin create-protocol --securityenabled=false http-redirect
//        asadmin create-protocol --securityenabled=false pu-protocol
            ClientResponse response = post(URL_PROTOCOL, new HashMap<String, String>() {{ put ("securityenabled", "false"); put("id", redirectProtocolName); }});
            checkStatusForSuccess(response);
            response = post(URL_PROTOCOL, new HashMap<String, String>() {{ put ("securityenabled", "false"); put("id", portUniProtocolName); }});
            checkStatusForSuccess(response);

//        asadmin create-protocol-filter --protocol http-redirect --classname com.sun.grizzly.config.HttpRedirectFilter redirect-filter
            response = post (URL_PROTOCOL + "/" + redirectProtocolName + "/create-protocol-filter",
                new HashMap<String, String>() {{
                    put ("id", redirectFilterName);
                    put ("protocol", redirectProtocolName);
                    put ("classname", "com.sun.grizzly.config.HttpRedirectFilter");
                }});
            checkStatusForSuccess(response);

//        asadmin create-protocol-finder --protocol pu-protocol --target-protocol http-listener-2 --classname com.sun.grizzly.config.HttpProtocolFinder http-finder
//        asadmin create-protocol-finder --protocol pu-protocol --target-protocol http-redirect   --classname com.sun.grizzly.config.HttpProtocolFinder http-redirect
            response = post (URL_PROTOCOL + "/" + portUniProtocolName + "/create-protocol-finder",
                new HashMap<String, String>() {{
                    put ("id", finderName1);
                    put ("protocol", portUniProtocolName);
                    put ("target-protocol", "http-listener-2");
                    put ("classname", "com.sun.grizzly.config.HttpProtocolFinder");
                }});
            checkStatusForSuccess(response);
            response = post (URL_PROTOCOL + "/" + portUniProtocolName + "/create-protocol-finder",
                new HashMap<String, String>() {{
                    put ("id", finderName2);
                    put ("protocol", portUniProtocolName);
                    put ("target-protocol", redirectProtocolName);
                    put ("classname", "com.sun.grizzly.config.HttpProtocolFinder");
                }});
            checkStatusForSuccess(response);


//        asadmin set configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.protocol=pu-protocol
            response = post("/domain/set", new HashMap<String, String>() {{
                put("configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.protocol", portUniProtocolName);
            }});
            checkStatusForSuccess(response);

            response = get("/domain/configs/config/server-config/network-config/network-listeners/network-listener/http-listener-1/find-http-protocol");
            checkStatusForSuccess(response);
            assertTrue(response.getEntity(String.class).contains("property name=\"protocol\" value=\"http-listener-2\""));
        } finally {
            ClientResponse response = post("/domain/set", new HashMap<String, String>() {{
                put("configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.protocol", "http-listener-1");
            }});
            checkStatusForSuccess(response);
            response = delete(URL_PROTOCOL + "/" + portUniProtocolName + "/delete-protocol-finder",
                new HashMap<String, String>() {{
                    put("protocol", portUniProtocolName);
                    put("id", finderName1);
                }} );
            checkStatusForSuccess(response);
            response = delete(URL_PROTOCOL + "/" + portUniProtocolName + "/delete-protocol-finder",
                new HashMap<String, String>() {{
                    put("protocol", portUniProtocolName);
                    put("id", finderName2);
                }} );
            checkStatusForSuccess(response);
            response = delete(URL_PROTOCOL + "/" + redirectProtocolName + "/protocol-chain-instance-handler/protocol-chain/protocol-filter/" + redirectFilterName,
                    new HashMap<String, String>() {{ put("protocol", redirectProtocolName); }} );
            checkStatusForSuccess(response);
            response = delete(URL_PROTOCOL + "/" + portUniProtocolName);
            checkStatusForSuccess(response);
            response = delete(URL_PROTOCOL + "/" + redirectProtocolName);
            checkStatusForSuccess(response);
        }
        
    }
}
