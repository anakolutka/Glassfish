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

package com.sun.enterprise.cli.commands;

import com.sun.enterprise.cli.framework.*;
import com.sun.enterprise.config.serverbeans.ServerTags;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.List;

public class ListMBeansCommand extends GenericCommand 
{
    private final static String ENABLED_OPERATION = "isMBeanEnabled"; 
    /**
     *  An abstract method that Executes the command
     *  @throws CommandException
     */
    public void runCommand() throws CommandException, CommandValidationException
    {
        if (!validateOptions())
            throw new CommandValidationException("Validation is false");

            //use http connector
        MBeanServerConnection mbsc = getMBeanServerConnection(getHost(), getPort(), 
                                                              getUser(), getPassword());
        final String objectName = getObjectName();
        final Object[] params = getParamsInfo();
        final String operationName = getOperationName();
        final String[] types = getTypesInfo();

        try
        { 
	    //if (System.getProperty("Debug") != null) printDebug(mbsc, objectName);
            List returnValue = (List) mbsc.invoke(new ObjectName(objectName), 
					     operationName, params, types);
            if (returnValue.size() == 0)
            {
                CLILogger.getInstance().printDetailMessage(
                                            getLocalizedString("NoElementsToList"));
            }
            ObjectName objName;
            String target = getOperands().size() > 0 ? 
                            (String) getOperands().get(0):null;
            CLILogger.getInstance().printDebugMessage("target = " + target);
            CLILogger.getInstance().printDebugMessage("list size = " + returnValue.size());
            
            for (int ii=0; ii<returnValue.size(); ii++)
            {
                objName = (ObjectName) returnValue.get(ii);
                CLILogger.getInstance().printDebugMessage("objName = " + objName.toString());
                String mbeanName = (String) mbsc.getAttribute(objName, ServerTags.NAME);
                String mbeanObjName = (String) mbsc.getAttribute(objName, 
                                                                ServerTags.OBJECT_NAME);
                final String boolVal = (String) mbsc.getAttribute(objName, ServerTags.ENABLED);
                boolean enabled = Boolean.valueOf(boolVal);

                //CLILogger.getInstance().printDebugMessage("enabled = " + enabled);
                String printString = mbeanName + " " + mbeanObjName + " " + 
                                        (enabled ? getLocalizedString("Enabled")
                                                : getLocalizedString("Disabled"));
                CLILogger.getInstance().printMessage(printString);
            }
            
	    CLILogger.getInstance().printDetailMessage(getLocalizedString(
						       "CommandSuccessful",
						       new Object[] {name}));
        }
        catch(Exception e)
        {
            displayExceptionMessage(e);
        }        
    }
}
