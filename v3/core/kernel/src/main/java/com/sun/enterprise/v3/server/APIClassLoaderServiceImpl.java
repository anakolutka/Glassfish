/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved.
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


package com.sun.enterprise.v3.server;

import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.ModuleDependency;
import com.sun.enterprise.module.ModuleMetadata;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.logging.LogDomains;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;
import org.osgi.framework.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is responsible for creating a ClassLoader that can
 * load classes exported by any OSGi bundle in the system for public use.
 * Such classes include Java EE API, AMX API, appserv-ext API, etc.
 * CommonClassLoader delegates to this class loader..
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
@Service
public class APIClassLoaderServiceImpl implements PostConstruct {

    /*
     * Implementation Note: This class depends on OSGi runtime, so
     * not portable on HK2.
     */
    private ClassLoader APIClassLoader;
    @Inject
    ModulesRegistry mr;
    private static final String APIExporterModuleName =
            "GlassFish-Application-Common-Module"; // NOI18N
    final static Logger logger = LogDomains.getLogger(LogDomains.LOADER_LOGGER);
    private Module APIModule;

    public void postConstruct() {
        try {
            createAPIClassLoader();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createAPIClassLoader() throws IOException {
        final Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.putValue(Constants.BUNDLE_SYMBOLICNAME, APIExporterModuleName);
        attrs.putValue(Constants.DYNAMICIMPORT_PACKAGE, "*");
        attrs.putValue(Constants.BUNDLE_MANIFESTVERSION, "2"); // R4 spec
        // This is needed, else the manifest will be written out as an empty file
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        final File f = File.createTempFile(APIExporterModuleName, ".jar");
        f.deleteOnExit();
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(f),
                manifest);
        jos.close();
        ModuleDefinition md = new ModuleDefinition() {
            private ModuleMetadata metadata = new ModuleMetadata();

            public String getName() {
                return APIExporterModuleName;
            }

            public String[] getPublicInterfaces() {
                return new String[0];
            }

            public ModuleDependency[] getDependencies() {
                return new ModuleDependency[0];
            }

            public URI[] getLocations() {
                return new URI[]{f.toURI()};
            }

            public String getVersion() {
                return null;
            }

            public String getImportPolicyClassName() {
                return null;
            }

            public String getLifecyclePolicyClassName() {
                return null;
            }

            public Manifest getManifest() {
                return manifest;
            }

            public ModuleMetadata getMetadata() {
                return metadata;
            }
        };
        APIModule = mr.add(md);
        APIClassLoader = APIModule.getClassLoader();
        logger.logp(Level.INFO, "APIClassLoaderService", "createAPIClassLoader",
                "APIClassLoader = {0}", new Object[]{APIClassLoader});
    }

    public ClassLoader getAPIClassLoader() {
        return APIClassLoader;
    }

}
