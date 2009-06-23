/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 */

package org.glassfish.persistence.jpa;

import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.deployment.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.PersistenceUnitsDescriptor;
import org.glassfish.persistence.common.Java2DBProcessorHelper;
import com.sun.logging.LogDomains;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceProvider;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads emf correspoding to a PersistenceUnit. Executes java2db if required.
 * @author Mitesh Meswani
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class PersistenceUnitLoader {

    /**
     * Conduit to talk with container
     */
    private ProviderContainerContractInfo providerContainerContractInfo;

    private EntityManagerFactory emf;

    private boolean java2db;

    /**
     * The processor instance for the Java2DB work.
     */
    private JPAJava2DBProcessor processor;

    // TODO change logger name from DPL_LOGGER to persistence logger
    private static Logger logger = LogDomains.getLogger(PersistenceUnitLoader.class, LogDomains.DPL_LOGGER);

    /**
     * Integration properties that include Java2DB support
     */
    private static Map<String, String> integrationPropertiesWithJava2DB;

    /**
     * Integration properties for loading PUs for execution
     */
    private static Map<String, String> integrationPropertiesWithoutJava2DB;

    PersistenceUnitLoader(PersistenceUnitDescriptor puToInstatntiate, ProviderContainerContractInfo providerContainerContractInfo) {
       this.providerContainerContractInfo = providerContainerContractInfo;

       // A hack to work around EclipseLink issue https://bugs.eclipse.org/bugs/show_bug.cgi?id=248328 for prelude
       // This should be removed once version of EclipseLink which fixes the issue is integrated.
       // set the system property required by EclipseLink before we load it.
       setSystemPropertyToEnableDoPrivilegedInEclipseLink();

       emf = loadPU(puToInstatntiate);
   }

    /**
     * @return The emf loaded.
     */
    EntityManagerFactory getEMF() {
        return emf;
    }

    private void setSystemPropertyToEnableDoPrivilegedInEclipseLink() {
        final String PROPERTY_NAME = "eclipselink.security.usedoprivileged";
        // Need not invoke in doPrivileged block as the whole call stack consist of trusted code when this code
        // is invoked
        if(System.getProperty(PROPERTY_NAME) == null) {
            // property not set. Set it to true
            System.setProperty(PROPERTY_NAME, String.valueOf(Boolean.TRUE) );
        }
    }

    /**
     * Loads an individual PersistenceUnitDescriptor and registers the
     * EntityManagerFactory in appropriate DOL structure.
     *
     * @param pud PersistenceUnitDescriptor to be loaded.
     */
    private EntityManagerFactory loadPU(PersistenceUnitDescriptor pud) {


        PersistenceUnitInfo pInfo = new PersistenceUnitInfoImpl(pud, providerContainerContractInfo);

        String applicationLocation = providerContainerContractInfo.getApplicationLocation();
        final boolean fineMsgLoggable = logger.isLoggable(Level.FINE);
        if(fineMsgLoggable) {
            logger.fine("Loading persistence unit for application: \"" + applicationLocation + "\"pu Root is: " +
                    pud.getPuRoot());
            logger.fine("PersistenceInfo for this pud is :\n" + pInfo); // NOI18N
        }

        PersistenceProvider provider;
        try {
            // See we use application CL as opposed to system CL to loadPU
            // provider. This allows user to get hold of provider specific
            // implementation classes in their code. But this also means
            // provider must not use appserver implementation classes directly
            // because once we implement isolation in our class loader hierarchy
            // the only classes available to application class loader would be
            // our appserver interface classes. By Sahoo
            provider =
                    PersistenceProvider.class.cast(
                    ClassLoader.class.cast(providerContainerContractInfo.getClassLoader())
                    .loadClass(pInfo.getPersistenceProviderClassName())
                    .newInstance());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // XXX  - use DeploymentContext directly instead of creating helper instance first

        boolean isDeploy = providerContainerContractInfo.isDeploy();
        if (isDeploy) {
            processor = new JPAJava2DBProcessor(new Java2DBProcessorHelper(providerContainerContractInfo.getDeploymentContext()));
            java2db = processor.isJava2DbPU(pud);
        }

        Map<String, String> overrides = (java2db)? integrationPropertiesWithJava2DB :
                integrationPropertiesWithoutJava2DB;

        EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
                pInfo, overrides);

        if (fineMsgLoggable) {
            logger.logp(Level.FINE, "PersistenceUnitLoader", "loadPU", // NOI18N
                        "emf = {0}", emf); // NOI18N
        }

        PersistenceUnitsDescriptor parent = pud.getParent();
        RootDeploymentDescriptor containingBundle = parent.getParent();
        providerContainerContractInfo.registerEMF(pInfo.getPersistenceUnitName(), pud.getPuRoot(), containingBundle, emf);

        if(fineMsgLoggable) {
            logger.fine("Finished loading persistence unit for application: " +  // NOI18N
                    applicationLocation);
        }
        return emf;
    }

    /**
     * Called during load when the correct classloader and transformer had been
     * already set.
     * For emf that require Java2DB, call createEntityManager() to populate
     * the DDL files, then iterate over those files and execute each line in them.
     */
    void doJava2DB() {
        if (java2db) {
            final boolean fineMsgLoggable = logger.isLoggable(Level.FINE);
            EntityManager em = null;
            try {
                if(fineMsgLoggable) {
                    logger.fine("<--- To Create EM"); // NOI18N
                }

                em = emf.createEntityManager();
            } catch(Throwable e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                // Log and ignore
            } finally {
                if(em != null) {
                    em.close();
                }
            }
            if(fineMsgLoggable) {
                logger.fine("---> Done Create EM"); // NOI18N
            }
            if(fineMsgLoggable) {
                logger.fine("<--- To Create Tables"); // NOI18N
            }

            processor.createTablesInDB();

            if(fineMsgLoggable) {
                logger.fine("---> Done Create Tables"); // NOI18N
            }
        }
    }

    static {
        /*
         * We set all the provider specific integration level properties here.
         * It knows about all the integration level properties that
         * are needed to integrate a provider with our container. When we add
         * support for other containers, we should modify this code so that user
         * does not have to specify such properties in their persistence.xml file.
         * These properties can be overriden by persistence.xml as per
         * the spec. Before applying default values for properties, this method
         * first checks if the properties have been set in the system
         * (typically done using -D option in domain.xml).
         *
         */
        // ------------------- The Base -------------------------

        Map<String, String> props = new HashMap<String, String>();

        final String ECLIPSELINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY =
                "eclipselink.target-server"; // NOI18N
        props.put(ECLIPSELINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY,
                System.getProperty(ECLIPSELINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY,
                        "SunAS9")); // NOI18N


        // TopLink specific properties:
        // See https://glassfish.dev.java.net/issues/show_bug.cgi?id=249
        final String TOPLINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY =
                "toplink.target-server"; // NOI18N
        props.put(TOPLINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY,
                System.getProperty(TOPLINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY,
                        "SunAS9")); // NOI18N

        // Hibernate specific properties:
        final String HIBERNATE_TRANSACTION_MANAGER_LOOKUP_CLASS_PROPERTY =
                "hibernate.transaction.manager_lookup_class"; // NOI18N
        props.put(HIBERNATE_TRANSACTION_MANAGER_LOOKUP_CLASS_PROPERTY,
                System.getProperty(HIBERNATE_TRANSACTION_MANAGER_LOOKUP_CLASS_PROPERTY,
                        "org.hibernate.transaction.SunONETransactionManagerLookup")); // NOI18N

        // use an unmodifiable map as we pass this to provider and we don't
        // provider to change this.
        Map<String, String> baseIntegrationProperties = Collections.unmodifiableMap(props);

        // ------------------- Java2DB -------------------------

        // Create map for Java2DB that includes base properties 
        // and Java2DB specific.
        Map<String, String> java2dbProps = new HashMap<String, String>(baseIntegrationProperties);

        final String DROP_AND_CREATE         = "drop-and-create-tables"; //NOI18N
        final String TOPLINK_DDL_GENERATION     = "toplink.ddl-generation"; // NOI18N
        java2dbProps.put(TOPLINK_DDL_GENERATION, DROP_AND_CREATE);

        final String ECLIPSELINK_DDL_GENERATION = "eclipselink.ddl-generation"; // NOI18N
        java2dbProps.put(ECLIPSELINK_DDL_GENERATION, DROP_AND_CREATE);

        final String DDL_SQL_SCRIPT_GENERATION = "sql-script"; // NOI18N
        final String TOPLINK_DDL_GENERATION_MODE     = "toplink.ddl-generation.output-mode"; // NOI18N
        java2dbProps.put(TOPLINK_DDL_GENERATION_MODE, DDL_SQL_SCRIPT_GENERATION);

        final String ECLIPSELINK_DDL_GENERATION_MODE = "eclipselink.ddl-generation.output-mode"; // NOI18N
        java2dbProps.put(ECLIPSELINK_DDL_GENERATION_MODE, DDL_SQL_SCRIPT_GENERATION);

        // use an unmodifiable map as we pass this to provider and we don't
        // provider to change this.
        integrationPropertiesWithJava2DB = Collections.unmodifiableMap(java2dbProps);
        
        // ------------------- Non-Java2DB -------------------------

        // Create map for non-Java2DB case, which is the load or Java2DB is not specified.
        Map<String, String> nonjava2dbProps = new HashMap<String, String>(baseIntegrationProperties);

        final String ECLIPSELINK_DDL_GENERATION_MODE_PROPERTY =
                "eclipselink.ddl-generation.output-mode"; // NOI18N
        nonjava2dbProps.put(ECLIPSELINK_DDL_GENERATION_MODE_PROPERTY,
                System.getProperty(ECLIPSELINK_DDL_GENERATION_MODE_PROPERTY,
                        "none")); // NOI18N

        // These constants are defined in the entity-persistence module. Redefining them for now.
        final String TOPLINK_DDL_GENERATION_MODE_PROPERTY =
                "toplink.ddl-generation.output-mode"; // NOI18N
        nonjava2dbProps.put(TOPLINK_DDL_GENERATION_MODE_PROPERTY,
                System.getProperty(TOPLINK_DDL_GENERATION_MODE_PROPERTY,
                        "none")); // NOI18N

        // use an unmodifiable map as we pass this to provider and we don't
        // provider to change this.
        integrationPropertiesWithoutJava2DB = Collections.unmodifiableMap(nonjava2dbProps);
    }


}
