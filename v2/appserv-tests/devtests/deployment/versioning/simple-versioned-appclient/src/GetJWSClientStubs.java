package versionedappclient.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Retrieve and store at the given place (arg[1]) the client stubs and the application
 * bits of the currently enabled version of the appclient available at the given URL (arg[0]).
 *
 * This is not the real client class, this class is only used to retrieve
 * the stubs of the currently enabled version. This is because the
 * "asadmin get-client-stubs" command is not aware of the enabled state.
 *
 * This class retrieve the stubs from the JWS URL of the application.
 *
 * @author Romain GRECOURT - SERLI (romain.grecourt@serli.com)
 */
public class GetJWSClientStubs {

    String javaWebStartUrl;
    String stubsPath;

    public GetJWSClientStubs(String[] args) {
        if (args.length != 2) {
            usage();
            System.exit(1);
        } else {
            javaWebStartUrl = args[0];
            stubsPath = args[1];
        }
    }

    // extract the value attribute of the agent.args jnlp xml element
    private String extractJnlpAgentArgsValue(File xmlFile) throws ParserConfigurationException, SAXException,
            IOException, XPathExpressionException {
        String agentArgsValue = null;
        if (xmlFile != null && xmlFile.canRead()) {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true); // never forget this!
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr =
                    xpath.compile("//property[@name='agent.args']/@value");

            Object result = expr.evaluate(doc, XPathConstants.NODESET);

            // retrieve the result
            NodeList nodes = (NodeList) result;
            if (nodes.getLength() == 1) {
                agentArgsValue = nodes.item(0).getNodeValue();
            }
        }
        return agentArgsValue;
    }

    // extract the url from the value attribute of agent.args jnlp xml element
    private String extractAgentArgsValueUrl(String value) {
        String stubsUrl = null;
        String searchPattern = "Client.jar";

        if (value != null && !value.isEmpty()) {
            StringTokenizer st = new StringTokenizer(value, ",");

            while (st.hasMoreTokens()) {
                String curToken = (String) st.nextElement();
                if (curToken.contains(searchPattern)) {
                    st = new StringTokenizer(curToken, "=");
                    while (st.hasMoreElements()) {
                        curToken = (String) st.nextElement();
                        if (curToken.contains(searchPattern)) {
                            stubsUrl = curToken;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        return stubsUrl;
    }

    private File get(String url, String workingDir) throws IOException {
        File result = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        if (url != null && !url.isEmpty()) {
            try {
                URL u = new URI(url).normalize().toURL();

                HttpURLConnection c1 = (HttpURLConnection) u.openConnection();
                int code = c1.getResponseCode();

                if (code == 200) {
                    if (workingDir != null) {
                        File wd = new File(workingDir);
                        if (!wd.isDirectory()) {
                            wd = wd.getParentFile();
                        }
                        String stubsFileName = u.getPath();
                        stubsFileName =
                                stubsFileName.substring(stubsFileName.lastIndexOf("/") + 1);

                        result = new File(wd, stubsFileName);
                        if (result.exists()) {
                            result.delete();
                        }
                    } else {
                        result = File.createTempFile("jws", null);
                    }

                    InputStream is = c1.getInputStream();
                    bis = new BufferedInputStream(is);
                    fos = new FileOutputStream(result);

                    byte[] buf = new byte[256];
                    int readStatus;
                    while ((readStatus = bis.read(buf)) != -1) {
                        fos.write(buf, 0, readStatus);
                    }
                } else {
                    log("bad http code: " + code);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (bis != null) {
                    bis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            }
        }
        return result;
    }

    private String makeJWSAppURLFromStubsURL(String stubsURL) {
        String globalAppURL =
                stubsURL.substring(0, stubsURL.lastIndexOf("/") + 1);
        String appclientAbsoluteName =
                stubsURL.substring(stubsURL.lastIndexOf("/") + 1, stubsURL.lastIndexOf("."));
        String untaggedName =
                appclientAbsoluteName.substring(0, appclientAbsoluteName.lastIndexOf("Client"));

        return globalAppURL.concat(appclientAbsoluteName).concat("/").concat(untaggedName).concat(".jar");
    }

    public void process() {
        try {
            log("trying to retrieve the jnlp file available at: " + javaWebStartUrl);
            // get the jnlp file available at the JWS url
            File jnlpFile = get(javaWebStartUrl, null);
            if (jnlpFile.exists()) {
                log("jnlp file retrieved!");
            }
            // parse the jnlp file
            String agentArgsValue = extractJnlpAgentArgsValue(jnlpFile);
            if (agentArgsValue != null) {
                log("xpath request ran OK!");
                // retrieve the stubs url of the currently enabled version
                String currentlyEnabledStubsUrl =
                        extractAgentArgsValueUrl(agentArgsValue);
                if (currentlyEnabledStubsUrl != null) {
                    log("stubs url retrieved: "+currentlyEnabledStubsUrl);
                    // get the stubs of the currently enabled version
                    File stubsFile = get(currentlyEnabledStubsUrl, stubsPath);
                    if (stubsFile != null && stubsFile.canRead()) {
                        log("clientStubs saved at: " + stubsFile.getPath());
                        File workingDir = new File(stubsPath);
                        if (!workingDir.isDirectory()) {
                            workingDir = workingDir.getParentFile();
                        }
                        String stubsFileName = stubsFile.getName();
                        stubsFileName =
                                stubsFileName.substring(0, stubsFileName.lastIndexOf(".jar"));
                        File appFileDir = new File(workingDir, stubsFileName);
                        if (appFileDir.exists()) {
                            appFileDir.delete();
                        }
                        appFileDir.mkdir();
                        String currentlyEnabledAppURL =
                                makeJWSAppURLFromStubsURL(currentlyEnabledStubsUrl);
                        File appFile = get(currentlyEnabledAppURL, appFileDir.getPath());
                        if (appFile != null && appFile.canRead()) {
                            log("clientApp saved at: " + appFile.getPath());
                            log("return code (0): everything is OK");
                            System.exit(0);
                        }
                    } else {
                        log("an error occured when downloading the stubs jar");
                    }
                } else {
                    log("an error occured when trying to retrieve the stubs URL");
                }
            } else {
                log("an error occured during XPATH request execution");
            }
        } catch (Exception ex) {
            log(ex.getLocalizedMessage());
        }
        log("return code (1): an error occured");
        System.exit(1);
    }

    private void log(String message) {
        System.err.println("[versionedappclient.client.GetJWSClientStubs]:: " + message);
    }

    private void usage() {
        System.out.println("arg1: JavaWebStartUrl - arg2: DirPath");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        GetJWSClientStubs client = new GetJWSClientStubs(args);
        client.process();
    }
}
