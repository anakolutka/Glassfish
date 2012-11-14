/*
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.s1asdev.crd;

import com.sun.ejte.ccl.reporter.SimpleReporterAdapter;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ConnectorResourceDefinitionDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.archivist.ApplicationArchivist;
import com.sun.enterprise.loader.ASURLClassLoader;
import junit.framework.TestCase;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.ejb.deployment.archivist.EjbArchivist;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.web.deployment.archivist.WebArchivist;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
public class ArchiveTest extends TestCase {
    String archiveDir = null;
    private static SimpleReporterAdapter stat =  new SimpleReporterAdapter("appserv-tests");

    protected void setUp() throws Exception {
        super.setUp();
        TestUtil.setupHK2();
        archiveDir = System.getProperty("ArchiveDir");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testApplicationArchive() throws Exception{
        String tcName = "connector-resource-definition-application-archive-test";

        try{
            doTestApplicationArchive();
            stat.addStatus(tcName, stat.PASS);
        }catch(Exception e){
            stat.addStatus(tcName, stat.FAIL);
            throw e;
        }
    }
    private void doTestApplicationArchive() throws Exception{
        String appArchiveName = "connector-resource-definitionApp-UT";
        File archive = new File(archiveDir, appArchiveName);
        assertTrue("Do not fing the archive "+archive.getAbsolutePath(), archive.exists());

        ApplicationArchivist reader = (ApplicationArchivist) TestUtil.getByType(ApplicationArchivist.class);
        reader.setAnnotationProcessingRequested(true);
        ASURLClassLoader classLoader = new ASURLClassLoader(this.getClass().getClassLoader());
        classLoader.addURL(archive.toURL());
        reader.setClassLoader(classLoader);
        
        Application applicationDesc = reader.open(archive);
//        System.out.println("--------Connector resoruce in application.xml----------");
//        for( ConnectorResourceDefinitionDescriptor crdd: applicationDesc.getConnectorResourceDefinitionDescriptors()){
//            System.out.println(crdd.getDescription());
//            System.out.println(crdd.getName());
//            for(Object key: crdd.getProperties().keySet()){
//                System.out.println("  "+key+"="+crdd.getProperties().get(key));
//            }
//            System.out.println("");
//        }
        
        Map<String,ConnectorResourceDefinitionDescriptor> expectedCRDDs = 
                new HashMap<String,ConnectorResourceDefinitionDescriptor>();
        ConnectorResourceDefinitionDescriptor desc;

        desc = new ConnectorResourceDefinitionDescriptor();
        desc.setDescription("global-scope resource defined in application DD");
        desc.setName("java:global/env/ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "RaApplicationName");
        expectedCRDDs.put(desc.getName(), desc);

        desc = new ConnectorResourceDefinitionDescriptor();
        desc.setDescription("application-scope resource defined in application DD");
        desc.setName("java:app/env/ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "RaApplicationName");
        expectedCRDDs.put(desc.getName(), desc);

        TestUtil.compareCRDD(expectedCRDDs, applicationDesc.getResourceDescriptors(JavaEEResourceType.CRD));

    }

    public void testWebArchive() throws Exception{
        String tcName = "connector-resource-definition-web-archive-test";

        try{
            doTestWebArchive();
            stat.addStatus(tcName, stat.PASS);
        }catch(Exception e){
            stat.addStatus(tcName, stat.FAIL);
            throw e;
        }
    }

    private void doTestWebArchive() throws Exception{
        String appArchiveName = "connector-resource-definition-web";
        File archive = new File(archiveDir, appArchiveName);
        assertTrue("Do not fing the archive "+archive.getAbsolutePath(), archive.exists());

        ASURLClassLoader classLoader = new ASURLClassLoader(this.getClass().getClassLoader());
        classLoader.addURL(archive.toURL());

        WebArchivist reader = (WebArchivist) TestUtil.getByType(WebArchivist.class);
        reader.setAnnotationProcessingRequested(true);
        reader.setClassLoader(classLoader);
        assertTrue("Archivist should handle annotations.", reader.isAnnotationProcessingRequested());
        
        WebBundleDescriptor webDesc = reader.open(archive);

        Map<String,ConnectorResourceDefinitionDescriptor> expectedCRDDs = 
                new HashMap<String,ConnectorResourceDefinitionDescriptor>();
        ConnectorResourceDefinitionDescriptor desc;

        desc = new ConnectorResourceDefinitionDescriptor();
        desc.setDescription("global-scope resource to be modified by DD");
        desc.setName("java:global/env/Servlet_ModByDD_ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "NoTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
        expectedCRDDs.put(desc.getName(), desc);

        desc = new ConnectorResourceDefinitionDescriptor();
        desc.setDescription("global-scope resource defined by @ConnectorResourceDefinition");
        desc.setName("java:global/env/Servlet_ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
        expectedCRDDs.put(desc.getName(), desc);
        
        desc = new ConnectorResourceDefinitionDescriptor();
        desc.setDescription("application-scope resource defined by @ConnectorResourceDefinition");
        desc.setName("java:app/env/Servlet_ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
        expectedCRDDs.put(desc.getName(), desc);
        
        desc = new ConnectorResourceDefinitionDescriptor();
        desc.setDescription("module-scope resource defined by @ConnectorResourceDefinition");
        desc.setName("java:module/env/Servlet_ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
        expectedCRDDs.put(desc.getName(), desc);
        
        desc = new ConnectorResourceDefinitionDescriptor();
        desc.setDescription("component-scope resource defined by @ConnectorResourceDefinition");
        desc.setName("java:comp/env/Servlet_ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
        expectedCRDDs.put(desc.getName(), desc);
        
        desc = new ConnectorResourceDefinitionDescriptor();
        desc.setDescription("global-scope resource defined in Web DD");
        desc.setName("java:global/env/Web_DD_ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
        expectedCRDDs.put(desc.getName(), desc);
        
        desc = new ConnectorResourceDefinitionDescriptor();
        desc.setDescription("application-scope resource defined in Web DD");
        desc.setName("java:app/env/Web_DD_ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
        expectedCRDDs.put(desc.getName(), desc);
        
        desc = new ConnectorResourceDefinitionDescriptor();
        desc.setDescription("module-scope resource defined in Web DD");
        desc.setName("java:module/env/Web_DD_ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
        expectedCRDDs.put(desc.getName(), desc);

        TestUtil.compareCRDD(expectedCRDDs, webDesc.getResourceDescriptors(JavaEEResourceType.CRD));
    }

    public void testEJBArchive() throws Exception{
        String tcName = "connector-resource-definition-EJB-archive-test";

        try{
            doTestEJBArchive();
            stat.addStatus(tcName, stat.PASS);
        }catch(Exception e){
            stat.addStatus(tcName, stat.FAIL);
            throw e;
        }
    }
    private void doTestEJBArchive() throws Exception{
        String appArchiveName = "connector-resource-definition-ejb";
        File archive = new File(archiveDir, appArchiveName);
        assertTrue("Do not fing the archive "+archive.getAbsolutePath(), archive.exists());

        ASURLClassLoader classLoader = new ASURLClassLoader(this.getClass().getClassLoader());
        classLoader.addURL(archive.toURL());
               
        EjbArchivist reader = (EjbArchivist) TestUtil.getByType(EjbArchivist.class);
        reader.setClassLoader(classLoader);
        reader.setAnnotationProcessingRequested(true);
        assertTrue("Archivist should handle annotations.", reader.isAnnotationProcessingRequested());

        EjbBundleDescriptorImpl ejbBundleDesc = reader.open(archive);
        Set<Descriptor> acturalCRDDs = new HashSet<Descriptor>();
        for( EjbDescriptor ejbDesc: ejbBundleDesc.getEjbs()){
            acturalCRDDs.addAll(ejbDesc.getResourceDescriptors(JavaEEResourceType.CRD));
        }
        
        Map<String,ConnectorResourceDefinitionDescriptor> expectedCRDDs = 
                new HashMap<String,ConnectorResourceDefinitionDescriptor>();
        ConnectorResourceDefinitionDescriptor desc;


        desc = new ConnectorResourceDefinitionDescriptor();
        desc.setDescription("global-scope resource to be modified by DD");
        desc.setName("java:global/env/HelloStatefulEJB_ModByDD_ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "NoTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
        expectedCRDDs.put(desc.getName(), desc);

        desc.setDescription("global-scope resource to be modified by DD");
        desc.setName("java:global/env/HelloEJB_ModByDD_ConnectorResource");
        desc.setClassName("javax.resource.cci.ConnectionFactory");
        desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "NoTransaction");
        desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
        expectedCRDDs.put(desc.getName(), desc);

        // connector-resource in DD for stateful EJB
        {
            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("global-scope resource defined in EJB DD");
            desc.setName("java:global/env/HelloStatefulEJB_DD_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);

            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("application-scope resource defined in EJB DD");
            desc.setName("java:app/env/HelloStatefulEJB_DD_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);

            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("module-scope resource defined in EJB DD");
            desc.setName("java:module/env/HelloStatefulEJB_DD_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);

            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("component-scope resource defined in EJB DD");
            desc.setName("java:comp/env/HelloStatefulEJB_DD_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);
        }
        // connector-resource in DD for stateless EJB
        {
            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("global-scope resource defined in EJB DD");
            desc.setName("java:global/env/HelloEJB_DD_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);

            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("application-scope resource defined in EJB DD");
            desc.setName("java:app/env/HelloEJB_DD_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);

            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("module-scope resource defined in EJB DD");
            desc.setName("java:module/env/HelloEJB_DD_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);

            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("component-scope resource defined in EJB DD");
            desc.setName("java:comp/env/HelloEJB_DD_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);
        }
        
        // connector-resource in annotation for stateful EJB
        {
            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("global-scope resource defined by @ConnectorResourceDefinition");
            desc.setName("java:global/env/HelloStatefulEJB_Annotation_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);
            
            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("application-scope resource defined by @ConnectorResourceDefinition");
            desc.setName("java:app/env/HelloStatefulEJB_Annotation_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);

            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("module-scope resource defined by @ConnectorResourceDefinition");
            desc.setName("java:module/env/HelloStatefulEJB_Annotation_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);

            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("component-scope resource defined by @ConnectorResourceDefinition");
            desc.setName("java:comp/env/HelloStatefulEJB_Annotation_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);
        }

        // connector-resource in annotation for stateless EJB
        {
            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("global-scope resource defined by @ConnectorResourceDefinition");
            desc.setName("java:global/env/HelloEJB_Annotation_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);
            
            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("application-scope resource defined by @ConnectorResourceDefinition");
            desc.setName("java:app/env/HelloEJB_Annotation_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);
            
            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("module-scope resource defined by @ConnectorResourceDefinition");
            desc.setName("java:module/env/HelloEJB_Annotation_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);
            
            desc = new ConnectorResourceDefinitionDescriptor();
            desc.setDescription("component-scope resource defined by @ConnectorResourceDefinition");
            desc.setName("java:comp/env/HelloEJB_Annotation_ConnectorResource");
            desc.setClassName("javax.resource.cci.ConnectionFactory");
            desc.addProperty("org.glassfish.connector-connection-pool.transaction-support", "LocalTransaction");
            desc.addProperty("org.glassfish.connector-connection-pool.resource-adapter-name", "crd-ra");
            expectedCRDDs.put(desc.getName(), desc);
        }
        
        TestUtil.compareCRDD(expectedCRDDs, acturalCRDDs);

    }
    
}
