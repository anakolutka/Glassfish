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

package com.sun.enterprise.v3.web;

import com.sun.enterprise.v3.deployment.GenericSniffer;
import org.jvnet.glassfish.api.container.Sniffer;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Singleton;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * this is necessary as long as the web container is not moved to the web subdirectory
 *
 * @author Jerome Dochez
 */
@Service(name="web")
@Scoped(Singleton.class)
public class WebSniffer  extends GenericSniffer implements Sniffer {

    public WebSniffer() {
        this("web", "WEB-INF/web.xml", null);
    }
    
    public WebSniffer(String containerName, String appStigma, String urlPattern) {
        super(containerName, appStigma, urlPattern);
    }

    /**
     * Sets up the container libraries so that any imported bundle from the
     * connector jar file will now be known to the module subsystem
     * @param containerHome is where the container implementation resides
     * @param logger the logger to use
     * @throws java.io.IOException exception if something goes sour
     */
    @Override
    public void setup(String containerHome, Logger logger) throws IOException {
        // do nothing, we are embedded in GFv3 for now
    }
    

    final String[] deployers = { "com.sun.enterprise.v3.web.WebDeployer" };
        
    public String[] getDeployersNames() {
        return deployers;
    }    
}
