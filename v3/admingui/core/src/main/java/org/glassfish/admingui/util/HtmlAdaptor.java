/*
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.util;

/**
 *
 * @author anilam
 */
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class HtmlAdaptor {

    private static boolean _bHtmlAdaptorServerRegistered = false;

    public static void registerHTMLAdaptor(MBeanServerConnection mbsc) {
        if(_bHtmlAdaptorServerRegistered)
            return;
        try {
            int port = Integer.parseInt(System.getProperty("html.adaptor.port", "4444"));
            Class cl =  Class.forName("com.sun.jdmk.comm.HtmlAdaptorServer");
            Constructor contr = cl.getConstructor(new Class[]{Integer.TYPE});
            Object adaptor = contr.newInstance(new Object[]{Integer.valueOf(port)});
            Method method = cl.getMethod("start");
            ObjectName htmlAdaptorObjectName = new ObjectName(
                    "Adaptor:name=html,port="+port);
            MBeanServer mbs = (MBeanServer) mbsc;
            mbs.registerMBean(adaptor, htmlAdaptorObjectName);
            method.invoke(adaptor);
            _bHtmlAdaptorServerRegistered = true;
        } catch (Exception e) {
            //System.out.println("Warning !! cannot create HTML Adapter. Ensure that you have jmxtools.jar in <glassfish>/lib directory");
            //System.out.println(e.getMessage());
            //e.printStackTrace();
        }
    }
}
