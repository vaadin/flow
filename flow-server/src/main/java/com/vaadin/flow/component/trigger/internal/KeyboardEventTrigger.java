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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;

/**
 * Fires on a DOM keyboard event ({@code keydown}, {@code keyup}). Exposes the
 * {@code KeyboardEvent} properties as static {@link Action.Input} fields on
 * {@link EventData}, and supports filtering by key via {@link #forKeys(Key...)}
 * so only the configured keys produce a server-side fire.
 * <p>
 * The single-argument constructor defaults to {@code keydown}, the standard
 * event for keyboard shortcuts. A typical shortcut wiring looks like:
 *
 * <pre>{@code
 * new KeyboardEventTrigger(ui).forKeys(Key.ENTER, Key.ESCAPE).triggers(action);
 * }</pre>
 *
 * <p>
 * Without a filter, the trigger fires on every keyboard event of the configured
 * name. With one or more {@link #forKeys(Key...)} calls, the client compares
 * {@code event.key} against the configured set and only invokes the action on a
 * match — unmatched events do not cross the network.
 * <p>
 * The modifier-key fields ({@code shiftKey}, {@code ctrlKey}, {@code altKey},
 * {@code metaKey}) are intentionally duplicated from
 * {@link MouseEventTrigger.EventData}: although the underlying JS property is
 * the same, each field is class-scoped so a single instance can't be safely
 * shared across unrelated event families. Subclasses bound to a specific event
 * name may extend {@link EventData} to add event-specific properties.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class KeyboardEventTrigger extends DomEventTrigger {

    private final List<Key> keyFilter = new ArrayList<>();

    /**
     * Creates a keyboard-event trigger that fires on {@code keydown}. Use this
     * constructor for shortcut wirings; {@code keydown} is the standard event
     * for that purpose (it fires immediately on press, repeats while held, and
     * is dispatched before the browser's default action).
     *
     * @param host
     *            the component whose root element listens for the event, not
     *            {@code null}
     */
    public KeyboardEventTrigger(Component host) {
        this(host, "keydown");
    }

    /**
     * Creates a keyboard-event trigger that fires when the host receives a DOM
     * event with the given name.
     *
     * @param host
     *            the component whose root element listens for the event, not
     *            {@code null}
     * @param eventName
     *            the DOM keyboard-event name (e.g. {@code "keydown"},
     *            {@code "keyup"}), not {@code null}
     */
    public KeyboardEventTrigger(Component host, String eventName) {
        super(host, eventName);
    }

    /**
     * Restricts the trigger to fire only when {@code event.key} matches one of
     * the given keys. Multiple calls accumulate; with no call the trigger fires
     * on every keyboard event of the configured name.
     * <p>
     * Matching is done client-side against every printable representation in
     * {@link Key#getKeys()}, so a key like {@link Key#SPACE} (whose
     * {@code event.key} value is {@code " "}) matches the spacebar press.
     * Events that don't match are dropped in the browser handler — they do not
     * reach the server.
     *
     * @param keys
     *            the keys to allow; not {@code null}, none of the entries
     *            {@code null}
     * @return this trigger, for chaining
     */
    public KeyboardEventTrigger forKeys(Key... keys) {
        Objects.requireNonNull(keys, "keys");
        for (Key key : keys) {
            keyFilter.add(Objects.requireNonNull(key, "key"));
        }
        return this;
    }

    @Override
    public KeyboardEventTrigger preventDefault() {
        super.preventDefault();
        return this;
    }

    @Override
    public KeyboardEventTrigger stopPropagation() {
        super.stopPropagation();
        return this;
    }

    @Override
    protected void appendHandlerBody(StringBuilder body,
            List<Object> extraCaptures) {
        // Filter guard runs before preventDefault/stopPropagation — calling
        // either on an event the user didn't want to handle would be wrong.
        if (!keyFilter.isEmpty()) {
            List<String> allowed = keyFilter.stream()
                    .flatMap(k -> k.getKeys().stream()).distinct().toList();
            int captureIndex = 2 + extraCaptures.size();
            extraCaptures.add(allowed);
            body.append("if(!$").append(captureIndex)
                    .append(".includes(e.key))return;");
        }
        super.appendHandlerBody(body, extraCaptures);
    }

    /**
     * The {@code KeyboardEvent} properties exposed as static
     * {@link Action.Input} sources. Use these as the value source of an
     * {@link Action} wired to any {@link KeyboardEventTrigger} (or subclass).
     * <p>
     * Each field is bound to {@link KeyboardEventTrigger}; using it in the
     * handler of an unrelated trigger throws {@link IllegalArgumentException}
     * at {@link Trigger#triggers(Action...)} time.
     */
    public abstract static class EventData implements Serializable {

        /**
         * The class exists purely as a namespace for the static
         * {@link Action.Input} fields.
         */
        protected EventData() {
        }

        /**
         * {@code event.key} — the value of the key pressed, accounting for the
         * keyboard layout and modifiers (e.g. {@code "a"}, {@code "A"},
         * {@code "ArrowUp"}, {@code "Enter"}).
         */
        public static final Action.Input<String> key = eventProperty("key",
                KeyboardEventTrigger.class);

        /**
         * {@code event.code} — the physical key on the keyboard, independent of
         * layout or modifiers (e.g. {@code "KeyA"}, {@code "ArrowUp"},
         * {@code "Enter"}).
         */
        public static final Action.Input<String> code = eventProperty("code",
                KeyboardEventTrigger.class);

        /**
         * {@code event.repeat} — {@code true} when the event is fired by the OS
         * auto-repeat while the key is held down.
         */
        public static final Action.Input<Boolean> repeat = eventProperty(
                "repeat", KeyboardEventTrigger.class);

        /**
         * {@code event.isComposing} — {@code true} while the event is part of
         * an IME composition session (between {@code compositionstart} and
         * {@code compositionend}).
         */
        public static final Action.Input<Boolean> isComposing = eventProperty(
                "isComposing", KeyboardEventTrigger.class);

        /** {@code event.shiftKey} — whether shift was held during the event. */
        public static final Action.Input<Boolean> shiftKey = eventProperty(
                "shiftKey", KeyboardEventTrigger.class);

        /** {@code event.ctrlKey} — whether ctrl was held during the event. */
        public static final Action.Input<Boolean> ctrlKey = eventProperty(
                "ctrlKey", KeyboardEventTrigger.class);

        /** {@code event.altKey} — whether alt was held during the event. */
        public static final Action.Input<Boolean> altKey = eventProperty(
                "altKey", KeyboardEventTrigger.class);

        /** {@code event.metaKey} — whether meta was held during the event. */
        public static final Action.Input<Boolean> metaKey = eventProperty(
                "metaKey", KeyboardEventTrigger.class);
    }
}
