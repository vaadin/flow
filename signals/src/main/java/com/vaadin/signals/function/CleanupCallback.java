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
package com.vaadin.signals.function;

import com.vaadin.signals.Signal;

/**
 * Represents a cleanup operation that can be invoked to unregister a listener,
 * dispose of resources, or cancel an ongoing operation.
 * <p>
 * This is typically returned from registration methods such as
 * {@link Signal#effect(EffectAction)} to allow the caller to clean up the
 * registration when it's no longer needed.
 *
 * @see Signal#effect(EffectAction)
 */
@FunctionalInterface
public interface CleanupCallback {
    /**
     * Performs cleanup operations such as unregistering listeners or disposing
     * resources.
     */
    void cleanup();
}
