/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.fileinstall.internal;

import java.util.*;

import org.apache.felix.fileinstall.*;
import org.apache.felix.fileinstall.internal.Util.Logger;
import org.apache.felix.utils.collections.DictionaryAsMap;
import org.apache.felix.utils.properties.InterpolationHelper;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This clever little bundle watches a directory and will install any jar file
 * if finds in that directory (as long as it is a valid bundle and not a
 * fragment).
 *
 */
public class FileInstall implements BundleActivator
{
    static ServiceTracker padmin;
    static ServiceTracker startLevel;
    static Runnable cmSupport;
    static final Map /* <ServiceReference, ArtifactListener> */ listeners = new TreeMap /* <ServiceReference, ArtifactListener> */();
    static final BundleTransformer bundleTransformer = new BundleTransformer();
    BundleContext context;
    Map watchers = new HashMap();
    ServiceTracker listenersTracker;
    static boolean initialized;
    static final Object barrier = new Object();

    public void start(BundleContext context) throws Exception
    {
        this.context = context;

        Hashtable props = new Hashtable();
        props.put("url.handler.protocol", JarDirUrlHandler.PROTOCOL);
        context.registerService(org.osgi.service.url.URLStreamHandlerService.class.getName(), new JarDirUrlHandler(), props);

        padmin = new ServiceTracker(context, PackageAdmin.class.getName(), null);
        padmin.open();
        startLevel = new ServiceTracker(context, StartLevel.class.getName(), null);
        startLevel.open();
        String flt = "(|(" + Constants.OBJECTCLASS + "=" + ArtifactInstaller.class.getName() + ")"
                     + "(" + Constants.OBJECTCLASS + "=" + ArtifactTransformer.class.getName() + ")"
                     + "(" + Constants.OBJECTCLASS + "=" + ArtifactUrlTransformer.class.getName() + "))";
        listenersTracker = new ServiceTracker(context, FrameworkUtil.createFilter(flt), null)
        {
            public Object addingService(ServiceReference serviceReference)
            {
                ArtifactListener listener = (ArtifactListener) super.addingService(serviceReference);
                addListener(serviceReference, listener);
                return listener;
            }
            public void modifiedService(ServiceReference reference, Object service)
            {
                super.modifiedService(reference, service);
                removeListener(reference);
                addListener(reference, (ArtifactListener) service);
            }
            public void removedService(ServiceReference serviceReference, Object o)
            {
                removeListener(serviceReference);
            }
        };
        listenersTracker.open();

        try
        {
            cmSupport = new ConfigAdminSupport(context, this);
        }
        catch (NoClassDefFoundError e)
        {
            Util.log(context, Util.getGlobalLogLevel(context), Logger.LOG_DEBUG,
                "ConfigAdmin is not available, some features will be disabled", e);
        }

        // Created the initial configuration
        Hashtable ht = new Hashtable();

        set(ht, DirectoryWatcher.POLL);
        set(ht, DirectoryWatcher.DIR);
        set(ht, DirectoryWatcher.LOG_LEVEL);
        set(ht, DirectoryWatcher.FILTER);
        set(ht, DirectoryWatcher.TMPDIR);
        set(ht, DirectoryWatcher.START_NEW_BUNDLES);
        set(ht, DirectoryWatcher.USE_START_TRANSIENT);
        set(ht, DirectoryWatcher.NO_INITIAL_DELAY);
        set(ht, DirectoryWatcher.START_LEVEL);

        // check if dir is an array of dirs
        String dirs = (String)ht.get(DirectoryWatcher.DIR);
        if ( dirs != null && dirs.indexOf(',') != -1 )
        {
            StringTokenizer st = new StringTokenizer(dirs, ",");
            int index = 0;
            while ( st.hasMoreTokens() )
            {
                final String dir = st.nextToken().trim();
                ht.put(DirectoryWatcher.DIR, dir);

                String name = "initial";
                if ( index > 0 ) name = name + index;
                updated(name, new Hashtable(ht));

                index++;
            }
        }
        else
        {
            updated("initial", ht);
        }
        // now notify all the directory watchers to proceed
        // We need this to avoid race conditions observed in FELIX-2791
        synchronized (barrier) {
            initialized = true;
            barrier.notifyAll();
        }
    }

    // Adapted for FELIX-524
    private void set(Hashtable ht, String key)
    {
        Object o = context.getProperty(key);
        if (o == null)
        {
           o = System.getProperty(key.toUpperCase().replace('.', '_'));
            if (o == null)
            {
                return;
            }
        }
        ht.put(key, o);
    }

    public void stop(BundleContext context) throws Exception
    {
        synchronized (barrier) {
            initialized = false;
        }
        List /*<DirectoryWatcher>*/ toClose = new ArrayList /*<DirectoryWatcher>*/();
        synchronized (watchers)
        {
            toClose.addAll(watchers.values());
            watchers.clear();
        }
        for (Iterator w = toClose.iterator(); w.hasNext();)
        {
            try
            {
                DirectoryWatcher dir = (DirectoryWatcher) w.next();
                dir.close();
            }
            catch (Exception e)
            {
                // Ignore
            }
        }
        if (listenersTracker != null)
        {
            listenersTracker.close();
        }
        if (cmSupport != null)
        {
            cmSupport.run();
        }
        if (padmin != null)
        {
            padmin.close();
        }
    }

    public void deleted(String pid)
    {
        DirectoryWatcher watcher;
        synchronized (watchers)
        {
            watcher = (DirectoryWatcher) watchers.remove(pid);
        }
        if (watcher != null)
        {
            watcher.close();
        }
    }

    public void updated(String pid, Dictionary properties)
    {
        InterpolationHelper.performSubstitution(new DictionaryAsMap(properties), context);
        DirectoryWatcher watcher = null;
        synchronized (watchers)
        {
            watcher = (DirectoryWatcher) watchers.get(pid);
            if (watcher != null && watcher.getProperties().equals(properties))
            {
                return;
            }
        }
        if (watcher != null)
        {
            watcher.close();
        }
        watcher = new DirectoryWatcher(properties, context);
        synchronized (watchers)
        {
            watchers.put(pid, watcher);
        }
        watcher.start();
    }

    private void addListener(ServiceReference reference, ArtifactListener listener)
    {
        synchronized (listeners)
        {
            listeners.put(reference, listener);
        }
        notifyWatchers();
    }

    private void removeListener(ServiceReference reference)
    {
        synchronized (listeners)
        {
            listeners.remove(reference);
        }
        notifyWatchers();
    }

    private void notifyWatchers()
    {
        List /*<DirectoryWatcher>*/ toNotify = new ArrayList /*<DirectoryWatcher>*/();
        synchronized (watchers)
        {
            toNotify.addAll(watchers.values());
        }
        for (Iterator w = toNotify.iterator(); w.hasNext();)
        {
            DirectoryWatcher dir = (DirectoryWatcher) w.next();
            synchronized (dir)
            {
                dir.notifyAll();
            }
        }
    }

    static List getListeners()
    {
        synchronized (listeners)
        {
            List l = new ArrayList(listeners.values());
            l.add(bundleTransformer);
            return l;
        }
    }

    static PackageAdmin getPackageAdmin()
    {
        return getPackageAdmin(10000);
    }

    static PackageAdmin getPackageAdmin(long timeout)
    {
        try
        {
            return (PackageAdmin) padmin.waitForService(timeout);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    static StartLevel getStartLevel()
    {
        return getStartLevel(10000);
    }

    static StartLevel getStartLevel(long timeout)
    {
        try
        {
            return (StartLevel) startLevel.waitForService(timeout);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static class ConfigAdminSupport implements Runnable
    {
        private Tracker tracker;

        private ConfigAdminSupport(BundleContext context, FileInstall fileInstall)
        {
            tracker = new Tracker(context, fileInstall);
            Hashtable props = new Hashtable();
            props.put(Constants.SERVICE_PID, tracker.getName());
            context.registerService(ManagedServiceFactory.class.getName(), tracker, props);
            tracker.open();
        }

        public void run()
        {
            tracker.close();
        }

        private class Tracker extends ServiceTracker implements ManagedServiceFactory {

            private final FileInstall fileInstall;
            private ConfigInstaller configInstaller;

            private Tracker(BundleContext bundleContext, FileInstall fileInstall)
            {
                super(bundleContext, ConfigurationAdmin.class.getName(), null);
                this.fileInstall = fileInstall;
            }

            public String getName()
            {
                return "org.apache.felix.fileinstall";
            }

            public void updated(String s, Dictionary dictionary) throws ConfigurationException
            {
                fileInstall.updated(s, dictionary);
            }

            public void deleted(String s)
            {
                fileInstall.deleted(s);
            }

            public Object addingService(ServiceReference serviceReference)
            {
                ConfigurationAdmin cm = (ConfigurationAdmin) super.addingService(serviceReference);
                configInstaller = new ConfigInstaller(context, cm);
                configInstaller.init();
                return cm;
            }

            public void removedService(ServiceReference serviceReference, Object o)
            {
                if (configInstaller != null)
                {
                    configInstaller.destroy();
                    configInstaller = null;
                }
                super.removedService(serviceReference, o);
            }
        }
    }

}