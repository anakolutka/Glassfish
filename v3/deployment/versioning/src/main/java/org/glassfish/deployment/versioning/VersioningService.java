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
package org.glassfish.deployment.versioning;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 * This service provides methods to handle application names
 * in the versioning context
 *
 * @author Romain GRECOURT - SERLI (romain.grecourt@serli.com)
 */
@I18n("versioning.service")
@Service
@Scoped(PerLookup.class)
public class VersioningService {

    private static final LocalStringManagerImpl LOCALSTRINGS =
            new LocalStringManagerImpl(VersioningService.class);
    @Inject
    private CommandRunner commandRunner;
    @Inject
    private Domain domain;
    static final String REPOSITORY_DASH = "-";
    public static final String EXPRESSION_SEPARATOR = ":";
    public static final String EXPRESSION_WILDCARD = "*";

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
     * @throws VersioningSyntaxException if the given application name had some
     * critical patterns.
     */
    public static final String getUntaggedName(String appName)
            throws VersioningSyntaxException {

        if(appName != null && !appName.isEmpty()){
            int colonIndex = appName.indexOf(EXPRESSION_SEPARATOR);
            // if versioned
            if (colonIndex != -1) {

                // if appName is ending with a colon
                if (colonIndex == (appName.length() - 1)) {
                    throw new VersioningSyntaxException(
                            LOCALSTRINGS.getLocalString("invalid.appname",
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
     * @throws VersioningSyntaxException if the given application name had some
     * critical patterns.
     */
    public static final String getExpression(String appName)
            throws VersioningSyntaxException {

        int colonIndex = appName.indexOf(EXPRESSION_SEPARATOR);
        // if versioned
        if (colonIndex != -1) {
            if (colonIndex == (appName.length() - 1)) {
                throw new VersioningSyntaxException(
                        LOCALSTRINGS.getLocalString("invalid.appName",
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
     * @throws VersioningSyntaxException if the given application name had some
     * critical patterns.
     */
    public static final void checkIdentifier(String appName)
            throws VersioningSyntaxException {
        String identifier = getExpression(appName);
        if (identifier != null && identifier.contains(EXPRESSION_WILDCARD)) {
            throw new VersioningSyntaxException(
                    LOCALSTRINGS.getLocalString("versioning.service.wildcard.not.allowed",
                    "Wildcard character(s) are not allowed in a version identifier."));
        }
    }

    /**
     * Extract the set of version(s) of the given application from a set of
     * applications. This method is used by unit tests.
     *
     * @param untaggedName the application name as an untagged version : an
     * application name without version identifier
     * @param allApplicationRefs the set of application refs
     * @return all the version(s) of the given application in the given set of
     * applications
     */
    public static final List<String> getVersions(String untaggedName,
            List<ApplicationRef> allApplicationRefs) {

        List<String> allVersions = new ArrayList<String>();
        Iterator<ApplicationRef> it = allApplicationRefs.iterator();

        while (it.hasNext()) {
            ApplicationRef ref = it.next();

            // if a tagged version or untagged version of the app
            if (ref.getRef().startsWith(untaggedName + EXPRESSION_SEPARATOR)
                    || ref.getRef().equals(untaggedName)) {
                allVersions.add(ref.getRef());
            }
        }
        return allVersions;
    }

    /**
     * Extract the set of version(s) of the given application represented as
     * an untagged version name
     *
     * @param untaggedName the application name as an untagged version : an
     * application name without version identifier
     * @param target the target where we want to get all the versions
     * @return all the version(s) of the given application
     */
    public final List<String> getAllversions(String untaggedName, String target) {
        List<ApplicationRef> allApplicationRefs =
                domain.getApplicationRefsInTarget(target);
        return getVersions(untaggedName, allApplicationRefs);
    }

    /**
     * Search for the enabled version of the given application.
     *
     * @param name the application name
     * @param target an option supply from admin command, it's retained for
     * compatibility with other releases
     * @return the enabled version of the application, if exists
     * @throws VersioningSyntaxException if getUntaggedName throws an exception
     */
    public final String getEnabledVersion(String name, String target)
            throws VersioningSyntaxException {

        String untaggedName = getUntaggedName(name);
        List<String> allVersions = getAllversions(untaggedName, target);

        if (allVersions != null) {
            Iterator it = allVersions.iterator();

            while (it.hasNext()) {
                String app = (String) it.next();

                // if a version of the app is enabled
                ApplicationRef ref = domain.getApplicationRefInTarget(
                        app, target);
                if (ref != null && Boolean.valueOf(ref.getEnabled())) {
                    return app;
                }
            }
        }
        // no enabled version found
        return null;
    }

    /**
     * Search for the version(s) matched by the expression contained in the given
     * application name. This method is used by unit tests.
     *
     * @param listVersion the set of all versions of the application
     * @param appName the application name containing the expression
     * @return the expression matched list
     * @throws VersioningException if the expression is an identifier matching
     * a version not registered, or if getExpression throws an exception
     */
    public static final List<String> matchExpression(List<String> listVersion, String appName)
            throws VersioningException {

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
                throw new VersioningException(
                        LOCALSTRINGS.getLocalString("version.notreg",
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
                throw new VersioningException(
                        LOCALSTRINGS.getLocalString("version.notreg",
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
     * Process the expression matching operation of the given application name.
     *
     * @param name the application name containing the version expression
     * @param target the target we are looking for the verisons 
     * @return a List of all expression matched versions, return empty list
     * if no version is registered on this target
     * or if getUntaggedName throws an exception
     */
    public final List<String> getMatchedVersions(String name, String target)
            throws VersioningException {

        String untagged = getUntaggedName(name);
        List<String> allVersions = getAllversions(untagged, target);

        if (allVersions.size() == 0) {
            // if versionned
            if(!name.equals(untagged)){
                throw new VersioningException(
                        LOCALSTRINGS.getLocalString("application.noversion",
                        "Application {0} has no version registered",
                        untagged));  
            }
            return Collections.EMPTY_LIST;
        }

        return matchExpression(allVersions, name);
    }

    /**
     * Replaces the colon with a dash in the given application name.
     *
     * @param appName
     * @return return a valid repository name
     * @throws VersioningSyntaxException if getEpression and getUntaggedName
     * throws exception
     */
    public static final String getRepositoryName(String appName)
            throws VersioningSyntaxException {

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
     *  Disable the enabled version of the application if it exists. This method
     *  is used in versioning context.
     *
     *  @param appName application's name
     *  @param target an option supply from admin command, it's retained for
     * compatibility with other releases
     *  @param report ActionReport, report object to send back to client.
     */
    public void handleDisable(final String appName, final String target,
            final ActionReport report) throws VersioningSyntaxException {

        // retrieve the currently enabled version of the application
        String enabledVersion = getEnabledVersion(appName, target);

        // invoke disable if the currently enabled version is not itself
        if (enabledVersion != null
                && !enabledVersion.equals(appName)) {
            final ParameterMap parameters = new ParameterMap();
            parameters.add("DEFAULT", enabledVersion);
            parameters.add("target", target);

            ActionReport subReport = report.addSubActionsReport();

            CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation("disable", subReport);
            inv.parameters(parameters).execute();
        }
    }
}
