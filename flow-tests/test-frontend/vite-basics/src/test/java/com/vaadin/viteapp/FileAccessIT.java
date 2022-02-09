package com.vaadin.viteapp;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class FileAccessIT extends ViteDevModeIT {

    @Test
    public void expectedFoldersAccessible() throws Exception {
        /*
         * This just tests a few sample folders to see that there is not a
         * fundamental problem
         */
        assertAllowed("target/flow-frontend/Flow.js");
        assertAllowed("target/frontend/generated-flow-imports.js");
        assertAllowed("frontend/jsonloader.js");
    }

    private void assertAllowed(String fileInProject) throws IOException {
        String result = IOUtils.toString(getFsUrl(fileInProject),
                StandardCharsets.UTF_8);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void mostFoldersNotAccessible() throws Exception {
        /*
         * This just tests a few sample folders to see that there is not a
         * fundamental problem
         */
        assertDenied("target/vaadin-dev-server-settings.json");
        assertDenied("pom.xml");
        assertDenied("../pom.xml");
    }

    private void assertDenied(String fileInProject) {
        try {
            URL url = getFsUrl(fileInProject);
            IOUtils.toString(url, StandardCharsets.UTF_8);
            Assert.fail("Request for " + url + " should not succeed");
        } catch (IOException e) {
            Assert.assertTrue(
                    "Request for " + fileInProject + " should have failed",
                    e.getMessage().contains(
                            "Server returned HTTP response code: 403"));
        }

    }

    private URL getFsUrl(String fileInProject) throws IOException {
        // For Windows, the URLs should be like
        // http://localhost:8888/VAADIN/@fs/C:/Code/flow/flow-tests/test-frontend/vite-basics/target/vaadin-dev-server-settings.json

        String currentPath = new java.io.File(".").getCanonicalPath()
                .replace("\\", "/");
        if (!currentPath.startsWith("/")) {
            currentPath = "/" + currentPath;
        }
        return new URL("http://localhost:8888/VAADIN/@fs" + currentPath + "/"
                + fileInProject);
    }

}
