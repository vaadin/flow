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
package com.vaadin.flow.router;

import java.io.Serializable;

import com.vaadin.flow.component.Component;

/**
 * Data class for menu item information.
 * <p>
 * Only for read as data is immutable.
 *
 * @param title the title of the menu item
 * @param order the order of the menu item
 * @param exclude whether the menu item should be excluded
 * @param icon the icon of the menu item
 * @param menuClass the component class associated with this menu item
 */
public record MenuData(String title, Double order, boolean exclude, String icon,
        Class<? extends Component> menuClass) implements Serializable {

    /**
     * Gets the title of the menu item.
     *
     * @return the title of the menu item
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the order of the menu item.
     *
     * @return the order of the menu item
     */
    public Double getOrder() {
        return order;
    }

    /**
     * Gets whether the menu item should be excluded.
     *
     * @return whether the menu item should be excluded
     */
    public boolean isExclude() {
        return exclude;
    }

    /**
     * Gets the icon of the menu item.
     *
     * @return the icon of the menu item
     */
    public String getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return "MenuData{" + "title='" + title + '\'' + ", order=" + order
                + ", exclude=" + exclude + ", icon='" + icon + "', menuClass='"
                + menuClass + "'" + '}';
    }
}
