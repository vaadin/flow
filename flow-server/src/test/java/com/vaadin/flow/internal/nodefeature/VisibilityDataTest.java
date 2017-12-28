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
package com.vaadin.flow.internal.nodefeature;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.internal.StateNode;

public class VisibilityDataTest {

    @Test
    public void setVisible() {
        StateNode node = new StateNode(VisibilityData.class);
        VisibilityData data = node.getFeature(VisibilityData.class);

        Assert.assertNull(data.getValue());
        Assert.assertTrue(data.isVisible());

        data.setValue(true);
        Assert.assertTrue(data.isVisible());

        data.setValue(false);
        Assert.assertFalse(data.isVisible());
    }

    @Test
    public void allowsChanges_delegateToIsVisible() {
        VisibilityData data = Mockito.mock(VisibilityData.class);

        Mockito.doCallRealMethod().when(data).allowsChanges();

        Mockito.when(data.isVisible()).thenReturn(true);

        Assert.assertTrue(data.allowsChanges());

        Mockito.when(data.isVisible()).thenReturn(false);

        Assert.assertFalse(data.allowsChanges());
    }
}
