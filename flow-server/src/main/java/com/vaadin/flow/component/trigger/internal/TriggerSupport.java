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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.AbstractAction;
import com.vaadin.flow.component.trigger.AbstractArgument;
import com.vaadin.flow.component.trigger.AbstractCallbackAction;
import com.vaadin.flow.component.trigger.AbstractTrigger;
import com.vaadin.flow.component.trigger.Action;
import com.vaadin.flow.component.trigger.Argument;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.internal.nodefeature.ServerSideFeature;

/**
 * Per-element store of triggers, actions, arguments and bindings for the
 * trigger API. Lazily instantiated by {@link #on(Element)}. Emits client
 * snapshots via {@link Element#executeJs(String, Object...)} on every binding
 * change and on each (re-)attach.
 * <p>
 * For internal use only.
 */
public class TriggerSupport extends ServerSideFeature {

    private final Map<AbstractTrigger, Integer> triggerIds = new IdentityHashMap<>();
    private final Map<AbstractAction, Integer> actionIds = new IdentityHashMap<>();
    private final Map<AbstractArgument<?>, Integer> argumentIds = new IdentityHashMap<>();

    private final Map<Integer, AbstractTrigger> triggersById = new LinkedHashMap<>();
    private final Map<Integer, AbstractAction> actionsById = new LinkedHashMap<>();
    private final Map<Integer, AbstractArgument<?>> argumentsById = new LinkedHashMap<>();

    private record Binding(int triggerId,
            int[] actionIds) implements Serializable {
    }

    private final List<Binding> bindings = new ArrayList<>();

    private final List<Element> elementParams = new ArrayList<>();
    private final Map<Element, Integer> elementParamIndex = new IdentityHashMap<>();

    private int nextTriggerId = 0;
    private int nextActionId = 0;
    private int nextArgumentId = 0;

    private boolean attachListenerRegistered = false;
    private boolean syncScheduled = false;

    private transient @Nullable ReturnChannelRegistration mirrorChannel;

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
     * Looks up an action by id. Used when the client posts a server-side
     * notification back over the per-host return channel.
     *
     * @param id
     *            the action id
     * @return the action, or {@code null} if unknown
     */
    public @Nullable AbstractAction getAction(int id) {
        return actionsById.get(id);
    }

    /**
     * The host element this snapshot belongs to.
     *
     * @return the host element
     */
    public Element getHost() {
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
        ReturnChannelRegistration channel = getMirrorChannel();
        // Parameter layout: $0 = snapshot, $1..$N = extra elements,
        // $N+1 = mirror channel function. Pre-computed before assembling
        // the executeJs expression so the indices line up.
        int extras = elementParams.size();
        int channelIndex = 1 + extras;
        Object[] params = new Object[2 + extras];
        params[0] = snapshot;
        for (int i = 0; i < extras; i++) {
            params[i + 1] = elementParams.get(i);
        }
        params[channelIndex] = channel;

        StringBuilder call = new StringBuilder(
                "if (window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.triggers) {"
                        + " window.Vaadin.Flow.triggers.bind(this, $0");
        call.append(", [");
        for (int i = 0; i < extras; i++) {
            if (i > 0) {
                call.append(',');
            }
            call.append('$').append(i + 1);
        }
        call.append("], $").append(channelIndex);
        call.append("); } else { console.debug(")
                .append("'window.Vaadin.Flow.triggers not loaded'); }");
        host.executeJs(call.toString(), params);
    }

    private ReturnChannelRegistration getMirrorChannel() {
        if (mirrorChannel == null) {
            mirrorChannel = getNode().getFeature(ReturnChannelMap.class)
                    .registerChannel(this::dispatchMirror)
                    .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);
        }
        return mirrorChannel;
    }

    private void dispatchMirror(ArrayNode args) {
        if (args.isEmpty()) {
            return;
        }
        int actionId = args.get(0).asInt();
        AbstractAction action = actionsById.get(actionId);
        if (!(action instanceof AbstractCallbackAction<?> cb)) {
            return;
        }
        JsonNode payloadNode = args.size() > 1 ? args.get(1)
                : JacksonUtils.nullNode();
        invokeCallback(cb, payloadNode);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void invokeCallback(AbstractCallbackAction<?> action,
            JsonNode payloadNode) {
        if (payloadNode.isNull()) {
            return;
        }
        Object payload = JacksonUtils.readValue(payloadNode,
                action.getPayloadType());
        if (payload == null) {
            return;
        }
        ((AbstractCallbackAction) action).applyServerSideEffect(payload);
    }

    private ObjectNode buildSnapshot() {
        ObjectNode root = JacksonUtils.createObjectNode();
        // Iterate over copies so that builders that register new arguments
        // (which mutates argumentsById) don't disturb the in-progress
        // iteration. Order matters: actions may register arguments — process
        // triggers and actions first, arguments last.
        ObjectNode triggersNode = JacksonUtils.createObjectNode();
        for (Map.Entry<Integer, AbstractTrigger> e : List
                .copyOf(triggersById.entrySet())) {
            triggersNode.set(e.getKey().toString(),
                    entryFor(e.getValue().getTypeId(),
                            cfg -> e.getValue().buildClientConfig(cfg)));
        }
        root.set("triggers", triggersNode);

        ObjectNode actionsNode = JacksonUtils.createObjectNode();
        for (Map.Entry<Integer, AbstractAction> e : List
                .copyOf(actionsById.entrySet())) {
            actionsNode.set(e.getKey().toString(),
                    entryFor(e.getValue().getTypeId(),
                            cfg -> e.getValue().buildClientConfig(cfg)));
        }
        root.set("actions", actionsNode);

        ObjectNode argumentsNode = JacksonUtils.createObjectNode();
        for (Map.Entry<Integer, AbstractArgument<?>> e : List
                .copyOf(argumentsById.entrySet())) {
            argumentsNode.set(e.getKey().toString(),
                    entryFor(e.getValue().getTypeId(),
                            cfg -> e.getValue().buildClientConfig(cfg)));
        }
        root.set("arguments", argumentsNode);

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

    private ObjectNode entryFor(String typeId, BuildConfig builder) {
        ObjectNode entry = JacksonUtils.createObjectNode();
        entry.put("type", typeId);
        ObjectNode config = JacksonUtils.createObjectNode();
        builder.build(new EntryContext(config));
        entry.set("config", config);
        return entry;
    }

    @FunctionalInterface
    private interface BuildConfig {
        void build(ConfigContext context);
    }

    /**
     * Per-entry {@link ConfigContext} that scopes {@link #put} writes to one
     * config object while reusing the surrounding TriggerSupport for argument
     * and element registration.
     */
    private final class EntryContext implements ConfigContext {
        private final ObjectNode entry;

        EntryContext(ObjectNode entry) {
            this.entry = entry;
        }

        @Override
        public ConfigContext put(String key, @Nullable Object value) {
            Objects.requireNonNull(key);
            if (value == null) {
                entry.putNull(key);
            } else {
                entry.set(key, JacksonUtils.createNode(value));
            }
            return this;
        }

        @Override
        public int registerArgument(Argument<?> argument) {
            Objects.requireNonNull(argument);
            if (!(argument instanceof AbstractArgument<?> abstractArgument)) {
                throw new IllegalArgumentException(
                        "Argument must extend AbstractArgument: " + argument);
            }
            return argumentIds.computeIfAbsent(abstractArgument, a -> {
                int id = nextArgumentId++;
                argumentsById.put(id, a);
                return id;
            });
        }

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

    /**
     * Invokes the mirror dispatch as if the client had posted the given
     * arguments back over the return channel. The first element is the action
     * id; the second (optional) is the JSON payload. For testing only.
     *
     * @param args
     *            the args array, not {@code null}
     */
    public void dispatchMirrorForTest(ArrayNode args) {
        dispatchMirror(args);
    }

    /**
     * Convenience overload for tests: registers an argument directly with this
     * support, allocating an id without going through a
     * {@code buildClientConfig} call.
     *
     * @param argument
     *            the argument
     * @return the assigned id
     */
    public int registerArgumentForTest(Argument<?> argument) {
        return new EntryContext(JacksonUtils.createObjectNode())
                .registerArgument(argument);
    }
}
