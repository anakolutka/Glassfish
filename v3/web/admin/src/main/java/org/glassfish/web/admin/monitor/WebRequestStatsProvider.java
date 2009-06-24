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
import org.glassfish.api.statistics.CountStatistic;
import org.glassfish.api.statistics.TimeStatistic;
import org.glassfish.api.statistics.impl.CountStatisticImpl;
import org.glassfish.api.statistics.impl.TimeStatisticImpl;
import org.glassfish.flashlight.statistics.*;
import org.glassfish.flashlight.statistics.factory.CounterFactory;
import org.glassfish.flashlight.statistics.factory.TimeStatsFactory;
import org.glassfish.probe.provider.annotations.*;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.gmbal.ManagedAttribute;

/**
 * Provides the monitoring data at the Web container level
 *
 * @author Prashanth Abbagani
 */
@ManagedObject
@Description("Web Request Statistics")
public class WebRequestStatsProvider {
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
    private Counter errorCount = CounterFactory.createCount();
    private TimeStats requestProcessTime = TimeStatsFactory.createTimeStatsMilli();
    private Logger logger;
    
    private String virtualServerName = null;
    private String moduleName = null;

    public WebRequestStatsProvider(String appName, String vsName, Logger logger) {
        this.logger = logger;
        this.virtualServerName = vsName;
        this.moduleName = appName;
    }

    @ManagedAttribute(id="maxtime")
    @Description( "Longest response time for a request; not a cumulative value, but the largest response time from among the response times" )
    public TimeStatistic getMaxTime() {
        TimeStatisticImpl maxTime = new TimeStatisticImpl(
                requestProcessTime.getMaximumTime(),
                requestProcessTime.getMaximumTime(),
                requestProcessTime.getMinimumTime(),
                requestProcessTime.getTotalTime(),
                "MaxTime",
                "milliseconds",
                "Longest response time for a request; not a cumulative value, but the largest response time from among the response times",
                requestProcessTime.getStartTime(),
                requestProcessTime.getLastSampleTime());
        return maxTime;
    }

    @ManagedAttribute(id="requestcount")
    @Description("Cumulative number of requests processed so far")
    public CountStatistic getRequestCount() {
        CountStatisticImpl requestCount = new CountStatisticImpl("RequestCount", "count", "Cumulative number of requests processed so far");
        requestCount.setCount(requestProcessTime.getCount());
        return requestCount;
    }

    @ManagedAttribute(id="processingtime")
    @Description("Cumulative value of the times taken to process each request, with processing time being the average of request processing times over the request count")
    public TimeStatistic getProcessingTime() {
        TimeStatisticImpl processingTime = new TimeStatisticImpl(
                (long) requestProcessTime.getTime(),
                requestProcessTime.getMaximumTime(),
                requestProcessTime.getMinimumTime(),
                requestProcessTime.getTotalTime(),
                "ProcessingTime",
                "milliseconds",
                "Cumulative value of the times taken to process each request, with processing time being the average of request processing times over the request count ",
                requestProcessTime.getStartTime(),
                requestProcessTime.getLastSampleTime());
        return processingTime;
    }

    @ProbeListener("web:request::requestStartEvent")
    public void requestStartEvent(
        @ProbeParam("request") HttpServletRequest request,
        @ProbeParam("response") HttpServletResponse response,
        @ProbeParam("hostName") String hostName) {
        logger.finest("[TM]requestStartEvent Unprocessed received - virtual-server = " +
                            request.getServerName() + ":" + request.getServerPort() + 
                            ": application = " + request.getContextPath() + " : servlet = " +
                            request.getServletPath() + " : Expecting (vsName, appName) = (" +
                            virtualServerName + ", " + moduleName + ")");
        if ((virtualServerName != null) && (moduleName != null)) {
            //String vs = WebTelemetryBootstrap.getVirtualServerName(
            //    hostName, String.valueOf(request.getServerPort()));
            String contextPath = request.getContextPath();
            String appName = (contextPath == null)? null : 
                                WebStatsProviderBootstrap.getAppName(contextPath);
            if ((appName != null && hostName != null) && hostName.equals(virtualServerName) && appName.equals(moduleName)){
                //increment counts
                requestProcessTime.entry();
                logger.finest("[TM]requestStartEvent resolved - virtual-server = " +
                                    request.getServerName() + ": application = " +
                                    contextPath + " :appName = " + appName + " : servlet = " +
                                    request.getServletPath() + " : port = " +
                                    request.getServerPort());
            }
        }
        else {
            requestProcessTime.entry();
            logger.finest("[TM]requestStartEvent resolved - virtual-server = " +
                                request.getServerName() + ": application = " +
                                request.getContextPath() + " : servlet = " +
                                request.getServletPath());
        }
    }

    @ProbeListener("web:request::requestEndEvent")
    public void requestEndEvent(
        @ProbeParam("request") HttpServletRequest request,
        @ProbeParam("response") HttpServletResponse response,
        @ProbeParam("hostName") String hostName,
        @ProbeParam("statusCode") int statusCode) {
        logger.finest("[TM]requestEndEvent Unprocessed received - virtual-server = " +
                            request.getServerName() + ": application = " +
                            request.getContextPath() + " : servlet = " +
                            request.getServletPath() + " :Response code = " +
                            statusCode + " : Expecting (vsName, appName) = (" +
                            virtualServerName + ", " + moduleName + ")");
        if ((virtualServerName != null) && (moduleName != null)) {
            //String vs = WebTelemetryBootstrap.getVirtualServerName(
            //    hostName, String.valueOf(request.getServerPort()));
            String contextPath = request.getContextPath();
            String appName = (contextPath == null)? null : 
                                WebStatsProviderBootstrap.getAppName(contextPath);
            if ((appName != null && hostName != null) && hostName.equals(virtualServerName) && appName.equals(moduleName)){
                //increment counts
                requestProcessTime.exit();
                if (statusCode > 400)
                    errorCount.increment();
                logger.finest("[TM]requestEndEvent resolved - virtual-server = " +
                                    request.getServerName() + ": application = " +
                                    contextPath + " :appName = " + appName + " : servlet = " +
                                    request.getServletPath() + " : port = " +
                                    request.getServerPort() + " :Response code = " +
                            statusCode + " :Response time = " +
                            requestProcessTime.getTime());
            }
        }
        else {
            requestProcessTime.exit();
            if (statusCode > 400)
                errorCount.increment();
            logger.finest("[TM]requestEndEvent resolved - virtual-server = " +
                                request.getServerName() + ": application = " +
                                request.getContextPath() + " : servlet = " +
                                request.getServletPath() + " : port = " +
                                request.getServerPort()  + " :Response code = " +
                                statusCode + " :Response time = " +
                                requestProcessTime.getTime());
        }
    }

    
    public long getProcessTime() {
        return requestProcessTime.getTotalTime()/requestProcessTime.getCount();
    }

    public String getModuleName() {
        return moduleName;
    }
    
    public String getVSName() {
        return virtualServerName;
    }

}
