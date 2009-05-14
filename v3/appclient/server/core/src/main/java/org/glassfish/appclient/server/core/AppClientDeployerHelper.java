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
package org.glassfish.appclient.server.core;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.deployment.deploy.shared.OutputJarArchive;
import com.sun.enterprise.universal.io.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DownloadableArtifacts;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;

/**
 * Encapsulates the details of generating the required JAR file(s),
 * depending on whether the app client is stand-alone or is nested
 * inside an EAR.
 * <p>
 * See also {@link StandaloneAppClientDeployerHelper} and
 * {@link NestedAppClientDeployerHelper}, the concrete implementation subclasses
 * of AppClientDeployerHelper.
 *
 * @author tjquinn
 */
abstract class AppClientDeployerHelper {

    private final DeploymentContext dc;
    private final ApplicationClientDescriptor appClientDesc;
    private final URI facadeDirURI;
//    private final String renamedOriginalJarFileName;
    protected final AppClientArchivist archivist;

    /**
     * Returns the correct concrete implementation of Helper.
     * @param dc the DeploymentContext for this deployment
     * @return an instance of the correct type of Helper
     * @throws java.io.IOException
     */
    static AppClientDeployerHelper newInstance(
            final DeploymentContext dc,
            final AppClientArchivist archivist) throws IOException {
        ApplicationClientDescriptor bundleDesc = dc.getModuleMetaData(ApplicationClientDescriptor.class);
        Application application = bundleDesc.getApplication();
        boolean insideEar = ! application.isVirtual();

        return (insideEar ? new NestedAppClientDeployerHelper(dc, bundleDesc, archivist)
                          : new StandaloneAppClientDeployerHelper(dc, bundleDesc, archivist));
    }

    protected AppClientDeployerHelper(
            final DeploymentContext dc,
            final ApplicationClientDescriptor bundleDesc,
            final AppClientArchivist archivist) throws IOException {
        super();
        this.dc = dc;
        this.appClientDesc = bundleDesc;
        this.archivist = archivist;
        this.facadeDirURI = facadeServerURI(dc);
    }

    /**
     * Returns the URI to the server's copy of the facade JAR file.
     * @param dc the deployment context for the current deployment
     * @return
     */
    protected abstract URI facadeServerURI(DeploymentContext dc);

    /**
     * Returns the URI for the facade JAR, relative to the download
     * directory to which the user will fetch the relevant JARs (either
     * as part of "deploy --retrieve" or "get-client-stubs."
     * @param dc the deployment context for the current deployment
     * @return
     */
    protected abstract URI facadeUserURI(DeploymentContext dc);

    /**
     * Returns the file name (and type) for the facade, excluding any
     * directory information.
     * @param dc the deployment context for the current deployment
     * @return
     */
    protected abstract String facadeFileNameAndType(DeploymentContext dc);

    /**
     * Returns the URI to the developer's original app client JAR within
     * the download directory the user specifies in "deploy --retrieve" or
     * "get-client-stubs."
     * 
     * @param dc
     * @return
     */
    protected abstract URI appClientUserURI(DeploymentContext dc);

    /**
     * Returns the URI to be used for the GlassFish-AppClient manifest entry
     * in the facade.
     * 
     * @param dc
     * @return
     */
    protected abstract URI appClientUserURIForFacade(DeploymentContext dc);

    /**
     * Returns the URI to the server's copy of the developer's original app
     * client JAR.
     *
     * @param dc
     * @return
     */
    protected abstract URI appClientServerURI(DeploymentContext dc);

    /**
     * Returns the URI within the enclosing app of the app client JAR.
     * Stand-alone app clients are considered to lie within an "implied"
     * containing app; the URI for such app clients is just the file name
     * and type.  The URI for nested app clients within an EAR is the
     * module URI to the app client.
     * 
     * @param dc
     * @return
     */
    protected abstract URI appClientURIWithinApp(DeploymentContext dc);

    /**
     *
     * Returns the class path to be stored in the manifest for the
     * generated facade JAR file.
     *
     * @return
     */
    protected abstract String facadeClassPath();

    protected final DeploymentContext dc() {
        return dc;
    }

    protected ApplicationClientDescriptor appClientDesc() {
        return appClientDesc;
    }

//    protected String renamedOriginalJarFileName() {
//        return renamedOriginalJarFileName;
//    }

//    protected String chooseNameForRenamedOriginalJar(final DeploymentContext dc) throws IOException {
//        ModuleDescriptor modDesc = appClientDesc.getModuleDescriptor();
//        String moduleURI = modDesc.getArchiveUri();
//        return renameModUri(moduleURI, dc.getSource().getParentArchive());
//    }

//    private static String renameModUri(String jarName, ReadableArchive parent) throws IOException {
//        String ext = ".jar";
//        String suffix = ".orig";
//        String nameNoExt = jarName.substring(0, jarName.length() - 4); //".jar" length
//        String newName = nameNoExt + suffix + ext;
//        if (parent != null) {
//            for (int i = 1; parent.exists(newName); i++) {
//                newName = nameNoExt + suffix + "-" + i + ext;
//            }
//        }
//        return newName;
//    }


    /**
     * Creates a generated manifest for either the facade (for app client or
     * EAR deployments both) and the ${appName}Client.jar
     * file if this is an app client deployment.
     * <p>
     * Most of the manifest's contents is derived from the source, with
     * the class path passed in as an argument because it varies between the facade and
     * the ${appName}Client.jar file.
     *
     * @param sourceManifest
     * @param facadeManifest
     * @param classpath space-separated list of class path elements
     */
    private void initGeneratedManifest(
            final Manifest sourceManifest,
            final Manifest generatedManifest,
            final String classPath) {
        Attributes sourceMainAttrs = sourceManifest.getMainAttributes();
        Attributes facadeMainAttrs = generatedManifest.getMainAttributes();
        facadeMainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        facadeMainAttrs.put(Attributes.Name.MAIN_CLASS,
                AppClientDeployer.APPCLIENT_COMMAND_CLASS_NAME);
        facadeMainAttrs.put(AppClientDeployer.GLASSFISH_APPCLIENT_MAIN_CLASS,
                sourceMainAttrs.getValue(Attributes.Name.MAIN_CLASS));
        facadeMainAttrs.put(AppClientDeployer.GLASSFISH_APPCLIENT,
                appClientUserURIForFacade(dc).toASCIIString());
        String splash = sourceMainAttrs.getValue(AppClientDeployer.SPLASH_SCREEN_IMAGE);
        if (splash != null) {
            facadeMainAttrs.put(AppClientDeployer.SPLASH_SCREEN_IMAGE, splash);
        }
        facadeMainAttrs.put(Attributes.Name.CLASS_PATH, classPath);
    }

    private void writeUpdatedDescriptors(final OutputJarArchive facadeArchive, final ApplicationClientDescriptor acd) throws IOException {
        archivist.setDescriptor(acd);
        archivist.writeDeploymentDescriptors(facadeArchive);
    }

    protected void prepareJARs() throws IOException, URISyntaxException {
        generateAppClientFacade();
//        copyOriginalAppClientJAR(dc());
    }


//    protected void copyOriginalAppClientJAR(final DeploymentContext dc) throws IOException {
//        ReadableArchive originalSource = ((ExtendedDeploymentContext) dc).getOriginalSource();
//        originalSource.open(originalSource.getURI());
//        OutputJarArchive target = new OutputJarArchive();
//        target.create(appClientServerURI(dc));
//        /*
//         * Copy the manifest explicitly because ReadableArchive.entries()
//         * excludes the manifest.
//         */
//        Manifest originalManifest = originalSource.getManifest();
//        OutputStream os = target.putNextEntry(JarFile.MANIFEST_NAME);
//        originalManifest.write(os);
//        target.closeEntry();
//        ClientJarMakerUtils.copyArchive(originalSource, target, Collections.EMPTY_SET);
//        target.close();
//        originalSource.close();
//    }

    protected final void generateAppClientFacade() throws IOException, URISyntaxException {
        OutputJarArchive facadeArchive = new OutputJarArchive();
        facadeArchive.create(facadeServerURI(dc));
        ReadableArchive source = dc.getSource();
        Manifest sourceManifest = source.getManifest();
        Manifest facadeManifest = facadeArchive.getManifest();
        initGeneratedManifest(sourceManifest, facadeManifest, facadeClassPath());
        /*
         * If the developer's app client JAR contains a splash screen, copy
         * it from the original JAR to the facade so the Java launcher can
         * display it when the app client is launched.
         */
        String splash = sourceManifest.getMainAttributes().getValue(AppClientDeployer.SPLASH_SCREEN_IMAGE);
        if (splash != null) {
            ClientJarMakerUtils.copy(source, facadeArchive, splash);
        }
        /*
         * Write the manifest to the facade.
         */
        OutputStream os = facadeArchive.putNextEntry(JarFile.MANIFEST_NAME);
        facadeManifest.write(os);
        facadeArchive.closeEntry();
        /*
         * Write the updated descriptors to the facade.
         */
        writeUpdatedDescriptors(facadeArchive, appClientDesc);
        /*
         * Copy the facade main class into the facade.
         */
        os = facadeArchive.putNextEntry(AppClientDeployer.APPCLIENT_FACADE_CLASS_FILE);
        InputStream is = openByteCodeStream("/" + AppClientDeployer.APPCLIENT_FACADE_CLASS_FILE);
        /*
         * Note that the copyStream closes the output stream.
         */
        FileUtils.copyStream(is, os);
        try {
            is.close();
            facadeArchive.close();
        } catch (IOException ignore) {
        }
    }

    protected InputStream openByteCodeStream(final String resourceName) throws URISyntaxException, MalformedURLException, IOException {
       URI currentModule = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
       URI classURI = currentModule.resolve("gf-client-module.jar!" + resourceName);
       return URI.create("jar:" + classURI.toString()).toURL().openStream();
    }

    
    protected abstract Set<DownloadableArtifacts.FullAndPartURIs> downloads() throws IOException;
}
