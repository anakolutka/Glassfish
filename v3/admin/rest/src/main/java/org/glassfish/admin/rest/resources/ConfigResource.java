/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
* Copyright 2009 Sun Microsystems, Inc. All rights reserved.
* Generated code from the com.sun.enterprise.config.serverbeans.*
* config beans, based on  HK2 meta model for these beans
* see generator at org.admin.admin.rest.GeneratorResource
* date=Thu Jul 30 19:16:47 PDT 2009
* Very soon, this generated code will be replace by asm or even better...more dynamic logic.
* Ludovic Champenois ludo@dev.java.net
*
**/
package org.glassfish.admin.rest.resources;
import com.sun.enterprise.config.serverbeans.*;
import javax.ws.rs.*;
import org.glassfish.admin.rest.TemplateResource;
import org.glassfish.admin.rest.provider.GetResult;
import com.sun.enterprise.config.serverbeans.Config;
public class ConfigResource extends TemplateResource<Config> {

	@Path("admin-service/")
	public AdminServiceResource getAdminServiceResource() {
		AdminServiceResource resource = resourceContext.getResource(AdminServiceResource.class);
		resource.setEntity(getEntity().getAdminService() );
		return resource;
	}
	@Path("ejb-container/")
	public EjbContainerResource getEjbContainerResource() {
		EjbContainerResource resource = resourceContext.getResource(EjbContainerResource.class);
		resource.setEntity(getEntity().getEjbContainer() );
		return resource;
	}
	@Path("transaction-service/")
	public TransactionServiceResource getTransactionServiceResource() {
		TransactionServiceResource resource = resourceContext.getResource(TransactionServiceResource.class);
		resource.setEntity(getEntity().getTransactionService() );
		return resource;
	}
	@Path("management-rules/")
	public ManagementRulesResource getManagementRulesResource() {
		ManagementRulesResource resource = resourceContext.getResource(ManagementRulesResource.class);
		resource.setEntity(getEntity().getManagementRules() );
		return resource;
	}
	@Path("java-config/")
	public JavaConfigResource getJavaConfigResource() {
		JavaConfigResource resource = resourceContext.getResource(JavaConfigResource.class);
		resource.setEntity(getEntity().getJavaConfig() );
		return resource;
	}
	@Path("monitoring-service/")
	public MonitoringServiceResource getMonitoringServiceResource() {
		MonitoringServiceResource resource = resourceContext.getResource(MonitoringServiceResource.class);
		resource.setEntity(getEntity().getMonitoringService() );
		return resource;
	}
	@Path("group-management-service/")
	public GroupManagementServiceResource getGroupManagementServiceResource() {
		GroupManagementServiceResource resource = resourceContext.getResource(GroupManagementServiceResource.class);
		resource.setEntity(getEntity().getGroupManagementService() );
		return resource;
	}
	@Path("property/")
	public ListPropertyResource getPropertyResource() {
		ListPropertyResource resource = resourceContext.getResource(ListPropertyResource.class);
		resource.setEntity(getEntity().getProperty() );
		return resource;
	}
	@Path("availability-service/")
	public AvailabilityServiceResource getAvailabilityServiceResource() {
		AvailabilityServiceResource resource = resourceContext.getResource(AvailabilityServiceResource.class);
		resource.setEntity(getEntity().getAvailabilityService() );
		return resource;
	}
	@Path("thread-pools/")
	public ThreadPoolsResource getThreadPoolsResource() {
		ThreadPoolsResource resource = resourceContext.getResource(ThreadPoolsResource.class);
		resource.setEntity(getEntity().getThreadPools() );
		return resource;
	}
	@Path("web-container/")
	public WebContainerResource getWebContainerResource() {
		WebContainerResource resource = resourceContext.getResource(WebContainerResource.class);
		resource.setEntity(getEntity().getWebContainer() );
		return resource;
	}
	@Path("mdb-container/")
	public MdbContainerResource getMdbContainerResource() {
		MdbContainerResource resource = resourceContext.getResource(MdbContainerResource.class);
		resource.setEntity(getEntity().getMdbContainer() );
		return resource;
	}
	@Path("log-service/")
	public LogServiceResource getLogServiceResource() {
		LogServiceResource resource = resourceContext.getResource(LogServiceResource.class);
		resource.setEntity(getEntity().getLogService() );
		return resource;
	}
	@Path("http-service/")
	public HttpServiceResource getHttpServiceResource() {
		HttpServiceResource resource = resourceContext.getResource(HttpServiceResource.class);
		resource.setEntity(getEntity().getHttpService() );
		return resource;
	}
	@Path("jms-service/")
	public JmsServiceResource getJmsServiceResource() {
		JmsServiceResource resource = resourceContext.getResource(JmsServiceResource.class);
		resource.setEntity(getEntity().getJmsService() );
		return resource;
	}
	@Path("alert-service/")
	public AlertServiceResource getAlertServiceResource() {
		AlertServiceResource resource = resourceContext.getResource(AlertServiceResource.class);
		resource.setEntity(getEntity().getAlertService() );
		return resource;
	}
	@Path("connector-service/")
	public ConnectorServiceResource getConnectorServiceResource() {
		ConnectorServiceResource resource = resourceContext.getResource(ConnectorServiceResource.class);
		resource.setEntity(getEntity().getConnectorService() );
		return resource;
	}
	@Path("network-config/")
	public NetworkConfigResource getNetworkConfigResource() {
		NetworkConfigResource resource = resourceContext.getResource(NetworkConfigResource.class);
		resource.setEntity(getEntity().getNetworkConfig() );
		return resource;
	}
	@Path("system-property/")
	public ListSystemPropertyResource getSystemPropertyResource() {
		ListSystemPropertyResource resource = resourceContext.getResource(ListSystemPropertyResource.class);
		resource.setEntity(getEntity().getSystemProperty() );
		return resource;
	}
	@Path("iiop-service/")
	public IiopServiceResource getIiopServiceResource() {
		IiopServiceResource resource = resourceContext.getResource(IiopServiceResource.class);
		resource.setEntity(getEntity().getIiopService() );
		return resource;
	}
	@Path("diagnostic-service/")
	public DiagnosticServiceResource getDiagnosticServiceResource() {
		DiagnosticServiceResource resource = resourceContext.getResource(DiagnosticServiceResource.class);
		resource.setEntity(getEntity().getDiagnosticService() );
		return resource;
	}
	@Path("security-service/")
	public SecurityServiceResource getSecurityServiceResource() {
		SecurityServiceResource resource = resourceContext.getResource(SecurityServiceResource.class);
		resource.setEntity(getEntity().getSecurityService() );
		return resource;
	}
}
