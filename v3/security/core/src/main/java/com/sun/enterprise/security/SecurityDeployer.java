/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * Header Notice in each file and include the License file 
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.  
 * If applicable, add the following below the CDDL Header, 
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */
package com.sun.enterprise.security;

import com.sun.enterprise.security.web.integration.WebSecurityManagerFactory;
import com.sun.enterprise.security.web.integration.WebSecurityManager;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.deployment.common.SimpleDeployer;
import org.glassfish.deployment.common.DummyApplication;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.security.util.IASSecurityException;
import org.glassfish.internal.api.ServerContext;
import com.sun.logging.LogDomains;


import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.event.EventListener.Event;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PostConstruct;


/**
 * Security Deployer which generate and clean the security policies
 *
 */
@Service
public class SecurityDeployer extends SimpleDeployer<SecurityContainer, DummyApplication> implements PostConstruct  {

    private static final Logger _logger = LogDomains.getLogger(SecurityDeployer.class, LogDomains.SECURITY_LOGGER);
    @Inject
    private ServerContext serverContext;
    
    @Inject 
    private Habitat habitat;
    
    @Inject 
    private PolicyLoader policyLoader;
    
    @Inject 
    private WebSecurityManagerFactory wsmf;
    
    private EventListener listener = null;
    
    public static class AppDeployEventListener implements EventListener {

        public void event(Event event) {
            if (Deployment.APPLICATION_PREPARED.equals(event.type())) {
                //this is an Application Prepare Completion Event
                DeploymentContext dc = (DeploymentContext) event.hook();
                OpsParams params = dc.getCommandParameters(OpsParams.class);
                String appName = params.name();
                Application app = dc.getModuleMetaData(Application.class);
                Set<WebBundleDescriptor> webDesc = app.getWebBundleDescriptors();
                Set<EjbBundleDescriptor> ejbDesc = app.getEjbBundleDescriptors();
                try {
                    // link with the ejb name                     
                    String linkName = null;
                    boolean lastInService = false;
                    for (WebBundleDescriptor wbd : webDesc) {
                        String name = SecurityUtil.getContextID(wbd);
                        lastInService = SecurityUtil.linkPolicyFile(name, linkName, lastInService);
                        linkName = name;
                    }
                    for (EjbBundleDescriptor ejbd : ejbDesc) {
                        String name = SecurityUtil.getContextID(ejbd);
                        //handle EJB's inside a WAR file
                        if (!name.equals(linkName)) {
                            lastInService = SecurityUtil.linkPolicyFile(name, linkName, lastInService);
                            linkName = name;
                        }
                    }
                    //generate policies
                    for (WebBundleDescriptor wbd : webDesc) {
                        String name = SecurityUtil.getContextID(wbd);
                        SecurityUtil.generatePolicyFile(name);
                    }
                    for (EjbBundleDescriptor ejbd : ejbDesc) {
                        String name = SecurityUtil.getContextID(ejbd);
                        SecurityUtil.generatePolicyFile(name);
                    }

                } catch (IASSecurityException se) {
                    String msg = "Error in generating security policy for " + appName;
                    throw new DeploymentException(msg, se);
                }
            }
        }
    };
   
    // creates security policy if needed
    @Override
    protected void generateArtifacts(DeploymentContext dc)
            throws DeploymentException {
         generatePolicy(dc);
    }

    // removes security policy if needed
    @Override
    protected void cleanArtifacts(DeploymentContext dc)
            throws DeploymentException {
        removePolicy(dc);
    }

    @Override
    public DummyApplication load(SecurityContainer container, DeploymentContext context) {
        return new DummyApplication();
    }
    
    @Override
    public void unload(DummyApplication container, DeploymentContext context) {
        OpsParams params = context.getCommandParameters(OpsParams.class);
        cleanSecurityContext(params.name());
    }


    protected void generatePolicy(DeploymentContext dc)
            throws DeploymentException {
        OpsParams params = dc.getCommandParameters(OpsParams.class);
        String appName = params.name();
        try {
            //policyLoader.loadPolicy();
            Application app = dc.getModuleMetaData(Application.class);
            WebBundleDescriptor wbd = null;
            Set<WebBundleDescriptor> webDesc = app.getWebBundleDescriptors();
            if (webDesc == null) {
                //we now register the security deployer for jar's and EAR's as well.
                return;
            }
            Iterator<WebBundleDescriptor> iter = webDesc.iterator();
            if (!iter.hasNext()) {
                //we now register the security deployer for jar's and EAR's as well.
                return;
            }
            //TODO V3: shouldn't i iterate over all WBD's ?.
            wbd =  iter.next();
            
            // this should create all permissions
            wsmf.createManager(wbd,false,serverContext);
            // for an application the securityRoleMapper should already be
            // created. I am just creating the web permissions and handing
            // it to the security component.
            //Policy File Generation is handled in the EventListener above
            //String name = WebSecurityManager.getContextID(wbd);
            //SecurityUtil.generatePolicyFile(name);

        } catch (Exception se) {
            String msg = "Error in generating security policy for " + appName;
            throw new DeploymentException(msg, se);
        }
    }

    private void removePolicy(DeploymentContext dc) throws
            DeploymentException {
        OpsParams params = dc.getCommandParameters(OpsParams.class);
        String appName = params.name();

        try {
            String[] webcontexts = wsmf.getContextsForApp(appName, true);
            if (webcontexts != null) {
                  for (int i = 0; i < webcontexts.length; i++) {
                      if (webcontexts[i] != null) {
                          SecurityUtil.removePolicy(webcontexts[i]);
                      }
                  }
             }

            /* From V2 but keep commented until need is discovered
            //remove any remaining policy
            //This is to address the bug where the CONTEXT_ID in 
            //WebSecurityManagerFactory is not properly populated.
            //We force the sub-modules to be removed in this case.
            //This should not impact undeploy performance on DAS.
            //This needs to be fixed better later.
            String policyRootDir = System.getProperty(
                    "com.sun.enterprise.jaccprovider.property.repository");
            if (policyRootDir != null) {
                List<String> contextIds = new ArrayList<String>();
                File policyDir = new File(policyRootDir + File.separator + appName);
                if (policyDir.exists()) {
                    File[] policies = policyDir.listFiles();
                    for (int i = 0; i < policies.length; i++) {
                        if (policies[i].isDirectory()) {
                            contextIds.add(appName + '/' + policies[i].getName());
                        }
                    }
                } else {
                //we tried.  give up now.
                }
                if (contextIds.size() > 0) {
                    for (String cId : contextIds) {
                        SecurityUtil.removePolicy(cId);
                    }
                }
            }*/
        } catch (IASSecurityException ex) {
            String msg = "Error in removing security policy for " + appName;
            _logger.log(Level.WARNING, msg, ex);
            throw new DeploymentException(msg, ex);
        }
    }

    /**
     * Loads the meta date associated with the application.
     *
     * @parameters type type of metadata that this deployer has declared providing.
     */
    public <V> V loadMetaData(Class<V> type, DeploymentContext context) {
        return null;
    }

    public MetaData getMetaData() {
        return new MetaData(false, null, new Class[] {Application.class});
    }
    
     
    /**
     * Clean security policy generated at deployment time.
     * NOTE: This routine calls destroy on the WebSecurityManagers, 
     * but that does not cause deletion of the underlying policy (files).
     * The underlying policy is deleted when removePolicy 
     * (in AppDeployerBase and WebModuleDeployer) is called.
     * @param appName  the app name
     */
    private void cleanSecurityContext(String appName) {
	ArrayList<WebSecurityManager> managers =
	    wsmf.getManagersForApp(appName,true);
	for (int i = 0; managers != null && i < managers.size(); i++) {
  
	    try {
	         managers.get(i).destroy();
	    } catch (javax.security.jacc.PolicyContextException pce){
	         // log it and continue
	         _logger.log(Level.WARNING,
			     "Unable to destroy WebSecurityManager",
			     pce);
	    }
	}
    }

    public static List<EventTypes> getDeploymentEvents() {
        ArrayList<EventTypes> events = new ArrayList<EventTypes>();
        events.add(Deployment.APPLICATION_PREPARED);
        return events;
    }

    public void postConstruct() {
        listener = new AppDeployEventListener();
        Events events = habitat.getByContract(Events.class);
        events.register(listener);
    }
    
    
//    private static void initRoleMapperFactory() //throws Exception
//    {
//        Object o = null;
//        Class c = null;
//        // this should never fail.
//        try {
//            c = Class.forName("com.sun.enterprise.security.acl.RoleMapperFactory");
//            if (c != null) {
//                o = c.newInstance();
//                if (o != null && o instanceof SecurityRoleMapperFactory) {
//                    SecurityRoleMapperFactoryMgr.registerFactory((SecurityRoleMapperFactory) o);
//                }
//            }
//            if (o == null) {
//            //               _logger.log(Level.SEVERE,_localStrings.getLocalString("j2ee.norolemapper", "Cannot instantiate the SecurityRoleMapperFactory"));
//            }
//        } catch (Exception cnfe) {
////            _logger.log(Level.SEVERE,
////			_localStrings.getLocalString("j2ee.norolemapper", "Cannot instantiate the SecurityRoleMapperFactory"), 
////			cnfe);
////		cnfe.printStackTrace();
////		throw new RuntimeException(cnfe);
//        //   throw  cnfe;
//        }
//    }
}

