/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.osgijdbc;

import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.api.Globals;
import org.glassfish.osgijavaeebase.Extender;
import org.jvnet.hk2.component.Habitat;
import org.osgi.framework.*;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JDBCExtender implements Extender, SynchronousBundleListener {

    private BundleContext bundleContext;

    private ServiceRegistration urlHandlerService;

    private Set<DataSourceFactoryImpl> dataSourceFactories = new HashSet<DataSourceFactoryImpl>();
    private Habitat habitat;
    private GlassFishResourceProviderService rps;


    private static final Logger logger = Logger.getLogger(
            JDBCExtender.class.getPackage().getName());

    public JDBCExtender(BundleContext context) {
        this.bundleContext = context;
    }

    public void start() {
        debug("begin start()");
        habitat = Globals.getDefaultHabitat();
        bundleContext.addBundleListener(this);
        addURLHandler();
        rps = new GlassFishResourceProviderService(habitat, bundleContext);
        rps.registerJdbcResources();
        debug("completed start()");
    }

    public void stop() {
        rps.unRegisterJdbcResources();
        removeURLHandler();
        for (DataSourceFactoryImpl dsfi : dataSourceFactories) {
            dsfi.preDestroy();
        }
        bundleContext.removeBundleListener(this);
        debug("stopped");
    }

    private Habitat getHabitat(){
        return habitat;
    }

    private void addURLHandler() {

        //create parent class-loader (API ClassLoader to access Java EE API)
        ClassLoaderHierarchy clh = getHabitat().getByContract(ClassLoaderHierarchy.class);
        ClassLoader apiClassLoader = clh.getAPIClassLoader();

        Properties p = new Properties();
        p.put(URLConstants.URL_HANDLER_PROTOCOL, new String[]{Constants.JDBC_DRIVER_SCHEME});
        urlHandlerService = bundleContext.registerService(URLStreamHandlerService.class.getName(),
                new JDBCDriverURLStreamHandlerService(apiClassLoader), p);
    }

    private void removeURLHandler() {
        if (urlHandlerService != null) {
            urlHandlerService.unregister();
        }
    }

    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        Dictionary header = event.getBundle().getHeaders();
        switch (event.getType()) {
            case BundleEvent.STARTING:
                if (isJdbcDriverBundle(bundle)) {
                    debug("Starting JDBC Bundle : " + bundle.getSymbolicName());

                    DataSourceFactoryImpl dsfi = new DataSourceFactoryImpl(event.getBundle().getBundleContext());
                    dataSourceFactories.add(dsfi);

                    Properties serviceProperties = new Properties();
                    serviceProperties.put(DataSourceFactory.JDBC_DRIVER_CLASS,
                            header.get(Constants.DRIVER.replace(".", "_")));

                    String implVersion = (String) header.get(Constants.IMPL_VERSION);
                    if (implVersion != null) {
                        serviceProperties.put(DataSourceFactory.JDBC_DRIVER_VERSION, implVersion);
                    }

                    String implTitle = (String) header.get(Constants.IMPL_TITLE);
                    if (implTitle != null) {
                        serviceProperties.put(DataSourceFactory.JDBC_DRIVER_NAME, implTitle);
                    }
                    debug(" registering service for driver [" +
                            header.get(Constants.DRIVER.replace(".", "_")) + "]");
                    event.getBundle().getBundleContext().registerService(DataSourceFactory.class.getName(),
                            dsfi, serviceProperties);
                }
                break;
            case BundleEvent.STOPPED:
                if (isJdbcDriverBundle(bundle)) {
                    debug("JDBC Bundle Stopped : " + bundle.getSymbolicName());
                }
                break;
        }
    }

    private boolean isJdbcDriverBundle(Bundle b) {
        String osgiRFC = (String) b.getHeaders().get(Constants.OSGI_RFC_122);
        if (osgiRFC != null && Boolean.valueOf(osgiRFC)) {
            return true;
        } else {
            return false;
        }
    }

    private void debug(String s) {
        if(logger.isLoggable(Level.FINEST)){
            logger.finest("[osgi-jdbc] : " + s);
        }
    }
}
