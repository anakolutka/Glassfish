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
/*
 * ReplicationStore.java
 *
 * Created on November 17, 2005, 10:24 AM
 */

package org.glassfish.web.ha.session.management;


import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.enterprise.naming.util.ObjectInputOutputStreamFactory;
import com.sun.enterprise.naming.util.ObjectInputOutputStreamFactoryFactory;
import com.sun.enterprise.web.ServerConfigLookup;
import com.sun.logging.LogDomains;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.web.ha.session.management.SimpleMetadata;
import com.sun.appserv.util.cache.BaseCache;

import org.apache.catalina.*;
import org.apache.catalina.session.*;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author Larry White
 */
public class ReplicationStore extends HAStoreBase {
    
    
    /**
     * The logger to use for logging ALL web container related messages.
     */
    protected static final Logger _logger
        = LogDomains.getLogger(ReplicationStore.class, LogDomains.WEB_LOGGER);


    
    /**
     * Creates a new instance of ReplicationStore
     */
    public ReplicationStore(ServerConfigLookup serverConfigLookup, JavaEEIOUtils ioUtils) {
        super(serverConfigLookup, ioUtils);
        //setLogLevel();        
    }
    

    public String getApplicationId() {
        if(applicationId != null) {
            return applicationId;
        }
        applicationId = "WEB:" + super.getApplicationId();
        return applicationId;
    }    

    // HAStorePoolElement methods begin
    
    /**
     * Save the specified Session into this Store.  Any previously saved
     * information for the associated session identifier is replaced.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */    
    public void valveSave(Session session) throws IOException {
/*
        HASession haSess = (HASession)session;
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>valveSave id=" + haSess.getIdInternal() +
            " isPersistent=" + haSess.isPersistent() + " isDirty=" + haSess.isDirty());
        }        
        if( haSess.isPersistent() && !haSess.isDirty() ) {
            this.updateLastAccessTime(session);
        } else {
            this.doValveSave(session);
            haSess.setPersistent(true);
        }
        haSess.setDirty(false);
*/
        this.doValveSave(session);
    }

    /**
     * Save the specified Session into this Store.  Any previously saved
     * information for the associated session identifier is replaced.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */    
    public void doValveSave(Session session) throws IOException {
/*
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>doValveSave:id =" + ((HASession)session).getIdInternal());
            _logger.fine("ReplicationStore>>doValveSave:valid =" + session.getIsValid());
            _logger.fine("ReplicationStore>>doValveSave:ssoId=" + ((HASession)session).getSsoId());            
        }        
*/
        // begin 6470831 do not save if session is not valid
        if( !session.getIsValid() ) {
            return;
        }
        // end 6470831         
/*
        String userName = "";
        if(session.getPrincipal() !=null){
            userName = session.getPrincipal().getName();
            ((BaseHASession)session).setUserName(userName);
        }
*/
        byte[] sessionState = this.getByteArray(session, isReplicationCompressionEnabled());
        ReplicationManagerBase mgr
            = (ReplicationManagerBase)this.getManager();
        BackingStore replicator = mgr.getBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>doValveSave replicator: " + replicator);
            _logger.fine("ReplicationStore>>doValveSave version:" + session.getVersion());                       
        }        
        SimpleMetadata simpleMetadata =
            SimpleMetadataFactory.createSimpleMetadata(session.getVersion(),  //version
                session.getLastAccessedTime(), //lastaccesstime
                session.getMaxInactiveInterval(), //maxinactiveinterval
                sessionState); //state
        try {
//            HASession haSess = (HASession)session;
//            replicator.save(session.getIdInternal(), //id
//                    simpleMetadata, haSess.isPersistent());  //TODO: Revist the last param

            replicator.save(session.getIdInternal(), //id
                    simpleMetadata,true);
            _logger.info("Save succeeded.");

        } catch (BackingStoreException ex) {
            IOException ex1 = 
                (IOException) new IOException("Error during save: " + ex.getMessage()).initCause(ex);
            throw ex1;
        }
    }


    public void cleanup() {
        //FIXME;
    }
    
    public BaseCache getSessions(){
        //FIXME
        return null;
    }
  
    public void setSessions(BaseCache sesstable) {
        //FIXME;
    } 
    
    
    /**
     * Save the specified Session into this Store.  Any previously saved
     * information for the associated session identifier is replaced.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */    
    public void save(Session session) throws IOException {        
/*
        HASession haSess = (HASession)session;
        if( haSess.isPersistent() && !haSess.isDirty() ) {
            this.updateLastAccessTime(session);
        } else {
            this.doSave(session);
            haSess.setPersistent(true);
        }
        haSess.setDirty(false);        
*/
        this.doSave(session);
    }
    
    protected boolean isReplicationCompressionEnabled() {
        //XXX Need to fix this.
        return false;
    }

    /**
     * Save the specified Session into this Store.  Any previously saved
     * information for the associated session identifier is replaced.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */    
    public void doSave(Session session) throws IOException {
        byte[] sessionState = this.getByteArray(session, isReplicationCompressionEnabled());
        ReplicationManagerBase mgr
            = (ReplicationManagerBase)this.getManager();
        BackingStore backingStore = mgr.getBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>save: backing store : " + backingStore);                       
        }         
        SimpleMetadata simpleMetadata =
            SimpleMetadataFactory.createSimpleMetadata(session.getVersion(),  //version
                session.getLastAccessedTime(), //lastaccesstime
                session.getMaxInactiveInterval(), //maxinactiveinterval
                sessionState); //state
               
        try {
            backingStore.save(session.getIdInternal(), //id
                    simpleMetadata, true);  //TODO: Revist the last param
        } catch (BackingStoreException ex) {
            IOException ex1 = 
                (IOException) new IOException("Error during save: " + ex.getMessage()).initCause(ex);
            throw ex1;
        }
    }        
    
    /**
    * Clear sessions
    *
    * @exception IOException if an input/output error occurs
    */   
    public synchronized void clear() throws IOException {
        //FIXME

    }
    
    /**
    * Remove the Session with the specified session identifier from
    * this Store, if present.  If no such Session is present, this method
    * takes no action.
    *
    * @param id Session identifier of the Session to be removed
    *
    * @exception IOException if an input/output error occurs
    */
    public void doRemove(String id) throws IOException  {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>doRemove");                       
        }        
        ReplicationManagerBase mgr
            = (ReplicationManagerBase)this.getManager();
        BackingStore replicator = mgr.getBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>doRemove: replicator: " + replicator);                       
        }        
        try {
            replicator.remove(id);
        } catch (BackingStoreException ex) {
            //FIXME
        }
    }     
    
    /**
    * Remove the Session with the specified session identifier from
    * this Store, if present.  If no such Session is present, this method
    * takes no action.
    *
    * @param id Session identifier of the Session to be removed
    *
    * @exception IOException if an input/output error occurs
    */
    public synchronized void removeSynchronized(String id) throws IOException  {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>removeSynchronized");                       
        }         
        ReplicationManagerBase mgr
            = (ReplicationManagerBase)this.getManager();
        BackingStore replicator = mgr.getBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>removeSynchronized: replicator: " + replicator);                       
        }        
        try {
            replicator.remove(id);
        } catch (BackingStoreException ex) {
            //FIXME
        }
    }
    

    /**
     * Called by our background reaper thread to remove expired
     * sessions in the replica - this can be done on the same
     * instance - i.e. each instance will do its own
     *
     */
    public void processExpires() {        
        
//        ReplicationManagerBase replicationMgr =
//            (ReplicationManagerBase) this.getManager();
//        if(!(replicationMgr.isThirdPartyBackingStoreInUse())) {
//            replicationMgr.processExpiredReplicas();
//        } else {
//            removeExpiredSessions();
//        }
        removeExpiredSessions();
    }
    
    /** This method deletes all the sessions corresponding to the "appId"
     * that should be expired
     * @return number of removed sessions
     */    
    public int removeExpiredSessions() {        
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("IN ReplicationStore>>removeExpiredSessions");
        }
        int result = 0;
        ReplicationManagerBase mgr
            = (ReplicationManagerBase)this.getManager();
        if(mgr == null) {
            return result;
        }
        BackingStore backingStore = mgr.getBackingStore();
        try {
            //XXX Need to get the idle for millis from settings somewhere
            result = backingStore.removeExpired(30000);
        } catch (BackingStoreException ex) {
            //FIXME
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>removeExpiredSessions():number of expired sessions = " + result);
        }
        return result;

    }       
    

    /**
    * Load and return the Session associated with the specified session
    * identifier from this Store, without removing it.  If there is no
    * such stored Session, return <code>null</code>.
    *
    * @param id Session identifier of the session to load
    *
    * @exception ClassNotFoundException if a deserialization error occurs
    * @exception IOException if an input/output error occurs
    */
    public Session load(String id) throws ClassNotFoundException, IOException {
        return load(id, null);
    }    
    
    public Session load(String id, String version) throws ClassNotFoundException, IOException {
        try {
                return loadFromBackingStore(id, version);
        } catch (BackingStoreException ex) {
            IOException ex1 =
                    (IOException) new IOException("Error during load: " + ex.getMessage()).initCause(ex);
            throw ex1;
        }
    }


    public Session loadFromBackingStore(String id, String version)
            throws IOException, ClassNotFoundException, BackingStoreException {
        SimpleMetadata metaData = (SimpleMetadata) getBackingStore().load(id, version);
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>loadFromBackingStore:id=" +
                    id + ", metaData=" + metaData);
        }
        Session session = getSession(metaData);
        //validateAndSave(session);
        return session;
    }

    BackingStore getBackingStore() {
        ReplicationManagerBase mgr
                = (ReplicationManagerBase) this.getManager();
        BackingStore replicator = mgr.getBackingStore();
        return replicator;
    }






    /**
     * update the lastaccess time of the specified Session into this Store.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */    
    public void updateLastAccessTime(Session session) throws IOException {
        
        ReplicationManagerBase mgr
            = (ReplicationManagerBase)this.getManager();
        BackingStore replicator = mgr.getBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>updateLastAccessTime: replicator: " + replicator);                       
        }         
        try {
            /* FIXED
            replicator.updateLastAccessTime(session.getIdInternal(), //id
                    ((BaseHASession)session).getLastAccessedTimeInternal(), //lastaccesstime
                    session.getVersion()); //version
            */
            SimpleMetadata smd = SimpleMetadataFactory.createSimpleMetadata(session.getVersion(),
                    ((BaseHASession)session).getLastAccessedTimeInternal());
//            replicator.save(session.getIdInternal(), smd, !((HASession) session).isPersistent()); //version
         replicator.save(session.getIdInternal(), smd, true); //version
        } catch (BackingStoreException ex) {
            //FIXME
        }
    }        
    
    /**
    * Return an array containing the session identifiers of all Sessions
    * currently saved in this Store.  If there are no such Sessions, a
    * zero-length array is returned.
    *
    * @exception IOException if an input/output error occurred
    */  
    public String[] keys() throws IOException  {
        //FIXME
        return new String[0];
    }
    
    public int getSize() throws IOException {
        int result = 0;
        ReplicationManagerBase mgr
            = (ReplicationManagerBase)this.getManager();
        BackingStore replicator = mgr.getBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>getSize: replicator: " + replicator);                       
        }         
        try {
            result = replicator.size();
        } catch (BackingStoreException ex) {
            //nothing to do - ok to eat exception
        }
        return result;
    }    
    
    // Store methods end

    private Session getSession(SimpleMetadata metaData) throws IOException {
        if (metaData == null || metaData.getState() == null) {
            return null;
        } else {
//            HttpSessionExtraParams extraParams
//                    = (HttpSessionExtraParams) metaData.getExtraParam();
//            String ssoId = null;
//            if (extraParams != null) {
//                //ssoid is the extraParam in extraParams
//                ssoId = extraParams.getSsoId();
//            }
//            return getSession(metaData.getState(),
//                    ssoId, metaData.getVersion());
            return getSession(metaData.getState(),
                     metaData.getVersion(), metaData.getClass().getClassLoader());


        }
    }


    //public Session getSession(byte[] state, String ssoId, long version) throws IOException {
    public Session getSession(byte[] state,  long version, ClassLoader classLoader) throws IOException {
        Session _session = null;
        InputStream is = null;
        BufferedInputStream bis = null;
        ByteArrayInputStream bais = null;
        Loader loader = null;    
        ObjectInputStream ois = null;
        Container container = manager.getContainer();
        java.security.Principal pal=null; //MERGE chg added
        //ObjectInputOutputStreamFactory factory = ObjectInputOutputStreamFactoryFactory.getFactory();
            
        try
        {
            bais = new ByteArrayInputStream(state);
            bis = new BufferedInputStream(bais);
            if(isReplicationCompressionEnabled()) {
                is = new GZIPInputStream(bis);
            } else {
                is = bis;
            }            
            

            if(_logger.isLoggable(Level.FINEST)) {
                _logger.finest("loaded session from replicationstore, length = "+state.length);
            }

                try {
                   
                    //factory.createObjectInputStream(is);
                    ois = ioUtils.createObjectInputStream(is,true,classLoader);
                } catch (Exception ex) {}
            

            if (ois == null) {
                ois = new ObjectInputStream(is); 
            }
            if(ois != null) {               
                try {
                    _session = readSession(manager, ois);
                } 
                finally {
                    if (ois != null) {
                        try {
                            ois.close();
                            bis = null;
                        }
                        catch (IOException e) {
                        }
                    }
                }
            }
        }
        catch(ClassNotFoundException e)
        {
            IOException ex1 = (IOException) new IOException(
                    "Error during deserialization: " + e.getMessage()).initCause(e);
            throw ex1;
        }
        catch(IOException e)
        {
            //if (_logger.isLoggable(Level.FINE)) {
            //    _logger.fine("Exception occurred in getSession", e);
            //}
            throw e;
        }
/*        String username = ((HASession)_session).getUserName();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>getSession: username=" + username + " principal=" + _session.getPrincipal());                                  
        }        
        if((username !=null) && (!username.equals("")) && _session.getPrincipal() == null) {
            if (_debug > 0) {
                debug("Username retrieved is "+username);
            }
            pal = ((com.sun.web.security.RealmAdapter)container.getRealm()).createFailOveredPrincipal(username);
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("ReplicationStore>>getSession:created pal=" + pal);                                  
            }             
            if (_debug > 0) {
                debug("principal created using username  "+pal);
            }
            if(pal != null) {
                _session.setPrincipal(pal);
                if (_debug > 0) {
                    debug("getSession principal="+pal+" was added to session="+_session); 
                }                
            }
        }*/
        //--SRI        
        
        _session.setNew(false);
//        if(_logger.isLoggable(Level.FINE)) {
//            _logger.fine("ReplicationStore>>getSession:ssoId=" + ssoId);
//        }

/*
        ((HASession)_session).setVersion(version);
        ((HASession)_session).setDirty(false);
        ((HASession)_session).setPersistent(false);        
*/
        return _session;
    }
    


    
}
