package com.vaadin.flow.contexttest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class RootContextIT extends ChromeBrowserTest {

    private static final String JETTY_CONTEXT = "/custom-frontend-context";

    protected String getAppContext() {
        return "";
    }

    @Override
    protected String getTestPath() {
        return JETTY_CONTEXT + getAppContext();
    }

    @Test
    public void testStaticResource() {
        open();
        verifyFrontend();
    }

    private void verifyFrontend() {

        Assert.assertEquals("Piece of ES6 works from bundled frontend resources", findElementById("es6-div").getText());
    }

    protected WebElement findElementById(String id) {
        return findElement(By.id(id));
    }
}
