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
 

package com.sun.appserv.management.ext.logging;


import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.XTypes;

/**
	Supports accessing logging information in multiple ways.  The following are supported:
	<ul>
	<li>Emission of pseudo real-time JMX Notifications when a
		log record is created--see {@link LogRecordEmitter}</li>
	<li>Access to existing log file contents--see {@link LogFileAccess}</li>
	<li>Querying for log entries--see {@link LogQuery}</li>
	</ul>
	<p>
	A Logging always has j2eeType={@link XTypes#LOGGING} and
	the same name as the server it represents.
	@since AS 9.0
	@see com.sun.appserv.management.monitor.ServerRootMonitor#getLogging
 */
public interface Logging
	extends LogRecordEmitter, LogQuery, LogFileAccess, LogAnalyzer, AMX
{
	public static final String	J2EE_TYPE			= XTypes.LOGGING;
	
   /**
		Sets the log level of the Logger for the specified module.  This operation
		will not effect a change to the corresponding loggin configuration for that module.
		<b>This capability is subject to removal, due to the confusion
		caused by configuration settings not matching</b>.
		
		@param module	a module name as specified in {@link LogModuleNames}.
		@param level	a log level
    	@see com.sun.appserv.management.config.ModuleLogLevelsConfig
     */
    public void setModuleLogLevel( String module, String level );
    
   /**
   		Gets the log level of the Logger for the specified module, which may or may not
   		be the same as that found in the configuration.
		<b>This capability is subject to removal, due to the confusion
		caused by configuration settings not matching</b>.
   		
   		@param moduleName a module name as specified in {@link LogModuleNames}
   		@see com.sun.appserv.management.config.ModuleLogLevelsConfig
    */
    public String getModuleLogLevel( String moduleName );
    
    /**
        This method may be used to verify that your Logging listener is working
        correctly.
        @param level the log level of the log message.
        @param message  the message to be placed in Notif.getMessage()
     */
    public void testEmitLogMessage( final String level, final String message );
}







