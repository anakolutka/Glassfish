/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import com.sun.ejte.ccl.reporter.*;

/*
 * Unit test for Non blocking Write
 */
public class WebTest {

    private static String TEST_NAME = "non-blocking-output";
    private static String EXPECTED_RESPONSE = "onWritePossible";

    private static SimpleReporterAdapter stat
        = new SimpleReporterAdapter("appserv-tests");

    public static void main(String[] args) {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String contextRoot = args[2];
        stat.addDescription("Unit test for non blocking output");

        try {
            URL url = new URL("http://" + host + ":" + port + "/" + contextRoot + "/test");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.connect();
            BufferedReader input = null;
            String line = null;
            boolean expected = false;
            try {
                input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                int count = 0;
                boolean first = true;
                while ((line = input.readLine()) != null) {
                    expected = expected || line.endsWith(EXPECTED_RESPONSE);
                    System.out.println("\n " + (count++) + ": " + line.length());
                    int length = line.length();
                    int lengthToPrint = 20;
                    int end = ((length > lengthToPrint) ? lengthToPrint : length);
                    System.out.print(line.substring(0, end) + "...");
                    if (length > 20) {
                        System.out.println(line.substring(length - 20));
                    }
                    System.out.println();
                    if (first) {
                        int sleepInSec = 5;
                        System.out.println("Sleeping " + sleepInSec + " sec");
                        Thread.sleep(sleepInSec * 1000);
                        first = false;
                    }
                }
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch(Exception ex) {
                }
            }
            
            stat.addStatus(TEST_NAME, ((expected) ? stat.PASS : stat.FAIL));
        } catch(Exception ex) {
            ex.printStackTrace();
            stat.addStatus(TEST_NAME, stat.FAIL);
        }

        stat.printSummary();
    }
}
