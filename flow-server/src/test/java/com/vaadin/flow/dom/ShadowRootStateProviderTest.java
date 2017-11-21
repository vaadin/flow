/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.impl.ShadowRootStateProvider;
import com.vaadin.flow.nodefeature.ShadowRootData;

public class ShadowRootStateProviderTest {

    @Test
    public void supportsSelfCreatedNode() {
        ShadowRootStateProvider provider = ShadowRootStateProvider.get();
        StateNode node = new StateNode(ShadowRootData.class);
        StateNode shadowRoot = provider.createShadowRootNode(node);
        assertTrue(provider.supports(shadowRoot));
    }

    @Test
    public void doesNotSupportEmptyNode() {
        ShadowRootStateProvider provider = ShadowRootStateProvider.get();
        assertFalse(provider.supports(new StateNode()));
    }

    @Test
    public void createShadowRootNode_originalNodeIsInitialized() {
        ShadowRootStateProvider provider = ShadowRootStateProvider.get();
        StateNode node = new StateNode(ShadowRootData.class);
        StateNode shadowRoot = provider.createShadowRootNode(node);
        assertEquals(shadowRoot,
                node.getFeature(ShadowRootData.class).getShadowRoot());
    }

    @Test
    public void getParent_parentIsHostElement() {
        ShadowRootStateProvider provider = ShadowRootStateProvider.get();
        StateNode node = new StateNode(ShadowRootData.class);
        StateNode shadowRoot = provider.createShadowRootNode(node);
        assertEquals(node, shadowRoot.getParent());
    }

}
