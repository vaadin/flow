/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.page;

import java.io.Serializable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.UI;

import elemental.json.Json;
import elemental.json.JsonString;

public class HistoryTest {

    private class TestUI extends UI {
        @Override
        public Page getPage() {
            return page;
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

    private UI ui = new TestUI();
    private TestPage page = new TestPage(ui);
    private History history;

    @Before
    public void setup() {
        history = new History(ui);
    }

    @Test
    public void pushState_locationWithQueryParameters_queryParametersRetained() {
        history.pushState(Json.create("{foo:bar;}"), "context/view?param=4");

        Assert.assertEquals("push state JS not included",
                "setTimeout(() => window.history.pushState($0, '', $1))",
                page.expression);
        Assert.assertEquals("push state not included", "{foo:bar;}",
                ((JsonString) page.parameters[0]).getString());
        Assert.assertEquals("invalid location", "context/view?param=4",
                page.parameters[1]);

        history.pushState(Json.create("{foo:bar;}"), "context/view/?param=4");

        Assert.assertEquals("push state JS not included",
                "setTimeout(() => window.history.pushState($0, '', $1))",
                page.expression);
        Assert.assertEquals("push state not included", "{foo:bar;}",
                ((JsonString) page.parameters[0]).getString());
        Assert.assertEquals("invalid location", "context/view/?param=4",
                page.parameters[1]);
    }

    @Test
    public void pushState_locationWithFragment_fragmentRetained() {
        history.pushState(null, "context/view#foobar");

        Assert.assertEquals("push state JS not included",
                "setTimeout(() => window.history.pushState($0, '', $1))",
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("fragment not retained", "context/view#foobar",
                page.parameters[1]);

        history.pushState(null, "context/view/#foobar");

        Assert.assertEquals("push state JS not included",
                "setTimeout(() => window.history.pushState($0, '', $1))",
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("fragment not retained", "context/view/#foobar",
                page.parameters[1]);
    }

    @Test // #11628
    public void pushState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained() {
        history.pushState(null, "context/view?foo=bar#foobar");

        Assert.assertEquals("push state JS not included",
                "setTimeout(() => window.history.pushState($0, '', $1))",
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view?foo=bar#foobar",
                page.parameters[1]);

        history.pushState(null, "context/view/?foo=bar#foobar");

        Assert.assertEquals("push state JS not included",
                "setTimeout(() => window.history.pushState($0, '', $1))",
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view/?foo=bar#foobar",
                page.parameters[1]);
    }

    @Test // #11628
    public void replaceState_locationWithQueryParametersAndFragment_QueryParametersAndFragmentRetained() {
        history.replaceState(null, "context/view?foo=bar#foobar");

        Assert.assertEquals("push state JS not included",
                "setTimeout(() => window.history.replaceState($0, '', $1))",
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view?foo=bar#foobar",
                page.parameters[1]);

        history.replaceState(null, "context/view/?foo=bar#foobar");

        Assert.assertEquals("push state JS not included",
                "setTimeout(() => window.history.replaceState($0, '', $1))",
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("invalid location", "context/view/?foo=bar#foobar",
                page.parameters[1]);
    }

    @Test // #11628
    public void replaceState_locationEmpty_pushesPeriod() {
        history.replaceState(null, "");
        Assert.assertEquals("push state JS not included",
                "setTimeout(() => window.history.replaceState($0, '', $1))",
                page.expression);
        Assert.assertEquals(null, page.parameters[0]);
        Assert.assertEquals("location should be '.'", ".", page.parameters[1]);
    }

}
