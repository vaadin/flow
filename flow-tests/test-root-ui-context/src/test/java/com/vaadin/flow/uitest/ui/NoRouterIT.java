package com.vaadin.flow.uitest.ui;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;

public class NoRouterIT extends ChromeBrowserTest {

    @BrowserTest
    public void applicationShouldStart() {
        open();

        WebElement button = findElement(By.tagName("button"));
        button.click();

        Assertions.assertEquals(1,
                findElements(By.className("response")).size());
    }

}
