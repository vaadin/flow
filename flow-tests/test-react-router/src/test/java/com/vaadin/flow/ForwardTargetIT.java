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
