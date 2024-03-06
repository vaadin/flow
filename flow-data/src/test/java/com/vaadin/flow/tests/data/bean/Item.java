/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.tests.data.bean;

import java.util.Objects;

public class Item {

    private long id;
    private String value;
    private String description;

    public Item(long id) {
        this(id, null);
    }

    public Item(long id, String value) {
        this(id, value, null);
    }

    public Item(long id, String value, String description) {
        this.id = id;
        this.value = value;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Item item = (Item) o;
        return id == item.id && Objects.equals(value, item.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, value);
    }
}
