/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.data.renderer;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.data.provider.KeyMapper;
import com.vaadin.flow.data.renderer.BasicRenderer.SimpleValueRendering;
import com.vaadin.flow.dom.Element;

import elemental.json.Json;
import elemental.json.JsonObject;

public class NativeButtonRendererTest {

    @Test
    public void templateRenderered_containerIsDisabled_buttonIsDisabled() {
        NativeButtonRenderer<String> renderer = new NativeButtonRenderer<>(
                "Label");
        Element container = new Element("div");
        KeyMapper<String> keyMapper = new KeyMapper<>();
        Rendering<String> rendering = renderer.render(container, keyMapper);
        mockAttach(renderer, container, rendering, keyMapper);

        Assert.assertTrue("The DataGenerator should be present",
                rendering.getDataGenerator().isPresent());
        DataGenerator<String> dataGenerator = rendering.getDataGenerator()
                .get();

        JsonObject json = Json.createObject();
        dataGenerator.generateData("something", json);
        Assert.assertFalse("The button shouldn't be disabled", json.getBoolean(
                renderer.getTemplatePropertyName(rendering) + "_disabled"));

        mockDisabled(container);

        json = Json.createObject();
        dataGenerator.generateData("something", json);
        Assert.assertTrue("The button should be disabled", json.getBoolean(
                renderer.getTemplatePropertyName(rendering) + "_disabled"));
    }

    private void mockAttach(NativeButtonRenderer<String> renderer,
            Element container, Rendering<String> rendering,
            DataKeyMapper<String> keyMapper) {
        try {
            Method method = BasicRenderer.class.getDeclaredMethod(
                    "setupTemplateWhenAttached", Element.class,
                    SimpleValueRendering.class, DataKeyMapper.class);
            method.setAccessible(true);
            method.invoke(renderer, container, rendering, keyMapper);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mockDisabled(Element container) {
        container.setEnabled(false);
        container.getChildren().forEach(child -> child.setEnabled(false));
    }

}
