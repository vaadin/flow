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
package com.vaadin.client.flow;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.vaadin.flow.shared.ApplicationConstants;

public class ClientEngineSizeIT {

    @Test
    public void testClientEngineSize() throws Exception {
        File compiledModuleFolder = new File(
                "target/classes/META-INF/resources/"
                        + ApplicationConstants.CLIENT_ENGINE_PATH);
        if (!compiledModuleFolder.exists()) {
            throw new IOException(
                    "Folder with compiled client engine does not exist: "
                            + compiledModuleFolder.getAbsolutePath());
        }

        boolean cacheJsReported = false;

        for (File f : compiledModuleFolder.listFiles()) {
            if (f.getName().endsWith(".cache.js")) {
                if (cacheJsReported) {
                    throw new IOException(
                            "Multiple uncompressed cache.js files found!");
                }
                printTeamcityStats("clientEngine", f.length());
                cacheJsReported = true;
            }
        }
        if (!cacheJsReported) {
            throw new IOException("Uncompressed cache.js file not found!");
        }
    }

    private void printTeamcityStats(String key, long value) {
        // ##teamcity[buildStatisticValue key=&#39;&lt;valueTypeKey&gt;&#39;
        // value=&#39;&lt;value&gt;&#39;]
        System.out.println("##teamcity[buildStatisticValue key='" + key
                + "' value='" + value + "']");

    }

}
