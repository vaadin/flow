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

package com.vaadin.client.hummingbird;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.Registry;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.binding.Binder;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.client.Browser;
import elemental.dom.Element;

/**
 * @author Vaadin Ltd.
 */
public class GwtPolymerBindingsTest extends ClientEngineTestBase {
    private StateNode node;
    private Element element;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        Reactive.reset();

        node = new StateNode(0, new StateTree(new Registry()));
        // populate "element data" feature to be able to bind node as a plain
        // element
        node.getMap(NodeFeatures.ELEMENT_DATA);

        element = Browser.getDocument().createElement("div");
        Binder.bind(node, element);
    }

    public void testPropertyAdded() {
        String propertyName = "black";
        String propertyValue = "coffee";

        setModelProperty(node, propertyName, propertyValue);

        assertEquals(propertyValue,
                WidgetUtil.getJsProperty(element, propertyName));
    }

    public void testPropertyUpdated() {
        String propertyName = "black";
        String propertyValue = "coffee";
        setModelProperty(node, propertyName, propertyValue);
        String newValue = "tea";

        setModelProperty(node, propertyName, newValue);

        assertEquals(newValue, WidgetUtil.getJsProperty(element, propertyName));
    }

    private static void setModelProperty(StateNode stateNode, String name,
            String value) {
        stateNode.getMap(NodeFeatures.TEMPLATE_MODELMAP).getProperty(name)
                .setValue(value);
        Reactive.flush();
    }
}
