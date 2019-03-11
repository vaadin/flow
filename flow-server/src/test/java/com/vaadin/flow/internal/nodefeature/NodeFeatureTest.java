/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateNodeTest;
import com.vaadin.flow.internal.nodefeature.PushConfigurationMap.PushConfigurationParametersMap;

public class NodeFeatureTest {
    private static abstract class UnregisteredNodeFeature extends NodeFeature {
        public UnregisteredNodeFeature(StateNode node) {
            super(node);
        }
    }

    @Test(expected = AssertionError.class)
    public void testCreateNullTypeThrows() {
        NodeFeatureRegistry.create(null, StateNodeTest.createEmptyNode());
    }

    @Test(expected = AssertionError.class)
    public void testCreateNullNodeThrows() {
        NodeFeatureRegistry.create(ElementData.class, null);
    }

    @Test(expected = AssertionError.class)
    public void testCreateUnknownFeatureThrows() {
        NodeFeatureRegistry.create(UnregisteredNodeFeature.class,
                StateNodeTest.createEmptyNode());
    }

    private static Map<Class<? extends NodeFeature>, Integer> buildExpectedIdMap() {
        Map<Class<? extends NodeFeature>, Integer> expectedIds = new HashMap<>();

        expectedIds.put(ElementData.class, NodeFeatures.ELEMENT_DATA);
        expectedIds.put(ElementPropertyMap.class,
                NodeFeatures.ELEMENT_PROPERTIES);
        expectedIds.put(ElementAttributeMap.class,
                NodeFeatures.ELEMENT_ATTRIBUTES);
        expectedIds.put(ElementChildrenList.class,
                NodeFeatures.ELEMENT_CHILDREN);
        expectedIds.put(ElementListenerMap.class,
                NodeFeatures.ELEMENT_LISTENERS);
        expectedIds.put(PushConfigurationMap.class,
                NodeFeatures.UI_PUSHCONFIGURATION);
        expectedIds.put(PushConfigurationParametersMap.class,
                NodeFeatures.UI_PUSHCONFIGURATION_PARAMETERS);
        expectedIds.put(TextNodeMap.class, NodeFeatures.TEXT_NODE);
        expectedIds.put(PollConfigurationMap.class,
                NodeFeatures.POLL_CONFIGURATION);
        expectedIds.put(ReconnectDialogConfigurationMap.class,
                NodeFeatures.RECONNECT_DIALOG_CONFIGURATION);
        expectedIds.put(LoadingIndicatorConfigurationMap.class,
                NodeFeatures.LOADING_INDICATOR_CONFIGURATION);
        expectedIds.put(ElementClassList.class, NodeFeatures.CLASS_LIST);
        expectedIds.put(ElementStylePropertyMap.class,
                NodeFeatures.ELEMENT_STYLE_PROPERTIES);
        expectedIds.put(SynchronizedPropertiesList.class,
                NodeFeatures.SYNCHRONIZED_PROPERTIES);
        expectedIds.put(SynchronizedPropertyEventsList.class,
                NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);
        expectedIds.put(ComponentMapping.class, NodeFeatures.COMPONENT_MAPPING);
        expectedIds.put(ModelList.class, NodeFeatures.TEMPLATE_MODELLIST);
        expectedIds.put(PolymerServerEventHandlers.class,
                NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS);
        expectedIds.put(PolymerEventListenerMap.class,
                NodeFeatures.POLYMER_EVENT_LISTENERS);
        expectedIds.put(ClientCallableHandlers.class,
                NodeFeatures.CLIENT_DELEGATE_HANDLERS);
        expectedIds.put(ShadowRootData.class, NodeFeatures.SHADOW_ROOT_DATA);
        expectedIds.put(ShadowRootHost.class, NodeFeatures.SHADOW_ROOT_HOST);
        expectedIds.put(AttachExistingElementFeature.class,
                NodeFeatures.ATTACH_EXISTING_ELEMENT);
        expectedIds.put(BasicTypeValue.class, NodeFeatures.BASIC_TYPE_VALUE);
        expectedIds.put(VirtualChildrenList.class,
                NodeFeatures.VIRTUAL_CHILDREN);
        expectedIds.put(ReturnChannelMap.class,
                NodeFeatures.RETURN_CHANNEL_MAP);

        return expectedIds;
    }

    @Test
    public void testGetIdValues() {
        // Verifies that the ids are the same as on the client side
        Map<Class<? extends NodeFeature>, Integer> expectedIds = buildExpectedIdMap();

        Assert.assertEquals("The number of expected features is not up to date",
                expectedIds.size(), NodeFeatureRegistry.nodeFeatures.size());

        expectedIds.forEach((type, expectedId) -> {
            Assert.assertEquals("Unexpected id for " + type.getName(),
                    expectedId.intValue(), NodeFeatureRegistry.getId(type));
        });
    }

    @Test
    public void testGetById() {
        Map<Class<? extends NodeFeature>, Integer> expectedIds = buildExpectedIdMap();

        expectedIds.forEach((expectedType, id) -> {
            Assert.assertEquals("Unexpected type for id " + id, expectedType,
                    NodeFeatureRegistry.getFeature(id));
        });
    }

    @Test
    public void priorityOrder() {
        List<Class<? extends NodeFeature>> priorityOrder = buildExpectedIdMap()
                .keySet().stream()
                .sorted(NodeFeatureRegistry.PRIORITY_COMPARATOR)
                .collect(Collectors.toList());

        List<Class<? extends NodeFeature>> expectedOrder = Arrays.asList(
                /* Primary features */
                ElementData.class, TextNodeMap.class, ModelList.class,
                BasicTypeValue.class,

                /* Common element features */
                ElementChildrenList.class, ElementPropertyMap.class,

                /* Component mapped features */
                ComponentMapping.class, ClientCallableHandlers.class,

                /* Supplementary element stuff */
                ElementClassList.class, ElementAttributeMap.class,
                ElementListenerMap.class, SynchronizedPropertiesList.class,
                SynchronizedPropertyEventsList.class, VirtualChildrenList.class,

                /* Supplementary generic stuff */
                ReturnChannelMap.class,

                /* PolymerTemplate stuff */
                PolymerEventListenerMap.class, PolymerServerEventHandlers.class,

                /* Rarely used element stuff */
                ElementStylePropertyMap.class, ShadowRootData.class,
                ShadowRootHost.class, AttachExistingElementFeature.class,

                /* Only used for the root node */
                PushConfigurationMap.class,
                PushConfigurationParametersMap.class,
                LoadingIndicatorConfigurationMap.class,
                PollConfigurationMap.class,
                ReconnectDialogConfigurationMap.class);

        Assert.assertEquals(expectedOrder.size(), priorityOrder.size());

        for (int i = 0; i < priorityOrder.size(); i++) {
            if (priorityOrder.get(i) != expectedOrder.get(i)) {
                Assert.fail("Invalid priority ordering at index " + i
                        + ". Expected " + expectedOrder.get(i).getSimpleName()
                        + " but got " + priorityOrder.get(i).getSimpleName());
            }
        }
    }

}
