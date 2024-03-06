/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
            element.removeFromTree();
            newUI.getElement().appendChild(element);
        });
    }
}
