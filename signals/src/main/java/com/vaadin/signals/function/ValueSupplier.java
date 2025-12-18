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
 * Supplies a value within a specific context such as a transaction or
 * lock-protected block.
 * <p>
 * This is used when the result of an operation needs to be returned, such as
 * in transactional operations where you want to both modify signals and return
 * a value.
 *
 * @param <T>
 *            the supplied value type
 * @see Signal#runInTransaction(ValueSupplier)
 * @see Signal#runWithoutTransaction(ValueSupplier)
 * @see Signal#untracked(ValueSupplier)
 */
@FunctionalInterface
public interface ValueSupplier<T> {
    /**
     * Supplies a value.
     *
     * @return the supplied value, may be <code>null</code>
     */
    T supply();
}
