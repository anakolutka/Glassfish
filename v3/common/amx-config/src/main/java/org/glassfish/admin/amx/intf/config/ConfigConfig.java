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
package org.glassfish.admin.amx.intf.config;



import java.util.Map;
import org.glassfish.admin.amx.core.AMXProxy;

/**
	 Configuration for the &lt;config&gt; element.
 */
public interface ConfigConfig
	extends PropertiesAccess, SystemPropertiesAccess,
	NamedConfigElement
{
    public static final String AMX_TYPE = "config";
	
	/**
		Key for use with {@link ConfigsConfig#createConfigConfig},
		value must be {@link java.lang.Boolean}.
	 */
	public static final String DYNAMIC_RECONFIGURATION_ENABLED_KEY = "DynamicReconfigurationEnabled";
	
	/**
		Key for use with {@link ConfigsConfig#createConfigConfig}. Specifies
		the name of the config which should be *copied*.  Default is
		{@link #DEFAULT_SRC_CONFIG_NAME}.
	 */
	public static final String SRC_CONFIG_NAME_KEY = "SrcConfigKey";
	
	/**
		Default config which will be copied by
		{@link ConfigsConfig#createConfigConfig}.
		@see #SRC_CONFIG_NAME_KEY
	 */
	public static final String DEFAULT_SRC_CONFIG_NAME = "default-config";
    
	/**
		Configuration of the config element itself.
	 */
	/**
		Return the IiopServiceConfig.
	 */
	public IiopServiceConfig	getIiopService();
	
	/**
		Return the HttpServiceConfig.
	 */
	public HttpServiceConfig	getHttpService();
	
	/**
		Return the NetworkConfig.
	 */
	public AMXProxy	getNetworkConfig();
	
	/**
		Return the SecurityServiceConfig.
	 */
	public SecurityServiceConfig	getSecurityService();
	
	/**
		Return the MonitoringServiceConfig.
	 */
	public MonitoringServiceConfig	getMonitoringService();
	
	/**
		Return the AdminServiceConfig.
	 */
	public AdminServiceConfig	getAdminService();
    
	/** @since Glassfish V3 */
	public ThreadPoolsConfig         getThreadPools();
	
	/**
        @deprecated use {@link ThreadPoolsConfig#getThreadPool}
	 */
	public Map<String,ThreadPoolConfig>	getThreadPool();
    
    
// 	/**
//         @deprecated use {@link ThreadPoolsConfig#createThreadPoolConfig}
// 	 */
// 	public ThreadPoolConfig	createThreadPoolConfig( String name, Map<String,String> optional );
// 
// 	/**
//         @deprecated use {@link ThreadPoolsConfig#removeThreadPoolConfig}
// 	 */
// 	public void			removeThreadPoolConfig( String name );

	/**
	    Return the DiagnosticServiceConfig.  May be null.
	    @since AppServer 9.0
        */
	public DiagnosticServiceConfig getDiagnosticService();
    
	/**
		Return the WebContainerConfig.
	 */
	public WebContainerConfig	getWebContainer() ;
	
	/**
		Return the EJBContainerConfig.
	 */
	public EJBContainerConfig	getEJBContainer() ;
	
	/**
		Return the MDBContainerConfig.
	 */
	public MDBContainerConfig	getMDBContainer();
	
	/**
		Return the JavaConfig.
	 */
	public JavaConfig	getJava();
	
	/**
		Return the JMSServiceConfig.
	 */
	public JMSServiceConfig	getJMSService();
	
	/**
		Return the LogServiceConfig.
	 */
	public LogServiceConfig	getLogService();
	
	/**
		Return the TransactionServiceConfig.
	 */
	public TransactionServiceConfig	getTransactionService();
	
	/**
		Return the AvailabilityServiceConfig.
	 */
	public AvailabilityServiceConfig	getAvailabilityService();

	/**
		Return the ConnectorServiceConfig.
	 */
	public ConnectorServiceConfig	getConnectorService();
	
// 	/**
// 	    Create the ConnectorServiceConfig if it doesn't already exist.
// 	 */
// 	public ConnectorServiceConfig	createConnectorService();
// 	public void     removeConnectorService();
// 
// 	
// 	/**
// 	    Create the DiagnosticServiceConfig.
// 	    @since AppServer 9.0
//         */
// 	public DiagnosticServiceConfig createDiagnosticService();
// 	
// 	/**
// 	    Remove the DiagnosticServiceConfig.
// 	    @since AppServer 9.0
//         */
// 	public void removeDiagnosticService();

	/**
	Return the Group Management Service configuration.
	@since AppServer 9.0
	*/
	public GroupManagementServiceConfig getGroupManagementService();
	
	/**                  
        When set to "true" then any changes to the system (e.g.       
        applications deployed, resources created) will be             
        automatically applied to the affected servers without a       
        restart being required. When set to "false" such changes will 
        only be picked up by the affected servers when each server    
        restarts.
        @since AppServer 9.0                                                
     */
    @ResolveTo(Boolean.class)
	public String   getDynamicReconfigurationEnabled();
	
	/**
	    @see #getDynamicReconfigurationEnabled
        @since AppServer 9.0
	 */
	public void      setDynamicReconfigurationEnabled( String enabled );
	
	/**
	    @return ManagementRulesConfig (may be null );
	 */
	public ManagementRulesConfig    getManagementRules();
}

















