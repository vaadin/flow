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
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.BundledEventData;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.CspViolationReporter;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JavaScriptSemantics;
import com.vaadin.flow.internal.ParameterizedConstantPoolKey;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.shared.JsonConstants;

/**
 * Map of DOM events with server-side listeners. The key set of this map
 * describes the event types for which listeners are present. The values
 * associated with the keys are currently not used.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementListenerMap extends NodeMap {
    /**
     * Dummy filter string that always passes. The client runs it without
     * compiling it.
     */
    public static final String ALWAYS_TRUE_FILTER = JsonConstants.EVENT_FILTER_ALWAYS_TRUE;

    private static final EnumSet<DebouncePhase> NO_TIMEOUT_PHASES = EnumSet
            .of(DebouncePhase.LEADING);

    /**
     * Starts the key of a declared snippet. No JavaScript expression starts
     * with <code>@</code>, so the key of a snippet is never equal to the key of
     * an expression.
     */
    private static final String SNIPPET_KEY_PREFIX = "@";

    /**
     * {@code event} or {@code element}, followed by any number of {@code .name}
     * or {@code ?.name} segments. The client reads such a path without
     * compiling it, see {@code PropertyPathExpression.ts}, which matches the
     * same paths. The quantifiers are possessive so that a long input can't
     * cause deep backtracking; a segment always starts with a dot, so nothing
     * needs to be backtracked.
     */
    private static final Pattern PROPERTY_PATH = Pattern
            .compile("(event|element)(\\??\\.[A-Za-z_$][\\w$]*+)*+");

    private static final Entry ALWAYS_TRUE_ENTRY = new ExpressionEntry(
            ALWAYS_TRUE_FILTER);

    // Server-side only data
    private Map<String, List<DomEventListenerWrapper>> listeners;

    /**
     * Placeholder stored in the map instead of the actual event settings. The
     * settings JSON is derived on demand from the registered listeners when
     * changes are collected, to avoid keeping it in memory.
     */
    private enum EventSettingsMarker {
        INSTANCE
    }

    /**
     * Something that the client runs when an event occurs: a JavaScript
     * expression or a declared snippet. The client sends the value of the entry
     * to the server under the key of the entry.
     */
    sealed interface Entry extends Serializable
            permits ExpressionEntry, BundledExpressionEntry, SnippetEntry {
        String key();
    }

    /**
     * A JavaScript expression. The key is the expression itself, for example
     * <code>event.clientX</code>.
     *
     * @param key
     *            the expression
     */
    private record ExpressionEntry(String key) implements Entry {
    }

    /**
     * An {@link com.vaadin.flow.component.EventData} value that the build has
     * put into the bundle as a function. The key is the expression, as for an
     * {@link ExpressionEntry}, but the client is sent the identifier of the
     * function instead of compiling the expression, see
     * {@link BundledEventData}.
     *
     * @param key
     *            the expression
     */
    private record BundledExpressionEntry(String key) implements Entry {
    }

    /**
     * A function declared in the bundle, with the values it captures, that the
     * client runs when an event occurs: a filter added with
     * {@link DomListenerRegistration#setFilterExpression(DomEventFilter)} or
     * event data added with
     * {@link DomListenerRegistration#addEventData(DomEventData)}. The client
     * runs the call and sends the value it returns to the server under the key
     * of the entry.
     * <p>
     * The key is <code>@</code> followed by the key of the snippet, see
     * {@link JsCall#createSnippetKey()}.
     * <p>
     * An entry can have a side effect: the name of a method of the event,
     * <code>preventDefault</code> or <code>stopPropagation</code>. The client
     * then calls the method when the call returns a truthy value. This is used
     * for <code>addEventData("event.preventDefault()")</code> on a listener
     * with a declared filter: the event is prevented only when the filter
     * passes. The key then ends with <code>.</code> and the name of the method,
     * so it is different from the key of the filter itself. Such an entry is
     * not a filter, as it doesn't decide which listeners are notified.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param key
     *            the key that the client sends the value under, not
     *            <code>null</code>
     * @param call
     *            the call that the client runs, not <code>null</code>
     * @param sideEffect
     *            the name of the method of the event that the client calls when
     *            the call returns a truthy value, or <code>null</code> if there
     *            is no side effect
     * @param filter
     *            <code>true</code> if the entry is the filter of a listener,
     *            which is notified only when the value is truthy, even if the
     *            same call is also event data; <code>false</code> if it is only
     *            event data or has a side effect
     * @see #getSnippetEntries(String)
     */
    public record SnippetEntry(String key, JsCall call,
            @Nullable String sideEffect, boolean filter) implements Entry {

        static SnippetEntry of(DomEventData<?> eventData) {
            return new SnippetEntry(getEventDataKey(eventData),
                    eventData.getDeclaredCall(), null, false);
        }

        static SnippetEntry of(DomEventFilter filter) {
            return new SnippetEntry(SNIPPET_KEY_PREFIX + filter.getSnippetKey(),
                    filter.getDeclaredCall(), null, true);
        }

        SnippetEntry withSideEffect(String method) {
            return new SnippetEntry(key + "." + method, call, method, false);
        }
    }

    /**
     * The settings of one entry for an event type: the entry and its debounce
     * settings.
     */
    private static final class ExpressionSettings implements Serializable {
        private Entry entry;
        private final Map<Integer, Set<DebouncePhase>> debounceSettings = new HashMap<>();

        private ExpressionSettings(Entry entry) {
            this.entry = entry;
        }

        /**
         * Checks whether these settings are the same for every element that
         * uses them, so that they can be shared through the constant pool. A
         * snippet that captures for example an element is sent for each element
         * instead.
         *
         * @return <code>true</code> if the settings can be sent through the
         *         constant pool
         */
        private boolean isShared() {
            // A bundled expression captures nothing
            return !(entry instanceof SnippetEntry snippet)
                    || snippet.call().hasOnlyPlainSnippetCaptures();
        }

        public void addDebouncePhases(int timeout, Set<DebouncePhase> phases) {
            debounceSettings.merge(Integer.valueOf(timeout), phases,
                    (phases1, phases2) -> {
                        EnumSet<DebouncePhase> merge = EnumSet.copyOf(phases1);
                        merge.addAll(phases2);
                        return merge;
                    });
        }

        /**
         * Creates the JSON for the client. For an expression, this is only the
         * debounce settings, as the key is the expression. For a snippet, this
         * is an object with the function, the debounce settings, the side
         * effect and the captures. The captures are encoded now, so that an
         * element is sent with the id that it has now. An expression that the
         * bundle has a function for is sent like a snippet without captures, so
         * the client runs the function instead of compiling the key.
         *
         * @return the debounce settings for an expression, or the settings
         *         object for a snippet, not <code>null</code>
         */
        public JsonNode toJson() {
            if (entry instanceof BundledExpressionEntry(String key)) {
                ObjectNode json = JacksonUtils.createObjectNode();
                json.put(JsonConstants.EVENT_SETTINGS_FUNCTION,
                        BundledEventData.getFunctionId(key));
                json.set(JsonConstants.EVENT_SETTINGS_DEBOUNCE,
                        debounceToJson());
                return json;
            }
            if (!(entry instanceof SnippetEntry snippet)) {
                return debounceToJson();
            }
            ObjectNode json = JacksonUtils.createObjectNode();
            json.put(JsonConstants.EVENT_SETTINGS_FUNCTION,
                    snippet.call().getFunctionId());
            json.set(JsonConstants.EVENT_SETTINGS_DEBOUNCE, debounceToJson());
            if (snippet.sideEffect() != null) {
                json.put(JsonConstants.EVENT_SETTINGS_SIDE_EFFECT,
                        snippet.sideEffect());
            }
            ArrayNode captures = snippet.call().encodeSnippetCaptures();
            if (!captures.isEmpty()) {
                json.set(JsonConstants.EVENT_SETTINGS_CAPTURES, captures);
            }
            return json;
        }

        private JsonNode debounceToJson() {
            if (debounceSettings.isEmpty()) {
                return JacksonUtils.createNode(false);
            } else if (debounceSettings.size() == 1
                    && debounceSettings.containsKey(Integer.valueOf(0))) {
                // Shorthand if only debounce is a dummy filter debounce
                return JacksonUtils.createNode(true);
            } else {
                // [[timeout1, phase1, phase2, ...], [timeout2, phase1, ...]]
                return debounceSettings.entrySet().stream()
                        .map(debounce -> Stream.concat(
                                Stream.of(JacksonUtils
                                        .createNode(debounce.getKey())),
                                debounce.getValue().stream()
                                        .map(DebouncePhase::getIdentifier)
                                        .map(JacksonUtils::createNode))
                                .collect(JacksonUtils.asArray()))
                        .collect(JacksonUtils.asArray());
            }

        }
    }

    private static class DomEventListenerWrapper
            implements DomListenerRegistration {
        private final String type;
        private final DomEventListener origin;
        private final ElementListenerMap listenerMap;

        private DisabledUpdateMode mode = DisabledUpdateMode.ONLY_WHEN_ENABLED;
        /*
         * Exactly sized unmodifiable list rather than a set: listeners hold a
         * handful of expressions, so a hash set would cost several times more
         * memory for each one. The list may be shared with other listeners, see
         * addEventData(List), and is replaced with a copy when adding.
         */
        private List<String> eventDataExpressions;
        // Shared like eventDataExpressions
        private List<String> bundledEventData;
        private List<DomEventData<?>> eventDataSnippets;
        private Entry filter;

        private int debounceTimeout = 0;
        private EnumSet<DebouncePhase> debouncePhases = NO_TIMEOUT_PHASES;
        private List<SerializableRunnable> unregisterHandlers;
        private boolean allowInert;

        private DomEventListenerWrapper(ElementListenerMap listenerMap,
                String type, DomEventListener origin) {
            this.listenerMap = listenerMap;
            this.type = type;
            this.origin = origin;
        }

        @Override
        public String getEventType() {
            return type;
        }

        @Override
        public void remove() {
            if (unregisterHandlers != null) {
                unregisterHandlers.forEach(SerializableRunnable::run);
            }

            listenerMap.removeListener(type, this);

            // update settings after removal. If we have listeners of the
            // same type registered, we want to remove settings set by this
            // particular listener from the overall set
            // fixes #5090
            if (listenerMap.listeners != null
                    && listenerMap.listeners.containsKey(type)) {
                listenerMap.updateEventSettings(type);
            }
        }

        @Override
        public DomListenerRegistration addEventData(String eventData) {
            if (eventData == null) {
                throw new IllegalArgumentException(
                        "The event data expression must not be null");
            }
            if (isCompiledByClient(eventData)) {
                CspViolationReporter.reportStringJavaScript();
            }

            appendEventData(List.of(eventData));

            listenerMap.updateEventSettings(type);

            return this;
        }

        @Override
        public DomListenerRegistration addEventData(List<String> eventData) {
            requireEventData(eventData);
            if (eventData.stream()
                    .anyMatch(ElementListenerMap::isCompiledByClient)) {
                CspViolationReporter.reportStringJavaScript();
            }

            appendEventData(eventData);

            listenerMap.updateEventSettings(type);

            return this;
        }

        private void addEventDataWithoutCspReport(List<String> eventData) {
            requireEventData(eventData);

            appendEventData(eventData);

            listenerMap.updateEventSettings(type);
        }

        private void addBundledEventData(List<String> eventData) {
            requireEventData(eventData);

            if (bundledEventData == null) {
                bundledEventData = List.copyOf(eventData);
            } else if (!bundledEventData.containsAll(eventData)) {
                bundledEventData = Stream
                        .concat(bundledEventData.stream(), eventData.stream())
                        .distinct().toList();
            }

            listenerMap.updateEventSettings(type);
        }

        private static void requireEventData(List<String> eventData) {
            if (eventData == null) {
                throw new IllegalArgumentException(
                        "The event data expressions must not be null");
            }
            for (String expression : eventData) {
                if (expression == null) {
                    throw new IllegalArgumentException(
                            "The event data expression must not be null");
                }
            }
        }

        private void appendEventData(List<String> eventData) {
            if (eventDataExpressions == null) {
                // Keeps the given list if it is already unmodifiable
                eventDataExpressions = List.copyOf(eventData);
            } else if (!eventDataExpressions.containsAll(eventData)) {
                eventDataExpressions = Stream
                        .concat(eventDataExpressions.stream(),
                                eventData.stream())
                        .distinct().toList();
            }
        }

        @Override
        public DomListenerRegistration addEventData(DomEventData<?> eventData) {
            Objects.requireNonNull(eventData, "Event data cannot be null");

            if (eventDataSnippets == null) {
                // Most listeners have one value, not the default ten
                eventDataSnippets = new ArrayList<>(1);
            }
            String key = eventData.getSnippetKey();
            if (eventDataSnippets.stream()
                    .noneMatch(added -> added.getSnippetKey().equals(key))) {
                eventDataSnippets.add(eventData);
            }

            listenerMap.updateEventSettings(type);

            return this;
        }

        @Override
        public DomListenerRegistration setDisabledUpdateMode(
                DisabledUpdateMode disabledUpdateMode) {
            if (disabledUpdateMode == null) {
                throw new IllegalArgumentException(
                        "RPC communication control mode for disabled element must not be null");
            }

            mode = disabledUpdateMode;
            return this;
        }

        @Override
        public DomListenerRegistration setFilter(String filter) {
            if (filter != null && !isPropertyPath(filter)) {
                CspViolationReporter.reportStringJavaScript();
            }
            return updateFilter(
                    filter == null ? null : new ExpressionEntry(filter));
        }

        @Override
        public DomListenerRegistration setFilterExpression(
                DomEventFilter filter) {
            return updateFilter(
                    filter == null ? null : SnippetEntry.of(filter));
        }

        private DomListenerRegistration updateFilter(Entry filter) {
            this.filter = filter;

            listenerMap.updateEventSettings(type);

            return this;
        }

        @Override
        public String getFilter() {
            // A declared filter has no expression to return
            return filter instanceof ExpressionEntry(String key) ? key : null;
        }

        boolean matchesFilter(JsonNode eventData) {
            if (filter == null) {
                // No filter: always matches
                return true;
            }

            if (eventData == null) {
                // No event data: cannot match the filter
                return false;
            }

            // The browser sends the value that the filter returned, which does
            // not have to be a boolean. The value of a filter that did not
            // pass is also sent when another filter of the event passed.
            return JavaScriptSemantics.isTrueish(eventData.get(filter.key()));
        }

        @Override
        public DomListenerRegistration debounce(int timeout,
                DebouncePhase firstPhase, DebouncePhase... additionalPhases) {
            if (timeout < 0) {
                throw new IllegalArgumentException(
                        "Timeout cannot be negative");
            }

            debounceTimeout = timeout;

            if (timeout == 0) {
                debouncePhases = NO_TIMEOUT_PHASES;
            } else {
                debouncePhases = EnumSet.of(firstPhase, additionalPhases);
            }

            listenerMap.updateEventSettings(type);

            return this;
        }

        @Override
        public int getDebounceTimeout() {
            return debounceTimeout;
        }

        @Override
        public Set<DebouncePhase> getDebouncePhases() {
            return Collections.unmodifiableSet(debouncePhases);
        }

        public boolean matchesPhase(DebouncePhase phase) {
            return debouncePhases.contains(phase);
        }

        @Override
        public DomListenerRegistration onUnregister(
                SerializableRunnable unregisterHandler) {
            if (unregisterHandlers == null) {
                unregisterHandlers = new ArrayList<>(1);
            }
            unregisterHandlers.add(Objects.requireNonNull(unregisterHandler,
                    "Unregister handler cannot be null"));
            return this;
        }

        private boolean isPropertySynchronized(String propertyName) {
            return hasEventData(
                    JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN + propertyName);
        }

        private boolean hasEventData(String expression) {
            return eventDataExpressions != null
                    && eventDataExpressions.contains(expression);
        }

        @Override
        public DomListenerRegistration allowInert() {
            allowInert = true;
            return this;
        }
    }

    /**
     * Checks whether the client compiles the given event data from a string.
     * The client reads a synchronized property, the state node closest to the
     * event target or a property path, also one that leads to the element to
     * map to a state node, and calls <code>preventDefault()</code> or
     * <code>stopPropagation()</code>, without compiling anything.
     *
     * @param eventData
     *            the event data, as passed to
     *            {@link DomListenerRegistration#addEventData(String)}, not
     *            <code>null</code>
     * @return <code>true</code> if the client compiles the event data with
     *         <code>new Function</code>, which a content security policy
     *         without <code>unsafe-eval</code> does not allow
     */
    public static boolean isCompiledByClient(String eventData) {
        String expression = eventData
                .startsWith(JsonConstants.MAP_STATE_NODE_EVENT_DATA)
                        ? eventData.substring(
                                JsonConstants.MAP_STATE_NODE_EVENT_DATA
                                        .length())
                        : eventData;
        return !eventData.startsWith(JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN)
                && !eventData.equals(JsonConstants.MAP_STATE_NODE_EVENT_DATA)
                && !eventData.equals(JsonConstants.EVENT_DATA_PREVENT_DEFAULT)
                && !eventData.equals(JsonConstants.EVENT_DATA_STOP_PROPAGATION)
                && !isPropertyPath(expression);
    }

    private static boolean isPropertyPath(String expression) {
        return PROPERTY_PATH.matcher(expression).matches();
    }

    /**
     * Creates a new element listener map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     *
     */
    public ElementListenerMap(StateNode node) {
        super(node);
    }

    /**
     * Add eventData for an event type.
     *
     * @param eventType
     *            the event type
     * @param listener
     *            the listener to add
     * @return a handle for configuring and removing the listener
     */
    public DomListenerRegistration add(String eventType,
            DomEventListener listener) {
        assert eventType != null;
        assert listener != null;

        if (!contains(eventType)) {
            assert listeners == null || !listeners.containsKey(eventType);

            ArrayList<DomEventListenerWrapper> listenerList = new ArrayList<>(
                    1);

            if (listeners == null) {
                listeners = Collections.singletonMap(eventType, listenerList);
            } else {
                if (listeners.size() == 1 && !(listeners instanceof HashMap)) {
                    listeners = new HashMap<>(listeners);
                }

                listeners.put(eventType, listenerList);
            }

        }

        DomEventListenerWrapper listenerWrapper = new DomEventListenerWrapper(
                this, eventType, listener);

        listeners.get(eventType).add(listenerWrapper);

        updateEventSettings(eventType);

        return listenerWrapper;
    }

    private Collection<DomEventListenerWrapper> getWrappers(String eventType) {
        if (listeners == null) {
            return Collections.emptyList();
        }
        List<DomEventListenerWrapper> typeListeners = listeners.get(eventType);
        if (typeListeners == null) {
            return Collections.emptyList();
        }

        return typeListeners;
    }

    private Map<String, ExpressionSettings> collectEventExpressions(
            String eventType) {
        Map<String, ExpressionSettings> expressions = new HashMap<>();
        boolean hasUnfilteredListener = false;
        boolean hasFilteredListener = false;

        Function<Entry, ExpressionSettings> ensureExpression = entry -> {
            ExpressionSettings settings = expressions.computeIfAbsent(
                    entry.key(), key -> new ExpressionSettings(entry));
            // The same call can be the filter of one listener and event data
            // of another, which share a key. The value then still decides
            // whether the filtered listener is notified.
            if (entry instanceof SnippetEntry snippet && snippet.filter()) {
                settings.entry = snippet;
            }
            return settings;
        };

        Collection<DomEventListenerWrapper> wrappers = getWrappers(eventType);

        for (DomEventListenerWrapper wrapper : wrappers) {
            Entry filter = wrapper.filter;

            // Process event data expressions, handling preventDefault and
            // stopPropagation specially
            if (wrapper.eventDataExpressions != null) {
                for (String expression : wrapper.eventDataExpressions) {
                    ensureExpression.apply(makeConditional(expression, filter));
                }
            }
            if (wrapper.bundledEventData != null) {
                for (String expression : wrapper.bundledEventData) {
                    ensureExpression
                            .apply(new BundledExpressionEntry(expression));
                }
            }
            if (wrapper.eventDataSnippets != null) {
                for (DomEventData<?> eventData : wrapper.eventDataSnippets) {
                    ensureExpression.apply(SnippetEntry.of(eventData));
                }
            }

            int timeout = wrapper.debounceTimeout;
            if (timeout > 0 && filter == null) {
                filter = ALWAYS_TRUE_ENTRY;
            }

            if (filter == null) {
                hasUnfilteredListener = true;
            } else {
                hasFilteredListener = true;

                ensureExpression.apply(filter).addDebouncePhases(timeout,
                        wrapper.debouncePhases);
            }
        }

        if (hasFilteredListener && hasUnfilteredListener) {
            /*
             * If there are filters and none match, then client won't send
             * anything to the server.
             *
             * Include a filter that always passes to ensure that unfiltered
             * listeners are still notified.
             */
            ensureExpression.apply(ALWAYS_TRUE_ENTRY).addDebouncePhases(0,
                    NO_TIMEOUT_PHASES);
        }

        return expressions;
    }

    /**
     * Gets the entry for an event data expression of a listener with the given
     * filter. If the expression is <code>event.preventDefault()</code> or
     * <code>event.stopPropagation()</code> and the listener has a filter, the
     * method of the event is called only when the filter passes:
     * <ul>
     * <li>For a filter expression, for example <code>event.key ===
     * 'Enter'</code>, the entry is the expression <code>(event.key ===
     * 'Enter') &amp;&amp; event.preventDefault()</code>.</li>
     * <li>For a declared filter, the entry runs the filter and has the method
     * as its side effect. Nothing is made from strings, so no JavaScript is
     * compiled in the browser.</li>
     * </ul>
     *
     * @param expression
     *            the event data expression, not <code>null</code>
     * @param filter
     *            the filter of the listener, or <code>null</code> if it has
     *            none
     * @return the entry to send for the expression, not <code>null</code>
     */
    private static Entry makeConditional(String expression, Entry filter) {
        String method = switch (expression) {
        case JsonConstants.EVENT_DATA_PREVENT_DEFAULT -> "preventDefault";
        case JsonConstants.EVENT_DATA_STOP_PROPAGATION -> "stopPropagation";
        default -> null;
        };
        if (method == null || filter == null) {
            return new ExpressionEntry(expression);
        }
        return switch (filter) {
        case ExpressionEntry(String key) when key.isEmpty() ->
            new ExpressionEntry(expression);
        case ExpressionEntry(String key) ->
            new ExpressionEntry("(" + key + ") && event." + method + "()");
        case SnippetEntry snippet -> snippet.withSideEffect(method);
        case BundledExpressionEntry bundled -> throw new IllegalStateException(
                "Event data from the bundle is never the filter of a listener");
        };
    }

    private void updateEventSettings(String eventType) {
        put(eventType, EventSettingsMarker.INSTANCE);
    }

    @Override
    protected boolean producePutChange(String key, boolean hadValueEarlier,
            Serializable newValue) {
        // The stored marker never changes even though the settings derived
        // from it do, so every update needs to be sent
        return true;
    }

    /**
     * Creates the value that is sent to the client for an event type. The
     * settings that are the same for every element are sent through the
     * constant pool. If there are settings for this element only, such as a
     * snippet that captures an element, the value is the constant pool key
     * together with those settings.
     *
     * @param eventType
     *            the event type, not <code>null</code>
     * @return a {@link ConstantPoolKey}, or a
     *         {@link ParameterizedConstantPoolKey} if some settings are for
     *         this element only
     */
    private Serializable createEventSettingsKey(String eventType) {
        ObjectNode shared = JacksonUtils.createObjectNode();
        ObjectNode ownSettings = JacksonUtils.createObjectNode();
        collectEventExpressions(eventType).forEach(
                (key, settings) -> (settings.isShared() ? shared : ownSettings)
                        .set(key, settings.toJson()));

        ConstantPoolKey sharedKey = new ConstantPoolKey(shared);
        return ownSettings.isEmpty() ? sharedKey
                : new ParameterizedConstantPoolKey(sharedKey, ownSettings);
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        super.collectChanges(change -> {
            if (change instanceof MapPutChange putChange
                    && putChange.getValue() instanceof EventSettingsMarker) {
                String eventType = putChange.getKey();
                collector.accept(new MapPutChange(this, eventType,
                        createEventSettingsKey(eventType)));
            } else {
                collector.accept(change);
            }
        });
    }

    private void removeListener(String eventType,
            DomEventListenerWrapper wrapper) {
        if (listeners == null) {
            return;
        }
        Collection<DomEventListenerWrapper> listenerList = listeners
                .get(eventType);
        if (listenerList != null) {
            listenerList.remove(wrapper);

            // No more listeners of this type?
            if (listenerList.isEmpty()) {
                if (listeners.size() == 1) {
                    assert listeners.containsKey(eventType);
                    listeners = null;
                } else {
                    listeners.remove(eventType);
                }

                // Remove from the set that is synchronized with the client
                remove(eventType);
            }
        }
    }

    /**
     * Fires an event to all listeners registered for the given type.
     *
     * @param event
     *            the event to fire
     */
    public void fireEvent(DomEvent event) {
        if (listeners == null) {
            return;
        }
        final boolean isElementEnabled = event.getSource().isEnabled();

        final boolean isNavigationRequest = UI.BrowserNavigateEvent.EVENT_NAME
                .equals(event.getType())
                || UI.BrowserLeaveNavigationEvent.EVENT_NAME
                        .equals(event.getType())
                || UI.BrowserRefreshEvent.EVENT_NAME.equals(event.getType());

        final boolean inert = event.getSource().getNode().isInert();

        List<DomEventListenerWrapper> typeListeners = listeners
                .get(event.getType());
        if (typeListeners == null) {
            return;
        }

        List<DomEventListener> listeners = new ArrayList<>();
        for (DomEventListenerWrapper wrapper : typeListeners) {
            if (!isNavigationRequest && inert && !wrapper.allowInert) {
                // drop as inert
                LoggerFactory.getLogger(ElementListenerMap.class.getName())
                        .info("Ignored listener invocation for {} event from "
                                + "the client side for an inert {} element",
                                event.getType(), event.getSource().getTag());
                continue;
            }

            if ((isElementEnabled
                    || DisabledUpdateMode.ALWAYS.equals(wrapper.mode))
                    && wrapper.matchesFilter(event.getEventData())
                    && wrapper.matchesPhase(event.getPhase())) {
                listeners.add(wrapper.origin);
            }
        }

        listeners.forEach(listener -> listener.handleEvent(event));
    }

    /**
     * Gets the event data expressions defined for the given event name. This
     * method is currently only provided to facilitate unit testing.
     *
     * @param eventName
     *            the name of the event, not <code>null</code>
     * @return an unmodifiable set of event data expressions, not
     *         <code>null</code>
     * @since 3.0
     */
    public Set<String> getExpressions(String eventName) {
        Objects.requireNonNull(eventName, "Event name cannot be null");
        // Declared snippets have no expression
        return collectEventExpressions(eventName).values().stream()
                .map(settings -> settings.entry)
                .filter(entry -> !(entry instanceof SnippetEntry))
                .map(Entry::key).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Gets the declared snippets that the client runs for the given event name:
     * the filters and the event data that are declared as JavaScript functions
     * of the bundle instead of as expressions. Together with
     * {@link #getExpressions(String)}, this is everything the client runs when
     * the event occurs. This method is currently only provided to facilitate
     * testing, such as simulating events on the server.
     *
     * @param eventName
     *            the name of the event, not <code>null</code>
     * @return an unmodifiable set of the declared snippets, not
     *         <code>null</code>
     */
    public Set<SnippetEntry> getSnippetEntries(String eventName) {
        Objects.requireNonNull(eventName, "Event name cannot be null");
        return collectEventExpressions(eventName).values().stream()
                .map(settings -> settings.entry)
                .filter(SnippetEntry.class::isInstance)
                .map(SnippetEntry.class::cast)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Adds event data to a registration of this map like
     * {@link DomListenerRegistration#addEventData(List)}, but without reporting
     * them as JavaScript sent as a string. For the keys of data that client
     * code builds and sends with <code>sendEventMessage</code>, which the
     * client never compiles.
     *
     * @param registration
     *            a registration returned by
     *            {@link #add(String, DomEventListener)}, not <code>null</code>
     * @param eventData
     *            the keys of the event data, not <code>null</code> and not
     *            containing <code>null</code>
     */
    public static void addEventDataWithoutCspReport(
            DomListenerRegistration registration, List<String> eventData) {
        if (!(registration instanceof DomEventListenerWrapper wrapper)) {
            throw new IllegalArgumentException(
                    "The registration must be one returned by an ElementListenerMap");
        }
        wrapper.addEventDataWithoutCspReport(eventData);
    }

    /**
     * Adds {@link com.vaadin.flow.component.EventData} values to a registration
     * of this map like {@link DomListenerRegistration#addEventData(List)}, for
     * values that the build has put into the bundle as functions, see
     * {@link BundledEventData}. The client runs the function of the bundle
     * instead of compiling the value, and sends the result under the value, so
     * nothing is reported as JavaScript sent as a string.
     *
     * @param registration
     *            a registration returned by
     *            {@link #add(String, DomEventListener)}, not <code>null</code>
     * @param eventData
     *            the event data values, not <code>null</code> and not
     *            containing <code>null</code>
     */
    public static void addBundledEventData(DomListenerRegistration registration,
            List<String> eventData) {
        if (!(registration instanceof DomEventListenerWrapper wrapper)) {
            throw new IllegalArgumentException(
                    "The registration must be one returned by an ElementListenerMap");
        }
        wrapper.addBundledEventData(eventData);
    }

    /**
     * Gets the key that the client sends the value of the given event data
     * under. The key does not change after the event data is created.
     *
     * @param eventData
     *            the event data, not <code>null</code>
     * @return the key of the value, not <code>null</code>
     */
    public static String getEventDataKey(DomEventData<?> eventData) {
        Objects.requireNonNull(eventData, "Event data cannot be null");
        return SNIPPET_KEY_PREFIX + eventData.getSnippetKey();
    }

    /**
     * Gets the most permissive update mode for any event registration that is
     * configured to synchronize the given property.
     *
     * @param propertyName
     *            the property name to check, not <code>null</code>
     * @return the most permissive update mode, or <code>null</code> if
     *         synchronization is not configured for the given property
     * @since 1.3
     */
    public DisabledUpdateMode getPropertySynchronizationMode(
            String propertyName) {
        assert propertyName != null;

        if (listeners == null) {
            return null;
        }

        return listeners.values().stream().flatMap(List::stream)
                .filter(wrapper -> wrapper.isPropertySynchronized(propertyName))
                .map(wrapper -> wrapper.mode)
                .reduce(DisabledUpdateMode::mostPermissive).orElse(null);
    }

    /**
     * Returns {@code true} if any listener for the given property has
     * allowInert enabled. Note that this means that enabling allowInert for any
     * listener for a certain property will effectively allow it for all
     * listeners for said property.
     *
     * @param propertyName
     *            the property name to check, not <code>null</code>
     * @return {@code true} if allowInert is enabled for any listener for the
     *         given property, {@code false otherwise}
     * @since 24.6.8
     */
    public boolean hasAllowInertForProperty(String propertyName) {
        assert propertyName != null;

        if (listeners == null) {
            return false;
        }
        return listeners.values().stream().flatMap(List::stream)
                .filter(wrapper -> wrapper.isPropertySynchronized(propertyName))
                .anyMatch(wrapper -> wrapper.allowInert);
    }
}
