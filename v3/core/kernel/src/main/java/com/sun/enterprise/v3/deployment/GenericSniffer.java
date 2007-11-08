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
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */

package com.sun.enterprise.v3.deployment;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.impl.DirectoryBasedRepository;
import org.jvnet.glassfish.api.container.Sniffer;
import org.jvnet.glassfish.api.deployment.Deployer;
import org.jvnet.glassfish.api.deployment.archive.ReadableArchive;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Singleton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Generic implementation of the Sniffer service that can be programmatically instantiated
 *
 * @author Jerome Dochez
 */
public abstract class GenericSniffer implements Sniffer {

    @Inject
    protected ModulesRegistry modulesRegistry;

    final private String containerName;
    final private String appStigma;
    final private String urlPattern;

    public GenericSniffer(String containerName, String appStigma, String urlPattern) {
        this.containerName = containerName;
        this.appStigma = appStigma;
        this.urlPattern = urlPattern;
    }

    /**
     * Returns true if the passed file or directory is recognized by this
     * instance.
     *
     * @param location the file or directory to explore
     * @param loader class loader for this application
     * @return true if this sniffer handles this application type
     */
    public boolean handles(ReadableArchive location, ClassLoader loader) {
        if (appStigma != null) {
            InputStream is = null;
            try {
                is = location.getEntry(appStigma);
                if (is != null) {
                    is.close();
                    return true;
                }
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

    /**
     * Returns the pattern to apply against the request URL
     * If the pattern matches the URL, the service method of the associated
     * container will be invoked
     *
     * @return pattern instance
     */
    public Pattern getURLPattern() {
        if (urlPattern!=null) {
            return Pattern.compile(urlPattern);
        } else {
            return null;
        }
    }

    /**
     * Returns the container name associated with this sniffer
     *
     * @return the container name
     */
    public String getModuleType() {
        return containerName;
    }

    /**
     * Sets up the container libraries so that any imported bundle from the
     * connector jar file will now be known to the module subsystem
     * @param containerHome is where the container implementation resides
     * @param logger the logger to use
     * @throws IOException exception if something goes sour
     */
    public void setup(String containerHome, Logger logger) throws IOException {
        // In most cases, the location of the jar files for a
        // particular container is in <containerHome>/lib.
        if (!(new File(containerHome).exists())) {
            throw new FileNotFoundException(getModuleType() + " container not found at " + containerHome);
        }
        try {
            File libDirectory = new File(containerHome, "lib");
            if (!libDirectory.exists()) {
                logger.warning(getModuleType() + " container does not have a lib directory");
                return;
            }
            DirectoryBasedRepository containerRepo = new DirectoryBasedRepository(getModuleType(),
                    libDirectory);
            containerRepo.initialize();
            modulesRegistry.addRepository(containerRepo);
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Cannot set up the repository for the container", e);
            throw e;
        }
    }

    /**
     * Tears down a container, remove all imported libraries from the module
     * subsystem.
     * 
     */
    public void tearDown() {
        
        modulesRegistry.removeRepository(getModuleType());
        
    }
    
    /**
     * Returns the list of Deployers that this Sniffer
     *
     * @return Deployers for this sniffer
     */
    public Deployer[] getDeployers(Logger logger) {
        
        String deployersNames[] = getDeployersNames();
        if (deployersNames==null || deployersNames.length==0) {
            return null;
        }
        
        List<Deployer> validDeployers = new ArrayList<Deployer>();
        for (String deployerName : deployersNames) {            
            try {
                Class c = Class.forName(deployerName);
                validDeployers.add(Deployer.class.cast(c.newInstance()));
            } catch (ClassNotFoundException e) {
                if (logger!=null) {
                    logger.log(Level.SEVERE, "Invalid phobos installation, cannot find the deployer");
                }
                return null;
            } catch (IllegalAccessException e) {
                if (logger!=null) {
                    logger.log(Level.SEVERE, "Invalid phobos installation, cannot find the deployer", e);
                }
                return null;
            } catch (InstantiationException e) {
                if (logger!=null) {
                    logger.log(Level.SEVERE, "Invalid phobos installation, cannot instantiate the deployer", e);
                }
                return null;
            }
        }
        return validDeployers.toArray(new Deployer[validDeployers.size()]);
    }   
    
    /**
     * Returns the list of deployer class names
     * @return the deployer class names
     */
    public abstract String[] getDeployersNames();
}
