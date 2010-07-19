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

import java.util.HashMap;
import java.util.Map;
import com.sun.jersey.api.client.ClientResponse;
import javax.ws.rs.core.Cookie;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Mitesh Meswani
 */
public class TokenAuthenticationTest extends RestTestBase {
    private static final String URL_DOMAIN_SESSIONS = BASE_URL + "/sessions";
    private static final String URL_CREATE_USER = BASE_URL_DOMAIN + "/configs/config/server-config/security-service/auth-realm/admin-realm/create-user";
    private static final String URL_DELETE_USER = BASE_URL_DOMAIN + "/configs/config/server-config/security-service/auth-realm/admin-realm/delete-user";
    private static final String GF_REST_TOKEN_COOKIE_NAME = "gfresttoken";
    private static final String TEST_GROUP = "newgroup";

    @Test
    public void testTokenCreateAndDelete() {
        deleteUserAuthTestUser(); // just in case
        //Verify a session token got created
        String token = getSessionToken();

        // Verify we can use the session token.
        ClientResponse response = client.resource(BASE_URL_DOMAIN).cookie(new Cookie(GF_REST_TOKEN_COOKIE_NAME, token)).get(ClientResponse.class);
        assertTrue(isSuccess(response));

        //Delete the token
        response = client.resource(URL_DOMAIN_SESSIONS + "/" + token).cookie(new Cookie(GF_REST_TOKEN_COOKIE_NAME, token)).delete(ClientResponse.class); delete(URL_DOMAIN_SESSIONS);
        assertTrue(isSuccess(response));
    }

    @Test
    public void testAuthRequired() {
            Map<String, String> newUser = new HashMap<String, String>() {{
            put("id", AUTH_USER_NAME);
            put("groups", TEST_GROUP);
            put("AS_ADMIN_USERPASSWORD", AUTH_PASSWORD);
        }};

        try {
            // Delete the test user if it exists
            deleteUserAuthTestUser();

            // Verify that we can get unauthenticated access to the server
            ClientResponse response = get(BASE_URL_DOMAIN);
            assertTrue(isSuccess(response));

            // Create the new user
            response = post(URL_CREATE_USER, newUser);
            assertTrue(isSuccess(response));

            // Verify that we must now authentication (response.status = 401)
            response = get(BASE_URL_DOMAIN);
            assertFalse(isSuccess(response));

            // Authenticate, get the token, then "clear" the authentication
            authenticate();
            String token = getSessionToken();
            resetClient();

            // Build this request manually so we can pass the cookie
            response = client.resource(BASE_URL_DOMAIN).cookie(new Cookie(GF_REST_TOKEN_COOKIE_NAME, token)).get(ClientResponse.class);
            assertTrue(isSuccess(response));

            // Request again w/o the cookie.  This should fail.
            response = get(BASE_URL_DOMAIN);
            assertFalse(isSuccess(response));
        } finally {
            // Clean up after ourselves
            deleteUserAuthTestUser();
        }
    }

    protected String getSessionToken() {
        ClientResponse response = post(URL_DOMAIN_SESSIONS);
        assertTrue(isSuccess(response));
        return response.getEntity(String.class);
    }

    private void deleteUserAuthTestUser() {
        ClientResponse response = delete(URL_DELETE_USER, new HashMap<String, String>() {{ put("id", AUTH_USER_NAME); }});
        if (response.getStatus() == 401) {
            authenticate();
            response = delete(URL_DELETE_USER, new HashMap<String, String>() {{ put("id", AUTH_USER_NAME); }});
            assertTrue(isSuccess(response));
            resetClient();
        }
    }
}