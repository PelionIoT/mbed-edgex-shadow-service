
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
import com.arm.mbed.edgex.shadow.service.preferences.PreferenceManager;
import com.arm.mbed.edgex.shadow.service.servlet.EventsProcessor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Primary entry point for the connector-bridge Jetty application
 *
 * @author Doug Anson
 */
public class Main {

    public static void main(String[] args) throws Exception {
        ErrorLogger logger = new ErrorLogger();
        PreferenceManager preferences = new PreferenceManager(logger);

        // configure the error logger logging level
        logger.configureLoggingLevel(preferences);

        // initialize the server,,,
        Server server = new Server(preferences.intValueOf("shadow_service_port"));

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(preferences.valueOf("context_path"));
        server.setHandler(context);

        // check for and add SSL support if configured...
        if (preferences.booleanValueOf("use_ssl") == true) {
            // Enable SSL Support
            SslSocketConnector sslConnector = new SslSocketConnector();
            sslConnector.setPort(preferences.intValueOf("shadow_service_port") + 1);
            sslConnector.setHost("0.0.0.0");
            sslConnector.setKeystore("keystore.jks");
            sslConnector.setPassword(preferences.valueOf("keystore_password"));
            server.addConnector(sslConnector);
        }

        EventsProcessor eventsProcessor = new EventsProcessor();
        final Manager manager = eventsProcessor.manager();
        manager.initialize();
        
        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(
                new Thread() {
            @Override
            public void run() {
                // empty
                manager.closedown();
            }
        });

        // notification events: wildcard for domain inclusion
        context.addServlet(new ServletHolder(eventsProcessor), preferences.valueOf("events_path") + "/*");
        
        // start
        server.start();

        // JOIN
        server.join();
    }
}
