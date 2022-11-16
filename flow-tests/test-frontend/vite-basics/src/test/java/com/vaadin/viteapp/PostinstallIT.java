package com.vaadin.viteapp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;

import com.vaadin.testbench.BrowserTest;

public class PostinstallIT extends ViteDevModeIT {

    @BrowserTest
    public void postinstallRanForProject() throws IOException {
        waitForDevServer(); // This is what runs the postinstall script
        Assertions.assertEquals("hello", IOUtils.toString(
                getClass().getClassLoader().getResource("main.postinstall"),
                StandardCharsets.UTF_8));
    }
}
