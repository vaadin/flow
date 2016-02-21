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
package com.vaadin.client.hummingbird;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import elemental.events.EventRemover;

public class StateTreeTest {
    StateTree tree = new StateTree(null);
    StateNode node = new StateNode(5, tree);

    @Test
    public void testIdMappings() {

        StateNode nullNode = tree.getNode(node.getId());
        Assert.assertNull(nullNode);

        tree.registerNode(node);

        StateNode foundNode = tree.getNode(node.getId());
        Assert.assertSame(node, foundNode);
    }

    @Test(expected = AssertionError.class)
    public void testRegisterExistingThrows() {
        tree.registerNode(node);
        tree.registerNode(node);
    }

    @Test(expected = AssertionError.class)
    public void testRegisterNullThrows() {
        tree.registerNode(null);
    }

    @Test
    public void testNodeUnregister() {
        tree.registerNode(node);

        Assert.assertFalse(node.isUnregistered());

        AtomicReference<NodeUnregisterEvent> lastEvent = new AtomicReference<>();
        node.addUnregisterListener(new NodeUnregisterListener() {
            @Override
            public void onUnregister(NodeUnregisterEvent event) {
                Assert.assertNull("Unexpected event fired", lastEvent.get());
                lastEvent.set(event);
            }
        });

        tree.unregisterNode(node);

        NodeUnregisterEvent event = lastEvent.get();
        Assert.assertSame(node, event.getNode());

        Assert.assertTrue(node.isUnregistered());
        Assert.assertNull(tree.getNode(node.getId()));
    }

    @Test
    public void testRemoveUnregisterListener() {
        tree.registerNode(node);

        EventRemover remove = node
                .addUnregisterListener(e -> Assert.fail("Should never run"));

        remove.remove();

        tree.unregisterNode(node);
    }

    @Test(expected = AssertionError.class)
    public void unregisterNonRegisteredNode() {
        tree.unregisterNode(node);
    }

    @Test
    public void unregisterUnregisteredNode() {
        tree.registerNode(node);
        tree.unregisterNode(node);
        // Should run fine up to this point

        try {
            tree.unregisterNode(node);
            Assert.fail("Should have thrown");
        } catch (AssertionError expected) {
            // All is fine
        }
    }

}
