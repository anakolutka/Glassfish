import java.io.*;
import java.net.*;
import com.sun.ejte.ccl.reporter.*;

/*
 * This unit tests expects the following Set-Cookie response header, based on 
 * the cookie-properties specified in sun-web.xml:
 *
 *   Set-Cookie: JSESSIONID=[...]; Domain=.iplanet.com; Path=/
 *
 * This test does not make any assumption about the order of the cookie
 * attributes.
 */
public class WebTest {

    private static SimpleReporterAdapter stat
        = new SimpleReporterAdapter("appserv-tests");
    private static final String TEST_NAME = "web-cookie-properties";

    /*
     * Cookie path.
     * Make sure this matches the value specified in sun-web.xml.
     */
    private static final String COOKIE_PATH = "/";

    /*
     * Cookie domain.
     * Make sure this matches the value specified in sun-web.xml.
     */
    private static final String COOKIE_DOMAIN = ".iplanet.com";

    /*
     * Cookie comment.
     * Make sure this matches the value specified in sun-web.xml.
     */
    private static final String COOKIE_COMMENT
        = URLEncoder.encode("Sun-Java-System/Application-Server-PE-8.0 Session Tracking Cookie");

    private String host;
    private String port;
    private String contextRoot;
    private boolean fail;

    public WebTest(String[] args) {
        host = args[0];
        port = args[1];
        contextRoot = args[2];
    }
    
    public static void main(String[] args) {
        stat.addDescription("Unit test for Bugtraq 4924948");
        WebTest webTest = new WebTest(args);
        webTest.doTest();
        stat.printSummary(TEST_NAME);
    }

    public void doTest() {
     
        try { 
            invokeJsp();
        } catch (Exception ex) {
            System.out.println(TEST_NAME + " test failed");
            stat.addStatus(TEST_NAME, stat.FAIL);
            ex.printStackTrace();
        }

        if (!fail) {
            stat.addStatus(TEST_NAME, stat.PASS);
        }

        return;
    }

    private void invokeJsp() throws Exception {

        URL url = new URL("http://" + host  + ":" + port + contextRoot
                          + "/jsp/test.jsp");
        System.out.println("Invoking URL: " + url.toString());

        URLConnection conn = url.openConnection();
        String cookie = conn.getHeaderField("Set-Cookie");
        System.out.println("Response Set-Cookie: " + cookie);

        if (cookie != null) {

            // Check cookie domain
            String domain = getCookieField(cookie, "Domain=");
            if (domain != null) {
                if (!domain.equals(COOKIE_DOMAIN)) {
                    System.err.println("Wrong domain: " + domain
                                       + ", expected: " + COOKIE_DOMAIN);
                    stat.addStatus(TEST_NAME, stat.FAIL);
                    fail = true;
                }
	    } else {
                System.err.println("Missing cookie domain");
                stat.addStatus(TEST_NAME, stat.FAIL);
                fail = true;
            }

            // Check cookie path
            String path = getCookieField(cookie, "Path=");
            if (path != null) {
                if (!path.equals(COOKIE_PATH)) {
                    System.err.println("Wrong path: " + path
                                       + ", expected: " + COOKIE_PATH);
                    stat.addStatus(TEST_NAME, stat.FAIL);
                    fail = true;
                }
	    } else {
                System.err.println("Missing cookie path");
                stat.addStatus(TEST_NAME, stat.FAIL);
                fail = true;
            }

            // Check cookie comment
            String comment = getCookieField(cookie, "Comment=");
            if (comment != null) {
                if (!comment.equals(COOKIE_COMMENT)) {
                    System.err.println("Wrong comment: " + comment
                                       + ", expected: " + COOKIE_COMMENT);
                    stat.addStatus(TEST_NAME, stat.FAIL);
                    fail = true;
                }
	    } else {
                System.err.println("Missing cookie comment");
                stat.addStatus(TEST_NAME, stat.FAIL);
                fail = true;
            }

        } else {
            System.err.println("Missing Set-Cookie response header");
            stat.addStatus(TEST_NAME, stat.FAIL);
            fail = true;
        }
    }

    private String getCookieField(String cookie, String field) {

        String ret = null;

        int index = cookie.indexOf(field);
        if (index != -1) {
            int endIndex = cookie.indexOf(';', index);
            if (endIndex != -1) {
                ret = cookie.substring(index + field.length(), endIndex);
            } else {
                ret = cookie.substring(index + field.length());
            }
            ret = ret.trim();
        }

        return ret;
    }
}
