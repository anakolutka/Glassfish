/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 
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

package org.glassfish.appclient.server.core.jws;

import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.logging.LogDomains;
import java.beans.PropertyChangeEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.container.EndpointRegistrationException;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.appclient.server.core.AppClientDeployerHelper;
import org.glassfish.appclient.server.core.AppClientServerApplication;
import org.glassfish.appclient.server.core.jws.ExtensionFileManager.Extension;
import org.glassfish.appclient.server.core.jws.servedcontent.ASJarSigner;
import org.glassfish.appclient.server.core.jws.servedcontent.AutoSignedContent;
import org.glassfish.appclient.server.core.jws.servedcontent.Content;
import org.glassfish.appclient.server.core.jws.servedcontent.DynamicContent;
import org.glassfish.appclient.server.core.jws.servedcontent.FixedContent;
import org.glassfish.appclient.server.core.jws.servedcontent.SimpleDynamicContentImpl;
import org.glassfish.appclient.server.core.jws.servedcontent.StaticContent;
import org.glassfish.appclient.server.core.jws.servedcontent.TokenHelper;
import org.glassfish.deployment.common.DownloadableArtifacts.FullAndPartURIs;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;
import org.jvnet.hk2.config.types.Property;

/**
 * Encapsulates information related to Java Web Start support for a single
 * app client.
 * <p>
 * The AppClientServerApplication creates one instance of this class for each
 * app client that is deployed - either standalone or as part of an EAR.
 *
 * @author tjquinn
 */
@Service
@Scoped(PerLookup.class)
public class JavaWebStartInfo implements ConfigListener {

    private final static String GLASSFISH_DIRECTORY_PREFIX = "glassfish/";

    @Inject
    private JWSAdapterManager jwsAdapterManager;

    @Inject
    private ASJarSigner jarSigner;

    @Inject
    private DeveloperContentHandler dch;

    @Inject
    private ExtensionFileManager extensionFileManager;

    private AppClientServerApplication acServerApp;

    private Set<Content> myContent = null;

    private DeploymentContext dc = null;

    private TokenHelper tHelper;

    private Logger logger;

    private VendorInfo vendorInfo;

    final private Map<String,StaticContent> staticContent = new HashMap<String,StaticContent>();
    final private Map<String,DynamicContent> dynamicContent = new HashMap<String,DynamicContent>();

    private static final String JNLP_MIME_TYPE = "application/x-java-jnlp-file";

    private static final String DOC_TEMPLATE_PREFIX = "/org/glassfish/appclient/server/core/jws/templates/";

    private static final String MAIN_DOCUMENT_TEMPLATE =
            DOC_TEMPLATE_PREFIX + "appclientMainDocumentTemplate.jnlp";
    private static final String CLIENT_DOCUMENT_TEMPLATE =
            DOC_TEMPLATE_PREFIX + "appclientClientDocumentTemplate.jnlp";

        //, "appclientClientDocumentTemplate.jnlp"
        //, "appclientClientFacadeDocumentTemplate.jnlp"

    private static final String MAIN_IMAGE_XML_PROPERTY_NAME =
            "appclient.main.information.images";
    private static final String APP_LIBRARY_EXTENSION_PROPERTY_NAME = "app.library.extension";
    private static final String APP_CLIENT_MAIN_CLASS_ARGUMENTS_PROPERTY_NAME =
            "appclient.main.class.arguments";
    private static final String CLIENT_FACADE_JAR_PATH_PROPERTY_NAME =
            "client.facade.jar.path";
    private static final String CLIENT_JAR_PATH_PROPERTY_NAME =
            "client.jar.path";


    /**
     * records if the app client is eligible for Java Web Start support, as
     * defined in the developer-provided sun-application-client.xml descriptor
     */
    private boolean isJWSEligible;

    /**
     * records if the containing app is set to enable Java Web
     * Start access (in the domain.xml config for the application and the
     * module) - could be updated from a separate
     * thread if the administrator changes the java-web-start-enabled setting
     */
    private volatile boolean isJWSEnabledAtApp = true;
    private volatile boolean isJWSEnabledAtModule = true;

    private final JavaWebStartState jwsState = new JavaWebStartState();

    private final static String JAVA_WEB_START_ENABLED_PROPERTY_NAME = "" +
            "java-web-start-enabled";

    private AppClientDeployerHelper helper;

    private ApplicationClientDescriptor acDesc;

    private String jnlpDoc;

    private static LocalStringsImpl localStrings = new LocalStringsImpl(JavaWebStartInfo.class);
    private static LocalStringsImpl servedContentLocalStrings =
            new LocalStringsImpl(TokenHelper.class);

    private static class SignedSystemContentFromApp {
        private final String tokenName;
        private final String relativePath;

        private SignedSystemContentFromApp(String tokenName, String relativePath) {
            this.tokenName = tokenName;
            this.relativePath = relativePath;
        }

        String getRelativePath() {
            return relativePath;
        }

        String getTokenName() {
            return tokenName;
        }

        URI getRelativePathURI() {
            return URI.create(relativePath);
        }
    }

    private final static List<SignedSystemContentFromApp> SIGNED_SYSTEM_CONTENT_SERVED_AT_APP_LEVEL =
            Arrays.asList(
                new SignedSystemContentFromApp(
                    "gf-client.jar",
                    GLASSFISH_DIRECTORY_PREFIX + "modules/gf-client.jar"),
                new SignedSystemContentFromApp(
                    "gf-client-module.jar",
                    GLASSFISH_DIRECTORY_PREFIX + "modules/gf-client-module.jar")
                    );
    /**
     * Completes initialization of the object.  Should be invoked immediate
     * after the object is created by the habitat.
     *
     * @param acServerApp the per-client AppClientServerApplication object for the client of interest
     */
    public void init(final AppClientServerApplication acServerApp) {
        this.acServerApp = acServerApp;
        helper = acServerApp.helper();
        acDesc = acServerApp.getDescriptor();
        this.logger = LogDomains.getLogger(AppClientServerApplication.class,
                LogDomains.ACC_LOGGER);
        dc = acServerApp.dc();
        isJWSEligible = acDesc.getJavaWebStartAccessDescriptor().isEligible();
        isJWSEnabledAtApp = isJWSEnabled(dc.getAppProps());
        isJWSEnabledAtModule = isJWSEnabled(dc.getModuleProps());
        tHelper = TokenHelper.newInstance(helper, vendorInfo());
        final String devJNLPDoc = acDesc.getJavaWebStartAccessDescriptor().getJnlpDocument();
        final File sourceDir = acDesc.getApplication().isVirtual() ?
            dc.getSourceDir() : new File(dc.getSource().getParentArchive().getURI());
        this.jnlpDoc = devJNLPDoc;
        dch.init(dc.getClassLoader(),
                    tHelper,
                    sourceDir,
                    staticContent,
                    dynamicContent,
                    devJNLPDoc);
    }

    /**
     * Starts Java Web Start services for this client, if the client is
     * eligible (as decided by the developer) and enabled (as decided by the
     * administrator).
     */
    public void start() {
    /*
         * The developer might have disabled Java Web Start support in the
         * sun-application-client.xml or in the domain's configuration,
         * so check those before starting JWS services.
         */
        if (isJWSRunnable()) {
            jwsState.transition(JavaWebStartState.Action.START, new Runnable() {
                @Override
                public void run() {
                    try {
                        startJWSServices();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        }
    }

    /**
     * Stops Java Web Start services for this client.
     */
    public void stop() {
        jwsState.transition(JavaWebStartState.Action.STOP, new Runnable() {
            @Override
            public void run() {
                try {
                    stopJWSServices();
                } catch (EndpointRegistrationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    /**
     * Suspends Java Web Start services for this client.
     */
    public void suspend() {
        jwsState.transition(JavaWebStartState.Action.SUSPEND, new Runnable() {
            @Override
            public void run() {
                suspendJWSServices();
            }
        });

    }

    /**
     * Resumes Java Web Start services for this client.
     */
    public void resume() {
        if (isJWSRunnable()) {
            jwsState.transition(JavaWebStartState.Action.RESUME, new Runnable() {
                @Override
                public void run() {
                    resumeJWSServices();
                }
            });
        }
    }

    private void startJWSServices() throws EndpointRegistrationException, IOException {
        if (myContent == null) {
            processExtensionReferences();
            myContent = addClientContentToHTTPAdapters();
        }

        /*
         * Currently, we implement the ability to disable or enable app clients
         * within an EAR by marking the associated content as disabled or
         * enabled, which the Grizzly adapter looks at before responding to
         * a request for that bit of content.  So mark all the content as
         * started.
         */
        for (Content c : myContent) {
            c.start();
        }
        logger.log(Level.INFO, "enterprise.deployment.appclient.jws.started",
            new Object[] {acServerApp.moduleExpression(),
            JWSAdapterManager.userFriendlyContextRoot(acServerApp)});
    }

    private void processExtensionReferences() throws IOException {
        
        // TODO: needs to be expanded to handle signed library JARS, perhap signed by different certs

        tHelper.setProperty(APP_LIBRARY_EXTENSION_PROPERTY_NAME, 
                jarElementsForExtensions(extensionFileManager.findExtensionTransitiveClosure(
                new File(helper.appClientServerURI(dc)).getParentFile(),
                dc.getSource().getManifest().getMainAttributes())));

    }

    private String jarElementsForExtensions(final Set<Extension> exts) {
        final StringBuilder sb = new StringBuilder();
        for (Extension e : exts) {
            sb.append("<jar href=" + JWSAdapterManager.publicExtensionHref(e) + "/>");
        }
        return sb.toString();
    }
    
    private void stopJWSServices() throws EndpointRegistrationException {
        /*
         * Mark all this client's content as stopped so the Grizzly adapter
         * will not serve it.
         */
        for (Content c : myContent) {
            c.stop();
        }

        jwsAdapterManager.removeContentForAppClient(acServerApp.deployedAppName(), acServerApp);
        logger.log(Level.INFO, "enterprise.deployment.appclient.jws.stopped",
                acServerApp.moduleExpression());
    }

    private void suspendJWSServices() {
        for (Content c : myContent) {
            c.suspend();
        }
    }

    private void resumeJWSServices() {
        for (Content c : myContent) {
            c.resume();
        }
    }


    /**
     * Returns if this client is enabled for Java Web Start access.
     * <p>
     * The administrator can set the java-web-start-enabled property at
     * either the application level or the module level or both.  For this
     * client to be enabled, any such specified property must be set to true.
     * The default is true.
     */
    private boolean isJWSEnabled(final Properties props) {
        boolean result = true;
        final String propsSetting = props.getProperty(JAVA_WEB_START_ENABLED_PROPERTY_NAME);
        if (propsSetting != null) {
            result &= Boolean.parseBoolean(propsSetting);
        }
        return result;
    }

    private boolean isJWSEnabled() {
        return isJWSEnabledAtApp && isJWSEnabledAtModule;
    }

    private boolean isJWSRunnable() {
        if ( ! isJWSEligible) {
            logger.log(Level.INFO, "enterprise.deployment.appclient.jws.noStart.ineligible",
                    acServerApp.moduleExpression());
        }

        if ( ! isJWSEnabled()) {
            logger.log(Level.INFO, "enterprise.deployment.appclient.jws.noStart.disabled",
                    acServerApp.moduleExpression());
        }
        return isJWSEligible && isJWSEnabled();
    }

    private Set<Content> addClientContentToHTTPAdapters() throws EndpointRegistrationException, IOException {

        /*
         * NOTE - Be sure to initialize the static content first.  That method
         * assigns some properties that can appear as placeholders in the
         * dynamic content.
         */
        initClientStaticContent();

        initClientDynamicContent();

        dch.addDeveloperContent(jnlpDoc);
        
        Set<Content> result = new HashSet<Content>(staticContent.values());
        result.addAll(dynamicContent.values());

        jwsAdapterManager.addContentForAppClient(acServerApp.deployedAppName(),
                acServerApp, tHelper.tokens(),
                staticContent, dynamicContent);
        return result;
    }

    private void initClientStaticContent()
            throws IOException, EndpointRegistrationException {

        /*
         * The client-level adapter's static content is the app client JAR and
         * the app client facade.
         */
        createAndAddSignedContentFromAppFile(
                staticContent,
                helper.appClientServerURI(dc),
                helper.appClientUserURI(dc),
                CLIENT_JAR_PATH_PROPERTY_NAME);
        
        createAndAddSignedStaticContentFromGeneratedFile(
                staticContent, 
                helper.facadeServerURI(dc),
                helper.facadeUserURI(dc),
                CLIENT_FACADE_JAR_PATH_PROPERTY_NAME);

        /*
         * Make sure that there are versions of gf-client.jar and gf-client-module.jar
         * that are signed by the same cert used to sign the facade JAR for
         * this app.  That's because the user might have chosen to sign using
         * a particular alias so the end-users will accept JARs signed by
         * the corresponding cert.  (Java Web Start will prompt them to do this
         * during the download of signed JARs.)
         *
         * Note that the following logic makes sure that such signed versions
         * exist.  If multiple apps use the same cert to sign JARs, then the
         * multiple instances of AutoSignedContent class for the same signed
         * JAR will point to and reuse the same signed JAR, rather than
         * re-sign it each time an app needed it is started.
         */
        addSignedSystemContent(staticContent);

        /*
         * The developer might have used the sun-application-client.xml
         * java-web-start-support/vendor setting to communicate icon and/or
         * splash screen images URIs.
         */
        prepareImageInfo(staticContent);

        /*
         * Make sure that all the EAR-level JARs that this client refers to
         * are represented as content as well.  This could result in a JAR
         * referenced from multiple clients being added more than once, but
         * these library JARs are recorded in a set so each appears at most once.
         */
        for (FullAndPartURIs artifact : helper.earLevelDownloads()) {
            final String uriString = artifact.getPart().toASCIIString();
            /*
             * The EAR-level downloads include the unsigned client JAR(s).  If
             * we've generated a signed copy and added it to the static content
             * do not overwrite that entry.
             */
            if ( ! staticContent.containsKey(uriString)) {
                staticContent.put(uriString, new FixedContent(new File(artifact.getFull())));
            }
        }
    }

    private void addSignedSystemContent(
            final Map<String,StaticContent> content) {
        for (SignedSystemContentFromApp signedContentFromApp : SIGNED_SYSTEM_CONTENT_SERVED_AT_APP_LEVEL) {
            final AutoSignedContent signedContent =
                    jwsAdapterManager.appLevelSignedSystemContent(
                        signedContentFromApp.getRelativePath(),
                        jwsAdapterManager.signingAlias(dc));
            recordStaticContent(content, signedContent,
                    signedContentFromApp.getRelativePathURI(),
                    signedContentFromApp.getTokenName());
        }
    }

    private void createAndAddSignedContentFromAppFile(final Map<String,StaticContent> content,
            final URI uriToFile,
            final URI uriForLookup,
            final String tokenName) {

        final File unsignedFile = new File(uriToFile);
        final File signedFile = signedFileForProvidedAppFile(unsignedFile);
        createAndAddSignedStaticContent(content, unsignedFile, signedFile,
                uriForLookup, tokenName);
    }

    private void createAndAddSignedStaticContentFromGeneratedFile(final Map<String,StaticContent> content,
            final URI uriToFile,
            final URI uriForLookup,
            final String tokenName) {

        final File unsignedFile = new File(uriToFile);
        final File signedFile = signedFileForGeneratedAppFile(unsignedFile);
        createAndAddSignedStaticContent(content, unsignedFile, signedFile,
                uriForLookup, tokenName);
    }

    private void createAndAddSignedStaticContent(
            final Map<String,StaticContent> content,
            final File unsignedFile,
            final File signedFile,
            final URI uriForLookup,
            final String tokenName
            ) {
        signedFile.getParentFile().mkdirs();
        final StaticContent signedJarContent = new AutoSignedContent(
                unsignedFile,
                signedFile,
                jwsAdapterManager.signingAlias(dc),
                jarSigner);
        recordStaticContent(content, signedJarContent, uriForLookup, tokenName);
    }
    
    private void recordStaticContent(final Map<String,StaticContent> content,
            final StaticContent newContent,
            final URI uriForLookup,
            final String tokenName) {

        final String uriStringForLookup = uriForLookup.toASCIIString();
        recordStaticContent(content, newContent, uriStringForLookup);
        tHelper.setProperty(tokenName, uriForLookup.toASCIIString());
    }

    private void recordStaticContent(final Map<String,StaticContent> content,
            final StaticContent newContent,
            final String uriStringForLookup) {
        content.put(uriStringForLookup, newContent);
        logger.fine("Recording static content: URI for lookup = " +
                uriStringForLookup + "; content = " + newContent.toString());
    }

    private File signedFileForGeneratedAppFile(final File unsignedFile) {
        /*
         * Signed files at the app level go in
         *
         * generated/xml/(appName)/signed/(path-within-app-of-unsigned-file)
         *
         * and when we're signing a generated file we just use its URI
         * relative to the app's scratch directory to compute the URI relative
         * to generated/xml/(appName)/signed where the signed file should reside.
         */
        final File rootForSignedFilesInApp = new File(dc.getScratchDir("xml"), "signed/");
        rootForSignedFilesInApp.mkdirs();
        final URI unsignedFileURIRelativeToXMLDir = dc.getScratchDir("xml").toURI().
                relativize(unsignedFile.toURI());
        final URI signedFileURI = rootForSignedFilesInApp.toURI().resolve(unsignedFileURIRelativeToXMLDir);
        return new File(signedFileURI);
    }
    
    private File signedFileForProvidedAppFile(final File unsignedFile) {
        /*
         * Place a signed file for a developer-provided file at
         * 
         * generated/xml/(appName)/signed/(path-within-app-of-unsigned-file)
         * 
         * 
         */
        final File rootForSignedFilesInApp = new File(dc.getScratchDir("xml"), "signed/");
        rootForSignedFilesInApp.mkdirs();
        final URI signedFileURI = rootForSignedFilesInApp.toURI().
                resolve(helper.appClientURIWithinApp(dc));
        return new File(signedFileURI);
    }

    private void initClientDynamicContent() throws IOException {

        tHelper.setProperty(APP_CLIENT_MAIN_CLASS_ARGUMENTS_PROPERTY_NAME, "");

        final String mainDocument = dch.combineJNLP(
                    textFromURL(MAIN_DOCUMENT_TEMPLATE));
        createAndAddDynamicContentFromTemplateText(
                dynamicContent, tHelper.mainJNLP(), mainDocument);

        /*
         * Add the main JNLP again but with an empty URI string so the user
         * can launch the app client by specifying only the context root.
         */
        createAndAddDynamicContentFromTemplateText(dynamicContent, "", mainDocument);
        createAndAddDynamicContent(dynamicContent, tHelper.clientJNLP(), CLIENT_DOCUMENT_TEMPLATE);
    }

    private void createAndAddDynamicContent(
            final Map<String,DynamicContent> content,
            final String uriStringForContent,
            final String uriStringForTemplate) throws IOException {
        createAndAddDynamicContentFromTemplateText(
                content, uriStringForContent,
                textFromURL(uriStringForTemplate));
    }

    private void createAndAddDynamicContentFromTemplateText(
            final Map<String,DynamicContent> content,
            final String uriStringForContent,
            final String templateText) throws IOException {
        final String processedTemplate = Util.replaceTokens(
                templateText, tHelper.tokens());
        content.put(uriStringForContent, newDynamicContent(processedTemplate,
                JNLP_MIME_TYPE));
        logger.fine("Adding dyn content " + uriStringForContent + System.getProperty("line.separator") +
                (logger.isLoggable(Level.FINER) ? processedTemplate : ""));
    }

    private static DynamicContent newDynamicContent(final String template,
            final String mimeType) {
        return new SimpleDynamicContentImpl(template, mimeType);
    }

    /**
     * Prepares XML (for the generated JNLP) and the static content
     * for the icon image, the splash screen image, neither, or
     * both, depending on the contents (if any) of the <vendor> text in the
     * developer-provided sun-application-client.xml descriptor.
     */
    private void prepareImageInfo(final Map<String,StaticContent> staticContent) throws IOException {
        StringBuilder result = new StringBuilder();

        /*
         * Deployment has already expanded the app client module into a
         * directory, so each image entry of the JAR which the developer
         * specified in the descriptor should already reside as a
         * file on the disk within the DeploymentContext.getSource() directory.
         */
        addImageContentIfSpecified(vendorInfo().getImageURI(),
                vendorInfo().JNLPImageURI(), staticContent);
        addImageContentIfSpecified(vendorInfo().getSplashImageURI(),
                vendorInfo().JNLPSplashImageURI(), staticContent);

//        if (result.length() == 0) {
//            result.append("<!-- No image information specified in sun-application-client.xml -->");
//        }
//        tokens.setProperty(MAIN_IMAGE_XML_PROPERTY_NAME, result.toString());
    }

    private void addImageContentIfSpecified(
            final String imageURIStringWithinAppClient,
            final String imageURIStringForJNLP,
            final Map<String,StaticContent> staticContent) {

        if (imageURIStringWithinAppClient == null ||
                imageURIStringWithinAppClient.length() == 0) {
            return;
        }

        final URI absoluteImageURI = dc.getSource().getURI().resolve(imageURIStringWithinAppClient);
        final File imageFile = new File(absoluteImageURI);
        if ( ! imageFile.exists()) {
            return;
        }

        staticContent.put(imageURIStringForJNLP,
                new FixedContent(imageFile));
    }
    

    private VendorInfo vendorInfo() {
        if (vendorInfo == null) {
            vendorInfo = new VendorInfo(
                    acDesc.getJavaWebStartAccessDescriptor().getVendor(),
                    helper.pathToAppclientWithinApp(dc));
        }
        return vendorInfo;
    }

    public static class VendorInfo {
        private String vendorStringFromDescriptor;
        private String vendor = "";
        private String imageURIString = "";
        private String splashImageURIString = "";
        private final String JNLPPathFullPrefix;

        public VendorInfo(String vendorStringFromDescriptor,
                final String JNLPPathPrefix) {
            this.JNLPPathFullPrefix = "__content/" + JNLPPathPrefix;
            this.vendorStringFromDescriptor = vendorStringFromDescriptor != null ?
                vendorStringFromDescriptor : "";
            String [] parts = this.vendorStringFromDescriptor.split("::");
            if (parts.length == 1) {
                vendor = parts[0];
            } else if (parts.length == 2) {
                imageURIString = parts[0];
                vendor = parts[1];
            } else if (parts.length == 3) {
                imageURIString = parts[0];
                splashImageURIString = parts[1];
                vendor = parts[2];
            }
            if (vendor.length() == 0) {
                vendor = servedContentLocalStrings.get("jws.defaultVendorName");
            }
        }

        public String getVendor() {
            return vendor;
        }

        public String getImageURI() {
            return imageURIString;
        }

        public String getSplashImageURI() {
            return splashImageURIString;
        }

        public String JNLPImageURI() {
            return (imageURIString.length() > 0) ? 
                JNLPPathFullPrefix + imageURIString : "";
        }

        public String JNLPSplashImageURI() {
            return (splashImageURIString.length() > 0) ? 
                JNLPPathFullPrefix + splashImageURIString : "";
        }
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        /* Record any events we tried to process but could not. */
        List<UnprocessedChangeEvent> unprocessedEvents = new ArrayList<UnprocessedChangeEvent>();

        for (PropertyChangeEvent event : events) {
            try {
                processChangeEventIfInteresting(event);
            } catch (Exception e) {
                UnprocessedChangeEvent uce =
                        new UnprocessedChangeEvent(event, e.getLocalizedMessage());
                unprocessedEvents.add(uce);
            }
        }

        return (unprocessedEvents.size() > 0) ? new UnprocessedChangeEvents(unprocessedEvents) : null;
    }

    private void processChangeEventIfInteresting(final PropertyChangeEvent event) throws EndpointRegistrationException {
        /*
         * If the source is of type Application or Module and the newValue is of type
         * Property then this could be a change we're interested in.
         */
        final boolean isSourceApp = event.getSource() instanceof
                com.sun.enterprise.config.serverbeans.Application;
        final boolean isSourceModule = event.getSource() instanceof
                com.sun.enterprise.config.serverbeans.Module;

        if (     (! isSourceApp && ! isSourceModule)
              || ! (event.getNewValue() instanceof Property)) {
            return;
        }

        /*
         * Make sure the property name is java-web-start-enabled.
         */
        Property newPropertySetting = (Property) event.getNewValue();
        if ( ! newPropertySetting.getName().equals(JAVA_WEB_START_ENABLED_PROPERTY_NAME)) {
            return;
        }

        String eventSourceName;
        String thisAppOrModuleName;
        if (isSourceApp) {
            eventSourceName = ((com.sun.enterprise.config.serverbeans.Application) event.getSource()).getName();
            thisAppOrModuleName = acServerApp.registrationName();
        } else {
            eventSourceName = ((com.sun.enterprise.config.serverbeans.Module) event.getSource()).getName();
            thisAppOrModuleName = acDesc.getModuleName();
        }

        if ( ! thisAppOrModuleName.equals(eventSourceName)) {
            return;
        }

        /*
         * At this point we know that the event applies to this app client,
         * so return a Boolean carrying the newly-assigned value.
         */
        final Boolean newEnabledValue = Boolean.valueOf(newPropertySetting.getValue());
        final Property oldPropertySetting = (Property) event.getOldValue();
        final String oldPropertyValue = (oldPropertySetting != null)
                ? oldPropertySetting.getValue()
                : null;
        final Boolean oldEnabledValue = (oldPropertyValue == null
                ? Boolean.TRUE
                : Boolean.valueOf(oldPropertyValue));

        /*
         * Record the new value of the relevant enabled setting.
         */
        if (isSourceApp) {
            isJWSEnabledAtApp = newEnabledValue;
                } else {
            isJWSEnabledAtModule = newEnabledValue;
        }

        /*
         * Now act on the change of state.
         */
        if ( ! newEnabledValue.equals(oldEnabledValue)) {
            if (newEnabledValue) {
                start();
            } else {
                stop();
            }
        }
    }

    public static String textFromURL(final String templateURLString) throws IOException {
        final InputStream is = AppClientServerApplication.class.getResourceAsStream(templateURLString);
        if (is == null) {
            throw new FileNotFoundException(templateURLString);
        }
        StringBuilder sb = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(is);
        char[] buffer = new char[1024];
        int charsRead;
        try {
            while ((charsRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, charsRead);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IOException(templateURLString, e);
        } finally {
            try {
                reader.close();
            } catch (IOException ignore) {
                throw new IOException("Error closing template stream after error", ignore);
            }
        }

    }


}
