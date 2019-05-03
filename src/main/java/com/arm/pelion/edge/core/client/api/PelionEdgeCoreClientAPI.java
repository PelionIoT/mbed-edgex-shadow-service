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
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientNettyWebSocket;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;

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
    // confirm that the port number matches scripts/restart_mbed_edge_core.sh 
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
                this.m_connected = true;

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
        HashMap<String,String> req = new HashMap<>();
        req.put("deviceId",deviceId);
        return this.invokeRPCWithResult(this.createRequest("device_details", req));
    }
    
    // unregister device
    public boolean unregisterDevice(String deviceId) {
        HashMap<String,String> req = new HashMap<>();
        req.put("deviceId",deviceId);
        return this.invokeRPC(this.createRequest("device_unregister", req));
    }
    
    // register device
    public boolean registerDevice(Map device) {
        HashMap<String,String> req = new HashMap<>();
        req.put("deviceId",(String)null);
        return this.invokeRPC(this.createRequest("device_register", req));
    }
    
    // register our API
    public boolean register() {    
        HashMap<String,String> req = new HashMap<>();
        req.put("name",this.m_name);
        String rpc_method_name = "protocol_translator_register";
        
        // DEBUG
        this.errorLogger().warning("PelionEdgeCoreClientAPI: PT Registration: FN: " + rpc_method_name + " PARAM: " + req);
        
        // issue the registration request
        boolean status = this.invokeRPC(this.createRequest(rpc_method_name, req));
        if (status) {
            // success
            this.errorLogger().warning("PelionEdgeCoreClientAPI: PT registration SUCCESS");
        }
        else {
            // failure
            this.errorLogger().warning("PelionEdgeCoreClientAPI: PT registration FAILURE");
        }
        return status;
    }
    
    // create the request object
    private Request<JsonObject> createRequest(String rpc_method_name,Map params) {
        // create an RPC request instance
        Request<JsonObject> request = new Request<>();
        
        // set the RPC call method name
        request.setMethod(rpc_method_name);
        
        // allocate params object
        JsonObject request_params = new JsonObject();
        
        // iterate through the map and convert to the request format
        Iterator it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            request_params.addProperty((String)entry.getKey(),(String)entry.getValue());
        } 
        
        // set the parameters in the request
        request.setParams(request_params);
        
        // return the request
        return request;
    }
    
    // execute RPC (boolean return)
    private boolean invokeRPC(Request<JsonObject> request) {
        String reply = this.invokeRPCWithResult(request);
        if (reply != null) {
            // DEBUG
            this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC SUCCESS. REPLY: " + reply);
            
            // return success
            return true;
        }
        
        // return failure
        return false;
    }
    // execute RPC (with result)
    private String invokeRPCWithResult(Request<JsonObject> request) {
        String reply = null;
        
        try{
            // ensure we are connected
            if (this.m_connected == false) {
                // attempt a connection and registration
                this.m_connected = this.connect();
            }

            // continue only if connected
            if (this.m_connected) {
                try {
                    // invoke the RPC with our params
                    Response<JsonElement> response = this.m_client.sendRequest(request);
                    if (response != null) {
                        try {
                            reply = response.getResult().getAsString();
                        }
                        catch (Exception ex) {
                            this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC (result parsing) FAILURE: Exception: " + ex.getMessage());
                            reply = null;
                        }
                    }

                    // DEBUG
                    this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC SUCCESS: Reply: " + reply);
                } 
                catch (IOException ex) {
                    this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC FAILURE: Exception: " + ex.getMessage());
                    reply = null;
                }
            }
        }
        catch (Exception ex) {
             this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC FAILURE: Method: Exception: " + ex.getMessage());
             reply = null;
        }
        return reply;
    }
}