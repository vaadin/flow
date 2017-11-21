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
package com.vaadin.flow.nodefeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.Element;

public class AttachTemplateChildFeatureTest {

    @Test
    public void register_dataIsAvailaleByNode() {
        StateNode node = new StateNode();
        AttachTemplateChildFeature feature = new AttachTemplateChildFeature(
                node);

        StateNode child = Mockito.mock(StateNode.class);

        Element parent = Mockito.mock(Element.class);
        feature.register(parent, child);

        Mockito.verify(child).setParent(node);

        assertEquals(parent, feature.getParent(child));
    }

    @Test
    public void unregister_dataIsNotAvailaleByNode() {
        StateNode node = new StateNode();
        AttachTemplateChildFeature feature = new AttachTemplateChildFeature(
                node);

        StateNode child = Mockito.mock(StateNode.class);

        Element parent = Mockito.mock(Element.class);
        feature.register(parent, child);

        feature.unregister(child);

        assertNull(feature.getParent(child));
    }
}
