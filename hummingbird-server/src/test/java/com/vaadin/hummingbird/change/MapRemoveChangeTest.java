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

package com.vaadin.hummingbird.change;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.namespace.AbstractNamespaceTest;
import com.vaadin.hummingbird.namespace.ElementPropertiesNamespace;
import com.vaadin.hummingbird.namespace.MapNamespace;
import com.vaadin.hummingbird.namespace.NamespaceRegistry;
import com.vaadin.shared.JsonConstants;

import elemental.json.JsonObject;

public class MapRemoveChangeTest {
    private MapNamespace namespace = AbstractNamespaceTest
            .createNamespace(ElementPropertiesNamespace.class);

    @Test
    public void testJson() {
        MapRemoveChange change = new MapRemoveChange(namespace, "some");

        JsonObject json = change.toJson();

        Assert.assertEquals(change.getNode().getId(),
                (int) json.getNumber(JsonConstants.CHANGE_NODE));
        Assert.assertEquals(NamespaceRegistry.getId(namespace.getClass()),
                (int) json.getNumber(JsonConstants.CHANGE_NAMESPACE));
        Assert.assertEquals(JsonConstants.CHANGE_TYPE_REMOVE,
                json.getString(JsonConstants.CHANGE_TYPE));
        Assert.assertEquals("some",
                json.getString(JsonConstants.CHANGE_MAP_KEY));
    }

}
