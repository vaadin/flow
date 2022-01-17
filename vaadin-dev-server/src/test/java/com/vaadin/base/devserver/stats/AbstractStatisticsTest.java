package com.vaadin.base.devserver.stats;

import java.io.File;
import java.io.IOException;

import com.vaadin.flow.testutil.TestUtils;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public abstract class AbstractStatisticsTest {

    protected static final String DEFAULT_SERVER_MESSAGE = "{\"reportInterval\":86400,\"serverMessage\":\"\"}";
    protected static final String SERVER_MESSAGE_MESSAGE = "{\"reportInterval\":86400,\"serverMessage\":\"Hello\"}";
    protected static final String SERVER_MESSAGE_3H = "{\"reportInterval\":10800,\"serverMessage\":\"\"}";
    protected static final String SERVER_MESSAGE_48H = "{\"reportInterval\":172800,\"serverMessage\":\"\"}";
    protected static final String SERVER_MESSAGE_40D = "{\"reportInterval\":3456000,\"serverMessage\":\"\"}";
    protected static final String DEFAULT_PROJECT_ID = "12b7fc85f50e8c82cb6f4b03e12f2335";

    protected StatisticsStorage storage;
    protected StatisticsSender sender;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Create a temporary file from given test resource.
     *
     * @param testResourceName
     *            Name of the test resource
     * @return Temporary file
     */
    private File createTempStorage(String testResourceName)
            throws IOException {
        File original = new File(
                TestUtils.getTestResource(testResourceName).getFile());
        File result = temporaryFolder.newFile("test.json");
        FileUtils.copyFile(original, result);
        return result;
    }

    @Before
    public void setup() throws Exception {
        storage = Mockito.spy(new StatisticsStorage());
        sender = Mockito.spy(new StatisticsSender(storage));

        // Change the file storage and reporting parameters for testing
        Mockito.when(sender.getReportingUrl())
                .thenReturn("http://localhost:1234");
        Mockito.when(storage.getUsageStatisticsFile()).thenReturn(
                createTempStorage("stats-data/usage-statistics-1.json"));
    }
}
