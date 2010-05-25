/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
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
 *
 */
package com.sun.appserv.test.util.results;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SimpleReporterAdapter implements Serializable {
    public static final String PASS = "pass";
    public static final String DID_NOT_RUN = "did_not_run";
    public static final String FAIL = "fail";
    private static final Pattern TOKENIZER;
    private final boolean debug = false;
    private final Map<String, String> testCaseStatus = new LinkedHashMap<String, String>();
    private String testSuiteName = getTestSuiteName();
    private String testSuiteID = testSuiteName + "ID";
    private String testSuiteDescription;
    private String ws_home = "appserv-tests";

    static {
        String pattern = or(
            split("x", "X"),     // AbcDef -> Abc|Def
            split("X", "Xx"),    // USArmy -> US|Army
            //split("\\D","\\d"), // SSL2 -> SSL|2
            split("\\d", "\\D")  // SSL2Connector -> SSL|2|Connector
        );
        pattern = pattern.replace("x", "\\p{Lower}").replace("X", "\\p{Upper}");
        TOKENIZER = Pattern.compile(pattern);
    }

    @Deprecated
    public SimpleReporterAdapter() {
    }

    @Deprecated
    public SimpleReporterAdapter(String ws_root) {
        ws_home = ws_root;
    }

    public SimpleReporterAdapter(String ws_root, String suiteName) {
        ws_home = ws_root;
        testSuiteName = suiteName;
        testSuiteID = testSuiteName + "ID";
    }

    public void addStatus(String test, String status) {
        int blankIndex = test.indexOf(" ");
        String key = test;
        if (blankIndex != -1) {
            key = test.substring(test.indexOf(" "));
        }
        key = key.trim();
        if (debug) {
            System.out.println("Value of key is:" + key);
        }
        if (!testCaseStatus.containsKey(key)) {
            testCaseStatus.put(key, status.toLowerCase());
        }
    }

    public void addDescription(String s) {
        testSuiteDescription = s;
    }

    public void printStatus() {
        try {
            final Reporter reporter = Reporter.getInstance(ws_home);
            if (debug) {
                System.out.println("Generating report at " + reporter.getResultFile());
            }
            reporter.setTestSuite(testSuiteID, testSuiteName, testSuiteDescription);
            reporter.addTest(testSuiteID, testSuiteID, testSuiteName);
            int pass = 0;
            int fail = 0;
            int d_n_r = 0;
            System.out.println("\n\n-----------------------------------------");
            for (String testCaseName : testCaseStatus.keySet()) {
                String status = testCaseStatus.get(testCaseName);
                if (status.equalsIgnoreCase(PASS)) {
                    pass++;
                } else if (status.equalsIgnoreCase(DID_NOT_RUN)) {
                    d_n_r++;
                } else {
                    fail++;
                }
                System.out.println(String.format("- %-37s -", testCaseName + ": " + status.toUpperCase()));
                reporter.addTestCase(testSuiteID, testSuiteID, testCaseName + "ID", testCaseName);
                reporter.setTestCaseStatus(testSuiteID, testSuiteID, testCaseName + "ID", status);
            }
            if (pass == 0 && fail == 0 && d_n_r == 0) {
                d_n_r++;
                System.out.println(String.format("- %-37s -", testSuiteName + ": " + DID_NOT_RUN));
                reporter.addTestCase(testSuiteID, testSuiteID, testSuiteID, testSuiteName);
                reporter.setTestCaseStatus(testSuiteID, testSuiteID, testSuiteID, DID_NOT_RUN);
            }
            System.out.println("-----------------------------------------");
            result("PASS", pass);
            result("FAIL", fail);
            result("DID NOT RUN", d_n_r);
            System.out.println("-----------------------------------------");
            reporter.flushAll();
            createConfirmationFile();
        }
        catch (Throwable ex) {
            System.out.println("Reporter exception occurred!");
            if (debug) {
                ex.printStackTrace();
            }
        }
    }

    private void result(final String label, final int count) {
        System.out.println(String.format("- Total %-12s: %-17d -", label, count));
    }

    public void createConfirmationFile() {
        try {
            FileOutputStream fout = new FileOutputStream("RepRunConf.txt");
            try {
                fout.write("Test has been reported".getBytes());
            } finally {
                fout.close();
            }
        } catch (Exception e) {
            System.out.println("Exception while creating confirmation file!");
            if (debug) {
                e.printStackTrace();
            }
        }
    }

    @Deprecated
    public void printSummary(String s) {
        printStatus();
    }

    public void printSummary() {
        printStatus();
    }

    public void run() {
        printSummary();
    }

    private String getTestSuiteName() {
        List<StackTraceElement> list = new ArrayList<StackTraceElement>(
            Arrays.asList(Thread.currentThread().getStackTrace()));
        list.remove(0);
        File jar = locate(getClass().getName().replace('.', '/') + ".class");
        while (jar.equals(locate(list.get(0).getClassName().replace('.', '/') + ".class"))) {
            list.remove(0);
        }
        StackTraceElement element = list.get(0);
        File file = locate(element.getClassName().replace('.', '/') + ".class");
        StringBuilder buf = new StringBuilder(file.getName().length());
        for (String t : TOKENIZER.split(file.getName())) {
            if (buf.length() > 0) {
                buf.append('-');
            }
            buf.append(t.toLowerCase());
        }
        return buf.toString().trim();
    }

    public File locate(String resource) {
        String u = getClass().getClassLoader().getResource(resource).toString();
        File file = null;
        try {
            if (u.startsWith("jar:file:")) {
                file = new File(new URI(u.substring(4, u.indexOf("!"))));
            } else if (u.startsWith("file:")) {
                file = new File(new URI(u.substring(0, u.indexOf(resource))));
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return file;
    }

    public void clearStatus() {
        testCaseStatus.clear();
    }

    private static String or(String... tokens) {
        StringBuilder buf = new StringBuilder();
        for (String t : tokens) {
            if (buf.length() > 0) {
                buf.append('|');
            }
            buf.append(t);
        }
        return buf.toString();
    }

    private static String split(String lookback, String lookahead) {
        return "((?<=" + lookback + ")(?=" + lookahead + "))";
    }
}