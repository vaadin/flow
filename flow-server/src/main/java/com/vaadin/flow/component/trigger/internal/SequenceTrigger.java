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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;

/**
 * Fires when a specific ordered sequence of keys is pressed on the host's root
 * element — e.g. the konami code or a "hello" Easter egg. State is tracked
 * client-side; only a complete match produces a server-side fire, so partial
 * progress never crosses the network.
 * <p>
 * Example:
 *
 * <pre>{@code
 * new SequenceTrigger(layout, Key.KEY_H, Key.KEY_E, Key.KEY_L, Key.KEY_L,
 *         Key.KEY_O).triggers(action);
 * }</pre>
 *
 * <p>
 * Listens on {@code keydown}. Each key in the sequence matches against
 * {@code event.key} or {@code event.code}, so both event.key-named keys (e.g.
 * {@link Key#ENTER}) and event.code-named keys (e.g. {@link Key#KEY_S}) work.
 * Pressing a wrong key resets the position to 0; if that wrong key happens to
 * match position 0 of the sequence, the position advances to 1 (so
 * {@code "abab"} on a sequence configured as {@code "abab"} completes
 * correctly). There is no timeout — a partial sequence persists across
 * arbitrarily long gaps until either it completes or a non-matching key
 * arrives.
 * <p>
 * Inherits {@link #preventDefault()} and {@link #stopPropagation()} from
 * {@link DomEventTrigger} but does not apply them by default — sequences
 * typically share keys with normal typing and should not swallow every
 * keystroke.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class SequenceTrigger extends DomEventTrigger {

    private final List<List<String>> sequenceKeys;

    /**
     * Creates a sequence trigger that fires when the given keys are pressed in
     * order on {@code keydown}.
     *
     * @param host
     *            the component whose root element listens for the sequence, not
     *            {@code null}
     * @param sequence
     *            the keys to match, in order; must contain at least one key,
     *            none of the entries {@code null}
     */
    public SequenceTrigger(Component host, Key... sequence) {
        super(host, "keydown");
        Objects.requireNonNull(sequence, "sequence");
        if (sequence.length == 0) {
            throw new IllegalArgumentException(
                    "Sequence must contain at least one key");
        }
        List<List<String>> snapshot = new ArrayList<>(sequence.length);
        for (Key key : sequence) {
            Objects.requireNonNull(key, "key");
            snapshot.add(List.copyOf(key.getKeys()));
        }
        this.sequenceKeys = List.copyOf(snapshot);
    }

    @Override
    protected void appendInstallPrelude(StringBuilder prelude) {
        // Closure state — survives across event firings because the wrapper
        // h closes over it. One install = one position counter.
        prelude.append("let i=0;");
    }

    @Override
    protected void appendHandlerBody(StringBuilder body,
            List<Object> extraCaptures) {
        // SequenceTrigger extends DomEventTrigger directly, so this is the
        // first extraCaptures contributor — its capture is always at $2.
        // The base appendHandlerBody adds no captures, only suppression
        // statements. If a future subclass inserts captures earlier in the
        // chain this assumption breaks, and the index would need to be
        // computed from extraCaptures.size().
        if (!extraCaptures.isEmpty()) {
            throw new IllegalStateException(
                    "SequenceTrigger assumes its sequence capture lands at $2");
        }
        extraCaptures.add(sequenceKeys);
        // ok($) is true if either event.key or event.code is one of the
        // allowed names for the queried sequence slot.
        // - On mismatch, restart at 1 if this key matches slot 0, else 0.
        // - On match, advance; if not the last slot, return without firing.
        // The trailing $0(e); appended by DomEventTrigger#install runs only
        // when the sequence has just completed.
        body.append("const o=s=>s.includes(e.key)||s.includes(e.code);"
                + "if(!o($2[i])){i=o($2[0])?1:0;return;}"
                + "if(++i!==$2.length)return;i=0;");
        super.appendHandlerBody(body, extraCaptures);
    }
}
