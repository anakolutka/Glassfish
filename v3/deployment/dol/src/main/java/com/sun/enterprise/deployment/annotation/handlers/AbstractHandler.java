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
package com.sun.enterprise.deployment.annotation.handlers;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.MessageDriven;
import javax.ejb.Stateful;
import javax.ejb.Stateless;

import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.apf.ResultType;
import org.glassfish.apf.impl.AnnotationUtils;
import org.glassfish.apf.impl.HandlerProcessingResultImpl;

import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * This is an abstract base class for Handlers.
 * Concrete subclass need to implements the following methods:
 *     public Class&lt;? extends Annotation&gt; getAnnotationType();
 *     public HandlerProcessingResult processAnnotation(AnnotationInfo ainfo) 
 *          throws AnnotationProcessorException;
 *
 * @author Shing Wai Chan
 */
public abstract class AbstractHandler implements AnnotationHandler {
    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(AbstractHandler.class);
    protected Logger logger = AnnotationUtils.getLogger();

    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    public Class<? extends Annotation>[] getTypeDependencies() {
        return null;
    }

    // ----- end of implements AnnotationHandler -----

    /**
     * @return a default processed result
     */
    protected HandlerProcessingResult getDefaultProcessedResult() {
        return HandlerProcessingResultImpl.getDefaultResult(
                getAnnotationType(), ResultType.PROCESSED);
    }

    /**
     * @return a default failed result
     */
    protected HandlerProcessingResult getDefaultFailedResult() {
        return HandlerProcessingResultImpl.getDefaultResult(
                getAnnotationType(), ResultType.FAILED);
    }

    /**
     * @param aeHandler
     * @param ainfo
     * @return a result for invalid AnnotatedElementHandler
     */
    protected HandlerProcessingResult getInvalidAnnotatedElementHandlerResult(
            AnnotatedElementHandler aeHandler, AnnotationInfo ainfo)
            throws AnnotationProcessorException {

        if (logger.isLoggable(Level.FINE)) {
            log(Level.FINE, ainfo, 
                localStrings.getLocalString(
                "enterprise.deployment.annotation.handlers.invalidaehandler",
                "Invalid annotation symbol found for this type of class."));
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Invalid AnnotatedElementHandler: " + aeHandler);
        }

        return getDefaultProcessedResult();
    }

    protected void log(Level level, AnnotationInfo ainfo,
            String localizedMessage) throws AnnotationProcessorException {
        if (Level.SEVERE.equals(level)) {
            ainfo.getProcessingContext().getErrorHandler().error(
                new AnnotationProcessorException(localizedMessage, ainfo));
        } else if (Level.WARNING.equals(level)) {
            ainfo.getProcessingContext().getErrorHandler().warning(
                new AnnotationProcessorException(localizedMessage, ainfo));
        } else if (Level.FINE.equals(level)) {
            ainfo.getProcessingContext().getErrorHandler().fine(
                new AnnotationProcessorException(localizedMessage, ainfo));
        } else if (ainfo != null) {
            ainfo.getProcessingContext().getProcessor().log(
                level, ainfo, localizedMessage);
        } else {
            logger.log(level, localizedMessage);
        }
    }

    protected String getInjectionMethodPropertyName(Method method,
            AnnotationInfo ainfo) throws AnnotationProcessorException
    {
        String methodName = method.getName();
        String propertyName = methodName;

        if( (methodName.length() > 3) &&
            methodName.startsWith("set") ) {
            // Derive javabean property name.
            propertyName = 
                methodName.substring(3, 4).toLowerCase() +
                methodName.substring(4);
        }  else {
            throw new AnnotationProcessorException(
                localStrings.getLocalString(
                "enterprise.deployment.annotation.handlers.invalidinjectionmethodname",
                "Injection method name must start with \"set\""),
                ainfo);
        }

        return propertyName;
    }

    /**
     * Check if given method is a valid injection method.
     * Throw Exception if it is not.
     * @exception AnnotationProcessorException
     */
    protected void validateInjectionMethod(Method method, AnnotationInfo ainfo)
            throws AnnotationProcessorException {
        if (method.getParameterTypes().length!=1){
            throw new AnnotationProcessorException(
                localStrings.getLocalString(
                "enterprise.deployment.annotation.handlers.invalidinjectionmethod",
                "Injection on a method requires a JavaBeans setter method type with one parameter "),
                ainfo);
                
        }
        if (!void.class.equals(method.getReturnType())) {
            throw new AnnotationProcessorException(
                localStrings.getLocalString(
                "enterprise.deployment.annotation.handlers.injectionmethodmustreturnvoid",
                "Injection on a method requires a void return type"),
                ainfo);
        }
    }

    protected HandlerProcessingResult getOverallProcessingResult(
            List<HandlerProcessingResult> resultList) {
        HandlerProcessingResult overallProcessingResult = null;
        for (HandlerProcessingResult result : resultList) {
            if (overallProcessingResult == null ||
                    (result.getOverallResult().compareTo(
                    overallProcessingResult.getOverallResult()) > 0)) {
                overallProcessingResult = result;
            }
        }
        return overallProcessingResult;
    }

    /**
     * This is called by getTypeDependencies().
     * @return an array of all ejb annotation types 
     */
    protected Class<? extends Annotation>[] getEjbAnnotationTypes() {
        return new Class[] {
                MessageDriven.class, Stateful.class, Stateless.class };
    }
}
