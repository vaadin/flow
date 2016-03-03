package com.vaadin.client.hummingbird;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class ClientEngineSizeIT {

    @Test
    public void testClientEngineSize() throws Exception {
        File compiledModuleFolder = new File(
                "target/classes/META-INF/resources/VAADIN/client");
        if (!compiledModuleFolder.exists()) {
            throw new IOException(
                    "Folder with compiled client engine does not exist: "
                            + compiledModuleFolder.getAbsolutePath());
        }

        boolean cacheJsReported = false;
        boolean cacheJsGzReported = false;

        for (File f : compiledModuleFolder.listFiles()) {
            if (f.getName().endsWith(".cache.js")) {
                if (cacheJsReported) {
                    throw new IOException(
                            "Multiple uncompresed cache.js files found!");
                }
                printTeamcityStats("clientEngine", f.length());
                cacheJsReported = true;
            } else if (f.getName().endsWith(".cache.js.gz")) {
                if (cacheJsGzReported) {
                    throw new IOException(
                            "Multiple compressed cache.js.gz files found!");
                }
                printTeamcityStats("clientEngineGzipped", f.length());
                cacheJsGzReported = true;
            }
        }
        if (!cacheJsReported) {
            throw new IOException("Uncompressed cache.js file not found!");
        }
        if (!cacheJsGzReported) {
            throw new IOException("Compressed cache.js.gz file not found!");
        }
    }

    private void printTeamcityStats(String key, long value) {
        // ##teamcity[buildStatisticValue key=&#39;&lt;valueTypeKey&gt;&#39;
        // value=&#39;&lt;value&gt;&#39;]
        System.out.println("##teamcity[buildStatisticValue key='" + key
                + "' value='" + value + "']");

    }

}