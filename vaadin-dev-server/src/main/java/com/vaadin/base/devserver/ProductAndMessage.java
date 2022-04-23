package com.vaadin.base.devserver;

import java.io.Serializable;

import com.vaadin.pro.licensechecker.Product;

public class ProductAndMessage implements Serializable {
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
