package com.vaadin.base.devserver;

import com.sun.net.httpserver.HttpServer;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.testutil.TestUtils;
import junit.framework.TestCase;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

@NotThreadSafe
public class VaadinUsageStatisticsTests extends TestCase {

    public static final String USAGE_REPORT_URL_LOCAL = "http://localhost:8089/";
    public static final String DEFAULT_SERVER_MESSAGE = "{\"reportInterval\":86400,\"serverMessage\":\"\"}";
    public static final String SERVER_MESSAGE_MESSAGE  = "{\"reportInterval\":86400,\"serverMessage\":\"Hello\"}";
    public static final String SERVER_MESSAGE_3H  = "{\"reportInterval\":10800,\"serverMessage\":\"\"}";
    public static final String SERVER_MESSAGE_48H  = "{\"reportInterval\":172800,\"serverMessage\":\"\"}";
    public static final String SERVER_MESSAGE_40D  = "{\"reportInterval\":3456000,\"serverMessage\":\"\"}";
    public static final String INVALID_SERVER_MESSAGE = "{\"reportInterval\":3days,\"serverMessage\":\"\"}";
    private static final long SEC_12H =  60*60*12;
    private static final long SEC_24H =  60*60*24;
    private static final long SEC_48H =  60*60*48;
    private static final long SEC_30D =  60*60*24*30;

    @Before
    public void setup() throws Exception {
    }


    @After
    public void teardown() throws Exception {
    }

    @Test
    public void testClientTelemetry() throws Exception {

        ApplicationConfiguration configuration = mockAppConfig(true);
        String mavenProjectFolder = TestUtils.getTestFolder("stats-data/maven-project-folder1").toPath().toString();
        VaadinUsageStatistics statistics = VaadinUsageStatistics.init(configuration, mavenProjectFolder);
        statistics.setUsageReportingUrl("http://localhost:8089/");
        statistics.setUsageStatisticsStore(createTempStorage("stats-data/usage-statistics-1.json"));


        // Send and see that data ws collected
        try (TestHttpServer server = new TestHttpServer(200, DEFAULT_SERVER_MESSAGE)) {
            long lastSend = statistics.getLastSendTime();
            statistics.sendCurrentStatistics();
            long newSend = statistics.getLastSendTime();
            Assert.assertTrue("Send time should be updated",newSend > lastSend);
            Assert.assertTrue("Status should be 200", statistics.getLastSendStatus().contains("200"));
            Assert.assertEquals("Default interval should be 24H in seconds", SEC_24H, statistics.getInterval());
        }


    }

    @Test
    public void testMultipleProjects() throws Exception {

        ApplicationConfiguration configuration = mockAppConfig(true);
        String mavenProjectFolder1 = TestUtils.getTestFolder("stats-data/maven-project-folder1").toPath().toString();
        VaadinUsageStatistics statistics = VaadinUsageStatistics.init(configuration, mavenProjectFolder1);
        statistics.setUsageReportingUrl("http://localhost:8089/");
        statistics.setUsageStatisticsStore(createTempStorage("stats-data/usage-statistics-1.json"));

        // Switch project to track
        String mavenProjectFolder2 = TestUtils.getTestFolder("stats-data/maven-project-folder2").toPath().toString();
        statistics = VaadinUsageStatistics.init(configuration, mavenProjectFolder2);
        Assert.assertEquals("Expected to have no restarts", 0, statistics.getFieldValue("devModeStarts"));

        // Switch project to track
        String gradleProjectFolder1 = TestUtils.getTestFolder("stats-data/gradle-project-folder1").toPath().toString();
        statistics = VaadinUsageStatistics.init(configuration, gradleProjectFolder1);
        Assert.assertEquals("Expected to have no restarts", 0, statistics.getFieldValue("devModeStarts"));

        // Switch project to track
        String gradleProjectFolder2 = TestUtils.getTestFolder("stats-data/gradle-project-folder2").toPath().toString();
        statistics = VaadinUsageStatistics.init(configuration, gradleProjectFolder2);
        statistics = VaadinUsageStatistics.init(configuration, gradleProjectFolder2); // Double init to check restarts
        Assert.assertEquals("Expected to have 1 restarts", 1, statistics.getFieldValue("devModeStarts"));

        Assert.assertEquals("Expected to have 4 projects", 4, statistics.getNumberOfProjects());

    }

    @Test
    public void testSend() throws Exception {
        // Init for sending
        ApplicationConfiguration configuration = mockAppConfig(true);
        String mavenProjectFolder = TestUtils.getTestFolder("stats-data/maven-project-folder1").toPath().toString();
        VaadinUsageStatistics statistics = VaadinUsageStatistics.init(configuration, mavenProjectFolder);
        statistics.setUsageStatisticsStore(createTempStorage("stats-data/usage-statistics-1.json"));
        statistics.setUsageReportingUrl("http://localhost:8089/");

        // Test with default server response
        try (TestHttpServer server = new TestHttpServer(200, DEFAULT_SERVER_MESSAGE)) {
            long lastSend = statistics.getLastSendTime();
            statistics.sendCurrentStatistics();
            long newSend = statistics.getLastSendTime();
            Assert.assertTrue("Send time should be updated",newSend > lastSend);
            Assert.assertTrue("Status should be 200", statistics.getLastSendStatus().contains("200"));
            Assert.assertEquals("Default interval should be 24H in seconds", SEC_24H, statistics.getInterval());
        }

        // Test with server response with too custom interval
        try (TestHttpServer server = new TestHttpServer(200, SERVER_MESSAGE_48H)) {
            long lastSend = statistics.getLastSendTime();
            statistics.sendCurrentStatistics();
            long newSend = statistics.getLastSendTime();
            Assert.assertTrue("Send time should be updated",newSend > lastSend);
            Assert.assertTrue("Status should be 200", statistics.getLastSendStatus().contains("200"));
            Assert.assertEquals("Custom interval should be 48H in seconds",SEC_48H, statistics.getInterval());
        }

        // Test with server response with too short interval
        try (TestHttpServer server = new TestHttpServer(200, SERVER_MESSAGE_3H)) {
            long lastSend = statistics.getLastSendTime();
            statistics.sendCurrentStatistics();
            long newSend = statistics.getLastSendTime();
            Assert.assertTrue("Send time should be updated",newSend > lastSend);
            Assert.assertTrue("Status should be 200", statistics.getLastSendStatus().contains("200"));
            Assert.assertEquals("Minimum interval should be 12H in seconds",SEC_12H, statistics.getInterval());
        }

        // Test with server response with too long interval
        try (TestHttpServer server = new TestHttpServer(200, SERVER_MESSAGE_40D)) {
            long lastSend = statistics.getLastSendTime();
            statistics.sendCurrentStatistics();
            long newSend = statistics.getLastSendTime();
            Assert.assertTrue("Send time should be not be updated",newSend > lastSend);
            Assert.assertTrue("Status should be 200", statistics.getLastSendStatus().contains("200"));
            Assert.assertEquals("Maximum interval should be 30D in seconds",SEC_30D, statistics.getInterval());
        }


        // Test with server fail response
        try (TestHttpServer server = new TestHttpServer(500, SERVER_MESSAGE_40D)) {
            long lastSend = statistics.getLastSendTime();
            statistics.sendCurrentStatistics();
            long newSend = statistics.getLastSendTime();
            Assert.assertTrue("Send time should be updated",newSend > lastSend);
            Assert.assertTrue("Status should be 500", statistics.getLastSendStatus().contains("500"));
            Assert.assertEquals("In case of errors we should use default interval",SEC_24H, statistics.getInterval());
        }

        // Test with invalid material
        statistics.setUsageStatisticsStore(new File(TestUtils.getTestResource("stats-data/usage-statistics-2.json").getFile()));

    }

    @Test
    public void testMavenProjectProjectId() {
        String mavenProjectFolder1 = TestUtils.getTestFolder("stats-data/maven-project-folder1").toPath().toString();
        String mavenProjectFolder2 = TestUtils.getTestFolder("stats-data/maven-project-folder2").toPath().toString();
        String id1 = VaadinUsageStatistics.generateProjectId(mavenProjectFolder1);
        String id2 = VaadinUsageStatistics.generateProjectId(mavenProjectFolder2);
        Assert.assertNotNull(id1);
        Assert.assertNotNull(id2);
        Assert.assertNotEquals(id1,id2); // Should differ
    }

    @Test
    public void testMavenProjectSource() {
        String mavenProjectFolder1 = TestUtils.getTestFolder("stats-data/maven-project-folder1").toPath().toString();
        String mavenProjectFolder2 = TestUtils.getTestFolder("stats-data/maven-project-folder2").toPath().toString();
        String source1 = VaadinUsageStatistics.getProjectSource(mavenProjectFolder1);
        String source2 = VaadinUsageStatistics.getProjectSource(mavenProjectFolder2);
        Assert.assertEquals("https://start.vaadin.com/1",source1);
        Assert.assertEquals("https://start.vaadin.com/2",source2);
    }

    @Test
    public void testGradleProjectProjectId() {
        String gradleProjectFolder1 = TestUtils.getTestFolder("stats-data/gradle-project-folder1").toPath().toString();
        String gradleProjectFolder2 = TestUtils.getTestFolder("stats-data/gradle-project-folder2").toPath().toString();
        String id1 = VaadinUsageStatistics.generateProjectId(gradleProjectFolder1);
        String id2 = VaadinUsageStatistics.generateProjectId(gradleProjectFolder2);
        Assert.assertNotNull(id1);
        Assert.assertNotNull(id2);
        Assert.assertNotEquals(id1,id2); // Should differ
    }

    @Test
    public void testGradleProjectSource() {
        String gradleProjectFolder1 = TestUtils.getTestFolder("stats-data/gradle-project-folder1").toPath().toString();
        String gradleProjectFolder2 = TestUtils.getTestFolder("stats-data/gradle-project-folder2").toPath().toString();
        String source1 = VaadinUsageStatistics.getProjectSource(gradleProjectFolder1);
        String source2 = VaadinUsageStatistics.getProjectSource(gradleProjectFolder2);
        Assert.assertEquals("https://start.vaadin.com/1",source1);
        Assert.assertEquals("https://start.vaadin.com/2",source2);
    }

    @Test
    public void testMissingProject() {
        String mavenProjectFolder1 = TestUtils.getTestFolder("java").toPath().toString();
        String mavenProjectFolder2 = TestUtils.getTestFolder("stats-data/empty").toPath().toString();
        String id1 = VaadinUsageStatistics.generateProjectId(mavenProjectFolder1);
        String id2 = VaadinUsageStatistics.generateProjectId(mavenProjectFolder2);
        Assert.assertNotNull(id1);
        Assert.assertNotNull(id2);
        Assert.assertEquals(id1,id2); // Should be the default id in both cases
    }

    @Test
    public void testReadUserKey() throws IOException {
        ApplicationConfiguration configuration = mockAppConfig(true);
        String mavenProjectFolder = TestUtils.getTestFolder("stats-data/maven-project-folder1").toPath().toString();
        System.setProperty("user.home", TestUtils.getTestFolder("stats-data").toPath().toString()); //Change the home location
        VaadinUsageStatistics stats = VaadinUsageStatistics.init(configuration, mavenProjectFolder);

        // Read from file
        String keyString = "user-ab641d2c-test-test-file-223cf1fa628e";
        String key = stats.getUserKey();
        assertEquals(keyString,key);

        // Try with non existent
        File tempDir = File.createTempFile("user.home","test");
        tempDir.delete(); // Delete
        tempDir.mkdir(); // Recreate as directory
        tempDir.deleteOnExit();
        File vaadinHome = new File(tempDir, ".vaadin");
        vaadinHome.mkdir();
        System.setProperty("user.home", tempDir.getAbsolutePath()); //Change the home location
        String newKey = stats.getUserKey();
        assertNotNull(newKey);
        assertNotSame(keyString,newKey);
        File userKeyFile = new File(vaadinHome, "userKey");
        Assert.assertTrue("userKey should be created automatically",
                userKeyFile.exists());

    }

    @Test
    public void testReadProKey() {
        ApplicationConfiguration configuration = mockAppConfig(true);
        String mavenProjectFolder = TestUtils.getTestFolder("stats-data/maven-project-folder1").toPath().toString();
        System.setProperty("user.home", TestUtils.getTestFolder("stats-data").toPath().toString()); //Change the home location
        VaadinUsageStatistics stats = VaadinUsageStatistics.init(configuration, mavenProjectFolder);

        // File is used by default
        String keyStringFile = "test@vaadin.com/pro-536e1234-test-test-file-f7a1ef311234";
        String keyFile = stats.getProKey();
        assertEquals(keyStringFile, "test@vaadin.com/"+keyFile);

        // Check system property works
        String keyStringProp = "test@vaadin.com/pro-536e1234-test-test-prop-f7a1ef311234";
        System.setProperty("vaadin.proKey", keyStringProp);
        String keyProp = stats.getProKey();
        assertEquals(keyStringProp, "test@vaadin.com/"+keyProp);

    }


    @Test
    public void testLoadStatisticsDisabled() throws Exception {

        // Make sure by default statistics are enabled
        ApplicationConfiguration configuration = mockAppConfig(true);
        Assert.assertFalse(configuration.isProductionMode());
        Assert.assertTrue(configuration.isUsageStatisticsEnabled());

        // Initialize the statistics from Maven project
        String mavenProjectFolder = TestUtils.getTestFolder("stats-data/maven-project-folder1").toPath().toString();
        VaadinUsageStatistics statistics = VaadinUsageStatistics.init(configuration, mavenProjectFolder);

        // Make sure statistics are enabled
        Assert.assertTrue(statistics.isStatisticsEnabled());

        // Disable statistics in config
        Mockito.when(configuration.isUsageStatisticsEnabled()).thenReturn(false);

        // Reinit
        statistics = VaadinUsageStatistics.init(configuration, mavenProjectFolder);

        // Make sure statistics are disabled
        Assert.assertFalse(statistics.isStatisticsEnabled());

        // Enable statistics in config and enable production mode
        Mockito.when(configuration.isUsageStatisticsEnabled()).thenReturn(true);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        // Reinit
        statistics = VaadinUsageStatistics.init(configuration, mavenProjectFolder);

        // Make sure statistics are disabled in production mode
        Assert.assertFalse(statistics.isStatisticsEnabled());

    }

    private ApplicationConfiguration mockAppConfig(boolean enabled) {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(appConfig.isProductionMode()).thenReturn(false);
        Mockito.when(appConfig.isUsageStatisticsEnabled()).thenReturn(enabled);

        return appConfig;
    }

    private void setMock(VaadinUsageStatistics mock) {
        try {
            Field f = VaadinUsageStatistics.class.getDeclaredField("instance");
            f.setAccessible(true);
            AtomicReference<VaadinUsageStatistics> ref = (AtomicReference) f.get(f);
            ref.set(mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Create a temporary file from given test resource.
     *
      * @param testResourceName
     * @return Temporary file
     * @throws IOException
     */
    private static File createTempStorage(String testResourceName) throws IOException {
        File original = new File(TestUtils.getTestResource(testResourceName).getFile());
        File result = File.createTempFile("test", "json");
        result.deleteOnExit();
        FileUtils.copyFile(original, result);
        return result;
    }

    /** Simple HttpServer for testing.
     *
     */
    public static class TestHttpServer implements AutoCloseable {

        public static final int PORT = 8089;
        private HttpServer httpServer;
        private String lastRequestContent;

        public TestHttpServer(int code, String response) throws Exception {
            this.httpServer = createStubGatherServlet(PORT, code,
                    response);
        }

        private HttpServer createStubGatherServlet(int port, int status,
                String response) throws Exception {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(port),
                    0);
            httpServer.createContext("/", exchange -> {
                this.lastRequestContent = IOUtils.toString(exchange.getRequestBody(), Charset.defaultCharset());
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


        @Override public void close() throws Exception {
            if (httpServer != null) {
                httpServer.stop(0);
            }
        }

    }

}

