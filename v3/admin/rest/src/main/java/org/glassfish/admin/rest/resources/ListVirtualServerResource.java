/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
* Copyright 2009 Sun Microsystems, Inc. All rights reserved.
* Generated code from the com.sun.enterprise.config.serverbeans.*
* config beans, based on  HK2 meta model for these beans
* see generator at org.admin.admin.rest.GeneratorResource
* date=Tue Jun 30 14:26:58 PDT 2009
* Very soon, this generated code will be replace by asm or even better...more dynamic logic.
* Ludovic Champenois ludo@dev.java.net
*
**/
package org.glassfish.admin.rest.resources;
import com.sun.enterprise.config.serverbeans.*;
import javax.ws.rs.*;
import java.util.List;
import org.glassfish.admin.rest.TemplateListOfResource;
import com.sun.enterprise.config.serverbeans.VirtualServer;
public class ListVirtualServerResource extends TemplateListOfResource<VirtualServer> {


	@Path("{Id}/")
	public VirtualServerResource getVirtualServerResource(@PathParam("Id") String id) {
		VirtualServerResource resource = resourceContext.getResource(VirtualServerResource.class);
		for (VirtualServer c: entity){
			if(c.getId().equals(id)){
				resource.setEntity(c);
			}
		}
		return resource;
	}


public String getPostCommand() {
	return "create-virtual-server";
}
}
