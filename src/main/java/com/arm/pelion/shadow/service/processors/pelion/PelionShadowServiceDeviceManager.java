/**
 * @file PelionShadowServiceDeviceManager.java
 * @brief Pelion device shadow service device manager
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
package com.arm.pelion.shadow.service.processors.pelion;

import com.arm.pelion.api.PelionDeviceAPI;
import com.arm.pelion.edge.core.client.api.PelionEdgeCoreClientAPI;
import com.arm.pelion.rest.client.api.PelionRestClientAPI;
import com.arm.pelion.shadow.service.coordinator.Orchestrator;
import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.interfaces.DeviceResourceManagerInterface;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import java.util.Map;

/**
 * Pelion device shadow service device manager
 * @author Doug Anson
 */
public class PelionShadowServiceDeviceManager extends BaseClass implements Runnable {
    private DeviceResourceManagerInterface m_device_manager = null;
    private PelionDeviceAPI m_api = null;
    private Orchestrator m_orchestrator = null;
    
    // default constructor
    public PelionShadowServiceDeviceManager(ErrorLogger error_logger, PreferenceManager preference_manager, DeviceResourceManagerInterface device_manager,Orchestrator orchestrator) {
        super(error_logger, preference_manager);
        this.m_device_manager = device_manager;
        this.m_orchestrator = orchestrator;
        this.m_api = new PelionDeviceAPI(error_logger,preference_manager,orchestrator);
    }
    
    // validate the underlying connection
    public boolean validateUnderlyingConnection() {
        return this.connect();
    }
    
    // connect the API
    private boolean connect() {
        boolean connected = this.isConnected();
        if (connected == false) {
            return this.m_api.connect();
        }
        return connected;
    }
    
    // is connected?
    private boolean isConnected() {
        return this.m_api.isConnected();
    }
    
    // closedown 
    public void closedown() {
        this.m_api.disconnect();
    }
    
    // device exists in Pelion?
    public Map deviceExists(String ep) {        
        // if the device ID is null... assume it does NOT exist yet.
        if (ep == null) {
            // does not exist
            this.errorLogger().info("PelionShadowServiceDeviceManager: deviceExists: EP: <empty>. ERROR: no endpoint name given");
            return null;
        }
        else{
            // get the device details...
            String device = this.m_api.getDevice(ep);
            if (device != null) {
                // DEBUG
                this.errorLogger().info("PelionShadowServiceDeviceManager: deviceExists: EP: " + ep + " DEVICE: " + device);
                return this.m_orchestrator.getJSONParser().parseJson(device);
                
            }
            else {
                // DEBUG
                this.errorLogger().info("PelionShadowServiceDeviceManager: deviceExists: EP: " + ep + " DETAILS: <empty>. Device does NOT exist (OK).");
                return null;
            }
        }
    }
    
    // create the device
    public Map createDevice(Map device) {
        if (this.m_api.isConnected() == true) {
            // DEBUG
            this.errorLogger().info("PelionShadowServiceDeviceManager: createDevice: " + device);

            // register the device
            device = this.m_api.registerDevice(device);
            if (device != null) {
                // success!
                this.errorLogger().info("PelionShadowServiceDeviceManager: createDevice: SUCCESS: " + device);
            }
            else {
                // failure
                this.errorLogger().warning("PelionShadowServiceDeviceManager: createDevice: FAILURE: " + device);
                return null;
            }
        }
        else {
            // no connection
            this.errorLogger().warning("PelionShadowServiceDeviceManager: API not connected yet. Unable to create shadow...");
            return null;
        }
       
        // return the device
        return device;
    }
    
    // delete the device
    public boolean deleteDevice(String mbed_id) {
        boolean deleted = this.m_api.unregisterDevice(mbed_id);

        // DEBUG
        if (deleted) {
            // success
            this.errorLogger().info("PelionShadowServiceDeviceManager: deleteDevice: " + mbed_id + " SUCCESSFUL");
        }
        else {
            // success
            this.errorLogger().warning("PelionShadowServiceDeviceManager: deleteDevice: " + mbed_id + " FAILURE");
        }
        
        // return status
        return deleted;
    }
    
    // direct Pelion to create a device resource observation
    public boolean processDeviceObservation(String mbed_id,String ep,String uri,Object value) {
        return this.m_api.sendObservation(mbed_id,ep,uri,value);
    }
    
    // callback to process a device resource "get" request
    private String processDeviceResourceValueRequest(String mbed_id, String mbed_resource_uri, Object new_value) {        
        return this.m_device_manager.getDeviceResource(mbed_id, mbed_resource_uri, new_value);
    }
    
    // callback to process a device resource "put" request
    public String updateDeviceResource(String mbed_id, String mbed_resource_uri, Object new_value) {        
        return this.m_device_manager.updateDeviceResource(mbed_id, mbed_resource_uri, new_value);
    }

    // Runnable
    @Override
    public void run() {
    }
    
    // get the Pelion Rest Client API
    public PelionRestClientAPI getPelionRestClientAPI() {
        return this.m_api.getPelionRestClientAPI();
    }
    
    // get the Pelion edge core client API
    public PelionEdgeCoreClientAPI getPelionEdgeCoreClientAPI() {
        return this.m_api.getPelionEdgeCoreClientAPI();
    }
}
