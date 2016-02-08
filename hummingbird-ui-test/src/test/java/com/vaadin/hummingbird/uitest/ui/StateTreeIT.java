package com.vaadin.hummingbird.uitest.ui;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.uitest.MultiBrowserTest;

public class StateTreeIT extends MultiBrowserTest {

    @Test
    public void ensureDomContainsSomething() throws Exception {
        open();
        WebElement helloSpan = findElement(By.id("hello"));
        Assert.assertEquals("Hello world", helloSpan.getText());
        compareScreen("smoke");
    }

    private void compareScreen(String referenceId) throws IOException {
        testBench(driver).compareScreen(referenceId);
    }

}
