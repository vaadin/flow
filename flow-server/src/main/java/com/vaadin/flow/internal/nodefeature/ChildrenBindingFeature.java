/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.ListSignal;
import com.vaadin.signals.Signal;

/**
 * Node feature for binding a {@link Signal} to the children of a node.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ChildrenBindingFeature extends ServerSideFeature {

    /**
     * Creates a ChildrenBindingFeature for the given node.
     *
     * @param node
     *            the node which supports the feature
     */
    public ChildrenBindingFeature(StateNode node) {
        super(node);
    }

    private Registration registration;
    private ListSignal<?> listSignal;

    public void setBinding(Registration registration,
            ListSignal<?> listSignal) {
        this.registration = registration;
        this.listSignal = listSignal;
    }

    public boolean hasBinding() {
        return listSignal != null && registration != null;
    }

    public void removeBinding() {
        if (registration != null) {
            registration.remove();
        }
        registration = null;
        listSignal = null;
    }
}
