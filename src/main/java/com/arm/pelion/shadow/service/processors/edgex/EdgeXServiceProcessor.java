/**
 * @file EdgeXServiceProcessor.java
 * @brief EdgeX Service Processor
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
package com.arm.pelion.shadow.service.processors.edgex;

import com.arm.pelion.shadow.service.core.BaseClass;
import com.arm.pelion.shadow.service.core.ErrorLogger;
import com.arm.pelion.shadow.service.core.Transport.ReceiveListener;
import com.arm.pelion.shadow.service.core.TransportReceiveThread;
import com.arm.pelion.shadow.service.core.Utils;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import com.arm.pelion.shadow.service.transport.MQTTTransport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import com.arm.pelion.shadow.service.interfaces.DeviceShadowProcessorInterface;

/**
 * EdgeX Service Processor
 * @author Doug Anson
 */
public class EdgeXServiceProcessor extends BaseClass implements ReceiveListener {
    private MQTTTransport m_mqtt = null;
    private String m_mqtt_hostname = null;
    private int m_mqtt_port = 1883;
    private String m_mqtt_event_topic = null;
    private DeviceShadowProcessorInterface m_msp = null;
    private TransportReceiveThread m_receive = null;
    
    // Configuration
    private String m_edgex_ip_address = null;
    private int m_edgex_metadata_port = 0;
    private String m_edgex_metadata_uri = null;
    
    private int m_edgex_resource_values_port = 0;
    private String m_edgex_value_request_uri = null;
    private String m_edgex_value_set_request_uri = null;

    // base constructor
    public EdgeXServiceProcessor(ErrorLogger error_logger, PreferenceManager preference_manager,DeviceShadowProcessorInterface msp) {
        super(error_logger, preference_manager);
        
        // set our mbed shadow processor
        this.m_msp = msp;
        
        // tie to edgex event processor
        this.m_msp.setEdgeXEventProcessor(this);
        
        // create the MQTT transport
        this.m_mqtt = new MQTTTransport(error_logger, preference_manager);
        
        // create the receive thread
        this.m_receive = new TransportReceiveThread(this.m_mqtt);
        
        // gather the configuration
        this.m_mqtt_hostname = preference_manager.valueOf("mqtt_hostname");
        this.m_mqtt_port = preference_manager.intValueOf("mqtt_port");
        this.m_mqtt_event_topic = preference_manager.valueOf("mqtt_edgex_events_topic");
        
        // EdgeX configuration
        this.m_edgex_ip_address = preference_manager.valueOf("edgex_ip_address");
        this.m_edgex_metadata_port = preference_manager.intValueOf("edgex_metadata_port");
        this.m_edgex_metadata_uri = preference_manager.valueOf("edgex_metadata_uri");
        
        // EdgeX resource values editing
        this.m_edgex_resource_values_port = preference_manager.intValueOf("edgex_resource_values_port");
        this.m_edgex_value_request_uri = preference_manager.valueOf("edgex_value_request_uri");
        this.m_edgex_value_set_request_uri = preference_manager.valueOf("edgex_value_set_request_uri");
        
        // announce
        this.errorLogger().warning("EdgeXServiceProcessor installed. Date: " + Utils.dateToString(Utils.now()));
    }
    
    // get our mCS processor
    public DeviceShadowProcessorInterface mcsProcessor() {
        return this.m_msp;
    }
    
    // initialize
    public boolean initialize() {
        // setup the default credentials for connecting to the MQTT broker
        this.m_receive.setOnReceiveListener(this);
        boolean connect = this.m_mqtt.connect(this.m_mqtt_hostname,this.m_mqtt_port);
        if (connect == true) {
            // success!
            this.errorLogger().warning("EdgeXServiceProcessor: MQTT Connection (" + this.m_mqtt_hostname + ":" + this.m_mqtt_port + ") successful");
        
            // subscribe to the events topic
            Topic[] topics = new Topic[1];
            topics[0] = new Topic(this.m_mqtt_event_topic,QoS.AT_LEAST_ONCE);
            
            // DEBUG
            this.errorLogger().warning("EdgeXServiceProcessor: subscribing to: " + this.m_mqtt_event_topic + "...");
            this.m_mqtt.subscribe(topics);
            
            // start listening
            this.m_receive.start();
        }
        else {
            // failure
            this.errorLogger().warning("EdgeXServiceProcessor: MQTT Connection (" + this.m_mqtt_hostname + ":" + this.m_mqtt_port + ") FAILED");
        }
        
        // return the connection status
        return connect;
    }
    
    // create the EdgeX resource SET request URL
    private String buildEdgeXResourceValueSetRequestURL(Map edgex_device,String edgex_resource) {
        return "http://" + this.m_edgex_ip_address + ":" + this.m_edgex_resource_values_port + this.m_edgex_value_set_request_uri + edgex_resource;
    }
    
    // create the EdgeX resource value request URL
    private String buildEdgeXResourceValueRequestURL(Map edgex_device,String edgex_resource) {
        return "http://" + this.m_edgex_ip_address + ":" + this.m_edgex_resource_values_port + this.m_edgex_value_request_uri + edgex_resource;
    }
    
    // create the metadata URL for device info retrieval (NOTE: EdgeX device NAME must be used... ID will not work)
    public String buildEdgeXMetadataURL(String edgex_dev_name) {
        return "http://" + this.m_edgex_ip_address + ":" + this.m_edgex_metadata_port + this.m_edgex_metadata_uri + edgex_dev_name;
    }
    
    // closedown the mbed Client service processor
    public void closedown() {
        if (this.m_mqtt != null) {
            if (this.m_mqtt.isConnected()) {
                this.m_mqtt.disconnect(true);
            }
        }
    }

    @Override
    public void onMessageReceive(String topic, String message) {
        boolean processed = false;
        
        // make sure its our topic
        if (topic != null && topic.equalsIgnoreCase(this.m_mqtt_event_topic) == true) {
           // DEBUG
           this.errorLogger().warning("EdgeXServiceProcessor: onMessageReceive: topic: " + topic + " message: " + message);
           
           try {
                // Parse the JSON
                Map edgex_message = this.jsonParser().parseJson(message);

                // is the device shadowed already?
                if (this.m_msp.deviceShadowed(edgex_message) == false) {
                    // first we have to create the device shadow
                    this.errorLogger().info("EdgeXServiceProcessor: Creating device shadow for EdgeX ID: " + edgex_message.get("id"));
                    if (this.m_msp.createDeviceShadow(edgex_message) == true) {
                        // now that device shadow is created... send an observation for it
                        this.errorLogger().info("EdgeXServiceProcessor: Creating device shadow for EdgeX ID: " + edgex_message.get("id") + " SUCCESS");   
                    }
                    else {
                        // failed to create the device shadow
                        this.errorLogger().warning("EdgeXServiceProcessor: Creating device shadow for EdgeX ID: " + edgex_message.get("id") + " FAILED");
                    }
                }

                // should always be shadowed now!
                if (this.m_msp.deviceShadowed(edgex_message) == true) {
                    // DEBUG
                    this.errorLogger().info("EdgeXServiceProcessor: sending observation for EdgeX ID: " + edgex_message.get("id") + "...");

                    // already shadowed - so this is just an observation...
                    processed = this.m_msp.sendObservation(edgex_message);
                }
                else {
                    // ignore - unable to create a shadow...
                }
           }
           catch (Exception ex) {
               // Parse error
               this.errorLogger().warning("EdgeXServiceProcessor: JSON parse error: " + ex.getMessage(),ex);
           }
       }
       else {
           // ignore this message
           this.errorLogger().info("EdgeXServiceProcessor: onMessageReceive IGNORE: topic: " + topic + " message: " + message);
       }
    }
    
    // trim and reduce the EdgeX metadata that we are interested in...
    public Map trimEdgeXMetadata(Map raw_edgex_metadata) {
        // Create a new HashMap
        HashMap<String,Object> edgex_metadata = new HashMap<>();
        
        // Pull only key values we need for this metadata map...
        edgex_metadata.put("id",(String)raw_edgex_metadata.get("id"));
        edgex_metadata.put("name",(String)raw_edgex_metadata.get("name"));
        
        // Endpoint "type", if present, should be in key "device_type" 
        //edgex_metadata.put("device_type", null);
        
        // snag the profile JSON
        Map profile = (Map)raw_edgex_metadata.get("profile");
        
        // get the device resource array... record its length...
        List resources = (List)profile.get("deviceResources");
        edgex_metadata.put("num_resources",resources.size());
        
        // create the trimmed resource array list
        ArrayList<HashMap<String,String>> trimmed_resource_list = new ArrayList<>();
        
        // loop through the device resource array and build our "trimmed" version...
        for(int i=0;resources != null && i < resources.size();++i) {
            // Create a HashMap for the ith dev resource
            HashMap<String,String> trimmed_resource = new HashMap<>();
            
            // Get the ith dev resource
            Map resource = (Map)resources.get(i);
            
            // Get the ith dev resource properties
            Map properties = (Map)resource.get("properties");
            
            // Get the ith dev resource properties units info
            Map units = (Map)properties.get("units");
            
            // Get the ith dev resource properties values info
            Map values = (Map)properties.get("value");
            
            // Save the EdgeX resource name
            trimmed_resource.put("name",(String)resource.get("name"));
            
            // Save the EdgeX resource description
            trimmed_resource.put("description",(String)resource.get("description"));
            
            // Save the EdgeX units and values type information
            trimmed_resource.put("uom",(String)units.get("defaultValue"));
            trimmed_resource.put("rw",(String)units.get("readWrite"));
            trimmed_resource.put("type",(String)values.get("type"));
            trimmed_resource.put("value",(String)values.get("defaultValue"));
            
            // Save the trimmed resource to the trimmed resource list
            trimmed_resource_list.add(trimmed_resource);
        }
        
        // save off the trimmed array list...
        edgex_metadata.put("resources",(List)trimmed_resource_list);
        
        // return the trimmed metadata map
        return edgex_metadata;
    }
    
    // get a resources value for a given edgex device
    public String getDeviceResourceValue(Map edgex_device,String edgex_resource,Object data) {
        HashMap<String,Object> response = new HashMap<>();
        
        // GET the EdgeX resource value...
        String edgex_resource_value = this.getEdgeXResourceValue(edgex_device,edgex_resource);
        if (this.m_http.getLastResponseCode() < 300 && edgex_resource_value != null) {
            response.put("data",edgex_resource_value);
            response.put("success",(Boolean)true);
        }
        else {
            // unable to GET resource value
            this.errorLogger().warning("EdgeXServiceProcessor: Unable to GET resource value for: " + edgex_device.get("name") + " resource: " + edgex_resource + " status: " + this.m_http.getLastResponseCode());
        }
        
        // return the response as a JSON
        return this.jsonGenerator().generateJson(response);
    }
    
    // update a resources value for a given edgex device
    public String updateDeviceResourceValue(Map edgex_device,String edgex_resource,Object data) {
        HashMap<String,Object> response = new HashMap<>();
        
        // PUT the EdgeX resource value...
        String put_response = this.setEdgeXResourceValue(edgex_device,edgex_resource,(String)data);
        if (this.m_http.getLastResponseCode() < 300) {
            response.put("data",put_response);
            response.put("success",(Boolean)true);
        }
        else {
            // unable to PUT value to resource
            this.errorLogger().warning("EdgeXServiceProcessor: Unable to PUT EdgeX device: " + edgex_device.get("name") + " resource: " + edgex_resource + " with value: " + data + " status: " + this.m_http.getLastResponseCode());
        }
        
        // return the response as a JSON
        return this.jsonGenerator().generateJson(response);
    }
    
    // process a PUT request to a specific EdgeX device
    private String setEdgeXResourceValue(Map edgex_device,String edgex_resource,String value) {
        // create the URL to dispatch the GET request downstream within EdgeX
        String url = this.buildEdgeXResourceValueSetRequestURL(edgex_device,edgex_resource);
        
        // execute the request via HTTP
        String response = this.m_http.httpPut(url,value);
        if (this.m_http.getLastResponseCode() < 300) {
            // success!
            this.errorLogger().info("EdgeXServiceProcessor: resource: " + edgex_resource + " set to value: " + value);
        }
        else {
            // ERROR
            this.errorLogger().warning("EdgeXServiceProcessor: Unable to PUT resource: " + edgex_resource + " with value: " + value);
            response = null;
        }
        
        // return the response
        return response;
    }
    
    // process a GET request to a specific EdgeX device
    private String getEdgeXResourceValue(Map edgex_device,String edgex_resource) {
        String value = null;
        
        // create the URL to dispatch the GET request downstream within EdgeX
        String url = this.buildEdgeXResourceValueRequestURL(edgex_device,edgex_resource);
        
        // execute the request via HTTP
        value = this.m_http.httpGet(url);
        if (this.m_http.getLastResponseCode() < 300) {
            // success!
            this.errorLogger().info("EdgeXServiceProcessor: resource: " + edgex_resource + " value: " + value);
        }
        else {
            // ERROR
            this.errorLogger().warning("EdgeXServiceProcessor: Unable to GET value for resource: " + edgex_resource);
            value = null;
        }
        
        // return the value
        return value;
    }
}
