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
 * only runs while its node is visible.
 * <p>
 * JavaScript invocations for a node that is not visible, or that has an
 * invisible ancestor, are held back until the node becomes visible again. An
 * execution whose only purpose is to update the client side of its node, such
 * as sending the items of a data component, would therefore pile up undelivered
 * invocations for every response written while the node is hidden. The state
 * tree instead keeps an execution of this type waiting on the node, and runs it
 * before the first response after the node has become visible.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@FunctionalInterface
public interface VisibleNodeExecution
        extends SerializableConsumer<ExecutionContext> {
}
