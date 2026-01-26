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
 * Represents an action to be executed as a signal effect. The action is run
 * when the effect is created and subsequently run again whenever any signal
 * dependency changes.
 * <p>
 * Signal effects automatically track dependencies on all signals that are read
 * during the action execution. When any of those signals change, the action is
 * re-run with updated dependencies.
 *
 * @see Signal#effect(EffectAction)
 */
@FunctionalInterface
public interface EffectAction {
    /**
     * Executes the effect action, automatically tracking signal dependencies.
     */
    void execute();
}
