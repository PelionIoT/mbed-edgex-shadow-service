/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.arm.mbed.edge.core;

import com.arm.mbed.edgex.shadow.service.core.BaseClass;
import com.arm.mbed.edgex.shadow.service.core.ErrorLogger;
import com.arm.mbed.edgex.shadow.service.core.Utils;
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;
import java.io.IOException;

/**
 * Mbed Edge Core Status Checking
 * @author Doug Anson
 */
public class MbedEdgeCoreStatusCheck extends BaseClass implements Runnable {
    private Thread m_thread = null;
    private int m_wait_time_ms = 0;
    private boolean m_running = false;
    private boolean m_mbed_edge_running = true;
    private boolean m_mbed_edge_running_last = !this.m_mbed_edge_running;
    
    // primary constructor
    public MbedEdgeCoreStatusCheck(ErrorLogger logger, PreferenceManager preferences,int wait_time_ms) {
        super(logger,preferences);
        this.m_wait_time_ms = wait_time_ms;
        this.m_running = false;
    }
    
    // start 
    public void init() {
        if (this.m_thread == null) {
            this.m_thread = new Thread(this);
            this.m_running = true;
            this.m_thread.start();
        }
    }
    
    // stop
    public void stop() {
        this.m_running = false;
    }
    
    // mbed edge running?
    public boolean mbedEdgeRunning() {
        return this.m_mbed_edge_running;
    }
    
    // update the mbed edge core status
    private void updateMbedEdgeCoreStatus() {
        // execute the script to check the latest status
        String status = this.check().trim();
        
        //DEBUG
        this.errorLogger().info("MbedEdgeCore: " + status);
        
        // set the status
        if (status != null && status.equalsIgnoreCase("yes")) {
            // running
            this.m_mbed_edge_running = true;
        }
        else {
            // not running
            this.m_mbed_edge_running = false;
        }
        
        // DEBUG
        if (this.m_mbed_edge_running_last != this.m_mbed_edge_running) {
            if (this.m_mbed_edge_running == true) {
                this.errorLogger().warning("MbedEdgeCore: Service RUNNING");
            }
            else {
                this.errorLogger().warning("MbedEdgeCore: Service STOPPED");
            }
        }
        
        // save this iteration...
        this.m_mbed_edge_running_last = this.m_mbed_edge_running;
    }
    
    // execute script
    private String check() {
        // construct the arguments
        String cmd = "./scripts/mbed_edge_running.sh";
        String response = null;
        String error = null;

        try {
            // invoke the AWS CLI
            Process proc = Runtime.getRuntime().exec(cmd);
            response = Utils.convertStreamToString(proc.getInputStream());
            error = Utils.convertStreamToString(proc.getErrorStream());

            // wait to completion
            proc.waitFor();
            int status = proc.exitValue();

            // DEBUG
            if (status != 0) {
                // non-zero exit status
                this.errorLogger().info("MbedEdgeCoreStatusCheck: Invoked: " + cmd);
                this.errorLogger().info("MbedEdgeCoreStatusCheck: Response: " + response);
                this.errorLogger().info("MbedEdgeCoreStatusCheck: Error: " + error);
                this.errorLogger().info("MbedEdgeCoreStatusCheck: Exit Code: " + status);
            }
            else {
                // successful exit status
                this.errorLogger().info("MbedEdgeCoreStatusCheck: Invoked: " + cmd);
                this.errorLogger().info("MbedEdgeCoreStatusCheck: Response: " + response);
                this.errorLogger().info("MbedEdgeCoreStatusCheck: Exit Code: " + status);
            }
        }
        catch (IOException | InterruptedException ex) {
            this.errorLogger().warning("MbedEdgeCoreStatusCheck: Exception for command: " + cmd, ex);
            response = null;
        }

        // return the resposne
        return response;
    }
    
    // main loop
    private void mainloop() {
        while(this.m_running == true) {
            // check the status of mbed edge core
            this.updateMbedEdgeCoreStatus();
            
            // wait for a bit
            Utils.waitForABit(this.errorLogger(), this.m_wait_time_ms);
        }
        
        // we have halted checks
        this.errorLogger().warning("MbedEdgeCoreStatusCheck: checking loop has HALTED");
    }
    
    @Override
    public void run() {
        this.mainloop();
    }   
}
