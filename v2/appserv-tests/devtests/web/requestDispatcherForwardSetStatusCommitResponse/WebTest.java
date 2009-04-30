import java.io.*;
import java.net.*;
import com.sun.ejte.ccl.reporter.*;

/**
 * Unit test for CR 6374990 ("Response is not flushed to browser on
 * RequestDispatcher.forward()"):
 *
 * Make sure that if target of RD.forward() calls
 * HttpServletResponse.setStatus(), with a status code >=400, the status code
 * is not mapped to any error page before the response is committed (error
 * page mapping is supposed to occur only in the case of
 * HttpServletResponse.sendError()).
 *
 * In this case, we also don't want the default error page to be returned,
 * because the target servlet of the RD.forward() (To.java) has already
 * output a response, which is the response that this test client expects in
 * order for this test to pass.
 */
public class WebTest {

    private static SimpleReporterAdapter stat
        = new SimpleReporterAdapter("appserv-tests");

    private static final String TEST_NAME
        = "request-dispatcher-forward-set-status-commit-response";

    private static final String EXPECTED_RESPONSE
        = "This is error message from target servlet";

    private String host;
    private String port;
    private String contextRoot;
    private Socket sock = null;

    public WebTest(String[] args) {
        host = args[0];
        port = args[1];
        contextRoot = args[2];
    }
    
    public static void main(String[] args) {
        stat.addDescription("Unit test for 6374990");
        WebTest webTest = new WebTest(args);
        try {
            webTest.doTest();
            stat.addStatus(TEST_NAME, stat.PASS);
        } catch (Exception ex) {
            stat.addStatus(TEST_NAME, stat.FAIL);
            ex.printStackTrace();
        } finally {
            try {
                if (webTest.sock != null) {
                    webTest.sock.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }

        stat.printSummary(TEST_NAME);

        // Wait until the request has returned, to avoid undeploying this
        // test application prematurely.
        try {
            Thread.currentThread().sleep(10 * 1000);
        } catch (Exception e) {
            // Ignore
        }
    }

    public void doTest() throws Exception {

        sock = new Socket(host, new Integer(port).intValue());
        OutputStream os = sock.getOutputStream();
        String request = "GET " + contextRoot + "/From " + "HTTP/1.0\n";
        System.out.println(request);
        os.write(request.getBytes());
        os.write("\n".getBytes());

        long start = System.currentTimeMillis();
        long end = 0;

        InputStream is = null;
        BufferedReader bis = null;
        try {
            is = sock.getInputStream();
            bis = new BufferedReader(new InputStreamReader(is));
            boolean defaultErrorPage = false;
            String line = null;
            String found = null;
            String firstLine = null;
            while ((line = bis.readLine()) != null) {
                System.out.println(line);
                if (firstLine == null) {
                    firstLine = line;
                }
                if (EXPECTED_RESPONSE.equals(line)) {
                    end = System.currentTimeMillis();
                    found = line;
                }

                if (line.indexOf("DOCTYPE html PUBLIC") != -1) {
                    defaultErrorPage = true;
                }
            }
            if (!firstLine.startsWith("HTTP/1.1 444")) {
                throw new Exception("Unexpected return code: " + firstLine);
            }
            if (defaultErrorPage) {
                throw new Exception("Default error page found in response");
            }
            if (found == null) {
                throw new Exception("Wrong response. Expected: " +
                    EXPECTED_RESPONSE);
            }
            if ((end-start) >= (10*1000)) {
                throw new Exception("Response was delayed by 10 seconds " +
                    "or more, which is how long the " +
                    "origin servlet of the RD.forward() " +
                    "has been sleeping for. " + 
                    "The response should have been " +
                    "committed immediately.");
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }
}
