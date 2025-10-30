/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.spring.test;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Proxy {

    private Server server;
    @Value("${proxy.port:-1}")
    private int proxyPort;
    @Value("${proxy.path:''}")
    private String proxyPath;

    @PostConstruct
    public void start() throws Exception {
        if (proxyPort <= 0) {
            return;
        }
        server = new Server(proxyPort);

        // Setup proxy servlet
        ServletContextHandler context = new ServletContextHandler("/",
                ServletContextHandler.SESSIONS);
        server.setHandler(context);

        ServletHolder proxyServlet = new ServletHolder(
                PathRewritingProxyServlet.class);
        proxyServlet.setInitParameter("proxyTo", "http://localhost:8888/");
        proxyServlet.setInitParameter("prefix", proxyPath);
        context.addServlet(proxyServlet, "/*");

        server.start();
    }

    @PreDestroy
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

}
