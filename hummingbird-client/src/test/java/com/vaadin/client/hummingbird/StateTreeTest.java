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

import org.junit.Assert;
import org.junit.Test;

public class StateTreeTest {
    StateTree tree = new StateTree();
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

}
