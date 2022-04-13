package com.vaadin.base.devserver;

import com.vaadin.pro.licensechecker.Product;

public class ProductAndMessage {
    private Product product;
    private String message;

    public ProductAndMessage(Product product, String message) {
        this.product = product;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Product getProduct() {
        return product;
    }
}
