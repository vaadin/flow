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

/**
 * Base class for {@link Action} implementations. Subclasses append the JS that
 * runs when the trigger fires by overriding
 * {@link #appendStatement(JsBuilder, StringBuilder)}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract non-sealed class AbstractAction implements Action {

    /**
     * Appends this action's JS statement to {@code out}. Element references
     * must go through {@link JsBuilder#reference}.
     *
     * @param builder
     *            collects element parameter references, not {@code null}
     * @param out
     *            buffer to append into, not {@code null}
     */
    protected abstract void appendStatement(JsBuilder builder,
            StringBuilder out);
}
