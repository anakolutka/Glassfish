package org.glassfish.flashlight.provider;

import org.glassfish.flashlight.provider.FlashlightProbe;
import org.glassfish.flashlight.impl.core.*;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Singleton;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mahesh Kannan
 *         Date: Jul 20, 2008
 */
@Service
@Scoped(Singleton.class)
public class ProbeRegistry {

    private static ProbeRegistry _me = new ProbeRegistry();

    private ConcurrentHashMap<Integer, FlashlightProbe> probeMap =
                new ConcurrentHashMap<Integer, FlashlightProbe>();
    private ConcurrentHashMap<String, FlashlightProbe> probeDesc2ProbeMap =
                new ConcurrentHashMap<String, FlashlightProbe>();

    public static ProbeRegistry getInstance() {
        return _me;
    }

    public void registerProbe(FlashlightProbe probe) {
        probeMap.put(probe.getId(), probe);
        probeDesc2ProbeMap.put(probe.getProbeStr(), probe);
        //System.out.println("[FL]Registered probe : " + probe.getProbeStr());
    }

    public void unregisterProbe(FlashlightProbe probe) {
        probeMap.remove(probe.getId());
    }

    public void unregisterProbe(int id) {
        probeMap.remove(id);
    }

    public FlashlightProbe getProbe(int id) {
        return probeMap.get(id);
    }

    public FlashlightProbe getProbe(String probeStr) {
        //System.out.println("[FL]Get probe : " + probeStr);
        return probeDesc2ProbeMap.get(probeStr);
    }

    public static FlashlightProbe getProbeById(int id) {
        return _me.getProbe(id);
    }
}
