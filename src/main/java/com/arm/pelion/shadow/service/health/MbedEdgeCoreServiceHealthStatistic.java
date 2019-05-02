/**
 * @file MbedEdgeCoreServiceHealthStatistic.java
 * @brief Mbed Edge Core Health Status 
 * @author Doug Anson
 * @version 1.0
 * @see
 *
 * Copyright 2019. ARM Ltd. All rights reserved.
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

import com.arm.pelion.shadow.service.core.Utils;
import com.arm.pelion.shadow.service.health.interfaces.HealthCheckServiceInterface;
import java.io.IOException;

/**
 * Mbed Edge Core Service Health Status Checker
 * @author Doug Anson
 */
public class MbedEdgeCoreServiceHealthStatistic extends BaseValidatorClass implements Runnable {
    // primary constructor
    public MbedEdgeCoreServiceHealthStatistic(HealthCheckServiceInterface provider) {
        super(provider,"mbed_edge_core");
        this.m_value = (String)"Down";
    }
    
    // execute script
    private String check() {
        // construct the arguments
        String cmd = "./scripts/mbed_edge_running.sh";
        String response = null;
        String error = null;

        try {
            // invoke the AWS CLI
            Process proc = Runtime.getRuntime().exec(cmd);
            response = Utils.convertStreamToString(proc.getInputStream());
            error = Utils.convertStreamToString(proc.getErrorStream());

            // wait to completion
            proc.waitFor();
            int status = proc.exitValue();

            // DEBUG
            if (status != 0) {
                // non-zero exit status
                this.errorLogger().info("MbedEdgeCore: Invoked: " + cmd);
                this.errorLogger().info("MbedEdgeCore: Response: " + response);
                this.errorLogger().info("MbedEdgeCore: Error: " + error);
                this.errorLogger().info("MbedEdgeCore: Exit Code: " + status);
            }
            else {
                // successful exit status
                this.errorLogger().info("MbedEdgeCore: Invoked: " + cmd);
                this.errorLogger().info("MbedEdgeCore: Response: " + response);
                this.errorLogger().info("MbedEdgeCore: Exit Code: " + status);
            }
        }
        catch (IOException | InterruptedException ex) {
            this.errorLogger().warning("MbedEdgeCore: Exception for command: " + cmd, ex);
            response = null;
        }

        // return the resposne
        return response;
    }
    
    // update the mbed edge core status
    private void updateMbedEdgeCoreStatus() {
        // execute the script to check the latest status
        String status = this.check().trim();
        
        //DEBUG
        this.errorLogger().info("MbedEdgeCore: " + status);
        
        // set the status
        if (status != null && status.equalsIgnoreCase("yes")) {
            // running
            this.m_value = (String)"Running";
        }
        else {
            // not running
            this.m_value = (String)"Down";
        }
    }

    @Override
    protected void validate() {
        // get the latest mbed edge core status
        this.updateMbedEdgeCoreStatus();
        
        // update the statistic and notify...
        this.updateStatisticAndNotify();
        
        // DEBUG
        this.errorLogger().info("MbedEdgeCore: Service Running: " + (String)this.m_value);
    }
}
