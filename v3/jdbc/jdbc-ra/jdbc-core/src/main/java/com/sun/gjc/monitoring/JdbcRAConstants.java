/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.gjc.monitoring;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Interface that contains all the constants used in the jdbc-ra module.
 *
 * @author Shalini M
 */
public interface JdbcRAConstants {

    /**
     * Represents the statement cache hit monitoring event.
     */
    public static final String STATEMENT_CACHE_HIT = "statementCacheHitEvent";

    /**
     * Represents the statement cache miss monitoring event.
     */
    public static final String STATEMENT_CACHE_MISS = "statementCacheMissEvent";

    /**
     * Represents caching of sql query event.
     */
    public static final String CACHE_SQL_QUERY = "cacheSqlQueryEvent";

    /**
     * Represents module provider name.
     */
    public static final String GLASSFISH = "glassfish";

    /**
     * Represents the module name
     */
    public static final String JDBCRA = "jdbcra";

    /**
     * Represents probe provider name
     */
    public static final String STATEMENT_CACHE_PROBE = "statementcache";

    /**
     * Represents probe provider name for sql tracing.
     */
    public static final String SQL_TRACING_PROBE = "sqltracing";

    /**
     * Dotted name used in monitoring for Statement caching.
     */
    public static final String STATEMENT_CACHE_DOTTED_NAME = GLASSFISH + ":" +
            JDBCRA + ":" + STATEMENT_CACHE_PROBE + ":";

    /**
     * Dotted name used in monitoring for Sql Tracing.
     */
    public static final String SQL_TRACING_DOTTED_NAME = GLASSFISH + ":" +
            JDBCRA + ":" + SQL_TRACING_PROBE + ":";

    /**
     * Represents top queries to report.
     */
    public static final String REPORT_QUERIES = "reportQueriesEvent";

    /**
     * List of valid method names that can be used for sql trace monitoring.
     */
    public static final List<String> validSqlTracingMethodNames =
            Collections.unmodifiableList(
            Arrays.asList(
                "nativeSQL",
                "prepareCall",
                "prepareStatement",
                "addBatch",
                "execute",
                "executeQuery",
                "executeUpdate"
            ));
}
