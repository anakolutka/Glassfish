
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

package com.sun.enterprise.iiop.security;

import java.lang.InheritableThreadLocal;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.net.InetAddress;
import java.util.List;
import java.util.ArrayList;

import java.security.PrivilegedAction;
import java.security.AccessController;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
// GSS Related Functionality
import org.ietf.jgss.*;

import com.sun.enterprise.Switch;
import com.sun.enterprise.ExecutionContext;
import com.sun.enterprise.InvocationManager;
import com.sun.enterprise.InvocationException;
import com.sun.enterprise.ComponentInvocation;
import com.sun.enterprise.appclient.AppContainer;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.EjbReferenceDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbIORConfigurationDescriptor;
import com.sun.enterprise.iiop.CSIV2TaggedComponentInfo;
import org.omg.CORBA.ORB;
import com.sun.enterprise.iiop.IORToSocketInfoImpl;
import com.sun.enterprise.iiop.POAProtocolMgr;
import com.sun.enterprise.security.SSLUtils;
import com.sun.enterprise.security.auth.login.PasswordCredential;
import com.sun.enterprise.security.auth.login.X509CertificateCredential;
import com.sun.enterprise.security.auth.LoginContextDriver;
import com.sun.enterprise.util.Utility;
import com.sun.enterprise.util.ORBManager;
import com.sun.enterprise.util.TypeUtil;

import com.sun.corba.ee.spi.ior.IOR;
import com.sun.corba.ee.spi.ior.iiop.IIOPAddress;
import com.sun.corba.ee.spi.ior.iiop.IIOPProfileTemplate;
import com.sun.corba.ee.spi.transport.SocketInfo;
import com.sun.corba.ee.org.omg.CSI.*;
import com.sun.corba.ee.org.omg.CSIIOP.*;

import sun.security.x509.X500Name;
import com.sun.enterprise.log.Log;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.logging.*;
import com.sun.logging.*;

/** 
 * This class is responsible for making various decisions for selecting
 * security information to be sent in the IIOP message based on target
 * configuration and client policies.
 * Note: This class can be called concurrently by multiple client threads.
 * However, none of its methods need to be synchronized because the methods
 * either do not modify state or are idempotent.
 * 
 * @author Vivek Nagar
 * @author Harpreet Singh
 * @author Shing Wai Chan
 */

public final class SecurityMechanismSelector {

    private static final java.util.logging.Logger _logger =
       LogDomains.getLogger(LogDomains.CORBA_LOGGER);

    public static final String CLIENT_CONNECTION_CONTEXT = "ClientConnContext";
    public static final String SERVER_CONNECTION_CONTEXT = "ServerConnContext";

    private static Set corbaIORDescSet = null;
    private static boolean sslRequired = false;

    // List of hosts trusted by the client for sending passwords to.
    // Also, list of hosts trusted by the server for accepting propagated
    // identities.
    //private static String[] serverTrustedHosts = null;

    private static final LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(SecServerRequestInterceptor.class);

    // A reference to POAProtocolMgr will be obtained dynamically
    // and set if not null. So set it to null here.
    private static POAProtocolMgr protocolMgr = null;

   
            

    /**
     * Read the client and server preferences from the config files.
     */
    static {
        try {
	  
	    // Initialize client security config
	    String s = 
		(String)(ORBManager.getCSIv2Props()).get(ORBManager.ORB_SSL_CLIENT_REQUIRED);
	    if ( s != null && s.equals("true") ) {
		sslRequired = true;
	    }

	    // initialize corbaIORDescSet with security config for CORBA objects
	    corbaIORDescSet = new HashSet();
	    EjbIORConfigurationDescriptor iorDesc = 
					    new EjbIORConfigurationDescriptor();
	    EjbIORConfigurationDescriptor iorDesc2 = 
					    new EjbIORConfigurationDescriptor();
	    String serverSslReqd = (String)(ORBManager.getCSIv2Props()).get(ORBManager.ORB_SSL_SERVER_REQUIRED);
	    if ( serverSslReqd != null && serverSslReqd.equals("true") ) {
		iorDesc.setIntegrity(EjbIORConfigurationDescriptor.REQUIRED);
		iorDesc.setConfidentiality(
					EjbIORConfigurationDescriptor.REQUIRED);
		iorDesc2.setIntegrity(EjbIORConfigurationDescriptor.REQUIRED);
		iorDesc2.setConfidentiality(
					EjbIORConfigurationDescriptor.REQUIRED);
	    }
	    String clientAuthReq = 
		(String)(ORBManager.getCSIv2Props()).get(ORBManager.ORB_CLIENT_AUTH_REQUIRED);
	    if ( clientAuthReq != null && clientAuthReq.equals("true") ) {
		// Need auth either by SSL or username-password.
		// This sets SSL clientauth to required.
		iorDesc.setEstablishTrustInClient(
					EjbIORConfigurationDescriptor.REQUIRED);
		// This sets username-password auth to required.
		iorDesc2.setAuthMethodRequired(true);
		corbaIORDescSet.add(iorDesc2);
	    }
	    corbaIORDescSet.add(iorDesc);

        } catch(Exception e) {
            _logger.log(Level.SEVERE,"iiop.Exception",e);
        }
    }

    private CompoundSecMech mechanism = null;
    private ORB orb = null;
    private CSIV2TaggedComponentInfo ctc = null;

    /**
     * Default constructor.
     */
    public SecurityMechanismSelector() {
        this.orb = ORBManager.getORB();
        this.ctc = new CSIV2TaggedComponentInfo(orb);

    }

    public static ServerConnectionContext getServerConnectionContext() {
        Hashtable h = ConnectionExecutionContext.getContext();
        ServerConnectionContext scc = 
            (ServerConnectionContext) h.get(SERVER_CONNECTION_CONTEXT);
        return scc;
    }

    public static void setServerConnectionContext(ServerConnectionContext scc) {
        Hashtable h = ConnectionExecutionContext.getContext();
        h.put(SERVER_CONNECTION_CONTEXT, scc);
    }

    public ConnectionContext getClientConnectionContext() {
        Hashtable h = ConnectionExecutionContext.getContext();
        ConnectionContext scc = 
            (ConnectionContext) h.get(CLIENT_CONNECTION_CONTEXT);
        return scc;
    }

    public void setClientConnectionContext(ConnectionContext scc) {
        Hashtable h = ConnectionExecutionContext.getContext();
        h.put(CLIENT_CONNECTION_CONTEXT, scc);
    }

    /**
     * This method determines if SSL should be used to connect to the
     * target based on client and target policies. It will return null if
     * SSL should not be used or an SocketInfo containing the SSL port
     * if SSL should be used.
     */
    public SocketInfo getSSLPort(IOR ior, ConnectionContext ctx) 
    {
        SocketInfo info = null;
        try {
            mechanism = selectSecurityMechanism(ior);
        } catch(SecurityMechanismException sme) {
            throw new RuntimeException(sme.getMessage());
        }
        ctx.setIOR(ior);
        ctx.setMechanism(mechanism);

        TLS_SEC_TRANS ssl = null;
        if ( mechanism != null ) {
            ssl = ctc.getSSLInformation(mechanism);
        }

        if (ssl == null) {
            if (sslRequired) {
                // Attempt to create SSL connection to host, ORBInitialPort
                IIOPProfileTemplate templ = (IIOPProfileTemplate)
                    ior.getProfile().getTaggedProfileTemplate();
                IIOPAddress addr = templ.getPrimaryAddress();
                info = IORToSocketInfoImpl.createSocketInfo(
		        "SecurityMechanismSelector1",
                        "SSL", addr.getHost(), ORBManager.getORBInitialPort());
                return info;
            } else {
                return null;
            }
        }

        int targetRequires = ssl.target_requires;
        int targetSupports = ssl.target_supports;
        
        /*
         * If target requires any of Integrity, Confidentiality or 
         * EstablishTrustInClient, then SSL is used.
         */
        if (isSet(targetRequires, Integrity.value) || 
                isSet(targetRequires, Confidentiality.value) ||
                isSet(targetRequires, EstablishTrustInClient.value)) {
            if (_logger.isLoggable(Level.FINE)) {
                 _logger.log(Level.FINE, "Target requires SSL");
            }
            ctx.setSSLUsed(true);
            String type = "SSL";
            if(isSet(targetRequires, EstablishTrustInClient.value)) {
                type = "SSL_MUTUALAUTH";
                ctx.setSSLClientAuthenticationOccurred(true);
            } 
            short sslport = ssl.addresses[0].port;
            int ssl_port = Utility.shortToInt(sslport);
            String host_name = ssl.addresses[0].host_name;
            
            info = IORToSocketInfoImpl.createSocketInfo(
		    "SecurityMechanismSelector2",
                    type, host_name, ssl_port);

            return info;
        } else if (isSet(targetSupports, Integrity.value) || 
                    isSet(targetSupports, Confidentiality.value) ||
                    isSet(targetSupports, EstablishTrustInClient.value)) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Target supports SSL");
            }

            if ( sslRequired ) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Client is configured to require SSL for the target");
                }

                ctx.setSSLUsed(true);
                short sslport = ssl.addresses[0].port;
                String host_name = ssl.addresses[0].host_name;
                int ssl_port = Utility.shortToInt(sslport);
                info = IORToSocketInfoImpl.createSocketInfo(
		        "SecurityMechanismSelector3",
                        "SSL", host_name, ssl_port);
                return info;
            } else {
                return null;
            }
        } else if ( sslRequired ) {
	    throw new RuntimeException("SSL required by client but not supported by server.");
	} else {
	    return null;
	}
    }

    /*
    public String[] getServerTrustedHosts() {
        return serverTrustedHosts;
    }
    
    public void setServerTrustedHosts(String[] val) {
        this.serverTrustedHosts = val;
    }
    */
    
    public CompoundSecMech getMechanism() {
        return mechanism;
    }
    
    public void setMechanism(CompoundSecMech val) {
        this.mechanism = val;
    }
    
    public ORB getOrb() {
        return orb;
    }
    
    public void setOrb(ORB val) {
        this.orb = val;
    }
    
    public CSIV2TaggedComponentInfo getCtc() {
        return ctc;
    }
    
    public void setCtc(CSIV2TaggedComponentInfo val) {
        this.ctc = val;
    }
    
    public java.util.List<SocketInfo> getSSLPorts(IOR ior, ConnectionContext ctx) 
    {
        try {
            mechanism = selectSecurityMechanism(ior);
        } catch(SecurityMechanismException sme) {
            throw new RuntimeException(sme.getMessage());
        }
        ctx.setIOR(ior);
        ctx.setMechanism(mechanism);

        TLS_SEC_TRANS ssl = null;
        if ( mechanism != null ) {
            ssl = ctc.getSSLInformation(mechanism);
        }

        if (ssl == null) {
            if (sslRequired) {
                // Attempt to create SSL connection to host, ORBInitialPort
                IIOPProfileTemplate templ = (IIOPProfileTemplate)
                    ior.getProfile().getTaggedProfileTemplate();
                IIOPAddress addr = templ.getPrimaryAddress();
                SocketInfo info = IORToSocketInfoImpl.createSocketInfo(
		        "SecurityMechanismSelector1",
                        "SSL", addr.getHost(), ORBManager.getORBInitialPort());
                //SocketInfo[] sInfos = new SocketInfo[]{info};
                List<SocketInfo> sInfos = new ArrayList<SocketInfo>();
                sInfos.add(info);
                return sInfos;
            } else {
                return null;
            }
        }

        int targetRequires = ssl.target_requires;
        int targetSupports = ssl.target_supports;
        
        /*
         * If target requires any of Integrity, Confidentiality or 
         * EstablishTrustInClient, then SSL is used.
         */
        if (isSet(targetRequires, Integrity.value) || 
                isSet(targetRequires, Confidentiality.value) ||
                isSet(targetRequires, EstablishTrustInClient.value)) {
            if (_logger.isLoggable(Level.FINE)) {
                 _logger.log(Level.FINE, "Target requires SSL");
            }
            ctx.setSSLUsed(true);
            String type = "SSL";
            if(isSet(targetRequires, EstablishTrustInClient.value)) {
                type = "SSL_MUTUALAUTH";
                ctx.setSSLClientAuthenticationOccurred(true);
            } 
            //SocketInfo[] socketInfos = new SocketInfo[ssl.addresses.size];
            List<SocketInfo> socketInfos = new ArrayList<SocketInfo>();
            for(int addressIndex =0; addressIndex < ssl.addresses.length; addressIndex++){
                short sslport = ssl.addresses[addressIndex].port;
                int ssl_port = Utility.shortToInt(sslport);
                String host_name = ssl.addresses[addressIndex].host_name;
            
                SocketInfo sInfo = IORToSocketInfoImpl.createSocketInfo(
		    "SecurityMechanismSelector2",
                    type, host_name, ssl_port);
                socketInfos.add(sInfo);
            }
            return socketInfos;
        } else if (isSet(targetSupports, Integrity.value) || 
                    isSet(targetSupports, Confidentiality.value) ||
                    isSet(targetSupports, EstablishTrustInClient.value)) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Target supports SSL");
            }

            if ( sslRequired ) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Client is configured to require SSL for the target");
                }

                ctx.setSSLUsed(true);
                //SocketInfo[] socketInfos = new SocketInfo[ssl.addresses.size];
                List<SocketInfo> socketInfos = new ArrayList<SocketInfo>();
                for(int addressIndex =0; addressIndex < ssl.addresses.length; addressIndex++){
                    short sslport = ssl.addresses[addressIndex].port;
                    int ssl_port = Utility.shortToInt(sslport);
                    String host_name = ssl.addresses[addressIndex].host_name;

                    SocketInfo sInfo = IORToSocketInfoImpl.createSocketInfo(
                        "SecurityMechanismSelector3",
                        "SSL", host_name, ssl_port);
                    socketInfos.add(sInfo);
                }
                return socketInfos;                
            } else {
                return null;
            }
        } else if ( sslRequired ) {
	    throw new RuntimeException("SSL required by client but not supported by server.");
	} else {
	    return null;
	}
    }


    /**
     * Select the security context to be used by the CSIV2 layer
     * based on whether the current component is an application 
     * client or a web/EJB component.
     */

    public SecurityContext selectSecurityContext(IOR ior)
        throws InvalidIdentityTokenException, 
            InvalidMechanismException, SecurityMechanismException
    {
        SecurityContext context = null;   

	ConnectionContext cc = new ConnectionContext();
        getSSLPort(ior, cc);
        setClientConnectionContext(cc); 


        mechanism = cc.getMechanism();
        if(mechanism == null) {
            return null;
        }
        boolean sslUsed = cc.getSSLUsed();
        boolean clientAuthOccurred = cc.getSSLClientAuthenticationOccurred();
        InvocationManager im = Switch.getSwitch().getInvocationManager();
        if(im == null) {
            // Standalone client
            context = getSecurityContextForAppClient(null, sslUsed,
clientAuthOccurred); 
            return context;
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "SSL used:" + sslUsed + " SSL Mutual auth:" + clientAuthOccurred);
        }

        ComponentInvocation ci = null;
        // BEGIN IASRI# 4646060
        ci = im.getCurrentInvocation();
        if (ci == null) {
        // END IASRI# 4646060
            return null;
        }
        Object obj = ci.getContainerContext();
        if(obj instanceof AppContainer) {
            context = getSecurityContextForAppClient(ci, sslUsed, clientAuthOccurred);
        } else {
            context = getSecurityContextForWebOrEJB(ci, sslUsed, clientAuthOccurred);
        }
        return context;
    }

    /**
     * Create the security context to be used by the CSIV2 layer
     * to marshal in the service context of the IIOP message from an appclient
     * or standalone client.
     * @return the security context.
     */
    public SecurityContext getSecurityContextForAppClient(
                                        ComponentInvocation ci, 
                                        boolean sslUsed,
                                        boolean clientAuthOccurred) 
        throws InvalidMechanismException, InvalidIdentityTokenException,
                                            SecurityMechanismException {

        return sendUsernameAndPassword(ci, sslUsed, clientAuthOccurred);
    }

    /**
     * Create the security context to be used by the CSIV2 layer
     * to marshal in the service context of the IIOP message from an web
     * component or EJB invoking another EJB.
     * @return the security context.
     */
    public SecurityContext getSecurityContextForWebOrEJB(
                        ComponentInvocation ci, boolean sslUsed,
                        boolean clientAuthOccurred) 
        throws InvalidMechanismException, InvalidIdentityTokenException, 
                            SecurityMechanismException {

        SecurityContext ctx = null;
        if(!sslUsed) {
	    ctx = propagateIdentity(false, ci);
	} else {
	    ctx = propagateIdentity(clientAuthOccurred, ci);
	}
	return ctx;
    }

    private boolean isMechanismSupported(SAS_ContextSec sas){
        byte[][] mechanisms = sas.supported_naming_mechanisms;
        byte[] mechSupported = Utility.getMechanism();

	if (mechSupported == null) {
	    return false;
	}
        if (mechanisms[0].length != mechSupported.length)
            return false;
        for (int i=0; i<mechanisms[0].length; i++){
            if (mechanisms[0][i] != mechSupported[i])
                return false;
        }
        return true;    
    }
    public boolean isIdentityTypeSupported(SAS_ContextSec sas){
        int ident_token = sas.supported_identity_types;
        // the identity token matches atleast one of the types we support
        int value = ident_token &
            CSIV2TaggedComponentInfo.SUPPORTED_IDENTITY_TOKEN_TYPES;
        if (value != 0)
            return true;
        else 
            return false;
    }

    /**
     * Get the security context to send username and password in the
     * service context.
     * @param whether username/password will be sent over plain IIOP or
     *        over IIOP/SSL.
     * @return the security context.
     * @exception SecurityMechanismException if there was an error.
     */
    private SecurityContext sendUsernameAndPassword(ComponentInvocation ci,
						    boolean sslUsed,
						    boolean clientAuthOccurred) 
                throws SecurityMechanismException {
        SecurityContext ctx = null;
        if(mechanism == null) {
            return null;
        }
        AS_ContextSec asContext = mechanism.as_context_mech;
        if( isSet(asContext.target_requires, EstablishTrustInClient.value)
            || ( isSet(mechanism.target_requires, EstablishTrustInClient.value)
		 && !clientAuthOccurred ) ) {

            ctx = getUsernameAndPassword(ci);

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Sending Username/Password");
            }
        } else {
            return null;
        }
        return ctx;
    }

    /**
     * Get the security context to propagate principal/distinguished name
     * in the service context.
     * @param clientAuth whether SSL client authentication has happened.
     * @return the security context.
     * @exception SecurityMechanismException if there was an error.
     */
    private SecurityContext propagateIdentity(boolean clientAuth,
                                              ComponentInvocation ci) 
        throws InvalidIdentityTokenException, InvalidMechanismException,
SecurityMechanismException {
            
        SecurityContext ctx = null;
        if(mechanism == null) {
            return null;
        }
        AS_ContextSec asContext = mechanism.as_context_mech;
        SAS_ContextSec sasContext = mechanism.sas_context_mech;
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "SAS CONTEXT's target_requires=" + sasContext.target_requires);
            _logger.log(Level.FINE, "SAS CONTEXT's target_supports=" + sasContext.target_supports);
        }

        if(isSet(asContext.target_requires, EstablishTrustInClient.value)) {
            ctx = getUsernameAndPassword(ci);
            if (ctx.authcls == null){ // run as mode cannot send password
                String errmsg =
localStrings.getLocalString("securitymechansimselector.runas_cannot_propagate_username_password",
"Cannot propagate username/password required by target when using run as identity");

            _logger.log(Level.SEVERE,"iiop.runas_error",errmsg);
            throw new SecurityMechanismException (errmsg);
            }
        } else if(isSet(sasContext.target_supports, IdentityAssertion.value) ||
                  isSet(sasContext.target_requires, IdentityAssertion.value)) {
            // called from the client side. thus before getting the identity. check the
            // mechanisms and the identity token supported
            if(!isIdentityTypeSupported(sasContext)){
                String errmsg =
                    localStrings.getLocalString("securitymechanismselector.invalid_identity_type",
                                                "The given identity token is unsupported.");
                throw new InvalidIdentityTokenException(errmsg);
            }
            if (sasContext.target_supports == IdentityAssertion.value){
                if(!isMechanismSupported(sasContext)){
                    String errmsg =
                        localStrings.getLocalString("securitymechanismselector.invalid_mechanism",
                                                    "The given mechanism type is unsupported.");
                _logger.log(Level.SEVERE,"iiop.unsupported_type_error",errmsg);
                throw new InvalidMechanismException(errmsg);
                }
            }

            // propagate principal/certificate/distinguished name
            ctx = getIdentity();
        } else if(isSet(asContext.target_supports, 
                        EstablishTrustInClient.value)) {
            if (clientAuth) {   // client auth done we can send password
                ctx = getUsernameAndPassword(ci);
                if (ctx.authcls == null) {
                    return null; // runas mode dont have username/password
                                //  dont really need to send it too
                }
            } else { // not sending anything for unauthenticated client
                return null;
            }
        } else{
            return null;        //  will never come here
        }
        return ctx;
    }


    /**
     * Return whether the server is trusted or not based on configuration
     * information.
     * @return true if the server is trusted.
     */
    /*
    private boolean isServerTrusted() {
        String star = "*";
        // first check if "*" in trusted - then why bother
        // doing all the processing . We trust everything
        // System.out.println (" In server trusted ??"); 
        for (int i = 0; i < serverTrustedHosts.length; i++){
            if (serverTrustedHosts[i].length () == 1) {
                if (serverTrustedHosts[i].equals (star))
                    return true;
            }
        }
        ConnectionContext scc = getClientConnectionContext ();
        if (scc != null){
            Socket skt = scc.getSocket ();
            InetAddress adr = skt.getInetAddress ();
            // System.out.println (" Calling isServerTrusted");
            // System.out.println (" addres "+ adr.toString ()); 
            return isDomainInTrustedList (adr, serverTrustedHosts);
        } 
        return false;

    }
    */

    /**
     * Checks if a given domain is trusted.
     * e.g. domain = 123.203.1.1 is an IP address
     * trusted list = *.com, *.eng
     * should say that the given domain is trusted.
     * @param the InetAddress of the domain to be checked for
     * @param the array of trusted domains
     * @return true - if the given domain is trusted
     */
    /*
    private boolean isDomainInTrustedList (InetAddress inetAddress,
                                           String[] trusted)
        throws SecurityException 
    {
        boolean isTrusted = false;
        String domain = null; 
        String star = "*";
        String dot = ".";
        // lookup and get domain name
        try{
            domain = inetAddress.getHostName ();
        } catch (Exception e){
            _logger.log(Level.SEVERE,"iiop.domain_lookup_failed",inetAddress.getHostAddress ());
            return false;
        }  
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, " Verifying if domain address ="+
                 inetAddress.toString () + " is in the Trusted list ");
            _logger.log(Level.FINE, " the domain name is = "+ domain);
        }

        String[] domainTok = TypeUtil.stringToArray (domain, dot);
        // now lets go through the list of trusted domains
        // one at a time
        for (int i=0; i< trusted.length; i++){
            // String to compare with
            String[] toksList = 
                TypeUtil.stringToArray (trusted[i], dot);
            // cannot compare *.eng to *.eng.sun
            if (toksList.length != domainTok.length){
                isTrusted = false;
                continue;
            } else{
                for (int j=toksList.length-1; j>=0 ; j--){
                    // compare com in *.eng.com and abc.eng.com
                    // compare in the reverse order
                    if (toksList[j].equals (domainTok[j])){
                        isTrusted = true;
                    } else {
                        // compare * in abc.*.com and abc.eng.com
                        if (toksList[j].equals (star)){
                            isTrusted = true;
                        }
                        else {
                            // get out and try the next domain
                            isTrusted = false;
                            break;
                        }
                    }
                }
                // We went through one domain and found a match 
                // no need to compare further
                if (isTrusted) 
                    return isTrusted;
            }
        }
        return isTrusted;
    }
    */


    /**
     * Get the username and password either from the JAAS subject or
     * from thread local storage. For appclients if login has'nt happened
     * this method would trigger login and popup a user interface to
     * gather authentication information.
     * @return the security context.
     */
    private SecurityContext getUsernameAndPassword(ComponentInvocation ci) 
            throws SecurityMechanismException {
        try {
            Subject s = null;
            if(ci == null) {
		// Standalone client ... Changed the security context 
		// from which to fetch the subject
                com.sun.enterprise.security.ClientSecurityContext sc = 
                    com.sun.enterprise.security.ClientSecurityContext.getCurrent();
                if(sc == null) {
                    return null;
                }
                s = sc.getSubject();
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "SUBJECT:" + s);
                }
            } else {
                Object obj = ci.getContainerContext();
                if(obj instanceof AppContainer) {
		    // get the subject
                    com.sun.enterprise.security.ClientSecurityContext sc = 
                        com.sun.enterprise.security.ClientSecurityContext.getCurrent();
                    if(sc == null) {
			s = LoginContextDriver.doClientLogin(
                                AppContainer.USERNAME_PASSWORD,
                                AppContainer.getCallbackHandler());
                    } else {
                        s = sc.getSubject();
                    }
                } else {
                    // web/ejb
                    s = getSubjectFromSecurityCurrent();
                    // TODO check if username/password is available
                    // if not throw exception
                }
            }
            SecurityContext ctx = new SecurityContext();
            final Subject sub = s;
            ctx.subject = s;
            // determining if run-as has been used
            Set privateCredSet = 
                (Set) AccessController.doPrivileged(new PrivilegedAction() {
                    public java.lang.Object run() {
                        return sub.getPrivateCredentials();
                    }
                });
            if (privateCredSet.isEmpty()) { // this is runas case dont set
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "no private credential run as mode");
                }
                ctx.authcls = null; // the auth class
                ctx.identcls = GSSUPName.class;
                
            } else{
                /** lookup the realm name that is required by the server
                 * and set it up in the PasswordCredential class.
                 */
                AS_ContextSec asContext = mechanism.as_context_mech;
                final byte[] target_name = asContext.target_name;
                byte[] _realm = GSSUtils.importName(GSSUtils.GSSUP_MECH_OID, target_name);
                final String realm_name = new String(_realm);
                final Iterator it = privateCredSet.iterator();
                for(;it.hasNext();){
                    AccessController.doPrivileged(new PrivilegedAction(){
                        public java.lang.Object run(){
                            PasswordCredential pc = (PasswordCredential) it.next();
                            pc.setRealm(realm_name);
                            return null;
                        }
                    });
                }
                ctx.authcls = PasswordCredential.class;
            }
            return ctx;
        } catch(Exception e) {
            _logger.log(Level.SEVERE,"iiop.user_password_exception",e);
            return null;
        }
    }

    /**
     * Get the principal/distinguished name from thread local storage.
     * @return the security context.
     */
    private SecurityContext getIdentity() 
            throws SecurityMechanismException {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Getting PRINCIPAL/DN from TLS");
        }

        SecurityContext ctx = new SecurityContext();
        final SecurityContext sCtx = ctx;
	// get stuff from the SecurityContext class
        com.sun.enterprise.security.SecurityContext scontext = 
            com.sun.enterprise.security.SecurityContext.getCurrent();
        if ((scontext == null) ||
             scontext.didServerGenerateCredentials()){  
            
            // a default guest/guest123 was created
            sCtx.identcls = AnonCredential.class;
            
            AccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run(){
                    // remove all the public and private credentials 
                    Subject sub = new Subject();
                    sCtx.subject = sub; 
                    sCtx.subject.getPublicCredentials().add(new AnonCredential());
                    return null;
                }
            });
            return sCtx;
        }

        Subject s = getSubjectFromSecurityCurrent();
        ctx.subject = s;

        // Figure out the credential class
        final Subject sub = s;
        Set credSet = 
            (Set) AccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                    return sub.getPrivateCredentials(PasswordCredential.class);
                }
            });
        if(credSet.size() == 1) {
            ctx.identcls = GSSUPName.class;
            final Set cs = credSet;
            Subject subj = (Subject) AccessController.doPrivileged(new
            PrivilegedAction() { 
                public java.lang.Object run() {    
                    Subject ss = new Subject();
                    Iterator iter = cs.iterator();
                    PasswordCredential pc = (PasswordCredential) iter.next();
                    GSSUPName gssname = new GSSUPName(pc.getUser(), pc.getRealm());
                    ss.getPublicCredentials().add(gssname);
                    return ss;
                }
            });
            ctx.subject = subj;
            return ctx;
        }

        credSet = s.getPublicCredentials();
        if(credSet.size() != 1) {
            _logger.log(Level.SEVERE,"iiop.principal_error");
            return null;
        } else {
            Iterator credIter = credSet.iterator();
            if(credIter.hasNext()) {
                Object o = credIter.next();
                if(o instanceof GSSUPName) {
                    ctx.identcls = GSSUPName.class;
                } else if(o instanceof X500Name) {
                    ctx.identcls = X500Name.class;
                } else {
                    ctx.identcls = X509CertificateCredential.class;
                }
            } else {
        	_logger.log(Level.SEVERE,"iiop.credential_error");
                return null;
            }
        }
        return ctx;
    }

    private Subject getSubjectFromSecurityCurrent() 
            throws SecurityMechanismException {
        com.sun.enterprise.security.SecurityContext sc = null;
        sc = com.sun.enterprise.security.SecurityContext.getCurrent();
        if(sc == null) {
            if(_logger.isLoggable(Level.FINE)) {
        	_logger.log(Level.FINE," SETTING GUEST ---");
            }
            sc = com.sun.enterprise.security.SecurityContext.init();
        }
        if(sc == null) {
            throw new SecurityMechanismException("Could not find " +
                        " security information");
        }
        Subject s = sc.getSubject();
        if(s == null) {
            throw new SecurityMechanismException("Could not find " +
                " subject information in the security context.");
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Subject in security current:" + s);
        }
        return s;
    }

    public CompoundSecMech selectSecurityMechanism(IOR ior) 
            throws SecurityMechanismException {
        CompoundSecMech[] mechList = ctc.getSecurityMechanisms(ior);
        CompoundSecMech mech = selectSecurityMechanism(mechList);
        return mech;
    }

    /**
     * Select the security mechanism from the list of compound security
     * mechanisms.
     */
    private CompoundSecMech selectSecurityMechanism(
                CompoundSecMech[] mechList) throws SecurityMechanismException {
        // We should choose from list of compound security mechanisms
        // which are in decreasing preference order. Right now we select
        // the first one.
        if(mechList == null || mechList.length == 0) {
            return null;
        }
        CompoundSecMech mech = null;
        for(int i = 0; i < mechList.length; i++) {
            mech = mechList[i];
            boolean useMech = useMechanism(mech);
            if(useMech) {
                return mech;
            }
        }
        throw new SecurityMechanismException("Cannot use any of the " +
            " target's supported mechanisms");
    }

    private boolean useMechanism(CompoundSecMech mech) {
        boolean val = true;
        TLS_SEC_TRANS tls = ctc.getSSLInformation(mech);

        if(tls == null) {
            return true;
        }
        int targetRequires = tls.target_requires;
        if(isSet(targetRequires, EstablishTrustInClient.value)) {
            if(! SSLUtils.isKeyAvailable()) {
                val = false;
            }
        }
        return val;
    }

    // Returns the target_name from PasswordCredential in Subject subj
    // subj must contain a single instance of PasswordCredential.

    private byte[] getTargetName(Subject subj)
    {
  
        byte[] tgt_name = {} ;

        final Subject sub = subj;
        final Set credset = 
            (Set) AccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                    return sub.getPrivateCredentials(PasswordCredential.class);
                }
            });
        if(credset.size() == 1) {
            tgt_name = (byte[]) AccessController.doPrivileged(new PrivilegedAction() { 
                public java.lang.Object run() {    
                    Iterator iter = credset.iterator();
                    PasswordCredential pc = (PasswordCredential) iter.next();
                    return pc.getTargetName();
                }
            });
        }
        return tgt_name;
    }

    private boolean evaluate_client_conformance_ssl(
                        EjbIORConfigurationDescriptor iordesc,
                        boolean  ssl_used,
                        X509Certificate[] certchain)
    {
      try {
        if(_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE,
			"SecurityMechanismSelector.evaluate_client_conformance_ssl->:");
	}
	
        boolean ssl_required  = false;
        boolean ssl_supported = false;
        int ssl_target_requires = 0;
        int ssl_target_supports = 0;

        /*************************************************************************
         * Conformance Matrix:
         *
         * |---------------|---------------------|---------------------|------------|
         * | SSLClientAuth | targetrequires.ETIC | targetSupports.ETIC | Conformant |
         * |---------------|---------------------|---------------------|------------|
         * |     Yes       |          0          |      1              |    Yes     |
         * |     Yes       |          0          |      0              |    No      |
         * |     Yes       |          1          |      X              |    Yes     |
         * |     No        |          0          |      X              |    Yes     |
         * |     No        |          1          |      X              |    No      |
         * |---------------|---------------------|---------------------|------------|
         *
         *************************************************************************/

        // gather the configured SSL security policies.
 
        ssl_target_requires = this.ctc.getTargetRequires(iordesc);
        ssl_target_supports = this.ctc.getTargetSupports(iordesc);

        if (    isSet(ssl_target_requires, Integrity.value)
             || isSet(ssl_target_requires, Confidentiality.value)
             || isSet(ssl_target_requires, EstablishTrustInClient.value))
            ssl_required = true;
        else
            ssl_required = false;

        if ( ssl_target_supports != 0)
            ssl_supported = true;
        else
            ssl_supported = false;

        /* Check for conformance for using SSL usage.
         * 
         * a. if SSL was used, then either the target must require or support
         *    SSL. In the latter case, SSL is used because of client policy.
         *
         * b. if SSL was not used, then the target must not require it either.
         *    The target may or may not support SSL (it is irrelevant).
         */
        if(_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE,
			"SecurityMechanismSelector.evaluate_client_conformance_ssl:"
			+ " " + isSet(ssl_target_requires, Integrity.value)
			+ " " + isSet(ssl_target_requires, Confidentiality.value)
			+ " " + isSet(ssl_target_requires, EstablishTrustInClient.value)
			+ " " + ssl_required
			+ " " + ssl_supported
			+ " " + ssl_used);
	}

        if (ssl_used) {
            if (! (ssl_required || ssl_supported))
                return false;  // security mechanism did not match
        } else {
            if (ssl_required)
                return false;  // security mechanism did not match
        }

        /* Check for conformance for SSL client authentication.
         *
         * a. if client performed SSL client authentication, then the target
         *    must either require or support SSL client authentication. If 
         *    the target only supports, SSL client authentication is used
         *    because of client security policy.
         *
         * b. if SSL client authentication was not used, then the target must
         *    not require SSL client authentcation either. The target may or may 
         *    not support SSL client authentication (it is irrelevant).
         */

        if(_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE,
			"SecurityMechanismSelector.evaluate_client_conformance_ssl:"
			+ " " + certchain
			+ " " + isSet(ssl_target_requires, EstablishTrustInClient.value)
			+ " " + isSet(ssl_target_supports, EstablishTrustInClient.value));
	}

        if (certchain != null) {
            if ( ! ( isSet(ssl_target_requires, EstablishTrustInClient.value)
                     || isSet(ssl_target_supports, EstablishTrustInClient.value)))
            return false; // security mechanism did not match
        } else {
            if (isSet(ssl_target_requires, EstablishTrustInClient.value))
                return false; // security mechanism did not match
        }

        if(_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE,
			"SecurityMechanismSelector.evaluate_client_conformance_ssl: true");
	}

        return true ; // mechanism matched
      } finally {
	  if(_logger.isLoggable(Level.FINE)) {
	      _logger.log(Level.FINE,
			  "SecurityMechanismSelector.evaluate_client_conformance_ssl<-:");
	  }
      }
    }

   
   /* Evaluates a client's conformance to a security policies
    * at the client authentication layer.
    *
    * returns true if conformant ; else returns false
    */
    private boolean evaluate_client_conformance_ascontext(
                        SecurityContext ctx,
                        EjbIORConfigurationDescriptor iordesc)
    {

        boolean client_authenticated = false;

        // get requirements and supports at the client authentication layer
        AS_ContextSec ascontext = null;
        try {
            ascontext = this.ctc.createASContextSec(iordesc);
        } catch (Exception e) {
            _logger.log(Level.SEVERE, "iiop.createcontextsec_exception",e);

            return false;
        }
   
        // ascontext should never be null - perhaps an exception should be raised.
        // for now return false;
        if (ascontext == null)
            return false;
            
        /*************************************************************************
         * Conformance Matrix:
         *
         * |------------|---------------------|---------------------|------------|
         * | ClientAuth | targetrequires.ETIC | targetSupports.ETIC | Conformant |
         * |------------|---------------------|---------------------|------------|
         * |     Yes    |          0          |      1              |    Yes     |
         * |     Yes    |          0          |      0              |    No      |
         * |     Yes    |          1          |      X              |    Yes     |
         * |     No     |          0          |      X              |    Yes     |
         * |     No     |          1          |      X              |    No      |
         * |------------|---------------------|---------------------|------------|
         *
         * Abbreviations: ETIC - EstablishTrusInClient
         * 
         *************************************************************************/

        if  ( (ctx != null) && (ctx.authcls != null) &&  (ctx.subject != null))
            client_authenticated = true;
        else
            client_authenticated = false;

        if (client_authenticated) {
            if ( ! ( isSet(ascontext.target_requires, EstablishTrustInClient.value)
                     || isSet(ascontext.target_supports, EstablishTrustInClient.value))){
            return false; // non conforming client
            }
            // match the target_name from client with the target_name in policy 

            byte [] client_tgtname = getTargetName(ctx.subject);

            if (ascontext.target_name.length != client_tgtname.length){
                return false; // mechanism did not match.
            }            
            for (int i=0; i < ascontext.target_name.length ; i ++)
                if (ascontext.target_name[i] != client_tgtname[i]){
                    return false; // mechanism did not match
                }
        } else { 
            if ( isSet(ascontext.target_requires, EstablishTrustInClient.value)){
                return false;  // no mechanism match.
            }
        }
        return true;
    }

   /* Evaluates a client's conformance to a security policy
    * at the sas context layer. The security policy
    * is derived from the EjbIORConfigurationDescriptor.
    *
    * returns true if conformant ; else returns false
    */
    private boolean evaluate_client_conformance_sascontext(
                        SecurityContext ctx,
                        EjbIORConfigurationDescriptor iordesc)
    {

        boolean caller_propagated = false;

        // get requirements and supports at the sas context layer
        SAS_ContextSec sascontext = null;
        try {
            sascontext = this.ctc.createSASContextSec(iordesc);
        } catch (Exception e) {
            _logger.log(Level.SEVERE,"iiop.createcontextsec_exception",e);
            return false;
        }
   
        if (sascontext == null)  // this should never be null
            return false;
            
        if  ( (ctx != null) && (ctx.identcls != null) &&  (ctx.subject != null))
            caller_propagated = true;
        else
            caller_propagated = false;

        if (caller_propagated) {
            if ( ! isSet(sascontext.target_supports, IdentityAssertion.value))
                return false; // target does not support IdentityAssertion

            /* There is no need further checking here since SecServerRequestInterceptor
             * code filters out the following:
             * a. IdentityAssertions of types other than those required by level 0 
             *    (for e.g. IdentityExtension)
             * b. unsupported identity types.
             * 
             * The checks are done in SecServerRequestInterceptor rather than here
             * to minimize code changes.
             */
            return true;
        }
        return true; //  either caller was not propagated or mechanism matched.
    }
       
                                                    

    /**
     * Evaluates a client's conformance to the security policies configured
     * on the target.
     * Returns true if conformant to the security policies
     * otherwise return false.
     *
     * Conformance checking is done as follows:
     * First, the object_id is mapped to the set of EjbIORConfigurationDescriptor.
     * Each EjbIORConfigurationDescriptor corresponds to a single CompoundSecMechanism
     * of the CSIv2 spec. A client is considered to be conformant if a
CompoundSecMechanism
     * consistent with the client's actions is found i.e. transport_mech,
as_context_mech
     * and sas_context_mech must all be consistent.
     * 
     */
    private boolean evaluate_client_conformance(SecurityContext   ctx,
                                                byte[]            object_id,
                                                boolean           ssl_used,
                                                X509Certificate[] certchain)
    {
        // Obtain the IOR configuration descriptors for the Ejb using
        // the object_id within the SecurityContext field.

        // if object_id is null then nothing to evaluate. This is a sanity
        // check - for the object_id should never be null.
        
        if (object_id == null)
            return true;

        if (protocolMgr == null)
            protocolMgr = (POAProtocolMgr)Switch.getSwitch().getProtocolManager();

        // Check to make sure protocolMgr is not null. 
        // This could happen during server initialization or if this call
        // is on a callback object in the client VM. 
        if (protocolMgr == null)
            return true;

        EjbDescriptor ejbDesc = protocolMgr.getEjbDescriptor(object_id);

        Set iorDescSet = null;
        if (ejbDesc != null) {
	    iorDescSet = ejbDesc.getIORConfigurationDescriptors();
	}
	else {
	    // Probably a non-EJB CORBA object.
	    // Create a temporary EjbIORConfigurationDescriptor.
	    iorDescSet = corbaIORDescSet;
	}

	if(_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE,
			"SecurityMechanismSelector.evaluate_client_conformance: iorDescSet: " + iorDescSet);
	}

        /* if there are no IORConfigurationDescriptors configured, then
         * no security policy is configured. So consider the client 
         * to be conformant.
         */
        if (iorDescSet.isEmpty())
            return true;

        // go through each EjbIORConfigurationDescriptor trying to find
        // a find a CompoundSecMechanism that matches client's actions.
        boolean checkSkipped = false;
        for (Iterator itr = iorDescSet.iterator(); itr.hasNext();) {
            EjbIORConfigurationDescriptor iorDesc = 
                (EjbIORConfigurationDescriptor) itr.next();
            if(skip_client_conformance(iorDesc)){
		if(_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE,
				"SecurityMechanismSelector.evaluate_client_conformance: skip_client_conformance");
		}
                checkSkipped = true;
                continue;
            }
            if (! evaluate_client_conformance_ssl(iorDesc, ssl_used, certchain)){
		if(_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE,
				"SecurityMechanismSelector.evaluate_client_conformance: evaluate_client_conformance_ssl");
		}
                checkSkipped = false;
                continue;
            }
            if ( ! evaluate_client_conformance_ascontext(ctx, iorDesc)){
		if(_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE,
				"SecurityMechanismSelector.evaluate_client_conformance: evaluate_client_conformance_ascontext");
		}
                checkSkipped = false;
                continue;
            }
            if  ( ! evaluate_client_conformance_sascontext(ctx, iorDesc)){
		if(_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE,
				"SecurityMechanismSelector.evaluate_client_conformance: evaluate_client_conformance_sascontext");
		}
               checkSkipped = false;
               continue;
            }
            return true;  // security policy matched.
        }
        if(checkSkipped)
            return true;
        return false; // No matching security policy found
    }     
        
     /** if ejb requires no security - then skip checking the client-conformance
     */    
    private boolean skip_client_conformance (EjbIORConfigurationDescriptor ior){
        String none = ior.NONE;        
	// sanity check
	if(ior == null){
	   return false;
	}
        // SSL is required and/or supported either
        if(!none.equalsIgnoreCase(ior.getIntegrity())){
            return false;
        }
        if(!none.equalsIgnoreCase(ior.getConfidentiality())){
            return false;
        }
        if(!none.equalsIgnoreCase(ior.getEstablishTrustInClient())){
            return false;
        }
        // Username password is required
        if(ior.isAuthMethodRequired()){
            return false;
        }
        // caller propagation is supported
        if(!none.equalsIgnoreCase(ior.getCallerPropagation())){
            return false;
        }
        return true;
    }
    /**
     * Called by the target to interpret client credentials after validation. 
     */
    public SecurityContext evaluateTrust(SecurityContext ctx, byte[] object_id)
        throws SecurityMechanismException
    {
        SecurityContext ssc = null;
        Socket socket = null;

        // ssl_used is true if SSL was used.        
        boolean ssl_used        = false ;

        // X509 Certificicate chain is non null if client has authenticated at
        // the SSL level.

        X509Certificate[] certChain = null ;

        // First gather all the information and then check the 
        // conformance of the client to the security policies.
        // If the test for client conformance passes, then set the
        // security context.

        ServerConnectionContext scc = getServerConnectionContext();

        if(scc != null) {
            socket = scc.getSocket();
            if ((socket != null) && (socket instanceof SSLSocket)) {
                ssl_used = true; // SSL was used
                // checkif there is a transport principal
                SSLSocket sslSock = (SSLSocket) socket;
                SSLSession sslSession = sslSock.getSession();
                try {
                    certChain = (X509Certificate[])sslSession.getPeerCertificates();
                } catch(Exception e) {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "iiop.cannot_get_peercert", e);
                    }
                }
            }
        }

        // For a local invocation - we don't need to check the security
        // policies. The following condition guarantees the call is local
        // and thus bypassing policy checks.
        if (socket == null && ctx == null)
            return null;

        if ( evaluate_client_conformance(ctx, object_id, ssl_used, certChain) 
                                                                     == false) {
            String msg = "Trust evaluation failed because ";
            msg = msg + "client does not conform to configured security policies";
            throw new SecurityMechanismException(msg);
        }            
 
        if ( ctx == null ) {
            if ( socket == null || !ssl_used || certChain == null )  {
                // Transport info is null and security context is null.
                // No need to set the anonymous credential here,
                // it will get set if any security operations 
                // (e.g. getCallerPrincipal) are done.
                // Note: if the target object is not an EJB, 
                // no security ctx is needed.
                return null;
            }  else {
                // Set the transport principal in subject and
                // return the X500Name class
                ssc = new SecurityContext();
                X500Name x500Name = (X500Name) certChain[0].getSubjectDN();
                ssc.subject = new Subject();
                ssc.subject.getPublicCredentials().add(x500Name);
                ssc.identcls = X500Name.class;
                ssc.authcls = null;
                return ssc;
            }
        } else {
            ssc = ctx;
        }

        Class authCls = ctx.authcls;
        Class identCls = ctx.identcls;
        ssc.subject = ctx.subject;

        ssc.authcls = null;
        ssc.identcls = null;

        if (identCls != null)
            ssc.identcls = identCls;
        else if (authCls != null)
            ssc.authcls = authCls;
        else
            ssc.identcls = AnonCredential.class;

        return ssc;
    }

    private boolean isSet(int val1, int val2) {
        if((val1 & val2) == val2) {
            return true;
        }
        return false;
    }
}


/**
 * This class that implements ConnectionExecutionContext that gets 
 * stored in Thread Local Storage. If the current thread creates
 * child threads, the context info that is  stored in the current 
 * thread is automatically propogated to the child threads.
 * 
 * Two class methods serve as a convinient way to set/get the 
 * Context information within the current thread.   
 *
 * Thread Local Storage is a concept introduced in JDK1.2. So, it
 * will not work on earlier releases of JDK.
 *
 * @see java.lang.ThreadLocal
 * @see java.lang.InheritableThreadLocal
 * 
 */
class ConnectionExecutionContext {
    private static final InheritableThreadLocal connCurrent= new InheritableThreadLocal();

    /** 
     * This method can be used to add a new hashtable for storing the 
     * Thread specific context information. This method is useful to add a 
     * deserialized Context information that arrived over the wire.
     * @param A hashtable that stores the current thread's context
     * information.
     */
    public static void setContext(Hashtable ctxTable) {
        if (ctxTable != null) {
            connCurrent.set(ctxTable);
        } else {
            connCurrent.set(new Hashtable());
        }
    }

    /**
     * This method returns the hashtable that stores the thread specific
     * Context information.
     * @return The Context object stored in the current TLS. It always 
     * returns a non null value;
     */
    public static Hashtable getContext() {
         if (connCurrent.get() == null) {
             setContext(null); // Create a new one...
         } 
         return (Hashtable) connCurrent.get();
    }
}

