/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.impl.ShadowRootStateProvider;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ShadowRootData;

public class ShadowRootStateProviderTest {

    @Test
    public void supportsSelfCreatedNode() {
        ShadowRootStateProvider provider = ShadowRootStateProvider.get();
        StateNode node = new StateNode(ShadowRootData.class);
        StateNode shadowRoot = provider.createShadowRootNode(node);
        Assert.assertTrue(provider.supports(shadowRoot));
    }

    @Test
    public void doesNotSupportEmptyNode() {
        ShadowRootStateProvider provider = ShadowRootStateProvider.get();
        Assert.assertFalse(provider.supports(new StateNode()));
    }

    @Test
    public void createShadowRootNode_originalNodeIsInitialized() {
        ShadowRootStateProvider provider = ShadowRootStateProvider.get();
        StateNode node = new StateNode(ShadowRootData.class);
        StateNode shadowRoot = provider.createShadowRootNode(node);
        Assert.assertEquals(shadowRoot,
                node.getFeature(ShadowRootData.class).getShadowRoot());
    }

    @Test
    public void getParent_parentIsHostElement() {
        ShadowRootStateProvider provider = ShadowRootStateProvider.get();
        StateNode node = new StateNode(ShadowRootData.class);
        StateNode shadowRoot = provider.createShadowRootNode(node);
        Assert.assertEquals(node, shadowRoot.getParent());
    }

}
