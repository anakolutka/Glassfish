/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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

package com.sun.enterprise.v3.deployment;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.Param;
import org.glassfish.api.I18n;
import org.glassfish.api.container.Sniffer;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import com.sun.enterprise.v3.server.ApplicationLifecycle;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Engine;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;
import java.util.List;

/**
 * list-components command
 */
@Service(name="list-components")
@I18n("list.components")
@Scoped(PerLookup.class)
public class ListComponentsCommand extends ApplicationLifecycle implements AdminCommand {

    @Param(optional=true)
    String type = null;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListComponentsCommand.class);    

    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();

        ActionReport.MessagePart part = report.getTopMessagePart();        
        int numOfApplications = 0;
        for (Module module : applications.getModules()) {
            if (module instanceof Application) {
                final Application app = (Application)module;
                if (app.getObjectType().equals("user")) {
                    if (type==null || isApplicationOfThisType(app, type)) {
                        ActionReport.MessagePart childPart = part.addChild();
                        childPart.setMessage(app.getName() + " " +
                                             getSnifferEngines(app, true));
                        numOfApplications++;
                    }
                }
            }
        }
        if (numOfApplications == 0) {
            part.setMessage(localStrings.getLocalString("list.components.no.elements.to.list", "Nothing to List."));            
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

        /**
         * check the type of application by comparing the sniffer engine.
         * @param app - Application
         * @param type - type of application
         * @return true if application is of the specified type else return
         *  false.
         */
    boolean isApplicationOfThisType(final Application app, final String type) {
        final List<Engine> engineList = app.getEngine();
        for (Engine engine : engineList) {
            if (engine.getSniffer().equals(type)) {
                return true;
            }
        }
        return false;
    }

        /**
         * return all user visible sniffer engines in an application.
         * The return format is <sniffer1, sniffer2, ...>
         * @param app - Application
         * @return sniffer engines
         */
    String getSnifferEngines(final Application app, final boolean format) {
        final List<Engine> engineList = app.getEngine();
        StringBuffer se = new StringBuffer();
        if (!engineList.isEmpty()) {
            if (format) {
                se.append("<");
            }
            for (Engine engine : engineList) {
                final String engType = engine.getSniffer();
                if (displaySnifferEngine(engType)) {
                    se.append(engine.getSniffer() + ", ");
                }
            }
                //eliminate the last "," and end the list with ">"
            if (se.length()>2) {
                se.replace(se.length()-2, se.length(), (format)?">":"");
            } else if (format) {
                se.append(">");
            }
        }
        return se.toString();
    }

        /**
         * check to see if Sniffer engine is to be visible by the user.
         *
         * @param engType - type of sniffer engine
         * @return true if visible, else false.
         */
    boolean displaySnifferEngine(String engType) {
        final Sniffer sniffer = snifferManager.getSniffer(engType);
        return sniffer.isUserVisible();
    }
}
