/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.admin.rest.provider;

import com.sun.enterprise.v3.common.ActionReporter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.glassfish.admin.rest.results.ActionReportResult;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.Produces;
import org.glassfish.admin.rest.utils.xml.XmlArray;
import org.glassfish.admin.rest.utils.xml.XmlMap;
import org.glassfish.admin.rest.utils.xml.XmlObject;
import org.glassfish.api.ActionReport.MessagePart;

/**
 * @author Ludovic Champenois
 */
@Provider
@Produces(MediaType.APPLICATION_XML)
public class ActionReportResultXmlProvider extends BaseProvider<ActionReportResult> {
    public ActionReportResultXmlProvider() {
        super(ActionReportResult.class, MediaType.APPLICATION_XML_TYPE);
    }

    @Override
    public String getContent(ActionReportResult proxy) {
        ActionReporter ar = (ActionReporter)proxy.getActionReport();
        XmlObject result = processReport(ar);
        return result.toString();
    }

    protected XmlObject processReport(ActionReporter ar) {
        XmlMap result = new XmlMap("map");
        result.put("message", ar.getMessage());
        result.put("command", ar.getActionDescription());
        result.put("exit_code", ar.getActionExitCode().toString());

        Properties properties = ar.getTopMessagePart().getProps();
        if (!properties.isEmpty()) {
            result.put("properties", new XmlMap("properties", properties));
        }

        Properties extraProperties = ar.getExtraProperties();
        if ((extraProperties != null) && (!extraProperties.isEmpty())) {
            result.put("extraProperties", getExtraProperties(result, extraProperties));
        }

        List<MessagePart> children = ar.getTopMessagePart().getChildren();
        if (children.size() > 0) {
            result.put("children", processChildren(children));
        }

        List<ActionReporter> subReports = ar.getSubActionsReport();
        if (subReports.size() > 0) {
            result.put("subReports", processSubReports(subReports));
        }

        return result;
    }

    protected XmlArray processChildren(List<MessagePart> parts) {
        XmlArray array = new XmlArray("children");

        for (MessagePart part : parts) {
            XmlMap object = new XmlMap("part");
            object.put("message", part.getMessage());
            object.put("properties", new XmlMap("properties", part.getProps()));
            List<MessagePart> children = part.getChildren();
            if (children.size() > 0) {
                object.put("children", processChildren(part.getChildren()));
            }
            array.put(object);
        }

        return array;
    }

    protected XmlArray processSubReports(List<ActionReporter> subReports) {
        XmlArray array = new XmlArray("subReports");

        for (ActionReporter subReport : subReports) {
            array.put(processReport(subReport));
        }

        return array;
    }

    protected XmlMap getExtraProperties(XmlObject object, Properties props) {
        XmlMap extraProperties = new XmlMap("extraProperties");
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = entry.getKey().toString();
            Object value = getXmlObject(entry.getValue());
            extraProperties.put(key, value);
        }

        return extraProperties;
    }

    protected Object getXmlObject(Object object) {
        Object result = null;
        if (object instanceof Collection) {
            result = processCollection((Collection)object);
        } else if (object instanceof Map) {
            result = processMap((Map)object);
        } else if (object instanceof Number) {
            result = new XmlObject("number", (Number)object);
        } else if (object instanceof String) {
            result = object;
        } else {
            result = new XmlObject(object.getClass().getSimpleName(), object);
        }

        return result;
    }

    protected XmlArray processCollection(Collection c) {
        XmlArray result = new XmlArray("list");
        Iterator i = c.iterator();
        while (i.hasNext()) {
            Object item = i.next();
            Object obj = getXmlObject(item);
            if (!(obj instanceof XmlObject)) {
                obj = new XmlObject(obj.getClass().getSimpleName(), obj);
            }
            result.put((XmlObject)obj);
        }

        return result;
    }

    protected XmlMap processMap(Map map) {
        XmlMap result = new XmlMap("map");

        for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
            result.put(entry.getKey().toString(), getXmlObject(entry.getValue()));
        }

        return result;
    }

}