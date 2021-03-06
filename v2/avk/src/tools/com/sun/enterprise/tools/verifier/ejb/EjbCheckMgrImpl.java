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
package com.sun.enterprise.tools.verifier.ejb;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Set;

import com.sun.ejb.codegen.GeneratorException;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.io.EjbDeploymentDescriptorFile;
import com.sun.enterprise.deployment.util.EjbBundleValidator;
import com.sun.enterprise.deployment.util.ModuleDescriptor;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.dd.ParseDD;
import com.sun.enterprise.tools.verifier.wsclient.WebServiceClientCheckMgrImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.jdo.spi.persistence.support.ejb.ejbc.JDOCodeGenerator;

/**
 * Ejb harness
 */
public class EjbCheckMgrImpl extends CheckMgr implements JarCheck {

    /**
     * name of the file containing the list of tests for the ejb architecture
     */
    private static final String testsListFileName = "TestNamesEjb.xml"; // NOI18N
    private static final String sunONETestsListFileName = getSunPrefix()
            .concat(testsListFileName);
    // the JDO Code generator needs to be initialized once per BundleDescriptor
    private JDOCodeGenerator jdc = new JDOCodeGenerator();

    public EjbCheckMgrImpl(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
    }

    /**
     * Check Ejb for spec. conformance
     *
     * @param descriptor Ejb descriptor
     */
    public void check(Descriptor descriptor) throws Exception {
        // run persistence tests first.
        checkPersistenceUnits(EjbBundleDescriptor.class.cast(descriptor));
        // an EjbBundleDescriptor can have an WebServicesDescriptor
        checkWebServices(descriptor);
        // an EjbBundleDescriptor can have  WebService References
        checkWebServicesClient(descriptor);

        if (frameworkContext.isPartition() &&
                !frameworkContext.isEjb())
            return;

        EjbBundleDescriptor bundleDescriptor = (EjbBundleDescriptor) descriptor;
        setDescClassLoader(bundleDescriptor);
        // DOL (jerome): is asking us to call this in some cases, like when
        // an ejb-ref is unresolved etc.
        try {
            EjbBundleValidator validator = new EjbBundleValidator();
            validator.accept(bundleDescriptor);
        } catch (Exception e) {
        } // nothing can be done
        
        // initialize JDOC if bundle has CMP's
        if (bundleDescriptor.containsCMPEntity()) {
            try {
                // See bug #6274161. We now pass an additional boolean
                // to indicate whether we are in portable or AS mode.
                jdc.init(bundleDescriptor, context.getClassLoader(),
                        getAbstractArchiveUri(bundleDescriptor),
                        frameworkContext.isPortabilityMode());
            } catch (Throwable ex) {
                context.setJDOException(ex);
            }
        }
        // set the JDO Codegenerator into the context
        context.setJDOCodeGenerator(jdc);
        
        // run the ParseDD test
        if (bundleDescriptor.getSpecVersion().compareTo("2.1") < 0) { // NOI18N
            EjbDeploymentDescriptorFile ddf = new EjbDeploymentDescriptorFile();
            File file = new File(getAbstractArchiveUri(bundleDescriptor),
                    ddf.getDeploymentDescriptorPath());
            FileInputStream is = new FileInputStream(file);
            try {
                if (is != null) {
                    Result result = new ParseDD().validateEJBDescriptor(is);
                    result.setComponentName(new File(bundleDescriptor.getModuleDescriptor().
                            getArchiveUri()).getName());
                    setModuleName(result);
                    frameworkContext.getResultManager().add(result);
                }
            } finally {
                try {
                    if(is != null) {
                        is.close();
                    }
                } catch (Exception e) {}
            }
        }

        for (Iterator itr = bundleDescriptor.getEjbs().iterator();
             itr.hasNext();) {
            EjbDescriptor ejbDescriptor = (EjbDescriptor) itr.next();
            super.check(ejbDescriptor);
        }

        if (bundleDescriptor.containsCMPEntity() &&
                context.getJDOException() == null) {
            try {
                jdc.cleanup();
            } catch (GeneratorException ge) {
            } // eat up the exception
            context.setJDOCodeGenerator(null);
        }
    }

    /**
     * return the configuration file name for the list of tests pertinent to the
     * connector architecture
     *
     * @return <code>String</code> filename containing the list of tests
     */
    protected String getTestsListFileName() {
        return testsListFileName;
    }

    /**
     * @return <code>String</code> filename containing sunone tests
     */
    protected String getSunONETestsListFileName() {
        return sunONETestsListFileName;
    }

    protected void checkWebServicesClient(Descriptor descriptor)
            throws Exception {
        if (frameworkContext.isPartition() &&
                !frameworkContext.isWebServicesClient())
            return;
        EjbBundleDescriptor desc = (EjbBundleDescriptor) descriptor;
        WebServiceClientCheckMgrImpl webServiceClientCheckMgr = new WebServiceClientCheckMgrImpl(
                frameworkContext);
        if (desc.hasWebServiceClients()) {
            Set ejbdescs = desc.getEjbs();
            Iterator ejbIt = ejbdescs.iterator();

            while (ejbIt.hasNext()) {
                EjbDescriptor ejbDesc = (EjbDescriptor) ejbIt.next();
                context.setEjbDescriptorForServiceRef(ejbDesc);
                Set serviceRefDescriptors = ejbDesc.getServiceReferenceDescriptors();
                Iterator it = serviceRefDescriptors.iterator();
                while (it.hasNext()) {
                    webServiceClientCheckMgr.setVerifierContext(context);
                    webServiceClientCheckMgr.check(
                            (ServiceReferenceDescriptor) it.next());
                }
            }
            context.setEjbDescriptorForServiceRef(null);
        }
    }

    protected String getSchemaVersion(Descriptor descriptor) {
        return getBundleDescriptor(descriptor).getSpecVersion();
    }

    protected void setModuleName(Result r) {
        r.setModuleName(Result.EJB);
    }

    protected EjbBundleDescriptor getBundleDescriptor(Descriptor descriptor) {
        return ((EjbDescriptor) descriptor).getEjbBundleDescriptor();
    }

    /**
     * entity and mdb assertions should not be run for session descriptors and 
     * similarly the other way round.
     */ 
    protected boolean isApplicable(TestInformation test, Descriptor descriptor) {
        String testName = test.getClassName();
        if(descriptor instanceof EjbSessionDescriptor && 
                (testName.indexOf("tests.ejb.entity")>=0 || // NOI18N
                testName.indexOf("tests.ejb.messagebean")>=0)) // NOI18N
            return false;
        if(descriptor instanceof EjbEntityDescriptor && 
                (testName.indexOf("tests.ejb.session")>=0 || // NOI18N
                testName.indexOf("tests.ejb.messagebean")>=0)) // NOI18N
            return false;
        if(descriptor instanceof EjbMessageBeanDescriptor && 
                (testName.indexOf("tests.ejb.session")>=0 || // NOI18N
                testName.indexOf("tests.ejb.entity")>=0)) // NOI18N
            return false;
        return true;
    }

    private String getAbstractArchiveUri(EjbBundleDescriptor desc) {
        String archBase = context.getAbstractArchive().getArchiveUri();
        ModuleDescriptor mdesc = desc.getModuleDescriptor();
        if(mdesc.isStandalone()) {
            return archBase;
        } else {
            return archBase + File.separator +
                    FileUtils.makeFriendlyFileName(mdesc.getArchiveUri());
        }
    }

    private void setDescClassLoader(EjbBundleDescriptor bundleDescriptor) {
        Iterator bundleItr = bundleDescriptor.getEjbs().iterator();
        while (bundleItr.hasNext()) {
            EjbDescriptor descriptor = (EjbDescriptor) bundleItr.next();
            if (descriptor instanceof IASEjbCMPEntityDescriptor) {
                ((IASEjbCMPEntityDescriptor) (descriptor)).setClassLoader(
                        context.getClassLoader());
            }
        }
    }

    protected ComponentNameConstructor getComponentNameConstructor(
            Descriptor descriptor) {
        return new ComponentNameConstructor((EjbDescriptor)descriptor);
    }

}
