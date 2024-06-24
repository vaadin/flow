/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import com.vaadin.pro.licensechecker.Product;

public class ProductAndMessage implements DebugWindowData {
    private final Product product;
    private final String message;

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

    @Override
    public String toJson() {
        return String.format(
                "{\"product\": {\"name\": \"%s\", \"version\": \"%s\"}, \"message\": \"%s\"}",
                product.getName(), product.getVersion(), message);
    }
}
