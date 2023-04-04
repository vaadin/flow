package com.vaadin.viteapp;

import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import com.vaadin.flow.testutil.ChromeBrowserTest;

abstract public class ViteDevModeIT extends ChromeBrowserTest {

    @Before
    public void openView() {
        getDriver().get(getRootURL());
        waitForDevServer();
        getCommandExecutor().waitForVaadin();
        waitUntil(ExpectedConditions
                .presenceOfElementLocated(By.id("loadAndShowJson")), 300);
    }
}
