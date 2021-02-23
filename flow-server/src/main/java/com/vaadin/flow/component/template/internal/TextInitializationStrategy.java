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
package com.vaadin.flow.component.template.internal;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.AbstractListChange;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;

/**
 * Initializes Element via setting a text value.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
class TextInitializationStrategy implements ElementInitializationStrategy, Serializable {

    @Override
    public void initialize(Element element, String name, String value) {
        // Set the text only for the server side, do not send the change to the client
        // so that it does not overwrite what is in the DOM
        ElementChildrenList children = element.getNode().getFeature(ElementChildrenList.class);
        List<AbstractListChange<StateNode>> changeTracker = children.getChangeTracker();
        int changesBefore = changeTracker.size();
        element.setText(value);
        while (changeTracker.size() > changesBefore) {
            changeTracker.remove(changeTracker.size() - 1);
        }
    }

}
