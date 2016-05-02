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
package com.vaadin.hummingbird.template;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelMap;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * @author Vaadin Ltd
 *
 */
public class ModelValueBindingTest {

    @Test
    public void getValue() {
        ModelValueBinding binding = new ModelValueBinding("type", "foo");
        StateNode node = new StateNode(ModelMap.class);

        node.getFeature(ModelMap.class).setValue("foo", "bar");

        Assert.assertEquals("bar", binding.getValue(node));
    }

    @Test
    public void toJson() {
        ModelValueBinding binding = new ModelValueBinding("type", "foo");
        JsonValue json = binding.toJson();

        Assert.assertTrue(json instanceof JsonObject);
        JsonObject object = (JsonObject) json;

        Assert.assertEquals("type",
                object.get(TemplateBinding.TYPE_PROPERTY).asString());
        Assert.assertEquals("foo",
                object.get(TemplateBinding.VALUE_PROPERTY).asString());
    }

}
