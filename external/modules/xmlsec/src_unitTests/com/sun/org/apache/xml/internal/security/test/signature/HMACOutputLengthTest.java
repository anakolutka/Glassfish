package com.sun.org.apache.xml.internal.security.test.signature;

import java.io.File;
import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.org.apache.xml.internal.security.Init;
import com.sun.org.apache.xml.internal.security.c14n.Canonicalizer;
import com.sun.org.apache.xml.internal.security.signature.XMLSignature;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureException;
import com.sun.org.apache.xml.internal.security.utils.Constants;

public class HMACOutputLengthTest extends TestCase {

    private static DocumentBuilderFactory dbf = null;

    protected void setUp() throws Exception {
        Init.init();
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
    }

    /** {@link org.apache.commons.logging} logging facility */
    static org.apache.commons.logging.Log log =
        org.apache.commons.logging.LogFactory.getLog
            (HMACOutputLengthTest.class.getName());

    private static final String BASEDIR = System.getProperty("basedir");
    private static final String SEP = System.getProperty("file.separator");

    public static Test suite() {
        return new TestSuite(HMACOutputLengthTest.class);
    }

    public HMACOutputLengthTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        String[] testCaseName = { "-noloading",
                                  HMACOutputLengthTest.class.getName() };

        junit.textui.TestRunner.main(testCaseName);
    }

    public void test_signature_enveloping_hmac_sha1_trunclen_0() throws Exception {
        try {
            validate("signature-enveloping-hmac-sha1-trunclen-0-attack.xml");
            fail("Expected HMACOutputLength exception");
        } catch (XMLSignatureException xse) {
            System.out.println(xse.getMessage());
            if (xse.getMsgID().equals("algorithms.HMACOutputLengthMin")) {
                // pass
            } else {
                fail(xse.getMessage());
            }
        }
    }
    public void test_signature_enveloping_hmac_sha1_trunclen_8() throws Exception {
        try {
            validate("signature-enveloping-hmac-sha1-trunclen-8-attack.xml");
        } catch (XMLSignatureException xse) {
            System.out.println(xse.getMessage());
            if (xse.getMsgID().equals("algorithms.HMACOutputLengthMin")) {
                // pass
            } else {
                fail(xse.getMessage());
            }
        }
    }

    private static void validate(String data) throws Exception {
        System.out.println("Validating " + data);
        File file = new File(BASEDIR + SEP + "data" + SEP + "javax" + SEP + "xml" + SEP + "crypto" + SEP + "dsig" + SEP, data);

        Document doc = dbf.newDocumentBuilder().parse(file);
        NodeList nl =
            doc.getElementsByTagNameNS(Constants.SignatureSpecNS, "Signature");
        if (nl.getLength() == 0) {
            throw new Exception("Couldn't find signature Element");
        }
        Element sigElement = (Element) nl.item(0);
        XMLSignature signature = new XMLSignature
            (sigElement, file.toURI().toString());
        SecretKey sk = signature.createSecretKey("secret".getBytes("ASCII"));
        System.out.println
            ("Validation status: " + signature.checkSignatureValue(sk));
    }

    public void test_generate_hmac_sha1_40() throws Exception {
        System.out.println("Generating ");

        Document doc = dbf.newDocumentBuilder().newDocument();
        XMLSignature sig = new XMLSignature
            (doc, null, XMLSignature.ALGO_ID_MAC_HMAC_SHA1, 40,
             Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS);
        try {
            sig.sign(getSecretKey("secret".getBytes("ASCII")));
            fail("Expected HMACOutputLength Exception");
        } catch (XMLSignatureException xse) {
            System.out.println(xse.getMessage());
            if (xse.getMsgID().equals("algorithms.HMACOutputLengthMin")) {
                // pass
            } else {
                fail(xse.getMessage());
            }
        }
    }

    private static SecretKey getSecretKey(final byte[] secret) {
        return new SecretKey() {
            public String getFormat()   { return "RAW"; }
            public byte[] getEncoded()  { return secret; }
            public String getAlgorithm(){ return "SECRET"; }
        };
    }
}
