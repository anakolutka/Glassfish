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

package com.sun.enterprise.v3.services.impl;

import com.sun.grizzly.Controller;
import com.sun.grizzly.ControllerStateListener;
import com.sun.grizzly.http.portunif.HttpProtocolFinder;
import com.sun.grizzly.portunif.PUPreProcessor;
import com.sun.grizzly.portunif.ProtocolFinder;
import com.sun.grizzly.portunif.ProtocolHandler;
import com.sun.grizzly.portunif.TLSPUPreProcessor;
import com.sun.grizzly.tcp.Adapter;
import com.sun.enterprise.util.Result;

import java.io.IOException;
import java.util.ArrayList;
import javax.net.ssl.SSLContext;

/**
 * <p>The GrizzlyServiceListener is responsible of mapping incoming requests
 * to the proper Container or Grizzly extensions. Registered Containers can be
 * notified by Grizzly using three mode:</p>
 * <ul><li>At the transport level: Containers can be notified when TCP, TLS or UDP
 *                                 requests are mapped to them.</li>
 * <li>At the protocol level: Containers can be notified when protocols
 *                            (ex: SIP, HTTP) requests are mapped to them.</li>
 * </li>At the requests level: Containers can be notified when specific patterns
 *                             requests are mapped to them.</li><ul>
 *
 * @author Jeanfrancois Arcand
 */
public class GrizzlyServiceListener {
    private Controller controller;
    
    private int port;
    
    private boolean isEmbeddedHttpSecured;
    private GrizzlyEmbeddedHttp embeddedHttp;
    
    private String name;
    
    public GrizzlyServiceListener() {
    }
    
    public GrizzlyServiceListener(Controller controller) {
        this.controller = controller;
    }

    public void start(final GrizzlyProxy.GrizzlyFuture future) throws IOException, InstantiationException {
        final Thread t = Thread.currentThread();
        embeddedHttp.initEndpoint();
        embeddedHttp.getController().addStateListener(new ControllerStateListener() {
            public void onStarted() {
            }

            public void onReady() {
                future.setResult(new Result<Thread>(t));
            }

            public void onStopped() {

            }

            public void onException(Throwable throwable) {
                future.setResult(new Result<Thread>(throwable));
            }
        });
        embeddedHttp.startEndpoint();
    }
    
    public void stop() {
        embeddedHttp.stopEndpoint();
    }
    
    public void initializeEmbeddedHttp(boolean isSecured) {
        this.isEmbeddedHttpSecured = isSecured;
        if (isSecured) {
            embeddedHttp = new GrizzlyEmbeddedHttps();
        } else {
            embeddedHttp = new GrizzlyEmbeddedHttp();
        }
        
        embeddedHttp.setPort(port);
    }
    
    public EndpointMapper<Adapter> configureEndpointMapper(boolean isWebProfileMode) {
        // [1] Detect TLS requests.
        // If sslContext is null, that means TLS is not enabled on that port.
        // We need to revisit the way GlassFish is configured and make
        // sure TLS is always enabled. We can always do what we did for 
        // GlassFish v2, which is to located the keystore/trustore by ourself.
        // TODO: Enable TLS support on all ports using com.sun.Grizzly.SSLConfig
        if (!isWebProfileMode) {
            ArrayList<PUPreProcessor> puPreProcessors = new ArrayList<PUPreProcessor>();

            WebProtocolHandler.Mode webProtocolHandlerMode;
            
            if (isEmbeddedHttpSecured) {
                SSLContext sslContext = ((GrizzlyEmbeddedHttps) embeddedHttp).getSSLContext();
                PUPreProcessor preProcessor = new TLSPUPreProcessor(sslContext);
                puPreProcessors.add(preProcessor);
                webProtocolHandlerMode = WebProtocolHandler.Mode.HTTPS;
            } else {
                webProtocolHandlerMode = WebProtocolHandler.Mode.HTTP;
            }

            // [2] Add our supported ProtocolFinder. By default, we support http/sip
            // TODO: The list of ProtocolFinder is retrieved using System.getProperties().
            ArrayList<ProtocolFinder> protocolFinders = new ArrayList<ProtocolFinder>();
            protocolFinders.add(new HttpProtocolFinder());

            // [3] Add our supported ProtocolHandler. By default we support http/sip.
            ArrayList<ProtocolHandler> protocolHandlers = new ArrayList<ProtocolHandler>();
            WebProtocolHandler webProtocolHandler = 
                    new WebProtocolHandler(webProtocolHandlerMode, embeddedHttp);
            protocolHandlers.add(webProtocolHandler);

            embeddedHttp.configurePortUnification(protocolFinders, protocolHandlers, puPreProcessors);
        }
        
        return embeddedHttp;
    }

    public Controller getController() {
        return controller;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public GrizzlyEmbeddedHttp getEmbeddedHttp() {
        return embeddedHttp;
    }

    public boolean isEmbeddedHttpSecured() {
        return isEmbeddedHttpSecured;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        
        if (embeddedHttp != null) {
            embeddedHttp.setPort(port);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}