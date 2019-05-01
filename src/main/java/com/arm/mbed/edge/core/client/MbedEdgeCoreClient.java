/**
 * @file MbedEdgeCoreClient.java
 * @brief mbed-edge core client API
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
package com.arm.mbed.edge.core.client;

import com.arm.mbed.edgex.shadow.service.core.BaseClass;
import com.arm.mbed.edgex.shadow.service.core.ErrorLogger;
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;

/**
 * Mbed Edge Core Client API for Java
 * @author Doug Anson
 */
public class MbedEdgeCoreClient extends BaseClass implements Runnable {

    // default constructor
    public MbedEdgeCoreClient(ErrorLogger error_logger, PreferenceManager preference_manager) {
        super(error_logger, preference_manager);
    }
    
    // initializer
    public void initialize(Object parent) {
        
    }

    // Runnable
    @Override
    public void run() {
    }
    
}
