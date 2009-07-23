/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
package com.sun.enterprise.transaction.monitoring;

import java.util.List;

import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.StringStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.impl.StringStatisticImpl;
import org.glassfish.external.probe.provider.annotations.*;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

import org.jvnet.hk2.annotations.Inject;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.transaction.api.TransactionAdminBean;

/**
 * Collects the Transaction Service monitoring data and provides it to the callers.
 *
 * @author Marina Vatkina
 */
//@AMXMetadata(type="transaction-service-mon", group="monitoring", isSingleton=true)
@ManagedObject
@Description("Transaction Service Statistics")
public class TransactionServiceStatsProvider {

    private static final int COLUMN_LENGTH = 25;

    private CountStatisticImpl activeCount = new CountStatisticImpl("ActiveCount", "count", 
            "Provides the number of transactions that are currently active.");

    private CountStatisticImpl committedCount = new CountStatisticImpl("CommittedCount", "count", 
            "Provides the number of transactions that have been committed.");

    private CountStatisticImpl rolledbackCount = new CountStatisticImpl("RolledbackCount", "count", 
            "Provides the number of transactions that have been rolled back.");

    private StringStatisticImpl inflightTransactions = new StringStatisticImpl("ActiveIds", "List", 
                "Provides the IDs of the transactions that are currently active a.k.a. in-flight " 
                + "transactions. Every such transaction can be rolled back after freezing the transaction " 
                + "service." );

    private StringStatisticImpl state = new StringStatisticImpl("State", "String", 
                "Indicates if the transaction service has been frozen");

    private boolean isFrozen = false;

    @Inject private JavaEETransactionManager txMgr;

    @ManagedAttribute(id="activecount")
    @Description( "Provides the number of transactions that are currently active." )
    public CountStatistic getActiveCount() {
        return activeCount.getStatistic();
    }

    @ManagedAttribute(id="committedcount")
    @Description( "Provides the number of transactions that have been committed." )
    public CountStatistic getCommittedCount() {
        return committedCount.getStatistic();
    }

    @ManagedAttribute(id="rolledbackcount")
    @Description( "Provides the number of transactions that have been rolled back." )
    public CountStatistic getRolledbackCount() {
        return rolledbackCount.getStatistic();
    }
    
    @ManagedAttribute(id="state")
    @Description( "Indicates if the transaction service has been frozen." )
    public StringStatistic getState() {
        state.setCurrent((isFrozen)? "True": "False");
        return state.getStatistic();
    }
    
    @ManagedAttribute(id="activeids")
    @Description( "List of inflight transactions." )
    public StringStatistic getActiveIds() {
        StringBuffer strBuf = new StringBuffer(1024);

        if (txMgr == null) {
            System.out.println("ERROR: Cannot construct the probe. Transaction Manager is NULL!");
            inflightTransactions.setCurrent("");
            return inflightTransactions.getStatistic();
        }

        List aList = txMgr.getActiveTransactions();
        if (!aList.isEmpty()) {
            //Set the headings for the tabular output
            if (aList.size() > 0) {
                String colName = "Transaction Id";
                strBuf.append("\n\n");
                strBuf.append(colName);
                for (int i=colName.length(); i<COLUMN_LENGTH+15; i++){
                    strBuf.append(" ");
                }
                colName = "Status";
                strBuf.append(colName);
                for (int i=colName.length(); i<COLUMN_LENGTH; i++){
                    strBuf.append(" ");
                }
                colName = "ElapsedTime(ms)";
                strBuf.append(colName);
                for (int i=colName.length(); i<COLUMN_LENGTH; i++){
                    strBuf.append(" ");
                }
                colName = "ComponentName";
                strBuf.append(colName);
                for (int i=colName.length(); i<COLUMN_LENGTH; i++){
                    strBuf.append(" ");
                }
                strBuf.append("ResourceNames\n");
            }

            for (int i=0; i < aList.size(); i++) {
                TransactionAdminBean txnBean = (TransactionAdminBean)aList.get(i);
                String txnId = txnBean.getId();

                strBuf.append("\n");
                strBuf.append(txnId);
                for (int j=txnId.length(); j<COLUMN_LENGTH+15; j++){
                    strBuf.append(" ");
                }
                strBuf.append(txnBean.getStatus());
                for (int j=txnBean.getStatus().length(); j<COLUMN_LENGTH; j++){
                    strBuf.append(" ");
                }
                strBuf.append(String.valueOf(txnBean.getElapsedTime()));
                for (int j=(String.valueOf(txnBean.getElapsedTime()).length()); j<COLUMN_LENGTH; j++){
                    strBuf.append(" ");
                }

                strBuf.append(txnBean.getComponentName());
                for (int j=txnBean.getComponentName().length(); j<COLUMN_LENGTH; j++){
                    strBuf.append(" ");
                }
                List<String> resourceList = txnBean.getResourceNames();
                if (resourceList != null) {
                    for (int k = 0; k < resourceList.size(); k++) {
                        strBuf.append(resourceList.get(k));
                        strBuf.append(",");
                    }
                }
            }
        }

        inflightTransactions.setCurrent((strBuf == null)? "" : strBuf.toString());
        return inflightTransactions.getStatistic();
    }
    
    @ProbeListener("glassfish:transaction:transaction-service:activated")
    public void transactionActivatedEvent() {
        activeCount.increment();
    }

    @ProbeListener("glassfish:transaction:transaction-service:deactivated")
    public void transactionDeactivatedEvent() {
        activeCount.decrement();
    }

    @ProbeListener("glassfish:transaction:transaction-service:committed")
    public void transactionCommittedEvent() {
        committedCount.increment();
        activeCount.decrement();
    }

    @ProbeListener("glassfish:transaction:transaction-service:rolledback")
    public void transactionRolledbackEvent() {
        rolledbackCount.increment();
        activeCount.decrement();
    }

    @ProbeListener("glassfish:transaction:transaction-service:freeze")
    public void freezeEvent(@ProbeParam("isFrozen") boolean b) {
        isFrozen = b;
    }
}
