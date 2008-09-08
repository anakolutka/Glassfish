/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * Header Notice in each file and include the License file 
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.  
 * If applicable, add the following below the CDDL Header, 
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */
package com.sun.enterprise.v3.services.impl;

import com.sun.grizzly.ProtocolFilter;
import com.sun.grizzly.http.DefaultProcessorTask;
import com.sun.grizzly.http.HttpWorkerThread;
import com.sun.grizzly.tcp.Adapter;
import com.sun.grizzly.tcp.Request;
import com.sun.grizzly.tcp.Response;
import com.sun.grizzly.tcp.StaticResourcesAdapter;
import com.sun.grizzly.util.InputReader;
import com.sun.grizzly.util.WorkerThread;
import com.sun.grizzly.util.buf.ByteChunk;
import java.util.List;
import java.util.logging.Level;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.ApplicationContainer;
import org.jvnet.hk2.component.Habitat;

import com.sun.grizzly.util.http.mapper.Mapper;
import com.sun.grizzly.util.http.mapper.MappingData;
import com.sun.grizzly.util.http.HttpRequestURIDecoder;
import com.sun.grizzly.util.buf.MessageBytes;
import com.sun.grizzly.util.buf.UDecoder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Contaier's mapper which maps {@link ByteBuffer} bytes representation 
 * to an  {@link Adapter}, {@link ApplicationContainer} and 
 * {@link ProtocolFilter} chain. The mapping result is stored inside
 * {@link MappindData} which is eventually shared with the {@link CoyoteAdapter}, 
 * which is the entry point with the Catalina Servlet Container.
 * 
 * @author Jeanfrancois Arcand
 * @author Alexey Stashok
 */
public class ContainerMapper extends StaticResourcesAdapter{

    private final static String ROOT = "/";
    private Mapper mapper;
    private GrizzlyEmbeddedHttp grizzlyEmbeddedHttp;
    private String defaultHostName = "server";
    private UDecoder urlDecoder = new UDecoder();
    private boolean snifferInitialized = false;

    private final Habitat habitat;
    private final GrizzlyService grizzlyService;

    private ConcurrentLinkedQueue<HttpParserState> parserStates;
    
    private final static int MAPPING_DATA = 12;
    private final static int MAPPED_ADAPTER = 13;
    
    private static byte[] errorBody =
            HttpUtils.getErrorPage("Glassfish/v3","HTTP Status 404");
    
    public ContainerMapper(GrizzlyService grizzlyService, GrizzlyEmbeddedHttp grizzlyEmbeddedHttp) {
        this.grizzlyEmbeddedHttp = grizzlyEmbeddedHttp;
        this.grizzlyService = grizzlyService;
        this.habitat = grizzlyService.habitat; 
        parserStates = new ConcurrentLinkedQueue<HttpParserState>();
        logger = GrizzlyEmbeddedHttp.logger();
        setRootFolder(GrizzlyEmbeddedHttp.getWebAppRootPath());
    }

    
    /**
     * Set the default host that will be used when we map.
     * @param defaultHost
     */
    protected void setDefaultHost(String defaultHost) {
        defaultHostName = defaultHost;
    }

    
    /**
     * Set the {@link V3Mapper} instance used for mapping the container 
     * and its associated {@link Adapter}.
     * @param mapper
     */
    protected void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    
    /**
     * Configure the {@link V3Mapper}. 
     */
    protected synchronized void configureMapper() {
        mapper.setDefaultHostName(defaultHostName);
        // Container deployed have the right to override the default setting.
        Mapper.setAllowReplacement(true);
    }

    
    /**
     * Map the request to its associated {@link Adapter}.
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    public void service(Request req, Response res) throws IOException{
        MappingData mappingData = null;
        try{        
            MessageBytes decodedURI = req.decodedURI();
            decodedURI.duplicate(req.requestURI());
            mappingData = (MappingData)req.getNote(MAPPING_DATA);
            if (mappingData == null){
                mappingData = new MappingData();
                req.setNote(MAPPING_DATA, mappingData);
            }

            Adapter adapter = null;      
            HttpRequestURIDecoder.decode(decodedURI,urlDecoder,null,null);

            adapter = map(req, decodedURI, mappingData);
            if (adapter == null && !snifferInitialized) {
                initializeFileURLPattern();
                adapter = map(req, decodedURI, mappingData);
            }

            if (adapter == null){
                super.service(req, res);
            }  else {  
                req.setNote(MAPPED_ADAPTER, adapter);
                adapter.service(req, res);
            }
        } catch (Exception ex){
            try{
                res.setStatus(404);
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Invalid URL: " + req.decodedURI(), ex);          
                }                 
                customizedErrorPage(req,res);
            } catch (Exception ex2){
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, "Unable to error page", ex2);          
                }                 
            }
        }  finally{
            if (mappingData != null){
                mappingData.recycle();
            }
        }
    }

    public synchronized void initializeFileURLPattern() {
        for (Sniffer sniffer : grizzlyService.habitat.getAllByContract(Sniffer.class)) {
            if (sniffer.getURLPatterns()!=null) {
                SnifferAdapter adapter = grizzlyService.habitat.getComponent(SnifferAdapter.class);
                adapter.initialize(sniffer, this);
                for (String pattern : sniffer.getURLPatterns()) {
                    this.register(pattern, grizzlyService.hosts, adapter, null, null);
                }
            }
        }
        snifferInitialized=true;
    }

    Adapter map(Request req, MessageBytes decodedURI, MappingData mappingData) throws Exception {

        if (mappingData==null) {
            mappingData = (MappingData)req.getNote(MAPPING_DATA);
        }

            // Map the request to its Adapter/Container and also it's Servlet if
            // the request is targetted to the CoyoteAdapter.
            mapper.map(req.serverName(), decodedURI, mappingData);

            ContextRootInfo contextRootInfo = null;
            if (mappingData.context != null && mappingData.context instanceof ContextRootInfo) {
                contextRootInfo = (ContextRootInfo) mappingData.context;
                return contextRootInfo.getAdapter();
            } else if (mappingData.context != null && mappingData.context.getClass()
                    .getName().equals("com.sun.enterprise.web.WebModule")) {
                return ((V3Mapper) mapper).getAdapter();
            }
        return null;
    }
    /**
     * Recycle the mapped {@link Adapter} and this instance.
     * @param req
     * @param res
     * @throws java.lang.Exception
     */
    @Override
    public void afterService(Request req, Response res) throws Exception{
        try{
            Adapter adapter = (Adapter)req.getNote(MAPPED_ADAPTER);
            if (adapter != null){
                adapter.afterService(req, res);

            }
            super.afterService(req, res);
        } finally {
            req.setNote(MAPPED_ADAPTER, null);
        }
    }
 
    
    /**
     * Return an error page customized for GlassFish v3. 
     * @param req
     * @param res
     * @throws java.lang.Exception
     */
    @Override
    protected void customizedErrorPage(Request req,Response res) throws Exception {
        ByteChunk chunk = new ByteChunk();
        chunk.setBytes(errorBody, 0, errorBody.length);
        res.setContentLength(errorBody.length);
        res.sendHeaders();
        res.doWrite(chunk);
    }
     
    
    public void register(String contextRoot, Collection<String> vs, Adapter adapter,
            ApplicationContainer container, List<ProtocolFilter> contextProtocolFilters) {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("MAPPER(" + this + ") REGISTER contextRoot: " + contextRoot +
                    " adapter: " + adapter + " container: " + container +
                    " port: " + grizzlyEmbeddedHttp.getPort());
        }

        /*
         * In the case of CoyoteAdapter, return, because the context will
         * have already been registered with the mapper by the connector's
         * MapperListener, in response to a JMX event
         */
        if (adapter.getClass().getName().equals("org.apache.catalina.connector.CoyoteAdapter")) {
            return;
        }

        for (String host : vs) {
            mapper.addContext(host, contextRoot,
                    new ContextRootInfo(adapter, container, contextProtocolFilters), new String[0], null);
        }
    }

    
    public void unregister(String contextRoot) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("MAPPER (" + this + ") UNREGISTER contextRoot: " + contextRoot);
        }
        mapper.removeContext(defaultHostName, contextRoot);
    }

    
    /**
     * Based on the context-root, configure Grizzly's ProtocolChain with the 
     * proper ProtocolFilter, and if available, proper Adapter.
     * @return true if the ProtocolFilter was properly set.
     */
    public boolean map(SelectionKey selectionKey, ByteBuffer byteBuffer, GlassfishProtocolChain protocolChain,
            List<ProtocolFilter> defaultProtocolFilters,
            ContextRootInfo fallbackContextRootInfo) throws Exception {

        HttpParserState state = parserStates.poll();
        if (state == null) {
            state = new HttpParserState();
        } else {
            state.reset();
        }
        
        state.setBuffer(byteBuffer);
        
        byte[] contextBytes = null;
        byte[] hostBytes = null;
        
        try {
            // Read the request line, and parse the context root by removing 
            // all trailling // or ?
            contextBytes = HttpUtils.readRequestLine(selectionKey, state,
                    InputReader.getDefaultReadTimeout());

            if (contextBytes != null) {
                state.setState(0);
                // Read available bytes and try to find the host header.
                hostBytes = HttpUtils.readHost(selectionKey, state,
                        InputReader.getDefaultReadTimeout());
            }
        } finally {
            parserStates.offer(state);
        }
                        
        // No bytes then fail.
        if (contextBytes == null) {
            return false;
        }

        MessageBytes decodedURI = MessageBytes.newInstance();
        decodedURI.setBytes(contextBytes, 0, contextBytes.length);

        MessageBytes hostMB = MessageBytes.newInstance();
        if (hostBytes != null) {
            hostMB.setBytes(hostBytes, 0, hostBytes.length);
        }
        
        // Decode the request to make sure this is not an attack like
        // a directory traversal vulnerability.
        try{
            HttpRequestURIDecoder.decode(decodedURI,urlDecoder,
                    (String)grizzlyEmbeddedHttp.getProperty("uriEncoding"),null);
        } catch (Exception ex){
            // We fail to decode the request, hence we don't service it.
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Invalid url", ex);          
            }  
            return false;
        }
        // Parse the host. If not found, add it based on the current host.
        HttpUtils.parseHost(hostMB, ((SocketChannel)selectionKey.channel()).socket());

        //TODO: Use ThreadAttachment instead.
        MappingData mappingData = new MappingData();
        
        // Map the request to its Adapter/Container and also it's Servlet if 
        // the request is targetted to the CoyoteAdapter.
        mapper.map(hostMB, decodedURI, mappingData);

        Adapter adapter = null;
        ContextRootInfo contextRootInfo = null;

        // First, let's see if the request is NOT for the CoyoteAdapter, but for
        // another adapter like grail/rail.
        if (mappingData.context != null && mappingData.context instanceof ContextRootInfo) {
            contextRootInfo = (ContextRootInfo) mappingData.context;
            adapter = contextRootInfo.getAdapter();
        } else if (mappingData.context != null && mappingData.context.getClass()
                .getName().equals("com.sun.enterprise.web.WebModule")) {
            
            // Copy the decoded bytes so it can be later re-used by the CoyoteAdapter
            MessageBytes fullDecodedUri = MessageBytes.newInstance();
            fullDecodedUri.duplicate(decodedURI);
            fullDecodedUri.toBytes();

            // We bind the current information to the WorkerThread so CoyoteAdapter
            // can read it and avoid trying to map. Note that we cannot re-use
            // the object as Grizzly ARP might used them from a different Thread.
            WorkerThread workerThread = (WorkerThread) Thread.currentThread();
            workerThread.getAttachment().setAttribute("mappingData", mappingData);
            workerThread.getAttachment().setAttribute("decodedURI", fullDecodedUri);

            adapter = ((V3Mapper) mapper).getAdapter();
        }    
                
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("MAP (" + this + ") contextRoot: " + new String(contextBytes) +
                    " defaultProtocolFilters: " + defaultProtocolFilters +
                    " fallback: " + fallbackContextRootInfo
                    + " adapter: " + adapter +
                    " mappingData.context " + mappingData.context);
        }                
        
        if (adapter == null && fallbackContextRootInfo != null) {
            adapter = fallbackContextRootInfo.getAdapter();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Fallback adapter is taken: " + adapter);
            }
        }

        if (adapter == null) {
            return false;
        }

        bindAdapter(adapter);

        List<ProtocolFilter> filtersToInject = null;

        // If we have a container, let the request flow throw the default http path
        if (contextRootInfo != null && contextRootInfo.getContainer() != null) {
            List<ProtocolFilter> contextRootProtocolFilters =
                    contextRootInfo.getProtocolFilters();

            filtersToInject = contextRootProtocolFilters != null ? contextRootProtocolFilters : defaultProtocolFilters;

            if (filtersToInject != null) {
                protocolChain.setDynamicProtocolFilters(filtersToInject);
            }
        } else if (fallbackContextRootInfo != null) {
            List<ProtocolFilter> fallbackProtocolFilters =
                    fallbackContextRootInfo.getProtocolFilters();
            if (fallbackProtocolFilters != null && !fallbackProtocolFilters.isEmpty()) {
                protocolChain.setDynamicProtocolFilters(fallbackProtocolFilters);
            }
        }

        return true;
    }

    /**
     * Return the Container associated with the current context-root, null
     * if not found. If the Adapter is found, bind it to the current 
     * ProcessorTask.
     */
    private Adapter bindAdapter(Adapter adapter) {
        // If no Adapter has been found, add a default one. This is the equivalent
        // of having virtual host.
        bindProcessorTask(adapter);

        return adapter;
    }

    // -------------------------------------------------------------------- //
    /**
     * Bind to the current WorkerThread the proper instance of ProcessorTask. 
     * @param adapter The Adapter associated with the ProcessorTask
     */
    private void bindProcessorTask(Adapter adapter) {
        HttpWorkerThread workerThread =
                (HttpWorkerThread) Thread.currentThread();
        DefaultProcessorTask processorTask =
                (DefaultProcessorTask) workerThread.getProcessorTask();
        if (processorTask == null) {
            try {
                //TODO: Promote setAdapter to ProcessorTask?
                processorTask = (DefaultProcessorTask) grizzlyEmbeddedHttp.getProcessorTask();
            } catch (ClassCastException ex) {
                logger.log(Level.SEVERE,
                        "Invalid ProcessorTask instance", ex);
            }
            workerThread.setProcessorTask(processorTask);
        }
        processorTask.setAdapter(adapter);
    }

    /**
     * Class represents context-root associated information
     */
    public static class ContextRootInfo {

        protected Adapter adapter;
        protected ApplicationContainer container;
        protected List<ProtocolFilter> protocolFilters;

        public ContextRootInfo() {
        }

        public ContextRootInfo(Adapter adapter, ApplicationContainer container, List<ProtocolFilter> protocolFilters) {
            this.adapter = adapter;
            this.container = container;
            this.protocolFilters = protocolFilters;
        }

        public Adapter getAdapter() {
            return adapter;
        }

        public void setAdapter(Adapter adapter) {
            this.adapter = adapter;
        }

        public ApplicationContainer getContainer() {
            return container;
        }

        public void setContainer(ApplicationContainer container) {
            this.container = container;
        }

        public List<ProtocolFilter> getProtocolFilters() {
            return protocolFilters;
        }

        public void setProtocolFilters(List<ProtocolFilter> protocolFilters) {
            this.protocolFilters = protocolFilters;
        }
    }
}
