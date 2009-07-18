/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.glassfish.flashlight.datatree.TreeNode;
import  org.glassfish.api.statistics.Statistic;

/**
 * @author Rajeshwar Patil
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class TreeNodeJsonProvider extends ProviderUtil implements MessageBodyWriter<List<TreeNode>> {

     @Context
     protected UriInfo uriInfo;

     public long getSize(final List<TreeNode> proxy, final Class<?> type, final Type genericType,
               final Annotation[] annotations, final MediaType mediaType) {
          return -1;
     }


     public boolean isWriteable(final Class<?> type, final Type genericType,
               final Annotation[] annotations, final MediaType mediaType) {
         if ("java.util.List<org.glassfish.flashlight.datatree.TreeNode>".equals(genericType.toString())) {
             return mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
         }
         return false;
     }


     public void writeTo(final List<TreeNode> proxy, final Class<?> type, final Type genericType,
               final Annotation[] annotations, final MediaType mediaType,
               final MultivaluedMap<String, Object> httpHeaders,
               final OutputStream entityStream) throws IOException, WebApplicationException {
         entityStream.write(getJson(proxy).getBytes());
     }


     private String getJson(List<TreeNode> proxy) {
        String result;
        result ="{" ;
           result = result + getTypeKey();
           result = result + ":";
           result = result + "{";
             result = result + getAttributes(proxy);
           result = result + "}";
           result = result + ",";
           result = result + getResourcesKey();
           result = result + ":";
           result = result + "[";
             result = result + getResourcesLinks(proxy);
           result = result + "]";
        result = result + "}" ;
        return result;
    }


    private String getTypeKey() {
       return upperCaseFirstLetter(eleminateHypen(getName(uriInfo.getPath(), '/')));
    }


    private String getAttributes(List<TreeNode> nodeList) {
        String result ="";
        for (TreeNode node : nodeList) {
            //process only the leaf nodes, if any
            if (!node.hasChildNodes()) {
                //getValue() on leaf node will return one of the following -
                //Statistic object, String object or the object for primitive type
                result = result + quote(node.getName()) + " : " + jsonForNodeValue(node.getValue());
                result = result + ",";
            }
        }

        int endIndex = result.length() - 1;
        if (endIndex > 0) result = result.substring(0, endIndex );
        return result;
    }


    private String getResourcesKey() {
        return quote("child-resources");
    }


    private String getResourcesLinks(List<TreeNode> nodeList) {
        String result = "";
        String elementName;
        for (TreeNode node: nodeList) {
            //process only the non-leaf nodes, if any
            if (node.hasChildNodes()) {
                try {
                    elementName = node.getName();
                    result = result + quote(getElementLink(uriInfo, elementName));
                    result = result + ",";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        int endIndex = result.length() - 1;
        if (endIndex > 0) result = result.substring(0, endIndex);
        return result;
    }


    private String jsonForNodeValue(Object value) {
        String result ="";
        if (value == null) return result;

        try {
            if (value instanceof Statistic) {
                Statistic statisticObject = (Statistic)value;
                Map map = getStatistics(statisticObject);
                Set<String> attributes = map.keySet();
                Object attributeValue;
                result = result + "{";
                for (String attributeName: attributes) {
                    
                    attributeValue = map.get(attributeName);
                    result = result + quote(attributeName) + " : " + jsonValue(attributeValue);
                    result = result + ",";
                }

                int endIndex = result.length() - 1;
                if (endIndex > 0) result = result.substring(0, endIndex);

                result = result + "}";
                return result;
            }
        } catch (Exception exception) {
            //log exception message as warning
        }

        result = result + jsonValue(value);

        return result;
    }


    private String jsonValue(Object value) {
        String result ="";

        if (value.getClass().getName().equals("java.lang.String")) {
            result = quote(value.toString());
        } else {
            result =  value.toString();
        }

        return result;
    }
}
