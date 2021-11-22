package com.vaadin.viteapp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.vaadin.testbench.TestBenchElement;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

public class ThemeReloadIT extends ViteDevModeIT {

    @Test
    @Ignore
    public void updateStyle_changeIsReloaded() throws IOException {
        TestBenchElement header = $("h2").first();
        Assert.assertEquals("rgba(0, 0, 255, 1)", header.getCssValue("color"));

        File baseDir = new File(System.getProperty("user.dir", "."));
        File themeFolder = new File(baseDir, "frontend/themes/vite-basics/");
        File stylesCss = new File(themeFolder, "styles.css");
        final String stylesContent = FileUtils.readFileToString(stylesCss,
                StandardCharsets.UTF_8);
        try {
            FileUtils.write(stylesCss,
                    stylesContent + "\nh2 { color: rgba(255, 0, 0, 1); }",
                    StandardCharsets.UTF_8.name());

            waitUntil(
                    webDriver -> webDriver.findElement(By.tagName("h2"))
                            .getCssValue("color").equals("rgba(255, 0, 0, 1)"),
                    30);
            header = $("h2").first();
            Assert.assertEquals("rgba(255, 0, 0, 1)",
                    header.getCssValue("color"));
        } finally {
            FileUtils.write(stylesCss, stylesContent,
                    StandardCharsets.UTF_8.name());
        }
    }
}
