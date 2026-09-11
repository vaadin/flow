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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.internal.ParameterizedConstantPoolKey;
import com.vaadin.flow.internal.StateNode;
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
     * Dummy filter string that always passes.
     */
    public static final String ALWAYS_TRUE_FILTER = "1";

    private static final EnumSet<DebouncePhase> NO_TIMEOUT_PHASES = EnumSet
            .of(DebouncePhase.LEADING);

    /**
     * Separator between the shared part and the capture specific part of the
     * key identifying an evaluated expression.
     */
    private static final char CAPTURE_KEY_SEPARATOR = '$';

    // Server-side only data
    private Map<String, List<DomEventListenerWrapper>> listeners;

    // Server-side only cache of the settings sent to the client, used for
    // interpreting the values that the client sends back
    private Map<String, EventSettings> settingsCache;

    /**
     * One JavaScript snippet that the client evaluates when the event occurs,
     * together with the values captured for it.
     * <p>
     * An entry is identified towards the client by a {@link #getKey() key}
     * derived from its contents rather than by its expression, so that two
     * entries that share an expression but use different captures stay apart,
     * and so that no JavaScript is sent back to the server when the event
     * occurs.
     */
    private static final class ExpressionEntry implements Serializable {
        private final String name;
        private final String expression;
        private final List<JsonNode> captures;

        private String sharedKey;
        private String key;

        private ExpressionEntry(String name, String expression,
                List<JsonNode> captures) {
            this.name = name;
            this.expression = expression;
            this.captures = captures;
        }

        /**
         * Creates an entry for an expression whose value is passed to the
         * server under the given name.
         */
        private static ExpressionEntry forEventData(String name,
                String expression, List<JsonNode> captures) {
            return new ExpressionEntry(Objects.requireNonNull(name), expression,
                    captures);
        }

        /**
         * Creates an entry for a filter expression. Filter values are only used
         * internally, so a filter entry has no name and is passed to the server
         * under its derived key.
         */
        private static ExpressionEntry forFilter(String expression,
                List<JsonNode> captures) {
            return new ExpressionEntry(null, expression, captures);
        }

        private boolean isFilter() {
            return name == null;
        }

        /**
         * Checks whether this is one of the pseudo expressions that the client
         * interprets based on the key instead of evaluating it as JavaScript.
         * Those keep using the pseudo expression as their key.
         */
        private boolean isPseudoExpression() {
            return expression
                    .startsWith(JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN)
                    || expression.startsWith(
                            JsonConstants.MAP_STATE_NODE_EVENT_DATA);
        }

        /**
         * Gets the key of the shared settings entry that this entry uses. All
         * entries that differ only by capture values share the same shared key,
         * which is what keeps the shared settings identical for e.g. two
         * elements that use the same expression with different captures.
         */
        private String getSharedKey() {
            if (sharedKey == null) {
                sharedKey = isPseudoExpression() ? expression
                        : hash((isFilter() ? "f" : "d") + captures.size() + '|'
                                + name + '|' + expression);
            }
            return sharedKey;
        }

        /**
         * Gets the key under which the client reports the value of this entry.
         */
        private String getKey() {
            if (key == null) {
                String sharedKey = getSharedKey();
                key = captures.isEmpty() ? sharedKey
                        : sharedKey + CAPTURE_KEY_SEPARATOR
                                + hash(capturesToJson().toString());
            }
            return key;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ExpressionEntry other
                    && getKey().equals(other.getKey());
        }

        @Override
        public int hashCode() {
            return getKey().hashCode();
        }

        private ArrayNode capturesToJson() {
            ArrayNode json = JacksonUtils.createArrayNode();
            captures.forEach(json::add);
            return json;
        }

        /**
         * Encodes the capture values of this entry for the client as
         * <code>[sharedKey, capture0, capture1, ...]</code>.
         */
        private ArrayNode toCaptureJson() {
            ArrayNode json = JacksonUtils.createArrayNode();
            json.add(getSharedKey());
            captures.forEach(json::add);
            return json;
        }
    }

    private static class ExpressionSettings implements Serializable {
        private final String expression;
        private final int captureCount;

        private Map<Integer, Set<DebouncePhase>> debounceSettings = new HashMap<>();

        private ExpressionSettings(String expression, int captureCount) {
            this.expression = expression;
            this.captureCount = captureCount;
        }

        public void addDebouncePhases(int timeout, Set<DebouncePhase> phases) {
            debounceSettings.merge(Integer.valueOf(timeout), phases,
                    (phases1, phases2) -> {
                        EnumSet<DebouncePhase> merge = EnumSet.copyOf(phases1);
                        merge.addAll(phases2);
                        return merge;
                    });
        }

        public JsonNode toJson() {
            ObjectNode json = JacksonUtils.createObjectNode();
            json.put(JsonConstants.EVENT_SETTINGS_EXPRESSION, expression);
            json.set(JsonConstants.EVENT_SETTINGS_DEBOUNCE, debounceToJson());
            if (captureCount > 0) {
                json.put(JsonConstants.EVENT_SETTINGS_CAPTURE_COUNT,
                        captureCount);
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
                        .map(entry -> Stream.concat(
                                Stream.of(JacksonUtils
                                        .createNode(entry.getKey().intValue())),
                                entry.getValue().stream()
                                        .map(DebouncePhase::getIdentifier)
                                        .map(JacksonUtils::createNode))
                                .collect(JacksonUtils.asArray()))
                        .collect(JacksonUtils.asArray());
            }
        }
    }

    /**
     * The settings for one event type, i.e. everything that the client needs to
     * know for evaluating the expressions of all listeners for that type.
     */
    private static class EventSettings implements Serializable {
        /**
         * Settings that are shared through the constant pool, keyed by
         * {@link ExpressionEntry#getSharedKey()}.
         */
        private final Map<String, ExpressionSettings> shared = new HashMap<>();

        /**
         * Capture values that are sent for this element only, keyed by
         * {@link ExpressionEntry#getKey()}.
         */
        private final Map<String, ExpressionEntry> captured = new LinkedHashMap<>();

        /**
         * The name to use for the value that the client reports for an entry,
         * keyed by {@link ExpressionEntry#getKey()}. Only contains entries for
         * which the key and the name differ.
         */
        private final Map<String, String> names = new HashMap<>();

        /**
         * The entry key that each name is used by, for detecting names that are
         * used by more than one entry.
         */
        private final Map<String, String> keysByName = new HashMap<>();

        /**
         * Names that are used by more than one entry, which means that only one
         * of the values is available to the listeners.
         */
        private final Set<String> conflictingNames = new LinkedHashSet<>();

        private ExpressionSettings add(ExpressionEntry entry) {
            ExpressionSettings settings = shared.computeIfAbsent(
                    entry.getSharedKey(),
                    key -> new ExpressionSettings(entry.expression,
                            entry.captures.size()));

            String key = entry.getKey();
            if (!entry.captures.isEmpty()) {
                captured.putIfAbsent(key, entry);
            }
            if (!entry.isFilter()) {
                String previousKey = keysByName.putIfAbsent(entry.name, key);
                if (previousKey != null && !previousKey.equals(key)) {
                    conflictingNames.add(entry.name);
                }
                if (!key.equals(entry.name)) {
                    names.put(key, entry.name);
                }
            }

            return settings;
        }
    }

    /**
     * Filter entry that always passes, used for making the client send events
     * also to listeners that have no filter of their own.
     */
    private static final ExpressionEntry ALWAYS_TRUE_ENTRY = ExpressionEntry
            .forFilter(ALWAYS_TRUE_FILTER, Collections.emptyList());

    /**
     * Settings used for event types that have no listeners.
     */
    private static final EventSettings EMPTY_SETTINGS = new EventSettings();

    /**
     * Creates a short key for the given content, using the same approach as
     * {@link ConstantPoolKey}.
     */
    private static String hash(String content) {
        byte[] digest = MessageDigestUtil.sha256(content);

        // Only use the first 64 bits to keep the key short
        ByteBuffer truncatedDigest = ByteBuffer.wrap(digest, 0, 8);

        return StandardCharsets.US_ASCII
                .decode(Base64.getEncoder().encode(truncatedDigest)).toString();
    }

    private static class DomEventListenerWrapper
            implements DomListenerRegistration {
        private final String type;
        private final DomEventListener origin;
        private final ElementListenerMap listenerMap;

        private DisabledUpdateMode mode = DisabledUpdateMode.ONLY_WHEN_ENABLED;
        private List<ExpressionEntry> eventDataEntries;
        private ExpressionEntry filter;

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

            return addEventData(eventData, eventData);
        }

        @Override
        public DomListenerRegistration addEventData(String name,
                String expression, Object... captures) {
            if (name == null) {
                throw new IllegalArgumentException(
                        "The event data name must not be null");
            }
            if (expression == null) {
                throw new IllegalArgumentException(
                        "The event data expression must not be null");
            }

            ExpressionEntry entry = ExpressionEntry.forEventData(name,
                    expression, encodeCaptures(captures));

            if (eventDataEntries == null) {
                // Don't use the no-args constructor that allocates for 10
                // entries
                eventDataEntries = new ArrayList<>(1);
            }
            if (!eventDataEntries.contains(entry)) {
                eventDataEntries.add(entry);
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
            return setFilter(filter, new Object[0]);
        }

        @Override
        public DomListenerRegistration setFilter(String filter,
                Object... captures) {
            this.filter = filter == null ? null
                    : ExpressionEntry.forFilter(filter,
                            encodeCaptures(captures));

            listenerMap.updateEventSettings(type);

            return this;
        }

        @Override
        public String getFilter() {
            return filter == null ? null : filter.expression;
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

            String key = filter.getKey();
            if (eventData.has(key)) {
                return eventData.get(key).booleanValue();
            } else {
                return false;
            }
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
            if (eventDataEntries == null) {
                return false;
            }
            String token = JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN
                    + propertyName;
            return eventDataEntries.stream()
                    .anyMatch(entry -> token.equals(entry.expression));
        }

        @Override
        public DomListenerRegistration allowInert() {
            allowInert = true;
            return this;
        }
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

    private EventSettings collectEventSettings(String eventType) {
        EventSettings settings = new EventSettings();
        boolean hasUnfilteredListener = false;
        boolean hasFilteredListener = false;

        Collection<DomEventListenerWrapper> wrappers = getWrappers(eventType);

        for (DomEventListenerWrapper wrapper : wrappers) {
            ExpressionEntry filter = wrapper.filter;

            // Process event data expressions, handling preventDefault and
            // stopPropagation specially
            if (wrapper.eventDataEntries != null) {
                for (ExpressionEntry entry : wrapper.eventDataEntries) {
                    settings.add(makeConditional(entry, filter));
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

                settings.add(filter).addDebouncePhases(timeout,
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
            settings.add(ALWAYS_TRUE_ENTRY).addDebouncePhases(0,
                    NO_TIMEOUT_PHASES);
        }

        if (!settings.conflictingNames.isEmpty()) {
            LoggerFactory.getLogger(ElementListenerMap.class).warn(
                    "The event data name(s) {} are used by multiple listeners "
                            + "for the {} event on the same element, but with "
                            + "different expressions or captures. Only one of "
                            + "the values will be available to the listeners. "
                            + "Use a unique name for each event data "
                            + "expression.",
                    settings.conflictingNames, eventType);
        }

        return settings;
    }

    /**
     * Makes the side effect of a <code>preventDefault</code> or
     * <code>stopPropagation</code> entry conditional on the filter of the same
     * listener, so that the side effect only happens for events that the
     * listener is actually interested in.
     * <p>
     * The filter is used as the left hand side of the combined expression so
     * that any captures of the filter keep their positions, which means that
     * only the filter may have captures.
     *
     * @param entry
     *            the event data entry to make conditional
     * @param filter
     *            the filter of the listener, or <code>null</code> if there is
     *            no filter
     * @return the entry to use, not <code>null</code>
     */
    private static ExpressionEntry makeConditional(ExpressionEntry entry,
            ExpressionEntry filter) {
        if (filter == null || filter.expression.isEmpty()
                || !("event.preventDefault()".equals(entry.expression)
                        || "event.stopPropagation()"
                                .equals(entry.expression))) {
            return entry;
        }

        return ExpressionEntry.forEventData(entry.name,
                "(" + filter.expression + ") && " + entry.expression,
                filter.captures);
    }

    private void updateEventSettings(String eventType) {
        EventSettings settings = collectEventSettings(eventType);

        cacheSettings(eventType, settings);

        ObjectNode sharedJson = JacksonUtils.createObject(settings.shared,
                ExpressionSettings::toJson);
        ConstantPoolKey constantPoolKey = new ConstantPoolKey(sharedJson);

        if (settings.captured.isEmpty()) {
            put(eventType, constantPoolKey);
        } else {
            ObjectNode captures = JacksonUtils.createObjectNode();
            settings.captured.forEach(
                    (key, entry) -> captures.set(key, entry.toCaptureJson()));

            put(eventType, new ParameterizedConstantPoolKey(constantPoolKey,
                    captures));
        }
    }

    /**
     * Gets the number of event types that settings are cached for. Package
     * private to facilitate unit testing.
     *
     * @return the number of cached event types
     */
    int getCachedSettingsCount() {
        return settingsCache == null ? 0 : settingsCache.size();
    }

    private void cacheSettings(String eventType, EventSettings settings) {
        if (settingsCache == null) {
            settingsCache = new HashMap<>();
        }
        settingsCache.put(eventType, settings);
    }

    private EventSettings getEventSettings(String eventType) {
        EventSettings cached = settingsCache == null ? null
                : settingsCache.get(eventType);
        if (cached != null) {
            return cached;
        }

        if (getWrappers(eventType).isEmpty()) {
            /*
             * The event type of an incoming event is defined by the client, so
             * caching for a type that has no listeners would let the client
             * grow this map without limit.
             */
            return EMPTY_SETTINGS;
        }

        EventSettings settings = collectEventSettings(eventType);
        cacheSettings(eventType, settings);
        return settings;
    }

    private static List<JsonNode> encodeCaptures(Object... captures) {
        if (captures == null || captures.length == 0) {
            return Collections.emptyList();
        }
        return Stream.of(captures).map(JacksonCodec::encodeWithTypeInfo)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Maps the keys that the client uses for reporting event data values to the
     * names that are used on the server. Values that the server has no name
     * for, such as filter results, are passed through as-is.
     *
     * @param eventType
     *            the type of the event, not <code>null</code>
     * @param eventData
     *            the event data as received from the client, not
     *            <code>null</code>
     * @return event data keyed by the names used on the server, not
     *         <code>null</code>
     */
    public JsonNode translateEventData(String eventType, JsonNode eventData) {
        assert eventType != null;
        assert eventData != null;

        Map<String, String> names = getEventSettings(eventType).names;
        if (names.isEmpty() || !eventData.isObject()) {
            return eventData;
        }

        ObjectNode translated = JacksonUtils.createObjectNode();
        eventData.properties().forEach(property -> translated.set(
                names.getOrDefault(property.getKey(), property.getKey()),
                property.getValue()));
        return translated;
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

                if (settingsCache != null) {
                    settingsCache.remove(eventType);
                }
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
        assert eventName != null;
        return collectEventSettings(eventName).shared.values().stream()
                .map(settings -> settings.expression)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Gets the key that the client uses when reporting the value of the filter
     * of the given registration. This method is currently only provided to
     * facilitate unit testing.
     *
     * @param registration
     *            the registration to get the filter key for, not
     *            <code>null</code>
     * @return the filter key, or <code>null</code> if the registration has no
     *         filter
     * @since 25.3
     */
    public static String getFilterKey(DomListenerRegistration registration) {
        assert registration != null;
        ExpressionEntry filter = ((DomEventListenerWrapper) registration).filter;
        return filter == null ? null : filter.getKey();
    }

    /**
     * Gets the key that the client uses when reporting the value of the event
     * data expression that is registered with the given name. This method is
     * currently only provided to facilitate unit testing.
     *
     * @param registration
     *            the registration to get the event data key for, not
     *            <code>null</code>
     * @param name
     *            the name of the event data, not <code>null</code>
     * @return the event data key, or <code>null</code> if the registration has
     *         no event data with the given name
     * @since 25.3
     */
    public static String getEventDataKey(DomListenerRegistration registration,
            String name) {
        assert registration != null;
        assert name != null;

        List<ExpressionEntry> entries = ((DomEventListenerWrapper) registration).eventDataEntries;
        if (entries == null) {
            return null;
        }
        return entries.stream().filter(entry -> name.equals(entry.name))
                .map(ExpressionEntry::getKey).findFirst().orElse(null);
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
