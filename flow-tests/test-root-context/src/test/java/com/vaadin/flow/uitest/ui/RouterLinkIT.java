package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class RouterLinkIT extends ChromeBrowserTest {


    @Test
    public void testRoutingLinks_insideServletMapping_updateLocation() {
        open();

        verifySamePage();

        if (hasClientIssue("7575")) {
            return;
        }

        testInsideServlet("foo", "foo", "", "foo");
        testInsideServlet("./foobar", "foobar", "", "foobar");
        testInsideServlet("foo/bar", "foo/bar", "", "foo/bar");

        testInsideServlet("./foobar?what=not", "foobar", "what=not",
                "foobar?what=not");

        testInsideServlet("/view/baz", "baz", "", "baz");

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

        clickLink("http://example.net/");

        String currentUrl = getDriver().getCurrentUrl();

        // Chrome changes url to whatever it can, removing www part, forcing
        // https.
        Assert.assertTrue("Invalid URL: " + currentUrl,
                currentUrl.equals("http://example.net/"));
    }

    @Test
    public void testImageInsideRouterLink() {
        open();

        verifySamePage();

        findElement(By.tagName("img")).click();

        verifyInsideServletLocation("image/link");

        if (hasClientIssue("7575")) {
            return;
        }

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
                getRootURL() + "/view/" + pathAfterServletMapping,
                getDriver().getCurrentUrl());
    }

    private void verifyParametersQuery(String parametersQuery) {
        Assert.assertEquals("Invalid server side event location",
                parametersQuery, findElement(By.id("queryParams")).getText());
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
