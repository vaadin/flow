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

import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for Html.bindHtmlContent(Signal<String>).
 */
class HtmlBindHtmlContentTest extends SignalsUnitTest {

    @Test
    void bindHtmlContent_componentAttachedBefore_bindingActive() {
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
    void bindHtmlContent_componentAttachedAfter_bindingActive() {
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
    void bindHtmlContent_componentAttached_bindingActive_updatesOnChange() {
        Html html = new Html("<div id='a'>init</div>");
        UI.getCurrent().add(html);

        ValueSignal<String> signal = new ValueSignal<>("<div id='b'>v1</div>");
        html.bindHtmlContent(signal);

        assertEquals("v1", html.getInnerHtml());
        assertEquals("b", html.getElement().getAttribute("id"));

        // update value while attached
        signal.set("<div id='c'>v2</div>");
        assertEquals("v2", html.getInnerHtml());
        assertEquals("c", html.getElement().getAttribute("id"));
    }

    @Test
    void bindHtmlContent_componentNotAttached_bindingInactive() {
        Html html = new Html("<div id='a'>init</div>");
        ValueSignal<String> signal = new ValueSignal<>(
                "<div id='b'>after</div>");
        html.bindHtmlContent(signal);

        // Probe runs immediately at bind time, applying the signal value
        assertEquals("after", html.getInnerHtml());
        assertEquals("b", html.getElement().getAttribute("id"));

        // Changes while detached are ignored (effect is passivated)
        signal.set("<div id='c'>ignored</div>");
        assertEquals("after", html.getInnerHtml());
        assertEquals("b", html.getElement().getAttribute("id"));
    }

    @Test
    void bindHtmlContent_componentDetached_bindingInactive() {
        Html html = new Html("<div id='a'>init</div>");
        UI.getCurrent().add(html);
        ValueSignal<String> signal = new ValueSignal<>(
                "<div id='b'>after</div>");
        html.bindHtmlContent(signal);
        // detach
        html.getElement().removeFromParent();

        // change ignored while detached
        signal.set("<div id='c'>ignored</div>");

        assertEquals("after", html.getInnerHtml());
        assertEquals("b", html.getElement().getAttribute("id"));
    }

    @Test
    void bindHtmlContent_componentReAttached_bindingActivate() {
        Html html = new Html("<div id='a'>init</div>");
        UI.getCurrent().add(html);
        ValueSignal<String> signal = new ValueSignal<>(
                "<div id='b'>after</div>");
        html.bindHtmlContent(signal);
        // detach
        html.getElement().removeFromParent();

        // change while detached
        signal.set("<div id='c'>after2</div>");
        // re-attach
        UI.getCurrent().add(html);

        assertEquals("after2", html.getInnerHtml());
        assertEquals("c", html.getElement().getAttribute("id"));
    }

    @Test
    void bindHtmlContent_withNullValue_recordsErrorAndDoesNotChange() {
        Html html = new Html("<div id='a'>init</div>");
        UI.getCurrent().add(html);
        ValueSignal<String> signal = new ValueSignal<>(
                "<div id='b'>after</div>");
        html.bindHtmlContent(signal);

        // sending null is rejected with an IllegalArgumentException inside the
        // effect function; state should not change, and an error should be
        // captured by the SignalsUnitTest error handler
        signal.set(null);

        assertEquals("after", html.getInnerHtml());
        assertEquals("b", html.getElement().getAttribute("id"));
        // one error captured
        assertEquals(1, events.size());
        assertEquals(IllegalArgumentException.class,
                events.getFirst().getThrowable().getClass());
        // clear events for next verification in SignalsUnitTest.after
        events.clear();
    }

    @Test
    void bindHtmlContent_nullSignal_throwsNPE() {
        Html html = new Html("<div id='a'>init</div>");
        UI.getCurrent().add(html);

        assertThrows(NullPointerException.class,
                () -> html.bindHtmlContent(null));
    }

    @Test
    void bindHtmlContent_setterAndRebindWhileActive_throwException() {
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

    @Test
    void signalConstructorWithSafelist_initialAndUpdatesSanitized() {
        ValueSignal<String> signal = new ValueSignal<>(
                "<div onclick='evil()'>v1<script>x()</script></div>");
        Html html = new Html(signal, () -> new Safelist().addTags("div"));
        UI.getCurrent().add(html);

        // initial value sanitized
        assertEquals("div", html.getElement().getTag());
        assertNull(html.getElement().getAttribute("onclick"));
        assertEquals("v1", html.getInnerHtml());

        // updates are sanitized too
        signal.set("<div onmouseover='evil()'>v2<script>y()</script></div>");
        assertNull(html.getElement().getAttribute("onmouseover"));
        assertEquals("v2", html.getInnerHtml());
    }

    @Test
    void signalConstructorWithSafelist_nullSupplier_throws() {
        assertThrows(NullPointerException.class,
                () -> new Html(new ValueSignal<>("<div>x</div>"),
                        (SerializableSupplier<Safelist>) null));
    }

    @Test
    void bindWithSafelist_withNullValue_recordsErrorAndDoesNotChange() {
        ValueSignal<String> signal = new ValueSignal<>("<div>after</div>");
        Html html = new Html(signal, () -> new Safelist().addTags("div"));
        UI.getCurrent().add(html);

        // A null value is rejected with a clear IllegalArgumentException
        // instead of reaching Jsoup; state does not change.
        signal.set(null);

        assertEquals("after", html.getInnerHtml());
        assertEquals(1, events.size());
        assertEquals(IllegalArgumentException.class,
                events.getFirst().getThrowable().getClass());
        events.clear();
    }

    @Test
    void bindWithSafelist_valueBecomesInvalidAfterClean_recordsErrorAndDoesNotChange() {
        ValueSignal<String> signal = new ValueSignal<>("<div>ok</div>");
        Html html = new Html(signal, () -> new Safelist().addTags("div"));
        UI.getCurrent().add(html);

        assertEquals("ok", html.getInnerHtml());

        // The safelist strips <script> entirely, so the cleaned value has no
        // top-level element. Applying it throws inside the effect; the error is
        // captured and the previous content is kept.
        signal.set("<script>evil()</script>");

        assertEquals("ok", html.getInnerHtml());
        assertEquals(1, events.size());
        assertEquals(IllegalArgumentException.class,
                events.getFirst().getThrowable().getClass());
        events.clear();
    }
}
