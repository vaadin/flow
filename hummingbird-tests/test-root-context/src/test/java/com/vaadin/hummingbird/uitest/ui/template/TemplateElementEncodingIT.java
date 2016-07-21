package com.vaadin.hummingbird.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;
import com.vaadin.testbench.By;

public class TemplateElementEncodingIT extends PhantomJSTest {

    @Test
    public void testEncoding() {
        open();

        waitForElementPresent(By.id("result"));

        WebElement element = findElement(By.id("result"));
        Assert.assertEquals("div span foobar", element.getText());
    }
}
