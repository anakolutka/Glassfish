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

package com.sun.enterprise.tools.upgrade.common;

import java.util.*;

/**
 *
 * author : Gautam Borah
 *
 */

public class CommonProperties{
    
    private static final String TRANSFORMATION_MODULE = "com.sun.enterprise.tools.upgrade.transform.TransformManager";
    private static final String NSS_TO_JKS_MODULE = "com.sun.enterprise.tools.upgrade.certconversion.NSStoJKSConversionModule";
    private static final String JKS_TO_JKS_MODULE = "com.sun.enterprise.tools.upgrade.certconversion.JKStoJKSConversionModule";
    private static final String NSS_TO_NSS_MODULE = "com.sun.enterprise.tools.upgrade.certconversion.NSStoNSSConversionModule";
    private static final String JKS_TO_NSS_MODULE = "com.sun.enterprise.tools.upgrade.certconversion.JKStoNSSConversionModule";
    private static final String REALM_MODULE = "com.sun.enterprise.tools.upgrade.realm.RealmUpgrade";
    private static final String DEPLOYMENT_MODULE = "com.sun.enterprise.tools.upgrade.deployment.DeploymentUpgrade";
    private static final String MISC_MODULE = "com.sun.enterprise.tools.upgrade.miscconfig.ConfigTransfers";
    private static final String CERT_CONVERSION = "com.sun.enterprise.tools.upgrade.certconversion.CertificateTransfer";
    /**
     * Adds modules to the linked list in the order of invocation
     */
    
    public static List getRegisteredModules(CommonInfoModel model){
        List list = new LinkedList();
        list.add(TRANSFORMATION_MODULE);
        list.add(DEPLOYMENT_MODULE);
        list.add(MISC_MODULE);
        list.add(REALM_MODULE);
        list.add(CERT_CONVERSION);
        return list;
    }
}
