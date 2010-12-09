/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.acme;

import java.sql.*;
import java.util.Set;
import java.util.HashSet;
import javax.ejb.*;
import javax.jms.*;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.annotation.Resource;

/**
 *
 * @author marina vatkina
 */

@Singleton
public class MyBean {

    private static final String XA_RESOURCE = "jdbc/xa";

    private Set<String> records = new HashSet<String>();

    @Resource(name="jms/MyQueueConnectionFactory", mappedName="jms/ejb_mdb_QCF")
    QueueConnectionFactory fInject;

    @Resource(mappedName="jms/ejb_mdb_Queue")
    Queue qInject;

    public void record(String msg) throws Exception {
         System.out.println("Adding msg: " + msg);
         records.add(msg);
    }

    public int verifyxa() throws Exception {
        InitialContext initCtx = new InitialContext();
        DataSource ds = (DataSource) initCtx.lookup(XA_RESOURCE);

        return verify(ds) + records.size();
   }

    public int verify(DataSource ds) throws Exception {
        String selectStatement = "select * from student";
        java.sql.Connection c = ds.getConnection();
        PreparedStatement ps = c.prepareStatement(selectStatement);
        ResultSet rs = ps.executeQuery();
        int result = 0;
        while (rs.next()) {
            result++;
            System.out.println("Found: " + rs.getString(1) + " : " + rs.getString(2));
        }
        rs.close();
        ps.close();
        c.close();

        return result;
    }

    public boolean testtwo(int id) throws Exception {
        InitialContext initCtx = new InitialContext();
        DataSource ds2 = (DataSource) initCtx.lookup(XA_RESOURCE);

        return test(id, ds2, true);
    }

    private boolean test(int id, DataSource ds, boolean useFailureInducer) throws Exception {
        String insertStatement = "insert into student values ( ? , ? )";
        java.sql.Connection c = ds.getConnection();
        PreparedStatement ps = c.prepareStatement(insertStatement);

        QueueConnection qConn = fInject.createQueueConnection();
        QueueSession qSession = qConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        QueueSender qSender = qSession.createSender(qInject);
        TextMessage tMessage = null;

        if (useFailureInducer) {
            com.sun.jts.utils.RecoveryHooks.FailureInducer.activateFailureInducer();
            com.sun.jts.utils.RecoveryHooks.FailureInducer.setWaitPoint(com.sun.jts.utils.RecoveryHooks.FailureInducer.PREPARED, 60);
        }

        for (int i = 0; i < 3; i++) {
            System.err.println("Call # " + (i + 1));
            ps.setString(1, "BAA" + id + i);
            ps.setString(2, "BBB" + id + i);
            ps.executeUpdate();

            tMessage = qSession.createTextMessage("MAA" + id + i);
            qSender.send(tMessage);

            if (!useFailureInducer) {
                try {
                    Thread.sleep(7000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        ps.close();
        c.close();
        qSession.close();
        qConn.close();
        System.err.println("Insert successfully");

        return true;
    }

}
