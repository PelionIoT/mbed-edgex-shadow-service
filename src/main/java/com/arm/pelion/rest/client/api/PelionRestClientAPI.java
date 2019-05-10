/**
 * @file PelionRestClientAPI.java
 * @brief Pelion Rest API Client
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
package com.arm.pelion.rest.client.api;

import com.arm.pelion.shadow.service.coordinator.Orchestrator;
import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import java.util.List;
import java.util.Map;

/**
 * Pelion Rest API Client
 * @author Doug Anson
 */
public class PelionRestClientAPI extends BaseClass {
    private String m_pelion_api_hostname = null;
    private int m_pelion_api_port = 0;
    private String m_connect_api_version = "2";
    private String m_device_api_version = "3";
    private String m_pelion_cloud_uri = "https://";
    private String m_pelion_device_api_base_url = null;
    private String m_pelion_connect_api_base_url = null;
    private String m_pelion_api_token = null;
    
     // Orchestrator
    private Orchestrator m_orchestrator = null;
    
    // default constructor
    public PelionRestClientAPI(ErrorLogger logger,PreferenceManager preferences,Orchestrator orchestrator) {
        super(logger,preferences);
        this.m_orchestrator = orchestrator;
        
        // set the URI for our mbed-edge instance
        try {
            // Pelion API Setup
            this.m_pelion_api_token = this.preferences().valueOf("api_key");
            this.m_pelion_api_hostname = this.preferences().valueOf("mds_address");
            if (this.m_pelion_api_hostname == null || this.m_pelion_api_hostname.length() == 0) {
                this.m_pelion_api_hostname = this.preferences().valueOf("api_endpoint_address");
            }
            this.m_pelion_api_port = this.preferences().intValueOf("mds_port");
            if (this.m_pelion_api_port <= 0) {
                this.m_pelion_api_port = 443;
            }
            this.m_pelion_device_api_base_url = this.createBaseURL(this.m_device_api_version);
            this.m_pelion_connect_api_base_url = this.createBaseURL(this.m_connect_api_version);
            
        }
        catch (Exception ex) {
            this.errorLogger().warning("PelionRestClientAPI: Exception in creation of URI: " + ex.getMessage());
        }
    }
    
    // initialize
    public void initialize() {
        
    }
    
    // connect to the PT
    public boolean connect() {
        // not used
        return true;
    }
    
    // disconnect from the PT
    public void disconnect() {
        // not used
    }
    
    // is connected?
    public boolean isConnected() {
        // not used
        return true;
    }
    
    // register
    public boolean register() {
        // not used
        return true;
    }
    
    // get the device via endpoint name lookup
    public String getDeviceFromEndpointName(String ep) { 
        String device_json = "{}";
        
        // get the device filtered by endpoint_name
        String url = createDeviceQueryURLWithEndpointName(ep);
        
        // get the JSON for the device
        String json = this.m_http.httpsGetApiTokenAuth(url, this.m_pelion_api_token, null, this.m_content_type);
        
        // parse and take only the first record (there should be ONLY one...)
        Map result = this.m_orchestrator.getJSONParser().parseJson(json);
        List list = (List)result.get("data");
        if (list != null && list.size() > 0) {
           device_json = this.m_orchestrator.getJSONGenerator().generateJson((Map)list.get(0));
        }
        
        // DEBUG
        this.errorLogger().info("PelionRestClientAPI: EP: " + ep + " URL: " + url + " JSON: " + device_json);
        
        // return the device json
        return device_json;
    }
    
    // get device
    public String getDevice(String id) {
        String url = this.createDeviceQueryURL(id);
        String response =  this.m_http.httpsGetApiTokenAuth(url, this.m_pelion_api_token, null, this.m_content_type);
        
        // DEBUG
        this.errorLogger().info("PelionRestClientAPI: getDevice: ID: " + id + " URL: " + url + " DETAILS: " + response);
        
        // return the response
        return response;
    }
    
    // unregister device
    public boolean unregisterDevice(String deviceId) {
        return true;
    }
    
    // dispatch an observation
    public boolean sendObservation(String mbed_id,String uri,Object value) {
        // DEBUG
        this.errorLogger().warning("PelionRestClientAPI: sendObservation: mbed ID: " + mbed_id + " URI: " + uri + " VALUE: " + value);
        return true;
    }
    
    // register device
    public Map registerDevice(Map device) {
        return device;
    }
    
    // create the device creation URL
    private String createDeviceCreationURL() {
        return this.m_pelion_device_api_base_url + "/devices";
    }
    
    // create the device deletion URL
    private String createDeviceDeleteURL(String device_id) {
        return this.m_pelion_device_api_base_url + "/devices/" + device_id;
    }
    
    // create the device get-resource URL
    private String createDeviceGetResourceURL(String device_id,String uri) {
        return this.m_pelion_connect_api_base_url + "/endpoints/" + device_id + uri;
    }
    
    // create the device update-resource URL
    private String createDeviceUpdateResourceURL(String device_id,String uri) {
        return this.m_pelion_connect_api_base_url + "/endpoints/" + device_id + uri;
    }
    
    // create the device query URL
    private String createDeviceQueryURL(String id) {
        return this.m_pelion_device_api_base_url + "/devices/" + id;
    }
    
    // create the device query URL (all devices)
    private String createDeviceQueryURLWithEndpointName(String ep) {
        return this.m_pelion_device_api_base_url + "/devices?filter=endpoint_name%3D" + ep;
    }
    
    // create the base URL for Pelion operations
    private String createBaseURL(String version) {
        return this.m_pelion_cloud_uri + this.m_pelion_api_hostname + ":" + this.m_pelion_api_port + "/v" + version;
    }
}