package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ForwardTargetIT extends ChromeBrowserTest {

    public static final String FORWARD_TARGET_VIEW = "/view/com.vaadin.flow.ForwardTargetView";

    // Test for https://github.com/vaadin/flow/issues/19794
    @Test
    public void testUrlIsCorrectAfterForward() {
        getDriver().get(getTestURL(getRootURL(), FORWARD_TARGET_VIEW, null));

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(FORWARD_TARGET_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + FORWARD_TARGET_VIEW);
        }

        getDriver().get(getTestURL(getRootURL(),
                "/view/com.vaadin.flow.ForwardingView", null));

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(FORWARD_TARGET_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + FORWARD_TARGET_VIEW);
        }

        Assert.assertTrue("URL was not the expected one after forward call",
                driver.getCurrentUrl().endsWith(FORWARD_TARGET_VIEW));
    }

    // Test for https://github.com/vaadin/flow/issues/19822
    @Test
    public void testSetParameterCalledOnlyOnceAfterForward() {
        getDriver().get(getTestURL(getRootURL(),
                "/view/com.vaadin.flow.ForwardingToParametersView", null));

        try {
            waitUntil(arg -> driver.getCurrentUrl().endsWith(
                    "/view/com.vaadin.flow.ForwardTargetWithParametersView"));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + "/view/com.vaadin.flow.ForwardTargetWithParametersView");
        }

        Assert.assertEquals("setParameter was called more than once", 1,
                $(SpanElement.class).all().stream()
                        .filter(span -> span.getText().equals("setParameter"))
                        .count());
    }

    // Test for https://github.com/vaadin/flow/issues/19822
    @Test
    public void testRouterLinkSetParameterCalledOnlyOnceAfterForward() {
        getDriver().get(getTestURL(getRootURL(),
                "/view/com.vaadin.flow.RouterLinkForwardingToParametersView",
                null));
        $("a").id("forwardViewLink").click();

        try {
            waitUntil(arg -> driver.getCurrentUrl().endsWith(
                    "/view/com.vaadin.flow.ForwardTargetWithParametersView"));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + "/view/com.vaadin.flow.ForwardTargetWithParametersView");
        }

        Assert.assertEquals("setParameter was called more than once", 1,
                $(SpanElement.class).all().stream()
                        .filter(span -> span.getText().equals("setParameter"))
                        .count());
    }

}
