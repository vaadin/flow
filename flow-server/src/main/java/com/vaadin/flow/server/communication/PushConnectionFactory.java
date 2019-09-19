/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.communication;

import java.util.ServiceLoader;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Factory that produces instances of {@link PushConnection}.
 *
 * Produces a {@link PushConnection} for the provided {@link UI}.
 *
 * Factory instances are by default discovered and instantiated using
 * {@link ServiceLoader}. This means that all implementations must have a
 * zero-argument constructor and the fully qualified name of the implementation
 * class must be listed on a separate line in a
 * META-INF/services/com.vaadin.flow.server.communication.PushConnectionFactory file
 * present in the jar file containing the implementation class.
 *
 * @since 1.0
 */
public interface PushConnectionFactory extends SerializableFunction<UI, PushConnection> {
}
