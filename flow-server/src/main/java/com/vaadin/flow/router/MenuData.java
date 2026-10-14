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

/**
 * Data class for menu item information.
 * <p>
 * Only for read as data is immutable.
 */
public class MenuData implements Serializable {

    private final String title;
    private final Double order;
    private final boolean exclude;
    private final String icon;

    /**
     * Creates a new instance of the menu data.
     *
     * @param title
     *            the title of the menu item
     * @param order
     *            the order of the menu item
     * @param exclude
     *            whether the menu item should be excluded
     * @param icon
     *            the icon of the menu item
     */
    public MenuData(String title, Double order, boolean exclude, String icon) {
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
                + ", exclude=" + exclude + ", icon='" + icon + '\'' + '}';
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
