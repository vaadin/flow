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
package com.vaadin.flow.component;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;

/**
 * A generic interface for components and other user interface objects that may
 * be enabled or disabled. The server will ignore incoming events for a disabled
 * element unless the listener has been explicitly configured to allow events in
 * a disabled state using e.g.
 * {@link DomListenerRegistration#setDisabledUpdateMode(DisabledUpdateMode)} or
 * {@link Element#addSynchronizedProperty(String, DisabledUpdateMode)}.
 * <p>
 * Implementing classes should <b>not</b> define their own implementations of
 * the methods defined in this interface since the framework's overall security
 * capabilities are dependent on a correct implementation. Instead, the visual
 * representation of a disabled component can be configured by overriding
 * {@link Component#onEnabledStateChanged(boolean)}.
 * <p>
 * An element may be implicitly or explicitly disabled.
 * <ul>
 * <li>It is explicitly disabled if it's disabled via the
 * {@code setEnabled(false)} call.
 * <li>It is implicitly disabled if it has an ascendant which is explicitly
 * disabled.
 * </ul>
 *
 * An element is enabled if it's not explicitly disabled and there is no
 * disabled ascendant.
 * <p>
 * An implicitly disabled element becomes enabled automatically if its disabled
 * ascendant becomes enabled.
 * <p>
 * An element may be explicitly disabled when it is only implicitly disabled.
 * Such element remains disabled when its ascendant becomes enabled.
 * <p>
 * Note that an element may change its enabled state if it's inside a disabled
 * parent and it becomes detached from it. In this case if it has not been
 * explicitly disabled then it becomes enabled until it's attached. If the new
 * parent is enabled then the element remains enabled. Otherwise it becomes
 * disabled again.
 *
 *
 * @author Vaadin Ltd
 * @since 1.0
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
        /*
         * XXX WARNING Do not override this method. Propagating the enabled
         * state to the element in this way is critical to fulfill generic
         * assumptions with regards to application security.
         */
        getElement().setEnabled(enabled);
    }

    /**
     * Returns whether the object is enabled or disabled.
     * <p>
     * Object may be enabled by itself by but if its ascendant is disabled then
     * it's considered as (implicitly) disabled.
     *
     * @return enabled state of the object
     */
    default boolean isEnabled() {
        /*
         * XXX WARNING Do not override this method. Reading the enabled state
         * from the element in this way is critical to fulfill generic
         * assumptions with regards to application security.
         */
        return getElement().isEnabled();
    }
}
