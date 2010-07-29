/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2005-2010 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.appserv.test.util.results;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @Class: TestSuite
 * @Description: Class holding One TestSuite info.
 * @Author : Ramesh Mandava
 * @Last Modified : By Ramesh on 10/24/2001
 * @Last Modified : By Ramesh on 1/20/2002 , For preserving order of entry of tests 		used a separate testIdVector
 * @Last Modified : By Justin Lee on 10/05/2009
 */
public class TestSuite {
    private String id;
    private String name = ReporterConstants.NA;
    private String description = ReporterConstants.NA;
    private Map<String, Test> tests = new TreeMap<String, Test>();
    int pass;
    int fail;
    int didNotRun;
    int total;
    public int number;
    private boolean written;

    public TestSuite() {
    }

    public TestSuite(String id) {
        this.id = SimpleReporterAdapter.checkNA(id);
    }

    public TestSuite(String id, String name) {
        this(id);
        this.name = SimpleReporterAdapter.checkNA(name);
    }

    public TestSuite(String id, String name, String description) {
        this(id, name);
        this.description = SimpleReporterAdapter.checkNA(description);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = SimpleReporterAdapter.checkNA(description);
    }

    public Map<String, Test> getTests() {
        return tests;
    }

    public void addTest(Test test) {
        if (tests.get(test.getName()) == null) {
            tests.put(test.getName(), test);
        } else {
            tests.get(test.getName()).merge(test);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TestSuite");
        sb.append("{id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", tests=").append(tests);
        sb.append('}');
        return sb.toString();
    }

    public String toXml() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<testsuite>\n");
        buffer.append("<id>" + id.trim() + "</id>\n");
        if (!name.equals(ReporterConstants.NA)) {
            buffer.append("<name>" + name.trim() + "</name>\n");
        }
        if (!description.equals(ReporterConstants.NA)) {
            buffer.append("<description><![CDATA[" + description.trim() + "]]></description>\n");
        }
        buffer.append("<tests>\n");
        for (Test myTest : tests.values()) {
            buffer.append(myTest.toXml());
        }
        buffer.append("</tests>\n");
        buffer.append("</testsuite>\n");
        return buffer.toString();
    }

    public String toHtml() {
        StringBuilder table = new StringBuilder(
            "<div id=\"table" + number + "\" class=\"suiteDetail\"><table width=\"40%\">"
                + ReportHandler.row(null, "td", "Testsuite Name", getName())
                + ReportHandler.row(null, "td", "Testsuite Description", getDescription())
                + ReportHandler.row(null, "th", "Name", "Status"));
        for (Test test : getTests().values()) {
            for (List<TestCase> list : test.getTestCases().values()) {
                for (TestCase testCase : list) {
                    final String status = testCase.getStatus();
                    table.append(String.format("<tr><td>%s</td>%s", testCase.getName(),
                        ReportHandler.cell(status.replaceAll("_", ""), 1, status)));
                }
            }
        }
        return table
            + "<tr class=\"nav\"><td colspan=\"2\">"
            + "[<a href=#DetailedResults>Detailed Results</a>"
            + "|<a href=#Summary>Summary</a>"
            + "|<a href=#TOP>Top</a>]"
            + "</td></tr>"
            + "</table></div><p>";

    }

    public boolean getWritten() {
        return written;
    }

    public void setWritten(final boolean written) {
        this.written = written;
    }

    public void merge(final TestSuite suite) {
        for (Test test : suite.getTests().values()) {
            addTest(test);
        }
    }
}
