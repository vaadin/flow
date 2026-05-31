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
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Replaces a target component's children with a server-provided list of
 * components when the bound trigger fires, then restores the original children
 * after a configurable timeout. Pure client-side — no server round-trip after
 * install.
 * <p>
 * The replacement components are attached as
 * {@linkplain com.vaadin.flow.dom.Node#appendVirtualChild virtual children} of
 * the target at construction time, so their DOM is sent to the browser once and
 * the swap on fire is a pure DOM operation. They live as virtual children for
 * the lifetime of the target — fine for a handful of replacements per target,
 * but avoid constructing many short-lived instances.
 * <p>
 * Slots are handled by the browser. Set {@code slot="…"} on the replacement
 * component's root element if the target uses named slots; the browser's
 * shadow-DOM slot routing distributes the children when they are appended:
 *
 * <pre>{@code
 * Icon check = new Icon(VaadinIcon.CHECK);
 * check.getElement().setAttribute("slot", "prefix");
 * new ClickTrigger(copyBtn).triggers(new ReplaceChildrenTemporarilyAction(
 *         copyBtn, check, new Text("Copied")));
 * }</pre>
 *
 * The "original" children are read on the client at the first fire of a cycle,
 * before the new children are inserted. Rapid re-fires within the timeout
 * window are coalesced (same semantics as
 * {@link SetPropertyTemporarilyAction}): the original captured on the first
 * fire is preserved, the replacements are re-applied, and the revert timer is
 * reset. All {@code ReplaceChildrenTemporarilyAction}s on the same target share
 * one stash entry (keyed on {@code "children"}), so two such actions in flight
 * together still restore the truly-original children at the end.
 * <p>
 * The default timeout is one second ({@link #DEFAULT_TIMEOUT}).
 * {@link Duration#ZERO} is allowed — the reversion is queued onto the next
 * event-loop turn, briefly showing the replacement before reverting.
 * <p>
 * Server-side state is not updated by this action; the changes (both the
 * temporary swap and the reversion) live in the browser until the next sync
 * from the client (if any). Plain-text replacements go through {@link Text}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ReplaceChildrenTemporarilyAction extends Action {

    // Shared key for the per-element stash. All ReplaceChildren actions on
    // the same target coalesce — first-fire originals win on revert.
    private static final String STASH_KEY = "children";

    /**
     * Default timeout used by constructors that don't take an explicit
     * {@link Duration}: one second.
     */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(1);

    private final Element target;
    private final Element[] replacements;
    private final long timeoutMillis;

    /**
     * Creates an action that temporarily replaces the children of
     * {@code target} with {@code replacementChildren} when the trigger fires,
     * reverting after {@link #DEFAULT_TIMEOUT}.
     *
     * @param target
     *            the component whose children to replace, not {@code null}
     * @param replacementChildren
     *            the replacement components; each must not be {@code null} and
     *            must not already have a parent. An empty array temporarily
     *            clears the children.
     */
    public ReplaceChildrenTemporarilyAction(Component target,
            Component... replacementChildren) {
        this(target, DEFAULT_TIMEOUT, replacementChildren);
    }

    /**
     * Creates an action that temporarily replaces the children of
     * {@code target} with {@code replacementChildren} when the trigger fires,
     * reverting after {@code timeout}.
     *
     * @param target
     *            the component whose children to replace, not {@code null}
     * @param timeout
     *            how long to keep the replacement before reverting, not
     *            {@code null} and not negative; {@link Duration#ZERO} is
     *            allowed
     * @param replacementChildren
     *            the replacement components; each must not be {@code null} and
     *            must not already have a parent. An empty array temporarily
     *            clears the children.
     */
    public ReplaceChildrenTemporarilyAction(Component target, Duration timeout,
            Component... replacementChildren) {
        this.target = Objects.requireNonNull(target, "target must not be null")
                .getElement();
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "timeout must not be negative: " + timeout);
        }
        this.timeoutMillis = timeout.toMillis();
        Objects.requireNonNull(replacementChildren,
                "replacementChildren must not be null");
        this.replacements = Arrays.stream(replacementChildren)
                .map(c -> Objects
                        .requireNonNull(c, "replacement child must not be null")
                        .getElement())
                .toArray(Element[]::new);
        // Register replacements as virtual children so the client has the
        // DOM available when the trigger fires. Throws if any replacement
        // is already parented elsewhere — the framework's message is clear
        // enough to surface as-is.
        if (replacements.length > 0) {
            this.target.appendVirtualChild(replacements);
        }
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        // Three payload-specific sub-functions: snapshot reads the current
        // child node list (including text and comment nodes), apply swaps
        // in the replacements, revert restores the snapshot. Action's
        // applyTemporarily helper handles stash/timer plumbing.
        JsFunction snapshot = JsFunction.of("return Array.from($0.childNodes)",
                target);
        JsFunction apply = JsFunction
                .of(buildReplaceChildrenBody(replacements.length),
                        applyCaptures())
                .withArguments("event");
        JsFunction revert = JsFunction
                .of("$0.replaceChildren(...original)", target)
                .withArguments("original");
        return applyTemporarily(target, STASH_KEY, snapshot, apply, revert,
                timeoutMillis);
    }

    private Object[] applyCaptures() {
        Object[] captures = new Object[replacements.length + 1];
        captures[0] = target;
        System.arraycopy(replacements, 0, captures, 1, replacements.length);
        return captures;
    }

    private static String buildReplaceChildrenBody(int replacementCount) {
        // Positional $N references only — not data interpolated into the
        // body. For zero replacements this produces "$0.replaceChildren()"
        // which correctly clears the children.
        String refs = IntStream.rangeClosed(1, replacementCount)
                .mapToObj(i -> "$" + i).collect(Collectors.joining(", "));
        return "$0.replaceChildren(" + refs + ")";
    }
}
