/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
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

package com.sun.enterprise.tools.verifier;

import com.sun.enterprise.glassfish.bootstrap.ASMainFelix;
import com.sun.enterprise.glassfish.bootstrap.Constants;
import com.sun.enterprise.module.bootstrap.ArgumentManager;
import com.sun.enterprise.module.bootstrap.StartupContext;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Responsible for starting Felix and then instructing it to start
 * verifier module.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class VerifierOSGiMain
{
    private static final String VERIFIER_MODULE = "org.glassfish.verifier";

    public static void main(String[] args) throws Exception
    {
        Properties ctx = buildStartupContextProperties(args);
        ASMainFelix main = new ASMainFelix() {

            @Override
            protected void configureEnvironment() throws IOException
            {
                // We can't share the same cache as glassfish, so we use our own.
                // More over, since we allow multiple verifier to be active,
                // we can't use a fixed cache dir.

                File cacheDir = File.createTempFile("verifier-felix-cache", "tmp");

                // delete the file and create a directory in its place.
                if (cacheDir.delete() && cacheDir.mkdirs()) {
                        System.out.println("Felix cache dir created at " + cacheDir.getAbsolutePath());
                } else {
                    throw new IOException("Not able to create felix cache dir " + cacheDir.getAbsolutePath());
                }
                cacheDir.deleteOnExit();
                System.setProperty("org.osgi.framework.storage", cacheDir.getAbsolutePath());
                System.setProperty(Constants.HK2_CACHE_DIR, cacheDir.getAbsolutePath()); // hk2 inhabitants cache
            }
        };
        main.start(ctx);
    }

    private static Properties buildStartupContextProperties(String... args) throws IOException
    {
        Properties p = ArgumentManager.argsToMap(args);
        p.put(StartupContext.TIME_ZERO_NAME, (new Long(System.currentTimeMillis())).toString());
        p.put(StartupContext.STARTUP_MODULE_NAME, VERIFIER_MODULE);
        addRawStartupInfo(args, p);
        return p;
    }

    /**
     * Need the raw unprocessed args for verifier to parse them
     *
     * @param args raw args to this main()
     * @param p the properties to save as a system property
     */
    private static void addRawStartupInfo(final String[] args, final Properties p) {
        //package the args...
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < args.length; i++) {
            if(i > 0)
                sb.append(Constants.ARG_SEP);

            sb.append(args[i]);
        }

        p.put(Constants.ORIGINAL_CP, System.getProperty("java.class.path"));
        p.put(Constants.ORIGINAL_CN, VerifierOSGiMain.class.getName());
        p.put(Constants.ORIGINAL_ARGS, sb.toString());
    }

}
