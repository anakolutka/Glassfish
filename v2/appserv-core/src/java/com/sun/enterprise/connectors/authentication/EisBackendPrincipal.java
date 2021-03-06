
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

package com.sun.enterprise.connectors.authentication;

import java.io.Serializable;

/**
 * This a javabean class thatabstracts the backend principal.
 * The backend principal consist of the userName and password
 * which is used for authenticating/getting connection from
 * the backend.
 * @author Srikanth P
 */

public class EisBackendPrincipal implements Serializable{


    private String userName;
    private String password;

    /** Default constructor
     */

    public EisBackendPrincipal() {

    }

    /** 
     * Constructor
     * @param userName UserName
     * @param password Password
     * @credential Credential
     */

    public EisBackendPrincipal(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    /** 
     * Setter method for UserName property
     * @param userName  UserName
     */

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /** 
     * Setter method for password property
     * @param password  Password
     */

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Getter method for UserName property
     * @return UserName
     */

    public String getUserName() {
        return userName;
    }

    /**
     * Getter method for Password property
     * @return Password
     */

    public String getPassword() {
        return password;
    }

    /** 
     * Overloaded method from "Object" class
     * Checks the equality.
     * @param backendPrincipal Backend principal against which equality has to
     *        be tested.
     * @return true if they are equal
     *         false if hey are not equal.
     */

    public boolean equals(Object backendPrincipal) {

        if(backendPrincipal == null || 
                 !(backendPrincipal instanceof EisBackendPrincipal)) {
            return false;
        }
        EisBackendPrincipal eisBackendPrincipal = 
                           (EisBackendPrincipal)backendPrincipal;
        
        if(isEqual(eisBackendPrincipal.userName,this.userName) &&
                isEqual(eisBackendPrincipal.password,this.password)) {
            return true;
        } else {
            return false;

        }
    }

    /** Checks whether two strings are equal including the null string
     *  cases.
     */

    private boolean isEqual(String in, String out) {
        if(in == null && out == null) {
            return true;
        }
        if(in == null || out == null) {
            return false;
        }
        return (out.equals(in));
    }
    
    /** 
     * Overloaded method from "Object" class
     * Generates the hashcode
     * 
     * @return a hash code value for this object
     */
    public int hashCode() {
        int result = 67;
        if(userName != null)
            result = 67 * result + userName.hashCode();
        if(password != null)
            result = 67*result + password.hashCode();
        return result;
    }
}
