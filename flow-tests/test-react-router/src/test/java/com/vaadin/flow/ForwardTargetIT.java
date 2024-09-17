package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ForwardTargetIT extends ChromeBrowserTest {

    // Test for https://github.com/vaadin/flow/issues/19794
    @Test
    public void testUrlIsCorrectAfterForward() {
        open();

        waitUntil(arg -> driver.getCurrentUrl().endsWith("ForwardTargetView"));

        openUrl("/view/com.vaadin.flow.ForwardingView");

        waitUntil(arg -> driver.getCurrentUrl().endsWith("ForwardTargetView"));

        Assert.assertTrue(driver.getCurrentUrl()
                .endsWith("com.vaadin.flow.ForwardTargetView"));
    }
}
