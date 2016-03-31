/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.component;

import java.util.Optional;

import com.vaadin.ui.Component;

/**
 * Interface for components which allows the user to add and remove components
 * to them.
 * <p>
 * The components are by default added as direct children to the component
 * element, as returned by {@link #getElement()}.
 *
 * @author Vaadin
 * @since
 */
public interface HasSimpleAddComponent extends Component {
    default void addComponents(Component... components) {
        for (Component component : components) {
            getElement().appendChild(component.getElement());
        }
    }

    default void removeComponents(Component... components) {
        for (Component component : components) {
            getElement().removeChild(component.getElement());
        }
    }

    default void removeAllComponents() {
        for (int i = 0; i < getElement().getChildCount(); i++) {
            Optional<Component> component = getElement().getChild(i)
                    .getComponent();
            if (component.isPresent()) {
                getElement().removeChild(i);
                i--;
            }
        }
    }

}
