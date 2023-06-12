/*
 * Copyright 2000-2023 Vaadin Ltd.
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

package com.vaadin.flow.server;

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public final class StaticFileServerTests {

    private Server server;

    private ServerConnector connector;

    private HttpClient client;

    private void start() throws Exception {
        startServer();
        startClient();
    }

    private void startServer() throws Exception {
        server = new Server(new QueuedThreadPool());
        connector = new ServerConnector(server);

        connector.setPort(0);

        server.addConnector(connector);
        server.setHandler(createContext());

        server.start();
    }

    private ServletContextHandler createContext() {
        final WebAppContext context = new WebAppContext();

        context.setBaseResource(ResourceFactory.root().newResource(
                StaticFileServerTests.class.getResource("/webapp")));
        context.setContextPath("/");
        context.setExtractWAR(false);

        final ServletHolder vaadinServletHolder = context
                .addServlet(VaadinServlet.class, "/*");

        vaadinServletHolder.setInitOrder(1);
        vaadinServletHolder.setInitParameter("pushMode", "automatic");

        context.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*\\.jar|.*/classes/.*");
        context.setConfigurationDiscovered(true);
        context.setParentLoaderPriority(true);

        return context;
    }

    private void startClient() throws Exception {
        final ClientConnector connector = new ClientConnector();

        connector.setSelectors(1);
        connector.setExecutor(new QueuedThreadPool());
        connector.setScheduler(
                new ScheduledExecutorScheduler("client-scheduler", false));

        client = new HttpClient(new HttpClientTransportOverHTTP(connector));

        client.setSocketAddressResolver(new SocketAddressResolver.Sync());

        client.start();
    }

    @Test
    public void serveJarResourceTwice() throws Exception {
        start();

        client.setConnectBlocking(true);

        String content = null;

        for (int i = 1; i <= 4; i++) {
            final ContentResponse response = client
                    .GET("http://localhost:" + connector.getLocalPort()
                            + "/VAADIN/static/push/vaadinPush.js");

            Assert.assertNotNull("Should not receive null response.", response);
            Assert.assertEquals("Expected ok status for request " + i, 200,
                    response.getStatus());

            if (content == null) {
                content = response.getContentAsString();
            } else {
                Assert.assertEquals(
                        "Content for request " + i + " differs from original",
                        content, response.getContentAsString());
            }
        }
    }

    @After
    public void disposeClient() throws Exception {
        if (client != null) {
            client.stop();

            client = null;
        }
    }

    @After
    public void disposeServer() throws Exception {
        if (server != null) {
            server.stop();

            server = null;
        }
    }

}
