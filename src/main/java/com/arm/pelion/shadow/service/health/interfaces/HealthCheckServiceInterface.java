/**
 * @file HealthCheckServiceInterface.java
 * @brief HealthCheck Service Provider Interface for pelion-bridge
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
package com.arm.pelion.shadow.service.health.interfaces;

import com.arm.pelion.shadow.service.coordinator.Orchestrator;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.health.HealthStatistic;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;


/**
 * Health Check Service Interface
 * @author Doug Anson
 */
public interface HealthCheckServiceInterface {
    // update a halth status statistic
    public void updateHealthStatistic(HealthStatistic statistic);
    
    // initialize the stats
    public void initialize();
    
    // get the error logger
    public ErrorLogger errorLogger();
    
    // get the preferences
    public PreferenceManager preferences();
    
    // statistics JSON
    public String statisticsJSON();
    
    // descriptions JSON
    public String descriptionsJSON();
    
    // get the orchestrator
    public Orchestrator getOrchestrator();
}