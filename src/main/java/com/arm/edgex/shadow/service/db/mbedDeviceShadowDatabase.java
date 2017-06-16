/**
 * @file    mbedDeviceShadowDatabase.java
 * @brief Device Shadow database/store
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
package com.arm.edgex.shadow.service.db;

import com.arm.mbed.edgex.shadow.service.core.BaseClass;
import com.arm.mbed.edgex.shadow.service.core.ErrorLogger;
import com.arm.mbed.edgex.shadow.service.core.Utils;
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;
import com.arm.mbed.edgex.shadow.service.processors.mbedClientServiceProcessor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * mbed Device Shadow Database
 * @author Doug Anson
 */
public class mbedDeviceShadowDatabase extends BaseClass {
    // restoration files
    private static String EDGEX_CACHE = "edgex_devices.ser";
    private static String ID_MAP_CACHE = "id_map.ser";
    private static String MBED_CACHE = "mbed_devices.ser";
    
    // enable/disable cache
    private boolean m_disable_cache = true;
    
    // Our DBs
    private HashMap<String,String> m_id_map_db = null;
    private HashMap<String,String> m_edgex_db = null;
    private HashMap<String,String> m_mbed_db = null;
    
    // default constructor
    public mbedDeviceShadowDatabase(ErrorLogger error_logger, PreferenceManager preference_manager) {
        super(error_logger, preference_manager);
        this.m_disable_cache = !(preference_manager.booleanValueOf("cache_enabled"));
    }
    
    // remove the cache files
    public void clearCacheFiles() {
        try {
            // delete EdgeX cache file
            File f = new File(EDGEX_CACHE);
            f.delete();
            
            // delete the mCS mbed cache file
            f = new File(MBED_CACHE);
            f.delete();
            
            // delete the ID mapping file
            f = new File(ID_MAP_CACHE);
            f.delete();
        }
        catch (Exception ex) {
            // silent exception
        }
    }
    
    // initialize the database
    public boolean initialize(mbedClientServiceProcessor processor) {
        boolean status = false;
        
        // reload edgex data from the file cache
        this.m_edgex_db = this.reloadFromCache(EDGEX_CACHE);
        if (this.m_edgex_db != null) {
            // get the mbed data from the file cache
            this.m_mbed_db = this.reloadFromCache(MBED_CACHE);
            if (this.m_mbed_db != null) {
                // get the ID map data from the file cache
                this.m_id_map_db = this.reloadFromCache(ID_MAP_CACHE);
            }
        }
        
        // continue if reloads succeed
        if (this.m_edgex_db != null && this.m_mbed_db != null && this.m_id_map_db != null) {
            // now revalidate each entry and prune any stale entries
            this.validateAndPrune(processor);
        }
        
        // write to cache to re-sync
        status = this.saveToCache();
        
        // return our status
        return status;
    }
    
    // add a new device
    public void addDevice(String mbed_id,Map mbed_json,String edgex_name,Map edgex_json) {
        // Stringify the JSON
        String mbed_json_str = Utils.removeArray(this.jsonGenerator().generateJson(mbed_json));
        String edgex_json_str = Utils.removeArray(this.jsonGenerator().generateJson(edgex_json)); 
        
        // DEBUG
        this.errorLogger().info("addDevice: mbed_id: " + mbed_id + " mbed_device: " + mbed_json_str + " edgex_name: " + edgex_name + " edgex_device: " + edgex_json_str);
        
        // save to the db
        this.m_id_map_db.put(mbed_id, edgex_name);
        this.m_mbed_db.put(mbed_id,mbed_json_str);
        this.m_edgex_db.put(edgex_name,edgex_json_str);
    }
    
    // get the mbed Device JSON
    public Map getMbedDevice(String mbed_id) {
        String mbed_json_str = this.m_mbed_db.get(mbed_id);
        if (mbed_json_str != null && mbed_json_str.length() > 0) {
            try {
                Map mbed_json = this.jsonParser().parseJson(mbed_json_str);
                return mbed_json;
            }
            catch (Exception ex) {
                // parsing error
                this.errorLogger().warning("getMbedDevice: JSON Parse exception: " + ex.getMessage() + " JSON: " + mbed_json_str,ex);
            }
        }
        return null;
    }
    
    // get the EdgeX Device JSON
    public Map getEdgeXDevice(String edgex_name) {
        String edgex_json_str = this.m_edgex_db.get(edgex_name);
        if (edgex_json_str != null && edgex_json_str.length() > 0) {
            try {
                Map edgex_json = this.jsonParser().parseJson(edgex_json_str);
                return edgex_json;
            }
            catch (Exception ex) {
                // parsing error
                this.errorLogger().warning("getEdgeXDevice: JSON Parse exception: " + ex.getMessage() + " JSON: " + edgex_json_str,ex);
            }
        }
        return null;
    }
    
    // save to cache
    public boolean saveToCache() {
        boolean status = false;
        if (this.m_disable_cache == false) {
            // write to cache
            this.errorLogger().info("mbedDeviceShadowDatabase: saveToCache: writing synced cache db...");
            status = this.saveToCache(EDGEX_CACHE,this.m_edgex_db);
            if (status == true) {
                status = this.saveToCache(MBED_CACHE,this.m_mbed_db);
                if (status == true) {
                    status = this.saveToCache(ID_MAP_CACHE,this.m_id_map_db);
                }
            }
        }
        else {
            // cache disabled
            this.errorLogger().info("mbedDeviceShadowDatabase: saveToCache: cache DISABLED (OK)");
            status = true;
        }
        return status;
    }
    
    // lookup the mbed ID for a given EdgeX Name
    public String lookupMbedName(String edgex_name) {
        // iterate over the db and validate each...
        for (HashMap.Entry mbed_device : this.m_id_map_db.entrySet()) {
            // get the ith entry...
            String mbed_id = (String)mbed_device.getKey();
            String tmp_edgex_name = (String)mbed_device.getValue();
            
            // if we match, return
            if (tmp_edgex_name.equalsIgnoreCase(edgex_name) == true) {
                // matched! return the mbed Name
                return mbed_id;
            }
        }
        return null;
    }
    
    // remove a device
    private void removeDevice(String mbed_id,String edgex_name) {
        this.m_mbed_db.remove(mbed_id);
        this.m_id_map_db.remove(mbed_id);
        this.m_edgex_db.remove(edgex_name);
    }
    
    // save DB to Cache
    private boolean saveToCache(String filename, HashMap db) {
        boolean status = false;
        
        if (this.m_disable_cache == false) {
            try {
                // write to the cache DB file
                FileOutputStream fos = new FileOutputStream(filename);
                ObjectOutputStream oos = new ObjectOutputStream(fos);

                // save...
                oos.writeObject(db);

                // close...
                oos.close();
                fos.close();

                // OK
                status = true;
            }
            catch (IOException ex) {
                // unable to write to cache file (ERROR)
                this.errorLogger().warning("mbedDeviceShadowDatabase:saveToCache: Cannot write DB to " + filename + " cache: " + ex.getLocalizedMessage());
                status = false;
            }
        }
        else {
            // caching disabled - to just return success
            this.errorLogger().info("mbedDeviceShadowDatabase:saveToCache: caching DISABLED (OK)");
            status = true;
        }
        
        // return our status
        return status;
    }
    
    // reload the DB from Cache
    private HashMap reloadFromCache(String filename) {
        boolean status = false;
        HashMap<String,String> db = null;
        
        if (this.m_disable_cache == false) {
            try
            {
                // open the cache file
                FileInputStream fis = new FileInputStream(filename);
                ObjectInputStream ois = new ObjectInputStream(fis);

                // read from file
                db = (HashMap) ois.readObject();

                // close out...
                ois.close();
                fis.close();

                // OK
                status = true;
            }
            catch (IOException ex) {
                // no cache file - so OK
                this.errorLogger().info("mbedDeviceShadowDatabase:reloadFromCache: Unable to open cache file (OK)...");
                status = true;

                // create an empty HashMap
                db = new HashMap<>();
            }
            catch (ClassNotFoundException ex) {
                // Error
                this.errorLogger().warning("mbedDeviceShadowDatabase:reloadFromCache: Class not found ERROR: " + ex.getLocalizedMessage());
                status = false;

                // clear out the db
                db = null;
            }
        }
        else {
            // cache disabled - just create a new HashMap
            db = new HashMap<>();
        }
        
        // return our db
        return db;
    }
    
    // validate and prune stale entries
    private void validateAndPrune(mbedClientServiceProcessor processor) {
        // iterate over the db and validate each...
        for (HashMap.Entry mbed_device : this.m_id_map_db.entrySet()) {
            // get the ith entry...
            String mbed_id = (String)mbed_device.getKey();
            String edgex_name = (String)mbed_device.getValue();
            
            // DEBUG
            this.errorLogger().info("validateAndPrune: mbed ID: " + mbed_device.getKey() + " EdgeX Name: " + edgex_name);
            
            // validate with the mbed client service
            if (processor.validate(mbed_id,edgex_name) == false) {
                // mbed client service does not recognize this device... so prune it...
                this.errorLogger().info("validateAndPrune: mbed ID: " + mbed_device.getKey() + " and EdgeX Name: " + edgex_name + " no longer appears to be a valid mapping... prunning...");
                
                // remove 
                this.removeDevice(mbed_id, edgex_name);
            }
        }
    }
}
