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
package com.vaadin.flow.renderer;

import com.vaadin.ui.Component;

/**
 * A renderer that allows the usage of regular {@link Component}s as an item
 * representation.
 *
 * @author Vaadin Ltd.
 *
 * @param <COMPONENT>
 *            the type of component this renderer can produce
 * @param <ITEM>
 *            the type of the input object that can be used by the rendered
 *            component
 *
 */
public interface ComponentRenderer<COMPONENT extends Component, ITEM> {

    /**
     * Effectively calls the functions to get component instances and set the
     * model items. This is called internally by the components which support
     * ComponentRenderers.
     *
     * @param item
     *            the corresponding model item for the component
     * @return a component instance, not {@code null}
     */
    COMPONENT createComponent(ITEM item);
}
