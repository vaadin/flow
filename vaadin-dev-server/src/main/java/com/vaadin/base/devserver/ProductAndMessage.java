/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import java.io.Serializable;

import com.vaadin.pro.licensechecker.Product;

class ProductAndMessage implements Serializable {
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
