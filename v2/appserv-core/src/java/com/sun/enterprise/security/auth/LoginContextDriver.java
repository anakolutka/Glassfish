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
package com.sun.enterprise.security.auth;

import java.util.Set;
import java.util.Iterator;
import java.util.logging.*;
import java.security.PrivilegedAction;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.x500.X500Principal;
import sun.security.x509.X500Name;
import com.sun.logging.*;
import com.sun.enterprise.iiop.security.GSSUPName;
import com.sun.enterprise.iiop.security.AnonCredential;
import com.sun.enterprise.security.AppservAccessController;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.auth.login.PasswordCredential;
import com.sun.enterprise.security.auth.login.X509CertificateCredential;
import com.sun.enterprise.security.auth.login.ServerLoginCallbackHandler;
import com.sun.enterprise.security.LoginException;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.Audit;
import com.sun.enterprise.security.auth.realm.certificate.CertificateRealm;
import com.sun.enterprise.security.audit.AuditManagerFactory;
import com.sun.enterprise.security.audit.AuditManager;

// FIXME: ACC methods need to be moved to ACC-specific class.
import com.sun.enterprise.security.ClientSecurityContext;
import com.sun.enterprise.appclient.AppContainer;


/** 
 *
 * This class is invoked implicitly by the server to log in the user
 * information that was sent on the wire by the client. Clients will 
 * use the <i>doClientLogin</i> method to simulate authentication to the
 * server.
 *
 * @author Harpreet Singh (hsingh@eng.sun.com)
 * @author Jyri Virkki
 *
 */
public class LoginContextDriver {

    private static final Logger _logger = 
        LogDomains.getLogger(LogDomains.SECURITY_LOGGER);

    private static final ServerLoginCallbackHandler
        dummyCallback = new ServerLoginCallbackHandler();

    private static final String CLIENT_JAAS_PASSWORD = "default";
    private static final String CLIENT_JAAS_CERTIFICATE = "certificate";

    public static final String CERT_REALMNAME = "certificate";
  
    public static final AuditManager AUDIT_MANAGER = 
        AuditManagerFactory.getAuditManagerInstance();
    /** This class cannot be instantiated
     *
     */
    private LoginContextDriver(){
    }
    /**
     * This method is  just a convenience wrapper for
     * <i>login(Subject, Class)</i> method. It will construct a
     * PasswordCredential class.
     *
     * @param String username
     * @param String password
     * @param String realmName the name of the realm to login into, if realmName
     * is null, we login into the default realm	 
     */
    public static void login(String username, String password, String realmName){

        if(realmName == null || !(Realm.isValidRealm(realmName))){    
            realmName = Realm.getDefaultRealm();
        }         
        final Subject fs = new Subject();
        final PasswordCredential pc =
            new PasswordCredential(username, password, realmName);
        
        AppservAccessController.doPrivileged(new PrivilegedAction(){
            public java.lang.Object run(){
                fs.getPrivateCredentials().add(pc);
                return fs;
            }
        });

        LoginContextDriver.login(fs, PasswordCredential.class);
    }

    
    /**
     * This method performs the login on the server side.
     *
     * <P>This method is the main login method for S1AS. It is called
     * with a Subject and the type (class) of credential which should
     * be checked. The Subject must contain a credential of the
     * specified type or login will fail.
     *
     * <P>While the implementation has been cleaned up, the login
     * process still consists of a number of special cases which are
     * treated separately at the realm level. In the future tighter
     * JAAS integration could clean some of this up.
     *
     * <P>The following credential types are recognized at this time:
     * <ul>
     *  <li>PasswordCredential - This is the general case for all login
     *      methods which rely on the client providing a name and password.
     *      It can be used with any realms/JAAS login modules which expect
     *      such data (e.g. file realm, LDAP realm, UNIX realm)
     *  <LI>X509CertificateCredential - Special case for SSL client auth.
     *      Here authentication has already been done by the SSL subsystem
     *      so this login only creates a security context based on the
     *      certificate data.
     *  <LI>AnonCredential - Unauthenticated session, set anonymous security
     *      context.
     *  <LI>GSSUPName - Retrieve user and realm and set security context.
     *  <LI>X500Name - Retrieve user and realm and set security context.
     * </ul>
     *
     * @param Subject the subject of the client
     * @param Class the class of the credential packaged in the subject.
     *
     */
    public static void login(Subject subject, Class cls)
        throws LoginException
    {
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST,
                        "Processing login with credentials of type: "+
                        cls.toString());
        }
        
        if(cls.equals(PasswordCredential.class)) {
            doPasswordLogin(subject);

        } else if (cls.equals(X509CertificateCredential.class)) {
            doCertificateLogin(subject);
            
        } else if (cls.equals(AnonCredential.class)) {
            doAnonLogin();
            
        } else if (cls.equals(GSSUPName.class)) {
            doGSSUPLogin(subject);
            
        } else if (cls.equals(X500Name.class)) {
            doX500Login(subject);
            
        } else {
            _logger.log(Level.INFO, "java_security.unknown_credential",
                        cls.toString());
            throw new
                LoginException("Unknown credential type, cannot login.");
        }
    }


    /**
     * This method is used for logging in a run As principal. It creates
     * a JAAS subject whose credential is to type GSSUPName.
     * This is used primarily for runas
     *
     */ 
    public static void loginPrincipal(String username, String realmName)
        throws LoginException {

        // no realm provided, assuming default
        if(realmName == null || realmName.length() == 0){
            realmName = Realm.getDefaultRealm();
        }
        
        final Subject s = new Subject();

        final com.sun.enterprise.deployment.PrincipalImpl p 
            = new com.sun.enterprise.deployment.PrincipalImpl(username);

        final GSSUPName name = new GSSUPName(username, realmName);

        AppservAccessController.doPrivileged(new PrivilegedAction() {
            public java.lang.Object run() {
                s.getPrincipals().add(p);
                s.getPublicCredentials().add(name);
                return null;
            }
        });

        setSecurityContext(username, s, realmName);
    }

    
    /**
     * This method logs out the user by clearing the security context.
     *
     */
    public static void logout() throws LoginException {
        unsetSecurityContext();
    }


    /**
     * Log in subject with PasswordCredential. This is a generic login
     * which applies to all login mechanisms which process PasswordCredential.
     * In other words, any mechanism which receives an actual username, realm
     * and password set from the client.
     *
     * <P>The realm contained in the credential is checked, and a JAAS
     * LoginContext is created using a context name obtained from the
     * appropriate Realm instance. The applicable JAAS LoginModule
     * is initialized (based on the jaas login configuration) and login()
     * is invoked on it.
     *
     * <P>RI code makes several assumptions which are retained here:
     * <ul>
     *  <li>The PasswordCredential is stored as a private credential of
     *      the subject.
     *  <li>There is only one such credential present (actually, only
     *      the first one is relevant if more are present).
     * </ui>
     *
     * @param s Subject to be authenticated.
     * @throws LoginException Thrown if the login fails.
     *
     */
    private static void doPasswordLogin(Subject subject)
        throws LoginException
    {
        final Subject s = subject;
        
        Object obj = getPrivateCredentials(s, PasswordCredential.class);
        assert obj != null;
        
        PasswordCredential p = (PasswordCredential) obj;
        String user = p.getUser();
        String pwd = p.getPassword();
        String realm = p.getRealm();
        String jaasCtx = null;
        try {
            jaasCtx = Realm.getInstance(realm).getJAASContext();
        } catch(Exception ex) {
            if( ex instanceof LoginException )
                throw (LoginException)ex;
            else
                throw (LoginException)new LoginException(ex.toString()).initCause(ex);
        }

        assert user != null;
        assert pwd != null;
        assert realm != null;
        assert jaasCtx != null;

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Logging in user [" + user + "] into realm: " +
                         realm + " using JAAS module: "+jaasCtx);
        }

        try {
            // A dummyCallback is used to satisfy JAAS but it is never used.
            // name/pwd info is already contained in Subject's Credential
            LoginContext lg = new LoginContext(jaasCtx, s, dummyCallback);
            lg.login();
            
        } catch (Exception e) {
            if (_logger.isLoggable(Level.INFO)) {
                _logger.log(Level.INFO, "java_security.audit_auth_refused",
                            user);
            } 
            if (_logger.isLoggable(Level.FINEST)) {
                _logger.log(Level.FINEST, "doPasswordLogin fails", e);
            }
            if(AUDIT_MANAGER.isAuditOn()){
                AUDIT_MANAGER.authentication(user, realm, false);
            }
            if( e instanceof LoginException )
                throw (LoginException)e;
            else
                throw (LoginException)
                    new LoginException("Login failed: " + e.toString()).initCause(e);
        }
        if(AUDIT_MANAGER.isAuditOn()){
            AUDIT_MANAGER.authentication(user, realm, true);
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Password login succeeded for : " + user);
        }

        setSecurityContext(user, s, realm);
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Set security context as user: "+user);
        }
    }

   /** Performs login for JMAC security. The difference between this
     * method and others is that it just verifies whether the login will succeed
     * in the given realm.
     * It does not set the result of the authentication in the appserver runtime
     * environment
     * A silent return from this method means that the given user succeeding in
     * authenticating with the given password in the given realm
     * @param subject
     * @param username
     * @param password
     * @param realmName the realm to authenticate under
     * @returns Subject on successful authentication
     * @throws LoginException
     */
    public static Subject jmacLogin(Subject subject,
            String username, String password, String realmName)
            throws LoginException {

        if(realmName == null || !(Realm.isValidRealm(realmName))){
            realmName = Realm.getDefaultRealm();
        }
        if (subject == null) {
            subject = new Subject();
        }
        final Subject fs = subject;
        final PasswordCredential pc =
            new PasswordCredential(username, password, realmName);

        AppservAccessController.doPrivileged(new PrivilegedAction(){
            public java.lang.Object run(){
                fs.getPrivateCredentials().add(pc);
                return fs;
            }
        });

        String jaasCtx = null;
        try {
            jaasCtx = Realm.getInstance(realmName).getJAASContext();
        } catch(Exception ex) {
            if( ex instanceof LoginException )
                throw (LoginException)ex;
            else
                throw (LoginException)
                    new LoginException(ex.toString()).initCause(ex);
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("jmac login user [" + username + "] into realm: " +
                         realmName + " using JAAS module: "+jaasCtx);
        }

        try{
            // A dummyCallback is used to satisfy JAAS but it is never used.
            // name/pwd info is already contained in Subject's Credential
            LoginContext lg = new LoginContext(jaasCtx, fs, dummyCallback);
            lg.login();
        } catch (Exception e) {
            if (_logger.isLoggable(Level.INFO)) {
                _logger.log(Level.INFO, "java_security.audit_auth_refused",
                            username);
            }
            if(AUDIT_MANAGER.isAuditOn()){
                AUDIT_MANAGER.authentication(username, realmName, false);
            }

            if( e instanceof LoginException )
                throw (LoginException)e;
            else
                throw (LoginException)
                    new LoginException("Login failed: " + e.toString()).initCause(e);
        }
        if(AUDIT_MANAGER.isAuditOn()){
            AUDIT_MANAGER.authentication(username, realmName, true);
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("jmac Password login succeeded for : " + username);
        }

        return subject;
        // do not set the security Context
    }

    public static Subject jmacLogin(Subject subject,
            X500Principal x500Principal) throws LoginException {

        if (subject == null) {
            subject = new Subject();
        }

        final Subject fs = subject;

        String userName = "";
        try {
            final X500Name x500Name = new X500Name(
                x500Principal.getName(X500Principal.RFC1779));
            userName = x500Name.toString();

            AppservAccessController.doPrivileged(new PrivilegedAction(){
                public java.lang.Object run(){
                    fs.getPublicCredentials().add(x500Name);
                    return fs;
                }
            });

            Realm realm = Realm.getInstance(CertificateRealm.AUTH_TYPE);
            CertificateRealm certRealm = (CertificateRealm)realm;
            certRealm.authenticate(fs, x500Name);
        } catch(Exception ex) {
            if (_logger.isLoggable(Level.INFO)) {
                _logger.log(Level.INFO, "java_security.audit_auth_refused",
                            userName);
            }
            if (AUDIT_MANAGER.isAuditOn()){
                AUDIT_MANAGER.authentication(userName,
                    CertificateRealm.AUTH_TYPE, false);
            }
            if (ex instanceof LoginException) {
                throw (LoginException)ex;
            } else {
                throw (LoginException)
                    new LoginException(ex.toString()).initCause(ex);
            }
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("jmac cert login succeeded for: " + userName);
        }

        if (AUDIT_MANAGER.isAuditOn()){
            AUDIT_MANAGER.authentication(userName,
                CertificateRealm.AUTH_TYPE, true);
        }
        // do not set the security Context

        return subject;
    }

    /**
     * A special case login for handling X509CertificateCredential.
     * This does not get triggered based on current RI code. See X500Login.
     *
     */
    private static void doCertificateLogin(Subject s)
        throws LoginException
    {
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE, "Processing X509 certificate login.");
        }
        String realm = CertificateRealm.AUTH_TYPE;
        String user = null;
        try{
            Object obj = getPublicCredentials(s, X509CertificateCredential.class);

            X509CertificateCredential xp = (X509CertificateCredential) obj;
            user = xp.getAlias();
            if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Set security context as user: "+user);
            }
            setSecurityContext(user, s, realm);
            if(AUDIT_MANAGER.isAuditOn()){
                AUDIT_MANAGER.authentication(user, realm, true);
            }
        } catch(LoginException le){
            if(AUDIT_MANAGER.isAuditOn()){
                AUDIT_MANAGER.authentication(user, realm, false);
            }
            throw le;
        }
    }


    /**
     * A special case login for anonymous credentials (no login info).
     *
     */
    private static void doAnonLogin()
        throws LoginException
    {
        // instance of anononymous credential login with guest
       SecurityContext.setUnauthenticatedContext();
       if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Set anonymous security context.");
        }  
    }


    /**
     * A special case login for GSSUPName credentials.
     *
     */
    private static void doGSSUPLogin(Subject s)
        throws LoginException
    {
       if(_logger.isLoggable(Level.FINE)){
             _logger.fine("Processing GSSUP login.");
       }
       String user = null;
       String realm = Realm.getDefaultRealm();
       try{
            Object obj = getPublicCredentials(s, GSSUPName.class);

            user = ((GSSUPName)obj).getUser();
    
            setSecurityContext(user, s, realm);
            if(AUDIT_MANAGER.isAuditOn()){
                AUDIT_MANAGER.authentication(user, realm, true);        
            }
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("GSSUP login succeeded for : " + user);
            }
       } catch (LoginException le){
           if(AUDIT_MANAGER.isAuditOn()){
               AUDIT_MANAGER.authentication(user, realm, false);
           }
           throw le;
       }
    }


    /**
     * A special case login for X500Name credentials.
     * This is invoked for certificate login because the containers
     * extract the X.500 name from the X.509 certificate before calling
     * into this class.
     *
     */
    private static void doX500Login(Subject s)
        throws LoginException
    {
       if(_logger.isLoggable(Level.FINE)){        
            _logger.fine("Processing X.500 name login.");
       }
       String user = null;
       String realm_name = null;
       try{
            X500Name x500name = (X500Name)getPublicCredentials(s, X500Name.class);
            user = x500name.getName();
        
            // In the RI-inherited implementation this directly creates
            // some credentials and sets the security context. This means
            // that the certificate realm does not get an opportunity to
            // process the request. While the realm will not do any
            // authentication (already done by this point) it can choose
            // to adjust the groups or principal name or other variables
            // of the security context. Of course, bug 4646134 needs to be
            // kept in mind at all times.

            Realm realm = Realm.getInstance(CertificateRealm.AUTH_TYPE);

            if (realm instanceof CertificateRealm) { // should always be true
    
                CertificateRealm certRealm = (CertificateRealm)realm;
                certRealm.authenticate(s, x500name);
                realm_name = CertificateRealm.AUTH_TYPE;
                if(AUDIT_MANAGER.isAuditOn()){
                    AUDIT_MANAGER.authentication(user, realm_name, true);
                }
            } else {            
                _logger.warning("certlogin.badrealm");            
                setSecurityContext(user, s, realm_name);
                realm_name = realm.getName();
            }
        
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("X.500 name login succeeded for : " + user);
            }
       } catch (LoginException le){
           if(AUDIT_MANAGER.isAuditOn()){
               AUDIT_MANAGER.authentication(user, realm_name, false);
           }
           throw le;
       } catch (Exception ex) {
           throw (LoginException)new LoginException(ex.toString()).initCause(ex);
       }
    }


    /**
     * Retrieve a public credential of the given type (java class) from the
     * subject.
     *
     * <P>This method retains the RI assumption that only the first
     * credential of the given type is used.
     *
     */
    private static Object getPublicCredentials(Subject s, Class cls)
        throws LoginException
    {
        Set credset = s.getPublicCredentials(cls);
        
        final Iterator iter = credset.iterator();

        if(!iter.hasNext()) {
            String credmsg = cls.toString();
            if(_logger.isLoggable(Level.FINER)){
                _logger.finer("Expected public credentials of type : " +
                          credmsg + " but none found.");
            }
            throw new LoginException("Expected public credential of type: "+
                                     credmsg + " but none found.");
        }
        
        Object obj = null;    
        try{
            obj = AppservAccessController.doPrivileged(new PrivilegedAction(){
                public java.lang.Object run(){
                    return iter.next();
                }
            });
        } catch (Exception e){
            // should never come here 
            if( e instanceof LoginException )
                throw (LoginException)e;
            else
                throw (LoginException)
                    new LoginException("Failed to retrieve public credential: "+
                                       e.toString()).initCause(e);
        }

        return obj;
    }


    /**
     * Retrieve a private credential of the given type (java class) from the
     * subject.
     *
     * <P>This method retains the RI assumption that only the first
     * credential of the given type is used.
     *
     */
    private static Object getPrivateCredentials(Subject subject,
                                                Class cls)
        throws LoginException
    {
        final Subject s = subject;
        final Class cl = cls;
        
        final Set credset = (Set)
            AppservAccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                    return
                        s.getPrivateCredentials(cl);
                }
            });

        final Iterator iter = credset.iterator();

        if (!iter.hasNext()) {
           String credmsg = cls.toString();
           if(_logger.isLoggable(Level.FINER)){
                 _logger.finer("Expected private credential of type: "+
                              credmsg + " but none found.");
           }
            throw new LoginException("Expected private credential of type: "+
                                     credmsg + " but none found.");
        }

        // retrieve only first credential of give type
        Object obj = null;
        try{
            obj = AppservAccessController.doPrivileged(new PrivilegedAction(){
                public java.lang.Object run(){
                    return iter.next();
                }
            });
        } catch (Exception e){
            // should never come here 
            if( e instanceof LoginException )
                throw (LoginException)e;
            else
                throw (LoginException)
                    new LoginException("Failed to retrieve private credential: "+
                                     e.toString()).initCause(e);
        }

        return obj;
    }

    
    /**
     * This method sets the security context on the current Thread Local 
     * Storage
     * @param String username is the user who authenticated
     * @param Subject is the subject representation of the user
     * @param Credentials the credentials that the server associated with it
     */
    private  static void setSecurityContext(String userName,
                                            Subject subject, String realm){ 

        SecurityContext securityContext =
            new SecurityContext(userName, subject, realm);
        SecurityContext.setCurrent(securityContext);
    }

    
    /**
     * Set the current security context on the Thread Local Storage to null.
     *
     */
    private  static void unsetSecurityContext() {
        SecurityContext.setCurrent((SecurityContext)null);
    }

    /**
     * Perform login on the client side.
     * It just simulates the login on the client side.
     * The method uses the callback handlers and generates correct
     * credential information that will be later sent to the server
     * @param int type whether it is <i> username_password</i> or 
     * <i> certificate </i> based login.
     * @param CallbackHandler the callback handler to gather user information.
     * @exception LoginException the exception thrown by the callback handler.
     */
    public  static Subject doClientLogin(int type,
                     javax.security.auth.callback.CallbackHandler jaasHandler)
        throws LoginException
    {
        final javax.security.auth.callback.CallbackHandler handler =
            jaasHandler;
        // the subject will actually be filled in with a PasswordCredential
        // required by the csiv2 layer in the LoginModule.
        // we create the dummy credential here and call the 
        // set security context. Thus, we have 2  credentials, one each for
        // the csiv2 layer and the other for the RI.
        final Subject subject = new Subject();
        if (type == AppContainer.USERNAME_PASSWORD){
            AppservAccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                    try{
                        LoginContext lg = 
                            new LoginContext(CLIENT_JAAS_PASSWORD, 
                                             subject, handler);
                        lg.login();
                    }catch(javax.security.auth.login.LoginException e){
                        throw (LoginException)
                            new LoginException(e.toString()).initCause(e);
                    }
                    
                    return null;
                }
            });
            postClientAuth(subject, PasswordCredential.class);
            return subject;
        } else if (type == AppContainer.CERTIFICATE){
            AppservAccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                    try{
                        LoginContext lg = 
                            new LoginContext(CLIENT_JAAS_CERTIFICATE,
                                             subject, handler);
                        lg.login();
                    }catch(javax.security.auth.login.LoginException e){
                        throw (LoginException)
                            new LoginException(e.toString()).initCause(e);
                    }
                    
                    return null;
                }
            });
            postClientAuth(subject, X509CertificateCredential.class);
            return subject;
        } else if (type == AppContainer.ALL){
            AppservAccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                    try{
                        LoginContext lgup =
                            new LoginContext(CLIENT_JAAS_PASSWORD,
                                             subject, handler);
                        LoginContext lgc = 
                            new LoginContext(CLIENT_JAAS_CERTIFICATE,
                                                 subject, handler);
                        lgup.login();
                        postClientAuth(subject, PasswordCredential.class);
                        
                        lgc.login();
                        postClientAuth(subject,
                                       X509CertificateCredential.class);
                    }catch(javax.security.auth.login.LoginException e){
                        throw (LoginException)
                            new LoginException(e.toString()).initCause(e);
                    }
                    
                    return null;
                }
            });
            return subject;
        } else{
            AppservAccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                    try{
                        LoginContext lg =
                            new LoginContext(CLIENT_JAAS_PASSWORD, 
                                             subject, handler);
                        lg.login();
                        postClientAuth(subject, PasswordCredential.class);
                    }catch(javax.security.auth.login.LoginException e){
                        throw (LoginException)
                            new LoginException(e.toString()).initCause(e);
                    }
                    return null;
                }
            });
            return subject;
        }
    }

    /**
     * Perform logout on the client side.
     * @exception LoginException
     */
    public static void doClientLogout() throws LoginException {
        unsetClientSecurityContext();
    }
    
    /**
     * Extract the relevant username and realm information from the
     * subject and sets the correct state in the security context. The
     * relevant information is set into the Thread Local Storage from
     * which then is extracted to send over the wire.
     *
     * @param Subject the subject returned by the JAAS login.
     * @param Class the class of the credential object stored in the subject
     *
     */
    private  static void postClientAuth(Subject subject, Class clazz){
        final Class clas = clazz;
        final Subject fs = subject;
        Set credset = 
            (Set) AppservAccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                if(_logger.isLoggable(Level.FINEST)){
                    _logger.log(Level.FINEST,"LCD post login subject :" + fs);
                }
                    return fs.getPrivateCredentials(clas);
                }
            });
        final Iterator iter = credset.iterator();
        while(iter.hasNext()) {
            Object obj = null;    
            try{
                obj = AppservAccessController.doPrivileged(new PrivilegedAction(){
                    public java.lang.Object run(){
                        return iter.next();
                    }
                });
            } catch (Exception e){
                // should never come here 
                _logger.log(Level.SEVERE,
                            "java_security.accesscontroller_action_exception",
                            e);
            }
            if(obj instanceof PasswordCredential) {
                PasswordCredential p = (PasswordCredential) obj;
                String user = p.getUser();
                if(_logger.isLoggable(Level.FINEST)){
                    String realm = p.getRealm();
                    _logger.log(Level.FINEST,"In LCD user-pass login:" +
                            user +" realm :" + realm);
                }
                setClientSecurityContext(user, fs);
                return;
            } else if (obj instanceof X509CertificateCredential){
                X509CertificateCredential p = (X509CertificateCredential) obj;
                String user = p.getAlias();
                if(_logger.isLoggable(Level.FINEST)){
                    String realm = p.getRealm();
                    _logger.log(Level.FINEST,"In LCD cert-login::" +
                                user +" realm :" + realm);
                }
                setClientSecurityContext(user, fs);
                return;
            }
        }
    }

    
    /**
     * Sets the security context on the appclient side.
     * It sets the relevant information into the TLS
     * @param String username is the user who authenticated
     * @param Subject is the subject representation of the user
     * @param Credentials the credentials that the server associated with it
     */
    private static void setClientSecurityContext(String username, 
                                                 Subject subject) {
                                                 
        ClientSecurityContext securityContext =
            new ClientSecurityContext(username, subject);
        ClientSecurityContext.setCurrent(securityContext);
    }

    
    /**
     * Unsets the current appclient security context on the Thread
     * Local Storage
     */
    private  static void unsetClientSecurityContext() {
        ClientSecurityContext.setCurrent(null);
    }

    

}
