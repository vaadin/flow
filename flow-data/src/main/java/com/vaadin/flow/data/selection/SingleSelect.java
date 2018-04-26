/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.data.selection;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValueAndElement;

/**
 * Single selection component whose selection is treated as a value.
 *
 * @author Vaadin Ltd
 *
 * @param <C>
 *            the selection component type
 * @param <T>
 *            the selection value type
 *
 */
public interface SingleSelect<C extends Component, T>
        extends HasValueAndElement<ComponentValueChangeEvent<C, T>, T> {

}
