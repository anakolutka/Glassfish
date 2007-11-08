import java.io.*;
import java.net.*;
import com.sun.ejte.ccl.reporter.*;

/*
 * Unit test for Bugzilla 30067 ("Scripting elements are disallowed here"
 * exception behind scriptless tag)
 */
public class WebTest {

    private static final String TEST_NAME = "jsp-scriptlet-after-scriptless-tag";

    private static SimpleReporterAdapter stat
        = new SimpleReporterAdapter("appserv-tests");

    private static final String EXPECTED = "Scriptless tag output";

    private String host;
    private String port;
    private String contextRoot;

    public WebTest(String[] args) {
        host = args[0];
        port = args[1];
        contextRoot = args[2];
    }
    
    public static void main(String[] args) {
        stat.addDescription("Unit test for Bugzilla 30067");
        WebTest webTest = new WebTest(args);
        webTest.doTest();
	stat.printSummary();
    }

    public void doTest() {
     
        try { 
            URL url = new URL("http://" + host  + ":" + port
                       + contextRoot + "/jsp/test.jspx");
            System.out.println("Connecting to: " + url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) { 
                stat.addStatus("Wrong response code. Expected: 200"
                               + ", received: " + responseCode, stat.FAIL);
            } else {

                BufferedReader bis = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                String line = null;
                String lastLine = null;
                while ((line = bis.readLine()) != null) {
                    lastLine = line;
                }
                if (!EXPECTED.equals(lastLine)) {
                    stat.addStatus("Wrong response body. Expected: " + EXPECTED
                                   + ", received: " + lastLine, stat.FAIL);
                } else {
                    stat.addStatus(TEST_NAME, stat.PASS);
                }
            }
        } catch (Exception ex) {
            System.out.println(TEST_NAME + " test failed.");
            stat.addStatus(TEST_NAME, stat.FAIL);
            ex.printStackTrace();
        }
    }

}
