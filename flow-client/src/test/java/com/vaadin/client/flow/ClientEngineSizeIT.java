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
