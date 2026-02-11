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
package com.vaadin.flow.component;

import org.junit.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for Html.bindHtmlContent(Signal<String>).
 */
public class HtmlBindHtmlContentTest extends SignalsUnitTest {

    @Test
    public void bindHtmlContent_componentAttachedBefore_bindingActive() {
        Html html = new Html("<div id='a'>init</div>");
        // attach before bind
        UI.getCurrent().add(html);

        ValueSignal<String> signal = new ValueSignal<>(
                "<div id='b'>after</div>");
        html.bindHtmlContent(signal);

        assertEquals("after", html.getInnerHtml());
        assertEquals("b", html.getElement().getAttribute("id"));
    }

    @Test
    public void bindHtmlContent_componentAttachedAfter_bindingActive() {
        Html html = new Html("<div id='a'>init</div>");
        ValueSignal<String> signal = new ValueSignal<>(
                "<div id='b'>after</div>");
        html.bindHtmlContent(signal);

        // attach after bind
        UI.getCurrent().add(html);

        assertEquals("after", html.getInnerHtml());
        assertEquals("b", html.getElement().getAttribute("id"));
    }

    @Test
    public void bindHtmlContent_componentAttached_bindingActive_updatesOnChange() {
        Html html = new Html("<div id='a'>init</div>");
        UI.getCurrent().add(html);

        ValueSignal<String> signal = new ValueSignal<>("<div id='b'>v1</div>");
        html.bindHtmlContent(signal);

        assertEquals("v1", html.getInnerHtml());
        assertEquals("b", html.getElement().getAttribute("id"));

        // update value while attached
        signal.value("<div id='c'>v2</div>");
        assertEquals("v2", html.getInnerHtml());
        assertEquals("c", html.getElement().getAttribute("id"));
    }

    @Test
    public void bindHtmlContent_componentNotAttached_bindingInactive() {
        Html html = new Html("<div id='a'>init</div>");
        ValueSignal<String> signal = new ValueSignal<>(
                "<div id='b'>after</div>");
        html.bindHtmlContent(signal);

        // change ignored while not attached
        signal.value("<div id='c'>ignored</div>");

        assertEquals("init", html.getInnerHtml());
        assertEquals("a", html.getElement().getAttribute("id"));
    }

    @Test
    public void bindHtmlContent_componentDetached_bindingInactive() {
        Html html = new Html("<div id='a'>init</div>");
        UI.getCurrent().add(html);
        ValueSignal<String> signal = new ValueSignal<>(
                "<div id='b'>after</div>");
        html.bindHtmlContent(signal);
        // detach
        html.getElement().removeFromParent();

        // change ignored while detached
        signal.value("<div id='c'>ignored</div>");

        assertEquals("after", html.getInnerHtml());
        assertEquals("b", html.getElement().getAttribute("id"));
    }

    @Test
    public void bindHtmlContent_componentReAttached_bindingActivate() {
        Html html = new Html("<div id='a'>init</div>");
        UI.getCurrent().add(html);
        ValueSignal<String> signal = new ValueSignal<>(
                "<div id='b'>after</div>");
        html.bindHtmlContent(signal);
        // detach
        html.getElement().removeFromParent();

        // change while detached
        signal.value("<div id='c'>after2</div>");
        // re-attach
        UI.getCurrent().add(html);

        assertEquals("after2", html.getInnerHtml());
        assertEquals("c", html.getElement().getAttribute("id"));
    }

    @Test
    public void bindHtmlContent_withNullValue_recordsErrorAndDoesNotChange() {
        Html html = new Html("<div id='a'>init</div>");
        UI.getCurrent().add(html);
        ValueSignal<String> signal = new ValueSignal<>(
                "<div id='b'>after</div>");
        html.bindHtmlContent(signal);

        // sending null will cause NPE in setHtmlContent inside effect function;
        // state should not change, and an error should be captured by
        // SignalsUnitTest error handler
        signal.value(null);

        assertEquals("after", html.getInnerHtml());
        assertEquals("b", html.getElement().getAttribute("id"));
        // one error captured
        assertEquals(1, events.size());
        assertEquals(NullPointerException.class,
                events.getFirst().getThrowable().getClass());
        // clear events for next verification in SignalsUnitTest.after
        events.clear();
    }

    @Test
    public void bindHtmlContent_nullSignal_throwsNPE() {
        Html html = new Html("<div id='a'>init</div>");
        UI.getCurrent().add(html);

        assertThrows(NullPointerException.class,
                () -> html.bindHtmlContent(null));
    }

    @Test
    public void bindHtmlContent_setterAndRebindWhileActive_throwException() {
        Html html = new Html("<div id='a'>init</div>");
        UI.getCurrent().add(html);
        ValueSignal<String> signal = new ValueSignal<>(
                "<div id='b'>after</div>");
        html.bindHtmlContent(signal);

        assertThrows(BindingActiveException.class,
                () -> html.setHtmlContent("<div id='c'>manual</div>"));
        assertThrows(BindingActiveException.class,
                () -> html.bindHtmlContent(new ValueSignal<>("<div>x</div>")));
        // state unchanged
        assertEquals("after", html.getInnerHtml());
        assertEquals("b", html.getElement().getAttribute("id"));
    }
}
