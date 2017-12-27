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
package com.vaadin.flow.template.angular;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ModelMap;
import com.vaadin.flow.template.angular.BindingValueProvider;
import com.vaadin.flow.template.angular.ModelValueBindingProvider;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * @author Vaadin Ltd
 *
 */
public class ModelValueBindingTest {

    public void getValue() {
        ModelValueBindingProvider binding = new ModelValueBindingProvider(
                "bar");
        StateNode node = new StateNode(ModelMap.class);

        ModelMap.get(node).setValue("bar", "someValue");

        Assert.assertEquals("someValue", binding.getValue(node));
    }

    @Test
    public void toJson() {
        ModelValueBindingProvider binding = new ModelValueBindingProvider(
                "bar");
        JsonValue json = binding.toJson();

        Assert.assertTrue(json instanceof JsonObject);
        JsonObject object = (JsonObject) json;

        Assert.assertEquals(ModelValueBindingProvider.TYPE,
                object.get(BindingValueProvider.TYPE_PROPERTY).asString());
        Assert.assertEquals("bar",
                object.get(BindingValueProvider.VALUE_PROPERTY).asString());
    }

}
