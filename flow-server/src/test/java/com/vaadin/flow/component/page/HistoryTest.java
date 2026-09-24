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
package com.vaadin.flow.component.page;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.BaseJsonNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.js.JsCall;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryTest {

    private MockUI ui;
    private History history;

    @BeforeEach
    void setup() {
        ui = MockUI.createUI();
        history = ui.getPage().getHistory();
        // The history writes the entry itself unless the client side router
        // takes the navigation, which is what each _react case turns on
        useReactRouter(false);
    }

    private void useReactRouter(boolean reactEnabled) {
        ((MockDeploymentConfiguration) ui.getSession().getService()
                .getDeploymentConfiguration()).setReactEnabled(reactEnabled);
    }

    @Test
    void pushState_locationWithQueryParameters_queryParametersRetained() {
        BaseJsonNode state = JacksonUtils.readTree("{'foo':'bar'}");

        history.pushState(state, "context/view?param=4");
        assertEquals(call("pushState", state, "context/view?param=4"),
                ui.onlyScheduledJsCall());

        history.pushState(state, "context/view/?param=4");
        assertEquals(call("pushState", state, "context/view/?param=4"),
                ui.onlyScheduledJsCall());
    }

    @Test
    void pushState_locationWithFragment_fragmentRetained() {
        history.pushState(null, "context/view#foobar");
        assertEquals(call("pushState", null, "context/view#foobar"),
                ui.onlyScheduledJsCall());

        history.pushState(null, "context/view/#foobar");
        assertEquals(call("pushState", null, "context/view/#foobar"),
                ui.onlyScheduledJsCall());
    }

    @Test // #11628
    void pushState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained() {
        history.pushState(null, "context/view?foo=bar#foobar");
        assertEquals(call("pushState", null, "context/view?foo=bar#foobar"),
                ui.onlyScheduledJsCall());

        history.pushState(null, "context/view/?foo=bar#foobar");
        assertEquals(call("pushState", null, "context/view/?foo=bar#foobar"),
                ui.onlyScheduledJsCall());
    }

    @Test // #11628
    void replaceState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained() {
        history.replaceState(null, "context/view?foo=bar#foobar");
        assertEquals(call("replaceState", null, "context/view?foo=bar#foobar"),
                ui.onlyScheduledJsCall());

        history.replaceState(null, "context/view/?foo=bar#foobar");
        assertEquals(call("replaceState", null, "context/view/?foo=bar#foobar"),
                ui.onlyScheduledJsCall());
    }

    @Test // #11628
    void replaceState_locationEmpty_pushesPeriod() {
        history.replaceState(null, "");

        assertEquals(call("replaceState", null, "."), ui.onlyScheduledJsCall(),
                "an empty location should be written as '.'");
    }

    @Test
    void pushState_locationWithQueryParameters_queryParametersRetained_react() {
        useReactRouter(true);
        BaseJsonNode state = JacksonUtils.readTree("{'foo':'bar'}");

        history.pushState(state, "context/view?param=4");
        assertEquals(
                call("navigatePushing", state, "context/view?param=4", false),
                ui.onlyScheduledJsCall());

        history.pushState(state, "context/view/?param=4");
        assertEquals(
                call("navigatePushing", state, "context/view/?param=4", false),
                ui.onlyScheduledJsCall());
    }

    @Test
    void pushState_locationWithFragment_fragmentRetained_react() {
        useReactRouter(true);

        history.pushState(null, "context/view#foobar");
        assertEquals(
                call("navigatePushing", null, "context/view#foobar", false),
                ui.onlyScheduledJsCall());

        history.pushState(null, "context/view/#foobar");
        assertEquals(
                call("navigatePushing", null, "context/view/#foobar", false),
                ui.onlyScheduledJsCall());
    }

    @Test // #11628
    void pushState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained_react() {
        useReactRouter(true);

        history.pushState(null, "context/view?foo=bar#foobar");
        assertEquals(call("navigatePushing", null,
                "context/view?foo=bar#foobar", false),
                ui.onlyScheduledJsCall());

        history.pushState(null, "context/view/?foo=bar#foobar");
        assertEquals(call("navigatePushing", null,
                "context/view/?foo=bar#foobar", false),
                ui.onlyScheduledJsCall());
    }

    @Test // #11628
    void replaceState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained_react() {
        useReactRouter(true);

        history.replaceState(null, "context/view?foo=bar#foobar");
        assertEquals(call("navigateReplacing", null,
                "context/view?foo=bar#foobar", false),
                ui.onlyScheduledJsCall());

        history.replaceState(null, "context/view/?foo=bar#foobar");
        assertEquals(call("navigateReplacing", null,
                "context/view/?foo=bar#foobar", false),
                ui.onlyScheduledJsCall());
    }

    @Test // #11628
    void replaceState_locationEmpty_pushesPeriod_react() {
        useReactRouter(true);

        history.replaceState(null, "");

        assertEquals(call("navigateReplacing", null, ".", false),
                ui.onlyScheduledJsCall(),
                "an empty location should be written as '.'");
    }

    @Test
    void writingAnEntry_declaresWhichOfThePairItIs() {
        // Pushing and replacing differ only in the function called, and the
        // two navigations only in the flag they carry, so a swap of either
        // pair would not show in the method that was called
        history.pushState(null, "view");
        assertTrue(ui.onlyScheduledJsCall().getExpression()
                .contains("window.history.pushState("));

        history.replaceState(null, "view");
        assertTrue(ui.onlyScheduledJsCall().getExpression()
                .contains("window.history.replaceState("));

        useReactRouter(true);

        history.pushState(null, "view");
        assertTrue(ui.onlyScheduledJsCall().getExpression()
                .contains("replace: false"));

        history.replaceState(null, "view");
        assertTrue(ui.onlyScheduledJsCall().getExpression()
                .contains("replace: true"));
    }

    @Test
    void navigatingTheHistory_runsTheDeclaredJavaScript() {
        // The three of them differ only in what they ask the browser for
        history.back();
        assertEquals(call("back"), ui.onlyScheduledJsCall());

        history.forward();
        assertEquals(call("forward"), ui.onlyScheduledJsCall());

        history.go(-2);
        assertEquals(call("go", -2), ui.onlyScheduledJsCall(),
                "steps should be passed on");
    }

    private static JsCall call(String methodName, Object... arguments) {
        return new JsCall(History.HistoryJs.class, methodName,
                Arrays.asList(arguments));
    }
}
