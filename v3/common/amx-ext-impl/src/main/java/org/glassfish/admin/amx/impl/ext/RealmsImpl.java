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
package org.glassfish.admin.amx.impl.ext;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import javax.management.ObjectName;

import org.glassfish.admin.amx.util.ListUtil;
import org.glassfish.admin.amx.base.DomainRoot;

import org.glassfish.admin.amx.intf.config.AuthRealm;
import org.glassfish.admin.amx.intf.config.SecurityService;
import org.glassfish.admin.amx.intf.config.Config;
import org.glassfish.admin.amx.intf.config.Property;

import org.glassfish.internal.api.Globals;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.User;
import org.glassfish.admin.amx.intf.config.Configs;
import org.glassfish.admin.amx.intf.config.Domain;
import org.glassfish.admin.amx.base.Realms;
import org.glassfish.admin.amx.impl.mbean.AMXImplBase;
import org.glassfish.admin.amx.util.CollectionUtil;




/**
    AMX Realms implementation.
    Note that realms don't load until {@link #loadRealms} is called.
 */
public final class RealmsImpl extends AMXImplBase
    // implements Realms
{
    private final RealmsManager mRealmsManager;
    
		public
	RealmsImpl( final ObjectName containerObjectName )
	{
        super( containerObjectName, Realms.class);
        
        mRealmsManager = Globals.getDefaultHabitat().getComponent(RealmsManager.class);
	}
    
    private volatile boolean    realmsLoaded = false;
    
        private void 
    loadRealms()
    {
        if ( realmsLoaded ) return;
        
        _loadRealms();
    }
    
        private synchronized void 
    _loadRealms()
    {
        if ( realmsLoaded ) throw new IllegalStateException();
        
        // this is ugly, the underlying API doesn't understand that there is more than one <security-service>,
        // each with one or more <auth-realm>
        final Domain domainConfig = getDomainRootProxy().child(Domain.class);
        if ( domainConfig == null ) {
            throw new IllegalStateException("Null Domain config");
        }
        final Config config = domainConfig.getConfigs().getConfig().values().iterator().next();
        final SecurityService ss = config.getSecurityService();
        
        final Map<String,AuthRealm> authRealmConfigs = ss.getAuthRealm();
        
        final List<String> goodRealms = new ArrayList<String>();
        for( final AuthRealm authRealm : authRealmConfigs.values() )
        {
            final Map<String,Property> propConfigs = authRealm.getProperty();
            final Properties props = new Properties();
            for (final Property p : propConfigs.values() )
            {
                final String value = p.resolveAttribute( "Value" );
                props.setProperty( p.getName(), value );
            }
            try
            {
                Realm.instantiate( authRealm.getName(), authRealm.getClassname(), props );
                goodRealms.add( authRealm.getName() );
            }
            catch( final Exception e )
            {
                e.printStackTrace();
            }
        }
        
        if ( goodRealms.size() != 0 )
        {
            final String goodRealm = goodRealms.iterator().next();
            try
            {
                final String defaultRealm = ss.getDefaultRealm();
                final Realm r = Realm.getInstance(defaultRealm);
                Realm.setDefaultRealm(defaultRealm);
            }
            catch (Exception e)
            {
                Realm.setDefaultRealm(goodRealms.iterator().next());
                e.printStackTrace();
            }
        }
        
        realmsLoaded = true;
    }
    
    public String[]
    getRealmNames()
    {
        loadRealms();
        final List<String> items = ListUtil.newList( mRealmsManager.getRealmNames() );
        return CollectionUtil.toArray(items, String.class);
    }
    
    public String[]
    getPredefinedAuthRealmClassNames()
    {
        final List<String> items = mRealmsManager.getPredefinedAuthRealmClassNames();
        return CollectionUtil.toArray(items, String.class);
    }
    
    
    public String getDefaultRealmName()
    {
        return mRealmsManager.getDefaultRealmName();
    }
    
    
    public void setDefaultRealmName(final String realmName)
    {
        mRealmsManager.setDefaultRealmName(realmName);
    }
        
        private Realm
    getRealm(final String realmName)
    {
        loadRealms();
        final Realm realm = mRealmsManager.getFromLoadedRealms(realmName);
        if ( realm == null )
        {
            throw new IllegalArgumentException( "No such realm: " + realmName );
        }
        return realm;
    }
    
    public void addUser(
        final String realmName,
        final String user,
        final String password,
        final String[] groupList )
    {
        checkSupportsUserManagement(realmName);
        
        try
        {
            final Realm realm = getRealm(realmName);
            realm.addUser(user, password, groupList);
            realm.persist();
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    public void updateUser(
        final String realmName,
        final String existingUser,
        final String newUser,
        final String password,
        final String[] groupList )
    {
        checkSupportsUserManagement(realmName);
        
        try
        {
            final Realm realm = getRealm(realmName);
            realm.updateUser(existingUser, newUser, password, groupList);
            realm.persist();
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    public void removeUser(final String realmName, final String user)
    {
        checkSupportsUserManagement(realmName);
        
        try
        {
            final Realm realm = getRealm(realmName);
            realm.removeUser(user);
            realm.persist();
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    public boolean supportsUserManagement(final String realmName)
    {
        return getRealm(realmName).supportsUserManagement();
    }
    
        private void
    checkSupportsUserManagement(final String realmName)
    {
        if ( ! supportsUserManagement(realmName) )
        {
            throw new IllegalStateException( "Realm " + realmName + " does not support user management" );
        }
    }

    
    
    public String[] getUserNames(final String realmName)
    {
        try
        {
            final List<String> names = ListUtil.newList( getRealm(realmName).getUserNames() );
            return CollectionUtil.toArray(names, String.class);
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    public String[] getGroupNames(final String realmName)
    {
        try
        {
            final List<String> names = ListUtil.newList( getRealm(realmName).getGroupNames() );
            return CollectionUtil.toArray(names, String.class);
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    public String[] getGroupNames(final String realmName, final String user)
    {
        try
        {
            return CollectionUtil.toArray(ListUtil.newList( getRealm(realmName).getGroupNames(user) ), String.class);
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    
    public Map<String,Object> getUserAttributes(final String realmName, final String username)
    {
        try
        {
            final User user = getRealm(realmName).getUser(username);
            final Map<String,Object> m = new HashMap<String,Object>();
            final List<String> attrNames = ListUtil.newList(user.getAttributeNames());
            for( final String attrName : attrNames ) 
            {
                m.put( attrName, user.getAttribute(attrName) );
            }
            return m;
        }
        catch( final Exception e )
        {
        throw new RuntimeException(e);
        }
    }
    
    private static void debug( final String s ) { System.out.println("##### " + s); }
    
    private static final String ADMIN_REALM = "admin-realm";
    private static final String ANONYMOUS_USER = "anonymous";
    private static final String FILE_REALM_CLASSNAME = "com.sun.enterprise.security.auth.realm.file.FileRealm";
            
    public boolean getAnonymousLogin() {
        final DomainRoot domainRoot = getDomainRootProxy();

        final Domain domainConfig = domainRoot.child(Domain.class);
        final Configs cc = domainConfig.getConfigs();
        final Map<String,Config> configs = cc.childrenMap(Config.class);

        // find the ADMIN_REALM
        AuthRealm adminFileAuthRealm = null;
        for( final Config config : configs.values() )
        {
            for( final AuthRealm auth : config.getSecurityService().childrenMap(AuthRealm.class).values() )
            {
                if ( auth.getName().equals(ADMIN_REALM) )
                {
                    adminFileAuthRealm = auth;
                    break;
                }
            } 
        }
        if (adminFileAuthRealm == null) {
            // There must always be an admin realm
            throw new IllegalStateException( "Cannot find admin realm" );
        }

        // Get FileRealm class name
        final String fileRealmClassName = adminFileAuthRealm.getClassname();
        if (fileRealmClassName != null && ! fileRealmClassName.equals(FILE_REALM_CLASSNAME)) {
            // This condition can arise if admin-realm is not a File realm. Then the API to extract
            // the anonymous user should be integrated for the logic below this line of code. for now,
            // we treat this as an error and instead of throwing exception return false;
            return false;
        }

        final Map<String,Property>  props = adminFileAuthRealm.getProperty();
        final Property keyfileProp = props.get("file");
        if ( keyfileProp == null ) {
            throw new IllegalStateException( "Cannot find property 'file'" );
        }
        //System.out.println( "############### keyFileProp: " + keyfileProp.getName() + " = " + keyfileProp.getValue() );
        final String keyFile = keyfileProp.resolveAttribute( "Value" );
        //System.out.println( "############### keyFile: " + keyfileProp.getValue() + " ===> " + keyFile);
        if (keyFile == null) {
            throw new IllegalStateException( "Cannot find key file" );
        }
        
        //System.out.println( "############### keyFile: " + keyFile);

        final String[] usernames = getUserNames(adminFileAuthRealm.getName());
        return usernames.length == 1 && usernames[0].equals(ANONYMOUS_USER);
        
        /*
        FileRealm fr = null;
        try {
            fr = new FileRealm(keyFile);
        } catch( final Exception e) {
            throw new RuntimeException(e);
        }

        // Check if the realm has only one user named annonymous.
        // Head off to the landing page if so; else to the regular
        // login page.
        try {
            final List<String>  usernames = ListUtil.newList( fr.getUserNames() );
            return usernames.size() == 1  && usernames.get(0).equals(ANONYMOUS_USER);
        } catch(final Exception e) {
            throw new RuntimeException(e);
        }
        */
    }
}























