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
package com.vaadin.hummingbird.uitest.ui;

import java.util.Arrays;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.namespace.ElementChildrenNamespace;
import com.vaadin.hummingbird.namespace.ElementDataNamespace;
import com.vaadin.hummingbird.namespace.ElementPropertiesNamespace;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class StateTreeUI extends UI {
    @Override
    protected void init(VaadinRequest request) {
        StateNode rootNode = getStateTree().getRootNode();

        rootNode.getNamespace(ElementDataNamespace.class).setTag("div");
        rootNode.getNamespace(ElementPropertiesNamespace.class)
                .setProperty("foo", "bar");

        StateNode childNode = new StateNode(ElementDataNamespace.class);
        childNode.getNamespace(ElementDataNamespace.class).setTag("span");
        rootNode.getNamespace(ElementChildrenNamespace.class).add(0, childNode);
    }
}
