import java.io.*;
import java.net.*;
import com.sun.ejte.ccl.reporter.*;

/*
 * Unit test for http://forums.java.net/jive/thread.jspa?messageID=299899
 * 
 * Make sure that session established by FormAuthenticator may be accessed
 * (resumed) by protected resource (in this case, AccessSession servlet) even
 * if the original request that caused a (re)login contains a cookie with an
 * invalid JSESSIONID.
 */
public class WebTest {

    private static final String TEST_NAME = "form-login-access-session-on-resumed-request";
    private static final String JSESSIONID = "JSESSIONID";

    private static SimpleReporterAdapter stat
        = new SimpleReporterAdapter("appserv-tests");

    private String host;
    private String port;
    private String contextRoot;
    private String adminUser;
    private String adminPassword;
    private String jsessionId;

    public WebTest(String[] args) {
        host = args[0];
        port = args[1];
        contextRoot = args[2];
        adminUser = args[3];
        adminPassword = args[4];
    }
    
    public static void main(String[] args) {

        stat.addDescription("Unit test that accesses session established by " +
                            "FormAuthenticator");
        WebTest webTest = new WebTest(args);

        try {
            webTest.run();
            stat.addStatus(TEST_NAME, stat.PASS);
        } catch( Exception ex) {
            ex.printStackTrace();
            stat.addStatus(TEST_NAME, stat.FAIL);
        }

	stat.printSummary();
    }

    public void run() throws Exception {

        jsessionId = accessServlet();
        String redirect = accessLoginPage();
        followRedirect(new URL(redirect).getPath());
    }

    /*
     * Attempt to access servlet resource protected by FORM based login.
     */
    private String accessServlet() throws Exception {

        Socket sock = new Socket(host, new Integer(port).intValue());
        OutputStream os = sock.getOutputStream();
        String get = "GET " + contextRoot + "/AccessSession HTTP/1.0\n";
        System.out.print(get);
        os.write(get.getBytes());
        String sendCookie = "Cookie: JSESSIONID=AABBCCDDEEFFGGHH\n";
        System.out.println(sendCookie);
        os.write(sendCookie.getBytes());
        os.write("\r\n".getBytes());
        
        InputStream is = sock.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String location = null;
        String cookie = null;
        String line = null;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            if (line.startsWith("Location:")) {
                location = line;
            } else if (line.startsWith("Set-Cookie")) {
                cookie = line;
            }
        }

        if (location == null) {
            throw new Exception("Missing Location response header");
        }

        if (cookie == null) {
            throw new Exception("Missing Set-Cookie response header");
        }

        return getSessionIdFromCookie(cookie, JSESSIONID);
    }

    /*
     * Access login.jsp.
     */
    private String accessLoginPage() throws Exception {

        Socket sock = new Socket(host, new Integer(port).intValue());
        OutputStream os = sock.getOutputStream();
        String get = "GET " + contextRoot
            + "/j_security_check?j_username=" + adminUser
            + "&j_password=" + adminPassword
            + " HTTP/1.0\n";
        System.out.println(get);
        os.write(get.getBytes());
        String cookie = "Cookie: " + jsessionId + "\n";
        os.write(cookie.getBytes());
        os.write("\r\n".getBytes());
        
        InputStream is = sock.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String location = null;
        String line = null;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            if (line.startsWith("Location:")) {
                location = line;
            }
        }

        if (location == null) {
            throw new Exception("Missing Location response header");
        }

        return location.substring("Location:".length()).trim();
    }

    /*
     * Follow redirect to original URL
     */
    private void followRedirect(String path) throws Exception {

        Socket sock = new Socket(host, new Integer(port).intValue());
        OutputStream os = sock.getOutputStream();
        String get = "GET " + path + " HTTP/1.0\n";
        System.out.print(get);
        os.write(get.getBytes());
        String sendCookie = "Cookie: " + jsessionId + "\n";
        System.out.println(sendCookie);
        os.write(sendCookie.getBytes());
        os.write("\r\n".getBytes());
        
        InputStream is = sock.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String response = null;
        String line = null;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            if (line.startsWith("JSESSIONID")) {
                response = line;
            }
        }

        if (!jsessionId.equals(response)) {
            throw new Exception("Missing response: " + jsessionId);
        }
    }

    private String getSessionIdFromCookie(String cookie, String field) {

        String ret = null;

        int index = cookie.indexOf(field);
        if (index != -1) {
            int endIndex = cookie.indexOf(';', index);
            if (endIndex != -1) {
                ret = cookie.substring(index, endIndex);
            } else {
                ret = cookie.substring(index);
            }
            ret = ret.trim();
        }

        return ret;
    }
}
