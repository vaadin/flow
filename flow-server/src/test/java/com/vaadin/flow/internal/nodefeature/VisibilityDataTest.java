/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.internal.StateNode;

public class VisibilityDataTest {

    @Test
    public void setVisible() {
        StateNode node = new StateNode(ElementData.class);
        ElementData data = node.getFeature(ElementData.class);

        Assert.assertNull(data.get(NodeProperties.VISIBLE));
        Assert.assertTrue(data.isVisible());

        data.put(NodeProperties.VISIBLE, true);
        Assert.assertTrue(data.isVisible());

        data.put(NodeProperties.VISIBLE, false);
        Assert.assertFalse(data.isVisible());
    }

    @Test
    public void allowsChanges_delegateToIsVisible() {
        ElementData data = Mockito.mock(ElementData.class);

        Mockito.doCallRealMethod().when(data).allowsChanges();

        Mockito.when(data.isVisible()).thenReturn(true);

        Assert.assertTrue(data.allowsChanges());

        Mockito.when(data.isVisible()).thenReturn(false);

        Assert.assertFalse(data.allowsChanges());
    }
}
