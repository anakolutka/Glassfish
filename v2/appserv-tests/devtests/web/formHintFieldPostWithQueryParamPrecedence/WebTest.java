import java.io.*;
import java.net.*;
import com.sun.ejte.ccl.reporter.*;

/*
 * Unit test for 6346738 ("getParameter() fails to return correct paramter
 * when locale-charset used QueryString not considered"):
 *
 * Make sure query param takes precedence (i.e., is returned as the first
 * element by ServletRequest.getParameterValues()) over param with same name in
 * POST body even when form-hint-field has been declared in sun-web.xml (which
 * causes the POST body to be parsed in order to determine request encoding).
 */
public class WebTest {

    private static final String TEST_NAME =
        "form-hint-field-post-with-query-param-precedence";

    private static final String EXPECTED_RESPONSE = "value1,value2";

    private static SimpleReporterAdapter stat
        = new SimpleReporterAdapter("appserv-tests");

    private String host;
    private String port;
    private String contextRoot;

    public WebTest(String[] args) {
        host = args[0];
        port = args[1];
        contextRoot = args[2];
    }
    
    public static void main(String[] args) {

        stat.addDescription("Unit test for 6346738");
        WebTest webTest = new WebTest(args);

        try {
            webTest.doTest();
        } catch (Exception ex) {
            ex.printStackTrace();
            stat.addStatus(TEST_NAME, stat.FAIL);
        }

	stat.printSummary();
    }

    public void doTest() throws Exception {

        String body = "param1=value2";

        // Create a socket to the host
        Socket socket = new Socket(host, new Integer(port).intValue());
    
        // Send header
        BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(
                                            socket.getOutputStream()));
        wr.write("POST " + contextRoot + "/TestServlet?param1=value1"
                 + " HTTP/1.0\r\n");
        wr.write("Content-Length: " + body.length() + "\r\n");
        wr.write("Content-Type: application/x-www-form-urlencoded\r\n");
        wr.write("\r\n");
    
        // Send body
        wr.write(body);
        wr.flush();

        // Read response
        BufferedReader bis = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        String line = null;
        String lastLine = null;
        while ((line = bis.readLine()) != null) {
            lastLine = line;
        }

        if (!EXPECTED_RESPONSE.equals(lastLine)) {
            System.err.println("Wrong response. Expected: "
                               + EXPECTED_RESPONSE
                               + ", received: " + lastLine);
            stat.addStatus(TEST_NAME, stat.FAIL);
        } else {
            stat.addStatus(TEST_NAME, stat.PASS);
        }
    }
}
