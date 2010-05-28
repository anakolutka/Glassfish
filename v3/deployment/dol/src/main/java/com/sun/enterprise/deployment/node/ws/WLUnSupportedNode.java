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

package com.sun.enterprise.deployment.node.ws;

import com.sun.enterprise.deployment.Descriptor;
import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.w3c.dom.Node;

/**
 * This class represents nodes that are not supported.
 * Basically it consumes all sub nodes of the unsupported node and ignores them.
 *
 * @author Rama Pulavarthi
 */
class WLUnSupportedNode extends DeploymentDescriptorNode {
    private final XMLElement tag;

    public WLUnSupportedNode(XMLElement tag) {
        this.tag = tag;
    }

    @Override
    protected XMLElement getXMLRootTag() {
        return tag;
    }

    @Override
    public void setElementValue(XMLElement element, String value) {
        //ignore all sub nodes
    }

    public Node writeDescriptor(Node parent, String nodeName,
                                Descriptor descriptor) {
        // This node does not preserve the original node information
        // TODO, Fix to write it back.
        return parent;
    }

    @Override
    public Object getDescriptor() {
        return null;
    }

    @Override
    public void addDescriptor(Object descriptor) {
        //None of the sub nodes should call addDescriptor() on this node.
        // as this configuration only supplements webservices.xml configuration and
        // does not create new web services.
        DOLUtils.getDefaultLogger().info("Warning: For node" + tag +
                ", addDescriptor() should not have been called by" + descriptor.toString());

    }
}
