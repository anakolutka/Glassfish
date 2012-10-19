package org.jboss.weld.environment.se.test.instance;

import junit.framework.Assert;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Test;

/**
 * @author Mark Proctor
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class SimpleInstanceTest {
    @Test
    public void testSelect() throws Exception {
        Weld weld = new Weld();
        try {
            WeldContainer wc = weld.initialize();
            Assert.assertNotNull(wc.instance().select(KPT.class).select(new KPQLiteral()).get());
            Assert.assertNotNull(wc.instance().select(KPT.class, new KPQLiteral()).get());
        } finally {
            weld.shutdown();
        }
    }
}
