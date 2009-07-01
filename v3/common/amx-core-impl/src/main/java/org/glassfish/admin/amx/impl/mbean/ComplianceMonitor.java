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
package org.glassfish.admin.amx.impl.mbean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Set;


import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.glassfish.admin.amx.base.DomainRoot;
import org.glassfish.admin.amx.core.AMXValidator;
import org.glassfish.admin.amx.impl.util.ImplUtil;
import org.glassfish.admin.amx.util.jmx.JMXUtil;

/**
Validates AMX MBeans as they are registered.  Problems are emitted as WARNING to the server log.
 */
public final class ComplianceMonitor implements NotificationListener
{
    private static ComplianceMonitor INSTANCE = null;

    private final DomainRoot mDomainRoot;

    private final MBeanServer mServer;

    private volatile boolean mStarted = false;

    /** offloads the validation so as not to block during Notifications */
    private final ValidatorThread mValidatorThread;

    private ComplianceMonitor(final DomainRoot domainRoot)
    {
        mDomainRoot = domainRoot;

        mServer = (MBeanServer) domainRoot.extra().mbeanServerConnection();

        mValidatorThread = new ValidatorThread(mServer);
    }

    private void listen()
    {
        try
        {
            JMXUtil.listenToMBeanServerDelegate(mServer, this, null, null);

        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }

        // queue all existing MBeans
        final Set<ObjectName> existing = JMXUtil.queryAllInDomain(mServer, mDomainRoot.objectName().getDomain());
        for (final ObjectName objectName : existing)
        {
            //debug( "Queueing for validation: " + objectName );
            validate(objectName);
        }
    }

    public void validate(final ObjectName objectName)
    {
        mValidatorThread.add(objectName);
    }

    public static synchronized ComplianceMonitor getInstance(final DomainRoot domainRoot)
    {
        if (INSTANCE == null)
        {
            INSTANCE = new ComplianceMonitor(domainRoot);
            INSTANCE.listen(); // to start queuing immediately
        }
        return INSTANCE;
    }

    public void start()
    {
        if (!mStarted)
        {
            mValidatorThread.start();
        }
    }

    public void handleNotification(final Notification notifIn, final Object handback)
    {
        if ((notifIn instanceof MBeanServerNotification) &&
            notifIn.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION))
        {
            final MBeanServerNotification notif = (MBeanServerNotification) notifIn;
            final ObjectName objectName = notif.getMBeanName();
            if (objectName.getDomain().equals(mDomainRoot.objectName().getDomain()))
            {
                mValidatorThread.add(objectName);
            }
        }
    }

    private static final class ValidatorThread extends Thread
    {
        private final MBeanServer mServer;

        private final LinkedBlockingQueue<ObjectName> mMBeans = new LinkedBlockingQueue<ObjectName>();

        ValidatorThread(final MBeanServer server)
        {
            super("ComplianceMonitor.ValidatorThread");
            mServer = server;
        }
        
        private static final ObjectName QUIT = JMXUtil.newObjectName( "quit:type=quit" );

        void quit()
        {
            add(QUIT);
        }

        public void add(final ObjectName objectName)
        {
            mMBeans.add(objectName);
        }

        public void run()
        {
            try
            {
                doRun();
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }

        protected void doRun() throws Exception
        {
            //debug( "ValidatorThread.doRun(): started" );                
            while (true)
            {
                final ObjectName next = mMBeans.take(); // BLOCK until ready
                final List<ObjectName> toValidate = new ArrayList<ObjectName>();
                toValidate.add(next);
                mMBeans.drainTo(toValidate);    // efficiently get any additional ones
                if (mMBeans.contains(QUIT))
                {
                    break;  // poison, quit;
                }

                // process available MBeans as a group so we can emit summary information
                // as a group.
                final AMXValidator validator = new AMXValidator(mServer);
                try
                {
                    //debug( "VALIDATING MBeans: " + toValidate.size() );
                    final ObjectName[] objectNames = new ObjectName[ toValidate.size() ];
                    toValidate.toArray( objectNames );
                    final AMXValidator.ValidationResult result = validator.validate(objectNames);
                    if (result.numFailures() != 0)
                    {
                        ImplUtil.getLogger().info(result.toString());
                    }
                }
                catch (Throwable t)
                {
                    t.printStackTrace();
                }
            }
        }

    }

    private static void debug(final Object o)
    {
        System.out.println(o.toString());
    }

}






















