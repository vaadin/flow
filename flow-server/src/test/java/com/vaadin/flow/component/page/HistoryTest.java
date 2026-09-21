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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinitionProxy;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoryTest {

    private class TestUI extends UI {
        @Override
        public Page getPage() {
            return page;
        }

        @Override
        public VaadinSession getSession() {
            return session;
        }
    }

    private class TestPage extends Page {

        private JsCall call;

        public TestPage(UI ui) {
            super(ui);
        }

        @Override
        public <T> T executeJs(Class<T> definitionType) {
            // The UI of this test has no session to schedule an invocation
            // with, so the call is recorded rather than run
            return JsDefinitionProxy.create(definitionType, recorded -> {
                call = recorded;
                return null;
            });
        }
    }

    private TestUI ui = new TestUI();
    private TestPage page = new TestPage(ui);
    private History history;

    private VaadinService service = Mockito.mock(VaadinService.class);
    private VaadinSession session = Mockito.mock(VaadinSession.class);
    private DeploymentConfiguration configuration;

    @BeforeEach
    void setup() {
        history = new History(ui);
        configuration = Mockito.mock(DeploymentConfiguration.class);

        Mockito.when(session.getService()).thenReturn(service);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.isReactEnabled()).thenReturn(false);
    }

    @Test
    void pushState_locationWithQueryParameters_queryParametersRetained() {
        history.pushState(JacksonUtils.readTree("{'foo':'bar'}"),
                "context/view?param=4");

        assertEquals("pushState", page.call.methodName(),
                "push state not scheduled");
        assertEquals("{\"foo\":\"bar\"}",
                ((JsonNode) page.call.arguments().get(0)).toString(),
                "push state not included");
        assertEquals("context/view?param=4", page.call.arguments().get(1),
                "invalid location");

        history.pushState(JacksonUtils.readTree("{'foo':'bar'}"),
                "context/view/?param=4");

        assertEquals("pushState", page.call.methodName(),
                "push state not scheduled");
        assertEquals("{\"foo\":\"bar\"}",
                ((JsonNode) page.call.arguments().get(0)).toString(),
                "push state not included");
        assertEquals("context/view/?param=4", page.call.arguments().get(1),
                "invalid location");
    }

    @Test
    void pushState_locationWithFragment_fragmentRetained() {
        history.pushState(null, "context/view#foobar");

        assertEquals("pushState", page.call.methodName(),
                "push state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view#foobar", page.call.arguments().get(1),
                "fragment not retained");

        history.pushState(null, "context/view/#foobar");

        assertEquals("pushState", page.call.methodName(),
                "push state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view/#foobar", page.call.arguments().get(1),
                "fragment not retained");
    }

    @Test // #11628
    void pushState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained() {
        history.pushState(null, "context/view?foo=bar#foobar");

        assertEquals("pushState", page.call.methodName(),
                "push state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view?foo=bar#foobar",
                page.call.arguments().get(1), "invalid location");

        history.pushState(null, "context/view/?foo=bar#foobar");

        assertEquals("pushState", page.call.methodName(),
                "push state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view/?foo=bar#foobar",
                page.call.arguments().get(1), "invalid location");
    }

    @Test // #11628
    void replaceState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained() {
        history.replaceState(null, "context/view?foo=bar#foobar");

        assertEquals("replaceState", page.call.methodName(),
                "replace state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view?foo=bar#foobar",
                page.call.arguments().get(1), "invalid location");

        history.replaceState(null, "context/view/?foo=bar#foobar");

        assertEquals("replaceState", page.call.methodName(),
                "replace state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view/?foo=bar#foobar",
                page.call.arguments().get(1), "invalid location");
    }

    @Test // #11628
    void replaceState_locationEmpty_pushesPeriod() {
        history.replaceState(null, "");
        assertEquals("replaceState", page.call.methodName(),
                "replace state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals(".", page.call.arguments().get(1),
                "location should be '.'");
    }

    @Test
    void pushState_locationWithQueryParameters_queryParametersRetained_react() {
        Mockito.when(configuration.isReactEnabled()).thenReturn(true);
        history.pushState(JacksonUtils.readTree("{'foo':'bar'}"),
                "context/view?param=4");

        assertEquals("navigate", page.call.methodName(),
                "push state not scheduled");
        assertEquals("{\"foo\":\"bar\"}",
                ((JsonNode) page.call.arguments().get(0)).toString(),
                "push state not included");
        assertEquals("context/view?param=4", page.call.arguments().get(1),
                "invalid location");

        history.pushState(JacksonUtils.readTree("{'foo':'bar'}"),
                "context/view/?param=4");

        assertEquals("navigate", page.call.methodName(),
                "push state not scheduled");
        assertEquals("{\"foo\":\"bar\"}",
                ((JsonNode) page.call.arguments().get(0)).toString(),
                "push state not included");
        assertEquals("context/view/?param=4", page.call.arguments().get(1),
                "invalid location");
    }

    @Test
    void pushState_locationWithFragment_fragmentRetained_react() {
        Mockito.when(configuration.isReactEnabled()).thenReturn(true);
        history.pushState(null, "context/view#foobar");

        assertEquals("navigate", page.call.methodName(),
                "push state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view#foobar", page.call.arguments().get(1),
                "fragment not retained");

        history.pushState(null, "context/view/#foobar");

        assertEquals("navigate", page.call.methodName(),
                "push state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view/#foobar", page.call.arguments().get(1),
                "fragment not retained");
    }

    @Test // #11628
    void pushState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained_react() {
        Mockito.when(configuration.isReactEnabled()).thenReturn(true);
        history.pushState(null, "context/view?foo=bar#foobar");

        assertEquals("navigate", page.call.methodName(),
                "push state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view?foo=bar#foobar",
                page.call.arguments().get(1), "invalid location");

        history.pushState(null, "context/view/?foo=bar#foobar");

        assertEquals("navigate", page.call.methodName(),
                "push state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view/?foo=bar#foobar",
                page.call.arguments().get(1), "invalid location");
    }

    @Test // #11628
    void replaceState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained_react() {
        Mockito.when(configuration.isReactEnabled()).thenReturn(true);
        history.replaceState(null, "context/view?foo=bar#foobar");

        assertEquals("navigateReplacing", page.call.methodName(),
                "replace state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view?foo=bar#foobar",
                page.call.arguments().get(1), "invalid location");

        history.replaceState(null, "context/view/?foo=bar#foobar");

        assertEquals("navigateReplacing", page.call.methodName(),
                "replace state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals("context/view/?foo=bar#foobar",
                page.call.arguments().get(1), "invalid location");
    }

    @Test // #11628
    void replaceState_locationEmpty_pushesPeriod_react() {
        Mockito.when(configuration.isReactEnabled()).thenReturn(true);
        history.replaceState(null, "");
        assertEquals("navigateReplacing", page.call.methodName(),
                "replace state not scheduled");
        assertEquals(null, page.call.arguments().get(0));
        assertEquals(".", page.call.arguments().get(1),
                "location should be '.'");
    }
}
