package org.jboss.weld.environment.servlet.test.el;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import static org.jboss.weld.environment.servlet.test.util.JettyDeployments.JETTY_ENV;

@RunWith(Arquillian.class)
public class JsfTest extends JsfTestBase {

    @Deployment(testable = false)
    public static WebArchive deployment() {
        return JsfTestBase.deployment()
                .addAsWebInfResource(JETTY_ENV, "jetty-env.xml");
    }

}
