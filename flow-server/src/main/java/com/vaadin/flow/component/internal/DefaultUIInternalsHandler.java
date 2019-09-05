/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.internal;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;

/**
 * The default implementation of UIInternalsHandler which will update the UI
 * normally.
 */
public class DefaultUIInternalsHandler implements UIInternalsHandler {
    @Override
    public void updateRoot(UI ui, HasElement oldRoot, HasElement newRoot) {
        Element uiElement = ui.getElement();
        Element rootElement = newRoot.getElement();

        if (!uiElement.equals(rootElement.getParent())) {
            if (oldRoot != null) {
                oldRoot.getElement().removeFromParent();
            }
            rootElement.removeFromParent();
            uiElement.appendChild(rootElement);
        }
    }

    @Override
    public void moveToNewUI(UI oldUI, UI newUI) {
        final List<Element> uiChildren = oldUI.getElement().getChildren()
                .collect(Collectors.toList());
        uiChildren.forEach(element -> {
            element.removeFromTree();
            newUI.getElement().appendChild(element);
        });
    }
}
