/*
 * Copyright 2000-2016 Vaadin Ltd.
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

package com.vaadin.hummingbird.namespace;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StateNodeTest;
import com.vaadin.hummingbird.dom.impl.TextNodeNamespace;
import com.vaadin.hummingbird.namespace.PushConfigurationMap.PushConfigurationParametersMap;
import com.vaadin.hummingbird.shared.Namespaces;

public class NamespaceTest {
    private static abstract class UnregisteredNamespace extends Namespace {
        public UnregisteredNamespace(StateNode node) {
            super(node);
        }
    }

    @Test(expected = AssertionError.class)
    public void testCreateNullTypeThrows() {
        NamespaceRegistry.create(null, StateNodeTest.createEmptyNode());
    }

    @Test(expected = AssertionError.class)
    public void testCreateNullNodeThrows() {
        NamespaceRegistry.create(ElementDataNamespace.class, null);
    }

    @Test(expected = AssertionError.class)
    public void testCreateUnknownNamespaceThrows() {
        NamespaceRegistry.create(UnregisteredNamespace.class,
                StateNodeTest.createEmptyNode());
    }

    @Test
    public void testGetIdValues() {
        // Verifies that the ids are the same as on the client side
        Map<Class<? extends Namespace>, Integer> expectedIds = new HashMap<>();

        expectedIds.put(ElementDataNamespace.class, Namespaces.ELEMENT_DATA);
        expectedIds.put(ElementPropertyNamespace.class,
                Namespaces.ELEMENT_PROPERTIES);
        expectedIds.put(ElementAttributeNamespace.class,
                Namespaces.ELEMENT_ATTRIBUTES);
        expectedIds.put(ElementChildrenNamespace.class,
                Namespaces.ELEMENT_CHILDREN);
        expectedIds.put(ElementListenersNamespace.class,
                Namespaces.ELEMENT_LISTENERS);
        expectedIds.put(PushConfigurationMap.class,
                Namespaces.UI_PUSHCONFIGURATION);
        expectedIds.put(PushConfigurationParametersMap.class,
                Namespaces.UI_PUSHCONFIGURATION_PARAMETERS);
        expectedIds.put(TextNodeNamespace.class, Namespaces.TEXT_NODE);
        expectedIds.put(PollConfigurationNamespace.class,
                Namespaces.POLL_CONFIGURATION);
        expectedIds.put(ReconnectDialogConfigurationNamespace.class,
                Namespaces.RECONNECT_DIALOG_CONFIGURATION);
        expectedIds.put(LoadingIndicatorConfigurationNamespace.class,
                Namespaces.LOADING_INDICATOR_CONFIGURATION);

        Assert.assertEquals(
                "The number of expected namespaces is not up to date",
                expectedIds.size(), NamespaceRegistry.namespaces.size());

        expectedIds.forEach((type, expectedId) -> {
            Assert.assertEquals("Unexpected id for " + type.getName(),
                    expectedId.intValue(), NamespaceRegistry.getId(type));
        });
    }
}
