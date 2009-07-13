/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
* Copyright 2009 Sun Microsystems, Inc. All rights reserved.
* Generated code from the com.sun.enterprise.config.serverbeans.*
* config beans, based on  HK2 meta model for these beans
* see generator at org.admin.admin.rest.GeneratorResource
* date=Mon Jul 13 13:06:35 PDT 2009
* Very soon, this generated code will be replace by asm or even better...more dynamic logic.
* Ludovic Champenois ludo@dev.java.net
*
**/
package org.glassfish.admin.rest.resources;
import com.sun.enterprise.config.serverbeans.*;
import javax.ws.rs.*;
import org.glassfish.admin.rest.TemplateResource;
import org.glassfish.admin.rest.provider.GetResult;
import com.sun.enterprise.config.serverbeans.ThreadPools;
public class ThreadPoolsResource extends TemplateResource<ThreadPools> {

public String[] getCommandResourcesPaths() {
return new String[]{};
}

	@Path("thread-pool/")
	public ListThreadPoolResource getThreadPoolResource() {
		ListThreadPoolResource resource = resourceContext.getResource(ListThreadPoolResource.class);
		resource.setEntity(getEntity().getThreadPool() );
		return resource;
	}
}
