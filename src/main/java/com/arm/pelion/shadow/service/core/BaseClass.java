/**
 * @file    BaseClass.java
 * @brief base class for connector bridge
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
package com.arm.pelion.shadow.service.core;

import com.arm.pelion.shadow.service.json.JSONGenerator;
import com.arm.pelion.shadow.service.json.JSONParser;
import com.arm.pelion.shadow.service.preferences.PreferenceManager;
import com.arm.pelion.shadow.service.transport.HttpTransport;

/**
 * Base Class for fundamental logging and preferenceManager support
 *
 * @author Doug Anson
 */
public class BaseClass {

    private ErrorLogger m_error_logger = null;
    private PreferenceManager m_preference_manager = null;
    private JSONParser m_parser = null;
    private JSONGenerator m_generator = null;
    protected String m_content_type = null;
    protected HttpTransport m_http = null;

    // constructor
    /**
     *
     * @param error_logger
     * @param preference_manager
     */
    public BaseClass(ErrorLogger error_logger, PreferenceManager preference_manager) {
        this.m_error_logger = error_logger;
        this.m_preference_manager = preference_manager;
        this.m_parser = new JSONParser();
        this.m_generator = new JSONGenerator();
        if (preference_manager != null) {
            this.m_content_type = preference_manager.valueOf("content_type");
        }
        if (preference_manager != null) {
            this.m_http = (HttpTransport)preference_manager.getObjectHandle();
        }
    }
    
    // our defaulted content type
    protected String contentType() {
        return this.m_content_type;
    }
    
    // our JSON parser
    protected JSONParser jsonParser() {
        return this.m_parser;
    }
    
    // our JSON generator
    protected JSONGenerator jsonGenerator() {
        return this.m_generator;
    }

    // get our error handler
    /**
     *
     * @return
     */
    public com.arm.pelion.shadow.service.core.ErrorLogger errorLogger() {
        return this.m_error_logger;
    }

    // get the preferenceManager
    /**
     *
     * @return
     */
    public com.arm.pelion.shadow.service.preferences.PreferenceManager preferences() {
        return this.m_preference_manager;
    }

    /**
     *
     * @param key
     * @return
     */
    protected String prefValue(String key) {
        return this.prefValue(key, null);
    }
    
    /**
     *
     * @param value
     * @return
     */
    protected String getKeyForValue(String value) {
        return this.m_preference_manager.getKeyForValue(value);
    }

    /**
     *
     * @param key
     * @param suffix
     * @return
     */
    protected String prefValue(String key, String suffix) {
        if (this.m_preference_manager != null) {
            return this.m_preference_manager.valueOf(key, suffix);
        }
        return null;
    }

    /**
     *
     * @param key
     * @param def_value
     * @return
     */
    protected String prefValueWithDefault(String key, String def_value) {
        return this.prefValueWithDefault(key, null, def_value);
    }

    /**
     *
     * @param key
     * @param suffix
     * @param def_value
     * @return
     */
    protected String prefValueWithDefault(String key, String suffix, String def_value) {
        String value = this.prefValue(key, suffix);
        if (value != null && value.length() > 0) {
            return value;
        }
        return def_value;
    }

    /**
     *
     * @param key
     * @return
     */
    protected int prefIntValue(String key) {
        return this.prefIntValue(key, null);
    }

    /**
     *
     * @param key
     * @param suffix
     * @return
     */
    protected int prefIntValue(String key, String suffix) {
        if (this.m_preference_manager != null) {
            return this.m_preference_manager.intValueOf(key, suffix);
        }
        return -1;
    }

    /**
     *
     * @param key
     * @return
     */
    protected float prefFloatValue(String key) {
        return this.prefFloatValue(key, null);
    }

    /**
     *
     * @param key
     * @param suffix
     * @return
     */
    protected float prefFloatValue(String key, String suffix) {
        if (this.m_preference_manager != null) {
            return this.m_preference_manager.floatValueOf(key, suffix);
        }
        return (float) -1.0;
    }

    /**
     *
     * @param key
     * @return
     */
    protected boolean prefBoolValue(String key) {
        return this.prefBoolValue(key, null);
    }

    /**
     *
     * @param key
     * @param suffix
     * @return
     */
    protected boolean prefBoolValue(String key, String suffix) {
        if (this.m_preference_manager != null) {
            return this.m_preference_manager.booleanValueOf(key, suffix);
        }
        return false;
    }
}
