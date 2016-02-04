package com.vaadin.client.hummingbird;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

public class ClientEngineSizeIT {

    @Test
    public void testClientEngineSize() throws Exception {
        File compiledModuleFolder = new File(
                "target/classes/VAADIN/client/com.vaadin.ClientEngine");
        if (!compiledModuleFolder.exists()) {
            throw new IOException(
                    "Folder with compiled client engine does not exist: "
                            + compiledModuleFolder.getAbsolutePath());
        }

        for (File f : compiledModuleFolder.listFiles()) {
            if (f.getName().endsWith(".cache.js")) {
                printTeamcityStats("clientEngine", getSize(f));
            } else if (f.getName().endsWith(".cache.js.gz")) {
                printTeamcityStats("clientEngineGzipped", getSize(f));
            }
        }
    }

    private void printTeamcityStats(String key, long value) {
        // ##teamcity[buildStatisticValue key=&#39;&lt;valueTypeKey&gt;&#39;
        // value=&#39;&lt;value&gt;&#39;]
        System.out.println("##teamcity[buildStatisticValue key='" + key
                + "' value='" + value + "']");

    }

    private long getSize(File file) throws IOException {
        return Files.size(file.toPath());
    }
}