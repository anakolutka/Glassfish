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
package org.glassfish.deployment.client;

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ClientConfiguration;

/**
 * Represents any type of owner of a Target.
 * <p>
 * Each Target object needs to know what object created it so it can
 * delegate certain task to that object.  Different classes that connect to the
 * admin back-end in different ways can create Target objects, so this interface
 * prescribes the behavior that each such "owner" of Targets must provide.
 * <p>
 * Fully-formed Target objects will have links back to their respective TargetOwner
 * objects.
 * 
 * @author tjquinn
 */
public interface TargetOwner {

    /**
     * Creates a single {@link Target} with the given name.
     * @param name the name of the Target to be returned
     * @return a new Target
     */
    public Target createTarget(String name);
    
    /**
     * Creates several {@link Target} objects with the specified names.
     * @param names the names of the targets to be returned
     * @return new Targets, one for each name specified
     */
    public Target[] createTargets(String[] names);
    
    /**
     * Returns a {@link ClientConfiguration} for the specified {@link TargetModuleID}.
     * <p>
     * Typically the owner of the Target implied by the TargetModuleID will 
     * create the configuration.
     * @param targetModuleID the representation of the module deployed to the 
     * specified {@link Target}
     * @return a new ClientConfiguration suitable for launching the corresponding
     * app client
     */
    public ClientConfiguration getClientConfiguration(TargetModuleID targetModuleID);
    
    /**
     * Returns the Web URL for the specified module on the {@link Target} implied by 
     * the TargetModuleID.
     * @param tmid
     * @return
     */
    public String getWebURL(TargetModuleID tmid);
}
