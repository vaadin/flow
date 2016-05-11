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
package com.vaadin.client.hummingbird.binding;

import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.dom.Node;

/**
 * @author Vaadin Ltd
 *
 */
public interface BindingStrategy<T extends Node> {

    T create(StateNode node);

    boolean isAppliable(StateNode node);

    void bind(StateNode stateNode, T htmlNode, BinderContext factory);

    default String getTag(StateNode node) {
        return (String) node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeFeatures.TAG).getValue();
    }
}
