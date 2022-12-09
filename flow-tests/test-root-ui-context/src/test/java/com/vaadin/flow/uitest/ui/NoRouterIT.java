package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NoRouterIT extends ChromeBrowserTest {

    @Test
    @Ignore("Is no router a valid case?")
    public void applicationShouldStart() {
        open();

        WebElement button = findElement(By.tagName("button"));
        button.click();

        Assert.assertEquals(1, findElements(By.className("response")).size());
    }

    @Override
    protected String getTestPath() {
        return "/view/com.vaadin.flow.uitest.ui.NoRouterUI";
    }
}
