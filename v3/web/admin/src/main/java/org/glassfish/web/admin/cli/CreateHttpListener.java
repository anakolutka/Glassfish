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
package org.glassfish.web.admin.cli;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.Properties;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.grizzly.config.dom.Http;
import com.sun.grizzly.config.dom.NetworkConfig;
import com.sun.grizzly.config.dom.NetworkListener;
import com.sun.grizzly.config.dom.NetworkListeners;
import com.sun.grizzly.config.dom.Protocol;
import com.sun.grizzly.config.dom.Protocols;
import com.sun.grizzly.config.dom.ThreadPool;
import com.sun.grizzly.config.dom.Transport;
import com.sun.grizzly.config.dom.Transports;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Create Http Listener Command
 */
@Service(name = "create-http-listener")
@Scoped(PerLookup.class)
@I18n("create.http.listener")
public class CreateHttpListener implements AdminCommand {
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateHttpListener.class);

    @Param(name = "listeneraddress")
    String listenerAddress;

    @Param(name = "listenerport")
    String listenerPort;

    @Param(name = "defaultvs")
    String defaultVirtualServer;

    @Param(name = "servername", optional = true)
    String serverName;

    @Param(name = "acceptorthreads", optional = true)
    String acceptorThreads;

    @Param(name = "xpowered", optional = true, defaultValue = "true")
    Boolean xPoweredBy;

    @Param(name = "redirectport", optional = true)
    String redirectPort;

    @Param(name = "externalport", optional = true)
    String externalPort;

    @Param(name = "securityenabled", optional = true, defaultValue = "false")
    Boolean securityEnabled;

    @Param(optional = true, defaultValue = "true")
    Boolean enabled;

    @Param(optional = true, defaultValue = "false")
    Boolean secure; //FIXME

    @Param(name = "family", optional = true)
    String family;

    @Param(name = "blockingenabled", optional = true, defaultValue = "false")
    Boolean blockingEnabled;

    @Param(name = "property", optional = true)
    Properties properties;

    @Param(name = "listener_id", primary = true)
    String listenerId;

    @Inject
    Configs configs;

    /**
     * Executes the command with the command parameters passed as Properties where the keys are the paramter names and
     * the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        List<Config> configList = configs.getConfig();
        Config config = configList.get(0);
        NetworkConfig networkConfig = config.getNetworkConfig();
        HttpService httpService = config.getHttpService();
        // ensure we don't already have one of this name
        for (NetworkListener listener : networkConfig.getNetworkListeners().getNetworkListener()) {
            if (listener.getName().equals(listenerId)) {
                report.setMessage(localStrings.getLocalString("create.http.listener.duplicate",
                    "Http Listener named {0} already exists.", listenerId));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        //check port uniqueness, only for same address
        for (NetworkListener listener : networkConfig.getNetworkListeners()
            .getNetworkListener()) {
            if (listener.getPort().trim().equals(listenerPort) &&
                listener.getAddress().trim().equals(listenerAddress)) {
                String def = "Port is already taken by another listener, choose another port.";
                String msg = localStrings
                    .getLocalString("port.occupied", def, listenerPort, listener.getName(), listenerAddress);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        //no need to check the other things (e.g. id) for uniqueness
        // ensure that the specified default virtual server exists
        if (!defaultVirtualServerExists(httpService)) {
            report.setMessage(localStrings.getLocalString("create.http.listener.vs.notexists",
                "Virtual Server, {0} doesn't exist", defaultVirtualServer));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        VirtualServer vs = httpService.getVirtualServerByName(defaultVirtualServer);
        try {
            ConfigSupport.apply(new SingleConfigCode<NetworkConfig>() {
                public Object run(final NetworkConfig configParam) throws TransactionFailure {
                    return ConfigSupport.apply(new SingleConfigCode<NetworkListeners>() {
                        public Object run(NetworkListeners listenersParam)
                            throws TransactionFailure, PropertyVetoException {
                            final NetworkListener newListener = listenersParam.createChild(NetworkListener.class);
                            newListener.setName(listenerId);
                            newListener.setAddress(listenerAddress);
                            newListener.setPort(listenerPort);
                            createOrSetTransport(configParam, newListener);
                            createProtocol(configParam, newListener);
                            setThreadPool(configParam, newListener);
                            //add properties
/*
                            if (properties != null) {
                                for (Map.Entry entry : properties.entrySet()) {
                                    Property property = newListener.createChild(Property.class);
                                    property.setName((String) entry.getKey());
                                    property.setValue((String) entry.getValue());
                                    newListener.getProperty().add(property);
                                }
                            }
*/
                            listenersParam.getNetworkListener().add(newListener);
                            return newListener;
                        }
                    }, configParam.getNetworkListeners());
                }
            }, networkConfig);
            //now change the associated virtual server
            ConfigSupport.apply(new SingleConfigCode<VirtualServer>() {
                public Object run(VirtualServer avs) throws PropertyVetoException {
                    String DELIM = ",";
                    String lss = avs.getNetworkListeners();
                    boolean listenerShouldBeAdded = true;
                    if (lss == null || lss.length() == 0) {
                        lss = listenerId; //the only listener in the list
                    } else if (!lss.contains(listenerId)) { //listener does not already exist
                        if (!lss.endsWith(DELIM)) {
                            lss += DELIM;
                        }
                        lss += listenerId;
                    } else { //listener already exists in the list, do nothing
                        listenerShouldBeAdded = false;
                    }
                    if (listenerShouldBeAdded) {
                        avs.setNetworkListeners(lss);
                    }
                    return avs;
                }
            }, vs);

        }
        catch (
            TransactionFailure e
            ) {
            String actual = e.getMessage();
            String def = "Creation of: " + listenerId + "failed because of: " + actual;
            String msg = localStrings.getLocalString("create.http.listener.fail", def, listenerId, actual);
            report.setMessage(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private void setThreadPool(NetworkConfig config, NetworkListener newListener) {
        final List<ThreadPool> pools = config.getParent(Config.class).getThreadPools().getThreadPool();
        for (ThreadPool pool : pools) {
            if ("http-thread-pool".equals(pool.getName())) {
                newListener.setThreadPool(pool.getName());
            }
        }
        if (newListener.getThreadPool() == null && !pools.isEmpty()) {
            newListener.setThreadPool(pools.get(0).getName());
        }
    }

    private Transport createOrSetTransport(NetworkConfig config, NetworkListener listener)
        throws TransactionFailure {
        final Transports transports = config.getTransports();
        Transport transport = null;
        for (Transport item : transports.getTransport()) {
            if ("tcp".equals(item.getName())) {
                listener.setTransport(item.getName());
                transport = item;
            }
        }
        if (transport == null) {
            transport = (Transport) ConfigSupport.apply(new SingleConfigCode<Transports>() {
                public Object run(Transports param) throws TransactionFailure {
                    Transport newTransport = param.createChild(Transport.class);
                    newTransport.setName(listenerId);
                    newTransport.setAcceptorThreads(acceptorThreads);
                    param.getTransport().add(newTransport);
                    return newTransport;
                }
            }, config.getTransports());
            listener.setTransport(transport.getName());
        }
        return transport;
    }

    private Protocol createProtocol(NetworkConfig config, final NetworkListener listener)
        throws TransactionFailure {
        return (Protocol) ConfigSupport.apply(new SingleConfigCode<Protocols>() {
            public Object run(Protocols param) throws TransactionFailure {
                final Protocol protocol = param.createChild(Protocol.class);
                protocol.setSecurityEnabled(securityEnabled.toString());
                protocol.setName(listenerId);
                param.getProtocol().add(protocol);
                listener.setProtocol(protocol.getName());
                final Http http = protocol.createChild(Http.class);
                http.setDefaultVirtualServer(defaultVirtualServer);
                //listener.setRedirectPort(redirectPort) FIXME: Applicable only in case of cluster or enterprise profile
                http.setXpoweredBy(xPoweredBy.toString());
                //listener.Ssl(ssl); FIXME
                listener.setEnabled(enabled.toString());
                http.setServerName(serverName);
                protocol.setHttp(http);
                return protocol;
            }
        }, config.getProtocols());
    }

    private boolean defaultVirtualServerExists(HttpService httpService) {
        if (defaultVirtualServer == null) {
            return false;
        }
        List<VirtualServer> list = httpService.getVirtualServer();
        for (VirtualServer vs : list) {
            String currId = vs.getId();
            if (currId != null && currId.equals(defaultVirtualServer)) {
                return true;
            }
        }
        return false;
    }
}
