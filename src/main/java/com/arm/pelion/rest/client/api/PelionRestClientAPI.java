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

import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
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
    
    // default constructor
    public PelionRestClientAPI(ErrorLogger logger,PreferenceManager preferences) {
        super(logger,preferences);
        
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
            this.errorLogger().warning("MbedEdgeProtocolTranslatorClientAPI: Exception in creation of URI: " + ex.getMessage());
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
    
    // get device
    public String getDevice(String deviceId) {
        // we have to use the Pelion API directly as PT does not support this...
        String url = this.createDeviceQueryURL(deviceId);
        String response =  this.m_http.httpsGetApiTokenAuth(url,this.m_pelion_api_token, null, this.m_content_type);
        
        // DEBUG
        this.errorLogger().warning("MbedEdgeProtocolTranslatorClientAPI: getDevice: ID: " + deviceId + " DETAILS: " + response);
        
        // return the response
        return response;
    }
    
    // unregister device
    public boolean unregisterDevice(String deviceId) {
        return true;
    }
    
    // register device
    public boolean registerDevice(Map device) {
        
        return true;
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
    private String createDeviceQueryURL(String device_id) {
        return this.m_pelion_device_api_base_url + "/devices/" + device_id;
    }
    
    // create the base URL for Pelion operations
    private String createBaseURL(String version) {
        return this.m_pelion_cloud_uri + this.m_pelion_api_hostname + ":" + this.m_pelion_api_port + "/v" + version;
    }
}