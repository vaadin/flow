/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;

/**
 * The implementation of this interface is responsible for updating the UI with
 * given content.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public interface UIInternalUpdater extends Serializable {

    /**
     * Update root element of the given UI.
     *
     * @param ui
     *            the UI to be updated
     * @param oldRoot
     *            the old root to be removed
     * @param newRoot
     *            the new root to be added
     */
    default void updateRoot(UI ui, HasElement oldRoot, HasElement newRoot) {

        Element wrapperElement = ui.getWrapperElement();
        // server-side routing
        if (wrapperElement == null) {
            Element uiElement = ui.getElement();
            Element rootElement = newRoot.getElement();

            if (!uiElement.equals(rootElement.getParent())) {
                if (oldRoot != null) {
                    oldRoot.getElement().removeFromParent();
                }
                rootElement.removeFromParent();
                uiElement.appendChild(rootElement);
            }
        } else {
            // client-side routing
            Element rootElement = newRoot.getElement();
            if (newRoot instanceof UI.ClientViewPlaceholder) {
                // only need to remove all children when newRoot is a
                // placeholder
                wrapperElement.removeAllChildren();
            } else if (!wrapperElement.equals(rootElement.getParent())) {
                if (oldRoot != null) {
                    oldRoot.getElement().removeFromParent();
                }
                rootElement.removeFromParent();
                wrapperElement.appendChild(rootElement);
            }
        }
    }

    /**
     * Move all the children from the old UI to the new UI.
     *
     * @param oldUI
     *            the old UI whose children will be transferred to new UI
     * @param newUI
     *            the new UI where children of the old UI will be landed
     */
    default void moveToNewUI(UI oldUI, UI newUI) {
        final List<Element> uiChildren = oldUI.getElement().getChildren()
                .collect(Collectors.toList());
        uiChildren.forEach(element -> {
            element.removeFromTree(false);
            newUI.getElement().appendChild(element);
        });
    }
}
