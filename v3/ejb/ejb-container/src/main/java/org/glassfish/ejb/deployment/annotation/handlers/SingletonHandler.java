package org.glassfish.ejb.deployment.annotation.handlers;


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

import org.jvnet.hk2.annotations.Service;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.ejb.annotation.handlers.AbstractEjbHandler;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbSessionDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.DependsOn;

import org.glassfish.ejb.deployment.EjbSingletonDescriptor;

/**
 * This handler is responsible for handling the javax.ejb.Singleton
 *
 * @author Shing Wai Chan
 */
@Service
public class SingletonHandler extends AbstractEjbHandler {

    /** Creates a new instance of StatelessHandler */
    public SingletonHandler() {
        System.out.println("Created SingletonHandler......");
    }

    /**
     * @return the annoation type this annotation handler is handling
     */
    public Class<? extends Annotation> getAnnotationType() {
        return Singleton.class;
    }

    /**
     * Return the name attribute of given annotation.
     * @param annotation
     * @return name
     */
    protected String getAnnotatedName(Annotation annotation) {
        Singleton slAn = (Singleton) annotation;
        return slAn.name();
    }

    /**
     * Check if the given EjbDescriptor matches the given Annotation.
     * @param ejbDesc
     * @param annotation
     * @return boolean check for validity of EjbDescriptor
     */
    protected boolean isValidEjbDescriptor(EjbDescriptor ejbDesc,
            Annotation annotation) {
        return EjbSessionDescriptor.TYPE.equals(ejbDesc.getType());
    }

    /**
     * Create a new EjbDescriptor for a given elementName and AnnotationInfo.
     * @param elementName
     * @param ainfo
     * @return a new EjbDescriptor
     */
    protected EjbDescriptor createEjbDescriptor(String elementName,
            AnnotationInfo ainfo) throws AnnotationProcessorException {

        AnnotatedElement ae = ainfo.getAnnotatedElement();
        Class ejbClass = (Class)ae;
        EjbSingletonDescriptor newDescriptor = new EjbSingletonDescriptor();
        newDescriptor.setName(elementName);
        newDescriptor.setEjbClassName(ejbClass.getName());
        newDescriptor.setSingletonClass(ejbClass);

        doSingletonSpecificProcessing(newDescriptor);

        return newDescriptor;
    }

    /**
     * Set Annotation information to Descriptor.
     * This method will also be invoked for an existing descriptor with
     * annotation as user may not specific a complete xml.
     * @param ejbDesc
     * @param ainfo
     * @return HandlerProcessingResult
     */
    protected HandlerProcessingResult setEjbDescriptorInfo(
            EjbDescriptor ejbDesc, AnnotationInfo ainfo)
            throws AnnotationProcessorException {

        EjbSingletonDescriptor ejbSingletonDescriptor = (EjbSingletonDescriptor) ejbDesc;

        Singleton singleton = (Singleton) ainfo.getAnnotation();

        doDescriptionProcessing(singleton.description(), ejbDesc);
        doMappedNameProcessing(singleton.mappedName(), ejbDesc);

        doSingletonSpecificProcessing(ejbSingletonDescriptor);

        return setBusinessAndHomeInterfaces(ejbDesc, ainfo);
    }

    private void doSingletonSpecificProcessing(EjbSingletonDescriptor desc) {
        Class clz = desc.getSingletonClass();
        Startup st = (Startup) clz.getAnnotation(Startup.class);
        if (st != null) {
            desc.setStartup();
        }

        DependsOn dep = (DependsOn) clz.getAnnotation(DependsOn.class);
        if (dep != null) {
            desc.setDepends(dep.value());
        }
    }
}
