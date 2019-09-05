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

import java.io.Serializable;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;

/**
 * The implementation of this interface is responsible for updating the UI with
 * given content.
 */
public interface UIInternalsHandler extends Serializable {
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
    void updateRoot(UI ui, HasElement oldRoot, HasElement newRoot);

    /**
     * Move all the children from the old UI to the new UI.
     * 
     * @param oldUI
     *            the old UI whose children will be transfered to new UI
     * @param newUI
     *            the new UI where children of the old UI will be landed
     */
    void moveToNewUI(UI oldUI, UI newUI);
}
