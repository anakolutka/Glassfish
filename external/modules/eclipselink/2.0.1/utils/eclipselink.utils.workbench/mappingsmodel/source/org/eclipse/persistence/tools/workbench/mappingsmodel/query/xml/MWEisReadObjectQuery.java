/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
******************************************************************************/
package org.eclipse.persistence.tools.workbench.mappingsmodel.query.xml;

import org.eclipse.persistence.tools.workbench.mappingsmodel.query.MWReadObjectQuery;

import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.queries.ObjectLevelReadQuery;
import org.eclipse.persistence.queries.ReadObjectQuery;

public final class MWEisReadObjectQuery 
	extends MWAbstractEisReadQuery 
	implements MWReadObjectQuery
{
	
	// **************** Static methods ****************	

	public static XMLDescriptor buildDescriptor() {
		XMLDescriptor descriptor = new XMLDescriptor();
		descriptor.setJavaClass(MWEisReadObjectQuery.class);	
		descriptor.getInheritancePolicy().setParentClass(MWAbstractEisReadQuery.class);
		
		return descriptor;
	}
	
	// **************** Constructors ****************	
	
	/** Default constructor - for TopLink use only. */			
	private MWEisReadObjectQuery() {
		super();
	}

	MWEisReadObjectQuery(MWEisQueryManager parent, String name) {
		super(parent, name);
	}


	// ******************* Morphing *******************
	
	
	public String queryType() {
		return READ_OBJECT_QUERY;
	}
	
	public MWReadObjectQuery asReadObjectQuery() {
		return this;
	}
	
	
	// **************** Runtime Conversion ****************	
	
	protected ObjectLevelReadQuery buildRuntimeQuery() {
		return new ReadObjectQuery();
	}

}
