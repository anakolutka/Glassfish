/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.admin.monitor.jvm;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.jvnet.hk2.component.PostConstruct;
import org.glassfish.internal.api.*;

/**
 *
 * @author PRASHANTH ABBAGANI
 */
@Service
public class JVMStatsProviderBootstrap implements PostStartup, PostConstruct {

    private JVMClassLoadingStatsProvider clStatsProvider = new JVMClassLoadingStatsProvider();
    private JVMCompilationStatsProvider compileStatsProvider = new JVMCompilationStatsProvider();
    private JVMMemoryStatsProvider memoryStatsProvider = new JVMMemoryStatsProvider();
    private JVMOSStatsProvider osStatsProvider = new JVMOSStatsProvider();
    private JVMRuntimeStatsProvider runtimeStatsProvider = new JVMRuntimeStatsProvider();
    private JVMThreadSystemStatsProvider threadSysStatsProvider = new JVMThreadSystemStatsProvider();
    private List<JVMGCStatsProvider> jvmStatsProviderList = new ArrayList();
    private ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    public static final String JVM = "jvm";

    public void postConstruct(){

        /* register with monitoring */
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/class-loading-system", clStatsProvider);
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/compilation-system", compileStatsProvider);
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            JVMGCStatsProvider jvmStatsProvider = new JVMGCStatsProvider(gc.getName());
            jvmStatsProviderList.add(jvmStatsProvider);
            StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/garbage-collectors/"+gc.getName(), jvmStatsProvider);
        }
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/memory", memoryStatsProvider);
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/operating-system", osStatsProvider);
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/runtime", runtimeStatsProvider);
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/thread-system", threadSysStatsProvider);
        for (ThreadInfo t : threadBean.getThreadInfo(threadBean.getAllThreadIds(), 5)) {
            JVMThreadInfoStatsProvider threadInfoStatsProvider = new JVMThreadInfoStatsProvider(t);
            StatsProviderManager.register("jvm", PluginPoint.SERVER, 
                    "jvm/thread-system/thread-"+t.getThreadId(), threadInfoStatsProvider);
        }
    } 
}
