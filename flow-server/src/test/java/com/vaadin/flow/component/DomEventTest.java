/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.component;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;

public class DomEventTest {
    @DomEvent("event")
    public static class BareAnnotation extends ComponentEvent<Component> {
        public BareAnnotation(Component source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    @Test
    public void bareAnnotation() {
        assertSettings(BareAnnotation.class, null, 0);
    }

    @DomEvent(value = "event", filter = "a == b")
    public static class FilterEvent extends ComponentEvent<Component> {
        public FilterEvent(Component source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    @Test
    public void filter() {
        assertSettings(FilterEvent.class, "a == b", 0);
    }

    @DomEvent(value = "event", debounce = @DebounceSettings(timeout = 200, phases = {
            DebouncePhase.INTERMEDIATE, DebouncePhase.TRAILING }))
    public static class DebounceTimeoutPhasesEvent
            extends ComponentEvent<Component> {
        public DebounceTimeoutPhasesEvent(Component source,
                boolean fromClient) {
            super(source, fromClient);
        }
    }

    @Test
    public void debouncePhases() {
        assertSettings(DebounceTimeoutPhasesEvent.class,
                ElementListenerMap.ALWAYS_TRUE_FILTER, 200,
                DebouncePhase.INTERMEDIATE, DebouncePhase.TRAILING);
    }

    @DomEvent(value = "event", debounce = @DebounceSettings(timeout = 200, phases = {}))
    public static class DebounceEmptyPhasesEvent
            extends ComponentEvent<Component> {
        public DebounceEmptyPhasesEvent(Component source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void emptyPhases() {
        getEventSettings(DebounceEmptyPhasesEvent.class);
    }

    @DomEvent(value = "event", filter = "filter(event)", debounce = @DebounceSettings(timeout = 300, phases = DebouncePhase.TRAILING))
    public static class DebounceFilterEvent extends ComponentEvent<Component> {
        public DebounceFilterEvent(Component source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    @Test
    public void debounceFilter() {
        assertSettings(DebounceFilterEvent.class, "filter(event)", 300,
                DebouncePhase.TRAILING);
    }

    private <T extends ComponentEvent<Component>> void assertSettings(
            Class<T> eventType, String expectedFilter, int expectedTimeout,
            DebouncePhase... expectedPhases) {
        JsonObject settings = getEventSettings(eventType);

        if (expectedFilter == null) {
            Assert.assertArrayEquals(new String[0], settings.keys());
            return;
        }

        Assert.assertArrayEquals(new String[] { expectedFilter },
                settings.keys());

        if (expectedTimeout == 0 && expectedPhases.length == 0) {
            Assert.assertEquals(
                    "There should be a boolean instead of empty phase list",
                    JsonType.BOOLEAN, settings.get(expectedFilter).getType());
            boolean isFilter = settings.getBoolean(expectedFilter);
            Assert.assertTrue("Expression should be used as a filter",
                    isFilter);
            return;
        }

        JsonArray filterSettings = settings.getArray(expectedFilter);

        Assert.assertEquals(1, filterSettings.length());

        JsonArray filterSetting = filterSettings.getArray(0);

        Assert.assertEquals("Debunce timeout should be as expected",
                expectedTimeout, (int) filterSetting.getNumber(0));

        Assert.assertEquals("Number of phases should be as expected",
                expectedPhases.length, filterSetting.length() - 1);

        for (int i = 0; i < expectedPhases.length; i++) {
            String expectedIdentifier = expectedPhases[i].getIdentifier();
            Assert.assertEquals(expectedIdentifier,
                    filterSetting.getString(i + 1));
        }
    }

    private void assertFilter(String expectedFilter,
            JsonObject filterSettings) {
    }

    private <T extends ComponentEvent<Component>> JsonObject getEventSettings(
            Class<T> eventType) {
        Component component = new Component(new Element("element")) {
        };
        component.addListener(eventType, e -> {
        });

        ElementListenerMap elementListenerMap = component.getElement().getNode()
                .getFeature(ElementListenerMap.class);

        List<NodeChange> changes = new ArrayList<>();
        elementListenerMap.collectChanges(changes::add);

        Assert.assertEquals(1, changes.size());
        MapPutChange change = (MapPutChange) changes.get(0);
        Assert.assertEquals("event", change.getKey());

        ConstantPoolKey value = (ConstantPoolKey) change.getValue();
        JsonObject constantPoolUpdate = Json.createObject();
        value.export(constantPoolUpdate);

        String[] keys = constantPoolUpdate.keys();
        Assert.assertEquals(1, keys.length);
        JsonObject eventSettings = constantPoolUpdate.getObject(keys[0]);

        return eventSettings;
    }
}
