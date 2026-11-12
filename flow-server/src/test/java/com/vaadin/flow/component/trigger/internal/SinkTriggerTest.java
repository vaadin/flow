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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.internal.ValueJs;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the client-action seam: an action bound to a sink instead of to a
 * component, so a renderer can fire it once per element it renders on the
 * client while the server holds a single binding.
 */
class SinkTriggerTest {

    private static class Div extends Component {
        Div() {
            super(new Element("div"));
        }
    }

    /**
     * Stands in for the renderer: keeps the rendered action functions so they
     * can be handed to the client-side template.
     */
    private static class RecordingSink implements ClientActionSink {

        private final List<JsFunction> installed = new ArrayList<>();

        @Override
        public Registration install(JsFunction action) {
            installed.add(action);
            return () -> installed.remove(action);
        }
    }

    @Test
    void write_copiesTheFirstArgumentTheTemplatePasses() {
        UI ui = new MockUI();
        Div container = new Div();
        ui.add(container);
        RecordingSink sink = new RecordingSink();

        // Template: <button @click=${() => copy(item.email)}>Copy</button>
        new SinkTrigger(container.getElement(), sink)
                .triggers(Clipboard.writeText());

        // One binding for the whole renderer, whatever the row count.
        assertEquals(1, sink.installed.size());
        JsFunction action = sink.installed.get(0);

        assertCopiesFirstTemplateArgument(action);
    }

    @Test
    void writeWithCallbacks_reportsEachOutcomeBackToTheServer() {
        UI ui = new MockUI();
        Div container = new Div();
        ui.add(container);
        RecordingSink sink = new RecordingSink();
        List<@Nullable String> copied = new ArrayList<>();
        List<PromiseAction.Error> failed = new ArrayList<>();

        new SinkTrigger(container.getElement(), sink)
                .triggers(Clipboard.writeText(copied::add, failed::add));

        // Still one binding: the outcome travels over one return channel on
        // the renderer's element, shared by every element that calls copy.
        assertEquals(1, sink.installed.size());
        JsFunction action = sink.installed.get(0);
        assertEquals("reportOutcome",
                TriggerTestUtil.snippetOf(TriggerJs.class, action));
        assertCalledWithContext(action);
        ReturnChannelRegistration channel = singleChannel(action);

        // The outcome wrapper calls the same write as the plain overload.
        assertCopiesFirstTemplateArgument(
                (JsFunction) action.getCaptures().get(0));

        ObjectNode ok = JacksonUtils.createObjectNode();
        ok.put("ok", true);
        ok.put("value", "ada@example.com");
        channel.invoke(outcome(ok));

        ObjectNode error = JacksonUtils.createObjectNode();
        error.put("name", "NotAllowedError");
        error.put("message", "Write permission denied.");
        ObjectNode rejected = JacksonUtils.createObjectNode();
        rejected.put("ok", false);
        rejected.set("error", error);
        channel.invoke(outcome(rejected));

        assertEquals(List.of("ada@example.com"), copied);
        assertEquals(1, failed.size());
        assertEquals("NotAllowedError", failed.get(0).name());
        assertEquals("Write permission denied.", failed.get(0).message());
    }

    /**
     * A view-like owner that keeps the action in a field and whose callback
     * reaches back to it, the usual shape in an application: the action and its
     * owner form a reference cycle.
     */
    private static class CopyOwner implements Serializable {
        private final List<@Nullable String> copied = new ArrayList<>();
        private final Action copy = Clipboard.writeText(this::copied, err -> {
        });

        private void copied(@Nullable String value) {
            copied.add(value);
        }
    }

    @Test
    void action_survivesSerialization_evenWhenItsCallbacksReachBackToIt()
            throws Exception {
        CopyOwner restored = serializeAndDeserialize(new CopyOwner());

        UI ui = new MockUI();
        Div container = new Div();
        ui.add(container);
        RecordingSink sink = new RecordingSink();
        new SinkTrigger(container.getElement(), sink).triggers(restored.copy);

        ObjectNode ok = JacksonUtils.createObjectNode();
        ok.put("ok", true);
        ok.put("value", "ada@example.com");
        singleChannel(sink.installed.get(0)).invoke(outcome(ok));

        // The restored callback still reaches the restored owner.
        assertEquals(List.of("ada@example.com"), restored.copied);
    }

    @Test
    void unarmedCheckPasses_andRemovingTheTriggerDetachesTheBinding() {
        UI ui = new MockUI();
        Div container = new Div();
        ui.add(container);
        RecordingSink sink = new RecordingSink();

        SinkTrigger trigger = new SinkTrigger(container.getElement(), sink);
        trigger.triggers(Clipboard.writeText());

        // The action is committed as part of binding, so the trigger is armed
        // and the before-response check must not throw.
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        trigger.remove();
        assertTrue(sink.installed.isEmpty(),
                "removing the trigger must detach the installed function");
    }

    @Test
    @SuppressWarnings("NullAway") // deliberately violates the sink contract
    void sinkReturningNullRegistration_isRejected() {
        UI ui = new MockUI();
        Div container = new Div();
        ui.add(container);

        Action copy = Clipboard.writeText();
        SinkTrigger trigger = new SinkTrigger(container.getElement(),
                action -> null);

        NullPointerException exception = assertThrows(
                NullPointerException.class, () -> trigger.triggers(copy));
        assertEquals("ClientActionSink.install must return a Registration",
                exception.getMessage());
    }

    @Test
    void composingSnippets_forwardTheContextToTheirInputs() {
        // An action reaches a context input only through the snippets that
        // compose it, so each snippet that calls an input has to take the
        // context and pass it on.
        for (Class<?> definition : List.of(TriggerJs.class,
                WriteToClipboardAction.WriteToClipboardJs.class,
                ShareAction.ShareJs.class, DownloadAction.DownloadJs.class,
                OpenInNewTabAction.OpenInNewTabJs.class)) {
            for (Method method : definition.getMethods()) {
                if (!List.of(method.getParameterTypes())
                        .contains(JsFunction.class)) {
                    continue;
                }
                String name = definition.getSimpleName() + "."
                        + method.getName();
                JsExpression expression = method
                        .getAnnotation(JsExpression.class);
                assertNotNull(expression, name);
                // The installers call the action with whatever their trigger
                // supplies; only the snippets that are themselves called with
                // event take part in the action's calling convention.
                List<String> calledWith = JsCall.getCalledWithNames(method);
                if (calledWith.isEmpty()) {
                    assertFalse(expression.value().contains("event"),
                            name + " uses event without being called with it");
                    continue;
                }
                assertEquals(List.of("event", "context"), calledWith, name);
                assertFalse(
                        Pattern.compile("\\$\\d+\\((event)?\\)")
                                .matcher(expression.value()).find(),
                        name + " calls an input without the context");
            }
        }
    }

    private static void assertCopiesFirstTemplateArgument(JsFunction write) {
        assertEquals("writeToClipboard", TriggerTestUtil.snippetOf(
                WriteToClipboardAction.WriteToClipboardJs.class, write));
        assertCalledWithContext(write);
        JsFunction text = (JsFunction) write.getCaptures().get(0);
        assertEquals("contextValue",
                TriggerTestUtil.snippetOf(TriggerJs.class, text));
        assertCalledWithContext(text);
        assertEquals(List.of("args", "0"), text.getCaptures());
        // Text only: the html slot stays empty.
        assertEquals("nullValue", TriggerTestUtil.snippetOf(ValueJs.class,
                (JsFunction) write.getCaptures().get(1)));
    }

    private static ReturnChannelRegistration singleChannel(JsFunction action) {
        List<ReturnChannelRegistration> channels = action.getCaptures().stream()
                .filter(ReturnChannelRegistration.class::isInstance)
                .map(ReturnChannelRegistration.class::cast).toList();
        assertEquals(1, channels.size());
        return channels.get(0);
    }

    private static void assertCalledWithContext(JsFunction function) {
        assertEquals(List.of("event", "context"),
                Objects.requireNonNull(function.getDeclaredCall())
                        .getCalledWithNames());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T serializeAndDeserialize(T object)
            throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    private static ArrayNode outcome(ObjectNode outcome) {
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        return args;
    }
}
