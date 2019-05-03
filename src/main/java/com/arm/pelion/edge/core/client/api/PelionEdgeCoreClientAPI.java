/**
 * @file PelionEdgeCoreClientAPI.java
 * @brief mbed-edge core client API for the EdgeX shadowing service
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
package com.arm.pelion.edge.core.client.api;

import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import com.google.gson.JsonElement;
import java.io.IOException;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientNettyWebSocket;

/**
 * Pelion edge core protocol translator client API for Java
 * @author Doug Anson
 */
public class PelionEdgeCoreClientAPI extends BaseClass {    
    // mbed-edge core PT config
    private String m_edge_core_ws_uri = null;
    private boolean m_connected = false;
    private String m_name = null;
    
    // websocket configuration (must have socat in container runtime)
    // confirm that the port number matches start_instance.sh 
    private int m_ws_port = 4455;
    private String m_ws_host = "localhost";
    
    // JSON-RPC/WS 
    private JsonRpcClient m_client = null;
        
    // default constructor
    public PelionEdgeCoreClientAPI(ErrorLogger logger,PreferenceManager preferences) {
        super(logger,preferences);
      
        // set the URI for our mbed-edge instance
        this.m_edge_core_ws_uri = "ws://" + this.m_ws_host + ":" + this.m_ws_port + "/1/pt";
        this.m_name = "edgex";
        
        // Announce
        this.errorLogger().warning("PelionDeviceAPI: Using EdgeCore Client API: " + this.m_edge_core_ws_uri);
      
    }
    
    // connect to the PT
    public boolean connect() {
        try {
            if (!this.m_connected) {
                // DEBUG
                this.errorLogger().warning("PelionEdgeCoreClientAPI: Connecting to: " + this.m_edge_core_ws_uri);
                
                // connect 
                this.m_client = new JsonRpcClientNettyWebSocket(this.m_edge_core_ws_uri);
                this.m_client.connect();
                this.m_connected = this.register();

                // DEBUG
                this.errorLogger().warning("PelionEdgeCoreClientAPI: CONNECTED: " + this.m_edge_core_ws_uri);
            }
        }
        catch (IOException ex) {
            this.errorLogger().warning("PelionEdgeCoreClientAPI: Exception in connect(): " + ex.getMessage());
        }
        return this.m_connected;
    }
    
    // disconnect from the PT
    public void disconnect() {
        if (this.m_connected) {
            // mbed-edge: closedown the WS socket
            if (this.m_client != null) {
                try {
                    this.m_client.close();
                }
                catch (IOException ex) {
                    this.errorLogger().warning("PelionEdgeCoreClientAPI: Exception in disconnect(): " + ex.getMessage());
                }
            }
        }
    }
    
    // is connected?
    public boolean isConnected() {
        // mbed-edge
        return this.m_connected;
    }
    
    // get device
    public String getDevice(String deviceId) {
        return this.invokeRPCWithResult("device_details", new Object[] {"deviceId",deviceId});
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
    
    // execute RPC (boolean return)
    private boolean invokeRPC(String rpc_method_name, Object[] params) {
        String reply = this.invokeRPCWithResult(rpc_method_name, params);
        if (reply != null) {
            return true;
        }
        return false;
    }
    // execute RPC (with result)
    private String invokeRPCWithResult(String rpc_method_name, Object[] params) {
        String reply = null;
        if (this.m_connected) {
            try {
                // invoke the RPC with our params
                JsonElement response = this.m_client.sendRequest(rpc_method_name,params);
                if (response != null) {
                    reply = response.getAsString();
                }
                
                // DEBUG
                this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC SUCCESS: Method: " + rpc_method_name + " Params: " + params + " Reply: " + reply);
            } 
            catch (IOException ex) {
                this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC FAILURE: Method: " + rpc_method_name + " Params: " + params + " Exception: " + ex.getMessage());
            }
        }
        return reply;
    }
}