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
package com.arm.pelion.shadow.service.servlet;

import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.core.Utils;
import com.arm.pelion.shadow.service.coordinator.Orchestrator;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import com.arm.pelion.shadow.service.processors.pelion.PelionShadowServiceProcessor;
import com.arm.pelion.shadow.service.processors.edgex.EdgeXServiceProcessor;
import com.arm.pelion.shadow.service.interfaces.DeviceShadowProcessorInterface;

/**
 * Main Servlet Manager
 *
 * @author Doug Anson
 */
public final class Manager {
    private Orchestrator m_orchestrator = null;
    private ErrorLogger m_error_logger = null;
    private PreferenceManager m_preference_manager = null;

    // default constructor
    @SuppressWarnings("empty-statement")
    public Manager(ErrorLogger error_logger,PreferenceManager preferences) {
        // save the error handler
        this.m_error_logger = error_logger;
        this.m_preference_manager = preferences;

        // announce our self
        this.errorLogger().warning("mbed EdgeX Shadow Service: Date: " + Utils.dateToString(Utils.now()));

        // configure the error logger logging level
        this.m_error_logger.configureLoggingLevel(this.m_preference_manager);
        
        // create the orchestrator
        this.m_orchestrator = new Orchestrator(this.m_error_logger,this.m_preference_manager);
        
        // create the mbed shadow service Processor
        DeviceShadowProcessorInterface msp = new PelionShadowServiceProcessor(this.m_error_logger,this.m_preference_manager,this.m_orchestrator);
        
        // add our EdgeX event processor
        EdgeXServiceProcessor edgex = new EdgeXServiceProcessor(this.m_error_logger,this.m_preference_manager,msp);
        
        // bind in orchestrator
        this.m_orchestrator.setMbedEdgeCoreServiceProcessor(msp);
        this.m_orchestrator.setEdgeXServiceProcessor(edgex);
    }

    // get the error logger
    public ErrorLogger errorLogger() {
        return this.m_error_logger;
    }

    // get the preferences db instance
    public final PreferenceManager preferences() {
        return this.m_preference_manager;
    }
    
    // initialize
    public boolean initialize() {
       return this.m_orchestrator.initialize();
    }
    
    // closedown
    public void closedown() {
        this.m_orchestrator.closedown();
    }
    
    // refresh the health statistics
    public void refreshHealthStats() {
        this.m_orchestrator.refreshHealthStats();
    }
    
    // validate underlying connections
    public void validateUnderlyingConnection() {
        this.m_orchestrator.validateUnderlyingConnection();
    }
}
