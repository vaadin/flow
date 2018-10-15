package com.vaadin.test.osgi;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class KarafIntegrationIT extends ChromeBrowserTest {

    private static final String URL_PREFIX = "http://localhost:8181/";
    private static final String APP1_URL = URL_PREFIX + "myapp1";
    private static final String APP2_URL = URL_PREFIX + "myapp2";

    @Test
    public void testApp1() {
        runBasicTest(APP1_URL, "bar");
        WebElement element = getDriver().findElement(By.id("bundle-button"));
    }

    @Test
    public void testApp2() {
        runBasicTest(APP2_URL, "foo");
    }

    private void runBasicTest(String app1Url, String text) {
        getDriver().navigate().to(app1Url);

        getDriver().findElement(By.id("bundle-input")).sendKeys(text);
        getDriver().findElement(By.id("bundle-button")).click();
        String foundText = getDriver().findElement(By.id("message")).getText();
        Assert.assertEquals("Thanks " + text + ", it works!", foundText);
    }

}
