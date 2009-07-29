/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
* Copyright 2009 Sun Microsystems, Inc. All rights reserved.
* Generated code from the com.sun.enterprise.config.serverbeans.*
* config beans, based on  HK2 meta model for these beans
* see generator at org.admin.admin.rest.GeneratorResource
* date=Tue Jul 28 17:11:41 PDT 2009
* Very soon, this generated code will be replace by asm or even better...more dynamic logic.
* Ludovic Champenois ludo@dev.java.net
*
**/
package org.glassfish.admin.rest.resources;
import com.sun.enterprise.config.serverbeans.*;
import javax.ws.rs.*;
import java.util.List;
import org.glassfish.admin.rest.TemplateListOfResource;
import org.glassfish.admin.rest.provider.GetResultList;
import org.glassfish.api.monitoring.MonitoringItem;
public class ListMonitoringItemResource extends TemplateListOfResource<MonitoringItem> {


	@Path("{Level}/")
	public MonitoringItemResource getMonitoringItemResource(@PathParam("Level") String id) {
		MonitoringItemResource resource = resourceContext.getResource(MonitoringItemResource.class);
		for (MonitoringItem c: entity){
//THIS KEY IS THE FIRST Attribute ONE ludo
			if(c.getLevel().equals(id)){
				resource.setEntity(c);
			}
		}
		return resource;
	}

public String[][] getCommandResourcesPaths() {
return new String[][]{};
}


public String getPostCommand() {
	return null;
}
}
