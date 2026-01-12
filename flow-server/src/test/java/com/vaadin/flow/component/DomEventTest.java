/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;

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
        JsonNode settings = getEventSettings(eventType);

        if (expectedFilter == null) {
            Assert.assertEquals(new ArrayList<String>(0),
                    JacksonUtils.getKeys(settings));
            return;
        }

        Assert.assertEquals(new ArrayList<String>() {
            {
                add(expectedFilter);
            }
        }, JacksonUtils.getKeys(settings));

        if (expectedTimeout == 0 && expectedPhases.length == 0) {
            Assert.assertEquals(
                    "There should be a boolean instead of empty phase list",
                    JsonNodeType.BOOLEAN,
                    settings.get(expectedFilter).getNodeType());
            boolean isFilter = settings.get(expectedFilter).booleanValue();
            Assert.assertTrue("Expression should be used as a filter",
                    isFilter);
            return;
        }

        ArrayNode filterSettings = (ArrayNode) settings.get(expectedFilter);

        Assert.assertEquals(1, filterSettings.size());

        ArrayNode filterSetting = (ArrayNode) filterSettings.get(0);

        Assert.assertEquals("Debunce timeout should be as expected",
                expectedTimeout, filterSetting.get(0).intValue());

        Assert.assertEquals("Number of phases should be as expected",
                expectedPhases.length, filterSetting.size() - 1);

        for (int i = 0; i < expectedPhases.length; i++) {
            String expectedIdentifier = expectedPhases[i].getIdentifier();
            Assert.assertEquals(expectedIdentifier,
                    filterSetting.get(i + 1).textValue());
        }
    }

    private <T extends ComponentEvent<Component>> JsonNode getEventSettings(
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
        ObjectNode constantPoolUpdate = JacksonUtils.createObjectNode();
        value.export(constantPoolUpdate);

        List<String> keys = JacksonUtils.getKeys(constantPoolUpdate);
        Assert.assertEquals(1, keys.size());

        return constantPoolUpdate.get(keys.get(0));
    }
}
