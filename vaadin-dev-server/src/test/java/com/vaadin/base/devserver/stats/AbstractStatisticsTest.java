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
package com.vaadin.base.devserver.stats;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.mockito.Mockito;

import com.vaadin.flow.testutil.TestUtils;

public abstract class AbstractStatisticsTest {

    protected static final String DEFAULT_SERVER_MESSAGE = "{\"reportInterval\":86400,\"serverMessage\":\"\"}";
    protected static final String SERVER_MESSAGE_MESSAGE = "{\"reportInterval\":86400,\"serverMessage\":\"Hello\"}";
    protected static final String SERVER_MESSAGE_3H = "{\"reportInterval\":10800,\"serverMessage\":\"\"}";
    protected static final String SERVER_MESSAGE_48H = "{\"reportInterval\":172800,\"serverMessage\":\"\"}";
    protected static final String SERVER_MESSAGE_40D = "{\"reportInterval\":3456000,\"serverMessage\":\"\"}";
    protected static final String DEFAULT_PROJECT_ID = "12b7fc85f50e8c82cb6f4b03e12f2335";

    protected StatisticsStorage storage;
    protected StatisticsSender sender;

    /**
     * Create a temporary file from given test resource.
     *
     * @param testResourceName
     *            Name of the test resource
     * @return Temporary file
     */
    protected static File createTempStorage(String testResourceName)
            throws IOException {
        File original = new File(
                TestUtils.getTestResource(testResourceName).getFile());
        File result = File.createTempFile("test", "json");
        result.deleteOnExit();
        FileUtils.copyFile(original, result);
        return result;
    }

    @Before
    public void setup() throws Exception {
        storage = Mockito.spy(new StatisticsStorage());
        storage.usageStatisticsFile = File.createTempFile("test-storage",
                "json");
        sender = Mockito.spy(new StatisticsSender(storage));
        Mockito.doAnswer(answer -> null).when(sender)
                .triggerSendIfNeeded(Mockito.any());
        // Change the file storage and reporting parameters for testing
        Mockito.when(storage.getUsageStatisticsFile()).thenReturn(
                createTempStorage("stats-data/usage-statistics-1.json"));
    }
}
