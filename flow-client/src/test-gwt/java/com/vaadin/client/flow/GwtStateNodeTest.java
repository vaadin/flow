/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.Registry;

public class GwtStateNodeTest extends ClientEngineTestBase {

    private StateTree tree;

    private static class TestData {
    }

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();

        tree = new StateTree(new Registry());
    }

    public void testNodeData_getNodeData_sameInstance() {
        StateNode node = new StateNode(1, tree);
        TestData data = new TestData();
        node.setNodeData(data);
        assertSame(data, node.getNodeData(TestData.class));
    }

}
