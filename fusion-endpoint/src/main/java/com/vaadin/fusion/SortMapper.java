package com.vaadin.fusion;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

public class SortMapper {

    public SortDTO toTransferType(Sort sort) {

        SortDTO sortDto = new SortDTO();
        List<OrderDTO> orders = new ArrayList<>();
        for (Order order : sort) {
            OrderDTO orderDTO = new OrderDTO();
            orderDTO.setProperty(order.getProperty());
            orderDTO.setDirection(order.getDirection());
            orderDTO.setIgnoreCase(order.isIgnoreCase());
            orderDTO.setNullHandling(order.getNullHandling());
            orders.add(orderDTO);
        }

        sortDto.setOrders(orders);
        return sortDto;
    }

    public Sort fromTransferType(SortDTO sort) {
        List<Order> orders = new ArrayList<>();
        for (OrderDTO orderDto : sort.getOrders()) {
            Order order = new Order(orderDto.getDirection(),
                    orderDto.getProperty(), orderDto.getNullHandling());
            if (orderDto.isIgnoreCase()) {
                order = order.ignoreCase();
            }
            orders.add(order);
        }
        return Sort.by(orders);
    }

}
