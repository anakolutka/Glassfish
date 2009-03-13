package org.glassfish.tests.kernel.deployment;

import org.junit.*;
import org.glassfish.api.event.Events;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.tests.utils.Utils;
import org.glassfish.tests.utils.ConfigApiTest;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.config.support.GlassFishDocument;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.DomDocument;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.sun.hk2.component.ExistingSingletonInhabitant;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.module.bootstrap.StartupContext;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: Mar 12, 2009
 * Time: 9:26:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class EventsTest extends ConfigApiTest {

    static Habitat habitat;
    static File dir;
    static File application;
    static List<EventListener.Event> allEvents = new ArrayList<EventListener.Event>();
    static private EventListener listener = new EventListener() {
        public void event(Event event) {
            //System.out.println("Received event " + event.name());
            allEvents.add(event);
        }
    };

    public String getFileName() {
        return "DomainTest";
    }

    public DomDocument getDocument(Habitat habitat) {
       DomDocument doc = habitat.getByType(GlassFishDocument.class);
        if (doc==null) {
            return new GlassFishDocument(habitat, Executors.newCachedThreadPool(new ThreadFactory() {

                        public Thread newThread(Runnable r) {
                            Thread t = Executors.defaultThreadFactory().newThread(r);
                            t.setDaemon(true);
                            return t;
                        }

                    }));
        }
        return doc;
    }

    @Before
    public void setup() throws IOException {

        // cludge to run only once yet not depend on a static method.
        if (habitat!=null) {
            return;
        }
        habitat  = super.getHabitat();
        habitat.addIndex(new ExistingSingletonInhabitant(habitat.getComponent(Server.class, "server")),
                     Server.class.getName(), ServerEnvironment.DEFAULT_INSTANCE_NAME);

        try {
            dir = File.createTempFile("glassfish", "kerneltests");
            application = File.createTempFile("kerneltest", "tmp");
        } catch (IOException e) {
            e.printStackTrace();
            throw e;

        }
        dir.delete();
        dir.mkdirs();

        application.delete();
        application.mkdirs();

        StartupContext startupContext = new StartupContext(dir, new String[0]);
        habitat.add(new ExistingSingletonInhabitant(startupContext));        

        Events events = habitat.getByContract(Events.class);
        events.register(listener);
    }

    @AfterClass
    public static void tearDown() {
       dir.delete();
       application.delete();

    }

    public static List<EventTypes> getSingletonModuleSuccessfullDeploymentEvents() {
        ArrayList<EventTypes> events = new ArrayList<EventTypes>();
        events.add(Deployment.MODULE_PREPARED);
        events.add(Deployment.MODULE_LOADED);
        events.add(Deployment.MODULE_STARTED);
        events.add(Deployment.APPLICATION_PREPARED);
        events.add(Deployment.APPLICATION_LOADED);
        events.add(Deployment.APPLICATION_STARTED);
        return events;
    }

    public static List<EventTypes> getSingletonModuleSuccessfullUndeploymentEvents() {
        ArrayList<EventTypes> events = new ArrayList<EventTypes>();
        events.add(Deployment.MODULE_STOPPED);
        events.add(Deployment.MODULE_UNLOADED);
        events.add(Deployment.MODULE_CLEANED);
        events.add(Deployment.APPLICATION_STOPPED);
        events.add(Deployment.APPLICATION_UNLOADED);
        events.add(Deployment.APPLICATION_CLEANED);
        return events;
    }

    public static List<EventTypes> asynchonousEvents() {
        ArrayList<EventTypes> events = new ArrayList<EventTypes>();
        events.add(Deployment.DEPLOYMENT_START);
        events.add(Deployment.DEPLOYMENT_SUCCESS);        
        events.add(Deployment.UNDEPLOYMENT_START);
        events.add(Deployment.UNDEPLOYMENT_SUCCESS);
        events.add(Deployment.UNDEPLOYMENT_FAILURE);
        return events;
    }

    @Test
    public void deployTest() throws Exception {

        final List<EventTypes> myTestEvents = getSingletonModuleSuccessfullDeploymentEvents();
        Events events = habitat.getByContract(Events.class);
        EventListener listener = new EventListener() {
            public void event(Event event) {
                if (myTestEvents.contains(event.type())) {
                    myTestEvents.remove(event.type());
                }
            }
        };
        events.register(listener);
        Deployment deployment = habitat.getByContract(Deployment.class);
        DeployCommandParameters params = new DeployCommandParameters(application);
        params.name = "fakeApplication";
        ExtendedDeploymentContext dc = deployment.getContext(Logger.getAnonymousLogger(), application, params);
        ActionReport report = habitat.getComponent(ActionReport.class, "hk2-agent");
        deployment.deploy(dc, report);
        events.unregister(listener);
        Assert.assertEquals(report.getActionExitCode(), ActionReport.ExitCode.SUCCESS);
        for (EventTypes et : myTestEvents) {
            System.out.println("An expected event of type " + et.type() + " was not received");
        }
    }

    @Test
    public void undeployTest() throws Exception {
        
        final List<EventTypes> myTestEvents = getSingletonModuleSuccessfullUndeploymentEvents();
        Events events = habitat.getByContract(Events.class);
        EventListener listener = new EventListener() {
            public void event(Event event) {
                if (myTestEvents.contains(event.type())) {
                    myTestEvents.remove(event.type());
                }
            }
        };
        events.register(listener);
        Deployment deployment = habitat.getByContract(Deployment.class);
        UndeployCommandParameters params = new UndeployCommandParameters("fakeApplication");
        ExtendedDeploymentContext dc = deployment.getContext(Logger.getAnonymousLogger(), application, params);
        ActionReport report = habitat.getComponent(ActionReport.class, "hk2-agent");
        deployment.undeploy("fakeApplication", dc, report);
        Assert.assertEquals(report.getActionExitCode(), ActionReport.ExitCode.SUCCESS);
        for (EventTypes et : myTestEvents) {
            System.out.println("An expected event of type " + et.type() + " was not received");
        }

    }

    @Test
    public void badUndeployTest() throws Exception {
        Deployment deployment = habitat.getByContract(Deployment.class);
        UndeployCommandParameters params = new UndeployCommandParameters("notavalidname");
        ExtendedDeploymentContext dc = deployment.getContext(Logger.getAnonymousLogger(), application, params);
        ActionReport report = habitat.getComponent(ActionReport.class, "hk2-agent");
        deployment.undeploy("fakeApplication", dc, report);
        Assert.assertEquals(report.getActionExitCode(), ActionReport.ExitCode.FAILURE);
    }

    @Test
    public void asynchronousEvents() {
        List<EventTypes> asyncEvents =  asynchonousEvents();
        Iterator<EventTypes> itr = asyncEvents.iterator();
        while (itr.hasNext()) {
            EventTypes et = itr.next();
            for (EventListener.Event evt : allEvents) {
                if (evt.is(et)) {
                    itr.remove();
                }
            }
        }
        for (EventTypes et : asyncEvents) {
            System.out.println("Asynchronous event " + et.type() + " was not received");    
        }
        Assert.assertTrue(asyncEvents.size()==0);        
    }
}
