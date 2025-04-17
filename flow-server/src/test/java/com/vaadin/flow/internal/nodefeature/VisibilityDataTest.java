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
