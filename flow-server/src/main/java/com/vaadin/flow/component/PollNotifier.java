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

import java.io.Serializable;

import com.vaadin.flow.shared.Registration;

/**
 * The interface for adding and removing {@link PollEvent} listeners.
 * <p>
 * By implementing this interface, a class publicly announces that it is able to
 * send {@link PollEvent PollEvents} whenever the client sends a periodic poll
 * message to the client, to check for asynchronous server-side modifications.
 *
 * @see UI#setPollInterval(int)
 * @since 1.0
 */
public interface PollNotifier extends Serializable {
    /**
     * Add a poll listener.
     * <p>
     * The listener is called whenever the client polls the server for
     * asynchronous UI updates.
     *
     * @see UI#setPollInterval(int)
     * @param listener
     *            the listener to add
     * @return a handle that can be used for removing the listener
     */
    default Registration addPollListener(
            ComponentEventListener<PollEvent> listener) {
        if (this instanceof Component) {
            return ComponentUtil.addListener((Component) this, PollEvent.class,
                    listener);
        } else {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addPollListener"));
        }
    }

}
