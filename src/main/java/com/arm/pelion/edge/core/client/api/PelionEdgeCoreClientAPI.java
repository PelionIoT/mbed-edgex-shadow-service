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

import com.arm.pelion.rest.client.api.PelionRestClientAPI;
import com.arm.pelion.shadow.service.coordinator.Orchestrator;
import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.core.Utils;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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
    // try this many times to connect to the underlying WS service
    private static final int MAX_CONNECTION_ATTEMPTS = 10;
    
    // mbed-edge core PT config
    private String m_edge_core_ws_pt_uri = null;
    private String m_edge_core_ws_mgmt_uri = null;
    private boolean m_connected = false;
    private boolean m_registered = false;
    private String m_name = null;
    
    // websocket configuration (must have socat in container runtime)
    // confirm that the port number matches scripts/restart_mbed_edge_core.sh 
    private int m_ws_port_pt = 4455;
    private int m_ws_port_mgmt = 4456;
    private String m_ws_host = "localhost";
    
    // JSON-RPC/WS 
    private JsonRpcClient m_client_pt = null;
    private JsonRpcClient m_client_mgmt = null;
    
    // Orchestrator
    private Orchestrator m_orchestrator = null;
        
    // default constructor
    public PelionEdgeCoreClientAPI(ErrorLogger logger,PreferenceManager preferences,Orchestrator orchestrator) {
        super(logger,preferences);
        this.m_orchestrator = orchestrator;
      
        // set the URI for our mbed-edge instance
        this.m_edge_core_ws_pt_uri = "ws://" + this.m_ws_host + ":" + this.m_ws_port_pt + "/1/pt";
        this.m_edge_core_ws_mgmt_uri = "ws://" + this.m_ws_host + ":" + this.m_ws_port_mgmt + "/1/mgmt";
        this.m_name = "edgex";
        
        // Announce
        this.errorLogger().warning("PelionDeviceAPI: Using EdgeCore Client API PT: " + this.m_edge_core_ws_pt_uri + " MGMT: " + this.m_edge_core_ws_mgmt_uri);
      
    }
    
    // connect to the PT
    public synchronized boolean connect() {
        try {
            if (!this.m_connected) {
                for(int i=0;i<MAX_CONNECTION_ATTEMPTS && this.m_connected == false;++i) {
                    try {
                        if (this.m_client_pt == null) {
                            // Connect PT
                            // DEBUG
                            this.errorLogger().info("PelionEdgeCoreClientAPI: PT Connecting to: " + this.m_edge_core_ws_pt_uri);

                            // not registered
                            this.m_registered = false;
                        
                            // connect PT 
                            this.m_client_pt = new JsonRpcClientNettyWebSocket(this.m_edge_core_ws_pt_uri);
                            this.m_client_pt.connect();
                        }

                        if (this.m_client_mgmt == null) {
                            // Connect MGMT
                            // DEBUG
                            this.errorLogger().info("PelionEdgeCoreClientAPI: MGMT Connecting to: " + this.m_edge_core_ws_mgmt_uri);

                            // connect MGMT 
                            this.m_client_mgmt = new JsonRpcClientNettyWebSocket(this.m_edge_core_ws_mgmt_uri);
                            this.m_client_mgmt.connect();
                        }

                        // PT + MGMT connected!!
                        this.m_connected = true;

                        // DEBUG
                        this.errorLogger().warning("PelionEdgeCoreClientAPI: PT WS CONNECTED: " + this.m_edge_core_ws_pt_uri);
                        this.errorLogger().warning("PelionEdgeCoreClientAPI: MGMT WS CONNECTED: " + this.m_edge_core_ws_mgmt_uri);
                    }
                    catch (IOException ioex) {
                        // Wait for a bit and try again
                        this.errorLogger().info("PelionEdgeCoreClientAPI: Connection retry: " + ioex.getMessage());
                        Utils.waitForABit(this.errorLogger(), 2500);
                    }
                }
            }
        }
        catch (Exception ex) {
            this.errorLogger().info("PelionEdgeCoreClientAPI: Exception in connect(): " + ex.getMessage());
        }
        return this.m_connected;
    }
    
    // disconnect from the PT
    public void disconnect() {
        if (this.m_connected) {
            // mbed-edge: closedown the WS socket (PT)
            if (this.m_client_pt != null) {
                try {
                    this.m_client_pt.close();
                }
                catch (IOException ex) {
                    this.errorLogger().info("PelionEdgeCoreClientAPI: Exception in PT disconnect(): " + ex.getMessage());
                }
            }
            
            // mbed-edge: closedown the WS socket (MGMT)
            if (this.m_client_mgmt != null) {
                try {
                    this.m_client_mgmt.close();
                }
                catch (IOException ex) {
                    this.errorLogger().info("PelionEdgeCoreClientAPI: Exception in MGMT disconnect(): " + ex.getMessage());
                }
            }
        }
        
        // we are disconnected
        this.m_connected = false;
        this.m_client_pt = null;
        this.m_client_mgmt = null;
        this.m_registered = false;
    }
    
    // is connected?
    public boolean isConnected() {
        // mbed-edge
        return this.m_connected;
    }
    
    // get devices
    private String getDevices() {
        HashMap<String,String> req = new HashMap<>();
        String devices = this.invokeRPCWithResult(this.m_client_mgmt,this.createRequest("devices", req));
        
        // DEBUG
        this.errorLogger().info("PelionEdgeCoreClientAPI: getDevices(): LIST: " + devices);
        
        
        // return the devices list
        return devices;
    }
    
    // get a specific device detail 
    private String getDeviceDetail(String key,String ep) {
        String detail = null;
        boolean found = false;
        
        // get the devices list
        String json = this.getDevices();

        // parse...
        if (json != null) {
            Map data = this.jsonParser().parseJson(json);
            List data_list = (List)data.get("data");
            for (int i=0;data_list != null && i<data_list.size() && !found;++i) {
                Map entry = (Map)data_list.get(i);
                String ep_entry = (String)entry.get(key);
                if (ep_entry != null && ep_entry.equalsIgnoreCase(ep)) {
                    // found our entry
                    found = true;
                    
                    // call the REST API to get he bleaping deviceID... why doesn't this just come down in this json???
                    String e = (String)entry.get("endpointName");
                    String device_json = this.getDeviceFromEndpointName(e);
                    
                    // parse and save...
                    Map device = this.m_orchestrator.getJSONParser().parseJson(device_json);
                    entry.put("mbed_record",device_json);
                    entry.put("id",(String)device.get("id"));
                    detail = this.jsonGenerator().generateJson(entry);
                }
            }
        }
        
        // DEBUG
        this.errorLogger().info("PelionEdgeCoreClientAPI: getDeviceDetail: EP: " + ep + " DETAIL: " + detail);
        
        // return the detail
        return detail;
    }
    
    // get the device JSON record in Pelion by the endpoint name
    private String getDeviceFromEndpointName(String ep) {
        PelionRestClientAPI api = this.m_orchestrator.getMbedEdgeCoreServiceProcessor().getPelionRestClientAPI();
        return api.getDeviceFromEndpointName(ep);
    }
    
    // get device
    public String getDevice(String ep) {
        // get the device detail 
        String device = this.getDeviceDetail("endpointName", ep);
        
        // DEBUG
        if (device == null) {
            this.errorLogger().info("PelionEdgeCoreClientAPI: getDevice(): EP: " + ep + " DEVICE: <empty>");
        }
        else {
            this.errorLogger().info("PelionEdgeCoreClientAPI: getDevice(): EP: " + ep + " DEVICE: " + device);
        }
        
        // return the device detail
        return device;
    }
    
    // unregister device
    public boolean unregisterDevice(String deviceId) {
        HashMap<String,String> req = new HashMap<>();
        req.put("deviceId",deviceId);
        return this.invokeRPC(this.m_client_pt,this.createRequest("device_unregister", req));
    }
    
    // register device
    public Map registerDevice(Map device) {
        // DEBUG
        //this.errorLogger().info("registerDevice(Edge): Device: " + device);
        
        // objects list
        HashMap<String,Object> objects = new HashMap<>();
        
        // loop through our resources and 
        List resources = (List)device.get("resources");
        
        // loop through our resources and create the instance buckets 
        for(int i=0;resources != null && i<resources.size();++i) {
            HashMap<String,Object> entry = (HashMap<String,Object>)resources.get(i);
            String uri = (String)entry.get("path");
            Integer oid = this.getObjectIdFromURI(uri);
            Integer rid = this.getResourceIdFromURI(uri);
            
            // check if we have an oid already...
            HashMap<String,Object> object = (HashMap<String,Object>)objects.get("" + oid);
            if (object == null) {
                // create a new one...
                object = new HashMap<>();
                ArrayList<HashMap<String,Object>> instances = new ArrayList<>();
                object.put("objectId", oid);
                object.put("objectInstances",instances);
                
                // and add it...
                objects.put("" + oid,object);
            }
            
            // we have to determine if we have a new resource (and instance) or if we have an additional instance of a previously set resource
            if (this.containsResource(object,rid) == true) {
                // this object contains another rid matching this one... so we will build out another instance...
                ArrayList<HashMap<String,Object>> instances = (ArrayList<HashMap<String,Object>>)object.get("objectInstances");
                if (instances != null) {
                    // add a new instance to this object
                    HashMap<String,Object> instance = new HashMap<>();
                    instance.put("objectInstanceId", (Integer)instances.size());  
                    instances.add(instance);
                    ArrayList<HashMap<String,Object>> res = new ArrayList<>();
                    instance.put("resources",res);
                    HashMap<String,Object> resource = new HashMap<>();
                    resource.put("resourceId",rid);
                    resource.put("type",this.convertType((String)entry.get("type")));
                    resource.put("value",this.encodeValue(entry.get("value")));
                    resource.put("operations",this.convertRWPermsToRWOperations((String)entry.get("rw")));
                    res.add(resource);
                }
            }
            else {
                // this object does NOT have a previous resource... so we create our first instance and then resource...
                ArrayList<HashMap<String,Object>> instances = (ArrayList<HashMap<String,Object>>)object.get("objectInstances");
                if (instances != null) {
                    // add a new instance to this object
                    HashMap<String,Object> instance = new HashMap<>();
                    instance.put("objectInstanceId", 0);  
                    instances.add(instance);
                    ArrayList<HashMap<String,Object>> res = new ArrayList<>();
                    instance.put("resources",res);
                    HashMap<String,Object> resource = new HashMap<>();
                    resource.put("resourceId",rid);
                    resource.put("type",this.convertType((String)entry.get("type")));
                    resource.put("value",this.encodeValue(entry.get("value")));
                    resource.put("operations",this.convertRWPermsToRWOperations((String)entry.get("rw")));
                    res.add(resource);
                }
            }
        }
        
        // now iterate through the objects and convert to a list
        ArrayList<HashMap<String,Object>> objects_list = new ArrayList<>();
        for (Map.Entry t : objects.entrySet()) {
            HashMap<String,Object> obj = (HashMap<String,Object>)t.getValue();
            objects_list.add(obj);
        }
        
        // create the params JSON as a string
        String params_json = this.m_orchestrator.getJSONGenerator().generateJson(objects_list);
        if (params_json != null && params_json.equalsIgnoreCase("[]") == false) {
            // create the device record and dispatch the RPC...
            HashMap<String,Object> req = new HashMap<>();
            req.put("deviceId",(String)device.get("ep"));
            req.put("objects",objects_list);

            // DEBUG
            //this.errorLogger().info("registerDevice(Edge): Input JSON: " + resources);
            //this.errorLogger().info("registerDevice(Edge): Converted JSON: " + req);

            // call to register the device...
            String reply =  this.invokeRPCWithResult(this.m_client_pt,this.createRequest("device_register", req));
            if (reply != null) {                
                // get the mbed device ID and save it...
                String id = this.getPelionDeviceIDForDeviceName((String)device.get("ep"));
                if (id != null) {
                    device.put("id", id);
                }
            }
            return device;
        }
        else {
            // non-mapped EdgeX resources - so unable to create new device
            this.errorLogger().warning("registerDevice(Edge): FAILED: Unable to map EdgeX resources to LWM2M: " + device);
            return null;
        }
    }
    
    // get the Pelion Device ID for a give Pelion Endpoint Name
    private String getPelionDeviceIDForDeviceName(String ep) {
        String id = null;
        
        // Get the device Detail
        String json = this.getDevice(ep);
        
        // DEBUG
        this.errorLogger().info("getPelionDeviceIDForDeviceName: EP: " + ep + " JSON: " + json);
        
        // if we have details, parse them and get the ID
        if (json != null && json.length() > 0) {
            Map device = this.m_orchestrator.getJSONParser().parseJson(json);
            if (device != null) {
                // DEBUG
                this.errorLogger().info("getPelionDeviceIDForDeviceName: EP: " + ep + " DEVICE: " + device);
                id = (String)device.get("id");
            }
        }
        
        // return the ID
        return id;
    }
    
    // does this object contain the given resource ID?
    private boolean containsResource(HashMap<String,Object> object, int rid) {
        boolean contains = false;
        
        ArrayList<HashMap<String,Object>> instances = (ArrayList<HashMap<String,Object>>)object.get("objectInstances");
        if (instances != null) {
            for(int i=0;instances != null && i<instances.size() && !contains;++i) {
                HashMap<String,Object> instance = instances.get(i);
                ArrayList<HashMap<String,Object>> resources = (ArrayList<HashMap<String,Object>>)instance.get("resource");
                for(int j=0;resources != null && j<resources.size() && !contains;++j) {
                    HashMap<String,Object> resource = resources.get(j);
                    if (resource != null) {
                        Integer r_rid = (Integer)resource.get("resourceId");
                        if (r_rid == rid) {
                            contains = true;
                        }
                    }
                }
            }
        }
        
        return contains;
    }
    
    // convert Type
    private String convertType(String type) {
        String converted_type = "string";
        
        if (type != null) {
            if (type.toLowerCase().contains("int")) {
                converted_type = "integer";
            }
            if (type.toLowerCase().contains("float") || type.toLowerCase().contains("double")) {
                converted_type = "float";
            }
        }
        
        // return converted type
        return converted_type;
    }
    
    // convert Perms to Operations
    private int convertRWPermsToRWOperations(String rw) {
        int operations = 0x00;
        if (rw != null && rw.length() > 0) {
            if (rw.equalsIgnoreCase("r")) {
                operations = Operations.READ;
            }
            if (rw.equalsIgnoreCase("w")) {
                operations = Operations.WRITE;
            }
            if (rw.equalsIgnoreCase("x")) {
                operations = Operations.EXECUTE;
            }
            if (rw.equalsIgnoreCase("d")) {
                operations = Operations.DELETE;
            }
            if (rw.equalsIgnoreCase("rw")) {
                operations = Operations.READ | Operations.WRITE;
            }
            if (rw.equalsIgnoreCase("wr")) {
                operations = Operations.READ | Operations.WRITE;
            }
            if (rw.equalsIgnoreCase("rwx")) {
                operations = Operations.READ | Operations.WRITE | Operations.EXECUTE;
            }
            if (rw.equalsIgnoreCase("rwxd")) {
                operations = Operations.READ | Operations.WRITE | Operations.EXECUTE | Operations.DELETE;
            }
        }
        return operations;
    }

    // create an object instance
    private HashMap<String,Object> createObjectInstance(Map entry) {
        HashMap<String,Object> object = new HashMap<>();
        ArrayList<HashMap<String,String>> resource_list = new ArrayList<>();
        String uri = (String)entry.get("path");
        Integer oid = this.getObjectIdFromURI(uri);
        Integer rid = this.getResourceIdFromURI(uri);
        object.put("objectId",oid);
        
        return null;
    }
    
     // dispatch an observation
    public boolean sendObservation(String ep,String uri,Object value) {
        // DEBUG
        this.errorLogger().warning("PelionEdgeCoreClientAPI: sendObservation: EP: " + ep + " URI: " + uri + " VALUE: " + value);
        
        // Parse the URI
        Integer oid = this.getObjectIdFromURI(uri);
        Integer oiid = this.getObjectInstanceIdFromURI(uri);
        Integer rid = this.getResourceIdFromURI(uri);
        
        // create the objects JSON
        HashMap<String,Object> object = new HashMap<>();
        
        // create the object instance
        object.put("objectId",oid);
        
        ArrayList<HashMap<String,Object>> objectInstances = new ArrayList<>();
        HashMap<String,Object> instance = new HashMap<>();
        
        instance.put("objectInstanceId",oiid);
        
        ArrayList<HashMap<String,Object>> resources = new ArrayList<>();
        HashMap<String,Object> resource = new HashMap<>();
        
        resource.put("resourceId",rid);  
        resource.put("value",this.encodeValue(value));
        
        resources.add(resource);
        
        instance.put("resources",resources);
        objectInstances.add(instance);
        
        object.put("objectInstances",objectInstances);
                
        ArrayList<HashMap<String,Object>> objects = new ArrayList<>();
        objects.add(object);
        
        // craft message to write a value via PT...
        HashMap<String,Object> req = new HashMap<>();
        req.put("deviceId",ep);
        req.put("objects",objects);
        
        // DEBUG
        //this.errorLogger().warning("sendObservation: REQUEST: " + req);
        
        // make the call to PT to update the value
        return this.invokeRPC(this.m_client_pt,this.createRequest("write", req));
    }
    
    // encode the value to Base64 
    private String encodeValue(Object value) {
        String encoded_value = null;
        
        if (value instanceof Integer) {
            int i = ((Integer)value);
            encoded_value = Base64.getEncoder().encodeToString(BigInteger.valueOf(i).toByteArray());
        }
        if (value instanceof String) {
            encoded_value = Base64.getEncoder().encodeToString(((String)value).getBytes());
        }
        if (value instanceof Float) {
            float f = ((Float)value);
            encoded_value = Base64.getEncoder().encodeToString(("" + f).getBytes());
        }
        if (value instanceof Double) {
            double d = ((Double)value);
            encoded_value = Base64.getEncoder().encodeToString(("" + d).getBytes());
        }
        return encoded_value;
    }
    
    // get the object id from the URI
    private Integer getObjectIdFromURI(String uri) {
        return Integer.parseInt(this.getURIField(0,uri));
    }
    
     // get the object instance id from the URI
    private Integer getObjectInstanceIdFromURI(String uri) {
        return Integer.parseInt(this.getURIField(1,uri));
    }
    
    // get the resource id from the URI
    private Integer getResourceIdFromURI(String uri) {
        return Integer.parseInt(this.getURIField(2,uri));
    }
    
    // get the specific field from the URI as a String
    private String getURIField(int which,String uri) {
        String field = "";
        
        if (uri != null && uri.length() > 0) {
            // split
            String[] fields = uri.replace("/"," ").trim().split(" ");
            
            // DEBUG
            //for(int i=0;i<fields.length;++i) {
            //    this.errorLogger().warning("getURIField(" + i + "): " + fields[i]);
            //}
            
            // reference the appropriate field
            if (which >= 0 && fields != null && which < fields.length) {
                field = fields[which];
            }
        }
        
        return field;
    }
    
    
    // register our API
    public boolean register() {    
        HashMap<String,String> req = new HashMap<>();
        req.put("name",this.m_name);
        String rpc_method_name = "protocol_translator_register";
        
        // DEBUG
        this.errorLogger().info("PelionEdgeCoreClientAPI: PT Registration: FN: " + rpc_method_name + " PARAM: " + req);
        
        // issue the registration request
        boolean status = this.invokeRPC(this.m_client_pt,this.createRequest(rpc_method_name, req));
        if (status) {
            // success
            this.errorLogger().info("PelionEdgeCoreClientAPI: PT registration SUCCESS");
        }
        else {
            // failure
            this.errorLogger().warning("PelionEdgeCoreClientAPI: PT registration FAILURE");
            
            // disconnect
            this.disconnect();
        }
        return status;
    }
    
    // create the request object
    private Request<JsonObject> createRequest(String rpc_method_name,Map params) {
        String json_str = this.m_orchestrator.getJSONGenerator().generateJson(params);
        return this.createRequest(rpc_method_name,json_str);
    }
    
    // create the request object
    private Request<JsonObject> createRequest(String rpc_method_name,String params) {
        // create an RPC request instance
        Request<JsonObject> request = new Request<>();
        
        // set the RPC call method name
        request.setMethod(rpc_method_name);
        
        // convert to the Google gson structures
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(params);
        
        // allocate params object
        JsonObject request_params = je.getAsJsonObject();
        
        // set the parameters in the request
        request.setParams(request_params);
        
        // DEBUG
        this.errorLogger().info("createRequest: METHOD: " + rpc_method_name + " PARAMS: " + request_params);
        
        // return the request
        return request;
    }
    
    // execute RPC (boolean return)
    private boolean invokeRPC(JsonRpcClient handle,Request<JsonObject> request) {
        String reply = this.invokeRPCWithResult(handle,request);
        if (reply != null && reply.length() > 0) {
            // DEBUG
            this.errorLogger().info("PelionEdgeCoreClientAPI: RPC SUCCESS. REPLY: " + reply);
            
            // return success
            return true;
        }
        
        // return failure
        return false;
    }
    // execute RPC (with result)
    private String invokeRPCWithResult(JsonRpcClient handle,Request<JsonObject> request) {
        String reply = null;
        
        try{
            // ensure we are connected
            if (this.m_connected == false) {
                // attempt a connection and registration
                this.m_connected = this.connect();
                
                // if connected and !registered
                if (this.m_connected == true && handle == this.m_client_pt && this.m_registered == false) {
                    this.m_registered = this.register();
                }
            }

            // continue only if connected
            if (this.m_connected && handle != null) {
                try {
                    // DEBUG
                    this.errorLogger().info("PelionEdgeCoreClientAPI: RPC(" + request.getMethod() + ") PARAMS: " + request.getParams().toString());
                    
                    // invoke the RPC with our params
                    Response<JsonElement> response = handle.sendRequest(request);
                    if (response != null) {
                        try {
                            if (response.getResult() != null) {
                                reply = response.getResult().toString();
                            }
                            else {
                                reply = "ok";
                            }
                        }
                        catch (Exception ex) {
                            this.errorLogger().info("PelionEdgeCoreClientAPI: RPC(" + request.getMethod() + ") (result parsing) FAILURE: Exception: " + ex.toString());
                            reply = null;
                        }
                    }

                    // DEBUG
                    if (reply == null) {
                        this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC(" + request.getMethod() + ") FAILURE: Reply: <empty>");
                    }
                    else {
                        this.errorLogger().info("PelionEdgeCoreClientAPI: RPC(" + request.getMethod() + ") SUCCESS: RESPONSE: " + reply);
                    }
                } 
                catch (IOException ex) {
                    this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC(" + request.getMethod() + ") FAILURE: Exception: " + ex.getMessage());
                    reply = null;
                }
            }
            else if (handle == null) {
                this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC(" + request.getMethod() + ") FAILURE: handle is NULL");
                reply = null;
            }
        }
        catch (Exception ex) {
             this.errorLogger().warning("PelionEdgeCoreClientAPI: RPC FAILURE: Method: Exception: " + ex.getMessage());
             reply = null;
        }
        return reply;
    }
}