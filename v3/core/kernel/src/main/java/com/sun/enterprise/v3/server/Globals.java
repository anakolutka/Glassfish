/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * Header Notice in each file and include the License file 
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.  
 * If applicable, add the following below the CDDL Header, 
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */

package com.sun.enterprise.v3.server;

import com.sun.enterprise.module.ModulesRegistry;
import org.jvnet.hk2.component.Habitat;

/**
 * Very sensitive class, anything stored here cannot be garbage collected
 *
 * @author Jerome Dochez
 */
public class Globals {
    
    static Globals globals;
    
    Habitat defaultHabitat;
    ModulesRegistry registry;
    
    /** Creates a new instance of ServiceContext */
    public static synchronized Globals initialize(Habitat defaultHabitat, ModulesRegistry registry) {
        globals = new Globals();
        globals.registry = registry;
        globals.defaultHabitat = defaultHabitat;
        return globals;
    }
    
    public static Globals getGlobals() {
        return globals;
    }
    

    public Habitat getDefaultHabitat() {
        return defaultHabitat;
    }
    
    public ModulesRegistry getServiceRegistry() {
        return registry;
    }
}
