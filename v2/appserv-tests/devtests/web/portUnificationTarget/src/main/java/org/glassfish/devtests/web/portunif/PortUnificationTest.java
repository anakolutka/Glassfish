/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.devtests.web.portunif;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

import com.sun.appserv.test.BaseDevTest;
import com.sun.grizzly.http.portunif.HttpProtocolFinder;

/*
 * Unit test for port unification
 */
public class PortUnificationTest extends BaseDevTest {
    private int port = Integer.valueOf(antProp("https.port"));
    private String puName = "pu-protocol";
    private String httpName = "pu-http-protocol";
    private String dummyName = "pu-dummy-protocol";

    public static void main(String[] args) throws IOException {
        new PortUnificationTest().run();
    }

    @Override
    protected String getTestName() {
        return "port-unification";
    }

    @Override
    protected String getTestDescription() {
        return "Unit test for managing port unification";
    }

    public void run() throws IOException {
        try {
            report("create-pu-protocol", asadmin("create-protocol", "--target", "c1",
                puName));
            createHttpElements();
            createDummyProtocolElements();
            report("set-listener", asadmin("set",
                "configs.config.c1-config.network-config.network-listeners.network-listener.http-listener-2.protocol="
                    + puName));
            report("enable-listener", asadmin("set",
                "configs.config.c1-config.network-config.network-listeners.network-listener.http-listener-2.enabled=true"));
            final String content = getContent(new URL("http://localhost:" + port).openConnection());
            report("http-read", content.contains("<h1>Your server is now running</h1>"));
            report("dummy-read", "Dummy-Protocol-Response".equals(getDummyProtocolContent("localhost")));

            AsadminReturn aReturn = asadminWithOutput("list-protocol-filters", "--target", "c1", "pu-dummy-protocol");
            report("list-protocol-filters", aReturn.out.contains("dummy-filter"));

            aReturn = asadminWithOutput("list-protocol-finders", "--target", "c1", "pu-protocol");
            report("list-protocol-finders", aReturn.out.contains("http-finder") && aReturn.out.contains("dummy-finder"));

            report("disable-listener", asadmin("set",
                "configs.config.c1-config.network-config.network-listeners.network-listener.http-listener-2.enabled=false"));
            report("reset-listener", asadmin("set",
                "configs.config.c1-config.network-config.network-listeners.network-listener.http-listener-2.protocol=http-listener-2"));
            deletePUElements();
        } finally {
            stat.printSummary();
        }
    }

    private String getContent(URLConnection connection) {
        InputStreamReader reader = null;
        try {
            try {
                reader = new InputStreamReader(connection.getInputStream());
                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    builder.append(buffer, 0, read);
                }
                return builder.toString();
            } finally {
                if(reader != null) {
                    reader.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String getDummyProtocolContent(String host) throws IOException {
        OutputStream os = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        Socket s = new Socket(host, port);
        try {
            os = s.getOutputStream();
            os.write("dummy-protocol".getBytes());
            os.flush();
            is = s.getInputStream();
            baos = new ByteArrayOutputStream();
            int b;
            while ((b = is.read()) != -1) {
                baos.write(b);
            }
        } finally {
            close(os);
            close(is);
            close(baos);
            s.close();
        }
        return new String(baos.toByteArray());
    }

    private void createDummyProtocolElements() {
        report("create-dummy-protocol", asadmin("create-protocol", "--target", "c1",
            dummyName));
        report("create-protocol-finder-dummy", asadmin("create-protocol-finder",
            "--target", "c1",
            "--protocol", puName,
            "--target-protocol", dummyName,
            "--classname", DummyProtocolFinder.class.getName(),
            "dummy-finder"));
        report("create-protocol-filter-dummy", asadmin("create-protocol-filter",
            "--target", "c1",
            "--protocol", dummyName,
            "--classname", DummyProtocolFilter.class.getName(),
            "dummy-filter"));
    }

    private void createHttpElements() {
        report("create-http-protocol", asadmin("create-protocol", "--target", "c1",
            httpName));
        report("create-http", asadmin("create-http",
            "--target", "c1",
            "--default-virtual-server", "server",
            httpName));
        report("create-protocol-finder-http", asadmin("create-protocol-finder",
            "--target", "c1",
            "--protocol", puName,
            "--target-protocol", httpName,
            "--classname", HttpProtocolFinder.class.getName(),
            "http-finder"));
    }

    private void deletePUElements() {
        report("delete-http-protocol", asadmin("delete-protocol", "--target", "c1",
            httpName));
        report("delete-protocol-finder-http", asadmin("delete-protocol-finder",
            "--target", "c1",
            "--protocol", puName,
            "http-finder"));
        report("delete-protocol-finder-dummy", asadmin("delete-protocol-finder",
            "--target", "c1",
            "--protocol", puName,
            "dummy-finder"));
        report("delete-protocol-filter-dummy", asadmin("delete-protocol-filter",
            "--target", "c1",
            "--protocol", dummyName,
            "dummy-filter"));
        report("delete-dummy-protocol", asadmin("delete-protocol",
            "--target", "c1",
            dummyName));
        report("delete-pu-protocol", asadmin("delete-protocol",
            "--target", "c1",
            puName));
    }

    private void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }
}
