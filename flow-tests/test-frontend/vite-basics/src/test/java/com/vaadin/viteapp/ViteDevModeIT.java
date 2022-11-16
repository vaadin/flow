package com.vaadin.viteapp;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;

abstract public class ViteDevModeIT extends ChromeBrowserTest {
    @BeforeAll
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void openView() {
        getDriver().get(getRootURL());
        waitForDevServer();
        getCommandExecutor().waitForVaadin();
        waitUntil(ExpectedConditions
                .presenceOfElementLocated(By.id("loadAndShowJson")), 300);
    }
}
