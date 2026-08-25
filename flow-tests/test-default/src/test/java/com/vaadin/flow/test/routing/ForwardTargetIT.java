/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.routing;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(ForwardTargetView.class)
public class ForwardTargetIT extends AbstractDefaultIT {

    public static final String FORWARD_TARGET_VIEW = "/com.vaadin.flow.ForwardTargetView";

    // Test for https://github.com/vaadin/flow/issues/19794
    @BrowserTest
    public void testUrlIsCorrectAfterForward() {
        getDriver().get(getRootURL() + FORWARD_TARGET_VIEW);

        try {
            waitUntil(arg -> getDriver().getCurrentUrl()
                    .endsWith(FORWARD_TARGET_VIEW));
        } catch (TimeoutException e) {
            Assertions.fail("URL wasn't updated to expected one: "
                    + FORWARD_TARGET_VIEW);
        }

        getDriver().get(getRootURL() + "/com.vaadin.flow.ForwardingView");

        try {
            waitUntil(arg -> getDriver().getCurrentUrl()
                    .endsWith(FORWARD_TARGET_VIEW));
        } catch (TimeoutException e) {
            Assertions.fail("URL wasn't updated to expected one: "
                    + FORWARD_TARGET_VIEW);
        }

        Assertions.assertTrue(
                getDriver().getCurrentUrl().endsWith(FORWARD_TARGET_VIEW),
                "URL was not the expected one after forward call");
    }

    // Test for https://github.com/vaadin/flow/issues/19822
    @BrowserTest
    public void testSetParameterCalledOnlyOnceAfterForward() {
        getDriver().get(
                getRootURL() + "/com.vaadin.flow.ForwardingToParametersView");

        try {
            waitUntil(arg -> getDriver().getCurrentUrl().endsWith(
                    "/com.vaadin.flow.ForwardTargetWithParametersView"));
        } catch (TimeoutException e) {
            Assertions.fail("URL wasn't updated to expected one: "
                    + "/com.vaadin.flow.ForwardTargetWithParametersView");
        }

        Assertions.assertEquals(1, $(SpanElement.class).all().stream()
                .filter(span -> span.getText().equals("setParameter")).count(),
                "setParameter was called more than once");
    }

    // Test for https://github.com/vaadin/flow/issues/19822
    @BrowserTest
    public void testRouterLinkSetParameterCalledOnlyOnceAfterForward() {
        getDriver().get(getRootURL()
                + "/com.vaadin.flow.RouterLinkForwardingToParametersView");
        $("a").id("forwardViewLink").click();

        try {
            waitUntil(arg -> getDriver().getCurrentUrl().endsWith(
                    "/com.vaadin.flow.ForwardTargetWithParametersView"));
        } catch (TimeoutException e) {
            Assertions.fail("URL wasn't updated to expected one: "
                    + "/com.vaadin.flow.ForwardTargetWithParametersView");
        }

        Assertions.assertEquals(1, $(SpanElement.class).all().stream()
                .filter(span -> span.getText().equals("setParameter")).count(),
                "setParameter was called more than once");
    }

}
