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
package com.vaadin.client.flow.reactive;

/**
 * Event fired when a computation is invalidated.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class InvalidateEvent {
    private Computation source;

    /**
     * Creates a new event for computation.
     *
     * @param source
     *            the invalidated computation
     */
    public InvalidateEvent(Computation source) {
        this.source = source;
    }

    /**
     * Gets the invalidated computation.
     *
     * @return the invalidated computation
     */
    public Computation getSource() {
        return source;
    }

}
