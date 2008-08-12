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

package org.glassfish.flashlight.provider;

import org.glassfish.flashlight.client.ProbeClientInvoker;
import org.glassfish.flashlight.client.ProbeHandle;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;

public class Probe
        implements ProbeHandle {

    private int id;

    private String moduleName;

    private String probeName;

    private String appName;

    private String providerName;

    private String[] probeParamNames;

    private Class[] paramTypes;

    private List<ProbeClientInvoker> invokerList = new ArrayList(2);

    private String providerJavaMethodName;

    private AtomicBoolean enabled = new AtomicBoolean(false);

    private String probeStr;

    public Probe(int id, String moduleName, String providerName, String appName, String probeName,
                 String[] probeParamNames, Class[] paramTypes) {
        this.id = id;
        this.moduleName = moduleName;
        this.providerName = providerName;
        this.appName = (appName == null) ? "" : appName;
        this.probeName = probeName;
        this.probeParamNames = probeParamNames;
        this.paramTypes = paramTypes;
        System.out.println("[FL]AppName = " + appName + " " + this.appName);
        this.probeStr = moduleName + ":" + providerName + ":" +
                this.appName + ":" + probeName;
    }

    public synchronized void addInvoker(ProbeClientInvoker invoker) {
        invokerList.add(invoker);
        enabled.set(true);
    }

    public synchronized void removeInvoker(ProbeClientInvoker invoker) {
        invokerList.remove(invoker);
        enabled.set(invokerList.size() > 0);
    }

    public void fireProbe(Object[] params) {

        System.out.println("[FL] fireProbe?? ==> " + enabled.get() + " " + invokerList.size());
        for (ProbeClientInvoker invoker : invokerList) {
            invoker.invoke(params);
        }
    }

    public boolean isEnabled() {        
        return enabled.get();
    }

    public int getId() {
        return id;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getAppName() {
        return appName;
    }

    public String getProbeName() {
        return probeName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String[] getProbeParamNames() {
        return probeParamNames;
    }

    public Class[] getParamTypes() {
        return paramTypes;
    }

    public String getProviderJavaMethodName() {
        return providerJavaMethodName;
    }

    public void setProviderJavaMethodName(String providerJavaMethodName) {
        this.providerJavaMethodName = providerJavaMethodName;
    }

    public String getProbeStr() {
        return probeStr;
    }

    public String toString() {
        StringBuilder sbldr = new StringBuilder(moduleName + ":" + providerName + ":" + appName + ":" + probeName);
        sbldr.append(" ").append(providerJavaMethodName).append("(");
        String delim = "";
        for (Class c : paramTypes) {
            sbldr.append(delim).append(c.getName());
            delim = ", ";
        }
        sbldr.append(")");

        return sbldr.toString();
    }
}
