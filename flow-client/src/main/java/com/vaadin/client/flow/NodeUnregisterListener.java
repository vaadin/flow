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
package com.vaadin.client.flow;

/**
 * A listener that will be notified when a state node is unregistered.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface NodeUnregisterListener {
    /**
     * Invoked when a state node is unregistered.
     *
     * @param event
     *            the node unregister event
     */
    void onUnregister(NodeUnregisterEvent event);
}
