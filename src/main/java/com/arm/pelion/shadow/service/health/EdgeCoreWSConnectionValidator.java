/**
 * @file EdgeCoreWSConnectionValidator.java
 * @brief Edge-core WS connection validation
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
 * This class periodically checks our edge-core WS connection
 *
 * @author Doug Anson
 */
public class EdgeCoreWSConnectionValidator extends BaseValidatorClass implements Runnable {
    // default constructor
    public EdgeCoreWSConnectionValidator(HealthCheckServiceInterface provider) {
        super(provider,"database");
        this.m_value = (Boolean)false;      // boolean value for this validator
    }   
    
    // validate
    @Override
    protected void validate() {
        // DEBUG
        this.errorLogger().info("EdgeCoreWSConnectionValidator: Validating Edge-Core WS Connections...");

        // validate the edge-core WS connections
        if (this.validateConnections() == true) {
            // DEBUG
            this.errorLogger().info("EdgeCoreWSConnectionValidator: Edge-Core WS connections OK.");
            this.m_value = (Boolean)true;
        }
        else {
            // DEBUG
            this.errorLogger().info("EdgeCoreWSConnectionValidator: Edge-Core WS connections are DOWN.");
            this.m_value = (Boolean)false;
        }
        
        // update our stats and notify if changed
        this.updateStatisticAndNotify();
    }

    // WORKER: validate the edge-core WS Connections
    private boolean validateConnections() {
        try {
            Orchestrator o = this.m_provider.getOrchestrator();
            return o.getMbedEdgeCoreServiceProcessor().validateUnderlyingConnection();
        }
        catch (Exception ex) {
            return false;
        }
    }
}