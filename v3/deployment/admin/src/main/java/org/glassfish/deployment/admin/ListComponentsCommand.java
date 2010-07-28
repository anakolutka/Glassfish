/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2010 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.deployment.admin;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.Cluster;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.Param;
import org.glassfish.api.I18n;
import org.glassfish.api.container.Sniffer;
import org.glassfish.internal.deployment.SnifferManager;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.deployment.common.DeploymentUtils;
import org.jvnet.hk2.annotations.Service;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PerLookup;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * list-components command
 */
@Service(name="list-components")
@I18n("list.components")
@Scoped(PerLookup.class)
@Cluster(value={RuntimeType.DAS})
@TargetType(value={CommandTarget.DOMAIN, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
public class ListComponentsCommand  implements AdminCommand {

    @Param(optional=true)
    String type = null;

    @Param(primary=true, optional=true)
    public String target = "server";

    @Param(optional=true, defaultValue="false", shortName="v")
    public Boolean verbose = false;

    @Inject
    protected Domain domain;

    @Inject
    SnifferManager snifferManager;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListComponentsCommand.class);    

    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();

        ActionReport.MessagePart part = report.getTopMessagePart();        
        int numOfApplications = 0;
        for (Application app : domain.getApplicationsInTarget(target)) {
                if (app.getObjectType().equals("user")) {
                    if (type==null || isApplicationOfThisType(app, type)) {
                        ActionReport.MessagePart childPart = part.addChild();
                        String message = app.getName() + " "
                                    + getAppSnifferEngines(app, true);
                        if( verbose ){
                            message += getVerboseStatus(app);
                        }
                        childPart.setMessage(message);
                        part.addProperty(app.getName(), 
                            getAppSnifferEngines(app, false));
                        numOfApplications++;
                    }
                }
        }
        if (numOfApplications == 0) {
            part.setMessage(localStrings.getLocalString("list.components.no.elements.to.list", "Nothing to List."));            
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private String getVerboseStatus(Application app) {
       String message = "";
       if (DeploymentUtils.isDomainTarget(target)) {
           // ignore --verbose for target domain 
           return message;
       }
       boolean isVersionEnabled = domain.isAppEnabledInTarget(app.getName(), target);
       if ( isVersionEnabled ) {
           message = localStrings.getLocalString("list.applications.verbose.enabled", "(enabled)");
       } else {
           message = localStrings.getLocalString("list.applications.verbose.disabled", "(disabled)");
       }
       return message;
   }


        /**
         * check the type of application by comparing the sniffer engine.
         * @param app - Application
         * @param type - type of application
         * @return true if application is of the specified type else return
         *  false.
         */
    boolean isApplicationOfThisType(final Application app, String type) {
        // do the type conversion to be compatible with v2 syntax
        if (type.equals("application")) {
            type = "ear";
        } else if (type.equals("webservice")) {
            type = "webservices";
        }

        List <Engine> engineList = getAppEngines(app);
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
     * @param module - Application's module
     * @return sniffer engines
     */
    String getAppSnifferEngines(final Application app, final boolean format) {
        return getSniffers(getAppEngines(app), format);
    }

    /**
     * return all user visible sniffer engines in an application.
     * The return format is <sniffer1, sniffer2, ...>
     * @param module - Application's module
     * @return sniffer engines
     */
    String getSnifferEngines(final Module module, final boolean format) {
        return getSniffers (module.getEngines(), format);
    }

    private String getSniffers(final List<Engine> engineList, 
        final boolean format) {
        Set<String> snifferSet = new LinkedHashSet<String>();
        for (Engine engine : engineList) {
            final String engType = engine.getSniffer();
            if (displaySnifferEngine(engType)) {
                snifferSet.add(engine.getSniffer());
            }
        }

        StringBuffer se = new StringBuffer();

        if (!snifferSet.isEmpty()) {
            if (format) {
                se.append("<");
            }
            for (String sniffer : snifferSet) {
                se.append(sniffer + ", ");
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

    private List<Engine> getAppEngines(final Application app) {
        final List<Engine> engineList = new ArrayList<Engine>();

        // first add application level engines
        engineList.addAll(app.getEngine());

        // now add module level engines
        for (Module module: app.getModule()) {
            engineList.addAll(module.getEngines());
        }

        return engineList;
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
