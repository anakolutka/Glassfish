/*
 * Copyright  2000,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.util;

/**
 * Implementation of FileNameMapper that always returns the same
 * target file name.
 *
 * <p>This is the default FileNameMapper for the archiving tasks and
 * uptodate.</p>
 *
 */
public class MergingMapper implements FileNameMapper {
    protected String[] mergedFile = null;

    /**
     * Ignored.
     */
    public void setFrom(String from) {
    }

    /**
     * Sets the name of the merged file.
     */
    public void setTo(String to) {
        mergedFile = new String[] {to};
    }

    /**
     * Returns an one-element array containing the file name set via setTo.
     */
    public String[] mapFileName(String sourceFileName) {
        return mergedFile;
    }

}
