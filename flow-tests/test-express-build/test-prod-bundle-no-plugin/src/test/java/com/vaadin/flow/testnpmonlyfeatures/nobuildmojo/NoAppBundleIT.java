package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.BundleValidationUtil;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NoAppBundleIT extends ChromeBrowserTest {

    @Before
    public void init() {
        open();
    }

    @Test
    public void noFrontendFilesCreated() throws IOException {
        waitForElementPresent(By.id("hello-component"));

        File baseDir = new File(System.getProperty("user.dir", "."));

        // should use default prod bundle
        assertDefaultProdBundle(baseDir);

        Assert.assertFalse("No node_modules should be created",
                new File(baseDir, "node_modules").exists());

        Assert.assertFalse("No package.json should be created",
                new File(baseDir, "package.json").exists());
        Assert.assertFalse("No vite generated should be created",
                new File(baseDir, "vite.generated.ts").exists());
        Assert.assertFalse("No vite config should be created",
                new File(baseDir, "vite.config.ts").exists());

        Assert.assertFalse("No types should be created",
                new File(baseDir, "types.d.ts").exists());
        Assert.assertFalse("No tsconfig should be created",
                new File(baseDir, "tsconfig.json").exists());
    }

    @Test
    public void serviceWorkerIsIncludedAndServed() {
        getDriver().get(getRootURL() + "/view/sw.js");
        String pageSource = driver.getPageSource();
        Assert.assertFalse("Service Worker is not served properly",
                pageSource.contains("Error 404 Not Found")
                        || pageSource.contains("Could not navigate to"));
    }

    private void assertDefaultProdBundle(File baseDir) throws IOException {
        File indexHtml = new File(baseDir,
                "target/classes/META-INF/VAADIN/webapp/index.html");
        Assert.assertTrue("Prod bundle should be copied", indexHtml.exists());
        String indexHtmlContent = FileUtils.readFileToString(indexHtml,
                StandardCharsets.UTF_8);
        Assert.assertTrue("Expected default production bundle to be used",
                indexHtmlContent.contains("default production bundle"));
    }
}
