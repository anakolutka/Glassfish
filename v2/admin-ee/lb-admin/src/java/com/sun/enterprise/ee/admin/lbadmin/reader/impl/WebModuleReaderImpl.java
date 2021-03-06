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
package com.sun.enterprise.ee.admin.lbadmin.reader.impl;

import com.sun.enterprise.config.ConfigBean;
import com.sun.enterprise.config.ConfigContext;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.WebModule;
import com.sun.enterprise.ee.admin.lbadmin.reader.api.WebModuleReader;
import com.sun.enterprise.ee.admin.lbadmin.reader.api.IdempotentUrlPatternReader;
import com.sun.enterprise.tools.common.dd.webapp.SunWebApp;

import com.sun.enterprise.ee.admin.lbadmin.transform.Visitor;
import com.sun.enterprise.ee.admin.lbadmin.transform.WebModuleVisitor;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.ee.EELogDomains;
import com.sun.enterprise.util.i18n.StringManager;

import com.sun.enterprise.ee.admin.lbadmin.reader.api.LbReaderException;

/**
 * Provides web module information relavant to Load balancer tier.
 *
 * @author Satish Viswanatham
 */
public class WebModuleReaderImpl implements WebModuleReader {

    public WebModuleReaderImpl(ConfigContext ctx, ApplicationRef ref, 
        ConfigBean wm,SunWebApp bean) {

        if ( wm != null ) {
            if (wm instanceof WebModule) {
                _wm = (WebModule) wm;
            } else {
                String msg = _strMgr.getString("UnknownTypeInWebModuleReader");
                throw new RuntimeException(msg);
            }
        } else {
            _wm = null;
        }
        _applicationRef = ref;
        _bean           = bean;
    }

    public String getContextRoot() throws LbReaderException {
        // Web module bean can have context root 
        // first get the context root from domain.xml.
        // If context root is not availble in domain.xml, get it from
        // sun-web.xml.
        String context = null;
        if (_wm != null) {
             context = _wm.getContextRoot();
        }

        if(context == null) {
            context =  _bean.getContextRoot();
        }
        return context;
    }

    public String getErrorUrl() throws LbReaderException {
        if (_bean == null) {
            return null;
        } else {
            return _bean.getErrorUrl();
        }
    }

    public boolean getLbEnabled() throws LbReaderException {
        return _applicationRef.isLbEnabled();
    }

    public String getDisableTimeoutInMinutes() throws LbReaderException {
        return _applicationRef.getDisableTimeoutInMinutes();
    }

    public IdempotentUrlPatternReader[] getIdempotentUrlPattern() 
                                                throws LbReaderException {
        if (_bean == null) {
            return null;
        } else {
            int len = _bean.sizeIdempotentUrlPattern();
            IdempotentUrlPatternReader []iRdrs=
                    new IdempotentUrlPatternReader [len];

            for(int i=0; i < len; i++) {
                iRdrs[i] = new IdempotentUrlPatternReaderImpl(_bean,i);
            }

            return iRdrs;
        }
    }

    public void accept(Visitor v) {
        WebModuleVisitor wv = (WebModuleVisitor)v;
        wv.visit(this);
    }

    // ---- VARIABLE(S) - PRIVATE -----------------------------
    private ApplicationRef _applicationRef     = null;
    private SunWebApp _bean                   = null;
    private WebModule _wm                   = null;
    private static final StringManager _strMgr = 
        StringManager.getManager(WebModuleReaderImpl.class);
}
