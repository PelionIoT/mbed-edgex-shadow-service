/**
 * @file MbedEdgeProtocolTranslatorClientAPI.java
 * @brief Mbed edge core protocol translator client API for Java
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
package com.arm.pelion.api;

import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import com.arm.pelion.rest.client.api.PelionRestClientAPI;
import com.arm.pelion.edge.core.client.api.PelionEdgeCoreClientAPI;
import java.util.Map;

/**
 * Mbed edge core protocol translator client API for Java
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
    
    // has the pelion API key been set?
    private boolean apiKeySet() {
        if (this.m_api_key != null && this.m_api_key.contains("ak_") == true) {
            // set
            return true;
        }
        return false;
    }
    
    // connect to the PT
    public boolean connect() {
        if (this.apiKeySet() == true) {
            if (m_use_edge == true) {
                // mbed-edge
                return this.m_edge_api.connect();
            }
            else {
                // Pelion API
                return this.m_pelion_api.connect();
            }
        }
        else {
            // no key set
            this.errorLogger().warning("PelionDeviceAPI: Not connecting yet. No Pelion API Key has been set yet (OK).");
        }
        return false;
    }
    
    // disconnect from the PT
    public void disconnect() {
        if (this.apiKeySet() == true) {
            if (m_use_edge == true) {
                // mbed-edge
                this.m_edge_api.disconnect();
            }
            else {
                // Pelion API
                this.m_pelion_api.disconnect();
            }
        }
    }
    
    // is connected?
    public boolean isConnected() {
        if (this.apiKeySet() == true) {
        if (m_use_edge == true) {
            // mbed-edge
            return this.m_edge_api.isConnected();
        }
        else {
            // Pelion API
            return this.m_pelion_api.isConnected();
        }
        }
        return false;
    }
    
    // get device
    public String getDevice(String deviceId) {
        if (this.apiKeySet() == true) {
            if (m_use_edge == true) {
                // mbed-edge: use Pelion Rest API
                return this.m_pelion_api.getDevice(deviceId);
            }
            else {
                // Pelion API
                return this.m_pelion_api.getDevice(deviceId);
            }
        }
        return null;
    }
    
    // unregister device
    public boolean unregisterDevice(String deviceId) {
        if (this.apiKeySet() == true) {
            if (m_use_edge == true) {
                // mbed-edge
                return this.m_edge_api.unregisterDevice(deviceId);
            }
            else {
                // Pelion API
                return this.m_pelion_api.unregisterDevice(deviceId);
            }
        }
        return false;
    }
    
    // register device
    public boolean registerDevice(Map device) {
        if (this.apiKeySet() == true) {
            if (m_use_edge == true) {
                // mbed-edge
                return this.m_edge_api.registerDevice(this.toMbedEdgeCorePTFormat(device));
            }
            else {
                // Pelion API
                return this.m_pelion_api.registerDevice(device);
            }
        }
        return false;
    }
    
    // register our API
    private boolean register() {
        if (this.apiKeySet() == true) {
            if (m_use_edge == true) {
                // mbed-edge
                return this.m_edge_api.register();
            }
            else {
                // Pelion API
                return this.m_pelion_api.register();
            }
        }
        return false;
    }
    
    // convert to the mbed-edge core format
    private Object[] toMbedEdgeCorePTFormat(Map device) {
        return null;
    }
}
