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
package com.sun.enterprise.deployment.archivist;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.ContractProvider;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Singleton;

import javax.enterprise.deploy.shared.ModuleType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This factory class is responsible for creating Archivists
 *
 * @author  Jerome Dochez
 */
@Service
@Scoped(Singleton.class)
public class ArchivistFactory implements ContractProvider {
    
    // TODO: right now the ApplicationArchivist is not in the list
    // to avoid circular injection
    @Inject
    PrivateArchivist[] privateArchivists;

    @Inject
    ArchiveFactory archiveFactory;

    public Archivist getArchivist(ReadableArchive archive, 
        ClassLoader cl) throws IOException {
        Archivist archivist = getPrivateArchivistFor(archive);
        if (archivist!=null) {
            archivist.setClassLoader(cl);
        }
        return archivist;
    }


    public Archivist getArchivist(ModuleType moduleType)
        throws IOException {
        return getPrivateArchivistFor(moduleType);
    }

    /**
     * Only archivists should have access to this API. we'll see how it works,
     * @param moduleType
     * @return
     * @throws IOException
     */
    Archivist getPrivateArchivistFor(ModuleType moduleType) 
        throws IOException {
        for (PrivateArchivist pa : privateArchivists) {
            Archivist a = Archivist.class.cast(pa);
            if (a.getModuleType().equals(moduleType)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Only archivists should have access to this API. we'll see how it works,
     * @param archive 
     * @return
     * @throws IOException
     */
    Archivist getPrivateArchivistFor(ReadableArchive archive) 
        throws IOException {
        //first, check the existence of any deployment descriptors
        for (PrivateArchivist pa : privateArchivists) {
            Archivist a = Archivist.class.cast(pa);
            if (a.hasStandardDeploymentDescriptor(archive) ||
                    a.hasRuntimeDeploymentDescriptor(archive)) {
                return a;
            }
        }

        // Java EE 5 Specification: Section EE.8.4.2.1

        //second, check file extension if any, excluding .jar as it needs
        //additional processing
        String uri = archive.getURI().getPath();
        File file = new File(uri);
        if (!file.isDirectory() && !uri.endsWith(Archivist.EJB_EXTENSION)) {
            for (PrivateArchivist pa : privateArchivists) {
                Archivist a = Archivist.class.cast(pa);
                if (uri.endsWith(a.getArchiveExtension())) {
                    return a;
                }
            }
        }

        //finally, still not returned here, call for additional processing
        for (PrivateArchivist pa : privateArchivists) {
            Archivist a = Archivist.class.cast(pa);
            if (a.postHandles(archive)) {
                return a;
            }
        }
 
        return null;
    }
}
