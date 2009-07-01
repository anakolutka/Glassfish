/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.appclient.client.acc;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.module.bootstrap.BootException;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.jvnet.hk2.component.Habitat;
import org.xml.sax.SAXParseException;

/**
 * Represents a generated JAR created during deployment corresponding to
 * the developer's original app client JAR or EAR.  Even if the facade object
 * represents an EAR facade, it uses the caller-supplied main class name
 * and/or caller-supplied app client name to select one of the app client
 * facades listed in the group facade.  That is, once fully initialized,
 * a given Facade instance represents the single app client to be launched.
 */
public class FacadeLaunchable implements Launchable {

    /** name of a manifest entry in an EAR facade listing the URIs of the individual app client facades in the group */
    public static final Attributes.Name GLASSFISH_APPCLIENT_GROUP = new Attributes.Name("GlassFish-AppClient-Group");

    /** name of a manifest entry in an app client facade indicating the app client's main class */
    public static final Attributes.Name GLASSFISH_APPCLIENT_MAIN_CLASS = new Attributes.Name("Glassfish-AppClient-Main-Class");

    /** name of a manifest entry in an app client facade listing the URI of the developer's original app client JAR */
    public static final Attributes.Name GLASSFISH_APPCLIENT = new Attributes.Name("GlassFish-AppClient");
    public static final ArchiveFactory archiveFactory = ACCModulesManager.getComponent(ArchiveFactory.class);
    private static final Logger logger = Logger.getLogger(FacadeLaunchable.class.getName());
    private static final LocalStringManager localStrings = new LocalStringManagerImpl(FacadeLaunchable.class);
    private final String mainClassNameToLaunch;
    private final URI[] classPathURIs;
    private final ReadableArchive clientRA;
    private ReadableArchive facadeClientRA;
    private AppClientArchivist facadeArchivist = null;
    private ApplicationClientDescriptor acDesc = null;
    private ClassLoader classLoader = null;
    private final Habitat habitat;
    private final String anchorDir;

    FacadeLaunchable(
            final Habitat habitat,
            final Attributes mainAttrs, final ReadableArchive facadeRA,
            final String anchorDir) throws IOException, URISyntaxException {
        this(habitat,
                facadeRA, mainAttrs,
                openOriginalArchive(facadeRA, mainAttrs.getValue(GLASSFISH_APPCLIENT)),
                mainAttrs.getValue(GLASSFISH_APPCLIENT_MAIN_CLASS),
                anchorDir);
    }

    private static ReadableArchive openOriginalArchive(final ReadableArchive facadeArchive, final String relativeURIToOriginalJar) throws IOException, URISyntaxException {
        URI uriToOriginal = facadeArchive.getURI().resolve(relativeURIToOriginalJar);
        return archiveFactory.openArchive(uriToOriginal);
    }

    FacadeLaunchable(
            final Habitat habitat,
            final ReadableArchive facadeClientRA,
            final Attributes mainAttrs,
            final ReadableArchive clientRA,
            final String mainClassNameToLaunch,
            final String anchorDir) throws IOException {
        super();
        this.facadeClientRA = facadeClientRA;
        this.mainClassNameToLaunch = mainClassNameToLaunch;
        this.clientRA = clientRA;
        this.classPathURIs = toURIs(mainAttrs.getValue(Name.CLASS_PATH));
        this.habitat = habitat;
        this.anchorDir = anchorDir;
    }

    public URI getURI() {
        return facadeClientRA.getURI();
    }

    public String getAnchorDir() {
        return anchorDir;
    }

    protected URI[] toURIs(final String uriList) {
        String[] uris = uriList.split(" ");
        URI[] result = new URI[uris.length];
        for (int i = 0; i < uris.length; i++) {
            result[i] = URI.create(uris[i]);
        }
        return result;
    }

    protected AppClientArchivist getFacadeArchivist() {
        if (facadeArchivist == null) {
            facadeArchivist = habitat.getComponent(AppClientArchivist.class);
        }
        return facadeArchivist;
    }

    public void validateDescriptor() {
        getFacadeArchivist().validate(classLoader);
    }

    /**
     * Returns a Facade object for the specified app client group facade.
     * <p>
     * The caller-supplied information is used to select the first app client
     * facade in the app client group that matches either the main class or
     * the app client name.  If the caller-supplied values are both null then
     * the method returns the first app client facade in the group.  If the
     * caller passes at least one non-null selector (main class or app client
     * name) but no app client matches, the method returns null.
     *
     * @param groupFacadeURI URI to the app client group facade
     * @param callerSuppliedMainClassName main class name to find; null if
     * the caller does not require selection based on the main class name
     * @param callerSuppliedAppName (display) nane of the app client to find; null
     * if the caller does not require selection based on display name
     * @return a Facade object representing the selected app client facade;
     * null if at least one of callerSuppliedMainClasName and callerSuppliedAppName
     * is not null and no app client matched the selection criteria
     * @throws java.io.IOException
     * @throws com.sun.enterprise.module.bootstrap.BootException
     * @throws java.net.URISyntaxException
     * @throws javax.xml.stream.XMLStreamException
     */
    static FacadeLaunchable newFacade(
            final Habitat habitat,
            final ReadableArchive facadeRA, final String callerSuppliedMainClassName, final String callerSuppliedAppName) throws IOException, BootException, URISyntaxException, XMLStreamException, SAXParseException, UserError {
        Manifest mf = facadeRA.getManifest();
        final Attributes mainAttrs = mf.getMainAttributes();
        FacadeLaunchable result = null;
        if (mainAttrs.containsKey(GLASSFISH_APPCLIENT)) {
            result = new FacadeLaunchable(habitat, mainAttrs, facadeRA,
                    dirContainingStandAloneFacade(facadeRA));
        } else {
            /*
             * The facade does not contain GlassFish-AppClient so if it is
             * a facade it must be an app client group facade.  Select
             * which app client facade within the group, if any, matches
             * the caller's selection criteria.
             */
            final String facadeGroupURIs = mainAttrs.getValue(GLASSFISH_APPCLIENT_GROUP);
            if (facadeGroupURIs != null) {
                result = selectFacadeFromGroup(
                        habitat, facadeRA.getURI(), archiveFactory,
                        facadeGroupURIs, callerSuppliedMainClassName,
                        callerSuppliedAppName, dirContainingClientFacadeInGroup(facadeRA));
            } else {
                return null;
            }
        }
        return result;
    }

    private static String dirContainingStandAloneFacade(final ReadableArchive facadeRA) throws URISyntaxException {
        final URI fileURI = new URI("file", facadeRA.getURI().getRawSchemeSpecificPart(), null);
        return new File(fileURI).getParent();
    }

    private static String dirContainingClientFacadeInGroup(final ReadableArchive groupFacadeRA) throws URISyntaxException {
        final String ssp = groupFacadeRA.getURI().getRawSchemeSpecificPart();
        final URI fileURI = new URI("file", ssp.substring(0, ssp.length() - ".jar".length()) + "/", null);
        return new File(fileURI).getAbsolutePath();
    }

    public Class getMainClass() throws ClassNotFoundException {
        return Class.forName(mainClassNameToLaunch, true, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Return the augmented descriptor constructed during deployment and
     * stored in the facade client JAR.
     * @param loader
     * @return
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXParseException
     */
    public ApplicationClientDescriptor getDescriptor(final ClassLoader loader) throws IOException, SAXParseException {
        if (acDesc == null) {
            /*
             * Open without passing the class loader to avoid validation.
             * We need to handle persistence before validating.
             */
            acDesc = getFacadeArchivist().open(facadeClientRA);
            Application.createApplication(habitat, null, acDesc.getModuleDescriptor());

            getFacadeArchivist().setDescriptor(acDesc);
            /*
             * But save the class loader for later use.
             */
            this.classLoader = loader;
        }
        return acDesc;
    }

    public URI[] getClassPathURIs() {
        return classPathURIs;
    }

    private static FacadeLaunchable selectFacadeFromGroup(
            final Habitat habitat,
            final URI groupFacadeURI, final ArchiveFactory af,
            final String groupURIs, final String callerSpecifiedMainClassName,
            final String callerSpecifiedAppClientName,
            final String anchorDir) throws IOException, BootException, URISyntaxException, SAXParseException, UserError {
        String[] archiveURIs = groupURIs.split(" ");
        /*
         * Search the app clients in the group in order, checking each for
         * a match on either the caller-specified main class or the caller-specified
         * client name.
         */
        if (archiveURIs.length == 0) {
            String msg = localStrings.getLocalString(FacadeLaunchable.class, "appclient.noClientsInGroup", "No app clients are listed in the app client group {0}", new Object[]{groupFacadeURI});
            throw new UserError(msg);
        }
        for (String uriText : archiveURIs) {
            URI clientFacadeURI = groupFacadeURI.resolve(uriText);
            ReadableArchive clientFacadeRA = af.openArchive(clientFacadeURI);
            Manifest facadeMF = clientFacadeRA.getManifest();
            Attributes facadeMainAttrs = facadeMF.getMainAttributes();
            URI clientURI = clientFacadeURI.resolve(facadeMF.getMainAttributes().getValue(GLASSFISH_APPCLIENT));
            ReadableArchive clientRA = af.openArchive(clientURI);
            /*
             * Look for an entry corresponding to the
             * main class or app name the caller requested.  Treat as a%
             * special case if the user specifies no main class and no
             * app name - use the first app client present.  Also use the
             * first app client if there is only one present; warn if
             * the user specified a main class or a name but it does not
             * match the single app client that's present.
             */
            FacadeLaunchable facade = null;
            if (archiveURIs.length == 1) {
                facade = new FacadeLaunchable(habitat, clientFacadeRA, facadeMainAttrs,
                        clientRA, facadeMainAttrs.getValue(GLASSFISH_APPCLIENT_MAIN_CLASS),
                        anchorDir);
                /*
                 * If the user specified a main class or an app name then warn
                 * if that value does not match the one client we found - but
                 * go ahead an run it anyway.
                 */
                if ((callerSpecifiedMainClassName != null &&
                        ! Launchable.LaunchableUtil.matchesMainClassName(clientRA, callerSpecifiedMainClassName))
                    ||
                    (callerSpecifiedAppClientName != null &&
                        ! Launchable.LaunchableUtil.matchesName(
                            groupFacadeURI, clientFacadeRA, callerSpecifiedAppClientName))) {
                    final String msg = localStrings.getLocalString(FacadeLaunchable.class, "appclient.singleNestedClientNoMatch", "Using the only client in {0} even though it does not match the specified main class name or client name", new Object[]{groupFacadeURI});
                    logger.warning(msg);
                }
            } else if (Launchable.LaunchableUtil.matchesMainClassName(clientRA, callerSpecifiedMainClassName)) {
                facade = new FacadeLaunchable(habitat, clientFacadeRA, 
                        facadeMainAttrs, clientRA, callerSpecifiedMainClassName,
                        anchorDir);
            } else if (Launchable.LaunchableUtil.matchesName(groupFacadeURI, clientFacadeRA, callerSpecifiedAppClientName)) {
                /*
                 * Get the main class name from the matching client JAR's manifest.
                 */
                Manifest clientMF = clientRA.getManifest();
                Attributes mainAttrs = clientMF.getMainAttributes();
                String clientMainClassName = mainAttrs.getValue(Attributes.Name.MAIN_CLASS);
                facade = new FacadeLaunchable(habitat, clientFacadeRA,
                        facadeMainAttrs, clientRA,
                        clientMainClassName,
                        anchorDir);
            }
            if (facade != null) {
                return facade;
            }
        }
        /*
         * No client facade matched the caller-provided selection (either
         * main class name or app client name.  Yet we know we're working
         * with a group facade, so report the failure to find a matching
         * client as an error.
         */
        final String msg = localStrings.getLocalString(FacadeLaunchable.class, "appclient.noMatchingClientInGroup", "No app client in the app client group {0} matches the main class name \"{1}\" or the app client name \"{2}\"", new Object[]{groupFacadeURI, callerSpecifiedMainClassName, callerSpecifiedAppClientName});
        throw new UserError(msg);
    }
}
