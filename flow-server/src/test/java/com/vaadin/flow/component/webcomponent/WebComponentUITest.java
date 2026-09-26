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
package com.vaadin.flow.component.webcomponent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebComponentUITest {

    @Test
    void addAttributes_callsTheDefinitionWithTheTagAndTheAttributes() {
        WebComponentUI ui = new WebComponentUI();
        MockVaadinSession session = new AlwaysLockedVaadinSession(
                new MockVaadinServletService());
        ui.getInternals().setSession(session);

        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("theme", "dark");
        attributes.put("data-size", "small");
        ui.addAttributes("my-component", attributes);

        List<PendingJavaScriptInvocation> invocations = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, invocations.size());
        assertEquals(
                new JsCall(WebComponentUI.ThemeAttributesJs.class,
                        "setAttributes",
                        List.of("my-component",
                                JacksonUtils.mapToJson(attributes))),
                invocations.get(0).getInvocation().getJsCall());
    }
}
