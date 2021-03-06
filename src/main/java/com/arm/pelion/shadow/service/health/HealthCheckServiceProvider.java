/**
 * @file HealthCheckServiceProvider.java
 * @brief Health check service provider implementation for Pelion bridge
 * @author Doug Anson
 * @version 1.0
 * @see
 *
 * Copyright 2018. ARM Ltd. All rights reserved.
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
package com.arm.pelion.shadow.service.health;

import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.Utils;
import com.arm.pelion.shadow.service.health.interfaces.HealthCheckServiceInterface;
import com.arm.pelion.shadow.service.health.interfaces.HealthStatisticListenerInterface;
import com.arm.pelion.shadow.service.coordinator.Orchestrator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Service Provider Instance
 * @author Doug Anson
 */
public class HealthCheckServiceProvider extends BaseClass implements HealthCheckServiceInterface, Runnable {
    private Orchestrator m_orchestrator = null;
    private ArrayList<HealthStatisticListenerInterface> m_listeners = null;
    private HashMap<String,HealthStatistic> m_statistics = null;
    private boolean m_running = false;
    private int m_health_status_update_ms = 0;
    private ArrayList<BaseValidatorClass> m_validator_list = null;
    
    // primary constructor
    public HealthCheckServiceProvider(Orchestrator orchestrator,int health_status_update_ms) {
        super(orchestrator.errorLogger(),orchestrator.preferences());
        this.m_orchestrator = orchestrator;
        this.m_statistics = new HashMap<>();
        this.m_validator_list = new ArrayList<>();
        this.m_listeners = new ArrayList<>();
        this.m_health_status_update_ms = health_status_update_ms;
    }
    
    // add a listener
    public void addListener(HealthStatisticListenerInterface listener) {
        if (listener != null) {
            this.m_listeners.add(listener);
        }
    }

    // update a given health statistic
    @Override
    public void updateHealthStatistic(HealthStatistic statistic) {
        this.m_statistics.put(statistic.name(),statistic);
    }
   
    // initialize our stats
    @Override
    public void initialize() { 
        // Edge-Core WS validator
        this.m_validator_list.add(new EdgeCoreWSConnectionValidator(this));
        
        // Shadow Count Statistic
        this.m_validator_list.add(new ShadowCountStatistic(this));
        
        // Thread Count Statistic
        this.m_validator_list.add(new ThreadCountStatistic(this));
        
        // Mbed Edge Core Service Health Statistic
        this.m_validator_list.add(new MbedEdgeCoreServiceHealthStatistic(this));
        
        // JVM Statistics
        this.m_validator_list.add(new MemoryStatistic(this,"total","MB"));
        this.m_validator_list.add(new MemoryStatistic(this,"free","MB"));
        this.m_validator_list.add(new MemoryStatistic(this,"used","MB"));
        this.m_validator_list.add(new MemoryStatistic(this,"max","MB"));
        this.m_validator_list.add(new MemoryStatistic(this,"processors","Processor(s)"));
        
        // ADD other validators here...
        
        // Run all..
        for(int i=0;i<this.m_validator_list.size();++i) {
            Thread t = new Thread(this.m_validator_list.get(i));
            t.start();
        }
    }
    
    // create a JSON output of the stats
    @Override
    public String statisticsJSON() {
        return this.m_orchestrator.getJSONGenerator().generateJson(this.createStatisticsJSON());
    }
    
    // create a JSON output of the stat descriptons 
    @Override
    public String descriptionsJSON() {
        return this.m_orchestrator.getJSONGenerator().generateJson(this.createDescriptonJSON());
    }
    
    // get the current time (formatted) 
    private String getCurrentFormattedTime() {
        // RFC 3339 formatted date
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date());
    }
    
    // create a map of key,value pairs 
    private Map createStatisticsJSON() {
        HashMap<String,Object> stats = new HashMap<>();
        for (Map.Entry<String, HealthStatistic> entry : this.m_statistics.entrySet()) {
            String name = entry.getKey();
            HealthStatistic statistic = entry.getValue();
            stats.put(name,statistic.value());
        }
        
        // add a timestamp
        stats.put("timestamp",(String)this.getCurrentFormattedTime());
        
        // Create the descriptions Map
        Map descriptions = this.createDescriptonJSON();
        
        // add a timestamp to the descriptions
        descriptions.put("timestamp","Recorded Date/Time");
        
        // add the descriptions 
        stats.put("descriptions",descriptions);
        
        // return the status
        return (Map)stats;
    }
    
    // create a map of key,description pairs 
    private Map createDescriptonJSON() {
        HashMap<String,String> descriptions = new HashMap<>();
        for (Map.Entry<String, HealthStatistic> entry : this.m_statistics.entrySet()) {
            String name = entry.getKey();
            HealthStatistic statistic = entry.getValue();
            descriptions.put(name,statistic.description());
        }
        return (Map)descriptions;
    }
    
    // manually update the health stats
    public void refreshHealthStats() {
        this.checkAndPublish();
    }
    
    // check and publish changes to listeners
    private void checkAndPublish() {
        String json = this.statisticsJSON();
        for(int i=0;i<this.m_listeners.size();++i) {
            this.m_listeners.get(i).publish(json);
        }
    }

    // run statistics loop
    @Override
    public void run() {
        this.m_running = true;
        this.healthStatsUpdateLoop();
    }
    
    // halt 
    public void halt() {
        this.m_running = false;
    }
    
    // main health statistics update loop
    private void healthStatsUpdateLoop() {
        while (this.m_running == true) {
            // validate the webhook
            this.checkAndPublish();

            // sleep for a bit...
            Utils.waitForABit(this.errorLogger(),this.m_health_status_update_ms);
        }
    }
    
    // get the orchestrator
    @Override
    public Orchestrator getOrchestrator() {
        return this.m_orchestrator;
    }
}