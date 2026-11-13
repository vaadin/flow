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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction.WriteToClipboardJs;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.js.JsCall;

/**
 * Does on the server what the browser does with the JavaScript of a trigger, so
 * that a test framework without a browser can run triggers when it simulates a
 * user.
 * <p>
 * A trigger reaches the browser as an element initializer whose function is
 * composed of the snippets of {@link TriggerJs}, {@link TriggerInputJs} and the
 * actions. {@link #readEventListener(JsFunction)} reads such an initializer,
 * and {@link EventListener#handleEvent(Browser)} runs it the way the browser
 * runs it when the event fires. What the browser itself provides, such as the
 * clipboard, comes from a {@link Browser} that the test framework implements.
 * <p>
 * Snippets that are not simulated throw {@link NotSimulatedException}. Each
 * snippet that is simulated here has to keep doing what its JavaScript does.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class TriggerSimulator {

    private TriggerSimulator() {
    }

    /**
     * What the simulated browser provides to the trigger JavaScript.
     */
    public interface Browser extends Serializable {

        /**
         * Replaces the content of the clipboard, as
         * {@code navigator.clipboard.write} does.
         *
         * @param text
         *            the plain text, or {@code null} for none
         * @param html
         *            the HTML, or {@code null} for none
         * @throws BrowserRejectedException
         *             if the browser refuses the write
         */
        void writeToClipboard(@Nullable String text, @Nullable String html);
    }

    /**
     * Thrown by a {@link Browser} to reject the promise of the call, as a
     * browser does when it refuses an operation.
     */
    public static final class BrowserRejectedException
            extends RuntimeException {

        private final String name;

        /**
         * Creates an exception for a rejection with the given DOM exception
         * name and message.
         *
         * @param name
         *            the name of the error, such as {@code NotAllowedError}
         * @param message
         *            the message of the error
         */
        public BrowserRejectedException(String name, String message) {
            super(message);
            this.name = name;
        }

        /**
         * Gets the name of the error, such as {@code NotAllowedError}.
         *
         * @return the name of the error
         */
        public String getName() {
            return name;
        }
    }

    /**
     * Thrown when a trigger uses JavaScript that is not simulated.
     */
    public static final class NotSimulatedException extends RuntimeException {

        private NotSimulatedException(String message) {
            super(message);
        }
    }

    /**
     * A listener that a trigger installs for a DOM event.
     *
     * @param eventName
     *            the name of the DOM event, such as {@code click}
     * @param action
     *            the function the listener calls
     */
    public record EventListener(String eventName,
            JsFunction action) implements Serializable {

        /**
         * Runs the listener as the browser does when the event fires.
         * <p>
         * The browser settles the promises that the listener starts only after
         * the event has been handled, so the outcomes the listener reports to
         * the server are delivered by the returned task. Run it once the
         * server-side event of the same user action has been fired.
         *
         * @param browser
         *            what the browser provides, not {@code null}
         * @return the task that delivers the outcomes of the promises the
         *         listener started, not {@code null}
         * @throws NotSimulatedException
         *             if the listener uses JavaScript that is not simulated;
         *             nothing has been done in that case
         */
        public Runnable handleEvent(Browser browser) {
            List<Runnable> settlements = new ArrayList<>();
            evaluate(action, browser, settlements);
            return () -> settlements.forEach(Runnable::run);
        }
    }

    /**
     * Reads the DOM event listener that a trigger installs with the given
     * element initializer.
     *
     * @param initializer
     *            the function of the element initializer, not {@code null}
     * @return the listener, not {@code null}
     * @throws NotSimulatedException
     *             if the initializer does not install a DOM event listener,
     *             such as a timeout or size trigger, or is not declared as a
     *             snippet
     */
    public static EventListener readEventListener(JsFunction initializer) {
        JsCall call = declaredCallOf(initializer);
        if (call.definitionType() == TriggerJs.class
                && "listenToEvent".equals(call.methodName())) {
            return new EventListener((String) call.arguments().get(1),
                    (JsFunction) call.arguments().get(0));
        }
        throw notSimulated(call);
    }

    /**
     * The result of a function that returns a promise, once the promise has
     * settled.
     */
    private record Settled(boolean ok, @Nullable Object value,
            @Nullable BrowserRejectedException error) implements Serializable {
    }

    private static @Nullable Object evaluate(JsFunction function,
            Browser browser, List<Runnable> settlements) {
        JsCall call = declaredCallOf(function);
        List<Object> args = call.arguments();
        Class<?> type = call.definitionType();
        String method = call.methodName();
        if (type == TriggerInputJs.class) {
            switch (method) {
            case "returnValue":
                return args.get(0);
            case "returnNull":
                return null;
            case "readProperty":
                return ((Element) args.get(0))
                        .getPropertyRaw((String) args.get(1));
            default:
                throw notSimulated(call);
            }
        }
        if (type == WriteToClipboardJs.class
                && "writeToClipboard".equals(method)) {
            // All inputs are read before the write, as the arguments of
            // writePayload are, so a failing input writes nothing
            String text = asText(
                    evaluate((JsFunction) args.get(0), browser, settlements));
            String html = asText(
                    evaluate((JsFunction) args.get(1), browser, settlements));
            if (evaluate((JsFunction) args.get(2), browser,
                    settlements) != null) {
                throw new NotSimulatedException(
                        "Copying an image is not simulated");
            }
            try {
                browser.writeToClipboard(text, html);
            } catch (BrowserRejectedException e) {
                return new Settled(false, null, e);
            }
            return new Settled(true, text != null ? text : html, null);
        }
        if (type == TriggerJs.class && "reportOutcome".equals(method)) {
            Object result = evaluate((JsFunction) args.get(0), browser,
                    settlements);
            Settled settled = result instanceof Settled s ? s
                    : new Settled(true, result, null);
            ReturnChannelRegistration channel = (ReturnChannelRegistration) args
                    .get(1);
            settlements.add(() -> channel.invoke(encodeOutcome(settled)));
            return null;
        }
        throw notSimulated(call);
    }

    private static @Nullable String asText(@Nullable Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        }
        throw new NotSimulatedException(
                "Copying a value that is not a string is not simulated: "
                        + value.getClass().getName());
    }

    // The arguments that the reportOutcome snippet sends to the return channel
    private static ArrayNode encodeOutcome(Settled settled) {
        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", settled.ok());
        if (settled.ok()) {
            Object value = settled.value();
            outcome.set("value", value == null ? JacksonUtils.nullNode()
                    : JacksonUtils.writeValue(value));
        } else {
            BrowserRejectedException error = Objects
                    .requireNonNull(settled.error());
            ObjectNode errorNode = JacksonUtils.createObjectNode();
            errorNode.put("name", error.getName());
            errorNode.put("message", error.getMessage());
            outcome.set("error", errorNode);
        }
        ArrayNode arguments = JacksonUtils.createArrayNode();
        arguments.add(outcome);
        return arguments;
    }

    private static JsCall declaredCallOf(JsFunction function) {
        JsCall call = function.getDeclaredCall();
        if (call == null) {
            throw new NotSimulatedException(
                    "JavaScript compiled from a string is not simulated");
        }
        return call;
    }

    private static NotSimulatedException notSimulated(JsCall call) {
        return new NotSimulatedException(call.definitionType().getSimpleName()
                + "." + call.methodName() + " is not simulated");
    }
}
