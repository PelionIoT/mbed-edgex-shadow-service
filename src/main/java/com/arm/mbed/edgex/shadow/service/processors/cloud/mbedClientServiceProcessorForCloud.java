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
package com.arm.mbed.edgex.shadow.service.processors.cloud;

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
public class mbedClientServiceProcessorForCloud extends mbedClientServiceProcessorBase implements mbedClientServiceProcessorInterface {
    // BYOC Support in Cloud
    private String m_mcs_cloud_cert_uri = "/cloud/certificates";
    private String m_mcs_cloud_creds_uri = "/cloud/credentials";
    private String m_mcs_certificate = null;
    private String m_mcs_certificate_id = null;
    
    // default constructor
    public mbedClientServiceProcessorForCloud(ErrorLogger error_logger, PreferenceManager preference_manager,String own_ip_address,int own_port) {
        super(error_logger, preference_manager, own_ip_address, own_port);
        
        // URI changes for Cloud...
        this.m_mcs_device_uri = "/cloud/devices";
        this.m_mcs_register_uri = "/registration";
    }
   
    // create our Cloud Certificate URL
    private String createCloudCertificateURL() {
        return "http://" + this.m_mcs_ip_address + ":" + this.m_mcs_port + this.m_mcs_cloud_cert_uri;
    }
    
    // create our Cloud Credentials URL
    private String createCloudCredentialsURL() {
        return "http://" + this.m_mcs_ip_address + ":" + this.m_mcs_port + this.m_mcs_cloud_creds_uri;
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
        HashMap<String,Object> request = new HashMap<>();
        
        // build out the create device JSON 
        request.put("credentialId",(String)mbed_device.get("credentialId"));
        
        // Convert to JSON string
        return Utils.removeArray(this.jsonGenerator().generateJson(request));
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
    
    // extract the certificate ID from the certificate data
    private String getCertificateID(String certificate_json) {
        return Utils.getStringElementFromJSON(this.errorLogger(),this.jsonParser(),certificate_json,"id");
    }
    
    // create the Cloud Certificate Request JSON
    private String createCloudCertificateRequestJSON(String api_key) {
        HashMap<String,Object> request = new HashMap<>();
        
        // create the JSON
        request.put("apiKey",(String)api_key);
        request.put("useBootstrap", (Boolean)true);
        request.put("name",(String)"EdgeX Shadow Service Device Certificate");
        request.put("description",(String)"Certificate used in EdgeX Shadow Devices");
        
        // return the json string
        return Utils.removeArray(this.jsonGenerator().generateJson(request));
    }
    
    // create the Cloud Certificate Request JSON
    private String createCloudCredentialsRequestJSON(String cert_id,Map mbed_device,Map edgex_device) {
        HashMap<String,Object> request = new HashMap<>();
        
        // create the JSON
        request.put("certificateId",cert_id);
        request.put("endpointName",edgex_device.get("name"));
        request.put("serialNumber","" + Utils.createRandomNumber());
        
        // return the json string
        return Utils.removeArray(this.jsonGenerator().generateJson(request));
    }
    
    // create the Cloud Certificate
    private String createCloudCertificate() {
        // create our Certificate URL
        String url = this.createCloudCertificateURL();
        
        // Create the Certificate request JSON
        String cert_request_json = this.createCloudCertificateRequestJSON(this.m_mcs_access_key);
        
        // Invoke the POST for creating the certificate
        String response = this.m_http.httpPost(url, cert_request_json);
        
        // DEBUG
        this.errorLogger().info("createCloudCertificate: Request: " + cert_request_json + " response: " + response);
        this.errorLogger().info("createCloudCertificate: URL: " + url);

        // 2i: Note the result
        if (this.m_http.getLastResponseCode() < 300) {
            // SUCCESS for certificate creation
            this.errorLogger().info("createCloudCertificate: certificate creation SUCCESS. status: " + this.m_http.getLastResponseCode());
            
            // return the response
            return response;
        }
        else {
            // FAILURE for the ith resource addition
            this.errorLogger().warning("createCloudCertificate: certficiate creation FALURE. status: " + this.m_http.getLastResponseCode());
        }
        return null;
    }
    
    // create the Cloud Client Creds
    private Map createCloudDeviceCredentials(String cert_id,Map mbed_device,Map edgex_device) {
        // create the Credentials URL
        String url = this.createCloudCredentialsURL();
        
        // create the Device Credentials request JSON
        String device_cred_request_json = this.createCloudCredentialsRequestJSON(cert_id,mbed_device,edgex_device);
        
        // Invoke the POST for creating the certificate
        String response = this.m_http.httpPost(url, device_cred_request_json);
        
        // DEBUG
        this.errorLogger().info("createCloudDeviceCredentials: Request: " + device_cred_request_json + " response: " + response);
        this.errorLogger().info("createCloudDeviceCredentials: URL: " + url);
        this.errorLogger().info("createCloudCertificate: mbed device: " + mbed_device);

        // 2i: Note the result
        if (this.m_http.getLastResponseCode() < 300) {
            // SUCCESS for certificate creation
            this.errorLogger().info("createCloudDeviceCredentials: device credential creation SUCCESS. status: " + this.m_http.getLastResponseCode());
            
            // extract the ID from the cred
            String id = Utils.getStringElementFromJSON(this.errorLogger(),this.jsonParser(),response.replaceAll("null","\"empty\""), "id");
            
            // add to the device credential
            mbed_device.put("credentialId",id);
        }
        else {
            // FAILURE for the ith resource addition
            this.errorLogger().warning("createCloudDeviceCredentials: device credential creation FALURE. status: " + this.m_http.getLastResponseCode());
        }
        
        // return the mbed device map
        return mbed_device;
    }
    
    // create credentials for an mbed device
    @Override
    protected Map createDeviceCredentials(Map mbed_device,Map edgex_device) {
        boolean cached = false;
        
        // initialize the certificate if needed (i.e. not present in the cache already...)
        if (this.m_mcs_certificate == null) {
            // try to restore from cache first
            this.m_mcs_certificate = this.m_db.getCloudCert();
            
            // if we do not have a cached copy... create a certificate...
            if (this.m_mcs_certificate == null) {
                // ask mCS/mbed Cloud for the certificate...
                this.m_mcs_certificate = this.createCloudCertificate();
            }
            else {
                // its already cached
                cached = true;
                
                // DEBUG
                this.errorLogger().info("createDeviceCredentials: device certificate imported from cache SUCCESSFUL");
            }
        }
            
        // parse the JSON and get the certificate ID
        this.m_mcs_certificate_id = this.getCertificateID(this.m_mcs_certificate);

        // if not cached & if we can parse the JSON successfully, lets cache the certificate for later...
        if (!cached && this.m_mcs_certificate_id != null && this.m_mcs_certificate_id.length() > 0) {
            // cache it!
            this.m_db.setCloudCert(this.m_mcs_certificate);

            // save it!
            this.m_db.saveCloudCert();
        }
        
        // make sure we have a certificate ID...
        if (this.m_mcs_certificate_id != null) {
            // now create the Cloud Credentials
            return this.createCloudDeviceCredentials(this.m_mcs_certificate_id,mbed_device,edgex_device);
        }
        else {
            // FAILURE for the ith resource addition
            this.errorLogger().warning("createDeviceCredentials: device credential creation FALURE: No device certificate created/loaded.");
        }
        return null;
    }
}
