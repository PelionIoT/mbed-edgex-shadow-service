/**
 * @file ShadowCountStatistic.java
 * @brief Pelion bridge shadow device count statistic
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
package com.arm.pelion.shadow.service.health;

import com.arm.pelion.shadow.service.coordinator.Orchestrator;
import com.arm.pelion.shadow.service.health.interfaces.HealthCheckServiceInterface;

/**
 * This class periodically checks how many device shadows its established
 *
 * @author Doug Anson
 */
public class ShadowCountStatistic extends BaseValidatorClass implements Runnable {    
    // default constructor
    public ShadowCountStatistic(HealthCheckServiceInterface provider) {
        super(provider,"shadow_count");
        this.m_value = (Integer)0;      // Integer value for this validator
    }   
    
    // validate
    @Override
    protected void validate() {
        this.m_value = (Integer)this.checkDeviceShadowCount();
        this.updateStatisticAndNotify();
        
        // DEBUG
        this.errorLogger().info("ShadowCountStatistic: Updated device shadow count: " + (Integer)this.m_value);
    }

    // WORKER: query how many device shadows we currently have
    private int checkDeviceShadowCount() {
        Orchestrator o = this.m_provider.getOrchestrator();
        this.m_value = (Integer)o.getMbedEdgeCoreServiceProcessor().getDB().getShadowCount();
        return (int)this.m_value;
    }
}