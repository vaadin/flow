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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.change.MapPutChange;
import com.vaadin.flow.change.NodeChange;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ElementDataTest extends AbstractNodeFeatureTest<ElementData> {
    private final ElementData elementData = new StateNode(
            Collections.singletonList(ElementData.class))
                    .getFeature(ElementData.class);

    @Test
    public void setGetTag() {
        Assert.assertNull("Tag should initially be null", elementData.getTag());

        elementData.setTag("myTag");

        Assert.assertEquals("myTag", elementData.getTag());
    }

    @Test
    public void setGetPayload() {
        Assert.assertNull("Tag should initially be null",
                elementData.getPayload());

        JsonObject object = Json.createObject();
        elementData.setPayload(object);

        Assert.assertEquals(object, elementData.getPayload());
    }

    @Test
    public void collectChanges_setTagOnly_onlyOneChange() {
        elementData.setTag("foo");
        List<NodeChange> changes = new ArrayList<>();
        elementData.collectChanges(changes::add);

        Assert.assertEquals(1, changes.size());
        Assert.assertTrue(changes.get(0) instanceof MapPutChange);
        MapPutChange change = (MapPutChange) changes.get(0);
        Assert.assertEquals(NodeProperties.TAG, change.getKey());
        Assert.assertEquals(elementData.getNode(), change.getNode());
        Assert.assertEquals("foo", change.getValue());
    }

    @Test
    public void collectChanges_setPayloadOnly_onlyOneChange() {
        JsonObject object = Json.createObject();
        elementData.setPayload(object);
        List<NodeChange> changes = new ArrayList<>();
        elementData.collectChanges(changes::add);

        Assert.assertEquals(1, changes.size());
        Assert.assertTrue(changes.get(0) instanceof MapPutChange);
        MapPutChange change = (MapPutChange) changes.get(0);
        Assert.assertEquals(NodeProperties.PAYLOAD, change.getKey());
        Assert.assertEquals(elementData.getNode(), change.getNode());
        Assert.assertEquals(object, change.getValue());
    }

    @Test
    public void collectChanges_setBothTagAndPayload_twoChanges() {
        JsonObject object = Json.createObject();
        elementData.setPayload(object);
        elementData.setTag("foo");

        List<NodeChange> changes = new ArrayList<>();
        elementData.collectChanges(changes::add);

        Assert.assertEquals(2, changes.size());
        Assert.assertTrue(changes.get(0) instanceof MapPutChange);
        Assert.assertTrue(changes.get(1) instanceof MapPutChange);

        MapPutChange change = (MapPutChange) changes.get(0);
        Assert.assertEquals(NodeProperties.TAG, change.getKey());
        Assert.assertEquals(elementData.getNode(), change.getNode());
        Assert.assertEquals("foo", change.getValue());

        change = (MapPutChange) changes.get(1);
        Assert.assertEquals(NodeProperties.PAYLOAD, change.getKey());
        Assert.assertEquals(elementData.getNode(), change.getNode());
        Assert.assertEquals(object, change.getValue());
    }
}
