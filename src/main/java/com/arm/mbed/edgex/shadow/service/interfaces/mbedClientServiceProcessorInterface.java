/**
 * @file    mbedClientServiceProcessorInterface.java
 * @brief mbed Client Service Interface
 * @author Doug Anson
 * @version 1.0
 * @see
 *
 * Copyright 2017. ARM Ltd. All rights reserved.
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
package com.arm.mbed.edgex.shadow.service.interfaces;

import com.arm.mbed.edgex.shadow.service.processors.EdgeXEventProcessor;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * mbedClientServiceProcessorInterface
 * @author Doug Anson
 */
public interface mbedClientServiceProcessorInterface {
    // set our EdgeX Event Processor
    public void setEdgeXEventProcessor(EdgeXEventProcessor edgex);
    
    // get our EdgeX Event Processor
    public EdgeXEventProcessor edgeXEventProcessor();
    
    // initialize the mbed Client service processor
    public boolean initialize();
    
    // process mCS events
    public void processEvent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
    
    // validate a specific mbed endpoint
    public boolean validate(String mbed_id,String edgex_name);
    
    // closedown the mbed Client service processor
    public void closedown();
    
    // send an observation
    public boolean sendObservation(Map edgex_message);
    
    // is this device mapped/shadowed?
    public boolean deviceShadowed(Map edgex_message);
    
    // create a device shadow
    public boolean createDeviceShadow(Map edgex_message);
    
    // map a single EdgeX resource to its equivalent mbed Resource
    public String mapMbedResourcePathToEdgeXResource(String mbed_path);
    
    // map a single EdgeX resource to its equivalent mbed Resource
    public String mapEdgeXResourceToMbedResource(String edgex_resource);
    
    // lookup our mbed device shadow for this edgex device
    public Map mbedDeviceToEdgeXDevice(String mbed_name);
}
