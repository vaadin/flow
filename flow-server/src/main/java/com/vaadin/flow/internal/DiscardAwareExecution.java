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
package com.vaadin.flow.internal;

import com.vaadin.flow.function.SerializableConsumer;

/**
 * An execution registered through
 * {@link StateTree#beforeClientResponse(StateNode, SerializableConsumer)} that
 * wants to know when the tree stops waiting to run it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see #executionDiscarded()
 */
public interface DiscardAwareExecution
        extends SerializableConsumer<ExecutionContext> {

    /**
     * Called when the state tree stops waiting to run this execution before a
     * response, which happens when the node it was registered for is detached
     * from the tree, and when the execution is flushed for a tree that it
     * cannot run in because its node has been moved to another one.
     * <p>
     * A detached node keeps its pending executions, so this may be called for
     * an execution that is still run later, when the node is attached to the
     * same tree again.
     */
    void executionDiscarded();
}
