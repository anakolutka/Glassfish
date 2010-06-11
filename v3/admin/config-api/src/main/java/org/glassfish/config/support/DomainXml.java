/*
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2010 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.config.support;

import com.sun.enterprise.universal.Duration;
import com.sun.enterprise.universal.NanoDuration;
import com.sun.enterprise.util.EarlyLogger;
import org.glassfish.server.ServerEnvironmentImpl;
import com.sun.enterprise.module.bootstrap.Populator;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.hk2.component.ExistingSingletonInhabitant;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.DomDocument;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamConstants;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import org.glassfish.api.admin.RuntimeType;


/**
 * Locates and parses the portion of <tt>domain.xml</tt> that we care.
 *
 * @author Jerome Dochez
 * @author Kohsuke Kawaguchi
 * @author Byron Nevins
 */
public abstract class DomainXml implements Populator {

    @Inject
    StartupContext context;

    @Inject
    protected Habitat habitat;

    @Inject
    ModulesRegistry registry;

    @Inject
    XMLInputFactory xif;

    @Inject
    ServerEnvironmentImpl env;

    @Override
    public void run(ConfigParser parser) {
            EarlyLogger.add(Level.FINE, "Startup class : " + this.getClass().getName());

        habitat.addComponent("parent-class-loader",
                new ExistingSingletonInhabitant<ClassLoader>(ClassLoader.class, registry.getParentClassLoader()));

        try {
             parseDomainXml(parser, getDomainXml(env), env.getInstanceName());
        } catch (IOException e) {
            // TODO: better exception handling scheme
            throw new RuntimeException("Failed to parse domain.xml",e);
        }

        // run the upgrades...
        if ("upgrade".equals(context.getPlatformMainServiceName())) {
            upgrade();
        }

        decorate();
    }

    protected void decorate() {

        Server server = habitat.getComponent(Server.class, env.getInstanceName());
        habitat.addIndex(new ExistingSingletonInhabitant<Server>(server),
                         Server.class.getName(), ServerEnvironment.DEFAULT_INSTANCE_NAME);

        habitat.addIndex(new ExistingSingletonInhabitant<Config>(habitat.getComponent(Config.class, server.getConfigRef())),
                         Config.class.getName(), ServerEnvironment.DEFAULT_INSTANCE_NAME);
        
    }

    protected void upgrade() {
        
        // run the upgrades...
        for (Inhabitant<? extends ConfigurationUpgrade> cu : habitat.getInhabitants(ConfigurationUpgrade.class)) {
            try {
                cu.get(); // run the upgrade                
                EarlyLogger.add(Level.FINE, "Successful Upgrade domain.xml with " + cu.getClass());
            } catch (Exception e) {
                EarlyLogger.add(Level.FINE,e.toString()+e);
                EarlyLogger.add(Level.SEVERE, cu.getClass() + " upgrading domain.xml failed " + e);
            }
        }
    }

    /**
     * Determines the location of <tt>domain.xml</tt> to be parsed.
     */
    protected URL getDomainXml(ServerEnvironmentImpl env) throws IOException {
        return new File(env.getConfigDirPath(), ServerEnvironmentImpl.kConfigXMLFileName).toURI().toURL();
    }


    /**
     * Parses <tt>domain.xml</tt>
     */
    protected void parseDomainXml(ConfigParser parser, final URL domainXml, final String serverName) {
        long startNano = System.nanoTime();

        try {
            ServerReaderFilter xsr = null;

            if(env.getRuntimeType() == RuntimeType.DAS)
                xsr = new DasReaderFilter(habitat, domainXml, xif);
            else if(env.getRuntimeType() == RuntimeType.INSTANCE)
                xsr = new InstanceReaderFilter(env.getInstanceName(), habitat, domainXml, xif);
            else
                throw new RuntimeException("Internal Error: Unknown server type: "
                        + env.getRuntimeType());

            parser.parse(xsr, getDomDocument());
            xsr.close();
            String errorMessage = xsr.configWasFound();

            if(errorMessage != null)
                EarlyLogger.add(Level.WARNING, errorMessage);
        } catch (Exception e) {
            if(e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new RuntimeException("Fatal Error.  Unable to parse " + domainXml, e);
        }
        EarlyLogger.add(Level.INFO, strings.get("time", new NanoDuration(System.nanoTime() - startNano).toString()));
    }

    protected abstract DomDocument getDomDocument();
    private final static LocalStringsImpl strings = new LocalStringsImpl(DomainXml.class);
}
