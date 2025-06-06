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
package com.vaadin.flow.internal;

import java.io.Serializable;

/**
 * Marker interface for (Lit and Polymer) templates. All frontend files linked
 * by implementors (with {@link com.vaadin.flow.component.dependency.JsModule})
 * will be copied to {@code META-INF/VAADIN/config/templates}.
 *
 * @author Vaadin Ltd
 */
public interface Template extends Serializable {
}
