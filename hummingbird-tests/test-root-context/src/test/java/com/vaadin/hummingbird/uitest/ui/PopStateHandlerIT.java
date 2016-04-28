package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class PopStateHandlerIT extends PhantomJSTest {

    @Test // issue #640
    public void testSamePathHashChanges_noServerSideEvent() {
        open();

        verifyNoServerVisit();

        String location = getTestURL() + "#foobar";

        getDriver().get(location);

        verifyNoServerVisit();
        verifyInsideServletLocation(location);

        location = getTestURL() + "#shazbot";

        executeScript("window.location.assign('" + location + "');");

        verifyNoServerVisit();
        verifyInsideServletLocation(location);
    }

    private void verifyInsideServletLocation(String url) {
        Assert.assertEquals("Invalid URL", url, getDriver().getCurrentUrl());
    }

    private void verifyNoServerVisit() {
        verifyPopStateEvent("no location");
    }

    private void verifyPopStateEvent(String location) {
        Assert.assertEquals("Invalid server side event location", location,
                findElement(By.id("location")).getText());
    }
}
