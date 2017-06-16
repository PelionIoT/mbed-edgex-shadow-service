/**
 * @file    mbedClientServiceProcessor.java
 * @brief EdgeX Event Processor
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
package com.arm.mbed.edgex.shadow.service.processors;

import com.arm.edgex.shadow.service.db.mbedDeviceShadowDatabase;
import com.arm.mbed.edgex.shadow.service.core.BaseClass;
import com.arm.mbed.edgex.shadow.service.core.ErrorLogger;
import com.arm.mbed.edgex.shadow.service.core.Utils;
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;
import com.arm.mbed.edgex.shadow.service.transport.HttpTransport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * mbed Client Service Event Processor
 * @author Doug Anson
 */
public class mbedClientServiceProcessor extends BaseClass {
    private HttpTransport m_http = null;
    private EdgeXEventProcessor m_edgex = null;
    private mbedDeviceShadowDatabase m_db = null;
    private String m_default_shadow_ept = null;
    
    // mCS Configuration
    private String m_mcs_ip_address = null;
    private int m_mcs_port = 0;
    private String m_mcs_access_key = null;
    private String m_mcs_domain_id = null;
    private String m_mcs_service_name = null;
    private String m_mcs_device_uri = null;
    private String m_mcs_resources_uri = null;
    private String m_mcs_register_uri = null;
    
    // default constructor
    public mbedClientServiceProcessor(ErrorLogger error_logger, PreferenceManager preference_manager) {
        super(error_logger, preference_manager);
        
        // create the HTTP transport
        this.m_http = new HttpTransport(error_logger,preference_manager);
        
        // create the shadow database
        this.m_db = new mbedDeviceShadowDatabase(error_logger,preference_manager);
        
        // get the default endpoint type for shadow devices
        this.m_default_shadow_ept = preference_manager.valueOf("mbed_default_ept");
        
        // mCS configuration
        this.m_mcs_ip_address = preference_manager.valueOf("mcs_ip_address");
        this.m_mcs_port = preference_manager.intValueOf("mcs_port");
        this.m_mcs_access_key = preference_manager.valueOf("mcs_access_key");
        this.m_mcs_domain_id = preference_manager.valueOf("mcs_domain_id");
        this.m_mcs_service_name = preference_manager.valueOf("mcs_service_name");
        this.m_mcs_device_uri = preference_manager.valueOf("mcs_device_uri");
        this.m_mcs_resources_uri = preference_manager.valueOf("mcs_resources_uri");
        this.m_mcs_register_uri = preference_manager.valueOf("mcs_register_uri");
    }
    
    // create the devices URI
    private String createMCSDevicesURL() {
        return "http://" + this.m_mcs_ip_address + ":" + this.m_mcs_port + this.m_mcs_device_uri;
    }
    
    // create the device resources URI
    private String createMCSDeviceResourcesURL(String id) {
        return "http://" + this.m_mcs_ip_address + ":" + this.m_mcs_port + this.m_mcs_device_uri + "/" + id + this.m_mcs_resources_uri;
    }
    
    // create the device registration URI
    private String createMCSDeviceRegisterURL(String id) {
        return "http://" + this.m_mcs_ip_address + ":" + this.m_mcs_port + this.m_mcs_device_uri + "/" + id + this.m_mcs_register_uri;
    }
    
    // create the authorization JSON
    private String createAuthJSON() {
        // Create the JSON as a Map
        HashMap<String,String> map = new HashMap<>();
        map.put("accessKey", this.m_mcs_access_key);
        map.put("domain",this.m_mcs_domain_id);
        map.put("service",this.m_mcs_service_name);
        
        // Convert to JSON string
        return Utils.removeArray(this.jsonGenerator().generateJson(map));
    }
    
    // create the mCS resource JSON
    // Example FORMAT: {"path":"/3201/0/5853","operation":["GET","PUT"],"valueType":"dynamic","value":"500:500","observable":false}
    private Map createResourceJSON(Map resource) {
        ArrayList<String> operations = new ArrayList<>();
        
        // Create the JSON as a Map
        HashMap<String,Object> map = new HashMap<>();
        
        // create the mCS-compatible JSON for the input resource
        map.put("path",(String)resource.get("path"));
        map.put("value",(String)resource.get("value"));
        
        // ASSUMPTION: all resources are dynamic in nature
        map.put("valueType","dynamic");
        
        // define the sorts of operations that are permissible
        operations.add("GET");
        String rw = (String)resource.get("rw");
        if (rw.equalsIgnoreCase("w") == true || rw.equalsIgnoreCase("rw") == true) {
            operations.add("PUT");
            operations.add("POST");
        }
        
        // add the operations options
        map.put("operation",(List)operations);
        
        // ASSUMPTION: all resources are observable
        map.put("observable",true);
        
        // return the map
        return map;
    }
    
    // set our EdgeX Event Processor
    public void setEdgeXEventProcessor(EdgeXEventProcessor edgex) {
        this.m_edgex = edgex;
    }
    
    // get our EdgeX Event Processor
    public EdgeXEventProcessor edgeXEventProcessor() {
        return this.m_edgex;
    }
    
    // initialize the mbed Client service processor
    public boolean initialize() {
        // initialize the database
        boolean status = this.m_db.initialize(this);
        
        // return our status
        return status;
    }
    
    // process mCS events
    public void processEvent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // process an event - this will a direct CoAP get/put/post FROM the mbed device to the EdgeX device ...
        
        // DEBUG
        this.errorLogger().info("processEvent: Received mCS device event: " + request);
        
        // XXX
    }
    
    // validate a specific mbed endpoint
    public boolean validate(String mbed_id,String edgex_name) {
        boolean validated = false;
        
        // First we validate the mbed device via mCS
        this.errorLogger().info("validate: Validating that mCS device is still in existance: " + mbed_id);
        if (this.validateMbedDevice(mbed_id) == true) {
            // now validate the EdgeX device via EdgeX
            validated = this.validateEdgeXDevice(edgex_name);
        }
        
        // return the validation status
        return validated;
    }
    
    // validate the mbed device via mCS
    private boolean validateMbedDevice(String mbed_id) {
        boolean validated = false;
        
        // Lets query mCS and get this device details
        String url = this.createMCSDevicesURL() + "/" + mbed_id;
        String response = this.m_http.httpGet(url);
        
        // analyze results
        if (this.m_http.getLastResponseCode() < 300) {
            // device exists in mCS
            this.errorLogger().info("validateMbedDevice: mbed ID: " + mbed_id + " is known to mCS: response: " + response);
            validated = true;
        }
        else {
            // device does not exist in mCS
            this.errorLogger().info("validateMbedDevice: mbed ID: " + mbed_id + " is NOT known to mCS: response: " + response);
        }
        
        // return the validation status
        return validated;
    }
    
    // validate the EdgeX device in EdgeX
    private boolean validateEdgeXDevice(String edgex_name) {
        boolean validated = false;
        
        // lets call EdgeX and get the device metaeata record... if get something, we assume the device exists in EdgeX
        Map details = this.lookupEdgeXDeviceDetails(edgex_name);
        if (details != null) {
            // device exists in EdgeX
            this.errorLogger().info("validateEdgeXDevice: EdgeX Name: " + edgex_name + " is a valid device in EdgeX: " + details);
            validated = true;
        }
        else {
            // device does not exist in EdgeX
            this.errorLogger().info("validateEdgeXDevice: EdgeX Name: " + edgex_name + " is NOT known to EdgeX");
        }
        
        
        // return the validation status
        return validated;
    }
    
    // closedown the mbed Client service processor
    public void closedown() {
        // sync the cache to file if enabled
        boolean saved = this.m_db.saveToCache();
        if (saved == true) {
            // successfully cached mappings
            this.errorLogger().info("mbedClientServiceProcessor:closedown: SUCCESS device mapping saved...");
        }
        else {
            // unable to cache mappings or disabled
            this.errorLogger().info("mbedClientServiceProcessor:closedown: FAILURE device mapping NOT saved...");
            
            // go ahead and clear the cache
            this.m_db.clearCacheFiles();
        }
    }

    // send an observation
    public boolean sendObservation(Map edgex_message) {
        boolean sent = false; 

        // pull the "readings" values
        List readings = (List)edgex_message.get("readings");
        for(int i=0;readings != null && i<readings.size();++i) {
            // get the ith readings
            Map reading = (Map)readings.get(i);
            
            // Create a trimmed mbed Map of the reading
            HashMap<String,Object> mbed_reading = new HashMap<>();
            mbed_reading.put("path",(String)this.mapEdgeXResourceToMbedResource((String)reading.get("name")));
            mbed_reading.put("value",reading.get("value"));
            
            // get the mbed ID for this EdgeX name
            String mbed_id = this.m_db.lookupMbedName((String)reading.get("device"));
            mbed_reading.put("id",mbed_id);
            
            // DEBUG
            this.errorLogger().info("sendObservation: mbed Observation reading: " + mbed_reading);
            
            // update the mbed path value via mCS
            String url = this.createMCSDeviceResourcesURL(mbed_id) + (String)mbed_reading.get("path");
            String mbed_reading_str = this.createResourceObservationJSON(mbed_reading);
            
            // now send the value 
            this.m_http.httpPut(url, mbed_reading_str);
            
            // check the result
            if (this.m_http.getLastResponseCode() < 300) {
                // DEBUG
                this.errorLogger().info("sendObservation: SUCCESS. Observation sent: " + mbed_reading);
                sent = true;
            }
            else {
                // FAILURE
                this.errorLogger().warning("sendObservation: FAILURE: Observation send failed. status: " + this.m_http.getLastResponseCode() + " reading: " + mbed_reading);
                sent = false;
            }
        }
        
        // return the observation send status
        return sent;
    }
    
    // create the Resource observation JSON for mCS
    private String createResourceObservationJSON(Map reading) {
        return "{\"value\":" + reading.get("value") + "}";
    }
    
    // send an observation to an mbed device via mCS
    private boolean sendObservation(String mbed_id,Map edgex_reading) {
        boolean sent = false;
        
        // return the observation send status
        return sent;
    }
    
    // is this device mapped/shadowed?
    public boolean deviceShadowed(Map edgex_message) {
        boolean shadowed = false;
        
        // Get the EdgeX device name
        String edgex_name = (String)edgex_message.get("device");
        
        Map mbed_device = this.edgexDeviceToMbedDevice(edgex_name);
        if (mbed_device != null) {
            // we have an entry
            shadowed = true;
        }
        
        // return our shadow status
        return shadowed;
    }
    
    // create a device shadow
    public boolean createDeviceShadow(Map edgex_message) {
        boolean created = false;
        
        // Get the EdgeX ID
        String edgex_dev_name = (String)edgex_message.get("device");
        
        // Get the resource information for the EdgeX device
        Map edgex_device = this.lookupEdgeXDeviceDetails(edgex_dev_name);
        if (edgex_device != null) {
            // create the mbed Shadow + its resources 
            Map mbed_device = this.createMbedDeviceShadow(edgex_device);
            if (mbed_device != null) {
                // save this new device off in the cache
                String mbed_id = (String)mbed_device.get("id");
                this.m_db.addDevice(mbed_id, mbed_device, edgex_dev_name, edgex_device);
                
                // save the config to cache
                created = this.m_db.saveToCache();
                
                // DEBUG
                if (created == true) {
                    // Successfully cache the shadow!
                    this.errorLogger().info("createDeviceShadow: SUCCESS. mbed shadow: " + mbed_id + " for EdgeX device: " + edgex_dev_name + " successfully cached");
                }
                else {
                    // Unable to cache the shadow
                    this.errorLogger().info("createDeviceShadow: FAILURE. mbed shadow: " + mbed_id + " for EdgeX device: " + edgex_dev_name + " NOT CACHED");
                }
            }
            else {
                // ERROR - unable to lookup metadata for device
                this.errorLogger().warning("createDeviceShadow: Unable to acquire metadata from EdgeX for device: " + edgex_dev_name);
            }
        }
        
        // return our creation status
        return created;
    }
    
    // create the mbed device shadow for our EdgeX device
    private Map createMbedDeviceShadow(Map edgex_device) {
        Map mbed_device = null;
        
        // Initialize the mbed device map
        mbed_device = this.initMbedDeviceMapFromEdgeMap(edgex_device);
        
        // Convert the EdgeX resources to mbed resources
        mbed_device = this.mapEdgeXResourcesToMbedResources(mbed_device,edgex_device);
        
        // With the mbed device map now formed, call mCS to create this device
        mbed_device = this.createShadow(mbed_device);
        if (mbed_device != null) {
            // DEBUG
            this.errorLogger().info("createMbedDeviceShadow: SUCCESS. mbed Device Shadow created: " + mbed_device);
         
            // return the mbed device
            return mbed_device;   
        }
        else {
            // ERROR in creating mbed device shadow
            this.errorLogger().warning("createMbedDeviceShadow: Unable to create mbed device shadow for: " + mbed_device);
        }
        return null;
    }
    
    // call mCS to create the device shadow in mbed Cloud
    private Map createShadow(Map mbed_device) {
        boolean created = false;
        
        // DEBUG
        this.errorLogger().info ("createShadow: creating mbed device shadow for device: " + mbed_device);
        
        // 1: Create the Device via mCS POST 
        String url = this.createMCSDevicesURL();
        String auth_json_str = this.createAuthJSON();
        String response = this.m_http.httpPost(url, auth_json_str);
        
        try {
            // check the status
            if (this.m_http.getLastResponseCode() < 300 && response != null && response.length() > 0) {
                // 2: Convert the response to JSON
                Map devices_json = this.jsonParser().parseJson(response);
                
                // 2: Get the mbed shadow ID
                String mbed_id = (String)devices_json.get("id");
                
                // 2: save the ID
                mbed_device.put("id",mbed_id);

                // 2: Next we create resources URL for the shadow
                url = this.createMCSDeviceResourcesURL(mbed_id);
                
                // 2: status
                boolean resources_added = true;
                
                // 2: mCS Resource list
                ArrayList<Map> mcs_resources = new ArrayList<>();
                
                // 2: Loop through the resources and create each within mCS
                List resource_list = (List)mbed_device.get("resources");
                for(int i=0;resource_list != null && i<resource_list.size() && resources_added == true;++i) {
                    // 2i: Get the ith Resource
                    Map resource = (Map)resource_list.get(i);
                    
                    // 2i: Create the mCS-compatible resource JSON
                    Map mcs_resource = this.createResourceJSON(resource);
                    
                    // 2i: Add to an array of resources
                    mcs_resources.add(mcs_resource);
                }
                
                // 2i: Stringify the array
                String mcs_resources_str = Utils.removeArray(this.jsonGenerator().generateJson(mcs_resources));
                
                // 2i: Invoke the PUT for these resources (no response expected)
                this.m_http.httpPut(url, mcs_resources_str);

                // 2i: Note the result
                if (this.m_http.getLastResponseCode() < 300) {
                    // SUCCESS for the ith resource addition
                    this.errorLogger().info("createShadow: ADD SUCCESS resources: " + mcs_resources_str + " device: " + mbed_id + " status: " + this.m_http.getLastResponseCode());
                }
                else {
                    // FAILURE for the ith resource addition
                    this.errorLogger().warning("createShadow: ADD FAILURE resource: " + mcs_resources_str + " device: " + mbed_id + " status: " + this.m_http.getLastResponseCode());
                    resources_added = false;
                }
                
                // 3: Now register the shadow device
                if (resources_added == true) {
                    // 3: register.. all resources added
                    url = this.createMCSDeviceRegisterURL(mbed_id);
                    
                    // 3: call POST to register the device (no response expected)
                    this.m_http.httpPost(url,auth_json_str);
                    
                    // 3: note the response
                    if (this.m_http.getLastResponseCode() < 300) {
                        // SUCCESS! mCS shadow device added
                        this.errorLogger().info("createShadow: SUCCESS device shadow CREATED. status: " + this.m_http.getLastResponseCode());
                        
                        // set the success status
                        created = true;
                    }
                    else {
                        // FAILURE: mCS shadow device NOT added
                        this.errorLogger().warning("createShadow: FAILED to register device shadow. status: " + this.m_http.getLastResponseCode());
                    }
                }
                else {
                    // 3: FAILURE 
                    this.errorLogger().warning("createShadow: NOT registering mCS shadow device due to errors: " + mbed_id);
                }
            }
            else {
                // ERROR: unable to POST to /devices 
                this.errorLogger().warning("createShadow: Unable to POST to mCS for /devices: error: " + this.m_http.getLastResponseCode() + " device: " + mbed_device + " response: " + response);
            }
        }
        catch(Exception ex) {
            // Exception in JSON parsing
            this.errorLogger().warning("createShadow: JSON parsing error: " + ex.getMessage(),ex);
        }
        
        // return the creation status
        if (created) {
            // DEBUG
            this.errorLogger().info("createShadow: SUCCESS. mbed shadow created: " + mbed_device);
            return mbed_device;
        }
        return null;
    }
    
    // initialize the mbed device map
    private Map initMbedDeviceMapFromEdgeMap(Map edgex_device) {
        HashMap<String,Object> mbed_device = new HashMap<>();
        
        // duplicate the basic information for this device... 
        mbed_device.put("num_resources",(Integer)edgex_device.get("num_resources"));
        
        // handle endpoint_type as a special case (should be as key "device_type" from EdgeX, if present)
        String ept = (String)edgex_device.get("device_type");
        if (ept == null) {
            ept = this.m_default_shadow_ept;
        }
        mbed_device.put("ept",ept);
        
        // return the mbed device map
        return mbed_device;
    }
    
    // map a single EdgeX resource to its equivalent mbed Resource
    private String mapEdgeXResourceToMbedResource(String edgex_resource) {
        // use the preference store - key is the EdgeX resoure name... value is the mbed Resource URI (path)
        return this.prefValue(edgex_resource);
    }
    
    // map EdgeX resources to mbed resources within the mbed device map
    private Map mapEdgeXResourcesToMbedResources(Map mbed_device,Map edgex_device) {
        // Create the mbed resource list
        ArrayList<HashMap<String,String>> mbed_resources = new ArrayList<>();
        
        // grab the EdgeX resource list
        List edgex_resources = (List)edgex_device.get("resources");
        
        // loop through resources list, and utilize the preferences store: search for the EdgeX resource name... 
        for(int i=0;edgex_resources != null && i<edgex_resources.size();++i) {
            // Get the map for the ith EdgeX resource
            Map resource = (Map)edgex_resources.get(i);
            
            // Get the ith EdgeX resource name
            String edgex_resource_name = (String)resource.get("name");
            
            // Create the ith mbed resource map
            HashMap<String,String> mbed_resource = new HashMap<>();
            
            // map the EdgeX resource name to the mbed Resource URI (path)...
            String mbed_uri = this.mapEdgeXResourceToMbedResource(edgex_resource_name);
            if (mbed_uri != null && mbed_uri.length() > 0) {
                // save the resource URI as a path
                mbed_resource.put("path",mbed_uri);
                
                // note the UOM, accessibility, and default value
                mbed_resource.put("uom",(String)resource.get("uom"));
                mbed_resource.put("rw",(String)resource.get("rw"));
                mbed_resource.put("type",(String)resource.get("type"));
                mbed_resource.put("value",(String)resource.get("value"));
                
                // add this resource to the list of mbed resources...
                mbed_resources.add(mbed_resource);
            }
        }
        
        // add the mbed resources to the mbed device map
        HashMap<String,Object> map = null;
        map = (HashMap<String,Object>)mbed_device;
        map.put("resources", mbed_resources);
        
        // return the map
        return mbed_device;
    }
    
    // lookup our EdgeX device details
    private Map lookupEdgeXDeviceDetails(String edgex_dev_name) {
       // create the URL to EdgeX that retrieves the metadata for the device
       String url = this.m_edgex.buildEdgeXMetadataURL(edgex_dev_name);
       
       // call EdgeX to retrieve the metadata for the device
       String edgex_metadata_str = this.m_http.httpGet(url);
       
       // make sure we got something back...
       if (this.m_http.getLastResponseCode() < 300 && edgex_metadata_str != null && edgex_metadata_str.length() > 0) {
            // remove any empty stuff that our crappy JSON parser cannot handle... ugh... 
            edgex_metadata_str = edgex_metadata_str.replace("\"\"","\" \"");
            edgex_metadata_str = edgex_metadata_str.replace("[]","null");
            edgex_metadata_str = edgex_metadata_str.replace("{}","null");
            
            try {
                // DEBUG
                //this.errorLogger().info("lookupEdgeXDeviceDetails: EdgeX Metadata: " + edgex_metadata_str);

                // convert the JSON string to a Map
                Map raw_edgex_metadata = this.jsonParser().parseJson(edgex_metadata_str);

                // now... trim down the metadata and make it what we minimally need
                Map trimmed_edgex_metadata = this.m_edgex.trimEdgeXMetadata(raw_edgex_metadata);
                
                // DEBUG
                this.errorLogger().info("lookupEdgeXDeviceDetails: EdgeX Metadata: " + trimmed_edgex_metadata);
                
                // return the trimmed details
                return trimmed_edgex_metadata;
            }
            catch (Exception ex) {
                // JSON parsing error
                this.errorLogger().warning("lookupEdgeXDeviceDetails: JSON parsing error: " + ex.getMessage(),ex);
            }
       }
       else {
           // HTTP error
           this.errorLogger().warning("lookupEdgeXDeviceDetails: metadata lookup FAILED... Response Code: " + this.m_http.getLastResponseCode() + " URL: " + url);
       }
       return null;
    }
    
    // lookup our mbed device shadow for this edgex device
    private Map edgexDeviceToMbedDevice(String edgex_name) {
        // Get our mbed ID
        String mbed_id = this.m_db.lookupMbedName(edgex_name);
        if (mbed_id != null) {
            return this.m_db.getMbedDevice(mbed_id);
        }
        return null;
    }
}
