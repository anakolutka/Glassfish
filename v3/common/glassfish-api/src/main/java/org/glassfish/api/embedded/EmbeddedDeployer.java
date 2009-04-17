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
package org.glassfish.api.embedded;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.DeployCommandParameters;

import java.util.Properties;
import java.util.Collection;
import java.io.File;

/**
 * Service to deploy applications to the embedded server.
 *
 * @author Jerome Dochez
 */
public interface EmbeddedDeployer {

    /**
     * Returns the location of the applications directory, where deployed applications
     * are saved.
     *
     * @return the deployed application directory.
     */
    public File getApplicationsDir();

    /**
     * Enables the auto-deployment
     */
    public void enableAutoDeploy();

    /**
     * Disables the auto-deployment
     */
    public void disableAutoDeploy();

    /**
     * Deploys a file or directory to the servers passing the deployment command parameters
     *
     * @param archive archive or directory of the application
     * @param params deployment command parameters
     * @return the deployed application name
     */
    public String deploy(File archive, DeployCommandParameters params);

    /**
     * Deploys an archive abstraction to the servers passing the deployment command parameters
     *
     * @param archive archive or directory of the application
     * @param params deployment command parameters
     * @return the deployed application name
     */
    public void deploy(ReadableArchive archive, DeployCommandParameters params);

    /**
     * Undeploys a previously deployed application
     *
     * @param name name returned by {@link EmbeddedDeployer#deploy(File, org.glassfish.api.deployment.DeployCommandParameters}
     */
    public void undeploy(String name);

    /**
     * Undeploys all deployed applications.
     */
    public void undeployAll();   
}
