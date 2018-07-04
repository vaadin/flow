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
package com.vaadin.flow.server;

import java.io.InputStream;
import java.io.Serializable;

/**
 * Creates input stream instances that provides the actual data of a
 * {@link StreamResource}.
 * <p>
 * The instance of this class should generate {@link InputStream} for the
 * resource.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@FunctionalInterface
public interface InputStreamFactory extends Serializable {
    /**
     * Produce {@link InputStream} instance to read resource data.
     * <p>
     * This method is called under the Vaadin session lock. So it's safe to
     * access the application/session data which is required to produce the
     * {@link InputStream} data. The presence of the lock on subsequent access
     * to the {@link InputStream} is controlled by {@link #requiresLock()}
     * method. So {@link #createInputStream()} method is the best place to do
     * your {@link InputStream} initialization.
     * <p>
     * Return value may not be null.
     *
     * @return data input stream. May not be null.
     */
    InputStream createInputStream();

    /**
     * If this method returns {@code true} (by default) then reading data from
     * input stream (via {@link #createInputStream()} will be done under session
     * lock and it's safe to access application data within {@link InputStream}
     * read methods. Otherwise session lock won't be acquired. In the latter
     * case one must not try to access application data.
     * <p>
     * {@link #createInputStream()} is called under the session lock. Normally
     * it should be enough to get all required data from the application at this
     * point and use it to produce the data via {@link InputStream}. In this
     * case one should override {@link #requiresLock()} method to return
     * {@code false}. E.g. if {@link InputStream} instance is remote URL input
     * stream then you don't want to lock session on reading data from it.
     *
     * @return {@code true} if data from the input stream should be read under
     *         the session lock, {@code false} otherwise
     */
    default boolean requiresLock() {
        return true;
    }
}
