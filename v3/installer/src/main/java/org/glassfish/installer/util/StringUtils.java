/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.installer.util;

import java.io.UnsupportedEncodingException;
import java.util.Vector;

/**
 * Utility class for String related operations.
 * @author sathyan
 */
public class StringUtils {

    /* Encoding scheme for String to be validated. */
    private static final String ASCII_CHARSET = "ISO-8859-1";

    /**	Checks to make sure that the input is only numbers.
     * @param inputStr String to be validated.
     * @return true/false.
     **/
    static public boolean isNumeric(String inputStr) {
        char str[] = inputStr.toCharArray();
        for (int i = 0; i < str.length; i++) {
            if (Character.isDigit(str[i]) == false) {
                return false;
            }
        }
        return true;
    }

    /**	Checks to make sure that the string has only numbers and alphabets.
     * @param inputStr String to be validated.
     * @return true/false.
     **/
    static public boolean isAlphaNumeric(String inputStr) {
        char str[] = inputStr.toCharArray();
        for (int i = 0; i < str.length; i++) {
            if (Character.isLetterOrDigit(str[i]) == false) {
                return false;
            }
        }
        return true;
    }

    /**	Checks to make sure that the input string only has ASCII characters.
     * @param inputStr String to be validated.
     * @return true/false.
     **/
    static public boolean isAscii(String str) {
        try {
            if (!str.equals(new String(str.getBytes(ASCII_CHARSET),
                    ASCII_CHARSET))) {
                return false;
            }
        } catch (UnsupportedEncodingException dummy) {
            return false;
        }
        return true;
    }

    /* scan the vector for given string.
     * @param str String to check.
     * @param v Vector to scan.
     * @return true/false depending on the presence of string in vector. 
     */
    static public boolean isStringInVector(String str, Vector v) {
        if (v == null) {
            return false;
        }

        for (int i = 0; i < v.size(); i++) {
            if (str.equals((String) v.elementAt(i))) {
                return true;
            }
        }
        return false;

    }

    /**
     * Substitute the token with the given value in a StringBuffer and
     * returns the StringBuffer.
     * @param str Input StringBuffer.
     * @param token Token to scan for in the buffer str.
     * @param newValue replacement string.
     * @return new String formed after token replacement.
     */
    static public String substString(StringBuffer str, String token, String newValue) {
        int offset = 0, tokenLen = token.length(), valLen = newValue.length();
        offset = str.toString().indexOf(token, offset);
        while (offset != -1) {
            str.replace(offset, offset + tokenLen, newValue);
            offset = offset + valLen;
            offset = str.toString().indexOf(token, offset);
        }
        return new String(str);

    }
}



