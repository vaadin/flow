package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class RoutingLinkIT extends PhantomJSTest {

    @Test
    public void testRoutingLinks_insideServletMapping_updateLocation() {
        open();

        verifySamePage();

        testInsideServlet("foo", "foo");
        testInsideServlet("foo/bar", "foo/bar");

        testInsideServlet("./foobar", "foobar");
        testInsideServlet("./foobar?what=not", "foobar?what=not");
        testInsideServlet("./foobar?what=not#fragment",
                "foobar?what=not#fragment");

        testInsideServlet("/run/baz", "baz");

        clickLink("empty");
        verifyInsideServletLocation("");
        verifySamePage();
    }

    @Test
    public void testRoutingLinks_outsideServletMapping_pageChanges() {
        open();

        verifySamePage();

        clickLink("/run");
        verifyNotSamePage();
    }

    // test not enabled because phantomjs ctrl+click is very unreliable
    // @Test
    public void testRoutingLinks_ctrlKeyPresed_noInterception() {
        open();

        verifySamePage();

        Assert.assertEquals("no location",
                findElement(By.id("location")).getText());
        Assert.assertEquals(0, findElements(By.id("linkOriginated")).size());

        new Actions(getDriver()).keyDown(Keys.CONTROL)
                .click(findElement(By.linkText("foo"))).build().perform();

        Assert.assertEquals("no location",
                findElement(By.id("location")).getText());
        Assert.assertEquals(0, findElements(By.id("linkOriginated")).size());
    }

    @Test
    public void testRoutingLinks_externalLink_pageChanges() {
        open();

        verifySamePage();

        clickLink("http://google.com");

        String currentUrl = getDriver().getCurrentUrl();
        Assert.assertTrue("Invalid URL: " + currentUrl,
                currentUrl.startsWith("http://www.google."));
    }

    private void testInsideServlet(String linkToTest,
            String pathAfterServletMapping) {
        clickLink(linkToTest);
        verifyInsideServletLocation(pathAfterServletMapping);
        verifyPopStateEvent(pathAfterServletMapping);
        verifySamePage();
    }

    private void clickLink(String linkText) {
        findElement(By.linkText(linkText)).click();
    }

    private void verifyInsideServletLocation(String pathAfterServletMapping) {
        Assert.assertEquals("Invalid URL",
                getBaseUrl() + "/run/" + pathAfterServletMapping,
                getDriver().getCurrentUrl());
    }

    private void verifyPopStateEvent(String location) {
        Assert.assertEquals("Invalid server side event location", location,
                findElement(By.id("location")).getText());
    }

    private void verifyNotSamePage() {
        Assert.assertEquals(0, findElements(By.id("location")).size());
    }

    private void verifySamePage() {
        Assert.assertNotNull("Page has changed",
                findElement(By.id("location")));
    }
}
