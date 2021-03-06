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

/*
 * $Id: HttpListenerMBean.java,v 1.3 2005/12/25 03:42:21 tcfujii Exp $
 */

package com.sun.enterprise.admin.mbeans;

import java.util.Map;
import java.util.logging.Level;

import javax.management.*;

import com.sun.enterprise.admin.util.jmx.AttributeListUtils;
import com.sun.enterprise.config.ConfigException;
import com.sun.enterprise.admin.config.BaseConfigMBean;

public class HttpListenerMBean extends BaseConfigMBean 
    implements MBeanRegistration
{
    private ObjectName on;

    static final String DEFAULT_VIRTUAL_SERVER = "default_virtual_server";

    public void setAttribute(Attribute attribute)
        throws AttributeNotFoundException, MBeanException, ReflectionException
    {
        String oldVs = null;
        if (isDefaultVirtualServer(attribute))
        {
            oldVs = (String)super.getAttribute(DEFAULT_VIRTUAL_SERVER);
        }

        super.setAttribute(attribute);

        if (isDefaultVirtualServer(attribute))
        {
            try
            {
                changeHttpListenerRef(oldVs, (String)attribute.getValue());
            }
            catch (ConfigException ce)
            {
                throw new MBeanException(ce);
            }
        }
    }

    public AttributeList setAttributes(AttributeList list)
    {
        String oldVs = null;
        if (isDefaultVirtualServerExists(list))
        {
            try
            {
                oldVs = (String)super.getAttribute(DEFAULT_VIRTUAL_SERVER);
            }
            catch (JMException e)
            {
                //Ignoring. This should not occur.
            }
        }
        AttributeList al = super.setAttributes(list);
        if (isDefaultVirtualServerExists(list) && 
            isDefaultVirtualServerExists(al))
        {
            try
            {
                changeHttpListenerRef(oldVs, getDefaultVirtualServer(list));
            }
            catch (ConfigException ce)
            {
                _sLogger.log(Level.WARNING, 
                    "httplistenerMBean.failed_to_add_http_listener_ref",ce);
            }
        }
        return al;
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name)
        throws Exception
    {
        on = super.preRegister(server, name);
        return on;
    }

    private void changeHttpListenerRef(String oldVs, String newVs) 
        throws ConfigException
    {
        HttpListenerVirtualServerAssociationMgr mgr = 
            new HttpListenerVirtualServerAssociationMgr(
                super.getConfigContext(), on.getKeyProperty("config"));
        final String id = on.getKeyProperty("id");
        mgr.changeHttpListenerRef(id, oldVs, newVs);
    }

    private boolean isDefaultVirtualServer(Attribute a)
    {
        return a.getName().equals(DEFAULT_VIRTUAL_SERVER);
    }

    private boolean isDefaultVirtualServerExists(AttributeList al)
    {
        return AttributeListUtils.containsNamedAttribute(al, 
                DEFAULT_VIRTUAL_SERVER);
    }

    private String getDefaultVirtualServer(AttributeList al)
    {
        final Map map = AttributeListUtils.asNameMap(al);
        final Attribute val = (Attribute)map.get(DEFAULT_VIRTUAL_SERVER);
        return (String)val.getValue();
    }
}
