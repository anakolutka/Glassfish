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

/* CVS information
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/comm/AuthenticationInfo.java,v 1.3 2005/12/25 04:26:30 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 04:26:30 $
*/

package com.sun.enterprise.admin.jmx.remote.comm;

/** An class that holds the user and password for the connection to the server. 
 * Instances of this class are immutable.
 * @author Kedar Mhaswade
 * @since S1AS7.0
 * @version 1.0
 */

public final class AuthenticationInfo {
	private final String  user;
	private final String  password;
	
	/** The only way to construct the instances of this class.
	 * @param user		the user name for the connection
	 * @param password  the clear text password for the connection
	 */
	public AuthenticationInfo(String user, String password) {
		this.user       = user;
		this.password   = password;
	}
	
	/** Returns the user name.
	 @return String
	 */
	public String getUser() {
		return user;
	}
	
	/** Returns the password in clear text.
	 @return String
	 */
	public String getPassword() {
		return password;
	}
}