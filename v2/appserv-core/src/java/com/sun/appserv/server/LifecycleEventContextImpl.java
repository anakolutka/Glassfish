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

/**
 * PROPRIETARY/CONFIDENTIAL.  Use of this product is subject to license terms.
 *
 * Copyright 2000-2001 by iPlanet/Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 */

package com.sun.appserv.server;

import javax.naming.InitialContext;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.enterprise.server.ServerContext;

/**
 * ServerContext interface:
 * the server-wide runtime environment created by ApplicationServer and shared
 * by its subsystems such as the web container or EJB container.
 */
public class LifecycleEventContextImpl implements LifecycleEventContext {
    
    private ServerContext ctx;

    private static Logger logger = LogDomains.getLogger(LogDomains.ROOT_LOGGER);

    /**
     * public constructor
     */
    public LifecycleEventContextImpl(ServerContext ctx) {
        this.ctx = ctx;
    }
    
    /**
     * Get the server command-line arguments
     */
    public String[] getCmdLineArgs() {
        return ctx.getCmdLineArgs();
    }
    
    /**
     * Get server installation root
     */
    public String getInstallRoot() {
        return ctx.getInstallRoot();
    }
    
    /**
     * Get the server instance name
     */
    public String getInstanceName() {
        return ctx.getInstanceName();
    }
    
    /** 
     * Get the initial naming context.
     */
    public InitialContext getInitialContext() {
        return ctx.getInitialContext();
    }

    /**
     * Writes the specified message to a server log file.
     *
     * @param msg 	a <code>String</code> specifying the 
     *			message to be written to the log file
     */
    public void log(String message) {
        logger.info(message);
    }
    
    /**
     * Writes an explanatory message and a stack trace
     * for a given <code>Throwable</code> exception
     * to the server log file.
     *
     * @param message 		a <code>String</code> that 
     *				describes the error or exception
     *
     * @param throwable 	the <code>Throwable</code> error 
     *				or exception
     */
    public void log(String message, Throwable throwable) {
        logger.log(Level.INFO, message, throwable);
    }
}
