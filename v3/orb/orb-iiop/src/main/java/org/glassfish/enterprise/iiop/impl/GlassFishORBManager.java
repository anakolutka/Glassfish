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
package org.glassfish.enterprise.iiop.impl;

import com.sun.corba.ee.spi.oa.rfm.ReferenceFactoryManager;
import com.sun.corba.ee.spi.orb.ORBFactory;
import com.sun.corba.ee.spi.orbutil.ORBConstants;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.logging.LogDomains;
import org.glassfish.api.admin.config.Property;
import org.glassfish.enterprise.iiop.api.GlassFishORBLifeCycleListener;
import org.omg.CORBA.ORB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class initializes the ORB with a list of (standard) properties
 * and provides a few convenience methods to get the ORB etc.
 */

public final class GlassFishORBManager {
    static Logger logger = LogDomains.getLogger(GlassFishORBManager.class, LogDomains.UTIL_LOGGER);

    private static IIOPUtils iiopUtils = IIOPUtils.getInstance();

    private static final boolean debug = true;

    // Various pluggable classes defined in the app server that are used
    // by the ORB.
    private static final String ORB_CLASS =
            "com.sun.corba.ee.impl.orb.ORBImpl";
    private static final String ORB_SINGLETON_CLASS =
            "com.sun.corba.ee.impl.orb.ORBSingleton";

    private static final String ORB_SE_CLASS =
            "com.sun.corba.se.impl.orb.ORBImpl";
    private static final String ORB_SE_SINGLETON_CLASS =
            "com.sun.corba.se.impl.orb.ORBSingleton";

    private static final String PEORB_CONFIG_CLASS =
            "org.glassfish.enterprise.iiop.impl.PEORBConfigurator";
    private static final String IIOP_SSL_SOCKET_FACTORY_CLASS =
            "com.sun.enterprise.iiop.IIOPSSLSocketFactory";
    private static final String RMI_UTIL_CLASS =
            "com.sun.corba.ee.impl.javax.rmi.CORBA.Util";
    private static final String RMI_STUB_CLASS =
            "com.sun.corba.ee.impl.javax.rmi.CORBA.StubDelegateImpl";
    private static final String RMI_PRO_CLASS =
            "com.sun.corba.ee.impl.javax.rmi.PortableRemoteObject";

    // JNDI constants
    public static final String JNDI_PROVIDER_URL_PROPERTY =
            "java.naming.provider.url";
    public static final String JNDI_CORBA_ORB_PROPERTY =
            "java.naming.corba.orb";

    // RMI-IIOP delegate constants
    public static final String ORB_UTIL_CLASS_PROPERTY =
            "javax.rmi.CORBA.UtilClass";
    public static final String RMIIIOP_STUB_DELEGATE_CLASS_PROPERTY =
            "javax.rmi.CORBA.StubClass";
    public static final String RMIIIOP_PRO_DELEGATE_CLASS_PROPERTY =
            "javax.rmi.CORBA.PortableRemoteObjectClass";

    // ORB constants: OMG standard
    public static final String OMG_ORB_CLASS_PROPERTY =
            "org.omg.CORBA.ORBClass";
    public static final String OMG_ORB_SINGLETON_CLASS_PROPERTY =
            "org.omg.CORBA.ORBSingletonClass";
    public static final String OMG_ORB_INIT_HOST_PROPERTY =
            ORBConstants.INITIAL_HOST_PROPERTY;
    public static final String OMG_ORB_INIT_PORT_PROPERTY =
            ORBConstants.INITIAL_PORT_PROPERTY;
    private static final String PI_ORB_INITIALIZER_CLASS_PREFIX =
            ORBConstants.PI_ORB_INITIALIZER_CLASS_PREFIX;

    // ORB constants: Sun specific
    public static final String SUN_USER_CONFIGURATOR_PREFIX =
            ORBConstants.USER_CONFIGURATOR_PREFIX;
    public static final String SUN_ORB_ID_PROPERTY =
            ORBConstants.ORB_ID_PROPERTY;
    public static final String SUN_ORB_SERVER_HOST_PROPERTY =
            ORBConstants.SERVER_HOST_PROPERTY;
    public static final String SUN_ORB_SERVER_PORT_PROPERTY =
            ORBConstants.SERVER_PORT_PROPERTY;
    public static final String SUN_ORB_SOCKET_FACTORY_CLASS_PROPERTY =
            ORBConstants.SOCKET_FACTORY_CLASS_PROPERTY;
    public static final String SUN_ORB_IOR_TO_SOCKETINFO_CLASS_PROPERTY =
            ORBConstants.IOR_TO_SOCKET_INFO_CLASS_PROPERTY;
    public static final String SUN_MAX_CONNECTIONS_PROPERTY =
            ORBConstants.HIGH_WATER_MARK_PROPERTY;
    public static final String ORB_LISTEN_SOCKET_PROPERTY =
            ORBConstants.LISTEN_SOCKET_PROPERTY;
    //
    // XXX The following constants do not appear to be used in the ORB
    public static final String ORB_DISABLED_PORTS_PROPERTY =
            "com.sun.CORBA.connection.ORBDisabledListenPorts";
    private static final String SUN_LISTEN_ADDR_ANY_ADDRESS =
            "com.sun.CORBA.orb.AddrAnyAddress";
    private static final String ORB_IOR_ADDR_ANY_INITIALIZER =
            "com.sun.enterprise.iiop.IORAddrAnyInitializer";

    // ORB configuration constants
    private static final String DEFAULT_SERVER_ID = "100";
    private static final String DEFAULT_MAX_CONNECTIONS = "1024";
    private static final String J2EE_INITIALIZER =
            "com.sun.enterprise.iiop.GlassFishORBInitializer";
    private static final String SUN_GIOP_DEFAULT_FRAGMENT_SIZE = "1024";
    private static final String SUN_GIOP_DEFAULT_BUFFER_SIZE = "1024";

    private static final String IIOP_CLEAR_TEXT_CONNECTION =
            "IIOP_CLEAR_TEXT";
    public static final String DEFAULT_ORB_INIT_HOST = "localhost";

    // This will only apply for stand-alone java clients, since
    // in the server the orb port comes from domain.xml, and in an appclient
    // the port is set from the sun-acc.xml.  It's set to the same 
    // value as the default orb port in domain.xml as a convenience.
    // That way the code only needs to do a "new InitialContext()"
    // without setting any jvm properties and the naming service will be
    // found.  Of course, if the port was changed in domain.xml for some
    // reason the code will still have to set org.omg.CORBA.ORBInitialPort.
    public static final String DEFAULT_ORB_INIT_PORT = "3700";

    // CSIv2 config
    private static final String SSL = "SSL";
    private static final String SSL_MUTUALAUTH = "SSL_MUTUALAUTH";
    private static final String ORB_SSL_CERTDB_PATH =
            "com.sun.CSIV2.ssl.CertDB";
    private static final String ORB_SSL_CERTDB_PASSWORD =
            "com.sun.CSIV2.ssl.CertDBPassword";
    public static final String SUN_GIOP_FRAGMENT_SIZE_PROPERTY =
            "com.sun.CORBA.giop.ORBFragmentSize";
    public static final String SUN_GIOP_BUFFER_SIZE_PROPERTY =
            "com.sun.CORBA.giop.ORBBufferSize";

    // This property is true (in appclient Main) 
    // if SSL is required to be used by clients.
    public static final String ORB_SSL_CLIENT_REQUIRED =
            "com.sun.CSIV2.ssl.client.required";
    //
    // This property is true if SSL is required to be used by 
    // non-EJB CORBA objects in the server.
    public static final String ORB_SSL_SERVER_REQUIRED =
            "com.sun.CSIV2.ssl.server.required";
    //
    // This property is true if client authentication is required by 
    // non-EJB CORBA objects in the server.
    public static final String ORB_CLIENT_AUTH_REQUIRED =
            "com.sun.CSIV2.client.auth.required";

    // We need this to get the ORB monitoring set up correctly
    public static final String S1AS_ORB_ID = "S1AS-ORB";

    // the ORB instance shared throughout the app server
    private static ORB orb = null;

    // The ReferenceFactoryManager from the orb.
    private static ReferenceFactoryManager rfm = null;

    private static int orbInitialPort = -1;

    private static IiopListener[] iiopListenerBeans = null;
    private static Orb orbBean = null;
    private static IiopService iiopServiceBean = null;

    private static Properties csiv2Props = new Properties();

    private static final Properties EMPTY_PROPERTIES = new Properties();

    private static AtomicBoolean propsInitialized = new AtomicBoolean(false);

    /**
     * don't want any subclassing of this class
     */
    private GlassFishORBManager() {
    }

    /**
     * Returns whether an adapterName (from ServerRequestInfo.adapter_name)
     * represents an EJB or not.
     */

    public static boolean isEjbAdapterName(String[] adapterName) {
        boolean result = false;
        if (rfm != null)
            result = rfm.isRfmName(adapterName);

        return result;
    }

    /**
     * Returns whether the operationName corresponds to an "is_a" call
     * or not (used to implement PortableRemoteObject.narrow.
     */
    public static boolean isIsACall(String operationName) {
        return operationName.equals("_is_a");
    }

    /**
     * Return the shared ORB instance for the app server.
     * If the ORB is not already initialized, it is created
     * with the standard server properties, which can be
     * overridden by Properties passed in the props argument.
     */
    public static synchronized ORB getORB(Properties props) {
        try {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "GlassFishORBManager.getORB->: " + orb);
            }

            initProperties();
            if (orb == null) {
                initORB(props);
            }

            return orb;
        } finally {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "GlassFishORBManager.getORB<-: " + orb);
            }
        }
    }

    public static synchronized Properties getCSIv2Props() {
        initProperties();

        return csiv2Props;
    }

    public static synchronized int getORBInitialPort() {
        initProperties();

        return orbInitialPort;
    }

    private static void initProperties() {
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "GlassFishORBManager.initProperties->: " + orb);
            }

            if (!propsInitialized.get()) {
                synchronized (propsInitialized) {
                    if (!propsInitialized.get()) {
                        IIOPUtils iiopUtils = IIOPUtils.getInstance();
                        iiopServiceBean = iiopUtils.getIiopService();
                        try {

                            if (iiopServiceBean == null) {
                                // serverContext is null inside the ACC.
                                String initialPort = checkORBInitialPort(EMPTY_PROPERTIES);

                                return;
                            } else {
                                assert (iiopServiceBean != null);

                                iiopListenerBeans = (IiopListener[]) iiopServiceBean.getIiopListener().toArray(new IiopListener[0]);
                                assert (iiopListenerBeans != null && iiopListenerBeans.length > 0);

                                // checkORBInitialPort looks at iiopListenerBeans, if present
                                String initialPort = checkORBInitialPort(EMPTY_PROPERTIES);

                                orbBean = iiopServiceBean.getOrb();
                                assert (orbBean != null);

                                // Initialize IOR security config for non-EJB CORBA objects
                                //iiopServiceBean.isClientAuthenticationRequired()));
                                csiv2Props.put(ORB_CLIENT_AUTH_REQUIRED, String.valueOf(
                                        iiopServiceBean.getClientAuthenticationRequired()));
                                boolean corbaSSLRequired = true;
                                // If there is at least one non-SSL listener, then it means
                                // SSL is not required for CORBA objects.
                                for (int i = 0; i < iiopListenerBeans.length; i++) {
                                    if (iiopListenerBeans[i].getSsl() == null) {
                                        corbaSSLRequired = false;
                                        break;
                                    }
                                }

                                csiv2Props.put(ORB_SSL_SERVER_REQUIRED, String.valueOf(
                                        corbaSSLRequired));
                            }
                        } catch (NullPointerException npe) {
                            // REVISIT: Ignoring the NPE because the appclient container shares this code
                            logger.log(Level.FINE, "Server Context is NULL. Ignoring and proceeding.");
                        } finally {
                            // Whether this succeeds or not, do initProperties only once.
                            propsInitialized.set(true);
                        }
                    }
                }
            }
        } finally {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "GlassFishORBManager.initProperties<-: " + orb);
            }
        }
    }

    /**
     * Set ORB-related system properties that are required in case
     * user code in the app server or app client container creates a
     * new ORB instance.  The default result of calling
     * ORB.init( String[], Properties ) must be a fully usuable, consistent
     * ORB.  This avoids difficulties with having the ORB class set
     * to a different ORB than the RMI-IIOP delegates.
     */
    public static void setORBSystemProperties() {

        java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction() {
                    public java.lang.Object run() {
                        if (System.getProperty(OMG_ORB_CLASS_PROPERTY) == null) {
                            // set ORB based on JVM vendor
                            if (System.getProperty("java.vendor").equals("Sun Microsystems Inc.")) {
                                System.setProperty(OMG_ORB_CLASS_PROPERTY, ORB_SE_CLASS);
                            } else {
                                // if not Sun, then set to EE class
                                System.setProperty(OMG_ORB_CLASS_PROPERTY, ORB_CLASS);
                            }
                        }

                        if (System.getProperty(OMG_ORB_SINGLETON_CLASS_PROPERTY) == null) {
                            // set ORBSingleton based on JVM vendor
                            if (System.getProperty("java.vendor").equals("Sun Microsystems Inc.")) {
                                System.setProperty(OMG_ORB_SINGLETON_CLASS_PROPERTY, ORB_SE_SINGLETON_CLASS);
                            } else {
                                // if not Sun, then set to EE class
                                System.setProperty(OMG_ORB_SINGLETON_CLASS_PROPERTY, ORB_SINGLETON_CLASS);
                            }
                        }

                        System.setProperty(ORB_UTIL_CLASS_PROPERTY,
                                RMI_UTIL_CLASS);

                        System.setProperty(RMIIIOP_STUB_DELEGATE_CLASS_PROPERTY,
                                RMI_STUB_CLASS);

                        System.setProperty(RMIIIOP_PRO_DELEGATE_CLASS_PROPERTY,
                                RMI_PRO_CLASS);

                        return null;
                    }
                }
        );
    }

    /**
     * Set the ORB properties for IIOP failover and load balancing.
     */
    private static void setFOLBProperties(Properties orbInitProperties) {

        orbInitProperties.put(ORBConstants.RFM_PROPERTY, "dummy");
        orbInitProperties.put(SUN_ORB_SOCKET_FACTORY_CLASS_PROPERTY,
                IIOP_SSL_SOCKET_FACTORY_CLASS);

        // ClientGroupManager.
        // Registers itself as
        //   ORBInitializer (that registers ClientRequestInterceptor)
        //   IIOPPrimaryToContactInfo
        //   IORToSocketInfo
        orbInitProperties.setProperty(
                ORBConstants.USER_CONFIGURATOR_PREFIX
                        + "com.sun.corba.ee.impl.folb.ClientGroupManager",
                "dummy");

        /*TODO
        // This configurator registers the CSIv2SSLTaggedComponentHandler
        orbInitProperties.setProperty(
                ORBConstants.USER_CONFIGURATOR_PREFIX
                        + CSIv2SSLTaggedComponentHandlerImpl.class.getName(),
        */
        if (ASORBUtilities.isGMSAvailableAndClusterHeartbeatEnabled()) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "GMS available and enabled - doing EE initialization");
            }

            // Register ServerGroupManager.
            // Causes it to register itself as an ORBInitializer
            // that then registers it as
            // IOR and ServerRequest Interceptors.
            orbInitProperties.setProperty(
                    ORBConstants.USER_CONFIGURATOR_PREFIX
                            + "com.sun.corba.ee.impl.folb.ServerGroupManager",
                    "dummy");

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Did EE property initialization");
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Doing PE initialization");
            }

            /*TODO
            orbInitProperties.put(ORBConstants.PI_ORB_INITIALIZER_CLASS_PREFIX
                    + FailoverIORInterceptor.class.getName(), "dummy");
            */
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Did PE property initialization");
            }
        }
    }

    private static void initORB(Properties props) {
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, ".initORB->: ");
            }

            setORBSystemProperties();

            Properties orbInitProperties = new Properties();
            orbInitProperties.putAll(props);

            orbInitProperties.put(ORBConstants.APPSERVER_MODE, "true");

            // The main configurator.
            orbInitProperties.put(SUN_USER_CONFIGURATOR_PREFIX
                    + PEORB_CONFIG_CLASS, "dummy");

            setFOLBProperties(orbInitProperties);

            // Standard OMG Properties.
            orbInitProperties.put(ORBConstants.ORB_SERVER_ID_PROPERTY,
                    DEFAULT_SERVER_ID);
            orbInitProperties.put(OMG_ORB_CLASS_PROPERTY, ORB_CLASS);
            orbInitProperties.put(
                    PI_ORB_INITIALIZER_CLASS_PREFIX + J2EE_INITIALIZER, "");

            orbInitProperties.put(ORBConstants.ALLOW_LOCAL_OPTIMIZATION,
                    "true");

            orbInitProperties.put(ORBConstants.GET_SERVICE_CONTEXT_RETURNS_NULL, "true");

            orbInitProperties.put(SUN_ORB_ID_PROPERTY, S1AS_ORB_ID);
            orbInitProperties.put(ORBConstants.SHOW_INFO_MESSAGES, "true");

            // Do this even if propertiesInitialized, since props may override
            // ORBInitialHost and port.
            String initialPort = checkORBInitialPort(orbInitProperties);

            String orbInitialHost = checkORBInitialHost(orbInitProperties);
            String[] orbInitRefArgs;
            if (System.getProperty(IIOP_ENDPOINTS_PROPERTY) != null &&
                    !System.getProperty(IIOP_ENDPOINTS_PROPERTY).equals("")) {
                orbInitRefArgs = getORBInitRef(System.getProperty(IIOP_ENDPOINTS_PROPERTY));
            } else {
                // Add -ORBInitRef for INS to work
                orbInitRefArgs = getORBInitRef(orbInitialHost, initialPort);
            }
            checkAdditionalORBListeners(orbInitProperties);
            checkConnectionSettings(orbInitProperties);
            checkMessageFragmentSize(orbInitProperties);
            checkServerSSLOutboundSettings(orbInitProperties);
            checkForOrbPropertyValues(orbInitProperties);

            Collection<GlassFishORBLifeCycleListener> lcListeners =
                    iiopUtils.getGlassFishORBLifeCycleListeners();

            List<String> argsList = new ArrayList<String>();
            for (String a : orbInitRefArgs) {
                argsList.add(a);
            }
            for (GlassFishORBLifeCycleListener listener : lcListeners) {
                listener.initializeORBInitProperties(argsList, orbInitProperties);
            }

            String[] args = argsList.toArray(new String[argsList.size()]);

            // The following is done only on the Server Side to set the
            // ThreadPoolManager in the ORB. ThreadPoolManager on the server
            // is initialized based on configuration parameters found in
            // domain.xml. On the client side this is not done

            if (!iiopUtils.isAppClientContainer()) {
                PEORBConfigurator.setThreadPoolManager();
            }

            // orb MUST be set before calling getFVDCodeBaseIOR, or we can
            // recurse back into initORB due to interceptors that run
            // when the TOA supporting the FVD is created!
            // DO NOT MODIFY initORB to return ORB!!!

            /**
             * we can't create object adapters inside the ORB init path, or else we'll get this same problem
             * in slightly different ways. (address in use exception)
             * Having an IORInterceptor (TxSecIORInterceptor) get called during ORB init always results in a
             * nested ORB.init call because of the call to getORB in the IORInterceptor.
             */
            final ClassLoader prevCL = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(GlassFishORBManager.class.getClassLoader());
                orb = ORBFactory.create(args, orbInitProperties);
                System.out.println("***********************");
                System.out.println("*** ORB: " + orb + " ***");
                System.out.println("***********************");
            } finally {
                Thread.currentThread().setContextClassLoader(prevCL);
            }

            // Done to indicate this is a server and
            // needs to create listen ports.
            try {
                org.omg.CORBA.Object obj =
                        orb.resolve_initial_references("RootPOA");
            } catch (org.omg.CORBA.ORBPackage.InvalidName in) {
                logger.log(Level.SEVERE, "enterprise.orb_reference_exception", in);
            }

            // J2EEServer's persistent server port is same as ORBInitialPort.
            orbInitialPort = GlassFishORBManager.getORBInitialPort();

            for (GlassFishORBLifeCycleListener listener : lcListeners) {
                listener.orbCreated(orb);
            }

            //TODO: The following two statements can be moved to some GlassFishORBLifeCycleListeners
            rfm = (ReferenceFactoryManager) orb.resolve_initial_references(
                    ORBConstants.REFERENCE_FACTORY_MANAGER);

            ASORBUtilities.initGIS(orb);

            // SeeBeyond fix for 6325988: needs testing.
            // Still do not know why this might make any difference.
            // Invoke this for its side-effects: ignore returned IOR.
            ((com.sun.corba.ee.spi.orb.ORB) orb).getFVDCodeBaseIOR();

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "enterprise_util.excep_in_createorb", ex);
            throw new RuntimeException(ex);
        } finally {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, ".initORB<-: ");
            }
        }
    }

    private static String checkForAddrAny(Properties props, String orbInitialHost) {
        if ((orbInitialHost.equals("0.0.0.0")) || (orbInitialHost.equals("::"))
                || (orbInitialHost.equals("::ffff:0.0.0.0"))) {
            /* FIXME -DHIRU
               props.setProperty(SUN_LISTEN_ADDR_ANY_ADDRESS, orbInitialHost);
               props.put(PI_ORB_INITIALIZER_CLASS_PREFIX + ORB_IOR_ADDR_ANY_INITIALIZER, "");
           */
            try {
                String localAddress = java.net.InetAddress.getLocalHost().getHostAddress();
                return localAddress;
            } catch (java.net.UnknownHostException uhe) {
                logger.log(Level.WARNING, "Unknown host exception - Setting host to localhost");
                return DEFAULT_ORB_INIT_HOST;
            }
        } else {
            // Set com.sun.CORBA.ORBServerHost only if it's not one of "0.0.0.0",
            // "::" or "::ffff:0.0.0.0"
            props.setProperty(SUN_ORB_SERVER_HOST_PROPERTY, orbInitialHost);
            return orbInitialHost;
        }
    }

    private static String checkORBInitialHost(Properties props) {
        // Host setting in system properties always takes precedence.
        String orbInitialHost = System.getProperty(OMG_ORB_INIT_HOST_PROPERTY);
        if (orbInitialHost == null)
            orbInitialHost = props.getProperty(OMG_ORB_INIT_HOST_PROPERTY);
        if (orbInitialHost == null) {
            try {
                orbInitialHost = iiopListenerBeans[0].getAddress();
                orbInitialHost = checkForAddrAny(props, orbInitialHost);
            } catch (NullPointerException npe) {
                // REVISIT: Ignoring the NPE because the appclient container shares this code
                logger.log(Level.FINE, "IIOP listener element is null. Ignoring and proceeding.");
            }
        }
        if (orbInitialHost == null)
            orbInitialHost = DEFAULT_ORB_INIT_HOST;

        props.setProperty(OMG_ORB_INIT_HOST_PROPERTY, orbInitialHost);

        if (debug) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Setting orb initial host to " + orbInitialHost);
        }
        return orbInitialHost;
    }

    private static String checkORBInitialPort(Properties props) {
        // Port setting in system properties always takes precedence.
        String initialPort = System.getProperty(OMG_ORB_INIT_PORT_PROPERTY);
        if (initialPort == null)
            initialPort = props.getProperty(OMG_ORB_INIT_PORT_PROPERTY);

        if (initialPort == null) {
            try {
                initialPort = iiopListenerBeans[0].getPort();
                if (!Boolean.valueOf(iiopListenerBeans[0].getEnabled())) {
                    props.setProperty(ORB_DISABLED_PORTS_PROPERTY, initialPort);
                }
                // Set the default server port to equal the initial port.
                // This will be the port that is used both for object refs and
                // for the name service
                // REVISIT: For now setting this value only if we have a valid
                // server configuration. This is meant to circumvent a client-side
                // problem. The ACC uses the same GlassFishORBManager and hence ends up
                // creating a listener (yuk!) during root POA initialization (yuk!).
                // It is best to let this listener not come up on any fixed port.
                // Once this problem is fixed we can move this property setting
                // outside.
                if (!Boolean.valueOf(iiopListenerBeans[0].getEnabled())) {
                    // If the plain iiop listener is disabled do not create
                    // a listener on this port - bug 4927187
                    props.setProperty(SUN_ORB_SERVER_PORT_PROPERTY, "-1");
                } else {
                    props.setProperty(SUN_ORB_SERVER_PORT_PROPERTY, initialPort);
                }
            } catch (NullPointerException npe) {
                logger.log(Level.FINE, "IIOP listener element is null. Ignoring and proceeding.");
            }
        }

        if (initialPort == null)
            initialPort = DEFAULT_ORB_INIT_PORT;

        // Make sure we set initial port in System properties so that
        // any instantiations of com.sun.jndi.cosnaming.CNCtxFactory
        // use same port.
        props.setProperty(OMG_ORB_INIT_PORT_PROPERTY, initialPort);

        // Done to initialize the Persistent Server Port, before any
        // POAs are created. This was earlier done in POAEJBORB
        // Do it only in the appserver, not on appclient
        if (!iiopUtils.isAppClientContainer()) {
            props.setProperty(ORBConstants.PERSISTENT_SERVER_PORT_PROPERTY,
                    initialPort);
        }

        if (debug) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Setting orb initial port to " + initialPort);
        }

        orbInitialPort = new Integer(initialPort).intValue();

        return initialPort;
    }

    private static void checkAdditionalORBListeners(Properties props) {
        // REVISIT: Having to do the null check because this code is shared by the ACC
        if (iiopListenerBeans != null) {
            // This should be the only place we set additional ORB listeners.
            // So there is no need to check if the property is already set.
            StringBuffer listenSockets = new StringBuffer("");
            for (int i = 0; i < iiopListenerBeans.length; i++) {
                if (i == 0 && iiopListenerBeans[0].getSsl() == null) {
                    // Ignore first listener if its non-SSL, because it
                    // gets created by default using ORBInitialHost/Port.
                    continue;
                }

                if (Boolean.valueOf(iiopListenerBeans[i].getEnabled())) {
                    if (!Boolean.valueOf(iiopListenerBeans[i].getSecurityEnabled()) ||
                            iiopListenerBeans[i].getSsl() == null) {

                        checkForAddrAny(props, iiopListenerBeans[i].getAddress());
                        listenSockets.append((listenSockets.length() > 0 ? "," : "")
                                + IIOP_CLEAR_TEXT_CONNECTION
                                + ":" + iiopListenerBeans[i].getPort());
                    } else {
                        Ssl sslBean = null;
                        sslBean = iiopListenerBeans[i].getSsl();
                        assert sslBean != null;

                        // parse clientAuth
                        String type;
                        boolean clientAuth = Boolean.valueOf(sslBean.getClientAuthEnabled());
                        if (clientAuth)
                            type = SSL_MUTUALAUTH;
                        else
                            type = SSL;

                        // Ignoring cert alias etc.
                        listenSockets.append((listenSockets.length() > 0 ? "," : "")
                                + type + ":" + iiopListenerBeans[i].getPort());
                    }
                }
            }

            // Set the value both in the props object and in the system properties.
            props.setProperty(ORB_LISTEN_SOCKET_PROPERTY, listenSockets.toString());
        }

        return;
    }

    private static void checkConnectionSettings(Properties props) {
        if (orbBean != null) {
            String maxConnections;

            try {
                maxConnections = orbBean.getMaxConnections();

                // Validate number formats
                Integer.parseInt(maxConnections);
            } catch (NumberFormatException nfe) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, "enterprise_util.excep_orbmgr_numfmt", nfe);
                }

                maxConnections = DEFAULT_MAX_CONNECTIONS;
            }

            props.setProperty(SUN_MAX_CONNECTIONS_PROPERTY, maxConnections);
        }
        return;
    }

    private static void checkMessageFragmentSize(Properties props) {
        if (orbBean != null) {
            String fragmentSize, bufferSize;
            try {
                int fsize = ((Integer.parseInt(orbBean.getMessageFragmentSize().trim())) / 8) * 8;
                if (fsize < 32) {
                    fragmentSize = "32";
                    logger.log(Level.INFO, "Setting ORB Message Fragment size to " + fragmentSize);
                } else {
                    fragmentSize = String.valueOf(fsize);
                }
                bufferSize = fragmentSize;
            } catch (NumberFormatException nfe) {
                // Print stack trace and use default values
                logger.log(Level.WARNING, "enterprise_util.excep_in_reading_fragment_size", nfe);
                logger.log(Level.INFO, "Setting ORB Message Fragment size to Default " +
                        SUN_GIOP_DEFAULT_FRAGMENT_SIZE);
                fragmentSize = SUN_GIOP_DEFAULT_FRAGMENT_SIZE;
                bufferSize = SUN_GIOP_DEFAULT_BUFFER_SIZE;
            }
            props.setProperty(SUN_GIOP_FRAGMENT_SIZE_PROPERTY, fragmentSize);
            props.setProperty(SUN_GIOP_BUFFER_SIZE_PROPERTY, bufferSize);
        }
    }

    private static void checkServerSSLOutboundSettings(Properties props) {
        if (iiopServiceBean != null) {
            SslClientConfig sslClientConfigBean = iiopServiceBean.getSslClientConfig();
            if (sslClientConfigBean != null) {
                Ssl ssl = sslClientConfigBean.getSsl();
                assert (ssl != null);
            }
        }
    }

    private static void checkForOrbPropertyValues(Properties props) {
        if (orbBean != null) {
            List<Property> orbBeanProps = orbBean.getProperty();
            if (orbBeanProps != null) {
                for (int i = 0; i < orbBeanProps.size(); i++) {
                    props.setProperty(orbBeanProps.get(i).getName(), orbBeanProps.get(i).getValue());
                }
            }
        }
    }

    private static String[] getORBInitRef(String orbInitialHost,
                                          String initialPort) {
        // Add -ORBInitRef NameService=....
        // This ensures that INS will be used to talk with the NameService.
        String[] newArgs = new String[]{
                "-ORBInitRef",
                "NameService=corbaloc:iiop:1.2@"
                        + orbInitialHost + ":"
                        + initialPort + "/NameService"
        };

        return newArgs;
    }

    private static String[] getORBInitRef(String endpoints) {

        String[] list = (String[]) endpoints.split(",");
        String corbalocURL = getCorbalocURL(list);
        logger.fine("GlassFishORBManager.getORBInitRef = " + corbalocURL);

        // Add -ORBInitRef NameService=....
        // This ensures that INS will be used to talk with the NameService.
        String[] newArgs = new String[]{
                "-ORBInitRef",
                "NameService=corbaloc:" + corbalocURL + "/NameService"
        };

        return newArgs;
    }

    //FixMe: Move this to naming

    public static final String IIOP_ENDPOINTS_PROPERTY =
            "com.sun.appserv.iiop.endpoints";

    private static final String IIOP_URL = "iiop:1.2@";

    public static String getCorbalocURL(Object[] list) {

        String corbalocURL = "";
        //convert list into corbaloc url
        for (int i = 0; i < list.length; i++) {
            logger.info("list[i] ==> " + list[i]);
            if (corbalocURL.equals("")) {
                corbalocURL = IIOP_URL + ((String) list[i]).trim();
            } else {
                corbalocURL = corbalocURL + "," +
                        IIOP_URL + ((String) list[i]).trim();
            }
        }
        logger.info("corbaloc url ==> " + corbalocURL);
        return corbalocURL;
    }
}
