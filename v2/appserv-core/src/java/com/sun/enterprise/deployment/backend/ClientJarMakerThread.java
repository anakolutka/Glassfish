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
package com.sun.enterprise.deployment.backend;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.deploy.shared.AbstractArchive;
import com.sun.enterprise.deployment.deploy.shared.FileArchive;
import com.sun.enterprise.deployment.deploy.shared.OutputJarArchive;
import com.sun.enterprise.deployment.interfaces.ClientJarMaker;
import com.sun.enterprise.deployment.interfaces.DeploymentImplConstants;
import com.sun.enterprise.deployment.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.util.ModuleDescriptor;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.zip.ZipItem;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.Properties;
import java.util.Set;

/**
 * This thread subclass is responsible for creating the client jar
 * file.
 *
 * @author Jerome Dochez
 */
public class ClientJarMakerThread extends Thread {

    private DeploymentRequest request;
    private File clientJar;
    private ZipItem[] clientStubs;
    private String clientJarChoice = null;
    
    private static StringManager localStrings = StringManager.getManager( Deployer.class );
    
    /** Creates a new instance of ClientJarMakerThread */
    public ClientJarMakerThread(DeploymentRequest request, File clientJar, 
                                ZipItem[] clientStubs, 
                                String clientJarChoice) {
        this.request = request;
        this.clientJar = clientJar;
        this.clientStubs = clientStubs;
        this.clientJarChoice = clientJarChoice;
    }
    
    public void run() {
        // first thing to do is to register ourselves in the 
        // client jar maker registry
        ClientJarMakerRegistry registry = ClientJarMakerRegistry.getInstance();
        
        String moduleID = request.getName();
        registry.register(moduleID, this);
        
        // now we build the client jar file 
        try {
            createClientJar(request, clientJar, clientStubs, clientJarChoice);
        } catch(IASDeploymentException e) {
            // unfortunetely, we cannot provide failures feedback to the client
            // at this point, but we certainly need to log it.
            DeploymentLogger.get().log(Level.SEVERE, 
                localStrings.getString("enterprise.deployment.error_creating_client_jar", 
                    e.getLocalizedMessage()) ,e);
        
        } finally {
            
            // we are done, unregister ouselves from the registry
            registry.unregister(moduleID);
        }
        
        // friendly log
        DeploymentLogger.get().fine("Created client jar file for " + moduleID + " at " + clientJar.getAbsolutePath());
        
    }
   
    /**
     * this method is called from the thread to create the client jar or 
     * synchronously from the Deployer 
     */
    public static final void createClientJar(
        DeploymentRequest request, File clientJar, ZipItem[] clientStubs, 
        String clientJarChoice) throws IASDeploymentException {
        try {         
            // client jar naming convension is <app-name>Client.jar
            OutputJarArchive target = new OutputJarArchive();
            target.create(clientJar.getAbsolutePath());
            
            RootDeploymentDescriptor descriptor;
            if (request.getDescriptor().isVirtual()) {
                descriptor = request.getDescriptor().getStandaloneBundleDescriptor();
            } else {
                descriptor = request.getDescriptor();
            }
            
            AbstractArchive source = new FileArchive();
            ((FileArchive) source).open(request.getDeployedDirectory().getAbsolutePath());
            PEDeploymentFactoryImpl pe = new PEDeploymentFactoryImpl();
            Properties props = getPropertiesForClientJarMaker(
                CLIENT_JAR_CHOICES.getClientJarChoice(clientJarChoice),
                request, descriptor);
            ClientJarMaker jarMaker = pe.getClientJarMaker(props);

            // copy xml files from generated directory archive to original 
            // directory archive so the created client jar contain 
            // processed xml files.
            if (FileUtils.safeIsDirectory(
                request.getGeneratedXMLDirectory())) {
                AbstractArchive source2 = new FileArchive();
                ((FileArchive) source2).open(
                    request.getGeneratedXMLDirectory().getAbsolutePath());
                jarMaker.create(descriptor, source, source2, target, clientStubs,
                    null);
                source2.close();
            } else {
                jarMaker.create(descriptor, source, target, clientStubs,null);
            }
            source.close();
            target.close();
        } catch(Exception e) {
            IASDeploymentException newE = new IASDeploymentException();
            newE.initCause(e);
            throw newE;
        }
    }

    private static Properties getPropertiesForClientJarMaker(
        CLIENT_JAR_CHOICES choice,
        DeploymentRequest request, RootDeploymentDescriptor descriptor) {
      
        boolean qualify = qualifyModuleClientFormat(request,descriptor);

        Properties props = null;
        Boolean propertySetting = choice.useModuleClientJarMaker(qualify);

        if (propertySetting != null) {
            props = new Properties();
            props.setProperty(
                DeploymentImplConstants.USE_MODULE_CLIENT_JAR_MAKER, 
                propertySetting.toString());
        }
        return props;
    }

    /**
     * This method determines if the generated appclient jar should be in
     * the standalone appclient module format. There are 3 cases to create
     * the simpler version of the generated appclient:
     * 1) deployed module is a standalone appclient/ejb
     * 2) deployed module is an ear containing 0 or 1 appclient jar
     * 3) deployed module does not contain appclient that uses persistence unit
     */
    private static boolean qualifyModuleClientFormat
        (DeploymentRequest request, RootDeploymentDescriptor descriptor) {

        // create appclient format for standalone appclient module
        if (request.getDescriptor().isVirtual()) {
            return true;
        }

        Application app = Application.class.cast(descriptor);
        Set appClients = app.getApplicationClientDescriptors();
        if (appClients != null) {
    
            // create ear format of appclient if there are more than
            // one appclients in the ear file
            if (appClients.size() > 1) {
                return false;
            }

            if (!appClients.isEmpty()) {
                ApplicationClientDescriptor ac = 
                ApplicationClientDescriptor.class.cast(appClients.iterator().next());

                // checks to see if this appclient has entries for
                // message-destination-ref.  if so, use ear format
                Set msgDestRefs = ac.getMessageDestinationReferenceDescriptors();
                if (msgDestRefs != null && !msgDestRefs.isEmpty()) {
                    return false;
                }

                // checks to see if this appclient depends on a PU.
                // if so, use ear format
                Set entityMgrFacRefs = ac.getEntityManagerFactoryReferenceDescriptors();
                if (entityMgrFacRefs != null && !entityMgrFacRefs.isEmpty()) {
                    return false;
                }
            }
        }

        for (Iterator modules = app.getModules(); modules.hasNext();) {
            ModuleDescriptor md = ModuleDescriptor.class.cast(modules.next());

            // checks to see if any of the sub modules uses altDD
            // if so, use the application package format.  we could
            // also choose to override the original dd with the altDD,
            // but that might get very confusing when someone is trying
            // to debug.
            if (md.getAlternateDescriptor() != null) {
                return false;
            }
        }

        return true;
    }

    private static enum CLIENT_JAR_CHOICES {

        //original implementation, for comparing behavior regression
        USE_ORIGINAL_MAKER {
        public Boolean useModuleClientJarMaker(boolean qualify) {
            return null;}},

        //transitional default option. only turn on the generation of the
        //appclient module format
        USE_TRANSITION_MAKER {
        public Boolean useModuleClientJarMaker(boolean qualify) {
            return qualify == true ? Boolean.TRUE : null;}},

        //alternative default option. generate appclient module format
        //or the ear module format accordingly
        USE_COMBO_MAKER {
        public Boolean useModuleClientJarMaker(boolean qualify) {
            return new Boolean(qualify);}},

        //current default. generate only the ear module format, i.e. no 
        //optimization on certain deployed ear
        USE_EAR_MAKER {
        public Boolean useModuleClientJarMaker(boolean qualify) {
            return Boolean.FALSE;}};
       
        public abstract Boolean useModuleClientJarMaker(boolean qualify);
       
        public static CLIENT_JAR_CHOICES DEFAULT_CHOICE = USE_EAR_MAKER;
       
        public static CLIENT_JAR_CHOICES getClientJarChoice(String choice) {
            try {
                if (choice == null) {
                    return DEFAULT_CHOICE;
                }
                return valueOf(choice);
            } catch (IllegalArgumentException iae) {
                return DEFAULT_CHOICE;
            }
        }
    }
}
