/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.demo;

import java.io.File;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;

/**
 * Server class for running component demos and executing integration tests.
 * <p>
 * The {@link #startServer()} method in this class opens a web server to
 * http://localhost:9998/ to serve all your demo views annotated with
 * <code>@Route</code> for development and integration testing. This class can
 * be extended for additional configuration.
 * 
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ComponentDemoServer {

    private static final String WEB_APP_PATH = "webapp";

    private static final String TEST_CLASSES_PATH = ".*/target/test-classes/";

    private static final String JAR_PATTERN = ".*\\.jar$";

    /**
     * Starts a web server to the port defined by {@link #getPort()}. It serves
     * the test UIs annotated with <code>@Route</code>.
     * 
     * @throws Exception
     *             if any issue on server start occurs
     * 
     * @return the server object
     */
    public Server startServer() throws Exception {

        Server server = new Server();

        final ServerConnector connector = new ServerConnector(server);
        connector.setPort(getPort());
        server.setConnectors(new Connector[] { connector });

        WebAppContext context = new WebAppContext();

        File file = new File(WEB_APP_PATH);
        if (!file.exists()) {
            try {
                if (!file.mkdirs()) {
                    throw new RuntimeException(
                            "Failed to create the following directory for webapp: "
                                    + WEB_APP_PATH);
                }
            } catch (SecurityException exception) {
                throw new RuntimeException(
                        "Failed to create the following directory for webapp: "
                                + WEB_APP_PATH,
                        exception);
            }
        }
        context.setWar(file.getPath());

        context.setContextPath("/");

        ClassList classlist = ClassList.setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration");

        // Enable annotation scanning for uitest classes.
        // Enable scanning for resources inside jar-files.
        context.setAttribute(WebInfConfiguration.CONTAINER_JAR_PATTERN,
                TEST_CLASSES_PATH + "|" + JAR_PATTERN);

        configure(context, server);
        server.setHandler(context);
        server.start();
        return server;
    }

    /**
     * Gets the port number to which this server will be connected.
     * 
     * @return the port number to which this server will be connected.
     */
    protected int getPort() {
        return 9998;
    }

    /**
     * Hook for additional configuration to perform before starting the server.
     *
     * @param context
     *            the context
     * @param server
     *            the server
     */
    protected void configure(WebAppContext context, Server server) {
        // override for additional configuration
    }

}
