package com.vaadin.fusion.endpointransfermapper;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.fusion.Nonnull;

import org.springframework.data.domain.Sort;

/**
 * A DTO for {@link Sort}.
 */
public class SortDTO {
    @Nonnull
    private List<OrderDTO> orders = new ArrayList<>();

    public List<OrderDTO> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderDTO> orders) {
        this.orders = orders;
    }

}
