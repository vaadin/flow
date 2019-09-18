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
package com.vaadin.flow.server.startup;

import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.PageTitle;

/**
 * Exception indicating a conflict between a route target implementing
 * {@link HasDynamicTitle} while also having a {@link PageTitle} annotation.
 *
 * @since 1.0
 */
public class DuplicateNavigationTitleException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message
     *            the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     */
    public DuplicateNavigationTitleException(String message) {
        super(message);
    }
}
