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

/**
 *	This generated bean class PropertyElement matches the schema element property
 *
 *	Generated on Wed Aug 13 10:43:32 PDT 2003
 */

package com.sun.enterprise.tools.common.dd.ejb;

import org.w3c.dom.*;
import org.netbeans.modules.schema2beans.*;
import java.beans.*;
import java.util.*;

// BEGIN_NOI18N

public class PropertyElement extends com.sun.enterprise.tools.common.dd.SunBaseBean
{

	static Vector comparators = new Vector();

	static public final String NAME = "Name";	// NOI18N
	static public final String VALUE = "Value";	// NOI18N

	public PropertyElement() {
		this(Common.USE_DEFAULT_VALUES);
	}

	public PropertyElement(int options)
	{
		super(comparators, new org.netbeans.modules.schema2beans.Version(1, 2, 0));
		// Properties (see root bean comments for the bean graph)
		this.createProperty("name", 	// NOI18N
			NAME, 
			Common.TYPE_1 | Common.TYPE_STRING | Common.TYPE_KEY, 
			String.class);
		this.createProperty("value", 	// NOI18N
			VALUE, 
			Common.TYPE_1 | Common.TYPE_STRING | Common.TYPE_KEY, 
			String.class);
		this.initialize(options);
	}

	// Setting the default values of the properties
	void initialize(int options)
	{
		
	}

	// This attribute is mandatory
	public void setName(String value) {
		this.setValue(NAME, value);
	}

	//
	public String getName() {
		return (String)this.getValue(NAME);
	}

	// This attribute is mandatory
	public void setValue(String value) {
		this.setValue(VALUE, value);
	}

	//
	public String getValue() {
		return (String)this.getValue(VALUE);
	}

	//
	public static void addComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
		comparators.add(c);
	}

	//
	public static void removeComparator(org.netbeans.modules.schema2beans.BeanComparator c) {
		comparators.remove(c);
	}
	public void validate() throws org.netbeans.modules.schema2beans.ValidateException {
		boolean restrictionFailure = false;
		// Validating property name
		if (getName() == null) {
			throw new org.netbeans.modules.schema2beans.ValidateException("getName() == null", "name", this);	// NOI18N
		}
		// Validating property value
		if (getValue() == null) {
			throw new org.netbeans.modules.schema2beans.ValidateException("getValue() == null", "value", this);	// NOI18N
		}
	}

	// Dump the content of this bean returning it as a String
	public void dump(StringBuffer str, String indent){
		String s;
		Object o;
		org.netbeans.modules.schema2beans.BaseBean n;
		str.append(indent);
		str.append("Name");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append("<");	// NOI18N
		s = this.getName();
		str.append((s==null?"null":s.trim()));	// NOI18N
		str.append(">\n");	// NOI18N
		this.dumpAttributes(NAME, 0, str, indent);

		str.append(indent);
		str.append("Value");	// NOI18N
		str.append(indent+"\t");	// NOI18N
		str.append("<");	// NOI18N
		s = this.getValue();
		str.append((s==null?"null":s.trim()));	// NOI18N
		str.append(">\n");	// NOI18N
		this.dumpAttributes(VALUE, 0, str, indent);

	}
	public String dumpBeanNode(){
		StringBuffer str = new StringBuffer();
		str.append("PropertyElement\n");	// NOI18N
		this.dump(str, "\n  ");	// NOI18N
		return str.toString();
	}}

// END_NOI18N


/*
		The following schema file has been used for generation:

<!--
  XML DTD for Sun ONE Application Server specific EJB jar module 
  deployment descriptor. This is a companion DTD for ejb-jar_2_1.xsd

  $Revision: 1.3 $
-->

<!--
This is the root element of the ejb module descriptor document.
-->
<!ELEMENT sun-ejb-jar (security-role-mapping*, enterprise-beans) >

<!-- 
System unique object id. Automatically generated and updated at deployment/redeployment 
-->
<!ELEMENT unique-id (#PCDATA)>

<!--
This is the root element describing all the runtime of an ejb-jar in the application.
-->
<!ELEMENT enterprise-beans (name?, unique-id?, ejb*, pm-descriptors?, cmp-resource?,
	message-destination*, webservice-description*)>

<!--
This is the element describing runtime bindings for a single ejb.

Properties applicable to all types of beans:
     ejb-name, ejb-ref*, jndi-name, resource-ref*, resource-env-ref*, pass-by-reference?, 
     ior-security-config?, gen-classes?, service-ref*

Additional properties applicable to a stateless session bean:
    bean-pool, webservice-endpoint

Additional properties applicable to a stateful session bean:
    bean-cache, webservice-endpoint

Additional properties applicable to an entity bean:
   is-read-only-bean?, refresh-period-in-seconds?, cmp?, commit-option?, bean-cache?, bean-pool?

Additional properties applicable to a message-driven bean:
   mdb-resource-adapter?, mdb-connection-factory?, jms-durable-subscription-name?, 
   jms-max-messages-load?, bean-pool?
   ( In case of MDB, jndi-name is the jndi name of the associated jms destination )
-->

<!ELEMENT ejb (ejb-name, jndi-name?, ejb-ref*, resource-ref*, resource-env-ref*, service-ref*, pass-by-reference?, 
               cmp?, principal?, mdb-connection-factory?, jms-durable-subscription-name?, 
               jms-max-messages-load?, ior-security-config?, is-read-only-bean?, 
               refresh-period-in-seconds?, commit-option?, cmt-timeout-in-seconds?, gen-classes?, 
               bean-pool?, bean-cache?, mdb-resource-adapter?, webservice-endpoint*, session?)>
<!--
The text in this element matches the ejb-name of the ejb to which it refers in ejb-jar.xml.
-->
<!ELEMENT ejb-name (#PCDATA)>

<!--
The text in this element is a true/false flag for read only beans.
-->
<!ELEMENT is-read-only-bean (#PCDATA)>

<!--
This is the root element which binds an ejb reference to a jndi name.
-->
<!ELEMENT ejb-ref (ejb-ref-name, jndi-name)>

<!--
The ejb ref name locates the name of the ejb reference in the application.
-->
<!ELEMENT ejb-ref-name (#PCDATA)>

<!--
This element describes runtime information for a CMP EntityBean object for 
EJB1.1 and EJB2.0 beans.  
-->
<!ELEMENT cmp (mapping-properties?, is-one-one-cmp?, one-one-finders?)>

<!--
This contains the location of the persistence vendor specific O/R mapping file
-->
<!ELEMENT mapping-properties (#PCDATA)>

<!--
This element in deprecated. It has been left in the DTD for validation purposes.
Any value will be ignored by the runtime.
-->
<!ELEMENT is-one-one-cmp (#PCDATA)>

<!--
This root element contains the finders for CMP 1.1.
-->
<!ELEMENT one-one-finders (finder+ )>

<!--
This root element contains the finder for CMP 1.1 with a method-name and query parameters
-->
<!ELEMENT finder (method-name, query-params?, query-filter?, query-variables?,  query-ordering?)>

<!--
This contains the query parameters for CMP 1.1 finder
-->
<!ELEMENT query-params (#PCDATA)>

<!--
This optional element contains the query filter for CMP 1.1 finder
-->
<!ELEMENT query-filter (#PCDATA)>

<!--
This optional element contains variables in query expression for CMP 1.1 finder
-->
<!ELEMENT query-variables (#PCDATA)>

<!--
This optional element contains the ordering specification for CMP 1.1 finder.
-->

<!ELEMENT query-ordering (#PCDATA)>

<!--
This element identifies the database and the policy for processing CMP beans
storage. The jndi-name element identifies either the persistence-manager-
factory-resource or the jdbc-resource as defined in the server configuration.
-->
<!ELEMENT cmp-resource (jndi-name, default-resource-principal?, property*,
        create-tables-at-deploy?, drop-tables-at-undeploy?,
        database-vendor-name?, schema-generator-properties?)>

<!--
This element contains the override properties for the schema generation 
from CMP beans in this module.
-->
<!ELEMENT schema-generator-properties (property*) >

<!--
This element specifies whether automatic creation of tables for the CMP beans 
is done at module deployment. Acceptable values are true or false
-->
<!ELEMENT create-tables-at-deploy ( #PCDATA )>

<!--
This element specifies whether automatic dropping of tables for the CMP beans 
is done at module undeployment. Acceptabel values are true of false
-->
<!ELEMENT drop-tables-at-undeploy ( #PCDATA )>

<!--
This element specifies the database vendor name for ddl files generated at
module deployment. Default is SQL92.
-->
<!ELEMENT database-vendor-name ( #PCDATA )>

<!--
This element specifies the connection factory associated with a message-driven bean.
-->
<!ELEMENT mdb-connection-factory (jndi-name, default-resource-principal?)>

<!--
This node holds information about a logical message destination
-->
<!ELEMENT message-destination (message-destination-name, jndi-name)>

<!--
This node holds the name of a logical message destination
-->
<!ELEMENT message-destination-name (#PCDATA)>

<!--
Specifies the name of a durable subscription associated with a message-driven bean's 
destination.  Required for a Topic destination, if subscription-durability is set to 
Durable (in ejb-jar.xml)
-->
<!ELEMENT jms-durable-subscription-name (#PCDATA)>

<!--
A string value specifies the maximum number of messages to load into a JMS session 
at one time for a message-driven bean to serve. If not specified, the default is 1.
-->
<!ELEMENT jms-max-messages-load (#PCDATA)>

<!--
This element contains all the generated class names for a bean.
-->
<!ELEMENT gen-classes ( remote-impl?, local-impl?, remote-home-impl?, local-home-impl? )>

<!--
This contains the fully qualified class name of the generated EJBObject impl class. 
-->
<!ELEMENT remote-impl (#PCDATA)>

<!--
This contains the fully qualified class name of the generated EJBLocalObject impl class. 
-->
<!ELEMENT local-impl (#PCDATA)>

<!--
This contains the fully qualified class name of the generated EJBHome impl class. 
-->
<!ELEMENT remote-home-impl (#PCDATA)>

<!--
This contains the fully qualified class name of the generated EJBLocalHome impl class. 
-->
<!ELEMENT local-home-impl (#PCDATA)>

<!--
This contains the bean cache properties. Used only for entity beans and stateful session beans
-->
<!ELEMENT bean-cache (max-cache-size?, resize-quantity?, is-cache-overflow-allowed?, cache-idle-timeout-in-seconds?, removal-timeout-in-seconds?, victim-selection-policy?)>

<!--
max-cache-size defines the maximum number of beans in the cache. Should be greater than 1.
Default is 512.
-->
<!ELEMENT max-cache-size (#PCDATA)>

<!--
is-cache-overflow-allowed is a boolean which indicates if the cache size is a hard limit or not. 
Default is true i.e there is no hard limit. max-cache-size is a hint to the cache implementation.
-->
<!ELEMENT is-cache-overflow-allowed (#PCDATA)>

<!--
cache-idle-timeout-in-seconds specifies the maximum time that a stateful session bean or 
entity bean is allowed to be idle in the cache. After this time, the bean is passivated 
to backup store. This is a hint to server. Default value for cache-idle-timeout-in-seconds 
is 600 seconds.
-->
<!ELEMENT cache-idle-timeout-in-seconds (#PCDATA)>


<!--
The amount of time that the bean remains passivated (i.e. idle in the backup store) is 
controlled by removal-timeout-in-seconds parameter.  Note that if a bean was not accessed beyond 
removal-timeout-in-seconds, then it will be removed from the backup store and hence will not 
be accessible to the client. The Default value for removal-timeout-in-seconds is 60min.
-->
<!ELEMENT removal-timeout-in-seconds (#PCDATA)>

<!--
victim-selection-policy specifies the algorithm to use to pick victims. 
Possible values are FIFO | LRU | NRU. Default is NRU, which is actually 
pseudo-random selection policy.
-->
<!ELEMENT victim-selection-policy (#PCDATA)>

<!--
bean-pool is a root element containing the bean pool properties. Used only for stateless session 
bean and message-driven bean pools.
-->
<!ELEMENT bean-pool (steady-pool-size?, resize-quantity?, max-pool-size?, pool-idle-timeout-in-seconds?, max-wait-time-in-millis?)>

<!--
steady-pool-size specified the initial and minimum number of beans that must be maintained in the pool. 
Valid values are from 0 to MAX_INTEGER. 
-->
<!ELEMENT steady-pool-size (#PCDATA)>

<!--
resize-quantity specifies the number of beans to be created or deleted when the pool 
or cache is being serviced by the server. Valid values are from 0 to MAX_INTEGER and 
subject to maximum size limit).  Default is 16.
-->
<!ELEMENT resize-quantity (#PCDATA)>

<!--
max-pool-size speifies the maximum pool size. Valid values are from 0 to MAX_INTEGER. 
Default is 64.
-->
<!ELEMENT max-pool-size (#PCDATA)>

<!--
pool-idle-timeout-in-seconds specifies the maximum time that a stateless session bean or 
message-driven bean is allowed to be idle in the pool. After this time, the bean is 
passivated to backup store.  This is a hint to server. Default value for 
pool-idle-timeout-in-seconds is 600 seconds.
-->
<!ELEMENT pool-idle-timeout-in-seconds (#PCDATA)>

<!--
A string field whose valid values are either "B", or "C". Default is "B" 
-->
<!ELEMENT commit-option (#PCDATA)>

<!--
Specifies the timeout for transactions started by the container. This value must be greater than zero, else it will be ignored by the container.
-->
<!ELEMENT cmt-timeout-in-seconds (#PCDATA)>

<!--
Specifies the maximum time that the caller is willing to wait to get a bean from the pool.
Wait time is infinite, if the value specified is 0. Deprecated.
-->
<!ELEMENT max-wait-time-in-millis (#PCDATA)>

<!--
refresh-period-in-seconds specifies the rate at which the read-only-bean must be refreshed 
from the data source. 0 (never refreshed) and positive (refreshed at specified intervals).
Specified value is a hint to the container.  Default is 600 seconds.
-->
<!ELEMENT refresh-period-in-seconds (#PCDATA)>

<!--
Specifies the jndi name string.
-->
<!ELEMENT  jndi-name (#PCDATA)>

<!--
This text nodes holds a name string.
-->
<!ELEMENT name (#PCDATA)>

<!--
This element holds password text.
-->
<!ELEMENT password (#PCDATA)>

<!--
This node describes a username on the platform.
-->
<!ELEMENT principal (name)>

<!-- 
security-role-mapping element maps the user principal or group 
to a different principal on the server. 
-->
<!ELEMENT security-role-mapping (role-name, (principal-name | group-name)+)> 

<!-- 
role-name specifies an accepted role 
-->
<!ELEMENT role-name (#PCDATA)> 

<!-- 
principal-name specifies a valid principal 
-->
<!ELEMENT principal-name (#PCDATA)> 

<!-- 
group-name specifies a valid group name 
-->
<!ELEMENT group-name (#PCDATA)> 

<!--
The name of a resource reference.
-->
<!ELEMENT res-ref-name (#PCDATA)>

<!--
resource-env-ref holds all the runtime bindings of a resource env reference.
-->
<!ELEMENT resource-env-ref ( resource-env-ref-name, jndi-name )>

<!--
name of a resource env reference.
-->
<!ELEMENT resource-env-ref-name (#PCDATA)>

<!--
resource-ref node holds all the runtime bindings of a resource reference.
-->
<!ELEMENT resource-ref  (res-ref-name, jndi-name, default-resource-principal?)>

<!--
user name and password to be used when none are specified while accesing a resource
-->
<!ELEMENT default-resource-principal ( name,  password)>

<!--
ior-security-config element describes the security configuration information for the IOR.
-->  
<!ELEMENT ior-security-config ( transport-config? , as-context?, sas-context?  )> 

<!--
transport-config is the root element for security between the end points
-->
<!ELEMENT transport-config ( integrity, confidentiality, establish-trust-in-target, establish-trust-in-client )> 

<!--
integrity element indicates if the server (target) supports integrity protected messages. 
The valid values are NONE, SUPPORTED or REQUIRED
-->  
<!ELEMENT integrity ( #PCDATA)>

<!--
confidentiality element indicates if the server (target) supports privacy protected 
messages. The values are NONE, SUPPORTED or REQUIRED
-->  
<!ELEMENT confidentiality ( #PCDATA)>

<!--
establish-trust-in-target element indicates if the target is capable of authenticating to a client. 
The values are NONE or SUPPORTED.
-->  
<!ELEMENT establish-trust-in-target ( #PCDATA)>

<!--
establish-trust-in-client element indicates if the target is capable of authenticating a client. The
values are NONE, SUPPORTED or REQUIRED.
-->  
<!ELEMENT establish-trust-in-client ( #PCDATA)>

<!--
as-context (CSIv2 authentication service) is the element describing the authentication 
mechanism that will be used to authenticate the client. If specified it will be the 
username-password mechanism.
-->  
<!ELEMENT as-context ( auth-method, realm, required )> 

<!--
required element specifies if the authentication method specified is required
to be used for client authentication. If so the EstablishTrustInClient bit
will be set in the target_requires field of the AS_Context. The element value
is either true or false. 
-->  
<!ELEMENT required ( #PCDATA )> 

<!--
auth-method element describes the authentication method. The only supported value
is USERNAME_PASSWORD
-->  
<!ELEMENT auth-method ( #PCDATA )> 

<!--
realm element describes the realm in which the user is authenticated. Must be 
a valid realm that is registered in server configuration.
-->  
<!ELEMENT realm ( #PCDATA )> 

<!--
sas-context (related to CSIv2 security attribute service) element describes 
the sas-context fields.
-->  
<!ELEMENT sas-context ( caller-propagation )> 

<!--
caller-propagation element indicates if the target will accept propagated caller identities
The values are NONE or SUPPORTED.
-->  
<!ELEMENT caller-propagation ( #PCDATA) >

<!-- 
pass-by-reference elements controls use of Pass by Reference semantics.  
EJB spec requires pass by value, which will be the default mode of operation. 
This can be set to true for non-compliant operation and possibly higher 
performance. For a stand-alone server, this can be used. By setting a similarly 
named element at sun-application.xml, it can apply to all the enclosed ejb 
modules. Allowed values are true and false. Default will be false.
 -->
<!ELEMENT pass-by-reference (#PCDATA)>

<!--
PM descriptors contain one or more pm descriptors, but only one of them must
be in use at any given time. If not specified, the Sun ONE CMP is used.
-->
<!ELEMENT pm-descriptors ( pm-descriptor+, pm-inuse)>

<!--
pm-descriptor describes the pluggable vendor implementation for the CMP 
support of the CMP entity beans in this module.
-->
<!ELEMENT pm-descriptor ( pm-identifier, pm-version, pm-config?, pm-class-generator, pm-mapping-factory?)>

<!--
pm-identifier element identifies the vendor who provides the CMP implementation
-->
<!ELEMENT pm-identifier (#PCDATA)>

<!--
pm-version further specifies which version of PM vendor product to be used
-->
<!ELEMENT pm-version (#PCDATA)>

<!--
pm-config specifies the vendor specific config file to be used
-->
<!ELEMENT pm-config (#PCDATA)>

<!--
pm-class-generator specifies the vendor specific class generator to be used
at the module deploymant time. This is the name of the class specific to this 
vendor.
-->
<!ELEMENT pm-class-generator (#PCDATA)>

<!--
pm-mapping-factory specifies the vendor specific mapping factory
This is the name of the class specific to a vendor.
-->
<!ELEMENT pm-mapping-factory (#PCDATA)>

<!--
pm-inuse specifies which CMP vendor is used.
-->
<!ELEMENT pm-inuse (pm-identifier, pm-version)>


<!--
This holds the runtime configuration properties of the message-driven bean 
in its operation environment.  For example, this may include information 
about the name of a physical JMS destination etc.
Defined this way to match the activation-config on the standard
deployment descriptor for message-driven bean.
-->
<!ELEMENT activation-config ( description?, activation-config-property+ ) >

<!--
provide an element description
-->
<!ELEMENT description (#PCDATA)>

<!--
This hold a particular activation config propery name-value pair
-->
<!ELEMENT activation-config-property ( 
  activation-config-property-name, activation-config-property-value ) >

<!--
This holds the name of a runtime activation-config property
-->
<!ELEMENT activation-config-property-name ( #PCDATA ) >

<!--
This holds the value of a runtime activation-config property
-->
<!ELEMENT activation-config-property-value ( #PCDATA ) >

<!--
This node holds the module ID of the resource adapter that
is responsible for delivering messages to the message-driven
bean, as well as the runtime configuration information for
the mdb.
-->
<!ELEMENT mdb-resource-adapter ( resource-adapter-mid, activation-config? )>

<!--
This node holds the module ID of the resource adapter that is responsible 
for delivering messages to the message-driven bean.
-->
<!ELEMENT resource-adapter-mid ( #PCDATA ) >

<!-- 
Generic name-value pairs property
-->
<!ELEMENT property ( name, value ) >

<!--
This text nodes holds a value string.
-->
<!ELEMENT value (#PCDATA)>



<!--
  			W E B   S E R V I C E S 
--> 					
<!--
Information about a web service endpoint.  
-->
<!ELEMENT webservice-endpoint ( port-component-name, endpoint-address-uri?, login-config?, transport-guarantee?, service-qname?, tie-class?, servlet-impl-class? )>

<!--
Unique name of a port component within a module
-->
<!ELEMENT port-component-name ( #PCDATA )>

<!--
Relative path combined with web server root to form fully qualified
endpoint address for a web service endpoint.  For servlet endpoints, this
value is relative to the servlet's web application context root.  In
all cases, this value must be a fixed pattern(i.e. no "*" allowed).
If the web service endpoint is a servlet that only implements a single
endpoint has only one url-pattern, it is not necessary to set 
this value since the container can derive it from web.xml.
-->
<!ELEMENT endpoint-address-uri ( #PCDATA )>

<!--
The name of tie implementation class for a port-component.  This is
not specified by the deployer.  It is derived during deployment.
-->
<!ELEMENT tie-class (#PCDATA)>

<!-- 
The service-qname element declares the specific WSDL service
element that is being refered to.  It is not set by the deployer.
It is derived during deployment.
-->
<!ELEMENT service-qname (namespaceURI, localpart)>

<!--
The localpart element indicates the local part of a QNAME.
-->
<!ELEMENT localpart (#PCDATA)>

<!--
The namespaceURI element indicates a URI.
-->
<!ELEMENT namespaceURI (#PCDATA)>

<!-- 
Optional authentication configuration for an EJB web service endpoint.
Not needed for servet web service endpoints.  Their security configuration
is contained in the standard web application descriptor.
-->
<!ELEMENT login-config ( auth-method )>

<!--
Name of application-written servlet impl class contained in deployed war.
This is not set by the deployer.  It is derived by the container
during deployment.
-->
<!ELEMENT servlet-impl-class (#PCDATA)>

<!--
Runtime settings for a web service reference.  In the simplest case,
there is no runtime information required for a service ref.  Runtime info
is only needed in the following cases :
 * to define the port that should be used to resolve a container-managed port
 * to define default Stub/Call property settings for Stub objects
 * to define the URL of a final WSDL document to be used instead of
the one packaged with a service-ref
-->
<!ELEMENT service-ref ( service-ref-name, port-info*, call-property*, wsdl-override?, service-impl-class?, service-qname? )>

<!--
Coded name (relative to java:comp/env) for a service-reference
-->
<!ELEMENT service-ref-name ( #PCDATA )>

<!--
Name of generated service implementation class. This is not set by the 
deployer. It is derived during deployment.
-->
<!ELEMENT service-impl-class ( #PCDATA )>

<!-- 
Information for a port within a service-reference.

Either service-endpoint-interface or wsdl-port or both
(service-endpoint-interface and wsdl-port) should be specified.  

If both are specified, wsdl-port represents the
port the container should choose for container-managed port selection.

The same wsdl-port value must not appear in
more than one port-info entry within the same service-ref.

If a particular service-endpoint-interface is using container-managed port
selection, it must not appear in more than one port-info entry
within the same service-ref.

-->
<!ELEMENT port-info ( service-endpoint-interface?, wsdl-port?, stub-property*, call-property* )>

<!--
Fully qualified name of service endpoint interface
-->
<!ELEMENT service-endpoint-interface ( #PCDATA )>

<!--
Specifies that the communication between client and server should 
be NONE, INTEGRAL, or CONFIDENTIAL. NONE means that the application 
does not require any transport guarantees. A value of INTEGRAL means 
that the application requires that the data sent between the client 
and server be sent in such a way that it can't be changed in transit. 
CONFIDENTIAL means that the application requires that the data be 
transmitted in a fashion that prevents other entities from observing 
the contents of the transmission. In most cases, the presence of the 
INTEGRAL or CONFIDENTIAL flag will indicate that the use of SSL is 
required.
-->
<!ELEMENT transport-guarantee ( #PCDATA )>


<!-- 
Port used in port-info.  
-->
<!ELEMENT wsdl-port ( namespaceURI, localpart )>

<!-- 
JAXRPC property values that should be set on a stub before it's returned to 
to the web service client.  The property names can be any properties supported
by the JAXRPC Stub implementation. See javadoc for javax.xml.rpc.Stub
-->
<!ELEMENT stub-property ( name, value )>

<!-- 
JAXRPC property values that should be set on a Call object before it's 
returned to the web service client.  The property names can be any 
properties supported by the JAXRPC Call implementation.  See javadoc
for javax.xml.rpc.Call
-->
<!ELEMENT call-property ( name, value )>

<!-- 
Runtime information about a web service.  

wsdl-publish-location is optionally used to specify 
where the final wsdl and any dependent files should be stored.  This location
resides on the file system from which deployment is initiated.

-->
<!ELEMENT webservice-description ( webservice-description-name, wsdl-publish-location? )>

<!--
Unique name of a webservice within a module
-->
<!ELEMENT webservice-description-name ( #PCDATA )>

<!--
This is a valid URL pointing to a final WSDL document. It is optional.
If specified, the WSDL document at this URL will be used during
deployment instead of the WSDL document associated with the
service-ref in the standard deployment descriptor.

Examples :

  // available via HTTP
  <wsdl-override>http://localhost:8000/myservice/myport?WSDL</wsdl-override>

  // in a file
  <wsdl-override>file:/home/user1/myfinalwsdl.wsdl</wsdl-override>

-->
<!ELEMENT wsdl-override ( #PCDATA )>

<!--
file: URL of a directory to which a web-service-description's wsdl should be
published during deployment.  Any required files will be published to this
directory, preserving their location relative to the module-specific
wsdl directory(META-INF/wsdl or WEB-INF/wsdl).

Example :

  For an ejb.jar whose webservices.xml wsdl-file element contains
    META-INF/wsdl/a/Foo.wsdl 

  <wsdl-publish-location>file:/home/user1/publish
  </wsdl-publish-location>

  The final wsdl will be stored in /home/user1/publish/a/Foo.wsdl

-->
<!ELEMENT wsdl-publish-location ( #PCDATA )>


<!--
  			S E / E E   V e r s i o n   r e l a t e d   f i e l d s 
--> 
<!--
The session element specifies a Stateful enterprise bean's check pointing mechanism properties
Used in: ejb
-->
<!ELEMENT session ( checkpoint-location?, quick-checkpoint?, checkpointed-methods? )>

<!-- 
The checkpoint-location element specifies a Stateful enterprise bean's check pointing mechanism type. The checkpoint-location element must be one of the following: 
<checkpoint-location>end-of-transaction</checkpoint-location> 
<checkpoint-location>end-of-method</checkpoint-location>
<checkpoint-location>end-of-transaction-or-method</checkpoint-location> 
Default value: end-of-transaction
Used in:  session
--> 
<!ELEMENT checkpoint-location (#PCDATA)> 


<!-- 
The quick-checkpoint element specifies an enterprise bean's if ejbPassivate/ejbActivate must be called or not during check pointing.
The checkpointing-type element must be one of the two following:
        <quick-checkpoint>true</quick-checkpoint> 
        <quick-checkpoint>false</quick-checkpoint> 
Dafault: true
Used in:  session
--> 
<!ELEMENT quick-checkpoint (#PCDATA)> 

<!-- 
This element specifies which methods require checkpointing. The methods specified in here, will be checkpointed, when the checkpoint-location is set to end-of-method.

Default: none or all methods if failover is enabled at application or cluster/instance levels
Used in: session
--> 
<!ELEMENT checkpointed-methods (description?, method+)>

<!--

Method as defined in the standard deployment descriptors.

Used in : checkpointed-methods
-->
<!ELEMENT method (description?, method-intf?, method-name,
method-params?)>

<!--

The method-intf element allows a method element to differentiate
between the methods with the same name and signature that are multiply
defined across the component and home interfaces (e.g, in both an
enterprise bean's remote and local interfaces; in both an enterprise bean's
home and remote interfaces, etc.)

The method-intf element must be one of the following:

	<method-intf>Home</method-intf>
	<method-intf>Remote</method-intf>
	<method-intf>LocalHome</method-intf>
	<method-intf>Local</method-intf>

Used in: method
-->
<!ELEMENT method-intf (#PCDATA)>

<!--
The method-name element contains a name of an enterprise bean method
or the asterisk (*) character. The asterisk is used when the element
denotes all the methods of an enterprise bean's component and home
interfaces.

Used in: method, query-method
-->
<!ELEMENT method-name (#PCDATA)>

<!--
The method-param element contains the fully-qualified Java type name
of a method parameter.

Used in: method-params
-->
<!ELEMENT method-param (#PCDATA)>

<!--
The method-params element contains a list of the fully-qualified Java
type names of the method parameters.

Used in: method, query-method
-->
<!ELEMENT method-params (method-param*)>



*/
