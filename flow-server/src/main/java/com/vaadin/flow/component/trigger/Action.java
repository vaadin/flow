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
package com.vaadin.flow.component.trigger;

import java.io.Serializable;

/**
 * Something that runs on the client when a {@link Trigger} fires.
 * <p>
 * An action may also have a server-side effect that mirrors what just happened
 * in the browser (for example, reporting back whether
 * {@code navigator.clipboard.writeText} resolved or rejected). The mirror runs
 * on the UI thread when the client posts a notification back through the per-
 * host return channel.
 * <p>
 * The same {@code Action} instance may be wired to multiple triggers on the
 * same host; the client-side handler runs once per binding.
 * <p>
 * Implementations should extend {@link AbstractAction}.
 */
public interface Action extends Serializable {
}
