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
package org.glassfish.api.embedded;

import org.glassfish.api.deployment.archive.ReadableArchiveAdapter;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URI;
import java.util.*;
import java.util.jar.Manifest;
import java.util.jar.JarFile;

/**
 * Abstraction for a scattered archive (parts disseminated in various directories)
 *
 * @author Jerome Dochez
 */
public class ScatteredArchive extends ReadableArchiveAdapter {


    public static class Builder {

        final String name;
        File topDir = null;
        File resources = null;
        final List<URL> urls = new ArrayList<URL>();
        final Map<String, File> metadata = new HashMap<String, File>();


        /**
         * Construct a new scattered archive builder with the minimum information
         * By default, a scattered archive is not different from any other
         * archive where all the files are located under a top level
         * directory (topDir).
         * Some files can then be scattered in different locations and be specified
         * through the appropriate setters.
         * Alternatively, topDir can be null to specify a truely scattered archive
         * and all the locations must be specified.
         *
         * @param name   archive name
         * @param topDir top level directory
         */
        public Builder(String name, File topDir) {
            this.name = name;
            this.topDir = topDir;
        }

        public Builder(String name, Collection<URL> urls) {
            this.name = name;
            for (URL u : urls) {
                this.urls.add(u);
            }
        }

        public Builder setResources(File resources) {
            this.resources = resources;
            return this;
        }

        public Builder addMetadata(String name, File metadata) {
            this.metadata.put(name, metadata);
            return this;
        }

        public Builder addMetadata(File metadata) {
            return addMetadata(metadata.getName(), metadata);
        }

        public Builder addClassPath(URL classpath) {
            this.urls.add(classpath);
            return this;
        }

        public ScatteredArchive buildJar() {
            return new ScatteredArchive(this, Builder.type.jar);
        }

        public ScatteredArchive buildWar() {
            return new ScatteredArchive(this, Builder.type.war);
        }

        enum type {
            jar, war
        }
    }

    final String name;
    final File topDir;
    final File resources;
    final List<URL> urls = new ArrayList<URL>();
    final Map<String, File> metadata = new HashMap<String, File>();
    final Builder.type type;

    private ScatteredArchive(Builder builder, Builder.type type) {
        name = builder.name;
        topDir = builder.topDir;
        resources = builder.resources;
        urls.addAll(builder.urls);
        metadata.putAll(builder.metadata);
        this.type = type;
    }

    /**
     * Get the classpath URLs
     *
     * @return A read-only copy of the classpath URL Collection
     */
    public Iterable<URL> getClassPath() {
        return Collections.unmodifiableCollection(urls);
    }

    /**
     * @return The resources directory
     */
    public File getResourcesDir() {
        return resources;
    }

    /**
     * Returns the InputStream for the given entry name
     * The file name must be relative to the root of the module.
     *
     * @param name the file name relative to the root of the module.
     * @return the InputStream for the given entry name or null if not found.
     */

    public InputStream getEntry(String name) throws IOException {
        File f = getFile(name);
        if (f.exists()) return new FileInputStream(f);
        return null;
    }

    /**
     * Returns whether or not a file by that name exists
     * The file name must be relative to the root of the module.
     *
     * @param name the file name relative to the root of the module.
     * @return does the file exist?
     */

    public boolean exists(String name) throws IOException {
        if ("WEB-INF".equals(name) && type == Builder.type.war) {
            return true;    
        }
        return getFile(name).exists();
    }

    /**
     * Returns an enumeration of the module file entries.  All elements
     * in the enumeration are of type String.  Each String represents a
     * file name relative to the root of the module.
     * <p><strong>Currently under construction</strong>
     *
     * @return an enumeration of the archive file entries.
     */
    public Enumeration<String> entries() {
        // TODO: abstraction breakage. We need file-level abstraction for archive
        // and then more structured abstraction.

        return (new Vector<String>()).elements();
    }

    /**
     * Returns the manifest information for this archive
     *
     * @return the manifest info
     */
    public Manifest getManifest() throws IOException {
        InputStream is = getEntry(JarFile.MANIFEST_NAME);
        if (is != null) {
            try {
                return new Manifest(is);
            } finally {
                is.close();
            }
        }
        return null;
    }

    /**
     * Returns the path used to create or open the underlying archive
     * <p/>
     * <p/>
     * TODO: abstraction breakage:
     * Several callers, most notably {@link org.glassfish.api.deployment.DeploymentContext#getSourceDir()}
     * implementation, assumes that this URI is an URL, and in fact file URL.
     * <p/>
     * <p/>
     * If this needs to be URL, use of {@link URI} is misleading. And furthermore,
     * if its needs to be a file URL, this should be {@link File}.
     *
     * @return the path for this archive.
     */
    public URI getURI() {
        if (topDir != null) {
            return topDir.toURI();
        }
        if (resources != null) {
            return resources.toURI();
        }
        try {
            return urls.get(0).toURI();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the name of the archive.
     * <p/>
     * Implementations should not return null.
     *
     * @return the name of the archive
     */
    public String getName() {
        return name;
    }

    /**
     * Returns an enumeration of the module file entries with the
     * specified prefix.  All elements in the enumeration are of
     * type String.  Each String represents a file name relative
     * to the root of the module.
     * <p><strong>Currently Not Supported</strong>
     *
     * @param s the prefix of entries to be included
     * @return an enumeration of the archive file entries.
     * @throws UnsupportedOperationException always
     */

    public Enumeration<String> entries(String s) {
        throw new UnsupportedOperationException("entries(String)");
    }

    @Override
    public Collection<String> getDirectories() throws IOException {
        return new ArrayList<String>();
    }

    public String toString() {
        return super.toString() + " located at " + (topDir == null ? resources : topDir);
    }


    public File getFile(String name) {
        if (metadata.containsKey(name)) {
            return metadata.get(name);
        }
        String shortName = (name.indexOf("/") != -1 ? name.substring(name.indexOf("/") + 1) : name);
        if (metadata.containsKey(shortName)) {
            return metadata.get(name);
        }
        if (resources != null) {
            return new File(resources, name);
        }
        return null;
    }
}
