/**
 * @file    Manager.java
 * @brief Servlet Manager
 * @author Doug Anson
 * @version 1.0
 * @see
 *
 * Copyright 2015. ARM Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.arm.mbed.edgex.shadow.service.servlet;

import com.arm.mbed.edgex.shadow.service.core.ErrorLogger;
import com.arm.mbed.edgex.shadow.service.core.Utils;
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;
import com.arm.mbed.edgex.shadow.service.processors.EdgeXEventProcessor;
import com.arm.mbed.edgex.shadow.service.processors.mbedClientServiceProcessor;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Main Servlet Manager
 *
 * @author Doug Anson
 */
public final class Manager {
    private HttpServlet m_servlet = null;
    private EdgeXEventProcessor m_event_processor = null;
    private mbedClientServiceProcessor m_mcs = null;
    private static volatile Manager m_manager = null;
    private ErrorLogger m_error_logger = null;
    private PreferenceManager m_preference_manager = null;

    // instance factory
    public static Manager getInstance(HttpServlet servlet,String own_ip_address,int own_port) {
        if (Manager.m_manager == null) {
            Manager.m_manager = new Manager(new ErrorLogger(),own_ip_address,own_port);
        }
        Manager.m_manager.setServlet(servlet);
        return Manager.m_manager;
    }

    // default constructor
    @SuppressWarnings("empty-statement")
    public Manager(ErrorLogger error_logger,String own_ip_address,int own_port) {
        // save the error handler
        this.m_error_logger = error_logger;
        this.m_preference_manager = new PreferenceManager(this.m_error_logger);

        // announce our self
        this.errorLogger().info("mbed EdgeX Shadow Service: Date: " + Utils.dateToString(Utils.now()));

        // configure the error logger logging level
        this.m_error_logger.configureLoggingLevel(this.m_preference_manager);
        
        // create the mCS Processor
        this.m_mcs = new mbedClientServiceProcessor(this.m_error_logger,this.m_preference_manager,own_ip_address,own_port);
        
        // add our EdgeX event processor
        this.m_event_processor = new EdgeXEventProcessor(this.m_error_logger,this.m_preference_manager,this.m_mcs);
    }
    
    // initialize the manager instance...
    public boolean initialize() {
        // initialize the mCS event processor
       if (this.m_mcs.initialize()) {
            return this.m_event_processor.initialize();
       }
        return false;
    }
    
    // closedown the manager instance
    public void closedown() {
        this.m_mcs.closedown();
        this.m_event_processor.closedown();
    }

    // process events
    public void processEvent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // let the mCS processor process the REST event
        this.m_mcs.processEvent(request, response);
    }
   
    // set the servlet
    private void setServlet(HttpServlet servlet) {
        this.m_servlet = servlet;
    }

    // get the servlet instance
    public HttpServlet getServlet() {
        return this.m_servlet;
    }

    // get the error logger
    public ErrorLogger errorLogger() {
        return this.m_error_logger;
    }

    // get the preferences db instance
    public final PreferenceManager preferences() {
        return this.m_preference_manager;
    }
}
