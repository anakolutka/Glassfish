 /*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * Header Notice in each file and include the License file 
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.  
 * If applicable, add the following below the CDDL Header, 
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */
package test.admin;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;

import com.sun.appserv.management.client.AMXBooter;
import com.sun.appserv.management.client.ProxyFactory;
import com.sun.appserv.management.DomainRoot;
import java.io.File;

/** The base class for admin console tests. Designed for extension.
 * @author jdlee@dev.java.net
 * @since GlassFish v3 Prelude
 */
public class BaseAdminConsoleTest {

    protected String adminUrl;
    private HttpClient client;
    private static final int AC_TEST_DELAY = 1000; // One second
    private static final int AC_TEST_ITERATIONS = 100; // Ten seconds
    private String host;
    private int port;
    private volatile MBeanServerConnection mMBeanServerConnection;
    private volatile DomainRoot mDomainRoot;
    private static final String FILE_SEP = System.getProperty("file.separator");
    private static final String CONSOLE_ARCHIVE_PATH = FILE_SEP + "lib" + FILE_SEP + 
            "install" + FILE_SEP + "applications" + FILE_SEP + "admingui.war";

    // Copied from Lloyd's AMX tests
    void setUpEnvironment(int port) {
        try {
            if (mMBeanServerConnection == null) {
                host = System.getProperty("http.host");
                this.port = port;

                mMBeanServerConnection = _getMBeanServerConnection();
                mDomainRoot = _getDomainRoot(mMBeanServerConnection);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This BeforeTest method will verify that the login form is available.  Once
     * it is found, the login form is submitted.  If the login succeeds, then
     * the tests are allowed to continue.  If the login fails, the each test will
     * fail.
     * @param url
     * @throws java.lang.Exception
     */
    @BeforeTest
    @Parameters({"admin.console.url", "amx.rmiport"})
    public void loginBeforeTest(String url, int port) throws Exception {
        this.adminUrl = url;
        setUpEnvironment(port);
        client = new HttpClient();

        boolean formFound = false;
        int iteration = 0;
        Assert.assertTrue(checkForAdminConsoleArchive(), "The admin console archive was not found.  Please check your installation.");

        while (!formFound && iteration < AC_TEST_ITERATIONS) {
            iteration++;

            formFound = getUrlAndTestForStrings(adminUrl + "login.jsf","id=\"Login.username\"");
            if (!formFound) {
                System.err.println("***** Login page not found.  Sleeping to allow app to deploy (" +
                        iteration + " of " + AC_TEST_ITERATIONS + ")...");
                Thread.sleep(AC_TEST_DELAY);
            }
        }

        Assert.assertTrue(formFound);

        // The login for was found, so let's now POST the form to authenticate our session.
        PostMethod post = new PostMethod(adminUrl + "j_security_check");
        post.setRequestBody(new NameValuePair[]{
                    new NameValuePair("j_username", "anonymous"), new NameValuePair("j_password", "")
                });
        post.getParams().setCookiePolicy(CookiePolicy.RFC_2109);

        int statusCode = client.executeMethod(post);
        if (statusCode == 302) {
            Header locationHeader = post.getResponseHeader("location");
            if (locationHeader != null) {
                Assert.assertEquals(this.adminUrl, locationHeader.getValue());
            } else {
                Assert.fail("Failed to login: no redirect header");
            }
        } else if (statusCode != HttpStatus.SC_OK) {
            Assert.fail("Login failed: " + post.getStatusLine() + ": " + statusCode);
        }
    }

    /**
     * This method uses the AMX API to get the install directory, then checks to
     * see if the admin console archive is present.
     * @return
     */
    protected boolean checkForAdminConsoleArchive() {
        File archive = new File(this.getDomainRoot().getInstallDir() + CONSOLE_ARCHIVE_PATH);
        return archive.exists();
    }

    @AfterTest
    public void shutdownClient() {
        client = null;
    }

    /**
     * This method will request the specified URL and examine the response for the
     * needle specified.
     * @param url
     * @param needle
     * @return
     * @throws java.lang.Exception
     */
    protected boolean getUrlAndTestForStrings(String url, String... needles) throws IOException {
        String haystack = getUrl(url);
        boolean allFound = true;
        for (String needle : needles) {
            if (haystack.indexOf(needle) == -1) {
                allFound = false;
            }
        }

        return allFound;
    }

    /**
     * Request the specified URL and return the contents as a String
     * @param url
     * @return
     * @throws java.io.IOException
     */
    protected String getUrl(String url) throws IOException {
        GetMethod get = new GetMethod(url);
        get.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
        get.setFollowRedirects(true);

        int statusCode = client.executeMethod(get);
        if (statusCode != HttpStatus.SC_OK) {
            Assert.fail("BaseAdminConsoleTest.getUrlAndTestForString() failed.  HTTP Status Code:  " + statusCode);
        }
        String response = getString(get.getResponseBodyAsStream());
        get.releaseConnection();
        return response;
    }

    /**
     * Read the entire contents of the InputStream and return them as a String
     * @param in
     * @return
     * @throws java.io.IOException
     */
    protected String getString(InputStream in) throws IOException {
        StringBuilder out = new StringBuilder();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            out.append(new String(b, 0, n));
        }
        in.close();
        return out.toString();
    }

    /*
     * These methods were all copied from Lloyd's AMX QL tests.
     */

    protected DomainRoot getDomainRoot() {
        return mDomainRoot;
    }

    protected DomainRoot _getDomainRoot(final MBeanServerConnection conn)
            throws MalformedURLException, IOException, java.net.MalformedURLException {
        final ObjectName domainRootObjectName = AMXBooter.bootAMX(conn);
        final DomainRoot domainRoot = ProxyFactory.getInstance(conn).getDomainRoot();
        return domainRoot;
    }

    private MBeanServerConnection _getMBeanServerConnection()
            throws MalformedURLException, IOException {
        // service:jmx:rmi:///jndi/rmi://192.168.1.8:8686/jmxrmi
        // service:jmx:jmxmp://localhost:8888
        // CHANGE to RMI once it's working
        //
        // final String urlStr = "service:jmx:jmxmp://" + mHost + ":" + mPort;
        final String urlStr = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";

        final JMXServiceURL url = new JMXServiceURL(urlStr);

        final JMXConnector jmxConn = JMXConnectorFactory.connect(url);
        final MBeanServerConnection conn = jmxConn.getMBeanServerConnection();
        conn.getDomains();	// sanity check
        return conn;
    }
}