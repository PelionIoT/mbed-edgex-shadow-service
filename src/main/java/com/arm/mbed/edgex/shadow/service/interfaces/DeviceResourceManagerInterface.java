/**
 * @file    DeviceResourceManagerInterface.java
 * @brief Device Resource Manager Interface
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

/**
 * Device Resource Manager Interface
 * @author Doug Anson
 */
public interface DeviceResourceManagerInterface {
    // request device resource value
    public String getDeviceResource(String mbed_id, String mbed_resource_uri, Object new_value);
    
    // request device resource modification 
    public String updateDeviceResource(String mbed_id, String mbed_resource_uri, Object new_value);
}
