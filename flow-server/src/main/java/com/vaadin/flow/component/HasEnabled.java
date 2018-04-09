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
package com.vaadin.flow.component;

import com.vaadin.flow.dom.Element;

/**
 * A generic interface for components and other user interface objects that may
 * be enabled or disabled.
 * <p>
 * Element may be implicitly or explicitly disabled.
 * <ul>
 * <li>Element is explicitly disabled if it's disabled via the
 * {@code setEnabled(false)} call.
 * <li>Element is implicitly disabled if it has an ascendant which is explicitly
 * disabled.
 * </ul>
 *
 * Element is enabled if it's not explicitly disabled and there is no disabled
 * ascendant.
 * <p>
 * An implicitly disabled element becomes enabled automatically if its disabled
 * ascendant becomes enabled.
 * <p>
 * Element may be explicitly disabled being in a disabled parent. Such element
 * remains disabled when its parent becomes enabled.
 * <p>
 * Note that an element may change its enabled state if it's inside disabled
 * parent and it becomes detached from it. In this case if it has not been
 * explicitly disabled then it becomes enabled until it's attached. If the new
 * parent is enabled then the element remains enabled. Otherwise it becomes
 * disabled again.
 *
 *
 * @author Vaadin Ltd
 *
 */
public interface HasEnabled extends HasElement {

    /**
     * Sets the UI object explicitly disabled or enabled.
     *
     * @param enabled
     *            if {@code false} then explicitly disables the object, if
     *            {@code true} then enables the object so that its state depends
     *            on parent
     */
    default void setEnabled(boolean enabled) {
        getElement().getNode().setEnabled(enabled);
    }

    /**
     * Returns whether the object is enabled or disabled.
     * <p>
     * Object may be enabled by itself by but if its ascendant is disabled then
     * it's considered as (implicitly) disabled.
     *
     * @return eanbled state of the object
     */
    default boolean isEnabled() {
        if (getElement().getNode().isEnabled()) {
            Element parent = getElement().getParent();
            while (parent != null) {
                if (parent instanceof HasEnabled) {
                    return ((HasEnabled) parent).isEnabled();
                }
                parent = parent.getParent();
            }
            return true;
        }
        return false;
    }
}
