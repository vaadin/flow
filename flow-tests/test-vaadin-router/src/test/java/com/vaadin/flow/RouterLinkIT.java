/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class RouterLinkIT extends ChromeBrowserTest {

    @Test
    // @Ignore("Ignored because of issue in fusion :
    // https://github.com/vaadin/flow/issues/7575")
    public void testRoutingLinks_insideServletMapping_updateLocation() {
        open();

        verifySamePage();

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
    @Ignore("Ignoring as test functionality is not understood.")
    public void testRoutingLinks_outsideServletMapping_pageChanges() {
        open();

        verifySamePage();

        // What route did this go to redirect outside???
        clickLink("/run");
        verifyNotSamePage();
    }

    @Test
    public void testRoutingLinks_externalLink_pageChanges() {
        open();

        verifySamePage();

        clickLink("https://example.net/");

        String currentUrl = getDriver().getCurrentUrl();

        // Chrome changes url to whatever it can, removing www part, forcing
        // https.
        Assert.assertEquals("Invalid URL: " + currentUrl,
                "https://example.net/", currentUrl);
    }

    @Test
    // @Ignore("Ignored because of issue in fusion :
    // https://github.com/vaadin/flow/issues/7575")
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
