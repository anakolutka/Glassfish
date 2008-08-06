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

package com.sun.enterprise.security.web.integration;

import com.sun.enterprise.security.web.integration.WebPrincipal;
import org.glassfish.internal.api.ServerContext;
import java.security.*;
import java.util.Set;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Collections;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.security.jacc.*;

import java.util.logging.*; 
import java.util.HashMap;

import com.sun.logging.LogDomains;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.security.common.AppservAccessController;
import com.sun.enterprise.security.CachedPermission;
import com.sun.enterprise.security.CachedPermissionImpl;
import com.sun.enterprise.security.PermissionCache;
import com.sun.enterprise.security.PermissionCacheFactory;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.audit.AuditManager;
import com.sun.enterprise.security.authorize.PolicyContextHandlerImpl;
//V3:Commented import com.sun.enterprise.web.VirtualServer;
import com.sun.enterprise.deployment.runtime.common.SecurityRoleMapping;
import org.glassfish.security.common.PrincipalImpl;
import org.glassfish.security.common.Group;
import com.sun.enterprise.config.serverbeans.*;
//V3:Commented import com.sun.enterprise.server.ApplicationServer;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.interfaces.SecurityRoleMapperFactory;
//import org.apache.catalina.Globals;
import com.sun.enterprise.security.SecurityServicesUtil;
import org.glassfish.internal.api.Globals;

/**
 * The class implements the JSR 115 - JavaTM Authorization Contract for Containers.
 * This class is a companion class of EJBSecurityManager.
 *
 * All the security decisions required to allow access to a resource are defined 
 * in that class.
 *
 * @author Jean-Francois Arcand
 * @author Harpreet Singh.
 * @todo introduce a new class called AbstractSecurityManager. Move functionality
 * from this class and EJBSecurityManager class and extend this class from 
 * AbstractSecurityManager
 */
public class WebSecurityManager {
    private static Logger logger = 
    Logger.getLogger(LogDomains.SECURITY_LOGGER);

    /**
     * Request path. Copied from org.apache.catalina.Globals;
     * Required to break dependence on WebTier of Security Module
     */
    public static final String CONSTRAINT_URI =
        "org.apache.catalina.CONSTRAINT_URI";
    
    private static final String RESOURCE = "hasResourcePermission";
    private static final String USERDATA = "hasUserDataPermission";
    private static final String ROLEREF = "hasRoleRefPermission";

    private static final String DEFAULT_PATTERN = "/";
    private static final String EMPTY_STRING = "";
 
    private static final PolicyContextHandlerImpl pcHandlerImpl =
            (PolicyContextHandlerImpl)PolicyContextHandlerImpl.getInstance();
    
    private static final Map ADMIN_PRINCIPAL = new HashMap();
    private static final Map ADMIN_GROUP = new HashMap();

    // The context ID associated with this instance. This is the name
    // of the application
    private  String CONTEXT_ID = null;
    private String CODEBASE = null;
    
    // The JACC policy provider.
    protected Policy policy = Policy.getPolicy();

    protected PolicyConfiguration policyConfiguration  = null;

    // if not available in the habitat, delegate to JDK's system-wide factory
    protected PolicyConfigurationFactory policyConfigurationFactory = null;
    protected CodeSource codesource = null;

    // protection domain cache
    private Map protectionDomainCache = 
        Collections.synchronizedMap(new WeakHashMap());

    private static WebResourcePermission allResources =
    new WebResourcePermission("/*",(String) null);

    private static WebUserDataPermission allConnections = 
    new WebUserDataPermission("/*",null);

    private static Permission[] protoPerms = {
        allResources,
	allConnections
    };

    // permissions tied to unchecked permission cache, and used
    // to determine if the effective policy is grant all
    // WebUserData and WebResource permisions.
    private CachedPermission allResourcesCP = null;
     
    private CachedPermission allConnectionsCP = null;
 
    // unchecked permission cache
    private PermissionCache uncheckedPermissionCache = null; 
    
    private static Set defaultPrincipalSet = 
	SecurityContext.getDefaultSecurityContext().getPrincipalSet();

    SecurityRoleMapperFactory factory;

    private ServerContext serverContext = null;
    // WebBundledescriptor
    private WebBundleDescriptor wbd = null;
    //TODO:V3 Copied from VirtualServer.java to avoid dependency on web-container module
    public static final String ADMIN_VS = "__asadmin";
    // Create a WebSecurityObject
    public WebSecurityManager(WebBundleDescriptor wbd) throws PolicyContextException {
        this(wbd,null);
    }
    
    public WebSecurityManager(WebBundleDescriptor wbd, ServerContext svc) throws PolicyContextException {
        this.wbd = wbd;
        this.CONTEXT_ID = getContextID(wbd);
        this.serverContext = svc;
        initialise();
        postConstruct();
    }

    private void postConstruct() {
        factory.setAppNameForContext(getAppId(), CONTEXT_ID);
    }

    private String removeSpaces(String withSpaces){
        return withSpaces.replace(' ', '_');
    }
    // fix for CR 6155144
    // used to get the policy context id. Also used by the RealmAdapter
    public static String getContextID(WebBundleDescriptor wbd) {
        String cid = null;
        if (wbd != null ) {
            String moduleId = wbd.getUniqueFriendlyId();
            cid = wbd.getApplication().getRegistrationName() +
                '/' + wbd.getUniqueFriendlyId();
        }
        return cid;
   }
      
    private void initialise() throws PolicyContextException {
        factory = Globals.get(SecurityRoleMapperFactory.class);
        policyConfigurationFactory = Globals.get(PolicyConfigurationFactory.class);
        AuditManager auditManager = SecurityServicesUtil.getInstance().getAuditManager();

        String appName = wbd.getApplication().getRegistrationName();
        CODEBASE = removeSpaces(CONTEXT_ID) ;
        //V3:Commented if(VirtualServer.ADMIN_VS.equals(getVirtualServers(appName))){
           if(ADMIN_VS.equals(getVirtualServers(appName))){
            LoginConfiguration lgConf = wbd.getLoginConfiguration();
            if (lgConf != null){
                String realmName = lgConf.getRealmName();
                SunWebApp sunDes = wbd.getSunDescriptor();
                if(sunDes != null){
                    SecurityRoleMapping[] srms = sunDes.getSecurityRoleMapping();
                    if(srms != null){
                        for (SecurityRoleMapping srm : srms) {
                            String[] principals = srm.getPrincipalName();
                            if (principals != null) {
                                for (String principal : principals) {
                                    ADMIN_PRINCIPAL.put(realmName + principal, new PrincipalImpl(principal));
                                }
                            }
                            for (String group : srm.getGroupNames()) {
                                ADMIN_GROUP.put(realmName + group, new Group(group));
                            }
                        }
                    }
                }
            }
        }
 
        // will require stuff in hash format for reference later on.
        try{
            java.net.URI uri = null;
            try{
		if(logger.isLoggable(Level.FINE))
		    logger.log(Level.FINE, "[Web-Security] Creating a Codebase URI with = "+CODEBASE);
		uri = new java.net.URI("file:///"+ CODEBASE);
		if(uri != null){
		    codesource = new CodeSource(new URL(uri.toString()), 
                            (java.security.cert.Certificate[]) null);
		}
		
            } catch(java.net.URISyntaxException use){
                // manually create the URL
                logger.log(Level.FINE, "[Web-Security] Error Creating URI ", use);
                throw new RuntimeException(use);
            }

        } catch(java.net.MalformedURLException mue){
            logger.log(Level.SEVERE, "ejbsm.codesourceerror", mue);
            throw new RuntimeException(mue);
        } 

        if(logger.isLoggable(Level.FINE)){
            logger.fine("[Web-Security] Context id (id under which  WEB component in application will be created) = "+ CONTEXT_ID);
            logger.fine("[Web-Security] Codebase (module id for web component) "+ CODEBASE);
        }

 	boolean inService = getFactory().inService(CONTEXT_ID);

 	// only regenerate policy file if it isn't already in service
 	// Consequently all things that deploy modules (as apposed to
 	// loading already deployed modules) must make sure pre-exiting
 	// pc is either in deleted or open state before this method
 	// (i.e. initialise) is called. That is, before constructing
 	// the WebSecurityManager. Note that policy statements are not
 	// removed to allow multiple web modules to be represented by same pc.
 
 	if (!inService) {
 	    policyConfiguration = getFactory().getPolicyConfiguration(CONTEXT_ID,false);
  	    generatePermissions();
  	}
 
 	if (uncheckedPermissionCache == null) {
 	    uncheckedPermissionCache =
		PermissionCacheFactory.createPermissionCache
		(this.CONTEXT_ID, codesource, protoPerms, null);

	    //if (uncheckedPermissionCache != null) {
 
		allResourcesCP = 
		    new CachedPermissionImpl(uncheckedPermissionCache,
					     allResources);
		allConnectionsCP = 
		    new CachedPermissionImpl(uncheckedPermissionCache,
					     allConnections);
	    //}

 	} else {
 	    uncheckedPermissionCache.reset();
 	}
 
    }
    // this will change too - get the application id name
    private String getAppId() {
        return wbd.getApplication().getRegistrationName();
    }

    public boolean permitAll(HttpServletRequest req) {
        boolean ret = false;
        WebResourcePermission webResPerm = createWebResourcePermission(req);
        if (uncheckedPermissionCache != null) {
           ret = uncheckedPermissionCache.checkPermission(webResPerm);
        }
        if (ret == false) {
            ret = checkPermissionWithoutCache(webResPerm, null);
        } 
        return ret;
    }

    /*
     * Invoke the <code>Policy</code> to determine if the <code>Permission</code>
     * object has security permission.
     * @param perm an instance of <code>Permission</code>.
     * @param principalSet a set containing the principals to check for authorization
     * @return true if granted, false if denied.
     */
    protected boolean checkPermission(Permission perm, Set principalSet) {   
        boolean ret = false;
        if (uncheckedPermissionCache != null) {
            ret = uncheckedPermissionCache.checkPermission(perm);
        }
        if (ret == false) {
            ret = checkPermissionWithoutCache(perm, principalSet);
        } else {
            try {
                setPolicyContext(CONTEXT_ID);
            } catch(Throwable t){
                if (logger.isLoggable(Level.FINE)){
 	            logger.log(Level.FINE, 
                        "[Web-Security] Web Permission Access Denied.",t);
                }
 	        ret = false;
            }
        }
        return ret;
    }

    private boolean checkPermissionWithoutCache(
            Permission perm, Set principalSet) {
         
        try{
 
 	    // NOTE: there is an assumption here, that this setting of the PC will
 	    // remain in affect through the component dispatch, and that the
 	    // component will not call into any other policy contexts.
	    // even so, could likely reset on failed check.
 	    
 	    setPolicyContext(CONTEXT_ID);
 
 	} catch(Throwable t){
            if (logger.isLoggable(Level.FINE)){
 	        logger.log(Level.FINE, 
                    "[Web-Security] Web Permission Access Denied.",t);
            }
 	    return false;
 	}
 
	ProtectionDomain prdm = 
		(ProtectionDomain)protectionDomainCache.get(principalSet);
  
	if (prdm == null) {

            Principal[] principals = null;
            principals = (principalSet == null ? null : 
                      (Principal []) principalSet.toArray(new Principal[0]));

            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE,"[Web-Security] Generating a protection domain for Permission check.");

                if (principals != null) {
                    for (int i=0; i<principals.length; i++){
                        logger.log(Level.FINE, "[Web-Security] Checking with Principal : "+ principals[i].toString());
                    }
                } else {
                    logger.log(Level.FINE, "[Web-Security] Checking with Principals: null");
                }
            }

            prdm = new ProtectionDomain(codesource, null, null,principals);
            protectionDomainCache.put(principalSet,prdm);
        }

        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, "[Web-Security] Codesource with Web URL: " + codesource.getLocation().toString());
            logger.log(Level.FINE, "[Web-Security] Checking Web Permission with Principals : "+ principalSetToString(principalSet));
            logger.log(Level.FINE, "[Web-Security] Web Permission = " +perm.toString());
        }
  
        return policy.implies(prdm, perm);
    }    

    protected PolicyConfigurationFactory getFactory() throws PolicyContextException{
        if (policyConfigurationFactory == null){
            try{
                policyConfigurationFactory = PolicyConfigurationFactory.getPolicyConfigurationFactory();
            } catch(java.lang.ClassNotFoundException ex){
                // FIX ME: Need to create an approriate exception
                throw new PolicyContextException(ex);
            } 
        }
        return policyConfigurationFactory;
    }
    
    private WebResourcePermission createWebResourcePermission(
            HttpServletRequest httpsr) {
        //TODO: V3 String uri = (String)httpsr.getAttribute(Globals.CONSTRAINT_URI);
        String uri = (String)httpsr.getAttribute(CONSTRAINT_URI);
        if (uri == null) {
            uri = httpsr.getRequestURI();
             if (uri != null) {
                // FIX TO BE CONFIRMED: subtract the context path
                String contextPath = httpsr.getContextPath();
                int contextLength = contextPath == null ? 0 : 
                    contextPath.length();
                if (contextLength > 0) {
                    uri = uri.substring(contextLength);
                }
            }
        }
        if (uri == null) {
            if (logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE,"[Web-Security] mappedUri is null");
            }
            throw new RuntimeException("Fatal Error in creating WebResourcePermission");
        }
        if(uri.equals("/")) {
            uri = EMPTY_STRING;
        }else {
	    // FIX TO BE CONFIRMED: encode all colons
 	    uri = uri.replaceAll(":","%3A");
  	}
        WebResourcePermission perm = new WebResourcePermission(
                uri, httpsr.getMethod());
        return perm;
    }
    
    /**
     * Perform access control based on the <code>HttpServletRequest</code>.
     * Return <code>true</code> if this constraint is satisfied and processing
     * should continue, or <code>false</code> otherwise.
     * @return true is the resource is granted, false if denied
     */
    public boolean hasResourcePermission(HttpServletRequest httpsr){
	SecurityContext sc = getSecurityContext(httpsr.getUserPrincipal());
        WebResourcePermission perm = createWebResourcePermission(httpsr);
        setSecurityInfo(httpsr);
        boolean isGranted = checkPermission(perm,sc.getPrincipalSet());
	SecurityContext.setCurrent(sc);
        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE,"[Web-Security] hasResource isGranted: " + isGranted);
            logger.log(Level.FINE,"[Web-Security] hasResource perm: " + perm);
        }
        AuditManager auditManager = SecurityServicesUtil.getInstance().getAuditManager();
        if(auditManager !=null && auditManager.isAuditOn()){
            Principal prin = httpsr.getUserPrincipal();
            String user = (prin != null) ? prin.getName(): null;
            auditManager.webInvocation(user, httpsr, RESOURCE, isGranted);
        }
        return isGranted;
    }
    
    
    /* 
     * Return <code>true</code> if the specified servletName has the specified
     * security role, within the context of the WebRoleRefPermission; 
     * otherwise return
     * <code>false</code>.
     *
     * @param principal servletName the resource's name.
     * @param principal Principal for whom the role is to be checked
     * @param role Security role to be checked
     * @return true is the resource is granted, false if denied
     */
    public boolean hasRoleRefPermission(String servletName, String role, Principal p) {
	Set principalSet = getSecurityContext(p).getPrincipalSet();
        WebRoleRefPermission perm = new WebRoleRefPermission(servletName, role);
        boolean isGranted = checkPermission(perm,principalSet);
        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE,"[Web-Security] hasRoleRef perm: " + perm);
            logger.log(Level.FINE,"[Web-Security] hasRoleRef isGranted: " + isGranted);
        }
        return isGranted;
    }

    
    /*
     * Enforce any user data constraint required by the security constraint
     * guarding this request URI.  Return <code>true</code> if this constraint
     * was not violated and processing should continue, or <code>false</code>
     * if we have created a response already.
     *
     * @return true is the user is granted, false if denied.
     */    
    public int hasUserDataPermission(HttpServletRequest httpsr){
        setSecurityInfo(httpsr);
        WebUserDataPermission perm = new WebUserDataPermission(httpsr);       
        boolean  isGranted = checkPermission(perm, defaultPrincipalSet);
        int result = 0;
       
        if ( isGranted ) {
            result = 1;
        }
 
        if(logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE,"[Web-Security] hasUserDataPermission perm: " + perm);
            logger.log(Level.FINE,"[Web-Security] hasUserDataPermission isGranted: " + isGranted);
        }

        AuditManager auditManager = SecurityServicesUtil.getInstance().getAuditManager();
        if(auditManager!= null && auditManager.isAuditOn()){
            Principal prin = httpsr.getUserPrincipal();
            String user = (prin != null) ? prin.getName(): null;
            auditManager.webInvocation(user, httpsr, USERDATA, isGranted);
        }

        if ( !isGranted ) {

             perm = new WebUserDataPermission
		 ( perm.getName(),
		   new String[] { httpsr.getMethod() },
		   "CONFIDENTIAL" );

             isGranted = checkPermission(perm, defaultPrincipalSet);

             if (isGranted)
                result = -1;
        }
        
        return result;
    }

    private void generatePermissions(){
        try{
            WebPermissionUtil.processConstraints(wbd, policyConfiguration);    
            WebPermissionUtil.createWebRoleRefPermission(wbd, policyConfiguration); 
        } catch (PolicyContextException pce){
            logger.log(Level.FINE,"[Web-Security] FATAL Permission Generation: " + pce.getMessage());
            throw new RuntimeException("Fatal error creating web permissions", pce);
        }
    }
    
    public void destroy() throws PolicyContextException {
        boolean wasInService = getFactory().inService(CONTEXT_ID);
	if (policyConfiguration == null) {
	    policyConfiguration = getFactory().
		getPolicyConfiguration(CONTEXT_ID,false);
	}
        if (wasInService) {
            policy.refresh();
            PermissionCacheFactory.removePermissionCache
		(uncheckedPermissionCache);
            uncheckedPermissionCache = null;
        }
        factory.removeAppNameForContext(CONTEXT_ID);
        // pc.delete() will be invoked during undeployment
	//policyConfiguration.delete();
        WebSecurityManagerFactory.getInstance().removeWebSecurityManager(CONTEXT_ID);
    }
   
    private static String setPolicyContext(final String ctxID) throws Throwable {
	String old = PolicyContext.getContextID();
	if (old != ctxID && 
	    (old == null || ctxID == null || !old.equals(ctxID))) {
  
	    if(logger.isLoggable(Level.FINE)){
		logger.fine("[Web-Security] Setting Policy Context ID: old = " + old + 
			    " ctxID = " + ctxID);
	    }
  
	    try {  
		AppservAccessController.doPrivileged(new PrivilegedExceptionAction(){
		    public java.lang.Object run() throws Exception{
			PolicyContext.setContextID(ctxID);
			return null;
		    }
		});
	    } catch (java.security.PrivilegedActionException pae) {
		Throwable cause = pae.getCause();
		if( cause instanceof java.security.AccessControlException) {
		    logger.log(Level.SEVERE,"[Web-Security] setPolicy SecurityPermission required to call PolicyContext.setContextID",cause);
		} else {
		    logger.log(Level.SEVERE,"[Web-Security] Unexpected Exception while setting policy context",cause);
		}
		throw cause;
	    }
	} else if(logger.isLoggable(Level.FINE)){
	    logger.fine("[Web-Security] Policy Context ID was: " + old);
	}
	return old;
    }
    
    /**
     * This is an private method for transforming principal into a SecurityContext
     * @param principal expected to be a WebPrincipal
     * @return SecurityContext
     */
    private SecurityContext getSecurityContext(Principal principal) {
        SecurityContext secContext = null;
        if (principal != null) {
	    if (principal instanceof WebPrincipal){
		WebPrincipal wp = (WebPrincipal)principal;
		secContext = wp.getSecurityContext();
	    }else {
		secContext = new SecurityContext(principal.getName(),null);
	    }
        } 
	if (secContext == null) {
            secContext = SecurityContext.getDefaultSecurityContext();
        }
	return secContext;
    }

    /**
     * This is an private method for policy context handler data info
     * @param httpRequest
     */
    private void setSecurityInfo(HttpServletRequest httpRequest) {
        if (httpRequest != null) {
            pcHandlerImpl.getHandlerData().setHttpServletRequest(httpRequest);
        }
    }

    private String principalSetToString(Set principalSet) {
 	String result = null;
 	if (principalSet != null) {
 	    Principal[] principals = 
 		(Principal []) principalSet.toArray(new Principal[0]);
 	    for (int i =0; i<principals.length; i++) {
 		if (i == 0) {
 		    result = principals[i].toString();
 		} else {
 		    result = result + ", "+ new String(principals[i].toString());
 		}
 	    }
 	}
 	return result;
    }
    
    /*V3:Commented, replacement code copied from web-container VirtualServer.java
    private String getVirtualServers(String appName) {
        String ret = null;
        try {
            ConfigContext ctx =
                ApplicationServer.getServerContext().getConfigContext();
            ret = ServerBeansFactory
                    .getVirtualServersByAppName(ctx, appName);
        } catch (ConfigException ce) {
            logger.log(Level.FINE, "Cannot get virtual server for " + appName, ce);
        }
        return ret;
    }*/
    
     /**
     * Virtual servers are maintained in the reference contained 
     * in Server element. First, we need to find the server
     * and then get the virtual server from the correct reference
     *
     * @param appName Name of the app to get vs
     *
     * @return virtual servers as a string (separated by space or comma)
     */
    private String getVirtualServers(String appName) {
        String ret = null;
        Server server = serverContext.getDefaultHabitat().getComponent(Server.class);
        for (ApplicationRef appRef : server.getApplicationRef()) {
            if (appRef.getRef().equals(appName)) {
                return appRef.getVirtualServers();
            }
        }
        
        return ret;
    }
    
    
    public static Principal getAdminPrincipal(String username, String realmName){
        return (Principal)ADMIN_PRINCIPAL.get(realmName+username);
    }
    public static Principal getAdminGroup(String group, String realmName){
        return (Principal)ADMIN_GROUP.get(realmName+group);
    }
    
    /**
      * returns true to indicate that a policy check was made
      * and there were no constrained resources.
      * when caching is disabled must always return false, which will
      * ensure that policy is consulted to authorize each request.
      */
    public boolean hasNoConstrainedResources() {
	if (allResourcesCP != null && allConnectionsCP != null) {
	    boolean x = allResourcesCP.checkPermission();
	    boolean y = allConnectionsCP.checkPermission();
	    return x && y;
        }
	return false;
    }
}
