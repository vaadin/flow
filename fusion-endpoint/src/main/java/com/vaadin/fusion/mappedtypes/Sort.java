/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.mappedtypes;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.fusion.Nonnull;

/**
 * A DTO for {@link org.springframework.data.domain.Sort}.
 */
public class Sort {
    @Nonnull
    private List<Order> orders = new ArrayList<>();

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

}
