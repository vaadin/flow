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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.AbstractAction;
import com.vaadin.flow.component.trigger.AbstractOutput;
import com.vaadin.flow.component.trigger.AbstractTrigger;
import com.vaadin.flow.component.trigger.Action;
import com.vaadin.flow.component.trigger.Output;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ServerSideFeature;

/**
 * Per-element store of triggers, actions, outputs and bindings for the trigger
 * API. Lazily instantiated by {@link #on(Element)}. Emits client snapshots via
 * {@link Element#executeJs(String, Object...)} on every binding change and on
 * each (re-)attach.
 * <p>
 * For internal use only.
 */
public class TriggerSupport extends ServerSideFeature implements ConfigContext {

    private final Map<AbstractTrigger, Integer> triggerIds = new IdentityHashMap<>();
    private final Map<AbstractAction, Integer> actionIds = new IdentityHashMap<>();
    private final Map<AbstractOutput<?>, Integer> outputIds = new IdentityHashMap<>();

    private final Map<Integer, AbstractTrigger> triggersById = new LinkedHashMap<>();
    private final Map<Integer, AbstractAction> actionsById = new LinkedHashMap<>();
    private final Map<Integer, AbstractOutput<?>> outputsById = new LinkedHashMap<>();

    private record Binding(int triggerId,
            int[] actionIds) implements Serializable {
    }

    private final List<Binding> bindings = new ArrayList<>();

    private final List<Element> elementParams = new ArrayList<>();
    private final Map<Element, Integer> elementParamIndex = new IdentityHashMap<>();

    private int nextTriggerId = 0;
    private int nextActionId = 0;
    private int nextOutputId = 0;

    private boolean attachListenerRegistered = false;
    private boolean syncScheduled = false;

    /**
     * Creates a TriggerSupport feature for the given state node.
     *
     * @param node
     *            the node
     */
    public TriggerSupport(StateNode node) {
        super(node);
    }

    /**
     * Gets or creates the TriggerSupport for the given element.
     *
     * @param host
     *            the element, not {@code null}
     * @return the TriggerSupport instance, never {@code null}
     */
    public static TriggerSupport on(Element host) {
        Objects.requireNonNull(host);
        return host.getNode().getFeature(TriggerSupport.class);
    }

    /**
     * Gets or creates the TriggerSupport for the given component's root
     * element.
     *
     * @param host
     *            the component, not {@code null}
     * @return the TriggerSupport instance, never {@code null}
     */
    public static TriggerSupport on(Component host) {
        Objects.requireNonNull(host);
        return on(host.getElement());
    }

    /**
     * Registers a trigger with this support, assigning it an id.
     *
     * @param trigger
     *            the trigger, not {@code null}
     * @return the assigned id
     */
    public int registerTrigger(AbstractTrigger trigger) {
        Objects.requireNonNull(trigger);
        return triggerIds.computeIfAbsent(trigger, t -> {
            int id = nextTriggerId++;
            triggersById.put(id, t);
            return id;
        });
    }

    /**
     * Registers an action with this support, assigning it an id, deduping by
     * identity.
     *
     * @param action
     *            the action, not {@code null}
     * @return the assigned id
     */
    public int registerAction(AbstractAction action) {
        Objects.requireNonNull(action);
        return actionIds.computeIfAbsent(action, a -> {
            int id = nextActionId++;
            actionsById.put(id, a);
            return id;
        });
    }

    /**
     * Registers an output with this support, assigning it an id, deduping by
     * identity.
     *
     * @param output
     *            the output, not {@code null}
     * @return the assigned id
     */
    @Override
    public int registerOutput(Output<?> output) {
        Objects.requireNonNull(output);
        if (!(output instanceof AbstractOutput<?> abstractOutput)) {
            throw new IllegalArgumentException(
                    "Output must extend AbstractOutput: " + output);
        }
        return outputIds.computeIfAbsent(abstractOutput, o -> {
            int id = nextOutputId++;
            outputsById.put(id, o);
            return id;
        });
    }

    /**
     * Returns a parameter index for the given element to be passed alongside
     * the snapshot. The host's own element is at index 0 (the {@code this} of
     * the executeJs invocation); other elements get sequential indices starting
     * at 1.
     *
     * @param element
     *            the element to reference, not {@code null}
     * @return the parameter index
     */
    @Override
    public int referenceElement(Element element) {
        Objects.requireNonNull(element);
        if (element == getHost()) {
            return 0;
        }
        Integer existing = elementParamIndex.get(element);
        if (existing != null) {
            return existing;
        }
        int index = elementParams.size() + 1;
        elementParams.add(element);
        elementParamIndex.put(element, index);
        return index;
    }

    /**
     * Adds a binding from a trigger to a sequence of actions.
     *
     * @param trigger
     *            the trigger, not {@code null}
     * @param actions
     *            the actions, not {@code null} or empty
     */
    public void bind(AbstractTrigger trigger, Action[] actions) {
        Objects.requireNonNull(trigger);
        Objects.requireNonNull(actions);
        if (actions.length == 0) {
            throw new IllegalArgumentException(
                    "At least one action is required");
        }
        int triggerId = registerTrigger(trigger);
        int[] actionIdArr = new int[actions.length];
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            Objects.requireNonNull(action, "Action must not be null");
            if (!(action instanceof AbstractAction abstractAction)) {
                throw new IllegalArgumentException(
                        "Action must extend AbstractAction: " + action);
            }
            actionIdArr[i] = registerAction(abstractAction);
        }
        bindings.add(new Binding(triggerId, actionIdArr));
        scheduleSync();
    }

    /**
     * Removes a trigger and all bindings created from it.
     *
     * @param trigger
     *            the trigger, not {@code null}
     */
    public void removeTrigger(AbstractTrigger trigger) {
        Objects.requireNonNull(trigger);
        Integer id = triggerIds.remove(trigger);
        if (id == null) {
            return;
        }
        triggersById.remove(id);
        bindings.removeIf(b -> b.triggerId() == id);
        scheduleSync();
    }

    /**
     * Looks up an action by id. Used when the client posts a server-side mirror
     * back over the {@code applyServerSideEffect} channel.
     *
     * @param id
     *            the action id
     * @return the action, or {@code null} if unknown
     */
    public @Nullable AbstractAction getAction(int id) {
        return actionsById.get(id);
    }

    private Element getHost() {
        return Element.get(getNode());
    }

    private void scheduleSync() {
        if (!attachListenerRegistered) {
            attachListenerRegistered = true;
            getHost().addAttachListener(e -> syncToClient());
        }
        if (syncScheduled) {
            return;
        }
        syncScheduled = true;
        getNode().runWhenAttached(ui -> ui.getInternals().getStateTree()
                .beforeClientResponse(getNode(), ctx -> flushSync()));
    }

    private void flushSync() {
        syncScheduled = false;
        if (!getNode().isAttached()) {
            return;
        }
        syncToClient();
    }

    private void syncToClient() {
        Element host = getHost();
        ObjectNode snapshot = buildSnapshot();
        Object[] params = new Object[1 + elementParams.size()];
        params[0] = snapshot;
        for (int i = 0; i < elementParams.size(); i++) {
            params[i + 1] = elementParams.get(i);
        }
        StringBuilder call = new StringBuilder(
                "if (window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.triggers) {"
                        + " window.Vaadin.Flow.triggers.bind(this, $0");
        if (!elementParams.isEmpty()) {
            call.append(", [");
            for (int i = 0; i < elementParams.size(); i++) {
                if (i > 0) {
                    call.append(',');
                }
                call.append('$').append(i + 1);
            }
            call.append(']');
        }
        call.append("); } else { console.debug(")
                .append("'window.Vaadin.Flow.triggers not loaded'); }");
        host.executeJs(call.toString(), params);
    }

    private ObjectNode buildSnapshot() {
        ObjectNode root = JacksonUtils.createObjectNode();
        // Iterate over copies so that builders that register new outputs
        // (which mutates outputsById) don't disturb the in-progress
        // iteration. Order matters: triggers and actions may register
        // outputs, and outputs may reference elements — process triggers
        // and actions first, then outputs last.
        ObjectNode triggersNode = JacksonUtils.createObjectNode();
        for (Map.Entry<Integer, AbstractTrigger> e : List
                .copyOf(triggersById.entrySet())) {
            ObjectNode entry = JacksonUtils.createObjectNode();
            entry.put("type", e.getValue().getTypeId());
            entry.set("config", e.getValue().buildClientConfig(this));
            triggersNode.set(e.getKey().toString(), entry);
        }
        root.set("triggers", triggersNode);

        ObjectNode actionsNode = JacksonUtils.createObjectNode();
        for (Map.Entry<Integer, AbstractAction> e : List
                .copyOf(actionsById.entrySet())) {
            ObjectNode entry = JacksonUtils.createObjectNode();
            entry.put("type", e.getValue().getTypeId());
            entry.set("config", e.getValue().buildClientConfig(this));
            actionsNode.set(e.getKey().toString(), entry);
        }
        root.set("actions", actionsNode);

        ObjectNode outputsNode = JacksonUtils.createObjectNode();
        for (Map.Entry<Integer, AbstractOutput<?>> e : List
                .copyOf(outputsById.entrySet())) {
            ObjectNode entry = JacksonUtils.createObjectNode();
            entry.put("type", e.getValue().getTypeId());
            entry.set("config", e.getValue().buildClientConfig(this));
            outputsNode.set(e.getKey().toString(), entry);
        }
        root.set("outputs", outputsNode);

        ArrayNode bindingsNode = JacksonUtils.createArrayNode();
        for (Binding b : bindings) {
            ObjectNode entry = JacksonUtils.createObjectNode();
            entry.put("trigger", b.triggerId());
            ArrayNode actionsArr = JacksonUtils.createArrayNode();
            for (int a : b.actionIds()) {
                actionsArr.add(a);
            }
            entry.set("actions", actionsArr);
            bindingsNode.add(entry);
        }
        root.set("bindings", bindingsNode);
        return root;
    }

    // Test-only accessors.

    /**
     * Builds the snapshot for testing.
     *
     * @return the snapshot
     */
    public ObjectNode snapshotForTest() {
        return buildSnapshot();
    }

    /**
     * Parameter array (excluding the host at index 0) for testing.
     *
     * @return the secondary elements
     */
    public Element[] elementParamsForTest() {
        return elementParams.toArray(new Element[0]);
    }
}
