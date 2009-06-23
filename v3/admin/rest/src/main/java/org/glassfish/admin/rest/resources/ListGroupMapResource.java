/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
* Copyright 2009 Sun Microsystems, Inc. All rights reserved.
* Generated code from the com.sun.enterprise.config.serverbeans.*
* config beans, based on  HK2 meta model for these beans
* see generator at org.admin.admin.rest.GeneratorResource
* date=Sat Jun 20 16:10:03 PDT 2009
* Very soon, this generated code will be replace by asm or even better...more dynamic logic.
* Ludovic Champenois ludo@dev.java.net
*
**/
package org.glassfish.admin.rest.resources;
import com.sun.enterprise.config.serverbeans.*;
import javax.ws.rs.*;
import java.util.List;
import org.glassfish.admin.rest.TemplateListOfResource;
import com.sun.enterprise.config.serverbeans.GroupMap;
public class ListGroupMapResource extends TemplateListOfResource<GroupMap> {


	@Path("{MappedGroup}/")
	public GroupMapResource getGroupMapResource(@PathParam("MappedGroup") String id) {
		GroupMapResource resource = resourceContext.getResource(GroupMapResource.class);
		for (GroupMap c: entity){
//THIS KEY IS THE FIRST Attribute ONE ludo
			if(c.getMappedGroup().equals(id)){
				resource.setEntity(c);
			}
		}
		return resource;
	}


public String getPostCommand() {
	return null;
}
}
