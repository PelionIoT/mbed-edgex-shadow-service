/**
 * @file PelionShadowServiceProcessor.java
 * @brief Pelion shadow service processor
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

import com.arm.pelion.edge.core.client.api.PelionEdgeCoreClientAPI;
import com.arm.pelion.rest.client.api.PelionRestClientAPI;
import com.arm.pelion.shadow.service.coordinator.Orchestrator;
import com.arm.pelion.shadow.service.db.mbedDeviceShadowDatabase;
import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.core.Utils;
import com.arm.pelion.shadow.service.interfaces.DeviceResourceManagerInterface;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import com.arm.pelion.shadow.service.processors.edgex.EdgeXServiceProcessor;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.arm.pelion.shadow.service.interfaces.DeviceShadowProcessorInterface;

/**
 * Pelion Shadow Service Processor
 * @author Doug Anson
 */
public class PelionShadowServiceProcessor extends BaseClass implements DeviceShadowProcessorInterface, DeviceResourceManagerInterface {
    // EdgeX Service processor
    private EdgeXServiceProcessor m_edgex = null;
    
    // Pelion Device Manager
    private PelionShadowServiceDeviceManager m_device_manager = null;
    
    // default EPT
    private String m_default_shadow_ept = null;

    // cache database
    private mbedDeviceShadowDatabase m_db = null;
    
    // Orchestrator
    private Orchestrator m_orchestrator = null;
    
    // default constructor
    public PelionShadowServiceProcessor(ErrorLogger error_logger, PreferenceManager preference_manager,Orchestrator orchestrator) {
        super(error_logger, preference_manager);
        this.m_orchestrator = orchestrator;
        
        // create the shadow database
        this.m_db = new mbedDeviceShadowDatabase(error_logger,preference_manager,orchestrator);
        
        // get the default endpoint type for shadow devices
        this.m_default_shadow_ept = preference_manager.valueOf("mbed_default_ept");
        
        // create the mbed edge core client API
        this.m_device_manager = new PelionShadowServiceDeviceManager(error_logger,preference_manager,this,orchestrator);
        
        // announce
        this.errorLogger().warning("PelionShadowServiceProcessor installed. Date: " + Utils.dateToString(Utils.now()));
    }

    @Override
    public boolean initialize() {
        this.errorLogger().info("PelionShadowServiceProcessor: in initialize()...");
        return this.m_db.initialize(this);
    }
    
    // validate the underlying connection
    @Override
    public void validateUnderlyingConnection() {
        this.m_device_manager.validateUnderlyingConnection();
    }
    
    // create the device shadow
    private Map createShadow(Map mbed_device) {
        // create the device shadow in pelion
        mbed_device = this.m_device_manager.createDevice(mbed_device);
        if (mbed_device != null) {
            this.errorLogger().info("PelionShadowServiceProcessor: Created Pelion device: " + mbed_device);
            
            // return the created device
            return mbed_device;
        }
        else {
            this.errorLogger().warning("PelionShadowServiceProcessor: ERROR creating Pelion device: " + mbed_device);
            return null;
        }
    }
    
    // remove the device shadow
    private boolean removeShadow(String mbed_id) {
        // remove the device shadow in pelion
        boolean removed = this.m_device_manager.deleteDevice(mbed_id);
        if (removed == true) {
            this.errorLogger().info("PelionShadowServiceProcessor: Removed Pelion device:" + mbed_id);
        }
        else {
            this.errorLogger().warning("PelionShadowServiceProcessor: ERROR removing Pelion device: " + mbed_id);
        }
        return removed;
    }
    
    // send an observation to the device shadow...
    @Override
    public boolean sendObservation(Map message) {
        boolean sent = true;
        
        // loop through the readings and process each one...
        List readings = (List)message.get("readings");
        for(int i=0;readings != null && i<readings.size();++i) {
            try {
                // parse the edgex_message into mbed device and resource detail
                Map reading = (Map)readings.get(i);
                String edgex_name = (String)reading.get("device");
                String edgex_resource = (String)reading.get("name");
                Object edgex_value = (Object)reading.get("value");
                String mbed_id = this.m_db.lookupMbedName(edgex_name);
                String mbed_resource_uri = this.mapEdgeXResourceToMbedResource(edgex_resource);
                Object new_value = edgex_value;

                // now send the observation into pelion if we have all of the data...
                if (mbed_id != null && mbed_resource_uri != null && new_value != null) {
                    // send the observation to pelion
                    sent = this.m_device_manager.processDeviceObservation(mbed_id, edgex_name, mbed_resource_uri, new_value);
                }
                else {
                    // error
                    this.errorLogger().warning("PelionShadowServiceProcessor: Unable to dispatch observation to Pelion (mapping issues) MBED_ID: " + mbed_id + " URI: " + mbed_resource_uri + " VALUE: " + new_value);
                    sent = false;
                }
            }
            catch (Exception ex) {
                // error
                this.errorLogger().warning("PelionShadowServiceProcessor: Unable to dispatch observation to Pelion (exception): " + ex.getMessage());
                sent = false;
            }
        }

        // return our send status
        return sent;
    }
    
    
    // shadow requests resource value "get"
    @Override
    public String getDeviceResource(String mbed_id, String mbed_resource_uri, Object new_value) {        
        // map the mbed_id to its edgex id
        Map edgex = this.mbedDeviceToEdgeXDevice(mbed_id);
        
        // map the mbed LWM2M resource URI to the edgex equivalent
        String edgex_resource_id = this.mapMbedResourcePathToEdgeXResource(mbed_resource_uri);
        
        // Call the EdgeX processor to process the modification request
        return this.m_edgex.updateDeviceResourceValue(edgex,edgex_resource_id,new_value);
    }
    
    // shadow requests resource modification/change
    @Override
    public String updateDeviceResource(String mbed_id, String mbed_resource_uri, Object new_value) {        
        // map the mbed_id to its edgex id
        Map edgex = this.mbedDeviceToEdgeXDevice(mbed_id);
        
        // map the mbed LWM2M resource URI to the edgex equivalent
        String edgex_resource_id = this.mapMbedResourcePathToEdgeXResource(mbed_resource_uri);
        
        // Call the EdgeX processor to process the modification request
        return this.m_edgex.updateDeviceResourceValue(edgex,edgex_resource_id,new_value);
    }
    
    // validate the mbed device via mCS
    private boolean validateMbedDevice(String mbed_id) {
        boolean exists = false;

        // query pelion via mbed edge 
        Map device = this.m_device_manager.deviceExists(mbed_id);
        if (device != null) {
            // device exists in Pelion
            this.errorLogger().info("PelionShadowServiceProcessor: mbed ID: " + mbed_id + " exists in Pelion");
        }
        else {
            // device does not exist in Pelion
            this.errorLogger().info("PelionShadowServiceProcessor: mbed ID: " + mbed_id + " does NOT exist in Pelion");
        }

        // return the validation status
        return exists;
    }
    
    // validate the EdgeX device in EdgeX
    private boolean validateEdgeXDevice(String edgex_name) {
        boolean validated = false;

        // lets call EdgeX and get the device metaeata record... if get something, we assume the device exists in EdgeX
        Map details = this.lookupEdgeXDeviceDetails(edgex_name);
        if (details != null) {
            // device exists in EdgeX
            this.errorLogger().info("PelionShadowServiceProcessor: EdgeX Name: " + edgex_name + " is a valid device in EdgeX: " + details);
            validated = true;
        }
        else {
            // device does not exist in EdgeX
            this.errorLogger().info("PelionShadowServiceProcessor: EdgeX Name: " + edgex_name + " is NOT known to EdgeX");
        }


        // return the validation status
        return validated;
    }

    @Override
    public boolean validate(String mbed_id, String edgex_name) {
        boolean validated = false;

        // First we validate the mbed device via mCS
        this.errorLogger().info("PelionShadowServiceProcessor: Validating that mbed-edge device is still in existance: " + mbed_id);
        if (this.validateMbedDevice(mbed_id) == true) {
            // now validate the EdgeX device via EdgeX
            validated = this.validateEdgeXDevice(edgex_name);
        }

        // return the validation status
        return validated;
    }

    @Override
    public void closedown() {
         // sync the cache to file if enabled
        boolean saved = this.m_db.saveToCache();
        if (saved == true) {
            // successfully cached mappings
            this.errorLogger().info("PelionShadowServiceProcessor:closedown: SUCCESS device mapping saved...");
            
            // close down the device manager
            this.m_device_manager.closedown();
        }
        else {
            // unable to cache mappings or disabled
            this.errorLogger().info("PelionShadowServiceProcessor:closedown: FAILURE device mapping NOT saved...");

            // go ahead and clear the cache
            this.m_db.clearCacheFiles();
        }
    }

    @Override
    public boolean deviceShadowed(Map edgex_message) {
        boolean shadowed = false;

        // Get the EdgeX device name
        String edgex_name = (String)edgex_message.get("device");

        Map mbed_device = this.edgexDeviceToMbedDevice(edgex_name);
        if (mbed_device != null) {
            // we have an entry
            shadowed = true;
        }
        
        // DEBUG
        this.errorLogger().info("PelionShadowServiceProcessor: deviceShadowed: DEV: " + mbed_device + " SHADOWED: " + shadowed + " Name: " + edgex_name);

        // return our shadow status
        return shadowed;
    }
    
    // initialize the mbed device map
    private Map initMbedDeviceMapFromEdgeMap(Map edgex_device) {
        HashMap<String,Object> mbed_device = new HashMap<>();
        
        // DEBUG
        this.errorLogger().info("initMbedDeviceMapFromEdgeMap: EDGEX_DEVICE: " + edgex_device);

        // duplicate the basic information for this device...
        mbed_device.put("num_resources",(Integer)edgex_device.get("num_resources"));

        // handle endpoint_type as a special case (should be as key "device_type" from EdgeX, if present)
        String ept = (String)edgex_device.get("device_type");
        if (ept == null) {
            ept = this.m_default_shadow_ept;
        }
        
        // grab device details
        mbed_device.put("ept",ept);
        mbed_device.put("ep",edgex_device.get("name"));
        mbed_device.put("edgex_id",edgex_device.get("id"));
        
        // DEBUG
        this.errorLogger().info("initMbedDeviceMapFromEdgeMap: MBED_DEVICE: " + mbed_device);

        // return the mbed device map
        return mbed_device;
    }

    // create the mbed device shadow for our EdgeX device
    private Map createMbedDeviceShadow(Map edgex_device) {
        Map mbed_device = null;

        // Initialize the mbed device map
        mbed_device = this.initMbedDeviceMapFromEdgeMap(edgex_device);

        // Convert the EdgeX resources to mbed resources
        mbed_device = this.mapEdgeXResourcesToMbedResources(mbed_device,edgex_device);
        
        // With the mbed device map now formed, call mCS to create this device
        Map final_mbed_device = this.createShadow(mbed_device);
        if (final_mbed_device != null) {
            // DEBUG
            this.errorLogger().info("PelionShadowServiceProcessor: SUCCESS. mbed Device Shadow created: " + final_mbed_device);

            // return the mbed device
            return final_mbed_device;
        }
        else {
            if (mbed_device != null) {
                // ERROR in creating mbed device shadow
                this.errorLogger().warning("PelionShadowServiceProcessor: Unable to create mbed device shadow for: " + mbed_device);
            }
            else {
                // ERROR in creating mbed device shadow
                this.errorLogger().warning("PelionShadowServiceProcessor: Unable to create mbed device (no device map)");
            }
        }
        return null;
    }
        
    @Override
    public boolean createDeviceShadow(Map edgex_message) {
         boolean created = false;

        // Get the EdgeX device name
        String edgex_dev_name = (String)edgex_message.get("device");

        // Get the resource information for the EdgeX device
        Map edgex_device = this.lookupEdgeXDeviceDetails(edgex_dev_name);
        if (edgex_device != null) {
            // create the mbed Shadow + its resources
            Map mbed_device = this.createMbedDeviceShadow(edgex_device);
            if (mbed_device != null) {
                // DEBUG
                this.errorLogger().info("PelionShadowServiceProcessor: DEVICE: " + mbed_device);
                
                // save this new device off in the cache
                String mbed_id = (String)mbed_device.get("id");
                this.m_db.addDevice(mbed_id, mbed_device, edgex_dev_name, edgex_device);

                // save the config to cache
                created = this.m_db.saveToCache();

                // DEBUG
                if (created == true) {
                    // Successfully cache the shadow!
                    this.errorLogger().info("PelionShadowServiceProcessor: SUCCESS. mbed shadow: " + mbed_id + " for EdgeX device: " + edgex_dev_name + " successfully cached");
                }
                else {
                    // Unable to cache the shadow
                    this.errorLogger().warning("PelionShadowServiceProcessor: FAILURE. mbed shadow: " + mbed_id + " for EdgeX device: " + edgex_dev_name + " NOT CACHED");
                }
            }
            else {
                // Unable to create shadow - no device map
                this.errorLogger().warning("PelionShadowServiceProcessor: Unable to create shadow for EdgeX device: " + edgex_dev_name + " (no device map)");
            }
        }

        // return our creation status
        return created;
    }
    
    private Map lookupEdgeXDeviceDetails(String edgex_dev_name) {
       // create the URL to EdgeX that retrieves the metadata for the device
       String url = this.m_edgex.buildEdgeXMetadataURL(edgex_dev_name);

       // call EdgeX to retrieve the metadata for the device
       //this.errorLogger().warning("lookupEdgeXDeviceDetails: URL: " + url);
       String edgex_metadata_str = this.m_http.httpGet(url);

       // make sure we got something back...
       if (this.m_http.getLastResponseCode() < 300 && edgex_metadata_str != null && edgex_metadata_str.length() > 0) {
            try {
                // DEBUG
                //this.errorLogger().info("lookupEdgeXDeviceDetails: EdgeX Metadata: " + edgex_metadata_str);

                // convert the JSON string to a Map
                Map raw_edgex_metadata = this.jsonParser().parseJson(edgex_metadata_str);

                // now... trim down the metadata and make it what we minimally need
                Map trimmed_edgex_metadata = this.m_edgex.trimEdgeXMetadata(raw_edgex_metadata);

                // DEBUG
                this.errorLogger().info("PelionShadowServiceProcessor: EdgeX Metadata: " + trimmed_edgex_metadata);

                // return the trimmed details
                return trimmed_edgex_metadata;
            }
            catch (Exception ex) {
                // JSON parsing error
                this.errorLogger().warning("PelionShadowServiceProcessor: JSON parsing error: " + ex.getMessage());
            }
       }
       else {
           // HTTP error
           this.errorLogger().warning("PelionShadowServiceProcessor: metadata lookup FAILED... Response Code: " + this.m_http.getLastResponseCode() + " URL: " + url);
       }
       return null;
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

     // map a single EdgeX resource to its equivalent mbed Resource
    @Override
    public String mapMbedResourcePathToEdgeXResource(String mbed_path) {
        // return the key whose value is given by our path value
        return this.getKeyForValue(mbed_path);
    }

    // map a single EdgeX resource to its equivalent mbed Resource
    @Override
    public String mapEdgeXResourceToMbedResource(String edgex_resource) {
        // use the preference store - key is the EdgeX resoure name... value is the mbed Resource URI (path)
        return this.prefValue(edgex_resource);
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

    // lookup our mbed device shadow for this edgex device
    @Override
    public Map mbedDeviceToEdgeXDevice(String mbed_name) {
        // Get our mbed ID
        String edgex_name = this.m_db.lookupEdgeXName(mbed_name);
        if (edgex_name != null) {
            return this.m_db.getEdgeXDevice(edgex_name);
        }
        return null;
    } 
    
    @Override
    public void setEdgeXEventProcessor(EdgeXServiceProcessor edgex) {
        this.m_edgex = edgex;
    }

    @Override
    public EdgeXServiceProcessor edgeXEventProcessor() {
        return this.m_edgex;
    }

    @Override
    public PelionRestClientAPI getPelionRestClientAPI() {
        return this.m_device_manager.getPelionRestClientAPI();
    }

    @Override
    public PelionEdgeCoreClientAPI getPelionEdgeCoreClientAPI() {
        return this.m_device_manager.getPelionEdgeCoreClientAPI();
    }
}
