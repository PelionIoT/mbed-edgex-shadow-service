/*
 * @file Orchestrator.java
 * @brief Orchestrator
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
package com.arm.pelion.shadow.service.coordinator;

import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.health.HealthCheckServiceProvider;
import com.arm.pelion.shadow.service.health.interfaces.HealthStatisticListenerInterface;
import com.arm.pelion.shadow.service.json.JSONGenerator;
import com.arm.pelion.shadow.service.json.JSONGeneratorFactory;
import com.arm.pelion.shadow.service.json.JSONParser;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import com.arm.pelion.shadow.service.processors.edgex.EdgeXServiceProcessor;
import java.util.Set;
import com.arm.pelion.shadow.service.interfaces.DeviceShadowProcessorInterface;

/**
 * Orchestrator
 * @author Doug Anson
 */
public class Orchestrator extends BaseClass implements HealthStatisticListenerInterface { 
    // Default Health Check Service Provider Sleep time in MS
    private static final int DEF_HEALTH_CHECK_SERVICE_PROVIDER_SLEEP_TIME_MS = (60000 * 10);    // 10 minutes
    
    // Health Stats Key 
    public static final String HEALTH_STATS_KEY = "[HEALTH_STATS]";        
    
    // database table delimiter
    private static String DEF_TABLENAME_DELIMITER = "_";

    private DeviceShadowProcessorInterface m_msp = null;
    private EdgeXServiceProcessor m_edgex = null;
    
    // JSON support
    private JSONGeneratorFactory m_json_factory = null;
    private JSONGenerator m_json_generator = null;
    private JSONParser m_json_parser = null;
    
    // Health Check Services Provider/Manager
    private boolean m_enable_health_checks = true;                 // true: enabled, false: disabled
    private HealthCheckServiceProvider m_health_check_service_provider = null;
    private Thread m_health_check_service_provider_thread = null;
    
    // Health Check Services Provider Sleep time (in ms)
    private int m_health_check_service_provider_sleep_time_ms = DEF_HEALTH_CHECK_SERVICE_PROVIDER_SLEEP_TIME_MS;
    
    private int m_thread_count = 0;
        
    // primary constructor
    public Orchestrator(ErrorLogger error_logger, PreferenceManager preference_manager) {
        super(error_logger, preference_manager);
        
        // JSON Factory
        this.m_json_factory = JSONGeneratorFactory.getInstance();

        // create the JSON Generator
        this.m_json_generator = this.m_json_factory.newJsonGenerator();

        // create the JSON Parser
        this.m_json_parser = this.m_json_factory.newJsonParser();
        
        // Get the health check service provider sleep time
        if (this.m_enable_health_checks == true) {
            this.m_health_check_service_provider_sleep_time_ms = preferences().intValueOf("heath_check_sleep_time_ms");
            if (this.m_health_check_service_provider_sleep_time_ms <= 0) {
                this.m_health_check_service_provider_sleep_time_ms = DEF_HEALTH_CHECK_SERVICE_PROVIDER_SLEEP_TIME_MS;
            }

            // DEBUG
            this.errorLogger().warning("Orchestrator: Stats Check Sleep Interval (ms): " + this.m_health_check_service_provider_sleep_time_ms);

            // create our health check service provider and its runtime thread...
            this.m_health_check_service_provider = new HealthCheckServiceProvider(this,this.m_health_check_service_provider_sleep_time_ms); 
            this.m_health_check_service_provider.initialize();
            this.m_health_check_service_provider.addListener(this);
        }
        else {
            // not enabled
            this.errorLogger().warning("Orchestrator: Stats Checking DISABLED");
        }        
    }
    
    // set the edge core service processor
    public void setMbedEdgeCoreServiceProcessor(DeviceShadowProcessorInterface msp) {
        this.m_msp = msp;
    }
    
    // get the edge core service processor
    public DeviceShadowProcessorInterface getMbedEdgeCoreServiceProcessor() {
        return m_msp;
    }
    
    // set the EdgeX service processor
    public void setEdgeXServiceProcessor(EdgeXServiceProcessor edgex) {
        this.m_edgex = edgex;
    }
    
    // get the EdgeX service processor
    public EdgeXServiceProcessor getEdgeXServiceProcessor() {
        return this.m_edgex;
    }
    
    // initialize the manager instance...
    public boolean initialize() {
       // initialize mbed edge core processor
       if (this.m_msp.initialize()) {
           // initialize EdgeX service processor
            return this.m_edgex.initialize();
       }
       return false;
    }
    
    // closedown the manager instance
    public void closedown() {
        this.m_msp.closedown();
        this.m_edgex.closedown();
    }
    
    // get the JSON parser instance
    public JSONParser getJSONParser() {
        return this.m_json_parser;
    }

    // get the JSON generation instance
    public JSONGenerator getJSONGenerator() {
        return this.m_json_generator;
    }
    
    // start statistics gathering
    private void startStatisticsMonitoring() {
        if (this.m_enable_health_checks == true) {
            try {
                // DEBUG
                this.errorLogger().warning("Orchestrator: Statistics and health monitoring starting...");
                this.m_health_check_service_provider_thread = new Thread(this.m_health_check_service_provider);
                if (this.m_health_check_service_provider_thread != null) {
                    this.m_health_check_service_provider_thread.start();
                }
            }
            catch (Exception ex) {
                this.errorLogger().critical("Orchestrator: Exception caught while starting health check provider: " + ex.getMessage());
            }
        }
    }
    
    // manual refresh the health stats
    public void refreshHealthStats() {
        if (this.m_health_check_service_provider != null) {
            this.m_health_check_service_provider.refreshHealthStats();
        }
    }

    @Override
    public void publish(String json) {
       // dump to error logger with a KEY that the properties-editor will detect
       this.errorLogger().critical(HEALTH_STATS_KEY + json);
       
       // note in log so that log moves...
       this.errorLogger().info("Health Stats: Updated (OK).");
    }
    
    // get the active thread count
    public int getActiveThreadCount() {
        try {
            int count = 0;
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            for (Thread t : threadSet) {
                if (t.isDaemon() || t.isAlive()) {
                    ++count;
                }
            }
                        
            // add one for main...
            ++count;
            
            // record the count
            this.m_thread_count = count;
        }
        catch (Exception ex) {
            this.errorLogger().warning("Orchestrator: Exception caught while counting threads: " + ex.getMessage());
            this.m_thread_count = -1;
        }
        
        return this.m_thread_count;
    }
}
