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

package com.sun.enterprise.webservice;

import java.security.Principal;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.EndpointReference;
import com.sun.web.security.WebPrincipal;
import com.sun.enterprise.Switch;
import com.sun.enterprise.InvocationManager;
import com.sun.ejb.containers.StatelessSessionContainer;
import com.sun.xml.ws.api.server.WSWebServiceContext;
import com.sun.xml.ws.api.message.Packet;

/**
 * <p><b>NOT THREAD SAFE: mutable instance variables</b>
 */
public final class WebServiceContextImpl implements WSWebServiceContext {
    
    public static final ThreadLocal msgContext = new ThreadLocal();
    
    public static final ThreadLocal principal = new ThreadLocal();

    private WSWebServiceContext jaxwsContextDelegate;
    
    public void setContextDelegate(WSWebServiceContext wsc) {
        this.jaxwsContextDelegate = wsc;
    }
    
    public MessageContext getMessageContext() {
        return this.jaxwsContextDelegate.getMessageContext();
    }

    public void setMessageContext(MessageContext ctxt) {
        msgContext.set(ctxt);
    }

    /*
     * this may still be required for EJB endpoints
     *
     */
    public void setUserPrincipal(WebPrincipal p) {
        principal.set(p);
    }
    
    public Principal getUserPrincipal() {
        // This could be an EJB endpoint; check the threadlocal variable
        WebPrincipal p = (WebPrincipal) principal.get();
        if (p != null) {
            return p;
        }
        // This is a servlet endpoint
        return this.jaxwsContextDelegate.getUserPrincipal();
    }

    public boolean isUserInRole(String role) {
        Switch sw = Switch.getSwitch();
        InvocationManager mgr = sw.getInvocationManager();
        Object o = mgr.getCurrentInvocation().getContainerContext();
        if(o instanceof StatelessSessionContainer) {
            StatelessSessionContainer cont = (StatelessSessionContainer) o;
            boolean res = cont.getSecurityManager().isCallerInRole(role);
            return res;
        }
        // This is a servlet endpoint
        return this.jaxwsContextDelegate.isUserInRole(role);
    }
    
    public EndpointReference getEndpointReference(Class clazz, org.w3c.dom.Element... params) {
        return this.jaxwsContextDelegate.getEndpointReference(clazz, params);
    }
    
    public EndpointReference getEndpointReference(org.w3c.dom.Element... params) {
        return this.jaxwsContextDelegate.getEndpointReference(params);
    }
    
    public Packet getRequestPacket() {
        return this.jaxwsContextDelegate.getRequestPacket();
    }
}
