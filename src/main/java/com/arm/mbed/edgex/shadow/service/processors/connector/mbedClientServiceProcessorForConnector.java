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
package com.arm.mbed.edgex.shadow.service.processors.connector;

import com.arm.mbed.edgex.shadow.service.processors.*;
import com.arm.mbed.edgex.shadow.service.core.ErrorLogger;
import com.arm.mbed.edgex.shadow.service.core.Utils;
import com.arm.mbed.edgex.shadow.service.interfaces.mbedClientServiceProcessorInterface;
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * mbed Client Service Event Processor
 * @author Doug Anson
 */
public class mbedClientServiceProcessorForConnector extends mbedClientServiceProcessorBase implements mbedClientServiceProcessorInterface {
    // default constructor
    public mbedClientServiceProcessorForConnector(ErrorLogger error_logger, PreferenceManager preference_manager,String own_ip_address,int own_port) {
        super(error_logger, preference_manager, own_ip_address, own_port);
    }
    
    // create our OWN webhook URL 
    @Override
    protected String createOwnCallbackURL() {
        return "http://" + this.m_own_ip_address + ":" + this.m_own_port + this.m_own_webhook_uri;
    }
    
    // create our mCS webhook Eventing URL
    @Override
    protected String createWebhookEventingURL() {
        return "http://" + this.m_mcs_ip_address + ":" + this.m_mcs_port + this.m_mcs_webhook_events_uri;
    }
    
    // create the devices URI
    @Override
    protected String createMCSDevicesURL() {
        return "http://" + this.m_mcs_ip_address + ":" + this.m_mcs_port + this.m_mcs_device_uri;
    }
    
    // create the device resources URI
    @Override
    protected String createMCSDeviceResourcesURL(String id) {
        return "http://" + this.m_mcs_ip_address + ":" + this.m_mcs_port + this.m_mcs_device_uri + "/" + id + this.m_mcs_resources_uri;
    }
    
    // create the device registration URI
    @Override
    protected String createMCSDeviceRegisterURL(String id) {
        return "http://" + this.m_mcs_ip_address + ":" + this.m_mcs_port + this.m_mcs_device_uri + "/" + id + this.m_mcs_register_uri;
    }
    
    // create the authorization JSON
    @Override
    protected String createAuthJSON(Map mbed_device) {
        String ept = (String)mbed_device.get("ept");
        
        // Create the JSON as a Map
        HashMap<String,String> map = new HashMap<>();
        map.put("accessKey", this.m_mcs_access_key);
        map.put("domain",this.m_mcs_domain_id);
        map.put("service",this.m_mcs_service_name);
        if (ept != null && ept.length() > 0) {
            map.put("type",ept);
        }
        
        // Convert to JSON string
        return Utils.removeArray(this.jsonGenerator().generateJson(map));
    }
    
    // create the mCS resource JSON
    // Example FORMAT: {"path":"/3201/0/5853","operation":["GET","PUT"],"valueType":"dynamic","value":"500:500","observable":false}
    @Override
    protected Map createResourceJSON(Map resource) {
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
    
    // create credentials for an mbed device
    @Override
    protected Map createDeviceCredentials(Map mbed_device,Map edgex_device) {
        // not used in Connector
        return mbed_device;
    }
}
