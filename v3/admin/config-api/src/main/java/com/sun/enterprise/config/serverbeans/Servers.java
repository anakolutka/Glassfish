/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.enterprise.config.serverbeans;

import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.Delete;
import org.glassfish.config.support.TypeAndNameResolver;
import org.glassfish.config.support.TypeResolver;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.DuckTyped;

import java.util.List;

/**
 * List of configured servers.
 */
@Configured
public interface Servers extends ConfigBeanProxy, Injectable  {

    /**
     * Return the list of currently configured server. Servers can
     * be added or removed by using the returned {@link java.util.List}
     * instance
     * 
     * @return the list of configured {@link Server}
     */
    @Element
    // example below on how to annotate a CRUD command with cluster specific data.
    @Create(value="create-instance", resolver= TypeResolver.class, decorator= Server.CreateDecorator.class,
        cluster=@org.glassfish.api.admin.Cluster(value = RuntimeType.DAS))
    @Delete(value="delete-instance", resolver= TypeAndNameResolver.class, decorator=Server.DeleteDecorator.class)
    public List<Server> getServer();

    /**
     * Return the server with the given name, or null if no such server exists.
     *
     * @param   name    the name of the server
     * @return          the Server object, or null if no such server
     */
    @DuckTyped
    public Server getServer(String name);

    class Duck {
        public static Server getServer(Servers instance, String name) {
            for (Server server : instance.getServer()) {
                if (server.getName().equals(name)) {
                    return server;
                }
            }
            return null;
        }
    }
}
