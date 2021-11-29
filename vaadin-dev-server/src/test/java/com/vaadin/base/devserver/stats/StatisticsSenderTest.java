package com.vaadin.base.devserver.stats;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import com.vaadin.flow.testutil.TestUtils;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class StatisticsSenderTest extends AbstractStatisticsTest {

    private static final long SEC_12H = 60 * 60 * 12;
    private static final long SEC_24H = 60 * 60 * 24;
    private static final long SEC_48H = 60 * 60 * 48;
    private static final long SEC_30D = 60 * 60 * 24 * 30;

    @Test
    public void send() throws Exception {

        // Init using test project
        String mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1").toPath()
                .toString();
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        // Test with default server response
        try (TestHttpServer server = new TestHttpServer(200,
                DEFAULT_SERVER_MESSAGE)) {
            ObjectNode fullStats = storage.read();
            long lastSend = sender.getLastSendTime(fullStats);
            sender.sendStatistics(fullStats);

            fullStats = storage.read();
            long newSend = sender.getLastSendTime(fullStats);
            Assert.assertTrue("Send time should be updated",
                    newSend > lastSend);
            Assert.assertTrue("Status should be 200",
                    sender.getLastSendStatus(fullStats).contains("200"));
            Assert.assertEquals("Default interval should be 24H in seconds",
                    SEC_24H, sender.getInterval(fullStats));
        }

        // Test with server response with too custom interval
        try (TestHttpServer server = new TestHttpServer(200,
                SERVER_MESSAGE_48H)) {
            ObjectNode fullStats = storage.read();
            long lastSend = sender.getLastSendTime(fullStats);
            sender.sendStatistics(fullStats);
            fullStats = storage.read();
            long newSend = sender.getLastSendTime(fullStats);
            Assert.assertTrue("Send time should be updated",
                    newSend > lastSend);
            Assert.assertTrue("Status should be 200",
                    sender.getLastSendStatus(fullStats).contains("200"));
            Assert.assertEquals("Custom interval should be 48H in seconds",
                    SEC_48H, sender.getInterval(fullStats));
        }

        // Test with server response with too short interval
        try (TestHttpServer server = new TestHttpServer(200,
                SERVER_MESSAGE_3H)) {
            ObjectNode fullStats = storage.read();
            long lastSend = sender.getLastSendTime(fullStats);
            sender.sendStatistics(fullStats);
            fullStats = storage.read();
            long newSend = sender.getLastSendTime(fullStats);
            Assert.assertTrue("Send time should be updated",
                    newSend > lastSend);
            Assert.assertTrue("Status should be 200",
                    sender.getLastSendStatus(fullStats).contains("200"));
            Assert.assertEquals("Minimum interval should be 12H in seconds",
                    SEC_12H, sender.getInterval(fullStats));
        }

        // Test with server response with too long interval
        try (TestHttpServer server = new TestHttpServer(200,
                SERVER_MESSAGE_40D)) {
            ObjectNode fullStats = storage.read();
            long lastSend = sender.getLastSendTime(fullStats);
            sender.sendStatistics(fullStats);
            fullStats = storage.read();
            long newSend = sender.getLastSendTime(fullStats);
            Assert.assertTrue("Send time should be not be updated",
                    newSend > lastSend);
            Assert.assertTrue("Status should be 200",
                    sender.getLastSendStatus(fullStats).contains("200"));
            Assert.assertEquals("Maximum interval should be 30D in seconds",
                    SEC_30D, sender.getInterval(fullStats));
        }

        // Test with server fail response
        try (TestHttpServer server = new TestHttpServer(500,
                SERVER_MESSAGE_40D)) {
            ObjectNode fullStats = storage.read();
            long lastSend = sender.getLastSendTime(fullStats);
            sender.sendStatistics(fullStats);
            fullStats = storage.read();

            long newSend = sender.getLastSendTime(fullStats);
            Assert.assertTrue("Send time should be updated",
                    newSend > lastSend);
            Assert.assertTrue("Status should be 500",
                    sender.getLastSendStatus(fullStats).contains("500"));
            Assert.assertEquals(
                    "In case of errors we should use default interval", SEC_24H,
                    sender.getInterval(fullStats));
        }

        // Test with server returned message
        try (TestHttpServer server = new TestHttpServer(200,
                SERVER_MESSAGE_MESSAGE)) {
            ObjectNode fullStats = storage.read();

            long lastSend = sender.getLastSendTime(fullStats);
            sender.sendStatistics(fullStats);
            fullStats = storage.read();
            long newSend = sender.getLastSendTime(fullStats);
            Assert.assertTrue("Send time should be updated",
                    newSend > lastSend);
            Assert.assertTrue("Status should be 200",
                    sender.getLastSendStatus(fullStats).contains("200"));
            Assert.assertEquals("Default interval should be 24H in seconds",
                    SEC_24H, sender.getInterval(fullStats));
            Assert.assertEquals("Message should be returned", "Hello",
                    sender.getLastServerMessage(fullStats));
        }

    }

    /**
     * Simple HttpServer for testing.
     */
    public static class TestHttpServer implements AutoCloseable {

        private HttpServer httpServer;
        private String lastRequestContent;

        public TestHttpServer(int code, String response) throws Exception {
            this.httpServer = createStubGatherServlet(HTTP_PORT, code,
                    response);
        }

        private HttpServer createStubGatherServlet(int port, int status,
                String response) throws Exception {
            HttpServer httpServer = HttpServer
                    .create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", exchange -> {
                this.lastRequestContent = IOUtils.toString(
                        exchange.getRequestBody(), Charset.defaultCharset());
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

        @Override
        public void close() throws Exception {
            if (httpServer != null) {
                httpServer.stop(0);
            }
        }

    }
}
