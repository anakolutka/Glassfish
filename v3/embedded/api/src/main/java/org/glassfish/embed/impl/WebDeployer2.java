/*
 * Copyright (c) 2008, Your Corporation. All Rights Reserved.
 */

package org.glassfish.embed.impl;

import com.sun.enterprise.web.WebDeployer;

import java.net.URL;
import java.io.IOException;
import org.glassfish.embed.GlassFish;
import org.jvnet.hk2.annotations.Inject;


/**
 * @author Kohsuke Kawaguchi
 */
public class WebDeployer2 extends WebDeployer {

    @Inject
    protected GlassFish server;

     protected URL getDefaultWebXML() throws IOException {
        URL url = server.getDefaultWebXml();
        if (url == null) {
            url = getClass().getClassLoader().getResource("org/glassfish/embed/default-web.xml");
        }
         if(url==null)
             throw new AssertionError("default-web.xml is missing from resources");
         return url;
     }
}
