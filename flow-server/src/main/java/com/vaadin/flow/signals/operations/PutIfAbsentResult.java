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
package com.vaadin.flow.signals.operations;

import java.io.Serializable;

import com.vaadin.flow.signals.Signal;

/**
 * The result of a put-if-absent operation. Contains information about whether a
 * new entry was created and a reference to the signal for the entry.
 *
 * @param <S>
 *            the signal type
 * @param created
 *            <code>true</code> if a new entry was inserted, <code>false</code>
 *            if the key already existed
 * @param entry
 *            the signal for the map entry, not <code>null</code>
 */
public record PutIfAbsentResult<S extends Signal<?>>(boolean created,
        S entry) implements Serializable {
}
