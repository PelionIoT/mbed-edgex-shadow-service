
/**
 * @file    Main.java
 * @brief main entry for the mbed edgex shadow service
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
import com.arm.mbed.edgex.shadow.service.servlet.Manager;
import com.arm.mbed.edgex.shadow.service.core.ErrorLogger;
import com.arm.mbed.edgex.shadow.service.core.Utils;
import com.arm.mbed.edgex.shadow.service.loggerservlet.LoggerWebSocketServlet;
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Primary entry point for the connector-bridge Jetty application
 *
 * @author Doug Anson
 */
public class Main implements Runnable {
    // our manager
    private Manager m_manager = null;
    
    // Jetty Server - WS logging
    private Server m_ws_service = null;
    
    // Defaults
    private static int DEF_THREAD_COUNT_CHECK_WAIT_MS = 60000;      // 1 minute between thread count checks
    private static int DEF_CORE_POOL_SIZE = 10;                     // default size of pool of threads
    private static int DEF_MAX_POOL_SIZE = 1000000;                 // max size of pool of threads
    
    // thread count wait time in ms
    private int m_thread_count_check_wait_ms = DEF_THREAD_COUNT_CHECK_WAIT_MS;
    
    // Thread count
    private int m_thread_count = 1;   // ourself
    private boolean m_running = false; 
    
    // Preferences and ErrorLogger
    protected ErrorLogger m_logger = null;
    protected PreferenceManager m_preferences = null;
    
    // main entry 
    public static void main(String[] args) throws Exception {
        Main m = new Main(args);
        m.go();
    }
    
    // default constructor
    public Main(String[] args) {
        m_logger = new ErrorLogger();
        m_preferences = new PreferenceManager(m_logger);

        // configure the error logger logging level
        m_logger.configureLoggingLevel(m_preferences);
        
        // create our Manager...
        if (m_manager == null) {
            m_manager = new Manager(m_logger,m_preferences);
        }
        
        // get the thread pooling configuration
        int core_pool_size = m_preferences.intValueOf("threads_core_pool_size");
        if (core_pool_size <= 0) {
            core_pool_size = DEF_CORE_POOL_SIZE;
        }
        int max_pool_size = m_preferences.intValueOf("threads_max_pool_size");
        if (max_pool_size <= 0) {
            max_pool_size = DEF_MAX_POOL_SIZE;
        }
        
        // Threading Pool Config
        m_logger.warning("Main: Jetty Thread Executor Pool: initial pool: " + core_pool_size + " max: " + max_pool_size);
        
        // Enable SSL Support
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath("keystore.jks");
        sslContextFactory.setKeyStorePassword(this.m_preferences.valueOf("mds_gw_keystore_password"));
        
        // setup our websocket server (must support WSS)
        m_ws_service = new Server();
        ServerConnector logger_server_connector = new ServerConnector(m_ws_service,sslContextFactory);
        logger_server_connector.setHost("0.0.0.0");
        logger_server_connector.setPort(m_preferences.intValueOf("websocket_streaming_port"));
        m_ws_service.addConnector(logger_server_connector);
        
        // default context handler for WS
        ServletContextHandler logger_context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        logger_context.setContextPath("/");
        m_ws_service.setHandler(logger_context);
        
        // Logging Service context handler
        ServletHolder logEvents = new ServletHolder("ws-logger",LoggerWebSocketServlet.class);
        logger_context.addServlet(logEvents, "/logger/*");
    }
    
    // primary loop initiation
    private void go() {
        // add a shutdown hook for graceful shutdowns...
        Runtime.getRuntime().addShutdownHook(
            new Thread() {
                @Override
                public void run() {
                    m_logger.warning("Main: Closing down Manager...");
                    m_manager.closedown();
                }
            }
        );
        
        try {
            // initialize
            this.initialize();
            
            // Start the Websocket Service
            m_logger.warning("Main: Starting logger service");
            this.m_ws_service.start();   
            
            // launch our worker thread
            Thread t = new Thread(this);
            t.start();

            // Join
            m_logger.warning("Main: Starting WS Joining...");
            this.m_ws_service.join();
        }
        catch (Exception ex) {
            m_logger.warning("Main: Exception caught while starting thread count updater task: " + ex.getMessage());
        }
    }
    
    // start our edge core shadow service
    private void initialize() {
        try {
            // Start the Bridge Service
            m_logger.warning("Main: Starting edge core shadow service...");
            m_manager.initialize();
        }
        catch (Exception ex) {
            m_logger.critical("Main: EXCEPTION during edge core shadow service start(): " + ex.getMessage(),ex);
        }
    }

    @Override
    public void run() {
        // DEBUG
        m_logger.warning("Main: Starting main loop...");
        
        // worker thread loop
        while(true) {
            this.m_manager.iterate();
            Utils.waitForABit(m_logger, 10000);
        }
    }
}
