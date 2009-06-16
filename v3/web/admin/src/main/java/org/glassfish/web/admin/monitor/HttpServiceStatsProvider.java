/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.web.admin.monitor;

import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jvnet.hk2.component.PostConstruct;

import org.glassfish.flashlight.statistics.*;
import org.glassfish.flashlight.statistics.factory.TimeStatsFactory;
import org.glassfish.flashlight.client.ProbeListener;
import org.glassfish.flashlight.provider.annotations.ProbeParam;
import org.glassfish.api.statistics.CountStatistic;
import org.glassfish.api.statistics.TimeStatistic;
import org.glassfish.api.statistics.impl.CountStatisticImpl;
import org.glassfish.api.statistics.impl.TimeStatisticImpl;
import java.util.HashMap;

import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * Provides the monitoring data at the Web container level
 *
 * @author Prashanth Abbagani
 */
@ManagedObject
@Description( "Web Container HTTP Service Statistics" )
public class HttpServiceStatsProvider implements PostConstruct {
    //Provides the longest response time for a request - not a cumulative value, 
    //but the largest response time from among the response times.
    //private Counter maxTime = CounterFactory.createCount();
    //Provides cumulative value of the times taken to process each request. 
    //The processing time is the average of request processing times over the request count.
    //private Counter processingTime = CounterFactory.createCount();
    //Provides cumulative number of the requests processed so far.
    //private Counter requestCount = CounterFactory.createCount();
    //Provides the cumulative value of the error count. The error count represents 
    //the number of cases where the response code was greater than or equal to 400.
    private CountStatisticImpl errorCount = new CountStatisticImpl("ErrorCount", "count", "Number of responses with a status code greater than 400");
    private CountStatisticImpl error200Count = new CountStatisticImpl("Count200", "count", "Number of responses with a status code equal to 200");
    private CountStatisticImpl error2xxCount = new CountStatisticImpl("Count2xx", "count", "Number of responses with a status code in the 2xx range");
    private CountStatisticImpl error302Count = new CountStatisticImpl("Count302", "count", "Number of responses with a status code equal to 302");
    private CountStatisticImpl error304Count = new CountStatisticImpl("Count304", "count", "Number of responses with a status code equal to 304");
    private CountStatisticImpl error3xxCount = new CountStatisticImpl("Count3xx", "count", "Number of responses with a status code in the 3xx range");
    private CountStatisticImpl error400Count = new CountStatisticImpl("Count400", "count", "Number of responses with a status code equal to 400");
    private CountStatisticImpl error401Count = new CountStatisticImpl("Count401", "count", "Number of responses with a status code equal to 401");
    private CountStatisticImpl error403Count = new CountStatisticImpl("Count403", "count", "Number of responses with a status code equal to 403");
    private CountStatisticImpl error404Count = new CountStatisticImpl("Count404", "count", "Number of responses with a status code equal to 404");
    private CountStatisticImpl error4xxCount = new CountStatisticImpl("Count4xx", "count", "Number of responses with a status code in the 4xx range");
    private CountStatisticImpl error503Count = new CountStatisticImpl("Count503", "count", "Number of responses with a status code equal to 503");
    private CountStatisticImpl error5xxCount = new CountStatisticImpl("Count5xx", "count", "Number of responses with a status code in the 5xx range");
    private CountStatisticImpl errorOtherCount = new CountStatisticImpl("CountOther", "count", "Number of responses with other status codes");
    private TimeStats requestProcessTime = TimeStatsFactory.createTimeStatsMilli();
    private Logger logger = Logger.getLogger(HttpServiceStatsProvider.class.getName());
    private String virtualServerName = null;
    private HashMap<String, CountStatisticImpl> errorMap = new HashMap<String, CountStatisticImpl>();

    public HttpServiceStatsProvider(String vsName) {
        this.virtualServerName = vsName;
        errorMap.put("200", error200Count);
        errorMap.put("2", error2xxCount);
        errorMap.put("302", error302Count);
        errorMap.put("304", error304Count);
        errorMap.put("3", error3xxCount);
        errorMap.put("400", error400Count);
        errorMap.put("401", error401Count);
        errorMap.put("403", error403Count);
        errorMap.put("404", error404Count);
        errorMap.put("4", error4xxCount);
        errorMap.put("503", error503Count);
        errorMap.put("5", error5xxCount);
        errorMap.put("9", errorOtherCount);
    }


    public void postConstruct() {
    }

    @ManagedAttribute(id="maxtime-count")
    @Description( "Provides the longest response time for a response - not a cumulative value, but the largest response time from among response times." )
    public TimeStatistic getMaximumTime() {
        TimeStatisticImpl maxTime = new TimeStatisticImpl(
                requestProcessTime.getMaximumTime(),
                requestProcessTime.getMaximumTime(),
                requestProcessTime.getMinimumTime(),
                requestProcessTime.getTotalTime(),
                "MaxTime",
                "milliseconds",
                "Provides the longest response time for a response - not a cumulative value, but the largest response time from among response times.",
                requestProcessTime.getStartTime(),
                requestProcessTime.getLastSampleTime());
        return maxTime;
    }

    @ManagedAttribute(id="requestcount-count")
    @Description( "Provides cumulative number of requests processed so far" )
    public CountStatistic getCount() {
        CountStatisticImpl requestCount = new CountStatisticImpl(
                "RequestCount",
                "count",
                "Provides cumulative number of requests processed so far.");
        requestCount.setCount(requestProcessTime.getCount());
        return requestCount;
    }

    @ManagedAttribute(id="processingtime-count")
    @Description( "Provides cumulative value of the times taken to process each request The processing time is the average request processing times over the request count." )
    public TimeStatistic getTime() {
        TimeStatisticImpl processingTime = new TimeStatisticImpl(
                (long) requestProcessTime.getTime(),
                requestProcessTime.getMaximumTime(),
                requestProcessTime.getMinimumTime(),
                requestProcessTime.getTotalTime(),
                "ProcessingTime",
                "milliseconds",
                "Provides cumulative value of the times taken to process each request The processing time is the average request processing times over the request count.",
                requestProcessTime.getStartTime(),
                requestProcessTime.getLastSampleTime());
        return processingTime;
    }

    @ManagedAttribute(id="errorcount-count")
    @Description( "" )
    public CountStatistic getErrorCount() {
        return errorCount;
    }
    
    @ManagedAttribute(id="count200-count")
    @Description( "" )
    public CountStatistic getError200Count() {
        return error200Count;
    }
    
    @ManagedAttribute(id="count2xx-count")
    @Description( "" )
    public CountStatistic getError2xxCount() {
        return error2xxCount;
    }
    
    @ManagedAttribute(id="count302-count")
    @Description( "" )
    public CountStatistic getError302Count() {
        return error302Count;
    }
    
    @ManagedAttribute(id="count304-count")
    @Description( "" )
    public CountStatistic getError304Count() {
        return error304Count;
    }

    @ManagedAttribute(id="count3xx-count")
    @Description( "" )
    public CountStatistic getError3xxCount() {
        return error3xxCount;
    }

    @ManagedAttribute(id="count400-count")
    @Description( "" )
    public CountStatistic getError400Count() {
        return error400Count;
    }

    @ManagedAttribute(id="count401-count")
    @Description( "" )
    public CountStatistic getError401Count() {
        return error401Count;
    }

    @ManagedAttribute(id="count403-count")
    @Description( "" )
    public CountStatistic getError403Count() {
        return error403Count;
    }

    @ManagedAttribute(id="count404-count")
    @Description( "" )
    public CountStatistic getError404Count() {
        return error404Count;
    }

    @ManagedAttribute(id="count4xx-count")
    @Description( "" )
    public CountStatistic getError4xxCount() {
        return error4xxCount;
    }

    @ManagedAttribute(id="count503-count")
    @Description( "" )
    public CountStatistic getError503Count() {
        return error503Count;
    }

    @ManagedAttribute(id="count5xx-count")
    @Description( "" )
    public CountStatistic getError5xxCount() {
        return this.error5xxCount;
    }

    @ManagedAttribute(id="countother-count")
    @Description( "" )
    public CountStatistic getErrorOtherCount() {
        return this.errorOtherCount;
    }

    @ProbeListener("web:request::requestStartEvent")
    public void requestStartEvent(
        @ProbeParam("request") HttpServletRequest request,
        @ProbeParam("response") HttpServletResponse response,
        @ProbeParam("hostName") String hostName) {
        //String vsName = HttpServiceTelemetryBootstrap.getVirtualServer(
        //    hostName, String.valueOf(request.getServerPort()));
        //if ((vsName != null) && (vsName.equals(virtualServerName))) {
        if ((hostName != null) && (hostName.equals(virtualServerName))) {
            requestProcessTime.entry();
            Logger.getLogger(HttpServiceStatsProvider.class.getName()).finest(
                    "[TM]requestStartEvent received - virtual-server = " +
                                request.getServerName() + " : port = " +
                                request.getServerPort());
            logger.finest("[TM]requestStartEvent received - virtual-server = " +
                                request.getServerName() + " : port = " +
                                request.getServerPort());
        }
    }

    @ProbeListener("web:request::requestEndEvent")
    public void requestEndEvent(
        @ProbeParam("request") HttpServletRequest request,
        @ProbeParam("response") HttpServletResponse response,
        @ProbeParam("hostName") String hostName,
        @ProbeParam("statusCode") int statusCode) {

    //    String vsName = HttpServiceTelemetryBootstrap.getVirtualServer(
    //        hostName, String.valueOf(request.getServerPort()));
   //     if ((vsName != null) && (vsName.equals(virtualServerName))) {
        if ((hostName != null) && (hostName.equals(virtualServerName))) {
            requestProcessTime.exit();
            incrementStatsCounter(statusCode);
            logger.finest("[TM]requestEndEvent received - virtual-server = " +
                                request.getServerName() + ": application = " +
                                request.getContextPath() + " : servlet = " +
                                request.getServletPath() + " :Response code = " +
                                statusCode + " :Response time = " +
                                requestProcessTime.getTime());
        }
    }

    
    public long getProcessTime() {
        return requestProcessTime.getTotalTime()/requestProcessTime.getCount();
    }

    private void incrementStatsCounter(int statusCode) {
        CountStatisticImpl errorCounter = errorMap.get(Integer.toString(statusCode));
        if (errorCounter == null) 
            errorCounter = errorMap.get(Integer.toString(Math.round(statusCode/100)));
        if (errorCounter == null)
            errorCounter = errorMap.get("9");
        errorCounter.increment();
        if (statusCode > 400)
            errorCount.increment();
    }
}