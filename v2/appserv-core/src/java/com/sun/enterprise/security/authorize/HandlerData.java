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

package com.sun.enterprise.security.authorize;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.security.jacc.PolicyContextHandler;
import javax.xml.soap.SOAPMessage;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.rpc.handler.soap.SOAPMessageContext;

import com.sun.ejb.Invocation;
import com.sun.enterprise.security.PermissionCacheFactory;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.jauth.SOAPAuthParam;

/**
 * This class implements a thread scoped data used for PolicyContext.
 * @author Harry Singh
 * @author Jyri Virkki
 * @author Shing Wai Chan
 *
 */
public class HandlerData {
    
    private HttpServletRequest httpReq = null;
    private Invocation inv = null;

    private HandlerData(){}


    public static HandlerData getInstance(){
	return new HandlerData();
    }

    public void setHttpServletRequest(HttpServletRequest httpReq) {
	this.httpReq = httpReq;
    }

    public void setInvocation(Invocation inv) {
        this.inv = inv;
    }

    public Object get(String key){
	if (PolicyContextHandlerImpl.HTTP_SERVLET_REQUEST.equalsIgnoreCase(key)){
	    return httpReq;
	} else if (PolicyContextHandlerImpl.SUBJECT.equalsIgnoreCase(key)){
	    return SecurityContext.getCurrent().getSubject();
	} else if (PolicyContextHandlerImpl.REUSE.equalsIgnoreCase(key)) {
            PermissionCacheFactory.resetCaches();
            return new Integer(0);
        }

        if (inv == null) {
            return null;
        }

	if (PolicyContextHandlerImpl.SOAP_MESSAGE.equalsIgnoreCase(key)){
            SOAPMessage soapMessage = null;
	    MessageContext msgContext = inv.messageContext;

            if (msgContext != null) {
                if (msgContext instanceof SOAPMessageContext) {
		    SOAPMessageContext smc =
                            (SOAPMessageContext) msgContext;
		    soapMessage = smc.getMessage();
                }
	    } else {
                soapMessage = inv.getSOAPMessage();
            }

	    return soapMessage;
	} else if (PolicyContextHandlerImpl.ENTERPRISE_BEAN.equalsIgnoreCase(key)){
	    return inv.getJaccEjb();
	} else if (PolicyContextHandlerImpl.EJB_ARGUMENTS.equalsIgnoreCase(key)){
            if (inv.isWebService) {
                return null;
            } else {
                return (inv.methodParams != null) ?
                    inv.methodParams : new Object[0];
            }
	}
	return null;
    }
}
