/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.enterprise.configapi.tests;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.component.Habitat;
import org.glassfish.config.support.ConfigurationPersistence;
import org.glassfish.config.support.GlassFishDocument;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.Config;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * User: Jerome Dochez
 * Date: Mar 12, 2008
 * Time: 8:50:42 PM
 */
public class DirectAccessTest extends ConfigApiTest {

    Habitat habitat;
    
    /**
     * Returns the file name without the .xml extension to load the test configuration
     * from. By default, it's the name of the TestClass.
     *
     * @return the configuration file name
     */
    public String getFileName() {
        return "DomainTest";
    }


    @Before
    public void setup() {
        habitat = Utils.getNewHabitat(getFileName());
    }


    @Test
    public void changedTest() throws TransactionFailure {

        HttpService service = habitat.getComponent(HttpService.class);
        final GlassFishDocument document = habitat.getByType(GlassFishDocument.class);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.reset();


        assertTrue(service!=null);
        final ConfigurationPersistence testPersistence =  new ConfigurationPersistence() {
            public void save(DomDocument doc) throws IOException, XMLStreamException {
                XMLOutputFactory factory = XMLOutputFactory.newInstance();
                XMLStreamWriter writer = factory.createXMLStreamWriter(baos);
                doc.writeTo(new IndentingXMLStreamWriter(writer));
                writer.close();
            }
        };

        TransactionListener testListener = new TransactionListener() {
            public void transactionCommited(List<PropertyChangeEvent> changes) {
                try {
                    testPersistence.save(document);
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (XMLStreamException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        };
        try {
            Transactions.get().addTransactionsListener(testListener);
            ConfigBean config = (ConfigBean) ConfigBean.unwrap(service.getHttpFileCache());
            ConfigBean config2 = (ConfigBean) ConfigBean.unwrap(service.getHttpProtocol());
            Map<ConfigBean, Map<String, String>> changes = new HashMap<ConfigBean, Map<String, String>>();
            Map<String, String> configChanges = new HashMap<String, String>();
            configChanges.put("max-age-in-seconds", "12543");
            configChanges.put("medium-file-size-limit-in-bytes", "1200");
            Map<String, String> config2Changes = new HashMap<String, String>();
            config2Changes.put("version", "12351");
            changes.put(config, configChanges);
            changes.put(config2, config2Changes);

            ConfigSupport.apply(changes);

        } finally {
            Transactions.get().waitForDrain();
            Transactions.get().removeTransactionsListener(testListener);
        }

        // now check if we persisted correctly...

        final String resultingXml = baos.toString();
        logger.fine(resultingXml);
        assertTrue(resultingXml.indexOf("max-age-in-seconds=\"12543\"")!=-1);
        assertTrue(resultingXml.indexOf("version=\"12351\"")!=-1);
    }

    
}
