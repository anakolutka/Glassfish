package org.glassfish.webservices.monitoring;

import org.glassfish.external.probe.provider.annotations.ProbeProvider;
import org.glassfish.external.probe.provider.annotations.Probe;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.Application;

/**
 * 109 deployment probe. A registered listener get to listen the emited
 * 109 deployment/undepolyment events.
 *
 * @author Jitendra Kotamraju
 */
@ProbeProvider(moduleProviderName="glassfish", moduleName="webservices", probeProviderName="109")
public class Deployment109ProbeProvider {

    @Probe(name="deploy")
    public void deploy(@ProbeParam("name") String name,
                       @ProbeParam("app") Application app,
                       @ProbeParam("endpoint") WebServiceEndpoint endpoint) {
        // intentionally left empty.
    }

    @Probe(name="undeploy")
    public void undeploy(@ProbeParam("name")String name) {
        // intentionally left empty.
    }
    
}