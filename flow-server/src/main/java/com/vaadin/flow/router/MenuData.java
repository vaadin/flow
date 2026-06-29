/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.Component;

/**
 * Data class for menu item information.
 * <p>
 * Only for read as data is immutable.
 *
 * @since 24.4
 */
public record MenuData(String title, Double order, boolean exclude, String icon,
        Class<? extends Component> menuClass) implements Serializable {

    /**
     * MenuData constructor.
     *
     * @param title
     *            title of the menu item
     * @param order
     *            order of the menu item
     * @param exclude
     *            whether the menu item should be excluded
     * @param icon
     *            the icon of the menu item
     *
     * @deprecated Use {@link #MenuData(String, Double, boolean, String, Class)}
     *             instead.
     */
    @Deprecated(forRemoval = true)
    public MenuData(String title, Double order, boolean exclude, String icon) {
        this(title, order, exclude, icon, null);
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
