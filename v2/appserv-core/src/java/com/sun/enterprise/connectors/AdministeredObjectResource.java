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

package com.sun.enterprise.connectors;

import com.sun.enterprise.Switch;
import com.sun.enterprise.deployment.AdminObject;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.resource.PoolingException;
import com.sun.enterprise.resource.ResourceInstaller;
import com.sun.enterprise.connectors.util.SetMethodAction;
import com.sun.enterprise.repository.J2EEResourceBase;
import com.sun.enterprise.repository.J2EEResource;
import com.sun.enterprise.repository.SerializableObjectRefAddr;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.naming.Reference;
import javax.naming.NamingException;

/**
 * Resource infor for Connector administered objects
 *
 * @author	Qingqing Ouyang
 */
public class AdministeredObjectResource extends J2EEResourceBase 
    implements Serializable {

    private String resadapter_;
    private String adminObjectClass_;
    private String adminObjectType_;
    private Set configProperties_;

    
    public AdministeredObjectResource (String name)
    {
        super(name);
    }

    protected J2EEResource doClone(String name) {
        AdministeredObjectResource clone = 
            new AdministeredObjectResource(name);
        clone.setResourceAdapter(getResourceAdapter());
        clone.setAdminObjectType(getAdminObjectType());
        return clone;
    }


    public int getType() {
        // FIX ME 
        return 0;
        //return J2EEResource.ADMINISTERED_OBJECT;
    }

    public void initialize(AdminObject desc) {
        configProperties_ = new HashSet();
        adminObjectClass_ = desc.getAdminObjectClass();
        adminObjectType_ = desc.getAdminObjectInterface();
    }

    public String getResourceAdapter() {
        return resadapter_;
    }

    public void setResourceAdapter(String resadapter) {
        resadapter_ = resadapter;
    }

    public String getAdminObjectType() {
        return adminObjectType_;
    }
    
    public void setAdminObjectType (String adminObjectType) {
        this.adminObjectType_ = adminObjectType;
    }

    public void setAdminObjectClass(String name) {
        this.adminObjectClass_ = name;
    }

    public String getAdminObjectClass() {
        return this.adminObjectClass_;
    }
    
    /*
     * Add a configProperty to the set
     */
    public void addConfigProperty(EnvironmentProperty configProperty)
    {
        this.configProperties_.add(configProperty);
    }

    /**
     * Add a configProperty to the set
     */
    public void removeConfigProperty(EnvironmentProperty configProperty)
    {
        this.configProperties_.remove(configProperty);
    }

    public Reference createAdminObjectReference() {
        Reference ref = 
            new Reference(getAdminObjectType(),
                    new SerializableObjectRefAddr("jndiName", this),
                    ConnectorConstants.ADMINISTERED_OBJECT_FACTORY, null);
        
        return ref;
    }


    // called by com.sun.enterprise.naming.factory.AdministeredObjectFactory
    // FIXME.  embedded??
    public Object createAdministeredObject(ClassLoader jcl)
        throws PoolingException {

        try {
            if (jcl == null) {
                // use context class loader
                jcl = (ClassLoader) AccessController.doPrivileged
                    (new PrivilegedAction() { 
                            public Object run() {
                                return 
                                    Thread.currentThread().getContextClassLoader();
                            }
                        });
            }
                
            
            Object adminObject = 
                jcl.loadClass(adminObjectClass_).newInstance();
            
            AccessController.doPrivileged
                (new SetMethodAction(adminObject, configProperties_));
            return adminObject;
        } catch (PrivilegedActionException ex) {
            throw (PoolingException) (new PoolingException().initCause(ex));
        } catch (Exception ex) {
            throw (PoolingException) (new PoolingException().initCause(ex));
        }

    }

    public String toString() {
        return "< Administered Object : " + getName() + 
            " , " + getResourceAdapter() +
            " , " + getAdminObjectType() + " >";
    }
}
