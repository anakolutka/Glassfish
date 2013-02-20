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
package org.glassfish.cdi;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

import java.util.logging.Logger;

import static java.util.logging.Level.FINE;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class CDILoggerInfo {
    public static final String CDI_LOGMSG_PREFIX = "AS-CDI";

    @LogMessagesResourceBundle
    public static final String CDI_WELD_LOGMESSAGE_RESOURCE = "org.glassfish.cdi.LogMessages";

    @LoggerInfo(subsystem = "AS-CDI", description = "CDI", publish = true)
    public static final String CDI_LOGGER_SUBSYSTEM_NAME = "javax.enterprise.inject.spi";
    private static final Logger CDI_LOGGER = Logger.getLogger(CDI_LOGGER_SUBSYSTEM_NAME, CDI_WELD_LOGMESSAGE_RESOURCE);

    public static Logger getLogger() {
        return CDI_LOGGER;
    }

    @LogMessageInfo( message = "Setting Context Class Loader for {0} to {1}.",
                     level = "FINER")
    public static final String SETTING_CONTEXT_CLASS_LOADER = CDI_LOGMSG_PREFIX + "-001";

    @LogMessageInfo( message = "BeanDeploymentArchiveImpl::addBeanClass - adding {0} to {1}.",
                     level = "FINE")
    public static final String ADD_BEAN_CLASS = CDI_LOGMSG_PREFIX + "-002";

    @LogMessageInfo( message = "{0} not added to beanClasses",
                     level = "FINE")
    public static final String ADD_BEAN_CLASS_ERROR = CDI_LOGMSG_PREFIX + "-003";

    @LogMessageInfo( message = "Processing {0} as it has {1} or {2}.",
                     level = "FINE")
    public static final String PROCESSING_BEANS_XML = CDI_LOGMSG_PREFIX + "-004";

    @LogMessageInfo( message = "Error while trying to load Bean Class {0} : {1}.",
                     level = "WARNING")
    public static final String ERROR_LOADING_BEAN_CLASS = CDI_LOGMSG_PREFIX + "-005";

    @LogMessageInfo( message = "Processing WEB-INF/lib in {0}.",
                     level = "FINE")
    public static final String PROCESSING_WEB_INF_LIB = CDI_LOGMSG_PREFIX + "-006";

    @LogMessageInfo( message = "WEB-INF/lib: considering {0} as a bean archive and hence added another BDA for it.",
                     level = "FINE")
    public static final String WEB_INF_LIB_CONSIDERING_BEAN_ARCHIVE = CDI_LOGMSG_PREFIX + "-007";

    @LogMessageInfo( message = "WEB-INF/lib: skipping {0} as it doesn't have beans.xml.",
                     level = "FINE")
    public static final String WEB_INF_LIB_SKIPPING_BEAN_ARCHIVE = CDI_LOGMSG_PREFIX + "-008";

    @LogMessageInfo( message = "JAR processing: {0} as a Bean archive jar since it has META-INF/beans.xml.",
                     level = "FINE")
    public static final String PROCESSING_BDA_JAR = CDI_LOGMSG_PREFIX + "-009";

    @LogMessageInfo( message = "Ensure {0} is associated with {1}",
                     level = "FINE")
    public static final String  ENSURE_ASSOCIATION = CDI_LOGMSG_PREFIX + "-010";

    @LogMessageInfo( message = "Ensure web lib jar visibility.  Updating {0}.",
                     level = "FINE")
    public static final String  ENSURE_WEB_LIB_JAR_VISIBILITY = CDI_LOGMSG_PREFIX + "-011";

    @LogMessageInfo( message = "Ensure web lib jar visibility.  Updating {0} to include {1}.",
                     level = "FINE")
    public static final String  ENSURE_WEB_LIB_JAR_VISIBILITY_2 = CDI_LOGMSG_PREFIX + "-012";

    @LogMessageInfo( message = "Collecting jar info for {0}.",
                     level = "FINE")
    public static final String COLLECTING_JAR_INFO = CDI_LOGMSG_PREFIX + "-013";

    @LogMessageInfo( message = "Error reading archive.  {0.}",
                     level = "FINE")
    public static final String ERROR_READING_ARCHIVE = CDI_LOGMSG_PREFIX + "-014";

    @LogMessageInfo( message = "Collecting rar info for {0}.",
                     level = "FINE")
    public static final String COLLECTING_RAR_INFO = CDI_LOGMSG_PREFIX + "-015";

    @LogMessageInfo( message = "Using Context Class Loader.",
                     level = "FINE")
    public static final String USING_CONTEXT_CLASS_LOADER = CDI_LOGMSG_PREFIX + "-016";

    @LogMessageInfo( message = "Context Class Loader is null. Using DeploymentImpl's classloader.",
                     level = "FINE")
    public static final String CONTEXT_CLASS_LOADER_NULL = CDI_LOGMSG_PREFIX + "-017";

    @LogMessageInfo( message = "Creating deployment for archive: {0}.",
                     level = "FINE")
    public static final String CREATING_DEPLOYMENT_ARCHIVE = CDI_LOGMSG_PREFIX + "-018";

    @LogMessageInfo( message = "getBeanDeploymentArchives returning {0}.",
                     level = "FINE")
    public static final String GET_BEAN_DEPLOYMENT_ARCHIVES = CDI_LOGMSG_PREFIX + "-019";

    @LogMessageInfo( message = "loadBeanDeploymentArchive for beanClass {0}",
                     level = "FINE")
    public static final String LOAD_BEAN_DEPLOYMENT_ARCHIVE = CDI_LOGMSG_PREFIX + "-020";

    @LogMessageInfo( message = "loadBeanDeploymentArchive checking for {0} in root BDA {0}.",
                     level = "FINE")
    public static final String LOAD_BEAN_DEPLOYMENT_ARCHIVE_CHECKING = CDI_LOGMSG_PREFIX + "-021";

    @LogMessageInfo( message = "loadBeanDeploymentArchive An existing BDA has this class {0}.  " +
                               "Adding this class as a bean class it to existing bda: {1}.",
                     level = "FINE")
    public static final String LOAD_BEAN_DEPLOYMENT_ARCHIVE_ADD_TO_EXISTING = CDI_LOGMSG_PREFIX + "-022";

    @LogMessageInfo( message = "loadBeanDeploymentArchive checking for {0} in subBDA {1}.",
                     level = "FINE")
    public static final String LOAD_BEAN_DEPLOYMENT_ARCHIVE_CHECKING_SUBBDA = CDI_LOGMSG_PREFIX + "-023";

    @LogMessageInfo( message = "loadBeanDeploymentArchive beanClass {0} not found in the BDAs of this deployment. " +
                               "Creating a new BDA.",
                     level = "FINE")
    public static final String LOAD_BEAN_DEPLOYMENT_ARCHIVE_CREATE_NEW_BDA = CDI_LOGMSG_PREFIX + "-024";

    @LogMessageInfo( message = "loadBeanDeploymentArchive new BDA {0} created. Adding new BDA to all root BDAs of this deployment.",
                     level = "FINE")
    public static final String LOAD_BEAN_DEPLOYMENT_ARCHIVE_ADD_NEW_BDA_TO_ROOTS = CDI_LOGMSG_PREFIX + "-025";

    @LogMessageInfo( message = "loadBeanDeploymentArchive for beanClass {0} returning the newly created BDA {1}",
                     level = "FINE")
    public static final String LOAD_BEAN_DEPLOYMENT_ARCHIVE_RETURNING_NEWLY_CREATED_BDA = CDI_LOGMSG_PREFIX + "-026";

    @LogMessageInfo( message = "Exception thrown while scanning for library jars. {0}",
                     level = "FINE")
    public static final String EXCEPTION_SCANNING_JARS = CDI_LOGMSG_PREFIX + "-027";

}
