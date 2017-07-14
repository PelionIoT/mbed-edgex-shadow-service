/**
 * @file    HttpTransport.java
 * @brief HTTP Transport Support
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
package com.arm.mbed.edgex.shadow.service.transport;

import com.arm.mbed.edgex.shadow.service.core.ErrorLogger;
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * HTTP Transport Support
 *
 * @author Doug Anson
 */
public class HttpTransport {

    private int m_last_response_code = 0;
    private ErrorLogger m_error_logger = null;
    private PreferenceManager m_preference_manager = null;
    private String m_content_type = null;

    /**
     * constructor
     * @param error_logger
     * @param preference_manager
     */
    public HttpTransport(ErrorLogger error_logger, PreferenceManager preference_manager) {
        this.m_error_logger = error_logger;
        this.m_preference_manager = preference_manager;
        if (preference_manager != null) {
            this.m_content_type = preference_manager.valueOf("content_type");
        }
    }
    
    /**
     * execute GET over http
     * @param url_str
     * @return
     */
    public String httpGet(String url_str) {
        return this.doHTTP("GET", url_str, null, this.contentType());
    }

    /**
     * execute POST over http
     * @param url_str
     * @param data
     * @return
     */
    public String httpPost(String url_str, String data) {
        return this.doHTTP("POST", url_str, data, this.contentType());
    }
    
    /**
     * execute PUT over http
     * @param url_str
     * @param data
     * @return
     */
    public String httpPut(String url_str, String data) {
        return this.doHTTP("PUT", url_str, data, this.contentType());
    }

    /**
     * execute DELETE over http
     * @param url_str
     * @param data
     * @return
     */
    public String httpDelete(String url_str, String data) {
        return this.doHTTP("DELETE", url_str, data, this.contentType());
    }

    // save the last response code
    private void saveResponseCode(int response_code) {
        this.m_last_response_code = response_code;
    }

    /**
     * get the last response code
     * @return
     */
    public int getLastResponseCode() {
        return this.m_last_response_code;
    }

    // perform an authenticated HTML operation
    private String doHTTP(String verb, String url_str, String data, String content_type) {
        String result = "";
        String line = "";
        URLConnection connection = null;
        
        try {
           // create the URL
           URL url = new URL(url_str);

           // create the connection  
           connection = (HttpURLConnection) (url.openConnection());
           ((HttpURLConnection) connection).setRequestMethod(verb);

           // if we have data to write to the HTTP stream... set it
           connection.setDoInput(true);
           connection.setDoOutput(true);

           // specify content type if requested
           if (content_type != null && content_type.length() > 0) {
                connection.setRequestProperty("Content-Type", content_type);
                connection.setRequestProperty("Accept", "*/*");

                // DEBUG
                //this.errorLogger().info("ContentType: " + content_type);
           }

           // add Connection: keep-alive (does not work...)
           connection.setRequestProperty("Connection", "keep-alive");
           
           // special gorp for HTTP DELETE
           if (verb != null && verb.equalsIgnoreCase("delete")) {
               connection.setRequestProperty("Access-Control-Allow-Methods", "OPTIONS, DELETE");
           }
           
           // DEBUG
           //this.errorLogger().info("HTTP(" + verb +") URL: " + url_str + " Data: " + data);

           // Write the input data to the HTTP stream...
           if (data != null && data.length() > 0) {
               try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream())) {
                   out.write(data);
               }
           }

            // look for the result to come back
            try {
                try (InputStream content = (InputStream) connection.getInputStream(); BufferedReader in = new BufferedReader(new InputStreamReader(content))) {
                    StringBuilder buf = new StringBuilder();
                    while ((line = in.readLine()) != null) {
                        buf.append(line);
                    }
                    result = buf.toString();
                }
            }
            catch (java.io.FileNotFoundException ex) {
                this.errorLogger().info("HTTP(" + verb + ") empty response (OK).");
                result = "";
            }

            // save off the HTTP response code...
            this.saveResponseCode(((HttpURLConnection) connection).getResponseCode());

            // DEBUG
            //this.errorLogger().info("HTTP(" + verb +") URL: " + url_str + " Data: " + data + " Response code: " + ((HttpURLConnection)connection).getResponseCode());
        }
        catch (IOException ex) {
            this.errorLogger().warning("Exception in doHTTP(" + verb + "): " + ex.getMessage());
            result = null;

            try {
                // check for non-null connection
                if (connection != null) {
                    // save off the HTTP response code...
                    this.saveResponseCode(((HttpURLConnection) connection).getResponseCode());
                }
                else {
                    this.errorLogger().warning("ERROR in doHTTP(" + verb + "): Connection is NULL");
                }
            }
            catch (IOException ex2) {
                this.errorLogger().warning("Exception in doHTTP(" + verb + "): Unable to save last response code: " + ex2.getMessage());
            }
        }

        // return the result
        return result;
    }
    
    // our defaulted content type
    private String contentType() {
        return this.m_content_type;
    }
    
    // get our error handler
    private com.arm.mbed.edgex.shadow.service.core.ErrorLogger errorLogger() {
        return this.m_error_logger;
    }

    // get the preferenceManager
    private com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager preferences() {
        return this.m_preference_manager;
    }
}
