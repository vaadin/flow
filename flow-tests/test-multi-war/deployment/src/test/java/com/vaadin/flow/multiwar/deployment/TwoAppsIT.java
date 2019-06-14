package com.vaadin.flow.multiwar.deployment;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TwoAppsIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return "/both.html";
    }

    @Test
    public void bothWebComponentsEmbedded() {
        open();
        WebElement hello1 = findElement(By.id("hello1"));
        WebElement hello2 = findElement(By.id("hello2"));

        Assert.assertEquals("Hello from com.vaadin.flow.multiwar.war1.HelloComponent", hello1.getText());
        Assert.assertEquals("Hello from com.vaadin.flow.multiwar.war2.HelloComponent", hello2.getText());
    }

}