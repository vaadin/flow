/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.base.devserver.stats;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.testutil.TestUtils;
import com.vaadin.flow.testutil.net.PortProber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsSenderTest extends AbstractStatisticsTest {

    private static final long SEC_12H = 60 * 60 * 12;
    private static final long SEC_24H = 60 * 60 * 24;
    private static final long SEC_48H = 60 * 60 * 48;
    private static final long SEC_30D = 60 * 60 * 24 * 30;
    private TestHttpServer server;

    @BeforeEach
    void setup() throws Exception {
        super.setup();

        // Init using test project
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);
    }

    private void createServer(int statusCode, String message) throws Exception {
        this.server = new TestHttpServer(statusCode, message);
        Mockito.when(sender.getReportingUrl())
                .thenReturn("http://localhost:" + server.getPort() + "/");
    }

    @AfterEach
    void teardown() throws Exception {
        if (server != null) {
            server.close();
            server = null;
        }
    }

    @Test
    void defaultServerResponse() throws Exception {
        // Test with default server response
        createServer(200, DEFAULT_SERVER_MESSAGE);
        ObjectNode fullStats = storage.read();
        long lastSend = sender.getLastSendTime(fullStats);
        sender.sendStatistics(fullStats);

        fullStats = storage.read();
        long newSend = sender.getLastSendTime(fullStats);
        assertTrue(newSend > lastSend, "Send time should be updated");
        assertTrue(sender.getLastSendStatus(fullStats).contains("200"),
                "Status should be 200");
        assertEquals(SEC_24H, sender.getInterval(fullStats),
                "Default interval should be 24H in seconds");
    }

    @Test
    void responseWithCustomInterval() throws Exception {

        // Test with server response with too custom interval
        createServer(200, SERVER_MESSAGE_48H);
        ObjectNode fullStats = storage.read();
        long lastSend = sender.getLastSendTime(fullStats);
        sender.sendStatistics(fullStats);
        fullStats = storage.read();
        long newSend = sender.getLastSendTime(fullStats);
        assertTrue(newSend > lastSend, "Send time should be updated");
        assertTrue(sender.getLastSendStatus(fullStats).contains("200"),
                "Status should be 200");
        assertEquals(SEC_48H, sender.getInterval(fullStats),
                "Custom interval should be 48H in seconds");
    }

    @Test
    void tooShortInterval() throws Exception {
        // Test with server response with too short interval
        createServer(200, SERVER_MESSAGE_3H);
        ObjectNode fullStats = storage.read();
        long lastSend = sender.getLastSendTime(fullStats);
        sender.sendStatistics(fullStats);
        fullStats = storage.read();
        long newSend = sender.getLastSendTime(fullStats);
        assertTrue(newSend > lastSend, "Send time should be updated");
        assertTrue(sender.getLastSendStatus(fullStats).contains("200"),
                "Status should be 200");
        assertEquals(SEC_12H, sender.getInterval(fullStats),
                "Minimum interval should be 12H in seconds");
    }

    @Test
    void tooLongInterval() throws Exception {
        // Test with server response with too long interval
        createServer(200, SERVER_MESSAGE_40D);
        ObjectNode fullStats = storage.read();
        long lastSend = sender.getLastSendTime(fullStats);
        sender.sendStatistics(fullStats);
        fullStats = storage.read();
        long newSend = sender.getLastSendTime(fullStats);
        assertTrue(newSend > lastSend, "Send time should be not be updated");
        assertTrue(sender.getLastSendStatus(fullStats).contains("200"),
                "Status should be 200");
        assertEquals(SEC_30D, sender.getInterval(fullStats),
                "Maximum interval should be 30D in seconds");
    }

    @Test
    void failResponse() throws Exception {

        // Test with server fail response
        createServer(500, SERVER_MESSAGE_40D);
        ObjectNode fullStats = storage.read();
        long lastSend = sender.getLastSendTime(fullStats);
        sender.sendStatistics(fullStats);
        fullStats = storage.read();

        long newSend = sender.getLastSendTime(fullStats);
        assertTrue(newSend > lastSend, "Send time should be updated");
        assertTrue(sender.getLastSendStatus(fullStats).contains("500"),
                "Status should be 500");
        assertEquals(SEC_24H, sender.getInterval(fullStats),
                "In case of errors we should use default interval");
    }

    @Test
    void returnedMessage() throws Exception {
        // Test with server returned message
        createServer(200, SERVER_MESSAGE_MESSAGE);
        ObjectNode fullStats = storage.read();

        long lastSend = sender.getLastSendTime(fullStats);
        sender.sendStatistics(fullStats);
        fullStats = storage.read();
        long newSend = sender.getLastSendTime(fullStats);
        assertTrue(newSend > lastSend, "Send time should be updated");
        assertTrue(sender.getLastSendStatus(fullStats).contains("200"),
                "Status should be 200");
        assertEquals(SEC_24H, sender.getInterval(fullStats),
                "Default interval should be 24H in seconds");
        assertEquals("Hello", sender.getLastServerMessage(fullStats),
                "Message should be returned");
    }

    /**
     * Simple HttpServer for testing.
     */
    public static class TestHttpServer implements AutoCloseable {

        private HttpServer httpServer;
        private String lastRequestContent;

        public TestHttpServer(int code, String response) throws Exception {
            this.httpServer = createStubGatherServlet(code, response);
        }

        private HttpServer createStubGatherServlet(int status, String response)
                throws Exception {
            HttpServer httpServer = HttpServer.create(
                    new InetSocketAddress(PortProber.findFreePort()), 0);
            httpServer.createContext("/", exchange -> {
                this.lastRequestContent = new String(
                        exchange.getRequestBody().readAllBytes(),
                        Charset.defaultCharset());
                exchange.sendResponseHeaders(status, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
            });
            httpServer.start();
            return httpServer;
        }

        public String getLastRequestContent() {
            return lastRequestContent;
        }

        public int getPort() {
            return httpServer.getAddress().getPort();
        }

        @Override
        public void close() throws Exception {
            if (httpServer != null) {
                httpServer.stop(0);
            }
        }

    }
}
