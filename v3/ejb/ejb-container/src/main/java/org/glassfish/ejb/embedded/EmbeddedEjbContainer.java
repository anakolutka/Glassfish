package org.glassfish.ejb.embedded;

import org.glassfish.api.embedded.EmbeddedContainer;
import org.glassfish.api.embedded.Port;
import org.glassfish.api.embedded.Server;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.beans.PropertyVetoException;

import com.sun.enterprise.config.serverbeans.EjbContainer;
import com.sun.enterprise.config.serverbeans.Config;

/**
 * @author Jerome Dochez
 */
@Service
public class EmbeddedEjbContainer implements EmbeddedContainer {

    final Habitat habitat;
    

    EmbeddedEjbContainer(EjbBuilder builder) {
        this.habitat = builder.habitat;
    }


    public List<Sniffer> getSniffers() {
        List<Sniffer> sniffers = new ArrayList<Sniffer>();
        sniffers.add(habitat.getComponent(Sniffer.class, "Ejb"));
        Sniffer security = habitat.getComponent(Sniffer.class, "Security");
        if (security!=null) {
            sniffers.add(security);
        }
        return sniffers;
    }

    public void start() {
    }

    public void stop() {

    }

}
