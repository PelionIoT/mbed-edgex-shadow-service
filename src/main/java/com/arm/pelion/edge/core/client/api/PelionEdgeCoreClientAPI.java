/**
 * @file PelionEdgeCoreClientAPI.java
 * @brief mbed-edge core client API for the EdgeX shadowing service
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
package com.arm.pelion.edge.core.client.api;

import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Pelion edge core protocol translator client API for Java
 * @author Doug Anson
 */
public class PelionEdgeCoreClientAPI extends BaseClass {    
    // mbed-edge core PT config
    private URI m_edge_core_ws_uri = null;
    private boolean m_connected = false;
    private String m_name = null;
    
    // JSON-RPC/WS 
    private JsonRpcClient m_client = null;
        
    // default constructor
    public PelionEdgeCoreClientAPI(ErrorLogger logger,PreferenceManager preferences) {
        super(logger,preferences);
        
        // set the URI for our mbed-edge instance
        try {
            this.m_edge_core_ws_uri = new URI("ws+unix:///tmp/edge.sock:/1/pt");
            this.m_name = "edgex";
        }
        catch (URISyntaxException ex) {
            this.errorLogger().warning("PelionEdgeCoreClientAPI: Exception in creation of URI: " + ex.getMessage());
        }
    }
    
    // connect to the PT
    public boolean connect() {
        try {
            if (!this.m_connected) {
                this.m_client = new JsonRpcClient();
                this.m_connected = this.register();

                // DEBUG
                this.errorLogger().warning("PelionEdgeCoreClientAPI: CONNECTED: " + this.m_edge_core_ws_uri);
            }
        }
        catch (Exception ex) {
            this.errorLogger().warning("PelionEdgeCoreClientAPI: Exception in creation of URL: " + ex.getMessage());
        }
        return this.m_connected;
    }
    
    // disconnect from the PT
    public void disconnect() {
        if (this.m_connected) {
            // mbed-edge: closedown the WS socket
        }
    }
    
    // is connected?
    public boolean isConnected() {
        // mbed-edge
        return this.m_connected;
    }
    
    // get device
    public String getDevice(String deviceId) {
        // not implemented
        return null;
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
    public boolean register() {
        return this.invokeRPC("protocol_translator_register", new Object[] {"name",this.m_name});
    }
    
    // execute RPC 
    private boolean invokeRPC(String rpc_method_name, Object[] params) {
        boolean status = false;
        
        if (this.m_connected) {
            try {
                // invoke the RPC with our params
                //this.m_client.invoke(rpc_method_name,params);
                
                // created successfully
                status = true;
                
                // DEBUG
                this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC SUCCESS: Method: " + rpc_method_name + " Params: " + params);
            } 
            catch (Throwable ex) {
                this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC FAILURE: Method: " + rpc_method_name + " Params: " + params + " Exception: " + ex.getMessage());
            }
        }
        return status;
    }
}