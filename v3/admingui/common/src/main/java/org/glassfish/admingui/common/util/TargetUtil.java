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
 *
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.glassfish.admingui.common.handlers.RestApiHandlers;

/**
 *
 * @author anilam
 */
public class TargetUtil {

    public  boolean isCluster(String name){
        return false;
    }

    public static List getStandaloneInstances(){
        List result = new ArrayList();
        String endpoint = GuiUtil.getSessionValue("REST_URL") + "/list-instances" ;
        Map attrsMap = new HashMap();
        attrsMap.put("standaloneonly", "true");
        try{
            //TODO:  need to change when switching to json
            Map responseMap = RestApiHandlers.restRequest( endpoint , attrsMap, "get" , null);
            ArrayList  messages = (ArrayList) responseMap.get("messages");
            Map message = (Map) messages.get(0);
            List<Map<String, String>> props = (List<Map<String, String>>) message.get("properties");
            if (props == null){
                return result;
            }
            for(Map<String, String> oneProp : props){
                result.add(oneProp.get("name"));
            }
        }catch (Exception ex){
            GuiUtil.getLogger().severe("Error in getStandaloneInstances ; \nendpoint = " +endpoint + ", attrsMap=" + attrsMap);
        }

        return result;
    }

    public static List getClusters(){
        List clusters = new ArrayList();
        try{
            clusters = RestApiHandlers.getChildrenNames(GuiUtil.getSessionValue("REST_URL") + "/clusters/cluster", "Name");
        }catch (Exception ex){
            GuiUtil.getLogger().severe("Error in getClusters;");
            ex.printStackTrace();
        }
        return clusters;
    }

}
