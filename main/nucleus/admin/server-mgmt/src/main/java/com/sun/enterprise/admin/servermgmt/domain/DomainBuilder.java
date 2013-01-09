/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package com.sun.enterprise.admin.servermgmt.domain;

import static com.sun.enterprise.admin.servermgmt.SLogger.UNHANDLED_EXCEPTION;
import static com.sun.enterprise.admin.servermgmt.SLogger.getLogger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.DomainException;
import com.sun.enterprise.admin.servermgmt.RepositoryException;
import com.sun.enterprise.admin.servermgmt.RepositoryManager;
import com.sun.enterprise.admin.servermgmt.pe.PEDomainConfigValidator;
import com.sun.enterprise.admin.servermgmt.pe.SubstitutableTokens;
import com.sun.enterprise.admin.servermgmt.stringsubs.StringSubstitutionFactory;
import com.sun.enterprise.admin.servermgmt.stringsubs.StringSubstitutor;
import com.sun.enterprise.admin.servermgmt.stringsubs.impl.AttributePreprocessorImpl;
import com.sun.enterprise.admin.servermgmt.template.TemplateInfoHolder;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.Property;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.PropertyType;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.io.FileUtils;

/**
 * Domain builder class.
 */
public class DomainBuilder {

    /* These properties are public interfaces, handle with care */
    private static final Logger _logger = Logger.getLogger(DomainPortValidator.class.getPackage().getName());
    private static final LocalStringsImpl _strings = new LocalStringsImpl(DomainBuilder.class);

    /** The default stringsubs configuration file name. */
    private final static String STRINGSUBS_FILE = "stringsubs.xml";
    /** The filename contains basic template information. */
    private final static String TEMPLATE_INFO_XML = "template-info.xml";

    private DomainConfig _domainConfig;
    private JarFile _templateJar;
    private DomainTemplate _domainTempalte;
    private Properties _defaultPortValues = new Properties();
    private byte[] keystoreBytes = null;
    private Set<String> extractedEntries = new HashSet<String>();

    /**
     * Create's a {@link DomainBuilder} object by initializing and loading the template jar.
     *
     * @param domainConfig An object contains domain creation parameters.
     * @throws DomainException If any error occurs during initialization.
     */
    public DomainBuilder(DomainConfig domainConfig) throws DomainException {
        _domainConfig = domainConfig;
        initialize();
    }

    /**
     * Initialize template by loading template jar.
     *
     * @throws DomainException If exception occurs in initializing the template jar.
     */
    // TODO : localization of index.html
    private void initialize() throws DomainException {
        String templateJarPath = (String)_domainConfig.get(DomainConfig.K_TEMPLATE_NAME);
        try {
            // Loads template-info.xml
            _templateJar = new JarFile(new File(templateJarPath));
            JarEntry je = _templateJar.getJarEntry(TEMPLATE_INFO_XML);
            if (je == null) {
                throw new DomainException(_strings.get("missingMandatoryFile", TEMPLATE_INFO_XML));
            }
            TemplateInfoHolder templateInfoHolder = new TemplateInfoHolder(_templateJar.getInputStream(je), templateJarPath);
            extractedEntries.add(TEMPLATE_INFO_XML);

            // Loads string substitution XML.
            je = _templateJar.getJarEntry(STRINGSUBS_FILE);
            StringSubstitutor stringSubstitutor = null;
            if (je != null) {
                stringSubstitutor = StringSubstitutionFactory.createStringSubstitutor(_templateJar.getInputStream(je));
                List<Property> defaultStringSubsProps = stringSubstitutor.getDefaultProperties(PropertyType.PORT);
                for (Property prop : defaultStringSubsProps) {
                    _defaultPortValues.setProperty(prop.getKey(), prop.getValue());
                }
                extractedEntries.add(je.getName());
            } else {
                _logger.log(Level.WARNING, _strings.get("missingFile", STRINGSUBS_FILE));
            }
            _domainTempalte = new DomainTemplate(templateInfoHolder, stringSubstitutor);

            // Loads default self signed certificate.
            je = _templateJar.getJarEntry("config/" + DomainConstants.KEYSTORE_FILE);
            if (je != null) {
                keystoreBytes = new byte[(int)je.getSize()];
                InputStream in = null;
                int count = 0;
                try {
                    in = _templateJar.getInputStream(je);
                    count = in.read(keystoreBytes);
                    if (count < keystoreBytes.length) {
                        throw new DomainException(_strings.get("loadingFailure", je.getName()));
                    }
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
                extractedEntries.add(je.getName());
            }
        } catch (Exception e) {
            throw new DomainException(e);
        }
    };

    /**
     * Validate's the template. 
     *
     * @throws DomainException If any exception occurs in validation.
     */
    public void validateTemplate() throws DomainException {
        try	{
            // Sanity check on the repository.
            RepositoryManager repoManager = new RepositoryManager();
            repoManager.checkRepository(_domainConfig, false);

            // Validate the port values.
            DomainPortValidator portValidator = new DomainPortValidator(_domainConfig, _defaultPortValues);
            portValidator.validateAndSetPorts();

            // Validate other domain config parameters.
            new PEDomainConfigValidator().validate(_domainConfig);

        } catch (Exception ex) {
            throw new DomainException(ex);
        }
    }

    /**
     * Performs all the domain configurations which includes security, configuration processing,
     * substitution of parameters... etc.
     * 
     * @throws DomainException If any exception occurs in configuration.
     */
    public void run() throws RepositoryException, DomainException {
        // Create domain directories.
        File domainDir = FileUtils.safeGetCanonicalFile(new File(_domainConfig.getRepositoryRoot(),
                _domainConfig.getDomainName()));
        try {
            if (!domainDir.mkdirs()) {
                throw new RepositoryException(_strings.get("directoryCreationError",	domainDir));
            }
        } catch (Exception e) {
            throw new RepositoryException(_strings.get("directoryCreationError", domainDir), e);
        }

        // Extract other jar entries
        try {
            byte[] buffer = new byte[10000];
            for (Enumeration<JarEntry> entry = _templateJar.entries(); entry.hasMoreElements();) {
                JarEntry jarEntry = (JarEntry)entry.nextElement();
                String entryName = jarEntry.getName();
                if (extractedEntries.contains(entryName)) {
                    continue;
                }
                if (jarEntry.isDirectory()) {
                    File dir = new File(domainDir, jarEntry.getName());
                    if (!dir.exists()) {
                        if (!dir.mkdir()) {
                            _logger.log(Level.WARNING, _strings.get("directoryCreationError", dir.getName())); 
                        }
                    }
                    continue;
                }
                InputStream in = null;
                BufferedOutputStream outputStream = null;
                try {
                    in = _templateJar.getInputStream(jarEntry);
                    outputStream = new BufferedOutputStream(new FileOutputStream(new File(domainDir.getAbsolutePath(),
                            jarEntry.getName())));
                    int i = 0;
                    while ((i = in.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, i);
                    }
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Exception io)
                        { /** ignore*/ }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Exception io)
                        { /** ignore*/ }
                    }
                }
            }

            File configDir = new File(domainDir, DomainConstants.CONFIG_DIR);
            String user =  (String) _domainConfig.get(DomainConfig.K_USER);
            String password = (String) _domainConfig.get(DomainConfig.K_PASSWORD);
            String masterPassword = (String) _domainConfig.get(DomainConfig.K_MASTER_PASSWORD);
            Boolean saveMasterPassword = (Boolean)_domainConfig.get(DomainConfig.K_SAVE_MASTER_PASSWORD);

            // Process domain security.
            DomainSecurity domainSecurity = new DomainSecurity();
            domainSecurity.processAdminKeyFile(new File(configDir, DomainConstants.ADMIN_KEY_FILE), user, password);
            try {
                domainSecurity.createSSLCertificateDatabase(configDir, _domainConfig, masterPassword);
            } catch (Exception e) {
                String msg = _strings.getString("SomeProblemWithKeytool", e.getMessage());
                System.err.println(msg);
                FileOutputStream fos = null;
                try {
                    File keystoreFile = new File(configDir, DomainConstants.KEYSTORE_FILE);
                    fos = new FileOutputStream(keystoreFile);
                    fos.write(keystoreBytes);
                } catch (Exception ex) {
                    getLogger().log(Level.SEVERE, UNHANDLED_EXCEPTION, ex);
                } finally {
                    if (fos != null)
                        fos.close();
                }
            }
            domainSecurity.changeMasterPasswordInMasterPasswordFile(new File(configDir, DomainConstants.MASTERPASSWORD_FILE), masterPassword, 
                    saveMasterPassword);
            domainSecurity.createPasswordAliasKeystore(new File(configDir, DomainConstants.DOMAIN_PASSWORD_FILE), masterPassword);

            // Perform string substitution.
            if (_domainTempalte.hasStringsubs()) {
                StringSubstitutor substitutor = _domainTempalte.getStringSubs();
                Map<String, String> lookUpMap = SubstitutableTokens.getSubstitutableTokens(_domainConfig);
                substitutor.setLookUpMap(lookUpMap);
                substitutor.setAttributePreprocessor(new AttributePreprocessorImpl(lookUpMap));
                substitutor.substituteAll();
            }

            // Change the permission for bin & config directories.
            try {
                //4958533
                domainSecurity.changeMode("-R u+x ", new File(domainDir, DomainConstants.BIN_DIR));
                domainSecurity.changeMode("-R g-rwx,o-rwx ", configDir);
                //4958533
            } catch (Exception e) {
                throw new DomainException(_strings.get("setPermissionError"), e);
            }

            // Generate domain-info.xml
            DomainInfoManager domainInfoManager = new DomainInfoManager();
            domainInfoManager.process(_domainTempalte, domainDir);

        } catch (DomainException de) {
            //roll-back
            FileUtils.liquidate(domainDir);
            throw de;
        } catch (Exception ex) {
            //roll-back
            FileUtils.liquidate(domainDir);
            throw new DomainException(ex);
        }
    };
}
