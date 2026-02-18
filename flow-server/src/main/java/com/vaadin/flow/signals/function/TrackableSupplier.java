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
package com.vaadin.flow.signals.function;

import java.io.Serializable;

import com.vaadin.flow.signals.impl.UsageRegistrar;
import com.vaadin.flow.signals.impl.UsageTracker;
import org.jspecify.annotations.Nullable;

/**
 * Represents an executable supplier that is potentially using signals and
 * therefore is a subject of dependency usage tracking.
 *
 * @param <T>
 *            the type of the supplied value
 * @see ValueSupplier#supply()
 * @see UsageTracker#tracked(TrackableSupplier, UsageRegistrar)
 */
@FunctionalInterface
public interface TrackableSupplier<T> extends Serializable {
    /**
     * Runs the function that potentially uses some signals and gets the result.
     *
     * @return the result value
     */
    @Nullable T supply();
}
