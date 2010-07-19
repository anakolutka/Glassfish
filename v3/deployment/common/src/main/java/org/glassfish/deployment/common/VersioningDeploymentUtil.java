/*
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

package org.glassfish.deployment.common;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class provides utility methods to handle application names
 *
 * @author Romain GRECOURT - SERLI (romain.grecourt@serli.com)
 */

public class VersioningDeploymentUtil {

    public static final LocalStringManagerImpl LOCALSTRINGS =
            new LocalStringManagerImpl(VersioningDeploymentUtil.class);
    public static final String EXPRESSION_SEPARATOR = ":";
    public static final String EXPRESSION_WILDCARD = "*";
    public static final String REPOSITORY_DASH = "~";

    /**
     * Extract the untagged name for a given application name that complies
     * with versioning rules (version identifier / expression) or not.
     *
     * If the application name is using versioning rules, the method will split
     * the application names with the colon character and retrieve the
     * untagged name from the first token.
     *
     * Else the given application name is an untagged name.
     *
     * @param appName the application name
     * @return the untagged version name
     * @throws VersioningDeploymentSyntaxException if the given application name had some
     * critical patterns.
     */
    public static final String getUntaggedName(String appName)
            throws VersioningDeploymentSyntaxException {

        if(appName != null && !appName.isEmpty()){
            int colonIndex = appName.indexOf(EXPRESSION_SEPARATOR);
            // if versioned
            if (colonIndex != -1) {

                // if appName is ending with a colon
                if (colonIndex == (appName.length() - 1)) {
                    throw new VersioningDeploymentSyntaxException(
                            LOCALSTRINGS.getLocalString("versioning.deployment.invalid.appname",
                            "excepted version identifier after colon: {0}",
                            appName));
                }
                return appName.substring(0, colonIndex);
            }
        }
        // not versioned
        return appName;
    }

    /**
     * Extract the version identifier / expression for a given application name
     * that complies with versioning rules.
     *
     * The method splits the application name with the colon character
     * and retrieve the 2nd token.
     *
     * @param appName the application name
     * @return the version identifier / expression extracted from application name
     * @throws VersioningDeploymentSyntaxException if the given application name had some
     * critical patterns.
     */
    public static final String getExpression(String appName)
            throws VersioningDeploymentSyntaxException {

        int colonIndex = appName.indexOf(EXPRESSION_SEPARATOR);
        // if versioned
        if (colonIndex != -1) {
            if (colonIndex == (appName.length() - 1)) {
                throw new VersioningDeploymentSyntaxException(
                        LOCALSTRINGS.getLocalString("versioning.deployment.invalid.appname",
                        "excepted version expression/identifier after colon: {0}",
                        appName));
            }
            return appName.substring(colonIndex + 1, appName.length());
        }
        // not versioned
        return null;
    }

    /**
     * Check a versionned application name.
     *
     * This method is used to provide consistant error messages for identifier
     * aware operations.
     *
     * @param appName the application name
     * @throws VersioningDeploymentSyntaxException if the given application name had some
     * critical patterns.
     */
    public static final void checkIdentifier(String appName)
            throws VersioningDeploymentSyntaxException {
        String identifier = getExpression(appName);
        if (identifier != null && identifier.contains(EXPRESSION_WILDCARD)) {
            throw new VersioningDeploymentSyntaxException(
                    LOCALSTRINGS.getLocalString("versioning.deployment.wildcard.not.allowed",
                    "Wildcard character(s) are not allowed in a version identifier."));
        }
    }

    /**
     * Extract the set of version(s) of the given application from a set of
     * applications. This method is used by unit tests.
     *
     * @param untaggedName the application name as an untagged version : an
     * application name without version identifier
     * @param allApplications the set of applications
     * @return all the version(s) of the given application in the given set of
     * applications
     */
    public static final List<String> getVersions(String untaggedName,
            List<Application> allApplications) {

        List<String> allVersions = new ArrayList<String>();
        Iterator<Application> it = allApplications.iterator();

        while (it.hasNext()) {
            Application app = it.next();

            // if a tagged version or untagged version of the app
            if (app.getName().startsWith(untaggedName + EXPRESSION_SEPARATOR)
                    || app.getName().equals(untaggedName)) {
                allVersions.add(app.getName());
            }
        }
        return allVersions;
    }

    /**
     * Search for the version(s) matched by the expression contained in the given
     * application name. This method is used by unit tests.
     *
     * @param listVersion the set of all versions of the application
     * @param appName the application name containing the expression
     * @return the expression matched list
     * @throws VersioningDeploymentException if the expression is an identifier matching
     * a version not registered, or if getExpression throws an exception
     */
    public static final List<String> matchExpression(List<String> listVersion, String appName)
            throws VersioningDeploymentException {

        if (listVersion.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        String expressionVersion = getExpression(appName);
        //List<String> matchedVersions = new ArrayList<String>(listVersion);

        // if using an untagged version
        if (expressionVersion == null) {
            // return the matched version if exist
            if (listVersion.contains(appName)) {
                return listVersion.subList(listVersion.indexOf(appName),
                        listVersion.indexOf(appName) + 1);
            } else {
                throw new VersioningDeploymentException(
                        LOCALSTRINGS.getLocalString("versioning.deployment.version.notreg",
                        "version {0} not registered",
                        appName));
            }
        }

        // if using an identifier
        if (expressionVersion.indexOf(EXPRESSION_WILDCARD) == -1) {
            // return the matched version if exist
            if (listVersion.contains(appName)) {
                return listVersion.subList(listVersion.indexOf(appName),
                        listVersion.indexOf(appName) + 1);
            } else {
                throw new VersioningDeploymentException(
                        LOCALSTRINGS.getLocalString("versioning.deployment.version.notreg",
                        "Version {0} not registered",
                        appName));
            }
        }

        StringTokenizer st = new StringTokenizer(expressionVersion,
                EXPRESSION_WILDCARD);
        String lastToken = null;
        List<String> matchedVersions = new ArrayList<String>(listVersion);

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            Iterator it = listVersion.iterator();

            while (it.hasNext()) {
                String app = (String) it.next();
                String identifier = getExpression(app);

                // get the position of the last token in the current identifier
                int lastTokenIndex = -1;
                if (lastToken != null) {
                    lastTokenIndex = identifier.indexOf(lastToken);
                }
                // matching expression on the current identifier
                if (identifier != null) {
                    if ( expressionVersion.startsWith(token)
                            && ! identifier.startsWith(token) ) {
                        matchedVersions.remove(app);
                    } else if ( expressionVersion.endsWith(token)
                            && !identifier.endsWith(token) ) {
                        matchedVersions.remove(app);
                    } else if ( !identifier.contains(token.subSequence(0, token.length() - 1))
                            || identifier.indexOf(token) <= lastTokenIndex ) {
                        matchedVersions.remove(app);
                    }
                } else {
                    matchedVersions.remove(app);
                }

            }
            lastToken = token;
        }
        // returns matched version(s)
        return matchedVersions;
    }

    /**
     * Replaces the colon with a dash in the given application name.
     *
     * @param appName
     * @return return a valid repository name
     * @throws VersioningDeploymentSyntaxException if getEpression and getUntaggedName
     * throws exception
     */
    public static final String getRepositoryName(String appName)
            throws VersioningDeploymentSyntaxException {

        String expression = getExpression(appName);
        String untaggedName = getUntaggedName(appName);

        if (expression != null) {

            StringBuilder repositoryNameBuilder = new StringBuilder(untaggedName);
            repositoryNameBuilder.append(REPOSITORY_DASH);
            repositoryNameBuilder.append(expression);
            return repositoryNameBuilder.toString();

        } else {
            return untaggedName;
        }
    }

    /**
     * @return return true is the given application name is not versioned
     */
    public static final Boolean isUntagged(String appName) {
        Boolean isUntagged = false;
        try {
            String untaggedName = VersioningDeploymentUtil.getUntaggedName(appName);
            if (untaggedName != null && untaggedName.equals(appName)) {
                isUntagged = true;
            }
        } catch (VersioningDeploymentSyntaxException e) {
        }
        return isUntagged;
    }
}
