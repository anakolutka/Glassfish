/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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
 *
 */



/**
 *
 * @author bnevins
 */
package org.glassfish.embed.impl;

import java.util.LinkedList;

import org.glassfish.api.ActionReport;
import org.glassfish.api.container.Sniffer;
import org.glassfish.internal.data.ContainerInfo;
import org.glassfish.internal.data.ContainerRegistry;
import org.jvnet.hk2.annotations.Inject;

import com.sun.enterprise.v3.deployment.DeploymentContextImpl;
import com.sun.enterprise.v3.server.ApplicationLifecycle;
import com.sun.enterprise.v3.server.ProgressTracker;

/**
 *
 * This implementation fix a problem in ApplicationLifeCycle.setupContainerInfos()
 * (in 10.0-build-20080724)
 * <p>
 *
 * In the case of the embedded GlassFish, when deploying a ScatteredWAR a single sniffer
 * is passed to that method and the requires array will be empty, and the provides array will contain only one entry
 * of type Application (if I remember well).
 * The mechanism used to compute and sort the deployers for a given archive is not
 * working correctly because it will return in that case an empty list of ContainerInfo
 * and the ScatteredWar will not be deployed (the following error is thrown:
 * "There is no installed container capable of handling this application")
 *
 * This is fixing that problem by adding a deployer if no deployers were found
 * by the default implementation
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ApplicationLifecycle2 extends ApplicationLifecycle {

    // workaround -> container registry not visible from super class
    @Inject protected ContainerRegistry creg;


    @Override
    protected LinkedList<ContainerInfo> setupContainerInfos(
            Iterable<Sniffer> sniffers, DeploymentContextImpl context,
            ActionReport report, ProgressTracker tracker) throws Exception {
        LinkedList<ContainerInfo> result = super.setupContainerInfos(sniffers, context, report, tracker);

        if (result != null && result.isEmpty()) {

            for (Sniffer sniffer : sniffers) {
                for (String containerName : sniffer.getContainersNames()) {
                    ContainerInfo<?, ?> containerInfo = creg.getContainer(containerName);
                    if (containerInfo != null) {
                        result.add(containerInfo);
                    }
                }
            }
        }
        return result;
    }

}
