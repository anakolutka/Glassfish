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
 package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.web.ErrorPageDescriptor;

    /** Objects exhiniting this interface represent an error page and the exception type or
    ** error code that will cause the redirect from the web container. 
    * @author Danny Coward
    */

public class ErrorPageDescriptorImpl implements ErrorPageDescriptor, java.io.Serializable{
    private int errorCode;
    private String exceptionType;
    private String location;
    
    /** The default constructor.
    */
    public ErrorPageDescriptorImpl() {
    
    }
    
    /** Constructor for error code to error page mapping.*/
    public ErrorPageDescriptorImpl(int errorCode, String location) {
	this.errorCode = errorCode;
	this.location = location;
    }
    
     /** Constructor for Java exception type to error page mapping.*/
    public ErrorPageDescriptorImpl(String exceptionType, String location) {
	this.exceptionType = exceptionType;
	this.location = location;
    }
	/** Return the error code. -1 if none. */
    public int getErrorCode() {
	return this.errorCode;
    }
	/** Sets the error code.*/
    public void setErrorCode(int errorCode) {
	this.errorCode = errorCode;
    }
	/** Return the error code as a string if there is no exception type, or the exception type if the error code is -1.*/
    public String getErrorSignifierAsString() {
	if ("".equals(this.getExceptionType())) {
	    return (Integer.valueOf(this.getErrorCode())).toString();
	}
	return this.getExceptionType();
    }
    /**Sets the error code if the argument is parsable as an int, or the exception type else.*/
    public void setErrorSignifierAsString(String errorSignifier) {
	try {
	    int errorCode = Integer.parseInt(errorSignifier);
	    this.setErrorCode(errorCode);
	    this.setExceptionType(null);
	    return;
	} catch (NumberFormatException nfe) {
	
	}
	this.setExceptionType(errorSignifier);
    }
    
    /** Return the exception type or the empty string if none.*/
    public String getExceptionType() {
	if (this.exceptionType == null) {
	    this.exceptionType = "";
	}
	return this.exceptionType;
    }
    
    /** Sets the exception type.*/
    public void setExceptionType(String exceptionType) {
	this.exceptionType = exceptionType;
    }
    /** Return the page to map to */
    public String getLocation() {
	if (this.location == null) {
	    this.location = "";
	}
	return this.location;
    }
    /* Set the page to map to */
    public void setLocation(String location) {
	this.location = location;
    }
    /* A formatted version of my state as a String. */
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("ErrorPage ").append(this.getErrorCode()).append(" ").append(
            this.getExceptionType()).append(" ").append(this.getLocation());
    }

}

