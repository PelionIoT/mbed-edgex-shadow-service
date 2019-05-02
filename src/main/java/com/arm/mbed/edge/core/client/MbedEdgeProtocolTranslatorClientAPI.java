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
package com.arm.mbed.edge.core.client;

import com.arm.mbed.edgex.shadow.service.core.BaseClass;
import com.arm.mbed.edgex.shadow.service.core.ErrorLogger;
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Mbed edge core protocol translator client API for Java
 * @author Doug Anson
 */
public class MbedEdgeProtocolTranslatorClientAPI extends BaseClass {
    private URI m_edge_core_ws_uri = null;
    private boolean m_connected = false;
    private String m_name = null;
    
    // Pelion attributes
    private String m_pelion_api_hostname = null;
    private int m_pelion_api_port = 0;
    private String m_connect_api_version = "2";
    private String m_device_api_version = "3";
    private String m_pelion_cloud_uri = "https://";
    private String m_pelion_device_api_base_url = null;
    private String m_pelion_connect_api_base_url = null;
    private String m_pelion_api_token = null;
    
    // JSON-RPC/WS 
    private JsonRpcHttpClient m_client = null;
    
    
    // default constructor
    public MbedEdgeProtocolTranslatorClientAPI(ErrorLogger logger,PreferenceManager preferences) {
        super(logger,preferences);
        
        // set the URI for our mbed-edge instance
        try {
            this.m_edge_core_ws_uri = new URI("ws+unix:///tmp/edge.sock:/1/pt");
            this.m_name = "edgex";
            
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
        catch (URISyntaxException ex) {
            this.errorLogger().warning("MbedEdgeProtocolTranslatorClientAPI: Exception in creation of URI: " + ex.getMessage());
        }
    }
    
    // connect to the PT
    public boolean connect() {
        try {
            if (!this.m_connected) {
                this.m_client = new JsonRpcHttpClient(this.m_edge_core_ws_uri.toURL());
                this.m_connected = this.register();
            }
        }
        catch (MalformedURLException ex) {
            this.errorLogger().warning("MbedEdgeProtocolTranslatorClientAPI: Exception in creation of URL: " + ex.getMessage());
        }
        return this.m_connected;
    }
    
    // disconnect from the PT
    public void disconnect() {
        if (this.m_connected) {
            
        }
    }
    
    // is connected?
    public boolean isConnected() {
        return this.m_connected;
    }
    
    // get device
    public String getDevice(String deviceId) {
        // we have to use the Pelion API directly as PT does not support this...
        String url = this.createDeviceQueryURL(deviceId);
        return this.m_http.httpsGetApiTokenAuth(url,this.m_pelion_api_token, null, this.m_content_type);
    }
    
    // unregister device
    public boolean unregisterDevice(String deviceId) {
        return this.invokeRPC("device_unregister",new Object[] {"deviceId",deviceId});
    }
    
    // register device
    public boolean registerDevice(Object[] device) {
        return this.invokeRPC("device_register",device);
    }
    
    // register our API
    private boolean register() {
        return this.invokeRPC("protocol_translator_register", new Object[] {"name",this.m_name});
    }
    
    // create the device query URL
    private String createDeviceQueryURL(String device_id) {
        return this.m_pelion_device_api_base_url + "/devices/" + device_id;
    }
    
    // create the base URL for Pelion operations
    private String createBaseURL(String version) {
        return this.m_pelion_cloud_uri + this.m_pelion_api_hostname + ":" + this.m_pelion_api_port + "/v" + version;
    }
    
    // execute RPC 
    private boolean invokeRPC(String rpc_method_name, Object[] params) {
        boolean status = false;
        
        if (this.m_connected) {
            try {
                // invoke the RPC with our params
                this.m_client.invoke(rpc_method_name,params);
                
                // created successfully
                status = true;
                
                // DEBUG
                this.errorLogger().warning("MbedEdgeProtocolTranslatorClientAPI: RPC SUCCESS: Method: " + rpc_method_name + " Params: " + params);
            } 
            catch (Throwable ex) {
                this.errorLogger().warning("MbedEdgeProtocolTranslatorClientAPI: RPC FAILURE: Method: " + rpc_method_name + " Params: " + params + " Exception: " + ex.getMessage());
            }
        }
        return status;
    }
}
