/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the "License").  You may not use this file except 
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt or 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html. 
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * HEADER in each file and include the License file at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable, 
 * add the following below this CDDL HEADER, with the 
 * fields enclosed by brackets "[]" replaced with your 
 * own identifying information: Portions Copyright [yyyy] 
 * [name of copyright owner]
 */


package com.sun.persistence.utility.database;

import com.sun.persistence.utility.LogHelperUtility;
import com.sun.persistence.utility.PropertyHelper;
import com.sun.persistence.utility.logging.Logger;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Mitesh Meswani This class defines string constants representing
 *         database names. This class is responsible to translate given database
 *         name to internal name.
 */
public class DBVendorTypeHelper {
    //Enum that corresponds to unknown database.
    public final static int OTHER_ENUM = -1;

    //Known databases.
    public final static int DEFAULT_DB_ENUM = 0;
    public final static int ORACLE_ENUM = 1;
    public final static int POINTBASE_ENUM = 2;
    public final static int MSSQL_ENUM = 3;
    public final static int SYBASE_ENUM = 4;
    public final static int DB2_ENUM = 5;
    public final static int MYSQL_ENUM = 6;
    public final static int INFORMIX_ENUM = 7;
    public final static int INGRES_ENUM = 8;
    public final static int DERBY_ENUM = 9;

    //Please increment following when a new known database is added.
    public final static int MAX_KNOWN_DB = 10;

    /**
     * Array that defines mapping from given string name to enum. Please make
     * sure that array indexes and enum values are kept in sync.
     */
    private final static String enumToStringMapping[] = {"SQL92", "ORACLE", //NOI18N
                                                         "POINTBASE", "MSSQL", //NOI18N
                                                         "SYBASE", "DB2", //NOI18N
                                                         "MYSQL", "INFORMIX", //NOI18N
                                                         "INGRES", "DERBY"}; // NOI18N

    public final static String DEFAULT_DB = enumToStringMapping[DEFAULT_DB_ENUM];
    public final static String ORACLE = enumToStringMapping[ORACLE_ENUM];
    public final static String POINTBASE = enumToStringMapping[POINTBASE_ENUM];
    public final static String MSSQL = enumToStringMapping[MSSQL_ENUM];
    public final static String SYBASE = enumToStringMapping[SYBASE_ENUM];
    public final static String DB2 = enumToStringMapping[DB2_ENUM];
    public final static String MYSQL = enumToStringMapping[MYSQL_ENUM];
    public final static String INFORMIX = enumToStringMapping[INFORMIX_ENUM];
    public final static String INGRES = enumToStringMapping[INGRES_ENUM];
    public final static String DERBY = enumToStringMapping[DERBY_ENUM];

    private final static String PROPERTY_PATH = "com/sun/persistence/utility/database/"; // NOI18N

    private final static String VENDOR_NAME_TO_TYPE_PROPERTY = "com.sun.persistence.utility.database.VENDOR_NAME_TO_TYPE"; //NOI18N
    private final static String VENDOR_NAME_TO_TYPE_RESOURCE_PROPERTY = "com.sun.persistence.utility.database.VENDOR_NAME_TO_TYPE_RESOURCE";  //NOI18N
    private final static String VENDOR_NAME_TO_TYPE_RESOURCE_DEFAULT_NAME = PROPERTY_PATH
            + "VendorNameToTypeMapping.properties";  //NOI18N

    /**
     * The logger.
     */
    private static final Logger logger = LogHelperUtility.getLogger();

    /**
     * Holds mapping between possible vendor names to internal types defined
     * above. vendor names are treated as regular expressions.
     */
    private static Properties _nameToVendorType = initializeNameToVendorType();;

    /**
     * Get Database Vendor Type from vendor name.
     * @param vendorName Input vendor name. Typically this is obtained by
     * querying <code>DatabaseMetaData</code>.
     * @return Database type that corresponds to <code>vendorName</code>. If
     *         vendorName does not match any of predefined vendor names, the
     *         database type returned is generated using following logic. <PRE>
     *         detectedDbType = vendorName.toUpperCase(); int i =
     *         detectedDbType.indexOf('/'); if (i > -1) { detectedDbType =
     *         detectedDbType.substring(0, i); } </PRE> If vendorName is null,
     *         <code>DEFAULT_DB</code> is returned.
     */
    public static String getDBType(String vendorName) {
        boolean debug = logger.isLoggable();
        if (debug) {
            logger.fine(
                    "utility.database.DBVendorTypeHelper.inputVendorName", //NOI18N
                    vendorName);
        }
        String detectedDbType = DEFAULT_DB;
        if (vendorName != null) {
            detectedDbType =
                    matchVendorNameInProperties(vendorName, _nameToVendorType);
            //If not able to detect dbType from properties, invent one by
            //manipulating input vendorName.
            if (detectedDbType == null) {
                detectedDbType = vendorName.toUpperCase();
                int i = detectedDbType.indexOf('/');
                if (i > -1) {
                    detectedDbType = detectedDbType.substring(0, i);
                }
            }
        }
        if (debug) {
            logger.fine(
                    "utility.database.DBVendorTypeHelper.detectedVendorType", //NOI18N
                    detectedDbType);
        }
        return detectedDbType;
    }

    /**
     * Gets enumerated database type for given metaData
     * @param metaData Input DataBaseMetaData.
     * @return Enumerated database type as described by {@link
     *         #getEnumDBType(java.lang.String)}.
     */
    public static int getEnumDBType(DatabaseMetaData metaData)
            throws SQLException {
        String dbType = getDBType(metaData.getDatabaseProductName());
        return getEnumDBType(dbType);
    }

    /**
     * Gets enumerated database type for given dbType
     * @param dbType Input database Type. This should have been obtained from a
     * previous call to {@link #getDBType(java.lang.String)}
     * @return dbType translated to one of the enumerations above. If dbType
     *         does not correspond to one of the predefined types, OTHER is
     *         returned
     */
    public static int getEnumDBType(String dbType) {
        int enumDBType = OTHER_ENUM;
        //Search through the array for dbType
        for (int i = 0; i < MAX_KNOWN_DB - 1 && enumDBType == OTHER_ENUM; i++) {
            if (enumToStringMapping[i].equals(dbType)) {
                enumDBType = i;
            }
        }
        return enumDBType;
    }

    /**
     * Determines whether to use uppercase schema name for a give database.
     * @param dmd The DatabaseMetaData for the database
     * @return true if upper case schemaname is to be used. False otherwise.
     * @throws SQLException
     */
    public static boolean requireUpperCaseSchema(DatabaseMetaData dmd)
            throws SQLException {
        int vendorTypeEnum = getEnumDBType(dmd);
        return ORACLE_ENUM == vendorTypeEnum
                || POINTBASE_ENUM == vendorTypeEnum;
    }

    /**
     * Allocate and initialize nameToVendorType if not already done.
     */
    private static Properties initializeNameToVendorType() {
        synchronized (DBVendorTypeHelper.class) {
            if (_nameToVendorType == null) {
                _nameToVendorType = new Properties();
                String resourceName = System.getProperty(
                        VENDOR_NAME_TO_TYPE_RESOURCE_PROPERTY,
                        VENDOR_NAME_TO_TYPE_RESOURCE_DEFAULT_NAME);
                try {
                    PropertyHelper.loadFromResource(
                            _nameToVendorType, resourceName,
                            DBVendorTypeHelper.class.getClassLoader());
                } catch (IOException e) {
                    if (logger.isLoggable()) {
                        logger.fine(
                                "utility.database.DBVendorTypeHelper.couldNotLoadResource", // NOI18N
                                resourceName, e);
                    }
                }
                overrideWithSystemProperties(_nameToVendorType);
            }
        }

        return _nameToVendorType;
    }

    /**
     * Match vendorName in properties specifieid by nameToVendorType.
     */
    private static String matchVendorNameInProperties(String vendorName,
            Properties nameToVendorType) {
        String dbType = null;
        //Iterate over all properties till we find match.
        for (Iterator iterator = nameToVendorType.entrySet().iterator();
             dbType == null && iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String regExpr = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (logger.isLoggable(Logger.FINEST)) {
                logger.finest(
                        "utility.database.DBVendorTypeHelper.regExprDbType", //NOI18N
                        regExpr, value);
            }
            if (matchPattern(regExpr, vendorName)) {
                dbType = value;
            }
        }
        return dbType;
    }

    /**
     * Matches target to pattern specified regExp. Returns false if there is any
     * error compiling regExp.
     * @param regExp The regular expression.
     * @param target The target against which we are trying to match regExp.
     * @return false if there is error compiling regExp or target does not match
     *         regExp. true if regExp matches pattern.
     */
    private static boolean matchPattern(String regExp, String target) {
        boolean matches = false;
        try {
            matches = Pattern.matches(regExp, target);
        } catch (PatternSyntaxException e) {
            logger.fine(
                    "utility.database.DBVendorTypeHelper.patternSyntaxException", //NOI18N
                    e);
        }
        return matches;
    }

    /**
     * Overrides nameToVendorType with any system properties defined.
     */
    private static void overrideWithSystemProperties(
            Properties nameToVendorType) {
        String vendorNameToType = null;
        boolean debug = logger.isLoggable();

        int counter = 1;
        do {
            String vendorNameToTypeProperty = VENDOR_NAME_TO_TYPE_PROPERTY
                    + counter++;
            vendorNameToType = System.getProperty(vendorNameToTypeProperty);
            if (vendorNameToType != null) {
                //Split the vendorNameToType into two at char '='
                String[] parsedProperty = vendorNameToType.split("=", 2); //NOI18N
                if (parsedProperty.length >= 2) {
                    String suggestedDbType = parsedProperty[0];
                    String regExp = parsedProperty[1];
                    if (debug) {
                        logger.fine(
                                "utility.database.DBVendorTypeHelper.traceVendorNameToTypeProperty", //NOI18N
                                vendorNameToTypeProperty, regExp,
                                suggestedDbType);
                    }
                    nameToVendorType.put(regExp, suggestedDbType);
                } else {
                    if (debug) {
                        logger.fine(
                                "utility.database.DBVendorTypeHelper.errorParsingVendorNameToTypeProperty", //NOI18N
                                vendorNameToTypeProperty, vendorNameToType);
                    }
                }
            }
        } while (vendorNameToType != null);
    }

}
