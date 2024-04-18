/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import java.util.Objects;

/**
 * Data class for menu item information.
 * <p>
 * Only for read as data is immutable.
 */
public class MenuData implements Serializable {

    private final String title;
    private final Long order;
    private final boolean exclude;
    private final String icon;

    /**
     * Creates a new instance of the menu data.
     *
     * @param title the title of the menu item
     * @param order the order of the menu item
     * @param exclude whether the menu item should be excluded
     * @param icon the icon of the menu item
     */
    public MenuData(String title, Long order, boolean exclude, String icon) {
        this.title = title;
        this.order = order;
        this.exclude = exclude;
        this.icon = icon;
    }

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
    public Long getOrder() {
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
        return "MenuData{" +
                "title='" + title + '\'' +
                ", order=" + order +
                ", exclude=" + exclude +
                ", icon='" + icon + '\'' +
                '}';
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof MenuData other
                && Objects.equals(title, other.title)
                && Objects.equals(order, other.order)
                && Objects.equals(exclude, other.exclude)
                && Objects.equals(icon, other.icon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, order, exclude, icon);
    }
}
