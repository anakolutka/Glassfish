/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package admin;

import java.io.*;

/**
 *
 * @author Byron Nevins
 */
final class TestUtils {
    private TestUtils() {
        // all-static class!
    }
    
    static File createPasswordFile() throws IOException {
        File f = File.createTempFile("password_junk", ".txt");
        //f.deleteOnExit();   // just in case
        PrintStream pwfile = new PrintStream(f);
        pwfile.println("AS_ADMIN_PASSWORD=admin123");
        pwfile.println("AS_ADMIN_MASTERPASSWORD=admin123");
        pwfile.close();
        return f;
    }
    public static String unecho(String s) {
        // remove the huge echo'd command from some output
        // it will be [enormous ugly command]EOL[output from command]

        // note that this will work for "\r\n" as well
        int index = s.indexOf('\n');

        if(index > 0)
            return s.substring(index);

        return s;
    }
    /**
     * If a system property has a value of the form "${propname}", then expand
     * it. If "propname" is not an existing Java system property then return null.
     *
     * We use this mainly because in ant the test may be invoked with something
     * like this:
     *             <jvmarg value="-Dssh.installdir=${ssh.installdir}"/>
     * If if ssh.installdir is not a defined property then ant will just pass
     * "${ssh.installdir}" as the value for ssh.installdir. In this case we
     * rather have the value be null to know the property was not set.
     *
     * @param propName
     * @return
     */
    static String getExpandedSystemProperty(String propName) {
        String value = System.getProperty(propName);
        if (value == null) {
            return null;
        }
        if (value.startsWith("${")) {
            int index1 = value.indexOf("{");
            int index2 = value.indexOf("}");
            String substring = value.substring(index1+1, index2);
            if (propName.equals(substring)) {
                // Have something like foo=${foo}. Can't expand, return null;
                return null;
            }
            return getExpandedSystemProperty(substring);
        } else {
            return value;
        }
    }
}
