/**
 *
 */
package org.glassfish.admin.mejb.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.j2ee.ListenerRegistration;
import javax.naming.Context;
import javax.naming.InitialContext;

import javax.management.j2ee.ManagementHome;
import org.glassfish.admin.mejb.MEJB;

/**
    Standalone MEJB test -- requires running server and disabling security on MEJB.
    
export V3M=/v3/glassfish/modules
export MAIN=org.glassfish.admin.mejb.test.MEJBTest
java -cp $V3M/gf-client.jar:$V3M/javax.management.j2ee.jar:target/MEJB.jar $MAIN

 */
public class MEJBTest {    
    private final MEJB mMEJB;
    
    public MEJBTest( final MEJB mejb )
    {
        mMEJB = mejb;
    }
    
    private void test() {
        try
        {
            _test();
        }
        catch( final Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    private void testMBean( final ObjectName objectName)
        throws Exception
    {
        println( "" );
        println( "" + objectName );
        
        final MEJB mejb = mMEJB;
        final MBeanInfo info = mejb.getMBeanInfo(objectName);
        final String[] attrNames = getAttributeNames( info.getAttributes() );
        
        println( "attributes: " + toString( newListFromArray(attrNames), ", " ) );
        
        final AttributeList list = mejb.getAttributes( objectName, attrNames );
        
        for( final String attrName : attrNames)
        {
            try
            {
                final Object value = mejb.getAttribute( objectName, attrName );
            }
            catch( Exception e )
            {
                println( "Attribute failed: " + attrName );
            }
        }
    }
    
    private void _test()
        throws Exception
    {
        final MEJB mejb = mMEJB;
        
        final String defaultDomain = mejb.getDefaultDomain();
        println("MEJB default domain = " + defaultDomain + ", MBeanCount = " + mejb.getMBeanCount() );
        
        final ListenerRegistration reg = mejb.getListenerRegistry();
        println( "Got ListenerRegistration: " + reg );
        final NotificationListener listener = new NotifListener();
        
        final String domain = "v3";
        final ObjectName pattern = newObjectName( domain + ":*" );
        final Set<ObjectName> items = mejb.queryNames( pattern, null);
        println("Queried " + pattern + ", got mbeans: " + items.size() );
        for( final ObjectName objectName : items )
        {
            if ( mejb.isRegistered(objectName) )
            {
                testMBean(objectName);
            }
        }
        
        // add listeners to all
        for( final ObjectName objectName : items )
        {
            if ( mejb.isRegistered(objectName) )
            {
                final NotificationFilter filter = null;
                final Object handback = null;
                reg.addNotificationListener( objectName, listener, filter, handback );
            }
        }
    }
    
    
    private static final class NotifListener implements NotificationListener
    {
        public NotifListener()
        {
        }
        
        public void handleNotification( final Notification notif, final Object handback )
        {
            System.out.println( "NotifListener: " + notif);
        }
    }


		public static String
	toString(
		final Collection<?> c,
		final String	 delim )
	{
        final StringBuffer buf = new StringBuffer();
        
        for( final Object item : c )
        {
            buf.append( "" + item );
            buf.append( delim );
        }
        if( c.size() != 0)
        {
            buf.setLength( buf.length() - delim.length() );
        }
        
        return buf.toString();
    }
            
		public static <T> List<T>
	newListFromArray( final T []  items )
	{
		final List<T>	list	= new ArrayList<T>();
		
		for( int i = 0; i < items.length; ++i )
		{
			list.add( items[ i ] );
		}

		return( list );
	}


	public static String []
	getAttributeNames( final MBeanAttributeInfo[]	infos  )
	{
		final String[]	names	= new String[ infos.length ];
		
		for( int i = 0; i < infos.length; ++i )
		{
			names[ i ]	= infos[ i ].getName();
		}
		
		return( names );
	}

    static ObjectName
	newObjectName( final String name )
	{
		try
		{
			return( new ObjectName( name ) ); 
		}
		catch( Exception e )
		{
			throw new RuntimeException( e.getMessage(), e );
		}
	}


    public static void main(String[] args) {
        try {
           // final Properties env = new Properties();
            //env.put("java.naming.factory.initial", "com.sun.jndi.cosnaming.CNCtxFactory");
            //env.put("java.naming.provider.url", "iiop://localhost:3700");
            final Context initial = new InitialContext();

            //final String mejbName = MEJBUtility.MEJB_DEFAULT_NAME;
            final String mejbName = "java:global/MEJB/MEJBBean";
            println("Looking up: " + mejbName);
            final Object objref = initial.lookup(mejbName);
           // println("Received from initial.lookup(): " + objref + " for " + mejbName);

            final ManagementHome home = (ManagementHome)objref;
            //println("ManagementHome: " + home + " for " + mejbName);
            final MEJB mejb = (MEJB)home.create();
            println("Got the MEJB");

            new MEJBTest( mejb ).test();

            mejb.remove();

        } catch (Exception ex) {
            System.err.println("Caught an unexpected exception!");
            ex.printStackTrace();
        }
    }

    /*
     lic static void main(String[] args) {
        try {
            final Properties env = new Properties();
            env.put("java.naming.factory.initial", "com.sun.jndi.cosnaming.CNCtxFactory");
            env.put("java.naming.provider.url", "iiop://localhost:3700");
            //env.put( Context.SECURITY_PRINCIPAL, "admin");
            //env.put( Context.SECURITY_CREDENTIALS, "adminadmin");
            //env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
            //env.put( Context.PROVIDER_URL, "iiop://localhost:3700");

            final Context initial = new InitialContext(env);

            final String mejbHomeName = "java:global/MEJB/MEJBBean!org.glassfish.admin.mejb.MEJBHome";
            final ManagementHome homerefref = (ManagementHome)initial.lookup(mejbHomeName);
            Management bean = homerefref.create();



            final String mejbName = MEJBUtility.MEJB_DEFAULT_NAME;
            //final String mejbName = "java:global/MEJB/MEJBBean";
            final Object objref = initial.lookup(mejbName);
            println("Received from initial.lookup(): " + objref + " for " + mejbName);

            final ManagementHome home = (ManagementHome)PortableRemoteObject.narrow(objref, ManagementHome.class);
            println("ManagementHome: " + home + " for " + mejbName);

            final Management mejb = home.create();
            println("Got the MEJB");

            test(mejb);

            mejb.remove();

        } catch (Exception ex) {
            System.err.println("Caught an unexpected exception!");
            ex.printStackTrace();
        }
    }
    */
    private static final void println(final Object o) {
        System.out.println("" + o);
    }
}