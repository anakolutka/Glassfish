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
package amxtest;

import org.testng.annotations.*;
import org.testng.Assert;

import javax.management.ObjectName;
import javax.management.AttributeList;
import javax.management.Attribute;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.glassfish.admin.amx.intf.config.*;
import org.glassfish.admin.amx.core.*;
import org.glassfish.admin.amx.base.*;
import org.glassfish.admin.amx.config.*;
//import org.glassfish.admin.amx.j2ee.*;
import org.glassfish.admin.amx.monitoring.*;
import org.glassfish.admin.amx.util.CollectionUtil;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.util.ListUtil;
import org.glassfish.admin.amx.util.MapUtil;
import org.glassfish.admin.amx.util.TypeCast;
import org.glassfish.admin.amx.logging.Logging;
import org.glassfish.admin.amx.annotation.*;


/** 
    Miscellaneous tests should go into this file, or another one like it.
 */
@Test(
    sequential=false, threadPoolSize=16,
    groups =
    {
        "amx"
    },
    description = "tests for AMXConfigProxy"
)
public final class AMXConfigProxyTests extends AMXTestBase
{
    public AMXConfigProxyTests()
    {
    }

     /** test all MBeans generically */
    @Test
    public void testForBogusConfigAnnotations()
    {
        final List<Class<? extends AMXProxy>> interfaces = getInterfaces().all();
        
        // AMXConfigProxy sub-interfaces should not use @ManagedAttribute or @ManagedOperation;
        // all such info is derived only from the ConfigBean.
        for( final Class<? extends AMXProxy>  intf : interfaces )
        {
            if ( ! AMXConfigProxy.class.isAssignableFrom(intf) ) continue;
            
            final Method[] methods = intf.getDeclaredMethods(); // declared methods only
            for( final Method m : methods )
            {
                final ManagedAttribute ma = m.getAnnotation(ManagedAttribute.class);
                final ManagedOperation mo = m.getAnnotation(ManagedOperation.class);
                final String desc = intf.getName() + "." + m.getName() + "()";
                
                assert ma == null :  "Config MBeans do not support @ManagedAttribute: " + desc;
                assert mo == null :  "Config MBeans do not support @ManagedOperation: " + desc;
            }
        }
    }
    
    
    private void _checkDefaultValues(final AMXConfigProxy amxConfig)
    {
        final String objectName = amxConfig.objectName().toString();

        // test the Map keyed by XML attribute name
        final Map<String, String> defaultValuesXML = amxConfig.getDefaultValues(false);
        for (final String attrName : defaultValuesXML.keySet())
        {
            // no default value should ever be null

            assert defaultValuesXML.get(attrName) != null :
            "null value for attribute " + attrName + " in " + objectName;
        }

        // test the Map keyed by AMXProxy attribute name
        final Map<String, String> defaultValuesAMX = amxConfig.getDefaultValues(true);

        assert defaultValuesXML.size() == defaultValuesAMX.size();
        for (final String attrName : defaultValuesAMX.keySet())
        {
            // no default value should ever be null

            assert defaultValuesAMX.get(attrName) != null :
            "null value for attribute " + attrName + " in " + objectName;
        }
    }

    private void _checkAttributeResolver(final AMXConfigProxy amxConfig)
    {
        final Set<String> attrNames = amxConfig.attributeNames();
        for (final String attrName : attrNames)
        {
            final String resolvedValue = amxConfig.resolveAttribute(attrName);
            if (resolvedValue != null)
            {
                // crude check
                assert resolvedValue.indexOf("${") < 0 :
                "Attribute " + attrName + " did not resolve: " + resolvedValue;
            }
        }

        final AttributeList attrsList = amxConfig.resolveAttributes( SetUtil.toStringArray(attrNames) );
        for (final Object o : attrsList)
        {
            final Attribute a = (Attribute) o;
            final String resolvedValue = "" + a.getValue();
            if (resolvedValue != null)
            {
                // crude check
                assert resolvedValue.indexOf("${") < 0 :
                "Attribute " + a.getName() + " did not resolve: " + resolvedValue;
            }
        }
    }
    
    
    @Test
    public void testAMXConfigDefaultValues()
    {
        for( final AMXConfigProxy amx : getAllConfig() )
        {
            _checkDefaultValues( amx );
        }
    }
    
    @Test
    public void testAMXConfigAttributeResolver()
    {
        for( final AMXConfigProxy amx : getAllConfig() )
        {
            _checkAttributeResolver( amx );
        }
    }
    
    
    @Test
    public void testConfigTools()
    {
        final ConfigTools ct = getDomainRootProxy().getExt().child(ConfigTools.class);
        
        final String[] namedTypes = ct.getConfigNamedTypes();
        assert namedTypes.length >= 13 :
            "Expecting at least 13 named types, got " + namedTypes.length + " = " + CollectionUtil.toString(SetUtil.newStringSet(namedTypes), ", ");
        
        final String[] resourceTypes = ct.getConfigResourceTypes();
        assert resourceTypes.length >= 10 :
            "Expecting at least 10 resource types, got " + resourceTypes.length + " = " + CollectionUtil.toString(SetUtil.newStringSet(resourceTypes), ", ");
                
        // create the properties List
        final List<Map<String,String>> props = new ArrayList<Map<String,String>>();
        final String NAME_PREFIX = "testConfigTools-";
        for( int i = 0 ; i < 10; ++i )
        {
            final Map<String,String>  m = new HashMap<String,String>();
            m.put( "Name", NAME_PREFIX + i );
            m.put( "Value", "value_" + i );
            m.put( "Description", "blah blah blah " + i );
            props.add( m );
        }
        
        // create a large number of properties, then remove them
        final Domain domainConfig = getDomainConfig();
        
        // remove first, in case they were left over from a failure.
        for( final Map<String,String> prop : props )
        {
            domainConfig.removeChild( "property", prop.get("Name") );
            domainConfig.removeChild( "system-property", prop.get("Name") );
        }

        // create them as properties and system properties
        ct.setProperties( domainConfig.objectName(), props, false );
        ct.setSystemProperties( domainConfig.objectName(), props, false );
        
        for( final Map<String,String> prop : props )
        {
            assert domainConfig.removeChild( "property", prop.get("Name") ) != null;
            assert domainConfig.removeChild( "system-property", prop.get("Name") ) != null;
        }
    }
    
    private Map<String,Object> newPropertyMap(final String name)
    {
        final Map<String,Object>    m = MapUtil.newMap();
        
        m.put( "Name", name );
        m.put( "Value", name + "-value" );
        m.put( "Description", "desc.for." + name );
        
        return m;
    }
    
    private Map<String,Object>[] newPropertyMaps(final String baseName, final int count)
    {
        final Map<String,Object>[] maps = TypeCast.asArray( new Map[count] );
        for( int i = 0; i < count; ++i )
        {
            maps[i] = newPropertyMap(baseName + i);
        }
        return maps;
    }
    
    @Test
    public void testCreateProperty()
    {
        final Domain amx = getDomainConfig();
        
        final String PROP_NAME = "AMXConfigProxyTests.TEST_PROP1";
        final String propType = Util.deduceType(Property.class);
        // remove any existing test element
        if ( amx.childrenMap(Property.class).get(PROP_NAME) != null )
        {
            try
            {
                amx.removeChild( propType, PROP_NAME );
                System.out.println( "Removed stale test config " + PROP_NAME );
            }
            catch( final Exception e )
            {
               assert false : "Unable to remove config " + PROP_NAME + ": " + e;
            }
        }
        
        final Map<String,Object> attrs = newPropertyMap(PROP_NAME);
        
        final AMXConfigProxy prop = amx.createChild( propType, attrs );
        assert prop.getName().equals(PROP_NAME);
        assert amx.childrenMap(Property.class).get(PROP_NAME) != null;
        
        amx.removeChild( propType, PROP_NAME );
        assert amx.childrenMap(Property.class).get(PROP_NAME) == null;
    }
    

    private void removeChild( final AMXConfigProxy amx, final String type, final String name )
    {
        if ( amx.childrenMap(type).get(name) != null )
        {
            try
            {
                amx.removeChild( type, name );
                System.out.println( "Removed stale test config " + name );
            }
            catch( final Exception e )
            {
               assert false : "Unable to remove config " + name + ": " + e;
            }
        }
    }
    
    @Test
    /**
        Verify that a resource with properties can be created and removed.
     */
    public void testCreateResource()
    {
        final Resources parent = getDomainConfig().getResources();
        
        final String name = "AMXConfigProxyTests.test-resource";
        final String type = Util.deduceType(CustomResource.class);
        removeChild( parent, type, name );
        
        final Map<String,Object> attrs = MapUtil.newMap();
        attrs.put( "Name", name );  // IMPORTANT: this verifies that Name is mapped to jndi-name
        attrs.put( "ResType", "java.lang.Properties" );
        attrs.put( "ObjectType", "user" );
        attrs.put( "Enabled", "false" );
        attrs.put( "Description", "test" );
        attrs.put( "FactoryClass", "com.foo.bar.FooFactory" );
        
        // include two property children in the new resource
        final Map[] propMaps = new Map[2];
        String prop1 = "prop1"; String prop2 = "prop2";
        propMaps[0] = newPropertyMap(prop1);
        propMaps[1] = newPropertyMap(prop2);
        attrs.put( Util.deduceType(Property.class), propMaps);
                
        AMXConfigProxy child = parent.createChild( type, attrs );
        assert child.getName().equals(name);
        assert parent.childrenMap(type).get(name) != null;
        parent.removeChild( type, name );
        assert parent.childrenMap(type).get(name) == null;
        
        // do it again, but this time we'll specify the name as its ral key value instead of "Name"
        attrs.remove("Name");
        attrs.put( "JndiName", name);
        child = parent.createChild( type, attrs );
        assert child.getName().equals(name);
        assert parent.childrenMap(type).get(name) != null;
        parent.removeChild( type, name );
        assert parent.childrenMap(type).get(name) == null;
    }
    
    @Test
    public void testCreateProperties()
    {
        final Domain parent = getDomainConfig();
        
        // create 100 <property> children
        // this is in part ot ensure that the AMXValidator doens't barf when objects register/unregister
        final int NUM_PROPS = 100;
        final Map<String,Object>[] propMaps = newPropertyMaps( "AMXConfigProxyTests.testCreateProperties-", NUM_PROPS);
        
        final Set<String> propNames = new HashSet<String>();
        for( int i = 0; i < propMaps.length; ++i )
        {
            propNames.add( (String)propMaps[i].get("Name") );
        }
        
        final String propType = Util.deduceType(Property.class);
        // first remove any existing test elements
        final Map<String,Property> existing = parent.childrenMap(Property.class);
        for( final String propName : propNames )
        {
            if ( existing.get(propName) != null )
            {
                try
                {
                    parent.removeChild( propType, propName );
                    System.out.println( "Removed stale test config " + propName );
                }
                catch( final Exception e )
                {
                   assert false : "Unable to remove config " + propName + ": " + e;
                }
            }
        }
        
        final Map<String,Map<String,Object>[]>  childrenMaps = MapUtil.newMap();
        childrenMaps.put( propType, propMaps );
        final Map<String,Object> parentAttrs = MapUtil.newMap(); // FIXME  ad some
        parentAttrs.put( "Locale", "EN_US");
        parentAttrs.put( "LogRoot",  parent.getLogRoot() );
        
        /*
        ** FIXME: HK2 code broken, see bug 
        ** https://glassfish.dev.java.net/issues/show_bug.cgi?id=8923
        parentAttrs.put( "Foo",  new String[] { "hello", "world" } );   // test List<String> elements
        @Element
        List<String>  getFoo();
        void setFoo( List<String> value) throws PropertyVetoException;
        
        Need to test this on a special @Configured which has both sub-elements and @Element List<String>
        */
        System.out.println( "SKIPPING test for String[] parameters due to HK2 bug #8923" );
        
        final AMXConfigProxy[] newChildren = parent.createChildren( childrenMaps, parentAttrs);
        final int numExpected = propMaps.length;
        assert newChildren.length == numExpected : "Expected " + numExpected + ", got " + newChildren.length;
        final Map<String,Property>  childrenProps = parent.childrenMap(Property.class);
        for( final String propName : propNames )
        {
            assert childrenProps.get(propName) != null : "property not created: " + propName;
        }
        
        for( final String propName : propNames )
        {
            parent.removeChild( propType, propName );
        }
        // verify that they're all gone
        final Map<String,Property>  remaining = parent.childrenMap(Property.class);
        for( final String propName : propNames )
        {
            parent.removeChild( propType, propName );
            assert remaining.get(propName) == null;
        }
    }
    
    
    
    /**
        Tests creating a whole config hiearchy
     */
    @Test
    public void createChildTest()
    {
        final Configs configs = getDomainConfig().getConfigs();
        
        final String configName = "AMXConfigProxyTests.TEST";
        final String type = Util.deduceType(Config.class);
        removeChild( configs, type, configName );
        
        final Map<String,Object>  configParams = MapUtil.newMap();
        configParams.put( "Name", configName );
        configParams.put( Util.deduceType(Property.class), newPropertyMaps("prop", 5) );
        configParams.put("DynamicReconfigurationEnabled", false );
                
        final Config child = configs.createChild( type, configParams ).as(Config.class);

        
        /*
        // create a new ConnectorConnectionPool with a SecurityMap containing a BackendPrincipal
        final Map<String,Object> params = new HashMap<String,Object>();
        params.put( "Name", NAME );
        params.put( "ResourceAdapterName", NAME );
        params.put( "ConnectionDefinitionName", NAME );
        params.put( "SteadyPoolSize", 23 ); // check that it works
        
        final Map<String,Object> securityParams = new HashMap<String,Object>();
        securityParams.put( "Name", NAME );
        params.put( Util.deduceType(SecurityMap.class), securityParams );
        
        final Map<String,Object> backendParams = new HashMap<String,Object>();
        backendParams.put( "UserName", "testUser" );
        backendParams.put( "Password", "testPassword" );
        securityParams.put( Util.deduceType(BackendPrincipal.class), backendParams );
        
        */
        
       //final AMXConfigProxy result = configs.createChild( Util.deduceType(Config.class), configMap);
        
       // return result.objectName();
    }

}




































