package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BackButtonServerRoundTripIT extends ChromeBrowserTest {
    @Test
    public void testForwardingToViewInSetParameter() {
        final String baseLoc =
                "/view/com.vaadin.flow.uitest.ui.BackButtonServerRoundTripView";
        getDriver().get(getRootURL() + baseLoc + "/1?query=foo");

        WebElement button = findElement(By.id(BackButtonServerRoundTripView.BUTTON_ID));
        button.click();

        final String queryValue0 = findElement(
                By.id(BackButtonServerRoundTripView.QUERY_LABEL_ID)).getText();
        Assert.assertTrue("should have received query parameter value 'bar'",
                queryValue0.equals("query=bar"));

        getDriver().navigate().back();

        final String queryValue1 = findElement(
                By.id(BackButtonServerRoundTripView.QUERY_LABEL_ID)).getText();
        Assert.assertTrue("should have received query parameter value 'foo'",
                queryValue1.equals("query=foo"));
    }
}


