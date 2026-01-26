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
 * Represents a task to be executed within a signal transaction context. All
 * signal operations performed within the task will be staged and atomically
 * committed at the end of the transaction.
 * <p>
 * The transaction will fail and not apply any of the commands if any of the
 * commands fail. Reading a signal value within a transaction makes the
 * transaction depend on that value, causing the transaction to fail if the
 * signal value is changed concurrently.
 *
 * @see Signal#runInTransaction(TransactionTask)
 * @see Signal#runInTransaction(ValueSupplier)
 */
@FunctionalInterface
public interface TransactionTask {
    /**
     * Executes the task within a transaction context.
     */
    void execute();
}
