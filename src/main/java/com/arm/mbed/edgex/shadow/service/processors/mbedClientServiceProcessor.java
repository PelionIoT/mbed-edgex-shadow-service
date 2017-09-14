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

import com.arm.mbed.edgex.shadow.service.core.BaseClass;
import com.arm.mbed.edgex.shadow.service.core.ErrorLogger;
import com.arm.mbed.edgex.shadow.service.interfaces.mbedClientServiceProcessorInterface;
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;
import com.arm.mbed.edgex.shadow.service.processors.cloud.mbedClientServiceProcessorForCloud;
import com.arm.mbed.edgex.shadow.service.processors.connector.mbedClientServiceProcessorForConnector;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * mbed Client Service Event Processor
 * @author Doug Anson
 */
public class mbedClientServiceProcessor extends BaseClass implements mbedClientServiceProcessorInterface {
    
    private String m_mcs_service_name = null;
    private mbedClientServiceProcessorInterface m_mcs = null;
    
    // default constructor
    public mbedClientServiceProcessor(ErrorLogger error_logger, PreferenceManager preference_manager,String own_ip_address,int own_port) {
        super(error_logger, preference_manager);
        
        // determine the type of mCS service we are using... 
        this.m_mcs_service_name = preference_manager.valueOf("mcs_service_name");
        if (this.m_mcs_service_name != null && this.m_mcs_service_name.equalsIgnoreCase("cloud") == true) {
            // mbed Cloud 
            this.errorLogger().info("mbedClientServiceProcessor: mbed Cloud used");
            this.m_mcs = new mbedClientServiceProcessorForCloud(error_logger,preference_manager,own_ip_address,own_port);
        }
        if (this.m_mcs_service_name != null && this.m_mcs_service_name.equalsIgnoreCase("connector") == true) {
            // mbed Connector
            this.errorLogger().info("mbedClientServiceProcessor: mbed Connector used");
            this.m_mcs = new mbedClientServiceProcessorForConnector(error_logger,preference_manager,own_ip_address,own_port);
        }
        
        // sanity check
        if (this.m_mcs == null) {
            this.errorLogger().critical("mbedClientServiceProcessor: ERROR: Invalid configuration(mcs_service_name)... Valid values: cloud,connector. Current value: " + this.m_mcs_service_name);
        }
    }

    // set our EdgeX Event Processor
    @Override
    public void setEdgeXEventProcessor(EdgeXEventProcessor edgex) {
        if (this.m_mcs != null) {
            this.m_mcs.setEdgeXEventProcessor(edgex);
        }
    }
    
    // get our EdgeX Event Processor
    @Override
    public EdgeXEventProcessor edgeXEventProcessor() {
        if (this.m_mcs != null) {
            return this.m_mcs.edgeXEventProcessor();
        }
        return null;
    }
    
    // initialize the mbed Client service processor
    @Override
    public boolean initialize() {
        if (this.m_mcs != null) {
            return this.m_mcs.initialize();
        }
        return false;
    }
    
    // process mCS events
    @Override
    public void processEvent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (this.m_mcs != null) {
            this.m_mcs.processEvent(request, response);
        }
    }
    
    // validate a specific mbed endpoint
    @Override
    public boolean validate(String mbed_id,String edgex_name) {
        if (this.m_mcs != null) {
            return this.m_mcs.validate(mbed_id,edgex_name);
        }
        return false;
    }
    
    // closedown the mbed Client service processor
    @Override
    public void closedown() {
        if (this.m_mcs != null) {
            this.m_mcs.closedown();
        }
    }
    
    // send an observation
    @Override
    public boolean sendObservation(Map edgex_message) {
        if (this.m_mcs != null) {
            return this.m_mcs.sendObservation(edgex_message);
        }
        return false;
    }
    
    // is this device mapped/shadowed?
    @Override
    public boolean deviceShadowed(Map edgex_message) {
        if (this.m_mcs != null) {
            return this.m_mcs.deviceShadowed(edgex_message);
        }
        return false;
    }
    
    // create a device shadow
    @Override
    public boolean createDeviceShadow(Map edgex_message) {
        if (this.m_mcs != null) {
            return this.m_mcs.createDeviceShadow(edgex_message);
        }
        return false;
    }
    
    // map a single EdgeX resource to its equivalent mbed Resource
    @Override
    public String mapMbedResourcePathToEdgeXResource(String mbed_path) {
        if (this.m_mcs != null) {
            return this.m_mcs.mapMbedResourcePathToEdgeXResource(mbed_path);
        }
        return null;
    }
    
    // map a single EdgeX resource to its equivalent mbed Resource
    @Override
    public String mapEdgeXResourceToMbedResource(String edgex_resource) {
        if (this.m_mcs != null) {
            return this.m_mcs.mapEdgeXResourceToMbedResource(edgex_resource);
        }
        return null;
    }
    
    // lookup our mbed device shadow for this edgex device
    @Override
    public Map mbedDeviceToEdgeXDevice(String mbed_name) {
        if (this.m_mcs != null) {
            return this.m_mcs.mbedDeviceToEdgeXDevice(mbed_name);
        }
        return null;
    }
}
