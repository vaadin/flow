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
package com.vaadin.flow.component.trigger.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.trigger.internal.TriggerSimulator.BrowserRejectedException;
import com.vaadin.flow.component.trigger.internal.TriggerSimulator.EventListener;
import com.vaadin.flow.component.trigger.internal.TriggerSimulator.NotSimulatedException;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerSimulatorTest {

    private final List<String> written = new ArrayList<>();
    private final TriggerSimulator.Browser browser = (text, html) -> written
            .add(text + "|" + html);

    private UI ui;
    private TagComponent button;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
        button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());
    }

    @Test
    void literalTextAndHtml_writtenToClipboardOnEvent() {
        new ClickTrigger(button).triggers(new WriteToClipboardAction(
                new LiteralInput<>("Hello"), new LiteralInput<>("<b>Hi</b>")));

        EventListener listener = readListener();
        assertEquals("click", listener.eventName());
        assertEquals(List.of(), written);

        listener.handleEvent(browser).run();

        assertEquals(List.of("Hello|<b>Hi</b>"), written);
    }

    @Test
    void propertyInput_writesCurrentPropertyValue() {
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(field.getElement());
        new ClickTrigger(button).triggers(new WriteToClipboardAction(
                new PropertyInput<>(field, "value", String.class), null));
        EventListener listener = readListener();

        field.getElement().setProperty("value", "Typed");
        listener.handleEvent(browser);

        assertEquals(List.of("Typed|null"), written);
    }

    @ParameterizedTest
    @CsvSource(value = { "Hello, NULL, copied Hello",
            "NULL, <b>Hi</b>, copied <b>Hi</b>" }, nullValues = "NULL")
    void outcome_deliveredBySettlementTask(@Nullable String text,
            @Nullable String html, String expected) {
        List<String> outcomes = new ArrayList<>();
        new ClickTrigger(button).triggers(new WriteToClipboardAction(
                text == null ? null : new LiteralInput<>(text),
                html == null ? null : new LiteralInput<>(html),
                copied -> outcomes.add("copied " + copied),
                error -> outcomes.add("error " + error)));

        Runnable settle = readListener().handleEvent(browser);
        assertEquals(List.of(), outcomes);

        settle.run();
        assertEquals(List.of(expected), outcomes);
    }

    @Test
    void rejectedWrite_deliveredAsError() {
        List<PromiseAction.@Nullable Error> errors = new ArrayList<>();
        new ClickTrigger(button).triggers(
                new WriteToClipboardAction(new LiteralInput<>("Hello"), null,
                        copied -> errors.add(null), errors::add));

        readListener().handleEvent((text, html) -> {
            throw new BrowserRejectedException("NotAllowedError", "Denied");
        }).run();

        assertEquals(
                List.of(new PromiseAction.Error("NotAllowedError", "Denied")),
                errors);
    }

    static Stream<Arguments> notSimulatedActions() {
        TriggerInputJs inputJs = JsSnippets.of(TriggerInputJs.class);
        return Stream.of(Arguments.of("image",
                (Function<TagComponent, Action>) other -> new WriteToClipboardAction(
                        new LiteralInput<>("Hello"), null,
                        new ImageBlobInput(other))),
                Arguments.of("not a string",
                        (Function<TagComponent, Action>) other -> {
                            @SuppressWarnings({ "unchecked", "rawtypes" })
                            Action.Input<String> number = (Action.Input) new LiteralInput<>(
                                    42);
                            return new WriteToClipboardAction(number, null);
                        }),
                Arguments.of("'value', which is not set on the server",
                        (Function<TagComponent, Action>) other -> new WriteToClipboardAction(
                                new PropertyInput<>(other, "value",
                                        String.class),
                                null)),
                Arguments.of("TriggerInputJs.returnEvent",
                        (Function<TagComponent, Action>) other -> new Action() {
                            @Override
                            protected JsFunction toJs(Trigger trigger) {
                                return inputJs.returnEvent();
                            }
                        }),
                Arguments.of("compiled from a string",
                        (Function<TagComponent, Action>) other -> new Action() {
                            @Override
                            protected JsFunction toJs(Trigger trigger) {
                                return JsFunction.of("return 1");
                            }
                        }));
    }

    @ParameterizedTest
    @MethodSource("notSimulatedActions")
    void actionNotSimulated_throwsAndWritesNothing(String reason,
            Function<TagComponent, Action> action) {
        TagComponent other = new TagComponent("img");
        ui.getElement().appendChild(other.getElement());
        new ClickTrigger(button).triggers(action.apply(other));
        EventListener listener = readListener();

        NotSimulatedException e = assertThrows(NotSimulatedException.class,
                () -> listener.handleEvent(browser));

        assertTrue(String.valueOf(e.getMessage()).contains(reason),
                e.getMessage());
        assertEquals(List.of(), written);
    }

    @Test
    void timeoutTrigger_notAnEventListener() {
        new TimeoutTrigger(button, Duration.ofMillis(100)).triggers(
                new WriteToClipboardAction(new LiteralInput<>("Hello"), null));
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        JsFunction initializer = singleInstallFn(ui);

        NotSimulatedException e = assertThrows(NotSimulatedException.class,
                () -> TriggerSimulator.readEventListener(initializer));

        assertTrue(String.valueOf(e.getMessage()).contains("runAfterTimeout"),
                e.getMessage());
    }

    private EventListener readListener() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return TriggerSimulator.readEventListener(singleInstallFn(ui));
    }
}
