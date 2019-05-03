/**
 * @file PelionDeviceAPI.java
 * @brief Pelion client API for Java (container)
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
package com.arm.pelion.api;

import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import com.arm.pelion.rest.client.api.PelionRestClientAPI;
import com.arm.pelion.edge.core.client.api.PelionEdgeCoreClientAPI;
import java.util.Map;

/**
 * Pelion client API for Java (container)
 * @author Doug Anson
 */
public class PelionDeviceAPI extends BaseClass {
    // use mbed-edge core or Pelion API?
    private static final boolean m_use_edge = true;    // true - use edge, false - use pelion rest API
    
    // Pelion Edge Core Client API Support
    private PelionEdgeCoreClientAPI m_edge_api = null;
    
    // Pelion Rest API Support
    private PelionRestClientAPI m_pelion_api = null;
    
    // Pelion API Key
    private String m_api_key = null;
    
    // default constructor
    public PelionDeviceAPI(ErrorLogger logger,PreferenceManager preferences) {
        super(logger,preferences);
        
        // get the Pelion API Key
        this.m_api_key = this.prefValue("api_key");
        
        // Pelion Edge Core Client API Support
        this.m_edge_api = new PelionEdgeCoreClientAPI(logger,preferences);
        
        // Pelion API
        this.m_pelion_api = new PelionRestClientAPI(logger,preferences);
        this.m_pelion_api.initialize();
    }
    
    // is the pelion API key set?
    private boolean apiKeySet() {
        if (this.m_api_key != null && this.m_api_key.contains("ak_") == true) {
            // api key is set...
            return true;
        }
        return false;
    }
    
    // is the mbed edge runtime operational
    private boolean mbedEdgeRunning() {
        return this.preferences().mbedEdgeRunning();
    }
    
    // connect to the PT
    public boolean connect() {
        if (m_use_edge == true) {
            if (this.mbedEdgeRunning() == true) {
                // mbed-edge
                boolean status = this.m_edge_api.connect();
                if (status) {
                    this.register();
                }
            }
            return false;
        }
        else {
            if (this.apiKeySet() == true) {
                // Pelion API
                boolean status = this.m_pelion_api.connect();
                if (status) {
                    this.register();
                }
            }
            return false;
        }
    }
    
    // disconnect from the PT
    public void disconnect() {
        if (m_use_edge == true) {
            if (this.mbedEdgeRunning() == true) {
                // mbed-edge
                this.m_edge_api.disconnect();
            }
        }
        else {
            if (this.apiKeySet() == true) {
                // Pelion API
                this.m_pelion_api.disconnect();
            }
        }
    }
    
    // is connected?
    public boolean isConnected() {
        if (m_use_edge == true) {
            if (this.mbedEdgeRunning() == true) {
                // mbed-edge
                return this.m_edge_api.isConnected();
            }
            return false;
        }
        else {
            if (this.apiKeySet() == true) {
                // Pelion API
                return this.m_pelion_api.isConnected();
            }
            return false;
        }
    }
    
    // get device
    public String getDevice(String deviceId) {
        if (m_use_edge == true) {
            if (this.apiKeySet() == true) {
                // mbed-edge: use Pelion Rest API
                return this.m_pelion_api.getDevice(deviceId);
            }
        }
        else {
            if (this.apiKeySet() == true) {
                // Pelion API
                return this.m_pelion_api.getDevice(deviceId);
            }
        }
        return null;
    }
    
    // unregister device
    public boolean unregisterDevice(String deviceId) {
        if (m_use_edge == true) {
            if (this.mbedEdgeRunning() == true) {
                // mbed-edge
                return this.m_edge_api.unregisterDevice(deviceId);
            }
            return false;
        }
        else {
            if (this.apiKeySet() == true) {
                // Pelion API
                return this.m_pelion_api.unregisterDevice(deviceId);
            }
            return false;
        }
    }
    
    // register device
    public boolean registerDevice(Map device) {
        if (m_use_edge == true) {
            if (this.mbedEdgeRunning() == true) {
                // mbed-edge
                return this.m_edge_api.registerDevice(device);
            }
            return false;  
        }
        else {
            if (this.apiKeySet() == true) {
                // Pelion API
                return this.m_pelion_api.registerDevice(device);
            }
            return false;
        }
    }
    
    // register our API
    private boolean register() {
        if (m_use_edge == true) {
            if (this.mbedEdgeRunning() == true) {
                // mbed-edge
                return this.m_edge_api.register();
            }
            return false;
        }
        else {
            if (this.apiKeySet() == true) {
                // Pelion API
                return this.m_pelion_api.register();
            }
            return false;
        }
    }
}
