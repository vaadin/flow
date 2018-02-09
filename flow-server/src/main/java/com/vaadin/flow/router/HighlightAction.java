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
package com.vaadin.flow.router;

import com.vaadin.flow.function.SerializableBiConsumer;

/**
 * An action to be performed to set the highlight state of the target.
 *
 * @param <T>
 *            the target type of the highlight action
 */
@FunctionalInterface
public interface HighlightAction<T> extends SerializableBiConsumer<T, Boolean> {

    /**
     * Performs the highlight action on the target.
     *
     * @param t
     *            the target of the highlight action
     * @param highlight
     *            true if the target should be highlighted
     */
    @Override
    void accept(T t, Boolean highlight);
}
