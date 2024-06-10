/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import java.io.Serializable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinSession;

import elemental.json.Json;
import elemental.json.JsonString;

public class HistoryTest {

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

        private String expression;

        private Serializable[] parameters;

        public TestPage(UI ui) {
            super(ui);
        }

        @Override
        public PendingJavaScriptResult executeJs(String expression,
                Serializable... parameters) {
            this.expression = expression;
            this.parameters = parameters;
            return null;
        }
    }

    private TestUI ui = new TestUI();
    private TestPage page = new TestPage(ui);
    private History history;

    private VaadinSession session = Mockito.mock(VaadinSession.class);
    private DeploymentConfiguration configuration;

    private static final String PUSH_STATE_JS = "setTimeout(() => { window.history.pushState($0, '', $1); window.dispatchEvent(new CustomEvent('vaadin-navigated')); })";
    private static final String REPLACE_STATE_JS = "setTimeout(() => { window.history.replaceState($0, '', $1); window.dispatchEvent(new CustomEvent('vaadin-navigated')); })";

    private static final String PUSH_STATE_REACT = "window.dispatchEvent(new CustomEvent('vaadin-navigate', { detail: { state: $0, url: $1, replace: false } }));";
    private static final String REPLACE_STATE_REACT = "window.dispatchEvent(new CustomEvent('vaadin-navigate', { detail: { state: $0, url: $1, replace: true } }));";

    @Before
    public void setup() {
        history = new History(ui);
        configuration = Mockito.mock(DeploymentConfiguration.class);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.isReactEnabled()).thenReturn(false);
    }

    @Test
    public void pushState_locationWithQueryParameters_queryParametersRetained() {
        history.pushState(Json.create("{foo:bar;}"), "context/view?param=4");

        Assert.assertEquals("push state JS not included", PUSH_STATE_JS,
                page.expression);
        Assert.assertEquals("push state not included", "{foo:bar;}",
                ((JsonString) page.parameters[0]).getString());
        Assert.assertEquals("invalid location", "context/view?param=4",
                page.parameters[1]);

        history.pushState(Json.create("{foo:bar;}"), "context/view/?param=4");

        Assert.assertEquals("push state JS not included", PUSH_STATE_JS,
                page.expression);
        Assert.assertEquals("push state not included", "{foo:bar;}",
                ((JsonString) page.parameters[0]).getString());
        Assert.assertEquals("invalid location", "context/view/?param=4",
                page.parameters[1]);
    }

    @Test
    public void pushState_locationWithFragment_fragmentRetained() {
        history.pushState(null, "context/view#foobar");

        Assert.assertEquals("push state JS not included", PUSH_STATE_JS,
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("fragment not retained", "context/view#foobar",
                page.parameters[1]);

        history.pushState(null, "context/view/#foobar");

        Assert.assertEquals("push state JS not included", PUSH_STATE_JS,
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("fragment not retained", "context/view/#foobar",
                page.parameters[1]);
    }

    @Test // #11628
    public void pushState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained() {
        history.pushState(null, "context/view?foo=bar#foobar");

        Assert.assertEquals("push state JS not included", PUSH_STATE_JS,
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view?foo=bar#foobar",
                page.parameters[1]);

        history.pushState(null, "context/view/?foo=bar#foobar");

        Assert.assertEquals("push state JS not included", PUSH_STATE_JS,
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view/?foo=bar#foobar",
                page.parameters[1]);
    }

    @Test // #11628
    public void replaceState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained() {
        history.replaceState(null, "context/view?foo=bar#foobar");

        Assert.assertEquals("replace state JS not included", REPLACE_STATE_JS,
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view?foo=bar#foobar",
                page.parameters[1]);

        history.replaceState(null, "context/view/?foo=bar#foobar");

        Assert.assertEquals("replace state JS not included", REPLACE_STATE_JS,
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view/?foo=bar#foobar",
                page.parameters[1]);
    }

    @Test // #11628
    public void replaceState_locationEmpty_pushesPeriod() {
        history.replaceState(null, "");
        Assert.assertEquals("replace state JS not included", REPLACE_STATE_JS,
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("location should be '.'", ".", page.parameters[1]);
    }


    @Test
    public void pushState_locationWithQueryParameters_queryParametersRetained_react() {
        Mockito.when(configuration.isReactEnabled()).thenReturn(true);
        history.pushState(Json.create("{foo:bar;}"), "context/view?param=4");

        Assert.assertEquals("push state JS not included", PUSH_STATE_REACT,
                page.expression);
        Assert.assertEquals("push state not included", "{foo:bar;}",
                ((JsonString) page.parameters[0]).getString());
        Assert.assertEquals("invalid location", "context/view?param=4",
                page.parameters[1]);

        history.pushState(Json.create("{foo:bar;}"), "context/view/?param=4");

        Assert.assertEquals("push state JS not included", PUSH_STATE_REACT,
                page.expression);
        Assert.assertEquals("push state not included", "{foo:bar;}",
                ((JsonString) page.parameters[0]).getString());
        Assert.assertEquals("invalid location", "context/view/?param=4",
                page.parameters[1]);
    }

    @Test
    public void pushState_locationWithFragment_fragmentRetained_react() {
        Mockito.when(configuration.isReactEnabled()).thenReturn(true);
        history.pushState(null, "context/view#foobar");

        Assert.assertEquals("push state JS not included", PUSH_STATE_REACT,
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("fragment not retained", "context/view#foobar",
                page.parameters[1]);

        history.pushState(null, "context/view/#foobar");

        Assert.assertEquals("push state JS not included", PUSH_STATE_REACT,
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("fragment not retained", "context/view/#foobar",
                page.parameters[1]);
    }

    @Test // #11628
    public void pushState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained_react() {
        Mockito.when(configuration.isReactEnabled()).thenReturn(true);
        history.pushState(null, "context/view?foo=bar#foobar");

        Assert.assertEquals("push state JS not included", PUSH_STATE_REACT,
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view?foo=bar#foobar",
                page.parameters[1]);

        history.pushState(null, "context/view/?foo=bar#foobar");

        Assert.assertEquals("push state JS not included", PUSH_STATE_REACT,
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view/?foo=bar#foobar",
                page.parameters[1]);
    }

    @Test // #11628
    public void replaceState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained_react() {
        Mockito.when(configuration.isReactEnabled()).thenReturn(true);
        history.replaceState(null, "context/view?foo=bar#foobar");

        Assert.assertEquals("replace state JS not included",
                REPLACE_STATE_REACT, page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view?foo=bar#foobar",
                page.parameters[1]);

        history.replaceState(null, "context/view/?foo=bar#foobar");

        Assert.assertEquals("replace state JS not included",
                REPLACE_STATE_REACT, page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view/?foo=bar#foobar",
                page.parameters[1]);
    }

    @Test // #11628
    public void replaceState_locationEmpty_pushesPeriod_react() {
        Mockito.when(configuration.isReactEnabled()).thenReturn(true);
        history.replaceState(null, "");
        Assert.assertEquals("replace state JS not included",
                REPLACE_STATE_REACT, page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("location should be '.'", ".", page.parameters[1]);
    }
}
