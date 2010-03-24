/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.web.embed.impl;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.glassfish.api.embedded.LifecycleException;
import org.glassfish.api.embedded.web.Context;
import org.glassfish.api.embedded.web.config.SecurityConfig;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.Constants;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.servlets.DefaultServlet;


/**
 * Representation of a web application.
 *
 * @author Amy Roh
 */
// TODO: Add support for configuring environment entries
public class ContextImpl extends StandardContext implements Context {


    // ----------------------------------------------------- Instance Variables
    
    private SecurityConfig config;
    
    // --------------------------------------------------------- Public Methods

    /**
     * Enables or disables directory listings on this <tt>Context</tt>.
     *
     * @param directoryListing true if directory listings are to be
     * enabled on this <tt>Context</tt>, false otherwise
     */
    public void setDirectoryListing(boolean directoryListing) {
        Wrapper wrapper = (Wrapper) findChild(Constants.DEFAULT_SERVLET_NAME);
        if (wrapper !=null) {
            Servlet servlet = ((StandardWrapper)wrapper).getServlet();
            if (servlet instanceof DefaultServlet) {
                ((DefaultServlet)servlet).setListings(directoryListing);
            }
        }
    }

    /**
     * Checks whether directory listings are enabled or disabled on this
     * <tt>Context</tt>.
     *
     * @return true if directory listings are enabled on this 
     * <tt>Context</tt>, false otherwise
     */
    public boolean isDirectoryListing() {      
        Wrapper wrapper = (Wrapper) findChild(Constants.DEFAULT_SERVLET_NAME);
        if (wrapper !=null) {
            Servlet servlet = ((StandardWrapper)wrapper).getServlet();
            if (servlet instanceof DefaultServlet) {
                return ((DefaultServlet)servlet).isListings();
            }
        }
        return false;
    }

    /**
     * Set the security related configuration for this context
     *
     * @see org.glassfish.web.embed.config.SecurityConfig
     *
     * @param config the security configuration for this context
     */
    public void setSecurityConfig(SecurityConfig config) {
        this.config = config;
        // TODO 
    }

    /**
     * Gets the security related configuration for this context
     *
     * @see org.glassfish.web.embed.config.SecurityConfig
     *
     * @return the security configuration for this context
     */
    public SecurityConfig getSecurityConfig() {
        return config;
    }
    
    // ------------------------------------------------- Lifecycle Methods
            
    /**
     * Enables this component.
     * 
     * @throws LifecycleException if this component fails to be enabled
     */    
    public void enable() throws LifecycleException {               
       try {
            start();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new LifecycleException(e);
        }
    }

    /**
     * Disables this component.
     * 
     * @throws LifecycleException if this component fails to be disabled
     */
    public void disable() throws LifecycleException {
       try {
            stop();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new LifecycleException(e);
        }        
    }
    
    
}
