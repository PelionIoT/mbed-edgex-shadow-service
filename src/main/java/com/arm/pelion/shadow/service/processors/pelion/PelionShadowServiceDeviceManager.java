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
    
    // default constructor
    public PelionShadowServiceDeviceManager(ErrorLogger error_logger, PreferenceManager preference_manager, DeviceResourceManagerInterface device_manager) {
        super(error_logger, preference_manager);
        this.m_device_manager = device_manager;
        this.m_api = new PelionDeviceAPI(error_logger,preference_manager);
    }
    
    // validate the underlying connection
    public void validateUnderlyingConnection() {
        this.connect();
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
    public boolean deviceExists(String mbed_id) {
        boolean exists = false;
        
        // if the device ID is null... assume it does NOT exist yet.
        if (mbed_id == null) {
            // does not exist
            this.errorLogger().warning("PelionShadowServiceDeviceManager: deviceExists: ID: <empty>. Device does NOT exist (OK).");
        }
        else{
            // get the device details...
            String json = this.m_api.getDevice(mbed_id);
            
            // DEBUG
            this.errorLogger().warning("PelionShadowServiceDeviceManager: deviceExists: ID: " + mbed_id + " DETAILS: " + json);
            
            // XXXX
        }
        
        // return existance
        return exists;
    }
    
    // create the device
    public boolean createDevice(Map device) {
        boolean created = false;
        
        // get the device ID first
        String mbed_id = (String)device.get("id");
        if (this.deviceExists(mbed_id) == false) {
            // Create the device
            this.errorLogger().warning("PelionShadowServiceDeviceManager: createDevice: " + device);
            created = this.m_api.registerDevice(device);
            if (created) {
                // success!
                this.errorLogger().warning("PelionShadowServiceDeviceManager: createDevice: SUCCESS");
            }
            else {
                // failure
                this.errorLogger().warning("PelionShadowServiceDeviceManager: createDevice: FAILURE");
            }
        }
        else {
            // already exists
            this.errorLogger().warning("PelionShadowServiceDeviceManager: Pelion device: " + mbed_id + " already exists (OK)");
            created = true;
        }
        
        // return status
        return created;
    }
    
    // delete the device
    public boolean deleteDevice(String mbed_id) {
        boolean deleted = false;
        
        // only delete devices that exist
        if (this.deviceExists(mbed_id) == true) {
            // delete the device
            deleted = this.m_api.unregisterDevice(mbed_id);
            
            // DEBUG
            if (deleted) {
                // success
                this.errorLogger().warning("PelionShadowServiceDeviceManager: deleteDevice: " + mbed_id + " SUCCESSFUL");
            }
            else {
                // success
                this.errorLogger().warning("PelionShadowServiceDeviceManager: deleteDevice: " + mbed_id + " FAILURE");
            }
        }
        else {
            // already exists
            this.errorLogger().warning("PelionShadowServiceDeviceManager: Pelion device: " + mbed_id + " does not exist... so already deleted.");
            deleted = true;
        }
        
        // return status
        return deleted;
    }
    
    // direct Pelion to create a device resource observation
    public void processDeviceObservation(String mbed_id,String uri,Object value) {
        // XXX update the device shadow and create an observation event
        this.errorLogger().warning("PelionShadowServiceDeviceManager: STUB: processDeviceObservation: mbed ID: " + mbed_id + " URI: " + uri + " VALUE: " + value);
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
    
}
