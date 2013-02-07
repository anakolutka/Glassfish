package org.glassfish.test.jms.injection.ejb;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.jms.*;
import javax.ejb.*;

public class MyInterceptor {

    static String context;

    @Resource(mappedName = "jms/jms_unit_test_Queue")
    private Queue queue;

    @Inject
    @JMSConnectionFactory("jms/jms_unit_test_QCF")
    @JMSSessionMode(JMSContext.AUTO_ACKNOWLEDGE)
    private JMSContext jmsContext;

     @AroundInvoke
     public Object sendMsg(InvocationContext ctx) throws Exception {

        Object[] params = ctx.getParameters();
        
        try {
            JMSProducer producer = jmsContext.createProducer();
            TextMessage msg = jmsContext.createTextMessage((String) params[0]);
            producer.send(queue, msg);
            context = jmsContext.toString();
        } catch (Exception e) {
            throw new EJBException(e);
        }
        return ctx.proceed();
    }
}