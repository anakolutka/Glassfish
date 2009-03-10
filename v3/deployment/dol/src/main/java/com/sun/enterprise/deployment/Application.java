/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package com.sun.enterprise.deployment;

import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.deployment.annotation.introspection.EjbComponentAnnotationScanner;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.deployment.archivist.ApplicationArchivist;
import com.sun.enterprise.deployment.archivist.EjbArchivist;
import com.sun.enterprise.deployment.deploy.shared.InputJarArchive;
import com.sun.enterprise.deployment.interfaces.SecurityRoleMapper;
import com.sun.enterprise.deployment.interfaces.SecurityRoleMapperFactory;
import com.sun.enterprise.deployment.node.ApplicationNode;
import com.sun.enterprise.deployment.runtime.common.SecurityRoleMapping;
import com.sun.enterprise.deployment.types.RoleMappingContainer;
import com.sun.enterprise.deployment.util.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.resource.common.Resource;
import org.jvnet.hk2.component.Habitat;

import javax.enterprise.deploy.shared.ModuleType;
import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Objects of this type encapsulate the data and behaviour of a J2EE
 * application.
 *
 * @author Danny Coward
 */

public class Application extends RootDeploymentDescriptor
        implements Roles, RoleMappingContainer {

    /**
     * default value for the library-directory element
     */
    private static final String LIBRARY_DIRECTORY_DEFAULT_VALUE = "lib";

    private static final String PERSISTENCE_UNIT_NAME_SEPARATOR = "#";

    /**
     * Store generated XML dir to be able to get the generated WSDL
     */
    private String generatedXMLDir;

    // Set of modules in this application
    private Set<ModuleDescriptor<BundleDescriptor>> modules = new OrderedSet<ModuleDescriptor<BundleDescriptor>>();

    // IASRI 4645310
    /**
     * unique id for this application
     */
    private long uniqueId;

    // IASRI 4645310
    /**
     * represents the virtual status of this application object
     */
    private boolean virtual = false;

    // IASRI 4662001, 4720955
    /**
     * represents whether all ejb modules in an application will be pass by
     * value or pass by reference
     */
    private Boolean passByReference = null;

    // table of EJB2.0 CMP descriptors (EntityBeans) 
    // keyed on their class names
    private HashMap cmpDescriptors = null;

    // use a String object as lock so it can be serialized as part
    // of the Application object
    private String cmpDescriptorsLock = new String("cmp descriptors lock");

    // flag to indicate that the memory representation of this application 
    // is not in sync with the disk representation
    private boolean isDirty;

    // data structure to map roles to users and groups
    private SecurityRoleMapper roleMapper;

    // IASRI 4648645 - application registration name
    /**
     * name used to register this application
     */
    private String registrationName;

    // realm associated with this application
    private String realm;

    // Physical entity manager factory corresponding to the unit name of 
    // each application-level persistence unit.  Only available at runtime.
    private Map<String, EntityManagerFactory> entityManagerFactories =
            new HashMap<String, EntityManagerFactory>();

    private Set<String> entityManagerFactoryUnitNames =
            new HashSet<String>();

    // for i18N
    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(Application.class);

    private Set<Role> appRoles;

    private String libraryDirectory;

    private List<SecurityRoleMapping> roleMaps = new ArrayList<SecurityRoleMapping>();

    private boolean loadedFromApplicationXml = true;

    private List<Resource> resourceList = null;

    private boolean isPackagedAsSingleModule = false;

    private final Habitat habitat;

    public Application(Habitat habitat) {
        super("", localStrings.getLocalString(
                "enterprise.deployment.application.description",
                "Application description"));
        this.habitat = habitat;
    }

    // Create logger object per Java SDK 1.4 to log messages
    // introduced Santanu De, Sun Microsystems, March 2002

    static Logger _logger = DOLUtils.getDefaultLogger();


    /**
     * @return the default version of the deployment descriptor
     *         loaded by this descriptor
     */
    public String getDefaultSpecVersion() {
        return ApplicationNode.SPEC_VERSION;
    }

    public boolean isEmpty() {
        return modules.isEmpty();
    }

    /**
     * Creates a new application to hold a standalone module
     *
     * @param name      the application name
     * @param newModule the standalone module descriptor
     * @return the application
     */
    public static Application createApplication(Habitat habitat, String name, ModuleDescriptor<BundleDescriptor> newModule) {

        // create a new empty application
        Application application = new Application(habitat);
        application.setVirtual(true);
        if (name == null && newModule.getDescriptor() != null) {
            name = newModule.getDescriptor().getDisplayName();

        }
        if (name != null) {
            application.setDisplayName(name);
            application.setName(name);
        }

        // add the module to it
        newModule.setStandalone(true);
        newModule.setArchiveUri(name);
        if (newModule.getDescriptor() != null) {
            newModule.getDescriptor().setApplication(application);
        }
        application.addModule(newModule);

        return application;
    }

    public void setGeneratedXMLDirectory(String xmlDir) {
        generatedXMLDir = xmlDir;
    }

    /**
     * Returns the generated XML directory for this app
     */
    public String getGeneratedXMLDirectory() {
        return generatedXMLDir;
    }

    // START OF IASRI 4648645 - application registration name
    /**
     * Sets the registration name for this application. This name is used
     * while deploying the application. The deployment process gurantees
     * that this name is unique.
     *
     * @param appId the registration name used for this application
     */
    public void setRegistrationName(String appId) {

        // at his point we need to swap our RoleMapper, if we have one... 
        SecurityRoleMapper roleMapper = null;
        try {
            roleMapper = getRoleMapper();
        } catch (IllegalArgumentException ignore) {
        }

        if (roleMapper != null) {
            SecurityRoleMapperFactory factory = habitat.getComponent(SecurityRoleMapperFactory.class);
            if (factory == null) {
                throw new IllegalArgumentException(localStrings.getLocalString(
                        "enterprise.deployment.norolemapperfactorydefine",
                        "This application has no role mapper factory defined"));
            }
            factory.removeRoleMapper(getName());
            roleMapper.setName(appId);
            factory.setRoleMapper(appId, roleMapper);
        }

        this.registrationName = appId;
    }

    /**
     * Returns the registration name of this application.
     *
     * @return the registration name of this application
     */
    public String getRegistrationName() {
        if (registrationName != null) {
            return registrationName;
        } else {
            return getName();
        }
    }
    // END OF IASRI 4648645

    /**
     * Set the physical entity manager factory for a persistence unit
     * within this application.
     * This method takes a parameter called persistenceRootUri to support for
     * fully-qualified persistence-unit-name syntax within
     * persistence-unit-refs and persistence-context-refs. The syntax is similar
     * to ejb-link and messge-destination-link. See (EJB 3 core spec: 15.10.2)
     *
     * @param unitName:           Name of the persistence-unit
     * @param persistenceRootUri: uri of the root of the persistence.xml
     *                            (excluding META-INF) in which the persistence unit was defined.
     *                            This uri is relative to the top of the .ear.
     * @param emf:                an entity manager factory.
     */
    public void addEntityManagerFactory(
            String unitName,
            String persistenceRootUri,
            EntityManagerFactory emf) {

        String fullyQualifiedUnitName = persistenceRootUri +
                PERSISTENCE_UNIT_NAME_SEPARATOR + unitName;

        // Always allow fully qualified lookup.
        entityManagerFactories.put(fullyQualifiedUnitName, emf);

        // Allow unqualified lookup, unless there are multiple .ear level
        // persistence units declaring the same persistence unit name. In that
        // case, only a fully-qualified lookup will work.  Note that even
        // though the entity manager factory map might contain more than one
        // key pointing to the same entity manager factory, the behavior
        // of getEntityManagerFactories() is not affected since it returns a Set.
        if (entityManagerFactoryUnitNames.contains(unitName)) {
            entityManagerFactories.remove(unitName);
        } else {
            entityManagerFactories.put(unitName, emf);
            entityManagerFactoryUnitNames.add(unitName);
        }
    }

    /**
     * Retrieve the physical entity manager factory associated with the
     * unitName of an application-level persistence unit.   Returns null if
     * no matching entry is found.
     */
    public EntityManagerFactory getEntityManagerFactory
            (String unitName, BundleDescriptor declaringModule) {

        String lookupString = unitName;

        int separatorIndex =
                unitName.lastIndexOf(PERSISTENCE_UNIT_NAME_SEPARATOR);

        if (separatorIndex != -1) {
            String unqualifiedUnitName =
                    unitName.substring(separatorIndex + 1);
            String path = unitName.substring(0, separatorIndex);

            String persistenceRootUri = getTargetUri(declaringModule, path);

            lookupString = persistenceRootUri +
                    PERSISTENCE_UNIT_NAME_SEPARATOR + unqualifiedUnitName;
        }

        return entityManagerFactories.get(lookupString);
    }

    /**
     * Returns the set of physical entity manager factories associated with
     * persistence units in this application.
     */
    public Set<EntityManagerFactory> getEntityManagerFactories() {

        return new HashSet<EntityManagerFactory>
                (entityManagerFactories.values());

    }

    /**
     * Return the set of roles used in this application. Currently, for release 1.0, it is an
     * * aggregation of all the roles in the sub-modules of the application.
     *
     * @return the Set of roles in the application.
     */
    public Set<Role> getRoles() {
        Set<Role> roles = new HashSet<Role>();
        for (WebBundleDescriptor wbd : getWebBundleDescriptors()) {
            if (wbd != null) {
                roles.addAll(wbd.getRoles());
            }
        }
        for (EjbBundleDescriptor ejbd : getEjbBundleDescriptors()) {
            if (ejbd != null) {
                roles.addAll(ejbd.getRoles());
            }
        }
        return roles;
    }

    /**
     * Return the set of com.sun.enterprise.deployment.Role objects
     * I have (the ones defined in application xml).
     */
    public Set<Role> getAppRoles() {
        if (this.appRoles == null) {
            this.appRoles = new HashSet<Role>();
        }
        return this.appRoles;
    }

    public void addAppRole(SecurityRoleDescriptor descriptor) {
        Role role = new Role(descriptor.getName());
        role.setDescription(descriptor.getDescription());
        getAppRoles().add(role);
    }


    /**
     * Adds a new abstract role
     */
    public void addRole(Role role) {
        for (WebBundleDescriptor wbd : getWebBundleDescriptors()) {
            wbd.addRole(role);
        }
        for (EjbBundleDescriptor ejbd : getEjbBundleDescriptors()) {
            ejbd.addRole(role);
        }
    }

    /**
     * Removes the given role.
     */
    public void removeRole(Role role) {
        getAppRoles().remove(role);
        for (WebBundleDescriptor wbd : getWebBundleDescriptors()) {
            wbd.removeRole(role);
        }
        for (EjbBundleDescriptor ejbd : getEjbBundleDescriptors()) {
            ejbd.removeRole(role);
        }
    }

    /**
     * Return the Set of all reource references that my components have.
     */

    public Set<ResourceReferenceDescriptor> getResourceReferenceDescriptors() {
        Set<ResourceReferenceDescriptor> resourceReferences = new HashSet<ResourceReferenceDescriptor>();
        for (EjbBundleDescriptor ejbd : getEjbBundleDescriptors()) {
            resourceReferences.addAll(ejbd.getResourceReferenceDescriptors());
        }
        return resourceReferences;
    }

    /**
     * Reset the display name of this application.
     *
     * @param name the display name of the application.
     */
    public void setName(String name) {
        name = name.replace('/', '-');
        name = name.replace('\\', '-'); // for deploying from NT to solaris & vice versa. This will
        // need to be cleaned when we clean up the backend for registering apps
        super.setName(name);
        if (this.getRoleMapper() != null) {
            this.getRoleMapper().setName(name);
        }
    }

    public void setLibraryDirectory(String value) {
        libraryDirectory = value;
    }

    /**
     * Returns an "intelligent" value for the library directory setting, meaning
     * the current value if it has been set to a non-null, non-empty value;
     * the default value if the value has never been set, and null if the value
     * has been set to empty.
     *
     * @return String value of the library directory setting
     */
    public String getLibraryDirectory() {
        if (libraryDirectory != null) {
            return (libraryDirectory.length() == 0) ? null : libraryDirectory;
        } else {
            return LIBRARY_DIRECTORY_DEFAULT_VALUE;
        }
    }

    public String getLibraryDirectoryRawValue() {
        return libraryDirectory;
    }

    /**
     * The number of Web Components in this application.
     * Current implementation only return the number of servlets
     * inside the application, and not the JSPs since we cannot
     * get that information from deployment descriptors.
     *
     * @return the number of Web Components
     */
    public int getWebComponentCount() {
        int count = 0;
        for (WebBundleDescriptor wbd : getWebBundleDescriptors()) {
            count = count + wbd.getWebComponentDescriptors().size();
        }
        return count;
    }

    public void removeModule(ModuleDescriptor<BundleDescriptor> descriptor) {
        if (modules.contains(descriptor)) {
            if (descriptor.getDescriptor() != null) {
                descriptor.getDescriptor().setApplication(null);
            }
            modules.remove(descriptor);
        }
    }

    public void addModule(ModuleDescriptor<BundleDescriptor> descriptor) {
        modules.add(descriptor);
        if (descriptor.getDescriptor() != null) {
            descriptor.getDescriptor().setApplication(this);
        }
    }

    /**
     * Obtain a full set of module descriptors
     *
     * @return the set of bundle descriptors
     */
    public Set<ModuleDescriptor<BundleDescriptor>> getModules() {
        return modules;
    }

    /**
     * The number of EJB JARs in this application.
     *
     * @return the number of EJB JARS
     */
    public int getEjbComponentCount() {
        int count = 0;
        for (EjbBundleDescriptor ejbd : this.getEjbBundleDescriptors()) {
            count = count + ejbd.getEjbs().size();
        }
        return count;
    }


    public int getRarComponentCount() {
        return getBundleDescriptors(ConnectorDescriptor.class).size();
    }


    /**
     * The Vector of EJB references in all subcomponents of this application.
     *
     * @return The Vector of EJB references
     */


    public Vector<Object> getEjbReferenceDescriptors() {
        Vector<Object> ejbReferenceDescriptors = new Vector<Object>();
        for (Object next : this.getNamedDescriptors()) {
            if (next instanceof EjbReferenceDescriptor) {
                ejbReferenceDescriptors.addElement(next);
            }
        }
        return ejbReferenceDescriptors;
    }


    /**
     * Obtain the EJB-JAR in this application of the given name.
     * If the JAR is not
     * present, throw an IllegalArgumentException.
     *
     * @return the EjbBundleDescriptor object with the given name
     */
    public EjbBundleDescriptor getEjbBundleByName(String name) {
        for (EjbBundleDescriptor ejbd : getEjbBundleDescriptors()) {
            if (ejbd.getDisplayName().equals(name))
                return ejbd;
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionapphasnoejbjarnamed",
                "This application has no ejb jars of name {0}",
                name));
    }

    /**
     * Get the uri of a target based on a source module and a
     * a relative uri from the perspective of that source module.
     *
     * @param origin            bundle descriptor within this application
     * @param relativeTargetUri relative uri from the given bundle descriptor
     * @return target uri
     */
    public String getTargetUri(BundleDescriptor origin,
                               String relativeTargetUri) {
        String targetUri = null;

        try {
            String archiveUri = origin.getModuleDescriptor().getArchiveUri();
            URI originUri = new URI(archiveUri);
            URI resolvedUri = originUri.resolve(relativeTargetUri);
            targetUri = resolvedUri.getPath();
        } catch (URISyntaxException use) {
            _logger.log(Level.FINE, "origin " + origin + " has invalid syntax",
                    use);
        }

        return targetUri;
    }

    /**
     * Get a target bundle descriptor based on an input bundle descriptor and
     * a relative uri from the perspective of the input bundle descriptor.
     *
     * @param origin            bundle descriptor within this application
     * @param relativeTargetUri relative uri from the given bundle descriptor
     *                          to another bundle within the application.
     * @return target BundleDescriptor or null if not found.
     */
    public BundleDescriptor getRelativeBundle(BundleDescriptor origin,
                                              String relativeTargetUri) {
        String targetBundleUri = getTargetUri(origin, relativeTargetUri);

        BundleDescriptor targetBundle = null;

        if (targetBundleUri != null) {
            Descriptor module = getModuleByUri(targetBundleUri);
            targetBundle = (module instanceof BundleDescriptor) ?
                    (BundleDescriptor) module : null;
        }

        return targetBundle;
    }

    /**
     * Return the relative uri between two modules, from the perspective
     * of the first bundle.
     *
     * @return relative uri or empty string if the two bundles are the same
     */
    public String getRelativeUri(BundleDescriptor origin,
                                 BundleDescriptor target) {

        String originUri = origin.getModuleDescriptor().getArchiveUri();
        String targetUri = target.getModuleDescriptor().getArchiveUri();

        StringTokenizer tokenizer = new StringTokenizer(originUri, "/");
        int numTokens = tokenizer.countTokens();
        int numSeparators = (numTokens > 0) ? (numTokens - 1) : 0;

        StringBuffer relativeUri = new StringBuffer();

        // The simplest way to compute a relative uri is to add one "../"
        // for each sub-path in the origin URI, then add the target URI.
        // It's possible for the result to not be normalized if the origin
        // and target have at least one common root, but that shouldn't
        // matter as long as when the relative URI is resolved against the
        // origin it produces the target.
        for (int i = 0; i < numSeparators; i++) {
            relativeUri.append("../");
        }

        relativeUri.append(targetUri);

        return relativeUri.toString();
    }

    /**
     * Get EJB-JAR of the given URI (filename within EAR)
     */
    public EjbBundleDescriptor getEjbBundleByUri(String name) {
        EjbBundleDescriptor desc = getModuleByTypeAndUri(EjbBundleDescriptor.class, name);
        if (desc != null) {
            return desc;        
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionapphasnoejbjarnamed",
                "This application has no ejb jars of name {0}",
                name));
    }

    /**
     * Lookup module by uri.
     *
     * @param uri the module path in the application archive
     * @return a bundle descriptor in this application identified by uri
     *         or null if not found.
     */
    public ModuleDescriptor<BundleDescriptor> getModuleDescriptorByUri(String uri) {
        for (ModuleDescriptor<BundleDescriptor> aModule : getModules()) {
            if (aModule.getArchiveUri().equals(uri)) {
                return aModule;
            }
        }
        return null;
    }


    /**
     * Lookup module by uri.
     *
     * @param uri the module path in the application archive
     * @return a bundle descriptor in this application identified by uri
     *         or null if not found.
     */
    public Collection<ModuleDescriptor<BundleDescriptor>> getModuleDescriptorsByType(XModuleType type) {
        if (type==null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        LinkedList<ModuleDescriptor<BundleDescriptor>> results = new LinkedList<ModuleDescriptor<BundleDescriptor>>();
        for (ModuleDescriptor<BundleDescriptor> aModule : getModules()) {
            if (type.equals(aModule.getModuleType())) {
                results.add(aModule);
            }
        }
        return results;
    }

    /**
     * Lookup module by uri.
     *
     * @param uri the module path in the application archive
     * @return a bundle descriptor in this application identified by uri
     *         or null if not found.
     */
    public BundleDescriptor getModuleByUri(String uri) {
        ModuleDescriptor<BundleDescriptor> md = getModuleDescriptorByUri(uri);
        if (md != null) {
            return md.getDescriptor();
        }
        return null;
    }

    /**
     * @param type the module type
     * @param uri  the module path in the application archive
     * @return a bundle descriptor in this application identified by
     *         its type and uri
     */
    public <T extends BundleDescriptor> T getModuleByTypeAndUri(Class<T> type, String uri) {
        for (ModuleDescriptor<BundleDescriptor> aModule : getModules()) {
            try {
                T descriptor = type.cast(aModule.getDescriptor());
                if (descriptor.getModuleDescriptor().getArchiveUri().equals(uri)) {
                    return descriptor;
                }
            } catch(ClassCastException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Obtain the EJB in this application of the given display name. If the EJB is not
     * present, throw an IllegalArgumentException.
     *
     * @param ejbName the name of the bean
     * @return the EjbDescriptor object with the given display name
     */
    public EjbDescriptor getEjbByName(String ejbName) {
        for (EjbBundleDescriptor ejbd : getEjbBundleDescriptors()) {
            if (ejbd.hasEjbByName(ejbName)) {
                return ejbd.getEjbByName(ejbName);
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionapphasnobeannamed",
                "This application has no beans of name {0}", ejbName));
    }

    /**
     * Return whether the application contains the given ejb by name..
     *
     * @param ejbName the name of the bean
     * @return true if there is a bean matching the given name
     */
    public boolean hasEjbByName(String ejbName) {
        for (EjbBundleDescriptor ebd : getBundleDescriptors(EjbBundleDescriptor.class)) {
            if (ebd.hasEjbByName(ejbName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtain the application client in this application of the given display name. If the application client is not
     * present, throw an IllegalArgumentException.
     *
     * @return the ApplicationClientDescriptor object with the given display name
     */
    public ApplicationClientDescriptor getApplicationClientByName(String name) {
        for (ApplicationClientDescriptor acd : getBundleDescriptors(ApplicationClientDescriptor.class)) {
            if (acd.getDisplayName().equals(name)) {
                return acd;
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionapphasnoappclientname",
                "This application has no application clients of name {0}", name));
    }

    /**
     * Obtain an application client descriptor in this application of the given URI. If the appclient is not
     * present, throw an IllegalArgumentException.
     */
    public ApplicationClientDescriptor getApplicationClientByUri(String name) {
        ApplicationClientDescriptor desc = getModuleByTypeAndUri(ApplicationClientDescriptor.class, name);
        if (desc != null) {
            return desc;
        }
        throw new IllegalArgumentException(name);
    }

    /**
     * Obtain the WAR in this application of the given display name. If the WAR is not
     * present, throw an IllegalArgumentException.
     *
     * @return the WebBundleDescriptor object with the given display name
     */
    public WebBundleDescriptor getWebBundleDescriptorByName(String name) {
        for (WebBundleDescriptor wbd : getBundleDescriptors(WebBundleDescriptor.class)) {
            if (wbd.getDisplayName().equals(name)) {
                return wbd;
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionapphasnowebappname",
                "This application has no web app of name {0}", name));
    }

    /**
     * Get WAR of a given URI (filename within EAR)
     */
    public WebBundleDescriptor getWebBundleDescriptorByUri(String name) {
        WebBundleDescriptor desc = getModuleByTypeAndUri(WebBundleDescriptor.class, name);
        if (desc != null){
            return desc;
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionapphasnowebappname",
                "This application has no web app of name {0}", name));
    }


    /**
     * Obtain the RAR in this application of the given URI. If the RAR is not
     * present, throw an IllegalArgumentException.
     */
    public ConnectorDescriptor getRarDescriptorByUri(String name) {
        ConnectorDescriptor desc = getModuleByTypeAndUri(ConnectorDescriptor.class, name);
        if (desc != null) {
            return desc;
        }
        throw new IllegalArgumentException(name);
    }


    /**
     * Obtain the full set of all the subcomponents of this application that use
     * a JNDI name environment..
     *
     * @return the Set of JndiNameEnvironment objects.
     */
    public Set<JndiNameEnvironment> getJndiNameEnvironments() {
        Set<JndiNameEnvironment> jndiNameEnvironments = new HashSet<JndiNameEnvironment>();
        jndiNameEnvironments.addAll(getWebBundleDescriptors());
        jndiNameEnvironments.addAll(getApplicationClientDescriptors());
        jndiNameEnvironments.addAll(getEjbDescriptors());
        return jndiNameEnvironments;
    }

    /**
     * Obtain the set of all service reference descriptors for
     * components in this application.
     */
    public Set<ResourceReferenceDescriptor> getServiceReferenceDescriptors() {
        Set<ResourceReferenceDescriptor> serviceRefs = new HashSet<ResourceReferenceDescriptor>();
        for (JndiNameEnvironment jndiNameEnvironment : getJndiNameEnvironments()) {
            serviceRefs.addAll(jndiNameEnvironment.getServiceReferenceDescriptors());
        }
        return serviceRefs;
    }

    /**
     * Return a set of all com.sun.enterprise.deployment.WebService
     * descriptors in the application.
     */
    public Set<WebService> getWebServiceDescriptors() {
        Set<WebService> webServiceDescriptors = new HashSet<WebService>();
        Set<BundleDescriptor> bundles = new HashSet<BundleDescriptor>();
        bundles.addAll(getEjbBundleDescriptors());
        bundles.addAll(getWebBundleDescriptors());
        for (BundleDescriptor next : bundles) {
            WebServicesDescriptor webServicesDesc =
                    next.getWebServices();
            webServiceDescriptors.addAll(webServicesDesc.getWebServices());
        }
        return webServiceDescriptors;
    }

    /**
     * Obtain the full set of all the WARs in this application.
     *
     * @return the Set of WebBundleDescriptor objects.
     */
    public Set<WebBundleDescriptor> getWebBundleDescriptors() {
        return getBundleDescriptors(WebBundleDescriptor.class);
    }

    /**
     * if this application object is virtual, return the standalone
     * bundle descriptor it is wrapping otherwise return null
     *
     * @return the wrapped standalone bundle descriptor
     */
    public BundleDescriptor getStandaloneBundleDescriptor() {
        if (isVirtual()) {
            if (getModules().size()>1) {
                // this is an error, the application is virtual,
                // which mean a wrapper for a standalone module and
                // it seems I have more than one module in my list...
                throw new IllegalStateException("Virtual application contains more than one module");
            }
            return getModules().iterator().next().getDescriptor();
        } else {
            return null;
        }
    }


    /**
     * Obtain a full set of bundle descriptors for a particular type
     *
     * @param type the bundle descriptor type requested
     * @return the set of bundle descriptors
     */
    public <T extends BundleDescriptor> Set<T> getBundleDescriptors(Class<T> type) {
        if (type == null) {
            return null;
        }
        Set<T> bundleSet = new OrderedSet<T>();
        for (ModuleDescriptor aModule : getModules()) {
            try {
                T descriptor = type.cast(aModule.getDescriptor());
                bundleSet.add(descriptor);
            } catch(ClassCastException e) {
                // ignore
            }
            // any children, this need to happen outside of the casting as the parent
            // type has nothing to do with the children extensions.
            bundleSet.addAll(aModule.getDescriptor().getExtensionsDescriptors(type));
        }
        return bundleSet;
    }

    /**
     * Obtain a set of all bundle descriptors, regardless of type
     *
     * @return the set of bundle descriptors
     */
    public Set<BundleDescriptor> getBundleDescriptors() {
        Set<BundleDescriptor> bundleSet = new OrderedSet<BundleDescriptor>();
        for (ModuleDescriptor<BundleDescriptor> aModule :  getModules()) {
            if (aModule.getDescriptor() != null) {
                bundleSet.add(aModule.getDescriptor());
            } else {
                DOLUtils.getDefaultLogger().fine("Null descriptor for module " + aModule.getArchiveUri());
            }
        }
        return bundleSet;
    }

    /**
     * Add a web bundle descriptor to this application.
     *
     * @param bundleDescriptor the web bundle descriptor to add
     */
    public void addBundleDescriptor(BundleDescriptor bundleDescriptor) {
        ModuleDescriptor newModule = bundleDescriptor.getModuleDescriptor();
        addModule(newModule);
    }

    /**
     * Remove a web bundle descriptor from this application.
     *
     * @param bundleDescriptor the web bundle descriptor to remove
     */
    public void removeBundleDescriptor(BundleDescriptor bundleDescriptor) {
        bundleDescriptor.setApplication(null);
        getWebBundleDescriptors().remove(bundleDescriptor);
    }


    /**
     * Obtain the full set of all the Ejb JAR deployment information in this application.
     *
     * @return the Set of EjbBundleDescriptor objects.
     */
    public Set<EjbBundleDescriptor> getEjbBundleDescriptors() {
        return getBundleDescriptors(EjbBundleDescriptor.class);
    }

    /**
     * Obtain the full set of all the Ejb JAR deployment information in this application.
     *
     * @return the Set of EjbBundleDescriptor objects.
     */
    public Set<ConnectorDescriptor> getRarDescriptors() {
        return getBundleDescriptors(ConnectorDescriptor.class);
    }

    /**
     * Return the Set of app client deploymenbt objects.
     */
    public Set<ApplicationClientDescriptor> getApplicationClientDescriptors() {
        return getBundleDescriptors(ApplicationClientDescriptor.class);
    }

    /**
     * Return the EjbCMPEntityDescriptor for a bean
     * for the given classname.
     * It is assumed that there is a 1-to-1 mapping
     * from class to descriptor.
     * This is called at runtime from the Persistence Manager.
     */
    public EjbCMPEntityDescriptor getCMPDescriptorFor(String className) {
        synchronized(cmpDescriptorsLock) {
        if (cmpDescriptors == null) {
            cmpDescriptors = new HashMap();
            for (EjbBundleDescriptor bundle : getBundleDescriptors(EjbBundleDescriptor.class)) {
                for (EjbDescriptor ejb : bundle.getEjbs()) {
                    if (ejb instanceof EjbCMPEntityDescriptor)
                        cmpDescriptors.put(ejb.getEjbImplClassName(), ejb);
                }
            }
        }
        return (EjbCMPEntityDescriptor) cmpDescriptors.get(className);
        }
    }


    /**
     * Return all the Named reference pairs I have. This is a Vector of NamedReferenceDescriptors - essentially a mapping of
     * components to the Named Objects they reference. E.g.:
     * * ejb -> ejb
     * * ejb - resource reference
     * * ejb - ejb ref
     * * ejb1 - ejb ref
     */
    public Vector<NamedReferencePair> getNamedReferencePairs() {
        Vector<NamedReferencePair> pairs = new Vector<NamedReferencePair>();
        for (EjbBundleDescriptor ejbBundleDescriptor : this.getEjbBundleDescriptors()) {
            pairs.addAll(ejbBundleDescriptor.getNamedReferencePairs());
        }
        for (WebBundleDescriptor webBundleDescriptor : this.getWebBundleDescriptors()) {
            pairs.addAll(webBundleDescriptor.getNamedReferencePairs());
        }
        for (ApplicationClientDescriptor applicationClientDescriptor : this.getApplicationClientDescriptors()) {
            pairs.addAll(applicationClientDescriptor.getNamedReferencePairs());
        }
        return pairs;
    }

    /**
     * return the set of descriptors with jndi names.
     */
    public Collection<Object> getNamedDescriptors() {
        Collection<Object> namedDescriptors = new Vector<Object>();
        for (Iterator itr = this.getEjbBundleDescriptors().iterator(); itr.hasNext();) {
            EjbBundleDescriptor ejbBundleDescriptor = (EjbBundleDescriptor) itr.next();
            namedDescriptors.addAll(ejbBundleDescriptor.getNamedDescriptors());
        }
        for (Iterator<WebBundleDescriptor> itr = this.getWebBundleDescriptors().iterator(); itr.hasNext();) {
            WebBundleDescriptor webBundleDescriptor = itr.next();
            namedDescriptors.addAll(webBundleDescriptor.getNamedDescriptors());
        }
        for (Iterator itr = this.getApplicationClientDescriptors().iterator(); itr.hasNext();) {
            ApplicationClientDescriptor applicationClientDescriptor = (ApplicationClientDescriptor) itr.next();
            namedDescriptors.addAll(applicationClientDescriptor.getNamedDescriptors());
        }
        return namedDescriptors;

    }

    /**
     * Return the Vector of ejb deployment objects.
     */
    public Vector getEjbDescriptors() {
        Vector ejbDescriptors = new Vector();
        for (EjbBundleDescriptor ejbBundleDescriptor : getBundleDescriptors(EjbBundleDescriptor.class)) {
            ejbDescriptors.addAll(ejbBundleDescriptor.getEjbs());
        }

        return ejbDescriptors;
    }

    /**
     * @return true if this bundle descriptor contains at least one CMP
     *         EntityBean
     */
    public boolean containsCMPEntity() {
        for (EjbBundleDescriptor ebd : getBundleDescriptors(EjbBundleDescriptor.class)) {
            if (ebd.containsCMPEntity())
                return true;
        }
        return false;
    }

    // START OF IASRI 4718761 - pass-by-ref need to compare DD from previous
    // deployment when reusing the old bits

    /**
     * Returns all the ejb descriptor in this application in ordered form.
     * The comparison is done based on the descriptor's name.
     *
     * @return all ejb descriptors in ordered form
     */
    public EjbDescriptor[] getSortedEjbDescriptors() {
        Vector ejbDesc = getEjbDescriptors();
        EjbDescriptor[] descs = (EjbDescriptor[]) ejbDesc.toArray(
                new EjbDescriptor[ejbDesc.size()]);

        // The sorting algorithm used by this api is a modified mergesort.
        // This algorithm offers guaranteed n*log(n) performance, and 
        // can approach linear performance on nearly sorted lists. 

        // since ejb name is only unique within a module, add the module uri
        // as the additional piece of information for comparison
        Arrays.sort(descs,
                new Comparator() {
                    public int compare(Object o1, Object o2) {
                        EjbDescriptor desc1 = (EjbDescriptor) o1;
                        EjbDescriptor desc2 = (EjbDescriptor) o2;
                        String moduleUri1 = desc1.getEjbBundleDescriptor().getModuleDescriptor().getArchiveUri();
                        String moduleUri2 = desc2.getEjbBundleDescriptor().getModuleDescriptor().getArchiveUri();
                        return (moduleUri1 + desc1.getName()).compareTo(
                                moduleUri2 + desc2.getName());
                    }
                }
        );

        return descs;
    }

    // END OF IASRI 4718761

    // START OF IASRI 4645310

    /**
     * Sets the virtual status of this application.
     * If this application object represents a stand alone module,
     * virtaul status should be true; else false.
     *
     * @param virtual new value of this application's virtaul status
     */
    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    /**
     * Returns the virtual status of this application.
     *
     * @return true if this application obj represents a stand alone module
     */
    public boolean isVirtual() {
        return this.virtual;
    }

    /**
     * Returns if the application is packaged in a single module.
     *
     * @return true if this application obj is packaged in a single module
     */
    public boolean isPackagedAsSingleModule() {
        return isPackagedAsSingleModule;
    }

    public void setPackagedAsSingleModule(boolean status) {
        this.isPackagedAsSingleModule = status;
    }
    
    /**
     * Sets the unique id for this application.  It traverses through all
     * the  ejbs in the application and sets the unique id for each of them.
     * The traversal is done in ascending element order.
     *
     * @param id unique id for this application
     */
    public void setUniqueId(long id) {
        _logger.log(Level.FINE, "[Application]uid: " + id);
        this.uniqueId = id;

        EjbDescriptor[] descs = getSortedEjbDescriptors();

        for (int i = 0; i < descs.length; i++) {
            // 2^16 beans max per stand alone module
            descs[i].setUniqueId((id | i));
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "[Application]desc name: " + descs[i].getName());
                _logger.log(Level.FINE, "[Application]desc id: " + descs[i].getUniqueId());
            }
        }
    }

    /**
     * Returns the unique id used for this application.
     *
     * @return unique id used for this application
     */
    public long getUniqueId() {
        return this.uniqueId;
    }
    // END OF IASRI 4645310

    // START IASRI 4662001, 4720955

    /**
     * Sets the pass-by-reference property for this application.
     * EJB spec requires pass-by-value (false) which is the default.
     * This can be set to true for non-compliant operation and possibly
     * higher performance.  For a stand-alone  server, this can be used.
     * By setting pass-by-reference in sun-application.xml, it can apply to
     * all the enclosed ejb modules.
     *
     * @param passByReference boolean true or false - pass-by-reference property of application.
     *                        true - application is pass-by-reference
     *                        false - application is pass-by-value
     */
    public void setPassByReference(boolean passByReference) {
        this.passByReference = Boolean.valueOf(passByReference);
    }

    /**
     * Gets the value of pass-by-reference property for this application
     * Checks to see if the pass-by-reference property is defined.  If
     * this application's pass-by-reference property is defined, this method
     * returns the value of the application's pass-by-reference property.
     * Otherwise, if the application's pass-by-reference property is undefined,
     * this method returns a default value of false.
     *
     * @return boolean pass-by-reference property for this application
     */
    public boolean getPassByReference() {
        boolean passByReference = false;

        if (this.isPassByReferenceDefined()) {
            passByReference = this.passByReference.booleanValue();
        }
        return passByReference;
    }
    // END OF IASRI 4662001, 4720955

    // START OF IASRI 4720955
    /* *
     * Determines if the application's pass-by-reference property has been
     * defined or undefined in sun-application.xml
     *
     * @return true - pass-by-reference is defined in sun-application.xml
     *         false - pass-by-reference is undefined in sun-application.xml
     */

    public boolean isPassByReferenceDefined() {
        boolean passByReferenceDefined = false;
        if (this.passByReference != null) {
            passByReferenceDefined = true;
        }
        return passByReferenceDefined;
    }
    // END OF IASRI 4720955

    /**
     * Add all the deployment information about the given application to me.
     */
    public void addApplication(Application application) {
        for (ModuleDescriptor md : application.getModules()) {
            addModule(md);
        }
    }

    /**
     * Return all my subcomponents that have a file format (EJB, WAR and
     * AppCLient JAR).
     */
    public Set getArchivableDescriptors() {
        Set archivableDescriptors = new OrderedSet();
        archivableDescriptors.addAll(getBundleDescriptors());
        return archivableDescriptors;
    }

    /**
     * Sets the mapping of rolename to users and groups on a particular server.
     */
    public void setRoleMapper(SecurityRoleMapper roleMapper) {
        // should verify against the roles
        this.roleMapper = roleMapper;
    }

    /**
     * Return true if I have information to do with deployment on a
     * particular operational environment.
     */
    public boolean hasRuntimeInformation() {
        return true;
    }

    /**
     * Return my mapping of rolename to users and groups on a particular
     * server.
     */
    public SecurityRoleMapper getRoleMapper() {
        if (this.roleMapper == null) {
            SecurityRoleMapperFactory factory = habitat.getComponent(SecurityRoleMapperFactory.class);
            if (factory == null) {
                _logger.log(Level.FINE, "SecurityRoleMapperFactory NOT set.");
            } else {
                this.roleMapper = factory.getRoleMapper(this.getName());
            }
        }
        return this.roleMapper;
    }

    /**
     * Sets the realm for this application
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * @return the realm for this application
     */
    public String getRealm() {
        return realm;
    }

    /**
     * A flag to indicate that my data has changed since the last save.
     */
    public boolean isDirty() {
        return this.isDirty;
    }

    /**
     * @return the class loader associated with this application
     */
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            throw new RuntimeException("No class loader associated with application " + getName());
        }
        return classLoader;
    }

    /**
     * A formatted String representing my state.
     */
    public void print(StringBuffer toStringBuffer) {
        toStringBuffer.append("Application");
        toStringBuffer.append("\n");
        super.print(toStringBuffer);
        toStringBuffer.append("\n smallIcon ").append(super.getSmallIconUri());
        for (ModuleDescriptor<BundleDescriptor> aModule : getModules()) {
            toStringBuffer.append("\n  Module : ");
            aModule.print(toStringBuffer);
        }
        toStringBuffer.append("\n EjbBundles: \n");
        if (this.getEjbBundleDescriptors() != null)
            printDescriptorSet(this.getEjbBundleDescriptors(), toStringBuffer);
        toStringBuffer.append("\n WebBundleDescriptors ");
        if (this.getWebBundleDescriptors() != null)
            printDescriptorSet(this.getWebBundleDescriptors(), toStringBuffer);
        toStringBuffer.append("\n applicationClientDescriptors ");
        if (this.getApplicationClientDescriptors() != null)
            printDescriptorSet(this.getApplicationClientDescriptors(), toStringBuffer);
        toStringBuffer.append("\n roles ").append(getRoles());
        toStringBuffer.append("\n RoleMapper ").append(this.getRoleMapper());
        toStringBuffer.append("\n Realm ").append(realm);
    }

    private void printDescriptorSet(Set descSet, StringBuffer sbuf) {
        for (Iterator itr = descSet.iterator(); itr.hasNext();) {
            Object obj = itr.next();
            if (obj instanceof Descriptor)
                ((Descriptor) obj).print(sbuf);
            else
                sbuf.append(obj);
        }
    }

    /**
     * visit the descriptor and all sub descriptors with a DOL visitor implementation
     *
     * @param aVisitor visitor to traverse the descriptors
     */
    public void visit(DescriptorVisitor aVisitor) {
        if (aVisitor instanceof ApplicationVisitor) {
            visit((ApplicationVisitor) aVisitor);
        } else {
            super.visit(aVisitor);
        }
    }

    /**
     * visit the descriptor and all sub descriptors with a DOL visitor implementation
     *
     * @param aVisitor visitor to traverse the descriptors
     */
    public void visit(ApplicationVisitor aVisitor) {
        aVisitor.accept(this);
        EjbBundleVisitor ejbBundleVisitor = aVisitor.getEjbBundleVisitor();
        if (ejbBundleVisitor != null) {
            for (EjbBundleDescriptor ebd : getBundleDescriptors(EjbBundleDescriptor.class)) {
                ebd.visit(ejbBundleVisitor);
            }
        }
        WebBundleVisitor webVisitor = aVisitor.getWebBundleVisitor();
        if (webVisitor != null) {
            for (WebBundleDescriptor wbd : getBundleDescriptors(WebBundleDescriptor.class)) {
                // This might be null in the case of an appclient 
                // processing a client stubs .jar whose original .ear contained
                // a .war.  This will be fixed correctly in the deployment
                // stage but until then adding a non-null check will prevent
                // the validation step from bombing.
                if (wbd != null) {
                    wbd.visit(webVisitor);
                }
            }
        }
        ConnectorVisitor connectorVisitor = aVisitor.getConnectorVisitor();
        if (connectorVisitor != null) {
            for (ConnectorDescriptor cd :  getBundleDescriptors(ConnectorDescriptor.class)) {
                cd.visit(connectorVisitor);
            }
        }

        AppClientVisitor appclientVisitor = aVisitor.getAppClientVisitor();
        if (appclientVisitor != null) {
            for (ApplicationClientDescriptor acd : getBundleDescriptors(ApplicationClientDescriptor.class)) {
                acd.visit(appclientVisitor);
            }
        }
    }


    /**
     * @return the module ID for this module descriptor
     */
    public String getModuleID() {
        return moduleID;
    }

    /**
     * @return true if this module is an application object
     */
    public boolean isApplication() {
        return true;
    }

    /**
     * @return the module type for this bundle descriptor
     */
    public XModuleType getModuleType() {
        return XModuleType.EAR;
    }

    public void addSecurityRoleMapping(SecurityRoleMapping roleMapping) {
        roleMaps.add(roleMapping);
    }

    public List<SecurityRoleMapping> getSecurityRoleMappings() {
        return roleMaps;
    }

    /**
     * This method records how this Application object is constructed.  We
     * keep this information to avoid additional disk access in
     * DescriptorArchivist.write() when deciding if the application.xml
     * should be copied or written to the generated/xml directory.
     */
    public void setLoadedFromApplicationXml(boolean bool) {
        loadedFromApplicationXml = bool;
    }

    /**
     * @return true if this Application is from reading application.xml from
     *         disk;  false if this Application object is derived from the content
     *         of the ear file.
     */
    public boolean isLoadedFromApplicationXml() {
        return loadedFromApplicationXml;
    }

    // getter and setter of in-memory object of sun-configuration.xml
    public void setResourceList(List<Resource> rList) {
        resourceList = rList;
    }

    public List<Resource> getResourceList() {
        return resourceList;
    }
}
