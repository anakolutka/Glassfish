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

package com.sun.enterprise.deployment.deploy.shared;

import com.sun.logging.LogDomains;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.jvnet.hk2.annotations.Service;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This implementation of the Archive deal with reading
 * jar files either from a JarFile or from a JarInputStream
 *
 * @author Jerome Dochez
 */
@Service(name="jar")
public class InputJarArchive extends JarArchive implements ReadableArchive {
    
    final static Logger logger = LogDomains.getLogger(LogDomains.DPL_LOGGER);

    // the file we are currently mapped to 
    protected JarFile jarFile=null;
    
    // in case this abstraction is dealing with a jar file
    // within a jar file, the jarFile will be null and this
    // JarInputStream will contain the 
    protected JarInputStream jarIS=null; 
    
    // the archive Uri
    private URI uri;

    // parent jar file for embedded jar
    private InputJarArchive parentArchive=null;

    private StringManager localStrings = StringManager.getManager(getClass());
    
    /**
     * Get the size of the archive
     * @return tje the size of this archive or -1 on error
     */
    public long getArchiveSize() throws NullPointerException, SecurityException {
        if(uri == null) {
            return -1;
        }
        File tmpFile = new File(uri);
        return(tmpFile.length());
    }
    
    /** @return an @see java.io.OutputStream for a new entry in this
     * current abstract archive.
     * @param name the entry name
     */
    public OutputStream addEntry(String name) throws IOException {
        throw new UnsupportedOperationException("Cannot write to an JAR archive open for reading");        
    }
    
    /** 
     * close the abstract archive
     */
    public void close() throws IOException {
        if (jarFile!=null) {
            jarFile.close();
            jarFile=null;
        }
        if (jarIS!=null) {
            jarIS.close();
            jarIS=null;
        }
    }
        
    /** 
     * creates a new abstract archive with the given path
     *
     * @param uri the path to create the archive
     */
    public void create(URI uri) throws IOException {
        throw new UnsupportedOperationException("Cannot write to an JAR archive open for reading");        
    }
        
    /** 
     * @return an @see java.util.Enumeration of entries in this abstract
     * archive
     */
    public Enumeration entries() {
        Vector entries = new Vector();
 
        if (parentArchive!=null) {
            try {
                // reopen the embedded archive and position the input stream
                // at the beginning of the desired element
                jarIS = new JarInputStream(parentArchive.jarFile.getInputStream(parentArchive.jarFile.getJarEntry(uri.getSchemeSpecificPart())));
                JarEntry ze;
                do {
                    ze = jarIS.getNextJarEntry();
                    if (ze!=null && !ze.isDirectory()) {
                        entries.add(ze.getName());
                    }                
                } while (ze!=null);
                jarIS.close();
                jarIS = null;
            } catch(IOException ioe) {
                return null;
            }
        } else {
            try {
                if (jarFile==null) {
                    getJarFile(uri);
                }
            } catch(IOException ioe) {
                return entries.elements();
            }
            if (jarFile==null) {
                return entries.elements();
            }
            for (Enumeration e = jarFile.entries();e.hasMoreElements();) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                if (!ze.isDirectory() && !ze.getName().equals(JarFile.MANIFEST_NAME)) {
                    entries.add(ze.getName());
                }
            }
        }
        return entries.elements();        
    }
    
    /**
     *  @return an @see java.util.Enumeration of entries in this abstract
     * archive, providing the list of embedded archive to not count their 
     * entries as part of this archive
     */
     public Enumeration entries(Enumeration embeddedArchives) {
	// jar file are not recursive    
  	return entries();
    }
    
    /**
     * @return a @see java.io.InputStream for an existing entry in
     * the current abstract archive
     * @param entryName entry name
     */
    public InputStream getEntry(String entryName) throws IOException {
        if (jarFile!=null) {
            ZipEntry ze = jarFile.getEntry(entryName);
            if (ze!=null) {
                return new BufferedInputStream(jarFile.getInputStream(ze));
            } else {
                return null;
            }            
        } else
	if ((parentArchive != null) && (parentArchive.jarFile != null)) {
            JarEntry je;
            // close the current input stream
            if (jarIS!=null) {
                jarIS.close();
            }
            
            // reopen the embedded archive and position the input stream
            // at the beginning of the desired element
	    JarEntry archiveJarEntry = (uri != null)? parentArchive.jarFile.getJarEntry(uri.getSchemeSpecificPart()) : null;
	    if (archiveJarEntry == null) {
		return null;
	    }
            jarIS = new JarInputStream(parentArchive.jarFile.getInputStream(archiveJarEntry));
            do {
                je = jarIS.getNextJarEntry();
            } while (je!=null && !je.getName().equals(entryName));
            if (je!=null) {
                return new BufferedInputStream(jarIS);
            } else {
                return null;
            }
        } else {
	    return null;
	}
    }

    /**
     * Returns the entry size for a given entry name or 0 if not known
     *
     * @param name the entry name
     * @return the entry size
     */
    public long getEntrySize(String name) {
        if (jarFile!=null) {
            ZipEntry ze = jarFile.getEntry(name);
            if (ze!=null) {
                return ze.getSize();
            }
        }
        return 0;
    }

    /** Open an abstract archive
     * @param uri the path to the archive
     */
    public void open(URI uri) throws IOException {
       this.uri = uri;
       jarFile = getJarFile(uri);
    }
    
    /**
     * @return a JarFile instance for a file path
     */
    protected JarFile getJarFile(URI uri) throws IOException {
        if (!uri.getScheme().equals("jar")) {
            throw new IOException("Wrong scheme for InputJarArchive : " + uri.getScheme());
        }
        jarFile = null;
        try {
            File file = new File(uri.getSchemeSpecificPart());
            if (file.exists()) {
                jarFile = new JarFile(file);
            }
        } catch(IOException e) {
            logger.log(Level.SEVERE, "enterprise.deployment.backend.fileOpenFailure", 
                    new Object[]{uri});
            // add the additional information about the path
            // since the IOException from jdk doesn't include that info
            String additionalInfo = localStrings.getString(
                "enterprise.deployment.invalid_zip_file", uri);
            IOException ioe = new IOException(e.getLocalizedMessage() + " --  " + additionalInfo);
            ioe.initCause(e);
            throw ioe;
        }
        return jarFile;
    }       
    
    
    /** 
     * @return the manifest information for this abstract archive
     */
    public Manifest getManifest() throws IOException {
        if (jarFile!=null) {
            return jarFile.getManifest();
        } 
        if (parentArchive!=null) {    
            // close the current input stream
            if (jarIS!=null) {
                jarIS.close();
            }
            // reopen the embedded archive and position the input stream
            // at the beginning of the desired element
            if (jarIS==null) {
                jarIS = new JarInputStream(parentArchive.jarFile.getInputStream(parentArchive.jarFile.getJarEntry(uri.getSchemeSpecificPart())));
            }
            Manifest m = jarIS.getManifest();
            if (m==null) {
               java.io.InputStream is = getEntry(java.util.jar.JarFile.MANIFEST_NAME);
               if (is!=null) {
                    m = new Manifest();
                    m.read(is);
                    is.close();
               }
            }
            return m;
        }                        
        return null;
    }

    /**
     * Returns the path used to create or open the underlying archive
     *
     * @return the path for this archive.
     */
    public URI getURI() {
        return uri;
    }

    /**
     * @return true if this abstract archive maps to an existing 
     * jar file
     */
    public boolean exists() {
        return jarFile!=null;
    }
    
    /**
     * deletes the underlying jar file
     */
    public boolean delete() {
        if (jarFile==null) {
            return false;
        }
        try {
            jarFile.close();
            jarFile = null;
        } catch (IOException ioe) {
            return false;
        }
        return FileUtils.deleteFile(new File(uri));
    }
    
    /**
     * rename the underlying jar file
     */
    public boolean renameTo(String name) {
        if (jarFile==null) {
            return false;
        }
        try {
            jarFile.close();
            jarFile = null;
        } catch (IOException ioe) {
            return false;
        }        
        return FileUtils.renameFile(new File(uri), new File(name));
    }
    
    /**
     * @return an Archive for an embedded archive indentified with
     * the name parameter
     */
    public ReadableArchive getSubArchive(String name) throws IOException {
        if (jarFile!=null) {
            // for now, I only support one level down embedded archives
            InputJarArchive ija = new InputJarArchive();
            JarEntry je = jarFile.getJarEntry(name);
            if (je!=null) {
                JarInputStream jis = new JarInputStream(new BufferedInputStream(jarFile.getInputStream(je)));
                try {
                    ija.uri = new URI("jar",name, null);
                } catch(URISyntaxException e) {
                    // do nothing
                }
                ija.jarIS = jis;
                ija.parentArchive = this;
                return ija;
            }
        }
        return null;
    }    
}
