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

package com.sun.enterprise.deployment.backend;

import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.io.*;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.archivist.Archivist;
import com.sun.enterprise.deployment.archivist.ArchivistFactory;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.deployment.backend.ClientJarMaker;
import com.sun.enterprise.deployment.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.util.ModuleDescriptor;
import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.deployment.io.DescriptorConstants;
import com.sun.enterprise.util.shared.ArchivistUtils;
import com.sun.enterprise.util.zip.ZipItem;
import com.sun.enterprise.v3.server.Globals;

/**
 * This class is responsible for creating an appclient jar file that
 * will be used by the appclient container to run the appclients for
 * the deployed application.
 *
 * @author Jerome Dochez
 */
class ClientJarMakerImpl implements ClientJarMaker {
    
    /**
     * Default constructor for this stateless object
     * @param props are the implementation properties (if any)
     */
    public ClientJarMakerImpl(Properties props) {
        this.props = props;
    }
    
    /**
     * creates the appclient container jar file
     * @param descriptor is the loaded module's deployment descriptor
     * @param source is the abstract archive for the source module deployed
     * @param target is the abstract archive for the desired appclient container jar file
     * @param stubs are the stubs generated by the deployment codegen
     * @param props is a properties collection to pass implementation parameters
     *
     * @throws IOException when the jar file creation fail
     */
    public void create(RootDeploymentDescriptor descriptor, 
        ReadableArchive source,
        WritableArchive target, ZipItem[] stubs, Properties props)
        throws IOException {
        create(descriptor, source, source, target, stubs, props);
    }

    /**
     * creates the appclient container jar file
     * @param descriptor is the loaded module's deployment descriptor
     * @param source is the abstract archive for the source module deployed
     * @param source is the abstract archive for the generated xml directory
     * @param target is the abstract archive for the desired appclient container jar file
     * @param stubs are the stubs generated by the deployment codegen
     * @param props is a properties collection to pass implementation parameters
     *
     * @throws IOException when the jar file creation fail
     */
    public void create(RootDeploymentDescriptor descriptor, ReadableArchive source,
        ReadableArchive source2, WritableArchive target,ZipItem[] stubs, 
        Properties props) throws IOException {
        
        ArchivistFactory archivistFactory = Globals.getGlobals().
            getDefaultHabitat().getComponent(ArchivistFactory.class);

        // in all cases we copy the stubs file in the target archive
        Set elements = new HashSet();
        for (int i=0; i<stubs.length;i++) {
            ZipItem item = stubs[i];
            if (elements.contains(item.getName())) {
                continue;
            }
            elements.add(item.getName());
            OutputStream os = null;
            InputStream is = null;
            try {
                os = target.putNextEntry(item.getName());
                is = new BufferedInputStream(new FileInputStream(item.getFile()));
                ArchivistUtils.copyWithoutClose(is, os);
            } finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    target.closeEntry();
                }
            }
        }
        Vector moduleNames = new Vector();
        
        if (descriptor.isApplication()) {
            Application app = (Application) descriptor;
            for (ModuleDescriptor md : app.getModules()) {
                Archivist moduleArchivist = archivistFactory.getPrivateArchivistFor(md.getModuleType());
                
                ReadableArchive subSource = source.getSubArchive(md.getArchiveUri());
                ReadableArchive subSource2 = source2.getSubArchive(md.getArchiveUri());
                moduleNames.add(md.getArchiveUri());
                
                // any file that needs to be kept in the sub module should be 
                // calculated here
                Vector subEntries = new Vector();
                // manifest file always stay in embedded jar
                subEntries.add(JarFile.MANIFEST_NAME);
   
                BundleDescriptor subBundleDesc = 
                    (BundleDescriptor) md.getDescriptor(); 
                // all mapping file stay within the embedded jar
                WebServicesDescriptor wsd = subBundleDesc.getWebServices();
                if (wsd!=null) {
                    for (Iterator itr = wsd.getWebServices().iterator();itr.hasNext();) {
                        WebService ws = (WebService) itr.next();
                        subEntries.add(ws.getMappingFileUri());
                    }                
                }
                
                Set refs = subBundleDesc.getServiceReferenceDescriptors();
                for (Iterator itr = refs.iterator();itr.hasNext();) {
                    ServiceReferenceDescriptor srd = (ServiceReferenceDescriptor) itr.next();
                    subEntries.add(srd.getMappingFileUri());
                }
                
                // first copy original module files in the root on the target
                // except for .rar files contents.
                // We need to do it first so we save the list of files to be saved in the
                // embedded archive (for proper deployment descriptor loading)
                List embeddedFiles = new ArrayList();
                for (Enumeration e = subSource.entries();e.hasMoreElements();) {
                    
                    String entryName = (String) e.nextElement();
                    
                    // Deployment Descriptors (and associated) go in the embedded files
                    if (entryName.endsWith(".xml")  ||
                        subEntries.contains(entryName) ||
                        entryName.startsWith(subBundleDesc.getWsdlDir())) {
                        
	                        embeddedFiles.add(entryName);
                    } else {
                        try {
                            copy(subSource, target, entryName);
                        } catch(IOException ioe) {
                            // dup, we ignore
                        }
                    }
                }
                
                // now we need to copy the files we saved inside the embedded
                // archive file
                
                WritableArchive subTarget = target.createSubArchive(md.getArchiveUri());
                
                // and copy the list of identified files inside it

                // copy deployment descriptor files from generated xml directory
                for (Iterator itr = embeddedFiles.iterator();itr.hasNext();) {
                    String entryName = (String) itr.next();
                    copyWithOverride(subSource, subSource2, subTarget, entryName);
                }

                copy(subSource, subSource2, subTarget, 
                    moduleArchivist.getStandardDDFile().getDeploymentDescriptorPath(),
                    embeddedFiles);

                // every module may not have a sun descriptor, e.g. par file does not have one.
                if(moduleArchivist.getConfigurationDDFile()!=null) {
                    copy(subSource, subSource2, subTarget,
                        moduleArchivist.getConfigurationDDFile().getDeploymentDescriptorPath(),
                        embeddedFiles);
                }

                // and the manifest file since it does not appear in the list of files...
                copy(subSource, subTarget, JarFile.MANIFEST_NAME);
                
                // we do not need to copy anything else from the source embedded module
                // since all .class files and resources have moved at the top level of the target
                // application client container jar, so we can close out both subarchives
                target.closeEntry(subTarget);
                subSource.close();
                subSource2.close();
            }
        }
        // standalone modules and .ear file level entries fall back here, we
        // just need to copy the original archive file elements at the root level
        // of the target application client container jar file.
        Archivist archivist = archivistFactory.getPrivateArchivistFor(descriptor.getModuleType());

        // because of the backend layout, the appclient jar file appears in the list of files
        // in the source archive (which is the exploded directory where we started writing
        // the appclient file... this is also true when doing deploydir deployment
        String appClientFileName = target.getURI().getSchemeSpecificPart().substring(target.getURI().getSchemeSpecificPart().lastIndexOf(File.separatorChar)+1);

        // and the manifest file since it does not appear in the 
        // list of files...
        copy(source, target, JarFile.MANIFEST_NAME);
        
        List xmlFiles = new ArrayList();
        String libDir = computeLibraryDirectory(descriptor);
        for (Enumeration e = ((FileArchive)source).entries(moduleNames.elements());e.hasMoreElements();) {
            String entryName = (String) e.nextElement();
            
            // if this is the appclient we are creating, we pass
            if (entryName.equals(appClientFileName)) {
                continue;
            }
            
            // now we need to write the elements in the target file and explode
            // if it is a utility jar file
            if (entryName.endsWith(".jar") && ! inLibDirSubdirectory(libDir, entryName)) {
                // explode
                ReadableArchive subSource = null;
                try {
                    subSource = source.getSubArchive(entryName);
                    for (Enumeration subEntries = subSource.entries();subEntries.hasMoreElements();) {
                        String subEntryName = (String) subEntries.nextElement();
                        if(DescriptorConstants.PERSISTENCE_DD_ENTRY.equals(subEntryName)){
                            // If we copy DescriptorConstants.PAR_DD_ENTRY into
                            // *Client.jar then during subsequent app loading time
                            // server will treat that jar as another PU Root and try to load it.
                            // so don't copy such a file.
                            continue;
                        }
                        copy(subSource, target, subEntryName);
                    }
                } finally {
                    if (subSource != null) {
                        subSource.close();
                    }
                }
            } else {
                if (entryName.endsWith(".xml")) {
                    xmlFiles.add(entryName);
                }
                copyWithOverride(source, source2, target, entryName);
            }
        }

        copy(source, source2, target, 
            archivist.getStandardDDFile().getDeploymentDescriptorPath(),
            xmlFiles);
        copy(source, source2, target, 
            archivist.getConfigurationDDFile().getDeploymentDescriptorPath(),
            xmlFiles);
    }

    /**
     * copy corresponding deployment descriptor if necessary.
     * @param source original source
     * @param source2 overrided source
     * @param target
     * @param fileEntryName
     * @param xmlFiles list of xml files no need to copy
     * @exception IOException
     */
    private void copy(ReadableArchive source, ReadableArchive source2,
            WritableArchive target, String fileEntryName, List xmlFiles)
            throws IOException {
        if (!xmlFiles.contains(fileEntryName)) {
            copyWithOverride(source, source2, target, fileEntryName);
        }
    }
    
    /**
     * copy the entryName element from the source abstract archive into
     * the target abstract archive
     */
    private void copy(ReadableArchive source, WritableArchive target, String entryName)
        throws IOException {
            
        InputStream is=null;
        OutputStream os=null;
        try {
            is = source.getEntry(entryName);
            if (is != null) {
                try {
                    os = target.putNextEntry(entryName);
                } catch(ZipException ze) {
                    // this is a dup...
                    return;
                }
                ArchivistUtils.copyWithoutClose(is, os);
            }
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            IOException closeEntryIOException = null;
            if (os!=null) {
                try {
                    target.closeEntry();
                } catch (IOException ioe) { 
                    closeEntryIOException = ioe;
                }
            }
            if (is!=null) {
                is.close();
            }
 
            if (closeEntryIOException != null) {
                throw closeEntryIOException;
            } 
        }
    }
    
    /**
     *Copy an entry from the overriding source to the target if it appears in 
     *the overriding source. Otherwise copy if from the normal source.
     *@param normalSource the ReadableArchive from which to copy the entry if not present in overridingSource
     *@param overridingSource the overriding ReadableArchive from which to copy the entry
     *@param target the ReadableArchive into which to copy the entry
     *@param entryName the name of the entry to copy
     *@throws IOException in case of error attempting to get the specified entry
     */
    private void copyWithOverride(ReadableArchive normalSource, ReadableArchive overridingSource, WritableArchive target, String entryName) throws IOException {
        InputStream is = overridingSource.getEntry(entryName);
        boolean result = (is != null);
        if (is != null) {
            /*
             *If the getEntry succeeds, a stream has been opened so close it.
             */
            is.close();
            copy(overridingSource, target, entryName);
        } else {
            /*
             *The entry does not appear in the overriding source.  Copy from
             *the normal source.
             */
            copy(normalSource, target, entryName);
        }
    }

    /**
     * Returns the library directory setting for an application, or null if the
     * module type is not an application.
     *@param d the descriptor for the module being processed
     *@return the library directory for an Application; null for other module types
     */
    private String computeLibraryDirectory(RootDeploymentDescriptor d) {
        String result = null;
        if (d instanceof Application) {
            result = ((Application) d).getLibraryDirectory();
        }
        return result;
    }
    
    /**
     * Returns whether the entry name resides in a subdirectory of the library-directory
     * for the application.
     *@param libDir the library directory string including the trailing slash; null if this is not an Application descriptor
     *@param entryName the entry to check
     *@return true if the entry name resides in a subdirectory of the library directory; false otherwise
     */
    private boolean inLibDirSubdirectory(String libDir, String entryName) {
        boolean result = false;
        if ((libDir != null) && entryName.startsWith(libDir)) {
            /*
             * Skip past the trailing slash from the library directory path.
             */
            String restOfPath = entryName.substring(libDir.length() + 1);
            result = restOfPath.contains("/");
        }
        return result;
    }
    
    protected Properties 	props;
}
