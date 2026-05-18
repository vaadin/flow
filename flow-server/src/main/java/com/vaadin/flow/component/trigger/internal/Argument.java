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
package com.vaadin.flow.component.trigger.internal;

import java.io.Serializable;

/**
 * A value produced on the client at the moment a trigger fires, snapshotted and
 * passed into the bound {@link Action actions}.
 * <p>
 * Arguments are resolved synchronously when the trigger fires, never
 * reactively.
 * <p>
 * The same {@code Argument} instance may be referenced from multiple actions;
 * its value is computed once per fire and reused.
 * <p>
 * Implementations should extend {@link AbstractArgument}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value produced
 */
public sealed interface Argument<T> extends Serializable
        permits AbstractArgument {
}
