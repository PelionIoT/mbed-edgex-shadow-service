/**
 * @file mbedEdgeCoreServiceProcessor.java
 * @brief mbed-edge core service processor
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
package com.arm.mbed.edgex.shadow.service.processors.edgecore;

import com.arm.edgex.shadow.service.db.mbedDeviceShadowDatabase;
import com.arm.mbed.edgex.shadow.service.core.BaseClass;
import com.arm.mbed.edgex.shadow.service.core.ErrorLogger;
import com.arm.mbed.edgex.shadow.service.core.Utils;
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;
import com.arm.mbed.edgex.shadow.service.processors.edgex.EdgeXServiceProcessor;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.arm.mbed.edgex.shadow.service.interfaces.mbedShadowProcessorInterface;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * mbed Edge Core Service Processor
 * @author Doug Anson
 */
public class mbedEdgeCoreServiceProcessor extends BaseClass implements mbedShadowProcessorInterface {
    // EdgeX Service processor
    private EdgeXServiceProcessor m_edgex = null;
    
    // default EPT
    private String m_default_shadow_ept = null;

    // cache database
    protected mbedDeviceShadowDatabase m_db = null;
    
    // default constructor
    public mbedEdgeCoreServiceProcessor(ErrorLogger error_logger, PreferenceManager preference_manager) {
        super(error_logger, preference_manager);
        
        // create the shadow database
        this.m_db = new mbedDeviceShadowDatabase(error_logger,preference_manager);
        
        // get the default endpoint type for shadow devices
        this.m_default_shadow_ept = preference_manager.valueOf("mbed_default_ept");
        
        // announce
        this.errorLogger().warning("mbedEdgeCoreServiceProcessor installed. Date: " + Utils.dateToString(Utils.now()));
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
    public boolean initialize() {
        this.errorLogger().info("mbedEdgeCoreServiceProcessor: in initialize()...");
         return this.m_db.initialize(this);
    }
    
    // process an mCS event
    private HashMap<String,Object> processEvent(Map request) {
        HashMap<String,Object> response = new HashMap<>();
        HashMap<String,String> headers = new HashMap<>();

        // set defaults
        response.put("content-type", "application/json");
        response.put("headers",headers);
        response.put("data", "{}");

        // lets process the event and get any answers back from EdgeX
        String response_json = this.edgeXEventProcessor().processEvent(request);
        if (response_json != null && response_json.length() > 0) {
            response.put("data",response_json);
        }

        // return the response
        return response;
    }
    
    // send the REST response back to mDS
    private void sendResponse(HttpServletResponse response, String content_type, HashMap<String,String> header, String body) {
        try {
            response.setContentType(content_type);
            response.setHeader("Pragma","no-cache");
            if (header != null) {
                for(HashMap.Entry<String,String> entry : header.entrySet()) {
                    response.addHeader(entry.getKey(),entry.getValue());
                }
            }
            try (PrintWriter out = response.getWriter()) {
                if (body != null && body.length() > 0) {
                    out.println(body);
                }
            }
        }
        catch (Exception ex) {
            // error - unable to send reply back to mCS
            this.errorLogger().critical("sendResponse: Unable to send response: " + ex.getMessage(), ex);
        }
    }

    // read in the request data
    private String readRequestData(HttpServletRequest request) {
        String data = null;

        try {
            BufferedReader reader = request.getReader();
            String line = reader.readLine();
            StringBuilder buf = new StringBuilder();
            while (line != null) {
                buf.append(line);
                line = reader.readLine();
            }
            data = buf.toString();
        }
        catch (IOException ex) {
            // error in read
            this.errorLogger().warning("readRequestData: Exception caught during request READ: " + ex.getMessage());
            data = null;
        }

        // return the data
        return data;
    }
    
     // validate the mbed device via mCS
    private boolean validateMbedDevice(String mbed_id) {
        boolean validated = false;

        // Lets query mCS and get this device details
        
        // XXXX
        String url = ""; // this.createMCSDevicesURL() + "/" + mbed_id;
        String response = "";  // this.m_http.httpGet(url);

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

    @Override
    public boolean validate(String mbed_id, String edgex_name) {
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

    @Override
    public void closedown() {
         // sync the cache to file if enabled
        boolean saved = this.m_db.saveToCache();
        if (saved == true) {
            // successfully cached mappings
            this.errorLogger().info("mbedEdgeCoreServiceProcessor:closedown: SUCCESS device mapping saved...");
        }
        else {
            // unable to cache mappings or disabled
            this.errorLogger().info("mbedEdgeCoreServiceProcessor:closedown: FAILURE device mapping NOT saved...");

            // go ahead and clear the cache
            this.m_db.clearCacheFiles();
        }
    }

    @Override
    public boolean sendObservation(Map edgex_message) {
        this.errorLogger().info("mbedEdgeCoreServiceProcessor: in sendObservation()... MAP: " + edgex_message);
        return true;
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

        // return our shadow status
        return shadowed;
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
    
    // create the shadow
    private Map createShadow(Map mbed_device) {
        this.errorLogger().info("in createShadow():  MAP: " + mbed_device);
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
        mbed_device = this.createShadow(mbed_device);
        if (mbed_device != null) {
            // DEBUG
            this.errorLogger().info("createMbedDeviceShadow: SUCCESS. mbed Device Shadow created: " + mbed_device);

            // return the mbed device
            return mbed_device;
        }
        else {
            if (mbed_device != null) {
                // ERROR in creating mbed device shadow
                this.errorLogger().warning("createMbedDeviceShadow: Unable to create mbed device shadow for: " + mbed_device);
            }
            else {
                // ERROR in creating mbed device shadow
                this.errorLogger().warning("createMbedDeviceShadow: Unable to create mbed device (no device map)");
            }
        }
        return null;
    }
        
    @Override
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
                // Unable to create shadow - no device map
                this.errorLogger().warning("createDeviceShadow: Unable to create shadow for EdgeX device: " + edgex_dev_name + " (no device map)");
            }
        }

        // return our creation status
        return created;
    }
    
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
}
