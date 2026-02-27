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

import com.vaadin.flow.signals.EffectContext;
import com.vaadin.flow.signals.Signal;

/**
 * Represents a context-aware action to be executed as a signal effect. The
 * action receives an {@link EffectContext} that provides information about why
 * the effect is running, such as whether it is the initial execution or whether
 * the change originated from a background context.
 * <p>
 * Signal effects automatically track dependencies on all signals that are read
 * during the action execution. When any of those signals change, the action is
 * re-run with updated dependencies.
 *
 * @see Signal#unboundEffect(EffectAction)
 * @see EffectContext
 */
@FunctionalInterface
public interface ContextualEffectAction extends Serializable {
    /**
     * Executes the effect action with the given context, automatically tracking
     * signal dependencies.
     *
     * @param context
     *            the effect context providing information about why the effect
     *            is running
     */
    void execute(EffectContext context);
}
