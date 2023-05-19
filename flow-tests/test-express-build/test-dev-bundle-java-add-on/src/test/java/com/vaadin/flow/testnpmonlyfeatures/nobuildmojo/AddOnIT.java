package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class AddOnIT extends ChromeBrowserTest {

    @Before
    public void init() {
        open();
    }

    @Override
    protected String getTestPath() {
        return "/view/com.vaadin.flow.addon.AddOnView";
    }

    @Test
    public void selectElementExists() {
        final WebElement select = findElement(By.cssSelector("select"));
        Assert.assertTrue("'select' element not visible", select.isDisplayed());
    }

    @Test
    public void noFrontendFilesCreated() {
        File baseDir = new File(System.getProperty("user.dir", "."));

        // shouldn't create a dev-bundle
        Assert.assertFalse("No dev-bundle should be created",
                new File(baseDir, Constants.DEV_BUNDLE_LOCATION).exists());
        Assert.assertFalse("No node_modules should be created",
                new File(baseDir, "node_modules").exists());

        Assert.assertFalse("No package.json should be created",
                new File(baseDir, "package.json").exists());
        Assert.assertFalse("No package-lock.json should be created",
                new File(baseDir, "package-lock.json").exists());
        Assert.assertFalse("No vite generated should be created",
                new File(baseDir, "vite.generated.ts").exists());
        Assert.assertFalse("No vite config should be created",
                new File(baseDir, "vite.config.ts").exists());
        Assert.assertFalse("No types should be created",
                new File(baseDir, "types.d.ts").exists());
        Assert.assertFalse("No tsconfig should be created",
                new File(baseDir, "tsconfig.json").exists());
        Assert.assertFalse("No package-lock.yaml should be created",
                new File(baseDir, "package-lock.yaml").exists());
        Assert.assertFalse("No .npmrc should be created",
                new File(baseDir, ".npmrc").exists());
        Assert.assertFalse("No .pnpmfile.cjs should be created",
                new File(baseDir, ".pnpmfile.cjs").exists());
    }
}
