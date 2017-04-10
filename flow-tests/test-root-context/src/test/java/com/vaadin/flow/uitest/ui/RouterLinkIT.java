package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.PhantomJSTest;

public class RouterLinkIT extends PhantomJSTest {

    @Test
    public void testRoutingLinks_insideServletMapping_updateLocation() {
        open();

        verifySamePage();

        testInsideServlet("foo", "foo", "", "foo");
        testInsideServlet("./foobar", "foobar", "", "foobar");
        testInsideServlet("foo/bar", "foo/bar", "", "foo/bar");

        testInsideServlet("./foobar?what=not", "foobar", "what=not",
                "foobar?what=not");

        testInsideServlet("/run/baz", "baz", "", "baz");

        testInsideServlet("./foobar?what=not#fragment", "foobar", "what=not",
                "foobar?what=not#fragment");

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

    @Test
    public void testRoutingLinks_externalLink_pageChanges() {
        open();

        verifySamePage();

        clickLink("http://google.com");

        String currentUrl = getDriver().getCurrentUrl();
        Assert.assertTrue("Invalid URL: " + currentUrl,
                currentUrl.startsWith("http://www.google."));
    }

    @Test
    public void testImageInsideRouterLink() {
        open();

        verifySamePage();

        findElement(By.tagName("img")).click();

        verifyInsideServletLocation("image/link");
        verifyPopStateEvent("image/link");
    }

    private void testInsideServlet(String linkToTest, String popStateLocation,
            String parametersQuery, String pathAfterServletMapping) {
        clickLink(linkToTest);
        verifyInsideServletLocation(pathAfterServletMapping);
        verifyParametersQuery(parametersQuery);
        verifyPopStateEvent(popStateLocation);
        verifySamePage();
    }

    private void clickLink(String linkText) {
        findElement(By.linkText(linkText)).click();
    }

    private void verifyInsideServletLocation(String pathAfterServletMapping) {
        Assert.assertEquals("Invalid URL",
                getRootURL() + "/run/" + pathAfterServletMapping,
                getDriver().getCurrentUrl());
    }

    private void verifyParametersQuery(String parametersQuery) {
        Assert.assertEquals("Invalid server side event location", parametersQuery,
                findElement(By.id("queryParams")).getText());
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
