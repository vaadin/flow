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
package com.vaadin.flow.router.internal;

import java.io.Serializable;

import com.vaadin.flow.component.Component;

/**
 * A pair of a navigation target for handling exceptions and the exception
 * type handled by the navigation target.
 *
 * @since 1.3
 */
public class ErrorTargetEntry implements Serializable {
    private final Class<? extends Component> navigationTarget;
    private final Class<? extends Exception> handledExceptionType;

    /**
     * Creates a new new entry with the given navigation target type and
     * exception type.
     *
     * @param navigationTarget
     *         the navigation target type, not <code>null</code>
     * @param handledExceptionType
     *         the exception type handled by the navigation target, not
     *         <code>null</code>
     */
    public ErrorTargetEntry(Class<? extends Component> navigationTarget,
            Class<? extends Exception> handledExceptionType) {
        assert navigationTarget != null;
        assert handledExceptionType != null;

        this.navigationTarget = navigationTarget;
        this.handledExceptionType = handledExceptionType;
    }

    /**
     * Gets the navigation target type.
     *
     * @return the navigation target type, not <code>null</code>
     */
    public Class<? extends Component> getNavigationTarget() {
        return navigationTarget;
    }

    /**
     * Gets the exception type handled by the navigation target.
     *
     * @return the exception type, not <code>null</code>
     */
    public Class<? extends Exception> getHandledExceptionType() {
        return handledExceptionType;
    }
}
