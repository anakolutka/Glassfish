/*
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
import java.io.*;
import java.util.Properties;
import java.net.*;
import java.security.KeyStore;
import javax.net.*;
import javax.net.ssl.*;
import com.sun.ejte.ccl.reporter.*;

public class WebTest{

    static SimpleReporterAdapter stat=
           new SimpleReporterAdapter("appserv-tests");
    private static final String TEST_NAME = "transportProtectedAnnotation";
    
    public static void main(String args[]) throws Exception{
        String host = args[0];
        String httpPort = args[1];
        String httpsPort = args[2];
        String contextRoot = args[3];
        String trustStorePath = args[4];

        stat.addDescription("Testing @TransportProtected");

        try {
            SSLSocketFactory ssf = getSSLSocketFactory(trustStorePath);

            boolean ok = testURL("GET", "https://" + host + ":" + httpsPort + "/" + contextRoot + "/myurl",
                    ssf, false, "c:Hello:true");
            ok = ok && testURL("GET", "https://" + host + ":" + httpsPort + "/" + contextRoot + "/myurl2",
                    ssf, false, "m:Hello:true");
            ok = ok && testURL("TRACE", "http://" + host + ":" + httpPort + "/" + contextRoot + "/myurl2",
                    null, true, "mfr:Hello:javaee:false");
            ok = ok && testURL("GET", "https://" + host + ":" + httpsPort + "/" + contextRoot + "/myurl3",
                    ssf, true, "g:Hello:javaee:true");
            
            ok = ok && testURL("TRACE", "http://" + host + ":" + httpPort + "/" + contextRoot + "/myurl3",
                    null, true, "t:Hello:javaee:false");
            stat.addStatus(TEST_NAME, ((ok)? stat.PASS : stat.FAIL));
        } catch (Throwable t) {
            stat.addStatus(TEST_NAME, stat.FAIL);
            t.printStackTrace();
        }
        stat.printSummary();
    }

    private static boolean testURL(String httpMethod, String url, SSLSocketFactory ssf,
            boolean needAuthenticate, String expected)
            throws Exception {

        System.out.println("Accessing " + httpMethod + " " + url);
        HttpURLConnection connection = doHandshake(httpMethod, url, ssf, needAuthenticate);
        boolean status = false;
        if (checkStatus(connection)) {
            status = parseResponse(connection, expected);
        }
        return status;
    }

    private static SSLSocketFactory getSSLSocketFactory(String trustStorePath)
                    throws Exception {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, getTrustManagers(trustStorePath), null);
        return sc.getSocketFactory();
    }

    private static HttpURLConnection doHandshake(String httpMethod,
            String urlAddress, SSLSocketFactory ssf,
            boolean needAuthenticate) throws Exception{

        URL url = new URL(urlAddress);
        HttpURLConnection connection = null;

        if (ssf != null) {
            HttpsURLConnection.setDefaultSSLSocketFactory(ssf);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setHostnameVerifier(
                new HostnameVerifier() {
                    public boolean verify(String rserver, SSLSession sses) {
                        return true;
                    }
            });
            
            connection = conn;
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        connection.setRequestMethod(httpMethod);
        connection.setDoOutput(true);
        if (needAuthenticate) {
            connection.setRequestProperty("Authorization", "Basic amF2YWVlOmphdmFlZQ==");
        }
        return connection;
    }

    private static boolean checkStatus(HttpURLConnection connection)
            throws Exception{

        int responseCode =  connection.getResponseCode();
        System.out.println("Response code: " + responseCode + " Expected code: 200"); 
        return (connection.getResponseCode() == 200);
    }

    private static boolean parseResponse(HttpURLConnection connection,
            String expected) throws Exception {

        BufferedReader in = null;
        boolean ok = false;
        try {
            in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
            
            String line = "";
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                if (line.equals(expected)) {
                    ok = true;
                    break;
                }
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                } 
            } catch(IOException ioe) {
                // ignore
            }
        }

        return ok;
    }

    private static TrustManager[] getTrustManagers(String path)
                    throws Exception {

        TrustManager[] tms = null;
        InputStream istream = null;

        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            istream = new FileInputStream(path);
            trustStore.load(istream, null);
            istream.close();
            istream = null;
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(trustStore);
            tms = tmf.getTrustManagers();

        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (IOException ioe) {
                    // Do nothing
                }
            }
        }

        return tms;
    }
}
