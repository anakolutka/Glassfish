/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.sun.org.apache.xml.internal.security.samples.encryption;


import java.io.File;
import java.io.FileOutputStream;

import java.security.Key;

import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;

import com.sun.org.apache.xml.internal.security.keys.KeyInfo;
import com.sun.org.apache.xml.internal.security.encryption.XMLCipher;
import com.sun.org.apache.xml.internal.security.encryption.EncryptedData;
import com.sun.org.apache.xml.internal.security.encryption.EncryptedKey;
import com.sun.org.apache.xml.internal.security.utils.Constants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;

/**
 * This sample demonstrates how to encrypt data inside an xml document.
 *
 * @author Vishal Mahajan (Sun Microsystems)
 */
public class Encrypter {

    /** {@link java.util.logging} logging facility */
    static java.util.logging.Logger log = 
        java.util.logging.Logger.getLogger(
            Encrypter.class.getName());

    static {
        com.sun.org.apache.xml.internal.security.Init.init();
    }

    private static Document createSampleDocument() throws Exception {

        javax.xml.parsers.DocumentBuilderFactory dbf =
            javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.newDocument();

        /**
         * Build a sample document. It will look something like:
         *
         * <apache:RootElement xmlns:apache="http://www.apache.org/ns/#app1">
         * <apache:foo>Some simple text</apache:foo>
         * </apache:RootElement>
         */
        Element root =
            document.createElementNS(
                "http://www.apache.org/ns/#app1", "apache:RootElement");
        root.setAttributeNS(
            Constants.NamespaceSpecNS,
            "xmlns:apache",
            "http://www.apache.org/ns/#app1");
        document.appendChild(root);

        root.appendChild(document.createTextNode("\n"));

        Element childElement =
            document.createElementNS(
                "http://www.apache.org/ns/#app1", "apache:foo");
        childElement.appendChild(
            document.createTextNode("Some simple text"));
        root.appendChild(childElement);

        root.appendChild(document.createTextNode("\n"));

        return document;
    }

    private static SecretKey GenerateAndStoreKeyEncryptionKey()
        throws Exception {

        String jceAlgorithmName = "DESede";
        KeyGenerator keyGenerator =
            KeyGenerator.getInstance(jceAlgorithmName);
        SecretKey kek = keyGenerator.generateKey();

        byte[] keyBytes = kek.getEncoded();
        File kekFile = new File("kek");
        FileOutputStream f = new FileOutputStream(kekFile);
        f.write(keyBytes);
        f.close();
        System.out.println(
            "Key encryption key stored in " + kekFile.toURL().toString());

        return kek;
    }

    private static SecretKey GenerateDataEncryptionKey() throws Exception {

        String jceAlgorithmName = "AES";
        KeyGenerator keyGenerator =
            KeyGenerator.getInstance(jceAlgorithmName);
        keyGenerator.init(128);
        return keyGenerator.generateKey();
    }

    private static void outputDocToFile(Document doc, String fileName)
        throws Exception {
        File encryptionFile = new File(fileName);
        FileOutputStream f = new FileOutputStream(encryptionFile);

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(f);
        transformer.transform(source, result);

        f.close();
        System.out.println(
            "Wrote document containing encrypted data to " +
            encryptionFile.toURL().toString());
    }

    public static void main(String unused[]) throws Exception {

        Document document = createSampleDocument();

        /*
         * Get a key to be used for encrypting the element.
         * Here we are generating an AES key.
         */
        Key symmetricKey = GenerateDataEncryptionKey();

        /*
         * Get a key to be used for encrypting the symmetric key.
         * Here we are generating a DESede key.
         */
        Key kek = GenerateAndStoreKeyEncryptionKey();

        String algorithmURI = XMLCipher.TRIPLEDES_KeyWrap;

        XMLCipher keyCipher =
            XMLCipher.getInstance(algorithmURI);
        keyCipher.init(XMLCipher.WRAP_MODE, kek);
        EncryptedKey encryptedKey =
            keyCipher.encryptKey(document, symmetricKey);

        /*
         * Let us encrypt the contents of the document element.
         */
        Element rootElement = document.getDocumentElement();

        algorithmURI = XMLCipher.AES_128;

        XMLCipher xmlCipher =
            XMLCipher.getInstance(algorithmURI);
        xmlCipher.init(XMLCipher.ENCRYPT_MODE, symmetricKey);

        /*
         * Setting keyinfo inside the encrypted data being prepared.
         */
        EncryptedData encryptedData = xmlCipher.getEncryptedData();
        KeyInfo keyInfo = new KeyInfo(document);
        keyInfo.add(encryptedKey);
        encryptedData.setKeyInfo(keyInfo);

        /*
         * doFinal -
         * "true" below indicates that we want to encrypt element's content
         * and not the element itself. Also, the doFinal method would
         * modify the document by replacing the EncrypteData element
         * for the data to be encrypted.
         */
        xmlCipher.doFinal(document, rootElement, true);

        /*
         * Output the document containing the encrypted information into
         * a file.
         */
        outputDocToFile(document, "encryptedInfo.xml");
    }
}
