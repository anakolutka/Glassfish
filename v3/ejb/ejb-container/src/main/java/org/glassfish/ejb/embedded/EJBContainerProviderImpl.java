/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.ejb.embedded;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

import javax.ejb.EJBException;
import javax.ejb.embeddable.EJBContainer;
import javax.ejb.spi.EJBContainerProvider;

import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.embedded.*;

import org.glassfish.deployment.common.GenericAnnotationDetector;
import org.glassfish.deployment.common.DeploymentUtils;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.io.EjbDeploymentDescriptorFile;
import com.sun.enterprise.deployment.util.ModuleDescriptor;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import com.sun.enterprise.security.embedded.EmbeddedSecurityUtil;
import org.glassfish.server.ServerEnvironmentImpl;

import org.jvnet.hk2.component.Habitat;
import com.sun.enterprise.module.bootstrap.Which;

/**
 * GlassFish implementation of the EJBContainerProvider.
 *
 * @author Marina Vatkina
 */
public class EJBContainerProviderImpl implements EJBContainerProvider {

    static final String KEEP_TEMPORARY_FILES = "org.glassfish.ejb.embedded.keep-temporary-files";

    private static final String GF_PROVIDER_NAME = EJBContainerProviderImpl.class.getName();
    private static final String GF_INSTALLATION_ROOT = "org.glassfish.ejb.embedded.glassfish.installation.root";
    private static final String GF_INSTANCE_ROOT = "org.glassfish.ejb.embedded.glassfish.instance.root";
    private static final String GF_DOMAIN_FILE = "org.glassfish.ejb.embedded.glassfish.configuration.file";
    private static final Attributes.Name ATTRIBUTE_NAME_SKIP = new Attributes.Name("Bundle-SymbolicName");
    private static final String[] ATTRIBUTE_VALUES_SKIP = 
            {"org.glassfish.", "com.sun.enterprise.", "org.eclipse."};
    private static final String[] ATTRIBUTE_VALUES_OK = {"sample", "test"};

    static final String GF_WEB_HTTP_PORT = "org.glassfish.ejb.embedded.glassfish.web.http.port";

    // Use Bundle from another package
    private static final Logger _logger = 
            LogDomains.getLogger(EJBContainerProviderImpl.class, LogDomains.EJB_LOGGER);
    private static final StringManager localStrings = 
            StringManager.getManager(EJBContainerProviderImpl.class);

    private static final Object lock = new Object();

    private static EJBContainerImpl container;
    private static Server server;
    private static Habitat habitat;
    private static ArchiveFactory archiveFactory;
    private static Class[] ejbAnnotations = null;

    public EJBContainerProviderImpl() {}

    public EJBContainer createEJBContainer(Map<?, ?> properties) throws EJBException {
        if (properties == null || properties.get(EJBContainer.PROVIDER) == null || 
                properties.get(EJBContainer.PROVIDER).equals(GF_PROVIDER_NAME)) {

            if (container != null && container.isOpen()) {
                throw new EJBException(localStrings.getString(
                        "ejb.embedded.exception_exists_container"));
            }

            init(properties);
            try {
                Set<DeploymentElement> modules = addModules(properties);
                if (!DeploymentElement.hasEJBModule(modules)) {
                    _logger.log(Level.SEVERE, "ejb.embedded.no_modules_found");
                } else {
                    container.deploy(properties, modules);
                }
                return container;
            } catch (Throwable t) {
                // Can't throw an exception - only return null.
                if (container != null) {
                    try {
                        _logger.info("[EJBContainerProviderImpl] Cleaning up on failure ...");
                        container.close();
                    } catch (Throwable t1) {
                        _logger.info("[EJBContainerProviderImpl] Error cleaning up..." + t1);
                    }
                }
                _logger.log(Level.SEVERE, "ejb.embedded.exception_instantiating", t);
            }
        }

        return null;
    }

    private void init(Map<?, ?> properties) throws EJBException {
        synchronized(lock) {
            // if (container == null || !container.isOpen()) {
                Server.Builder builder = new Server.Builder("GFEJBContainerProviderImpl");

                Result rs = getLocations(properties);
                if (rs == null) {
                    server = builder.build();
                    addWebContainerIfRequested(properties);
                } else {
                    EmbeddedFileSystem.Builder efsb = new EmbeddedFileSystem.Builder();
                    efsb.configurationFile(rs.domain_file);
                    efsb.installRoot(rs.installed_root, true);

                    builder.embeddedFileSystem(efsb.build());
                    server = builder.build();

                }

                EjbBuilder ejb = server.createConfig(EjbBuilder.class);
                EmbeddedEjbContainer ejbContainer = server.addContainer(ejb);
                server.addContainer(ContainerBuilder.Type.jpa);

                habitat = ejb.habitat;
                try {
                     if (rs != null) {
/** Uncomment when EmbeddedSecurityUtil.copyConfigFiles works correctly
                          // If we are running from an existing install, copy over security files to the temp instance
                          EmbeddedSecurityUtil.copyConfigFiles(habitat, rs.instance_root);
**/
                     }

                    server.start();
                } catch (Exception e) {
                    throw new EJBException(e);
                }
                EmbeddedDeployer deployer = server.getDeployer();

                Sniffer sniffer = habitat.getComponent(Sniffer.class, "Ejb");
                ejbAnnotations = sniffer.getAnnotationTypes();
                archiveFactory = habitat.getComponent(ArchiveFactory.class);

                container = new EJBContainerImpl(habitat, server, ejbContainer, deployer);
            // }
        }
    }

    /**
     * Adds EJB modules for the property in the properties Map or if such property
     * is not specified, from the System classpath. Also adds library references.
     */
    private Set<DeploymentElement> addModules(Map<?, ?> properties) {
        Set<DeploymentElement> modules = new HashSet<DeploymentElement>();
        Object obj = (properties == null)? null : properties.get(EJBContainer.MODULES);
        Set<String> moduleNames = new HashSet<String>();

        // Check EJBContainer.MODULES setting first - it can have an explicit set of files
        if (obj != null) {
            // Check module names first
            if (obj instanceof String) {
                moduleNames.add((String)obj);
            } else if (obj instanceof String[]) {
                String[] arr = (String[])obj;
                for (String s : arr) {
                    moduleNames.add(s);
                }
            } else if (obj instanceof File) {
                addModule(modules, moduleNames, (File)obj);
            } else if (obj instanceof File[]) {
                File[] arr = (File[])obj;
                for (File f : arr) {
                    addModule(modules, moduleNames, f);
                }
            } 
        } 

        if (modules.isEmpty()) {
            // No file is specified - load from the classpath
            String path = System.getProperty("java.class.path");
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("[EJBContainerProviderImpl] Looking for EJB modules in classpath: " + path);
            }
            String[] entries = path.split(File.pathSeparator);
            for (String s0 : entries) {
                addModule(modules, moduleNames, new File(s0));
            }
        }

        return modules;
    }

    /**
     * @returns DeploymentElement if this file represents an EJB module or a library.
     * Returns null if it's an EJB module which name is not present in the list of requested
     * module names.
     */
    private DeploymentElement getRequestedEJBModuleOrLibrary(File file, Set<String> moduleNames) 
            throws Exception {
        DeploymentElement result = null;
        String fileName = file.getName();
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("... Testing ... " + fileName);
        }
        ReadableArchive archive = null;
        InputStream is = null;
        try {
            boolean isEJBModule = false;
            String moduleName = DeploymentUtils.getDefaultEEName(fileName);
            archive = archiveFactory.openArchive(file);
            is = getDeploymentDescriptor(archive);
            if (is != null) {
                isEJBModule = true;
                EjbDeploymentDescriptorFile eddf =
                        new EjbDeploymentDescriptorFile();
                eddf.setXMLValidation(false);
                EjbBundleDescriptor bundleDesc =  (EjbBundleDescriptor) eddf.read(is);
                ModuleDescriptor moduleDesc = bundleDesc.getModuleDescriptor();
                moduleDesc.setArchiveUri(fileName);
                moduleName = moduleDesc.getModuleName();
            } else {
                GenericAnnotationDetector detector =
                    new GenericAnnotationDetector(ejbAnnotations);
                isEJBModule = detector.hasAnnotationInArchive(archive);
            }

            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("... is EJB module: " + isEJBModule);
                if (isEJBModule) {
                    _logger.fine("... is Requested EJB module [" + moduleName + "]: " 
                            + (moduleNames.isEmpty() || moduleNames.contains(moduleName)));
                }
            }

            if (!isEJBModule || moduleNames.isEmpty() || moduleNames.contains(moduleName)) {
                result = new DeploymentElement(file, isEJBModule);
            }

            return result;
        } finally {
            if (archive != null) 
                archive.close();
            if (is != null) 
                is.close();
        }
    }

    /**
     * Adds an a DeploymentElement to the Set of modules if it represents an EJB module or a library.
     */
    private void addModule(Set<DeploymentElement> modules, Set<String> moduleNames, File f) {
        try {
            if (f.exists() && !skipJar(f)) {
                
                DeploymentElement de = getRequestedEJBModuleOrLibrary(f, moduleNames);
                if (de != null) {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("... Added " + ((de.isEJBModule())? "EJB Module" : "library") + 
                                " .... " + de.getElement().getName());
                    }
                    modules.add(de);
                }
            } 
        } catch (Exception ioe) {
            _logger.log(Level.FINE, "ejb.embedded.io_exception", ioe);
            // skip it
        }
    }

    /**
     * @returns true if this jar is either a GlassFish module jar or one
     * of the other known implementation modules.
     */
    private boolean skipJar(File file) throws Exception {
        if (!file.isFile() ) {
            return false; // probably a directory
        }

        JarFile jf = null;
        try {
            jf = new JarFile(file);
            Manifest m = jf.getManifest();
            if (m != null) {
                java.util.jar.Attributes attributes = m.getMainAttributes();
                String value = attributes.getValue(ATTRIBUTE_NAME_SKIP);
                if (value != null) {
                    for (String skipValue : ATTRIBUTE_VALUES_SKIP) {
                        if (value.startsWith(skipValue)) {
                            for (String okValue : ATTRIBUTE_VALUES_OK) {
                                if (value.indexOf(okValue) > 0) {
                                    // Still OK
                                    return false;
                                }
                            }
                            // Not OK - skip it
                            _logger.info("... skipping... " + file.getName());
                            return true;
                        }
                    }
                }
            }
        } finally {
            if (jf != null) {
                try {
                    jf.close();
                } catch (IOException ex) {
                    _logger.log(Level.FINE, "Exception while closing JarFile "
                            + jf.getName() + ": ", ex);
                }
            }
        }

        return false;
    }

    /**
     * Create a File object from the location and report an error
     * if such file does not exist.
     * Returns null if such file does not exist.
     */
    private File getValidFile(String location) {
        File f = new File(location);
        if (!f.exists()) {
            _logger.log(Level.SEVERE, "ejb.embedded.location_not_exists", location);
            f = null;
        }
        return f;
    }

    /**
     * Create File objects corresponding to instance root and domain.xml location.
     */
    private Result getLocations(Map<?, ?> properties) throws EJBException {
        Result rs = null;
        String installed_root_location = null;
        String instance_root_location = null;
        String domain_file_location = null;

        if (properties != null) {
            // Check if anything is set
            installed_root_location = (String) properties.get(GF_INSTALLATION_ROOT);
            instance_root_location = (String) properties.get(GF_INSTANCE_ROOT);
            domain_file_location = (String) properties.get(GF_DOMAIN_FILE);
        }

        if (installed_root_location == null) {
            // Try to calculate installation location relative to 
            // the jar that contains this class
            try {
                installed_root_location = Which.jarFile(getClass()).
                        getParentFile().getParentFile().getAbsolutePath();
            } catch (Exception e) {
                _logger.log(Level.SEVERE, "ejb.embedded.cannot_determine_installation_location");
                _logger.log(Level.FINE, e.getMessage(), e);
            }
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("[EJBContainerProviderImpl] installed_root_location : " + installed_root_location);
        }
        if (installed_root_location != null) {
            File installed_root = getValidFile(installed_root_location);
            if (installed_root != null) {
                if (instance_root_location == null) {
                    // Calculate location for the domain relative to GF install
                    instance_root_location = installed_root_location 
                            + File.separatorChar + "domains" 
                            + File.separatorChar + "domain1";
                }

                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("[EJBContainerProviderImpl] instance_root_location: " + instance_root_location);
                }
                File instance_root = getValidFile(instance_root_location);
                if (instance_root != null) {
                    if (domain_file_location == null) {
                        // Calculate location for the domain.xml relative to GF instance
                        domain_file_location = instance_root_location
                                + File.separatorChar + "config" 
                                + File.separatorChar + "domain.xml";
                    }

                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("[EJBContainerProviderImpl] domain_file_location : " + domain_file_location);
                    }
                    File domain_file = getValidFile(domain_file_location);
                    if (domain_file != null) {
                        File temp_domain_file = null;
                        try {
                            DomainXmlTransformer dxf = new DomainXmlTransformer(domain_file, _logger);
                            boolean keep_ports = (properties == null)? false : ((properties.get(GF_WEB_HTTP_PORT) == null)? false : true);
                            temp_domain_file = dxf.transform(keep_ports);
                        } catch (Exception e) {
                            throw new EJBException(localStrings.getString(
                                    "ejb.embedded.exception_creating_temporary_domain_xml_file"), e);
                        }

                        if (temp_domain_file != null) {
                            domain_file = temp_domain_file;
                        } else {
                            throw new EJBException(localStrings.getString(
                                    "ejb.embedded.failed_create_temporary_domain_xml_file"));
                        }
                        rs = new Result(installed_root, instance_root, domain_file);
                    }
                }
            }
        }

        return rs;
    }

    private InputStream getDeploymentDescriptor(ReadableArchive archive) throws IOException {
        InputStream dd = archive.getEntry("META-INF/ejb-jar.xml");
        if (dd == null) {
            // Try EJB in a .war file as well
            dd = archive.getEntry("WEB-INF/ejb-jar.xml");
        }
        return dd;
    }

    private void addWebContainerIfRequested(Map<?, ?> properties) throws EJBException {
        String http_port = (properties == null)? null : (String)properties.get(GF_WEB_HTTP_PORT);
        if (http_port != null) {
            int port = 8080;
            try {
                port = Integer.valueOf(http_port);
            } catch (NumberFormatException e) {
                System.err.println("Using port 8080");
            }

            try {
                Port http = server.createPort(port);
                ContainerBuilder<EmbeddedContainer> cb = server.createConfig(ContainerBuilder.Type.web);
                EmbeddedContainer container = server.addContainer(cb);
                container.bind(http, "http");
            } catch (Exception e) {
                throw new EJBException(e);
            }
        }
    }

    private class Result {
        final File installed_root;
        final File instance_root;
        final File domain_file;

        Result (File installed_root, File instance_root, File domain_file) {
            this.installed_root  = installed_root;
            this.instance_root  = instance_root;
            this.domain_file  = domain_file;
        }
    }
}
